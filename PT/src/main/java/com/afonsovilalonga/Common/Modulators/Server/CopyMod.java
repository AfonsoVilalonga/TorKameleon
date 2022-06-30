package com.afonsovilalonga.Common.Modulators.Server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import com.afonsovilalonga.Common.Modulators.ModulatorServerInterface;
import com.afonsovilalonga.Common.Modulators.ModulatorTop;
import com.afonsovilalonga.Common.Utils.Config;


public class CopyMod extends ModulatorTop implements ModulatorServerInterface {

    private Socket bridge_conn;
    private String id;

    public CopyMod(Socket tor_socket, Socket socket, String id) {
        super(tor_socket);
        this.bridge_conn = socket;
        this.id = id;
    }

    @Override
    public void run() {
        Config config = Config.getInstance();

        Socket tor_socket = gettor_socket();
        ExecutorService executor = getExecutor();

        try {
            DataInputStream in_pt = new DataInputStream(new BufferedInputStream(bridge_conn.getInputStream()));
            DataOutputStream out_pt = new DataOutputStream(new BufferedOutputStream(bridge_conn.getOutputStream()));

            DataInputStream in_Tor = new DataInputStream(new BufferedInputStream(tor_socket.getInputStream()));
            DataOutputStream out_Tor = new DataOutputStream(new BufferedOutputStream(tor_socket.getOutputStream()));

            byte[] recv = new byte[config.getBufferSize()];
            byte[] send = new byte[config.getBufferSize()];

            executor.execute(() -> {
                try {
                    int i = 0;
                    while ((i = in_Tor.read(send)) != -1) {
                        out_pt.write(send, 0, i);
                        out_pt.flush();
                    }
                } catch (Exception e) {
                    notifyObserver(id);
                }
            });

            executor.execute(() -> {
                try {
                    int i = 0;
                    while ((i = in_pt.read(recv)) != -1) {
                        out_Tor.write(recv, 0, i);
                        out_Tor.flush();
                    }
                } catch (Exception e) {
                    notifyObserver(id);
                }
            });
        } catch (IOException e) {
            notifyObserver(id);
        }
    }

    @Override
    public void shutdown() {
        try {
            serviceShutdow();
            this.bridge_conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
