package com.afonsovilalonga.Common.Modulators.Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
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

            InputStream in_Tor = tor_socket.getInputStream();
            OutputStream out_Tor = tor_socket.getOutputStream();

            InputStream in_stunnel = bridge_conn.getInputStream();
            OutputStream out_stunnel = bridge_conn.getOutputStream();

            byte[] recv = new byte[config.getPTBufferSize()];
            byte[] send = new byte[config.getPTBufferSize()];

            CountDownLatch latch = new CountDownLatch(2);

            executor.execute(() -> {
                try {
                    int i = 0;
                    while (true) {
                        i = in_Tor.read(send);
                        out_stunnel.write(send, 0, i);
                        out_stunnel.flush();
                    }
                   
                } catch (Exception e) {}
                latch.countDown();
            });

            executor.execute(() -> {
                try {
                    int i = 0;
                    while (true) {
                        i = in_stunnel.read(recv);
                        out_Tor.write(recv, 0, i);
                        out_Tor.flush();
                    }
                } catch (Exception e) {}
                latch.countDown();
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
