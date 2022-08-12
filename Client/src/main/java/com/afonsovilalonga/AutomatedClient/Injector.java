package com.afonsovilalonga.AutomatedClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
    

    public static final byte VERSION_4 = 0x04;
    public static final byte VERSION_RESP = 0x00;
    public static final byte RESP_GRANTED = 0x5a;
    public static final byte TCP_STREAM = 0x01;
    

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
            System.out.println("oi");
            while(!tor_init(1000));
            executeCommand();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executeCommand() throws Exception {
        String tor_host = "localhost";
        int tor_port = 9050;
        Config config = Config.getInstance();
        String remote_host = config.get_remote_host();
        int remote_port_secure = config.getRemote_port_secure();
        int remote_port_unsecure = config.getRemote_port_unsecure();

        switch (protocol.toLowerCase()) {
            case "tcp":
                Socket tcp_socket = socksv4SendRequest("192.99.168.235", 10002, tor_host, tor_port);
                OutputStream out = tcp_socket.getOutputStream();
                InputStream in = tcp_socket.getInputStream();
                for(;;){
                    do_TCP_TLS(out, in, file);
                }
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

    private static void do_TCP_TLS(OutputStream out, InputStream in, String path) throws IOException {
        Stats stats = new Stats();

        out.write(String.format("GET %s HTTP/1.1\r\n\r\n", path).getBytes());
        byte[] size_buff = new byte[4]; 
        in.read(size_buff);
        ByteBuffer wrapper = ByteBuffer.wrap(size_buff);
        int size = wrapper.getInt();

        int recv = 0;
        int n = 0;
        byte[] buffer = new byte[BUF_SIZE];

        System.out.println(size);

        while ((n = in.read(buffer, 0, buffer.length)) != -1 && recv != size) {
            recv += n;
            stats.newRequest(n);
            //System.out.write(buffer, 0, n);
        }
        stats.printReport();
    }

    private static void do_TCP_TLS(Socket socket, String path) throws IOException {
        Stats stats = new Stats();
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();
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

    public static boolean tor_init(long sleep) {
        try (Socket tor = new Socket("localhost", 9051)) {
            DataOutputStream out_tor = new DataOutputStream(new BufferedOutputStream(tor.getOutputStream()));
            DataInputStream in_tor = new DataInputStream(new BufferedInputStream(tor.getInputStream()));

            boolean done = false;
            byte[] recv = new byte[2048];

            while (!done) {
                out_tor.write("AUTHENTICATE\r\n".getBytes());
                out_tor.flush();

                in_tor.read(recv);

                out_tor.writeBytes("GETINFO status/bootstrap-phase\r\n");
                out_tor.flush();

                recv = new byte[2048];
                in_tor.read(recv);

                String progress = new String(recv);

                if (progress.contains("100"))
                    done = true;
                else
                    Thread.sleep(sleep);
            }
            return done;
        } catch (IOException e) {
        } catch (InterruptedException e) {}
        return false;
    }


    private static Socket socksv4SendRequest(String remote_host, int remote_port,  String tor_host, int tor_port){
        Socket socket = new Socket();   
        try { 
            socket.connect(new InetSocketAddress(tor_host, tor_port));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            
            out.writeByte(VERSION_4);
            out.writeByte(TCP_STREAM);
            out.writeShort((short) remote_port);

            out.writeInt(0x01);
            out.writeByte(0x00);

            out.write(remote_host.getBytes());
            out.writeByte(0x00);

            out.flush();

            DataInputStream in = new DataInputStream(socket.getInputStream());
            
            byte version = in.readByte();
            byte resp = in.readByte();

            if(version != VERSION_RESP || resp != RESP_GRANTED){
                socket.close();
                return null;
            }

            in.readShort();
            in.readInt();
            
            return socket;
        } catch (IOException e) {
        }

        return null;
    } 


}
