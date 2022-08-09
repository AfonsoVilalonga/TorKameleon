package com.afonsovilalonga.HttpingIperf;

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

    public Ping() {
            new Thread(() -> {
                try (ServerSocket server = new ServerSocket(1234)) {
                    String tor_host = "localhost";
                    int tor_port = 9050;
                    while (true) {
                        Socket socket = server.accept();
                        Socket tor_sock = socksv4SendRequest("192.99.168.235", 10001, tor_host, tor_port);
                        System.out.println("Httping connection " + socket.getInetAddress() + ":" + socket.getPort());
                        try {
                            ping(socket, tor_sock);
                            tor_sock.close();
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } 
            }).start();

            new Thread(() -> {
                try (ServerSocket server = new ServerSocket(10010)) {
                    String tor_host = "localhost";
                    int tor_port = 9050;
                    int l = 0;
                    while (true) {
                        Socket socket_1 = server.accept();
                        Socket tor_sock =socksv4SendRequest("192.99.168.235", 5201, tor_host, tor_port);
                        System.out.println("Iperf connection " + socket_1.getInetAddress() + ":" + socket_1.getPort());
                        try {
                            iperf(socket_1, tor_sock, l);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        l++;
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } 
            }).start();
        
    }

    public void iperf(Socket socket, Socket tor_sock, int l) throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        OutputStream out_tor = tor_sock.getOutputStream();
        InputStream in_tor = tor_sock.getInputStream();



        executorService.execute(() -> {
            int b = 0;
            byte[] rcv = new byte[2048];
            try {
				while((b = in.read(rcv)) > 0){
				    out_tor.write(rcv,0,b);
                    //out.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

            try {
                socket.close();
                tor_sock.close();    
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        });

        executorService.execute(() -> {
            int n = 0;
            byte[] buffer = new byte[2048];
            
            try {
				while((n = in_tor.read(buffer)) > 0){
				    out.write(buffer,0,n);
                    //out.flush();
				}
			} catch (IOException e) {
                e.printStackTrace();
			}
            try {
                socket.close();
                tor_sock.close();   
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
 
        });
        
    }

    public void ping(Socket socket, Socket tor_sock) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        byte[] buffer = new byte[1024];
        int i = in.read(buffer);

        OutputStream out_tor = tor_sock.getOutputStream();
        InputStream in_tor = tor_sock.getInputStream();

        out_tor.write(buffer, 0, i);
        out_tor.flush();
        int n = 0;
        byte[] rcv = new byte[514];
        while ((n = in_tor.read(rcv)) >= 0) {
            baos.write(rcv, 0, n);
        }

        out.write(baos.toByteArray(), 0, baos.toByteArray().length);
        out.flush();

        socket.close();
        System.out.println(tor_sock.isClosed() + new String(baos.toByteArray()));
    }

    private static Socket socksv4SendRequest(String remote_host, int remote_port, String tor_host, int tor_port) {
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

            if (version != VERSION_RESP || resp != RESP_GRANTED) {
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
        } catch (InterruptedException e) {
        }
        return false;
    }
}
