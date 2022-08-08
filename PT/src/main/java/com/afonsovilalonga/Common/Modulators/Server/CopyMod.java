package com.afonsovilalonga.Common.Modulators.Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.afonsovilalonga.Common.Modulators.ModulatorServerInterface;
import com.afonsovilalonga.Common.Utils.Config;


public class CopyMod implements ModulatorServerInterface {

    private Socket tor_socket; 
    private Socket bridge_conn;

    public CopyMod(Socket tor_socket, Socket socket) {
        this.tor_socket = tor_socket;
        this.bridge_conn = socket;
    }

    @Override
    public void run() {
        Config config = Config.getInstance();

        try {
            ExecutorService executor = Executors.newFixedThreadPool(2);

            InputStream in_pt = bridge_conn.getInputStream();
            OutputStream out_pt = bridge_conn.getOutputStream();

            InputStream in_Tor = tor_socket.getInputStream();
            OutputStream out_Tor = tor_socket.getOutputStream();

            byte[] recv = new byte[config.getPTBufferSize()];
            byte[] send = new byte[config.getPTBufferSize()];

            executor.execute(() -> {
                try {
                    int i = 0;
                    while (true) {
                        i = in_Tor.read(send);
                        out_pt.write(send, 0, i);
                        out_pt.flush();
                    }
                } catch (Exception e) {
                }
            });

            executor.execute(() -> {
                try {
                    int i = 0;
                    while (true) {
                        i = in_pt.read(recv);
                        out_Tor.write(recv, 0, i);
                        out_Tor.flush();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
