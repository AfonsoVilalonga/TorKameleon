package com.afonsovilalonga.Common.Modulators.Server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.afonsovilalonga.Common.Modulators.ModulatorServerInterface;
import com.afonsovilalonga.Common.Utils.Config;

public class StunnelMod implements ModulatorServerInterface {

    private Socket bridge_conn;
    private Socket tor_socket;

    public StunnelMod(Socket tor_socket, Socket conn) {
        this.tor_socket = tor_socket;
        this.bridge_conn = conn;
    }

    @Override
    public void run() {
        Config config = Config.getInstance();

        try {
            ExecutorService executor = Executors.newFixedThreadPool(2);

            DataInputStream in_Tor = new DataInputStream(new BufferedInputStream(tor_socket.getInputStream()));
            DataOutputStream out_Tor = new DataOutputStream(new BufferedOutputStream(tor_socket.getOutputStream()));

            DataInputStream in_stunnel = new DataInputStream(new BufferedInputStream(bridge_conn.getInputStream()));
            DataOutputStream out_stunnel = new DataOutputStream(
                    new BufferedOutputStream(bridge_conn.getOutputStream()));

            byte[] recv = new byte[config.getPTBufferSize()];
            byte[] send = new byte[config.getPTBufferSize()];

            executor.execute(() -> {
                try {
                    int i = 0;
                    while (true) {
                        i = in_Tor.read(send);
                        out_stunnel.write(send, 0, i);
                        out_stunnel.flush();
                    }
                } catch (Exception e) {
                }
            });

            executor.execute(() -> {
                try {
                    int i = 0;
                    while (true) {
                        i = in_stunnel.read(recv);
                        out_Tor.write(recv, 0, i);
                        out_Tor.flush();
                    }
                } catch (Exception e) {
                }
            });
        } catch (IOException e) {
        }
    }

    @Override
    public void shutdown() {
        try {
            this.bridge_conn.close();
            this.tor_socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
