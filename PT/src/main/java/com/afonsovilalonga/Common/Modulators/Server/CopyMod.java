package com.afonsovilalonga.Common.Modulators.Server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import com.afonsovilalonga.Common.Modulators.ModulatorTop;


public class CopyMod extends ModulatorTop implements ModulatorServerInterface {

    private Socket bridge_conn;

    public CopyMod(Socket tor_socket, Socket socket) {
        super(tor_socket);
        this.bridge_conn = socket;
    }


    @Override
    public void shutdown() {
        try {
            super.serviceShutdow();
            this.bridge_conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Socket tor_socket = super.gettor_socket();
        ExecutorService executor = super.getExecutor();

        try {
            DataInputStream in_pt = new DataInputStream(new BufferedInputStream(bridge_conn.getInputStream()));
            DataOutputStream out_pt = new DataOutputStream(new BufferedOutputStream(bridge_conn.getOutputStream()));

            DataInputStream in_Tor = new DataInputStream(new BufferedInputStream(tor_socket.getInputStream()));
            DataOutputStream out_Tor = new DataOutputStream(new BufferedOutputStream(tor_socket.getOutputStream()));

            byte[] recv = new byte[20000];
            byte[] send = new byte[20000];

            executor.execute(() -> {
                try {
                    int i = 1;
                    while ((i = in_Tor.read(send)) != -1 && !super.getShutdown()) {
                        out_pt.write(send, 0, i);
                        out_pt.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            executor.execute(() -> {
                try {
                    int i = 1;
                    while ((i = in_pt.read(recv)) != -1 && !super.getShutdown()) {
                        out_Tor.write(recv, 0, i);
                        out_Tor.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
