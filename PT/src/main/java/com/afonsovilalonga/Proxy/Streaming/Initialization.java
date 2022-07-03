package com.afonsovilalonga.Proxy.Streaming;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


public class Initialization {
    
    private static final byte ACK_SUCC = 0x00;
    private static final byte ACK_FAILED = 0x0F;

    public static boolean startHandshake(String host, int port){
        try {
            //SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            //SSLSocket socket  = (SSLSocket) factory.createSocket(host, port);
            Socket socket = new Socket(host, port);

            DataOutputStream out_sock = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            DataInputStream in_sock = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            out_sock.writeByte(0x00);
            out_sock.flush();

            byte ack = in_sock.readByte();

            socket.close();

            if(ack == ACK_SUCC)
                return true;
            
               
        } catch (IOException e) {}
        
        return false;
    }

    public static boolean serverHandshake(Socket socket){
        try {
            DataOutputStream out_sock = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            DataInputStream in_sock = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            byte req = in_sock.readByte();

        
        } catch (IOException e) {}

        return false;
    }

}
