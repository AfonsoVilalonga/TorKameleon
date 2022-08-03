package com.afonsovilalonga.InteractiveClient;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.afonsovilalonga.Utils.Config;
import com.afonsovilalonga.Utils.DTLSOverDatagram;
import com.afonsovilalonga.Utils.Stats;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class InteractiveClient {
    public static final int BUF_SIZE = 4096;

    public static void run() throws Exception {
        Config config = Config.getInstance();
        String remote_host = config.get_remote_host();
        int remote_port_secure = config.getRemote_port_secure();
        int remote_port_unsecure = config.getRemote_port_unsecure();
        Scanner inFromUser;
        
        while (true) {
            inFromUser = new Scanner(System.in);
            String path = null;
            String protocol = null;
            try {
                String[] input = inFromUser.nextLine().split(" ");
                path = input[0];
                protocol = input[1];
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Usage: file protocol(tcp,udp,tls,dtls) or url browse");
                System.exit(1);
            }
            switch (protocol.toLowerCase()) {
                case "tcp":
                    Socket tcp_socket = new Socket(remote_host, remote_port_unsecure);
                    do_TCP_TLS(tcp_socket, path);
                    tcp_socket.close();
                    break;
                case "tls":
                    Socket tls_socket = getSecureSocket(remote_host, remote_port_secure);
                    do_TCP_TLS(tls_socket, path);
                    tls_socket.close();
                    break;
                case "udp":
                    DatagramSocket udp_socket = new DatagramSocket();
                    doUDP(path.getBytes(), udp_socket, remote_host, remote_port_unsecure);
                    udp_socket.close();
                    break;
                case "dtls":
                    DatagramSocket dtls_socket = new DatagramSocket();
                    doDTLS(dtls_socket, path.getBytes(), remote_host, remote_port_secure);
                    dtls_socket.close();
                    break;
            }
        }
    }

    private static Socket getSecureSocket(String host, int port) throws IOException {
        SSLSocketFactory factory =
                (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket =
                (SSLSocket) factory.createSocket(host, port);
        socket.startHandshake();
        return socket;
    }

    private static void do_TCP_TLS(Socket socket, String path) throws IOException {
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();
        Stats stats = new Stats();

        byte[] message = String.format("GET %s HTTP/1.1\r\n\r\n", path).getBytes();
        out.write(message);
        out.flush();
        int n;
        byte[] buffer = new byte[BUF_SIZE];
        while ((n = in.read(buffer, 0, buffer.length)) != -1) {
            stats.newRequest(n);
            System.out.write(buffer, 0, n);
        }
        stats.printReport();
    }

    private static void doUDP(byte[] path, DatagramSocket clientSocket, String host, int port) {
        try {
            InetAddress IPAddress = InetAddress.getByName(host);
            Stats stats = new Stats();
            DatagramPacket sendPacket = new DatagramPacket(path, path.length, IPAddress, port);
            clientSocket.send(sendPacket);
            //receive
            byte[] receive = new byte[BUF_SIZE];
            DatagramPacket receivedPacket = new DatagramPacket(receive, receive.length);
            try {
                while (true) {
                    clientSocket.receive(receivedPacket);
                    String receivedData = new String(receivedPacket.getData());
                    if (receivedData.contains("terminate_packet_receive")) {
                        break;
                    } //server side sends terminate receive packet
                    //clientSocket.send(receivedPacket);
                    stats.newRequest(receivedPacket.getLength());
                    System.out.println(receivedData);
                }
            } catch (Exception e) {
            }
            stats.printReport();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to do UDP");
        }
    }

    private static void doDTLS(DatagramSocket socket, byte[] filePath, String host, int port) {
        try {
            DTLSOverDatagram dtls = new DTLSOverDatagram();
            InetSocketAddress isa = new InetSocketAddress(host, port);

            SSLEngine engine = doDTLSHandshake(socket, isa, dtls);

            dtls.deliverAppData(engine, socket, ByteBuffer.wrap(filePath), isa);
            dtls.receiveAppData(engine, socket);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unable to do DTLS");
        }
    }

    private static SSLEngine doDTLSHandshake(DatagramSocket socket, InetSocketAddress isa, DTLSOverDatagram dtls) throws Exception {
        SSLEngine engine = dtls.createSSLEngine(true);
        dtls.handshake(engine, socket, isa, "Client");
        return engine;
    }

}

