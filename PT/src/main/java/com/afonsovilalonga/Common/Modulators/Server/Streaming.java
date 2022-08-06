package com.afonsovilalonga.Common.Modulators.Server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.java_websocket.WebSocket;

import com.afonsovilalonga.Common.Modulators.ModulatorServerInterface;
import com.afonsovilalonga.Common.Modulators.WebSocketWrapperPT;
import com.afonsovilalonga.Common.Utils.Config;

public class Streaming implements ModulatorServerInterface{

    private WebSocket bridge_conn;
    private PipedInputStream pin;
    private PipedOutputStream pout;
    private Socket tor_socket;

    public Streaming(Socket tor_socket, WebSocket bridge_conn, PipedInputStream pin, PipedOutputStream pout) {
        this.tor_socket = tor_socket;
        this.bridge_conn = bridge_conn;
        this.pin = pin;
        this.pout = pout;
    }

    @Override
    public void run() {
        Config config = Config.getInstance();
        
        try {
            ExecutorService executor = Executors.newFixedThreadPool(2);

            DataInputStream in_Tor = new DataInputStream(new BufferedInputStream(tor_socket.getInputStream()));
            DataOutputStream out_tor = new DataOutputStream(new BufferedOutputStream(tor_socket.getOutputStream()));

            byte[] send = new byte[config.getPTBufferSize()];
            byte[] recv = new byte[config.getPTBufferSize()];

            
            executor.execute(() -> {
                try {
                    int i = 0;
                    while (true) {
                        i = in_Tor.read(send);
                        WebSocketWrapperPT.send(Arrays.copyOfRange(send, 0, i), bridge_conn);         
                    }
                } catch (Exception e) {
                }
            });

                  
            executor.execute(() -> {
                try {
                    int i = 0;
                    while (true) {
                        i = pin.read(recv);
                        out_tor.write(recv, 0, i);
                        out_tor.flush();            
                    }
                } catch (Exception e) {
                }
            });
        } catch (IOException e) {}
    }

    @Override
    public void shutdown() {
        try {
            this.bridge_conn.close();
            this.tor_socket.close();
            pout.close();
            pin.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
