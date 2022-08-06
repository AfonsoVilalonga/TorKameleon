package com.afonsovilalonga.ChaffClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;


public class ChaffClient {

    public static final byte VERSION_4 = 0x04;
    public static final byte VERSION_RESP = 0x00;
    public static final byte RESP_GRANTED = 0x5a;
    public static final byte TCP_STREAM = 0x01;
    
    public ChaffClient(){
        try {
            torRequest();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void torRequest() throws IOException {
        String tor_host = "localhost";
        int tor_port = 9050;
        int tor_buffer_size = 512;

        Socket clientSocket = socksv4SendRequest("192.99.168.235", 10000, tor_host, tor_port);
                
        clientSocket.setReceiveBufferSize(tor_buffer_size);
        clientSocket.setSendBufferSize(tor_buffer_size);
        OutputStream out = clientSocket.getOutputStream();
        
        byte[] message = new byte[tor_buffer_size];
        while(true){
            out.write(message);
            out.flush();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
            e.printStackTrace();
        }

        return null;
    } 
}
