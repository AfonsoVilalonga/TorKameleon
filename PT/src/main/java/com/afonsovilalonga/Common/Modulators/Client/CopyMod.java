package com.afonsovilalonga.Common.Modulators.Client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.afonsovilalonga.Common.Initialization.Exceptions.BridgeFailedException;
import com.afonsovilalonga.Common.Initialization.PluggableTransportHanshake.InitializationPT;
import com.afonsovilalonga.Common.Modulators.ModulatorClientInterface;
import com.afonsovilalonga.Common.Socks.SocksProtocol;
import com.afonsovilalonga.Common.Utils.Config;
import com.google.common.base.Supplier;

public class CopyMod implements ModulatorClientInterface {
    private static final int RETRIES = 35;
    private static final int SLEEP_TIME = 1000;

    private Socket bridge_conn;
    private Socket tor_socket;

    public CopyMod(Socket tor_socket) {
        this.tor_socket = tor_socket;
    }

    @Override
    public boolean initialize(String host, int port, SocksProtocol s, String mod) {
        boolean result = reTry(() -> connectToBridge(host, port));
        if (result) {
            try {
                InitializationPT.bridge_protocol_client_side(bridge_conn, mod);
            } catch (BridgeFailedException e) {
                e.printStackTrace();
                try {
                    bridge_conn.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return false;
            }
            s.sendSocksResponseAccepted();
        } else {
            s.sendSocksResponseRejected(SocksProtocol.CONN_NOT_ALLOWED);
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        Config config = Config.getInstance();

        try {
            ExecutorService executor = Executors.newFixedThreadPool(2);

            DataInputStream in_pt = new DataInputStream(new BufferedInputStream(bridge_conn.getInputStream()));
            DataOutputStream out_pt = new DataOutputStream(new BufferedOutputStream(bridge_conn.getOutputStream()));

            DataInputStream in_Tor = new DataInputStream(new BufferedInputStream(tor_socket.getInputStream()));
            DataOutputStream out_Tor = new DataOutputStream(new BufferedOutputStream(tor_socket.getOutputStream()));

            byte[] recv = new byte[config.getPTBufferSize()];
            byte[] send = new byte[config.getPTBufferSize()];

            executor.execute(() -> {
                try {
                    int i = 1;
                    while (true) {
                        i = in_Tor.read(send);
                        out_pt.write(send, 0, i);
                        out_pt.flush();
                    }
                } catch (Exception e) {
                }
                System.exit(-1);
            });

            executor.execute(() -> {
                try {
                    int i = 1;
                    while (true) {
                        i = in_pt.read(recv);
                        out_Tor.write(recv, 0, i);
                        out_Tor.flush();
                    }
                } catch (Exception e) {}
                System.exit(-1);
            });
        } catch (IOException e) {
            System.out.println("Connection to Tor Bridge failed.");
            System.exit(-1);
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

    private boolean connectToBridge(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            this.bridge_conn = socket;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private <T> boolean reTry(Supplier<Boolean> func) {
        for (int i = 0; i < RETRIES; i++) {
            boolean result = func.get();

            if (result)
                return result;

            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e1) {
            }
        }
        return false;
    }
}
