package com.afonsovilalonga.Common.Initialization.ProxyStreamingHanshake;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.net.ssl.SSLSocket;


import com.afonsovilalonga.Common.Utils.Utilities;
import com.google.common.base.Supplier;

public class Initialization {
    private static final int RETRIES = 35;
    private static final int SLEEP_TIME = 1000;

    public static final byte ACCEPTED_REQ = 0x00;

    public static final byte ACK_SUCC = 0x00;
    public static final byte ACK_FAILED = 0x0F;

    public static boolean startHandshake(String host, int port){
        try {
            SSLSocket socket = reTry(() -> connectToBridge(host, port));
            
            OutputStream out_sock = socket.getOutputStream();
            DataInputStream in_sock = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            out_sock.write(ACCEPTED_REQ);
            out_sock.flush();

            byte ack = in_sock.readByte();

            socket.close();

            if(ack == ACK_SUCC)
                return true;
            
               
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return false;
    }

    public static byte serverHandshake(Socket socket){
        try {
           
            DataInputStream in_sock = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            byte req = in_sock.readByte();

            return req; 
        } catch (IOException e) {}
        return 0x0F;
    }

    public static void sendAccept(Socket socket){
        try{
            DataOutputStream out_sock = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out_sock.writeByte(ACCEPTED_REQ);
            out_sock.flush();
        } catch(IOException e) {}
    }

    public static void sendFalied(Socket socket){
        try{
            DataOutputStream out_sock = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out_sock.writeByte(ACK_FAILED);
            out_sock.flush();
        } catch(IOException e) {}
    }

    private static SSLSocket connectToBridge(String host, int port) {
        try {
            SSLSocket socket = Utilities.createSSLSocket(host, port);
            return socket;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static <T> SSLSocket reTry(Supplier<SSLSocket> func) {
        for (int i = 0; i < RETRIES; i++) {
            SSLSocket result = func.get();

            if (result != null)
                return result;

            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e1) {
            }
        }
        return null;
    }

}
