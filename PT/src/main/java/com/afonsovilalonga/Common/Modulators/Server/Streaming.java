package com.afonsovilalonga.Common.Modulators.Server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import org.java_websocket.WebSocket;

import com.afonsovilalonga.Common.Modulators.ModulatorServerInterface;
import com.afonsovilalonga.Common.Modulators.ModulatorTop;
import com.afonsovilalonga.Common.Modulators.WebSocketWrapper;
import com.afonsovilalonga.Common.Utils.Config;

public class Streaming extends ModulatorTop implements ModulatorServerInterface{

    private WebSocket bridge_conn;
    private String id;
    private PipedInputStream pin;

    public Streaming(Socket tor_socket, WebSocket bridge_conn, String id, PipedInputStream pin) {
        super(tor_socket);
        this.bridge_conn = bridge_conn;
        this.id = id;
        this.pin = pin;
    }

    @Override
    public void run() {
        Config config = Config.getInstance();

        Socket tor_socket = gettor_socket();
        ExecutorService executor = getExecutor();
        
        try {
            DataInputStream in_Tor = new DataInputStream(new BufferedInputStream(tor_socket.getInputStream()));
            DataOutputStream out_tor = new DataOutputStream(new BufferedOutputStream(tor_socket.getOutputStream()));

            byte[] send = new byte[config.getBufferSize()];
            byte[] recv = new byte[config.getBufferSize()];

            executor.execute(() -> {
                try {
                    int i = 0;
                    while ((i = in_Tor.read(send)) != -1) {
                        WebSocketWrapper.send(Arrays.copyOfRange(send, 0, i), bridge_conn);         
                    }
                } catch (Exception e) {
                    notifyObserver(id);
                }
            });

            executor.execute(() -> {
                try {
                    int i = 0;
                    while ((i = pin.read(recv)) != -1) {
                        out_tor.write(recv, 0, i);
                        out_tor.flush();            
                    }
                } catch (Exception e) {
                    notifyObserver();
                }
            });
        } catch (IOException e) {
            notifyObserver(id);
        }
    }

    @Override
    public void shutdown() {
        serviceShutdow();
        if(this.bridge_conn != null)
            this.bridge_conn.close();
    }
  
}
