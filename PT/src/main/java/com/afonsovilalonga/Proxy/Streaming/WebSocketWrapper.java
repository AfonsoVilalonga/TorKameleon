package com.afonsovilalonga.Proxy.Streaming;

import java.net.UnknownHostException;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class WebSocketWrapper extends WebSocketServer{

    public WebSocketWrapper() throws UnknownHostException {
        super();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }
    
}
