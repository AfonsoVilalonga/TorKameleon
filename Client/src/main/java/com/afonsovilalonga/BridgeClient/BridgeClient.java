package com.afonsovilalonga.BridgeClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;


public class BridgeClient {

    public static final byte VERSION_4 = 0x04;
    public static final byte VERSION_RESP = 0x00;
    public static final byte RESP_GRANTED = 0x5a;
    public static final byte TCP_STREAM = 0x01;
    
    public BridgeClient(){
        try {
            while(!tor_init(1000));
            torRequest();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void torRequest() throws IOException {
        String tor_host = "localhost";
        int tor_port = 9050;
        int tor_buffer_size = 512;

        Socket clientSocket = null;
        while(clientSocket == null)
            clientSocket = socksv4SendRequest("192.99.168.235", 10004, tor_host, tor_port);
                
        OutputStream out = clientSocket.getOutputStream();
        InputStream in = clientSocket.getInputStream();

        byte[] message = new byte[tor_buffer_size];
        byte[] rcv = new byte[tor_buffer_size];

        while(true){
            out.write(message);
            out.flush();
            int i = 0;
            while(i < 4*1024){
                int n = in.read(rcv);
                i += n;
            }
            System.out.print(" " + i);
            System.out.println();
        }    
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