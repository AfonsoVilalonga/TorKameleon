package com.afonsovilalonga.Common.Modulators.Client.Streaming;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.xml.bind.DatatypeConverter;


public class WebSocketWrapperClient extends WebSocketServer
{
    
    private static int TCP_PORT = 4444;
    private WebSocket webconn;
    private Socket tor_sock;

    public WebSocketWrapperClient(Socket tor_sock) {
        super(new InetSocketAddress(TCP_PORT));
        super.start();
        this.webconn = null;
        this.tor_sock = tor_sock;
        
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if(webconn == null)
            webconn = conn;
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println(" erro " + code + conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            DataOutputStream out_tor = new DataOutputStream(new BufferedOutputStream(tor_sock.getOutputStream()));

            byte[] recv = decodeBase64(message);
            for(byte i: recv){
                System.out.print(i + " ");
            }
            out_tor.write(recv);
            out_tor.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println(" erro2 " + conn);
        ex.printStackTrace();
    }   

    public void stop(){
        try {
            super.stop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void send(byte[] message){
        webconn.send(encodeBase64(message));
    }

    public String encodeBase64(byte[] bytes){
        String result = DatatypeConverter.printBase64Binary(bytes);
        return result;
    }

    public byte[] decodeBase64(String base64){
        return DatatypeConverter.parseBase64Binary(base64);
    }
}
