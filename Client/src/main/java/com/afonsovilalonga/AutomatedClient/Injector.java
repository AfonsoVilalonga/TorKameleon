package com.afonsovilalonga.AutomatedClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.afonsovilalonga.Utils.Config;
import com.afonsovilalonga.Utils.DTLSOverDatagram;
import com.afonsovilalonga.Utils.Stats;

public class Injector implements Runnable{
    
    private String file;
    private String protocol;
    private int num_reqs;

    public static final int TIMEOUT = 50000;
    public static final int BUF_SIZE = 4096;

    public Injector(String protocol, String file, int num_reqs){
        this.file = file;
        this.protocol = protocol;
        this.num_reqs = num_reqs;
    }

    @Override
    public void run() {
        try {
            executeCommand();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executeCommand() throws Exception {
        Config config = Config.getInstance();
        String remote_host = config.get_remote_host();
        int remote_port_secure = config.getRemote_port_secure();
        int remote_port_unsecure = config.getRemote_port_unsecure();

        switch (protocol.toLowerCase()) {
            case "tcp":
                for(int i = 0; i < num_reqs; i++){
                    Socket tcp_socket = new Socket(remote_host, remote_port_unsecure);
                    do_TCP_TLS(tcp_socket, file);
                    tcp_socket.close();
                }
               
                break;
            case "tls":
                for(int i = 0; i < num_reqs; i++){
                    Socket tls_socket = getSecureSocket(remote_host, remote_port_secure);
                    do_TCP_TLS(tls_socket, file);
                    tls_socket.close();
                }
                break;
            case "udp":
                for(int i = 0; i < num_reqs; i++){
                    DatagramSocket udp_socket = new DatagramSocket();
                    doUDP(file.getBytes(), udp_socket, remote_host, remote_port_unsecure);
                    udp_socket.close();
                }
                break;
            case "dtls":
                for(int i = 0; i < num_reqs; i++){
                    DatagramSocket dtls_socket = new DatagramSocket();
                    doDTLS(dtls_socket, file.getBytes(), remote_host, remote_port_secure);
                    dtls_socket.close();
                }
                break;
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

        out.write(String.format("GET %s HTTP/1.1\r\n\r\n", path).getBytes());

        int n = 0;
        byte[] buffer = new byte[BUF_SIZE];
        while ((n = in.read(buffer, 0, buffer.length)) != -1) {
            stats.newRequest(n);
            //System.out.write(buffer, 0, n);
        }
        stats.printReport();
    }

    private static void doUDP(byte[] path, DatagramSocket socket, String host, int port) {
        try {
            socket.setSoTimeout(TIMEOUT);
            InetAddress IPAddress = InetAddress.getByName(host);
            Stats stats = new Stats();
            DatagramPacket sendPacket = new DatagramPacket(path, path.length, IPAddress, port);
            socket.send(sendPacket);
            //receive
            byte[] receive = new byte[BUF_SIZE];
            DatagramPacket receivedPacket = new DatagramPacket(receive, receive.length);
            try {
                while (true) {
                    socket.receive(receivedPacket);
                    String receivedData = new String(receivedPacket.getData());
                    if (receivedData.contains("terminate_packet_receive")) {
                        break;
                    }
                    stats.newRequest(receivedPacket.getLength());
                    System.out.println(receivedData);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            socket.close();
            stats.printReport();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to do UDP");
        }
    }

    private static void doDTLS(DatagramSocket socket, byte[] filePath, String host, int port) {
        try {
            socket.setSoTimeout(TIMEOUT);
            DTLSOverDatagram dtls = new DTLSOverDatagram();
            InetSocketAddress isa = new InetSocketAddress(host, port);

            SSLEngine engine = doDTLSHandshake(socket, isa, dtls);

            dtls.deliverAppData(engine, socket, ByteBuffer.wrap(filePath), isa);
            dtls.receiveAppData(engine, socket);
            socket.close();
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
