package com.afonsovilalonga.Common.Modulators;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.xml.bind.DatatypeConverter;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.afonsovilalonga.Common.Utils.Config;

public class WebSocketWrapper extends WebSocketServer{

    private Map<Integer, PipedOutputStream> tor_socks;

    private CountDownLatch cl;
    private WebSocket lastConn;
    private boolean isBridgeOrProxy;

    public WebSocketWrapper(boolean isBridgeOrProxy) {
        super(new InetSocketAddress(Config.getInstance().getWebsocketPort()));

        start();
        tor_socks = new HashMap<>();
        this.isBridgeOrProxy = isBridgeOrProxy;

        cl = null;
        lastConn = null;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        lastConn = conn;
        cl.countDown();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if(!isBridgeOrProxy)
            onCloseOrError(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        PipedOutputStream tor = tor_socks.get(conn.hashCode());

        try {
            byte[] recv = decodeBase64(message);
            tor.write(recv);
            tor.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        onCloseOrError(conn);
    }


    public void setMutexAndWaitConn(CountDownLatch cl){
        this.cl = cl;
    }

    public void setTorConnToConn(PipedOutputStream tor_sock, WebSocket conn){
        tor_socks.put(conn.hashCode(), tor_sock);
    }

    public WebSocket getLaSocket(){
        return this.lastConn;
    }

    public static void send(byte[] message, WebSocket conn){
        conn.send(encodeBase64(message));
    }

    public byte[] decodeBase64(String base64){
        return DatatypeConverter.parseBase64Binary(base64);
    }

    public static String encodeBase64(byte[] bytes){
        String result = DatatypeConverter.printBase64Binary(bytes);
        return result;
    }

    private void onCloseOrError(WebSocket conn){
        PipedOutputStream tor = tor_socks.get(conn.hashCode());
        tor_socks.remove(conn.hashCode());
        try {
            tor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
