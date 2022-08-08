package com.afonsovilalonga.Httping;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Ping {
    public static final byte VERSION_4 = 0x04;
    public static final byte VERSION_RESP = 0x00;
    public static final byte RESP_GRANTED = 0x5a;
    public static final byte TCP_STREAM = 0x01;

    public static final int tor_buffer_size = 514;

    private Socket tor_sock;

    public Ping(){
        try {
            String tor_host = "localhost";
            int tor_port = 9050;
            int tor_buffer_size = 514;
    
            while(tor_sock == null)
                tor_sock = socksv4SendRequest("192.99.168.235", 10001, tor_host, tor_port);
                    
            tor_sock.setReceiveBufferSize(tor_buffer_size);
            tor_sock.setSendBufferSize(tor_buffer_size);

            new Thread(() -> {
                ExecutorService executor = null;
                try (ServerSocket server = new ServerSocket(1234)) {
                    executor = Executors.newFixedThreadPool(1);
                    while (true) {
                        final Socket socket = server.accept();
                        System.out.println("Httping connection " + socket.getInetAddress() + ":" + socket.getPort());
                        executor.execute(() -> {
                            try {
                                torRequest(socket);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } finally {
                    if (executor != null) {
                        executor.shutdown();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void torRequest(Socket socket) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();
        
        byte[] buffer = new byte[1024];
        int i = in.read(buffer);

        OutputStream out_tor = tor_sock.getOutputStream();
        InputStream in_tor = tor_sock.getInputStream();

        out_tor.write(buffer, 0, i);
        int n = 0;

        byte[] rcv = new byte[514];
        while((n = in_tor.read(rcv)) >= 0){
            baos.write(rcv, 0, n);
        }
        
        out.write(rcv, 0, rcv.length);
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
}
