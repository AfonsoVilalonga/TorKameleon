package com.afonsovilalonga.AutomatedClient;


import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.afonsovilalonga.Utils.DTLSOverDatagram;
import com.afonsovilalonga.Utils.Stats;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class AutomatedClient {
    public static final String SPACE = " ";
    public static final int TIMEOUT = 50000;
    private static int COMMAND_MAX_TIMER = 15000;
    private static int COMMAND_MIN_TIMER = 5000;
    public static int remote_port_secure = 2000;
    public static int remote_port_unsecure = 1234;
    public static String remote_host = "127.0.0.1"; // 172.28.0.5 or 127.0.0.1;
    public static final int BUF_SIZE = 4096;

    public static final List<String> files =
            List.of("/Files/large", "/Files/book.pdf", "/Files/small"); // "/Files/large", "/Files/small", ,
    public static final List<String> protocols =
            List.of("tcp", "tls", "udp", "dtls"); // "tls", "udp", "dtls"

    public static String command = "";

    public static void main(String[] argv) {
        //TIRMMRT certificate for server side authentication
        System.setProperty("javax.net.ssl.trustStore", "./keystore/tirmmrts");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        readConfigurationFiles();
        nextCommandTriggeredTimer();

    }

    private static void executeCommand() throws Exception {
        String path = null;
        String protocol = null;
        try {
            String[] input = command.split(SPACE);
            path = input[0];
            protocol = input[1];
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Usage: file protocol(tcp,udp,tls,dtls)");
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

    private static void nextCommand() {
        Random rand = new Random();
        String pickRandomFile = files.get(rand.nextInt(files.size()));
        String pickRandomProtocol = protocols.get(rand.nextInt(protocols.size()));
        command = pickRandomFile + SPACE + pickRandomProtocol;
        System.err.println("Selected command is: " + command);
    }

    private static void nextCommandTriggeredTimer() {
        Timer timer = new Timer();
        int command_timer = (new Random()).nextInt(COMMAND_MAX_TIMER - COMMAND_MIN_TIMER) + COMMAND_MIN_TIMER;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                nextCommand();
                try {
                    executeCommand();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, command_timer);
    }

    private static void readConfigurationFiles() {

        try (InputStream input = new FileInputStream("./configuration/config.properties")) {
            Properties prop = new Properties();

            prop.load(input);

            remote_host = prop.getProperty("remote_host");
            remote_port_unsecure = Integer.parseInt(prop.getProperty("remote_port_unsecure"));
            remote_port_secure = Integer.parseInt(prop.getProperty("remote_port_secure"));
            COMMAND_MAX_TIMER = Integer.parseInt(prop.getProperty("COMMAND_MAX_TIMER"));
            COMMAND_MIN_TIMER = Integer.parseInt(prop.getProperty("COMMAND_MIN_TIMER"));


        } catch (IOException e) {
            e.printStackTrace();
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

        out.write(String.format("GET %s HTTP/1.1", path).getBytes());

        int n = 0;
        byte[] buffer = new byte[BUF_SIZE];
        while ((n = in.read(buffer, 0, buffer.length)) != -1) {
            stats.newRequest(n);
            System.out.write(buffer, 0, n);
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