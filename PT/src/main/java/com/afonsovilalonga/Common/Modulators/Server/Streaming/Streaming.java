package com.afonsovilalonga.Common.Modulators.Server.Streaming;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import org.java_websocket.WebSocket;

import com.afonsovilalonga.Common.Modulators.ModulatorTop;
import com.afonsovilalonga.Common.Modulators.Server.ModulatorServerInterface;

public class Streaming extends ModulatorTop implements ModulatorServerInterface{

    private WebSocket bridge_conn;

    public Streaming(Socket tor_socket, WebSocket bridge_conn) {
        super(tor_socket);
        this.bridge_conn = bridge_conn;
    }

    @Override
    public void run() {
        Socket tor_socket = super.gettor_socket();
        ExecutorService executor = super.getExecutor();
        
        try {
            DataInputStream in_Tor = new DataInputStream(new BufferedInputStream(tor_socket.getInputStream()));

            byte[] send = new byte[4096];

            executor.execute(() -> {
                try {
                    int i = 0;
                    while ((i = in_Tor.read(send)) != -1 && !super.getShutdown()) {
                        WebSocketWrapperServer.send(Arrays.copyOfRange(send, 0, i), bridge_conn);         
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        super.serviceShutdow();
        if(this.bridge_conn != null)
            this.bridge_conn.close();
    }
  
}
