package com.afonsovilalonga.Common.Modulators.Server.Streaming;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.xml.bind.DatatypeConverter;


public class WebSocketWrapperServer extends WebSocketServer
{
    
    private static int TCP_PORT = 4444; 
    private Map<Integer, Socket> tor_socks;

    private CountDownLatch cl;
    private WebSocket lastConn;

    public WebSocketWrapperServer() {
        super(new InetSocketAddress(TCP_PORT));
        super.start();
        tor_socks = new HashMap<>();

        cl = null;
        lastConn = null;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        lastConn = conn;
        cl.countDown();
    }


    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {}

    @Override
    public void onMessage(WebSocket conn, String message) {
        Socket tor = tor_socks.get(conn.hashCode());

        try {
            DataOutputStream out_tor = new DataOutputStream(new BufferedOutputStream(tor.getOutputStream()));
            byte[] recv = decodeBase64(message);
            out_tor.write(recv);
            out_tor.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {}

    public void setMutexAndWaitConn(CountDownLatch cl){
        this.cl = cl;
    }

    public void setTorConnToConn(Socket tor_sock, WebSocket conn){
        tor_socks.put(conn.hashCode(), tor_sock);
    }

    public WebSocket getLaSocket(){
        return this.lastConn;
    }

    public byte[] decodeBase64(String base64){
        return DatatypeConverter.parseBase64Binary(base64);
    }

    public static String encodeBase64(byte[] bytes){
        String result = DatatypeConverter.printBase64Binary(bytes);
        return result;
    }

    public static void send(byte[] message, WebSocket conn){
        conn.send(encodeBase64(message));
    }

}
