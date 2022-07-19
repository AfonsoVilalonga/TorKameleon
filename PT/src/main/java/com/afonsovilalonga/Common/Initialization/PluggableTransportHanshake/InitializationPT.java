package com.afonsovilalonga.Common.Initialization.PluggableTransportHanshake;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

import com.afonsovilalonga.Common.Initialization.Exceptions.BridgeFailedException;

/**
 * Class for initialization and automation purposes
 */
public class InitializationPT {

    private static final int RETRIES = 10;

    private static final byte ACK_SUCC = 0x00;
    private static final byte ACK_REF = 0x01;
    private static final byte MOD_COPY = 0x00;
    private static final byte MOD_STUNNEL = 0x01;
    private static final byte MOD_STREAMING = 0x02;

    public static boolean tor_init(long sleep) {
        try (Socket tor = new Socket("127.0.0.1", 9051)) {
            DataOutputStream out_tor = new DataOutputStream(new BufferedOutputStream(tor.getOutputStream()));
            DataInputStream in_tor = new DataInputStream(new BufferedInputStream(tor.getInputStream()));

            boolean done = false;
            int i = 0;
            byte[] recv = new byte[2048];

            while (i < RETRIES && !done) {
                out_tor.write("AUTHENTICATE\r\n".getBytes());
                out_tor.flush();

                in_tor.read(recv);

                out_tor.writeBytes("GETINFO status/bootstrap-phase\r\n");
                out_tor.flush();

                recv = new byte[2048];
                in_tor.read(recv);

                String progress = new String(recv);

                if (progress.contains("100"))
                    done = true;
                else
                    Thread.sleep(sleep);

                i++;
            }
            return done;
        } catch (IOException e) {
        } catch (InterruptedException e) {}
        return false;
    }

    public static void bridge_protocol_client_side(Socket bridge_conn, String modulation) throws BridgeFailedException {
        try {
            DataOutputStream out_bridge = new DataOutputStream(new BufferedOutputStream(bridge_conn.getOutputStream()));
            DataInputStream in_bridge = new DataInputStream(new BufferedInputStream(bridge_conn.getInputStream()));

            client_side_logic(out_bridge, in_bridge, modulation);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void bridge_protocol_client_side(SSLSocket bridge_conn, String modulation)
            throws BridgeFailedException {
        try {
            DataOutputStream out_bridge = new DataOutputStream(new BufferedOutputStream(bridge_conn.getOutputStream()));
            DataInputStream in_bridge = new DataInputStream(new BufferedInputStream(bridge_conn.getInputStream()));

            client_side_logic(out_bridge, in_bridge, modulation);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte bridge_protocol_server_side(Socket bridge_conn) {
        try {
            DataInputStream in_bridge = new DataInputStream(new BufferedInputStream(bridge_conn.getInputStream()));
            byte mod = in_bridge.readByte();

            return mod;

        } catch (IOException e) {
            e.printStackTrace();
            try {
                bridge_conn.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return 0x0F;
    }

    public static void bridge_protocol_server_side_send_ack(Socket bridge_conn, byte mod) {
        try {
            DataOutputStream out_bridge = new DataOutputStream(new BufferedOutputStream(bridge_conn.getOutputStream()));
            if (mod != MOD_COPY && mod != MOD_STUNNEL && mod != MOD_STREAMING)
                out_bridge.writeByte(ACK_REF);

            else
                out_bridge.writeByte(ACK_SUCC);

            out_bridge.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String mapper(byte mod) {
        switch (mod) {
            case MOD_COPY:
                return "copy";
            case MOD_STUNNEL:
                return "stunnel";
            case MOD_STREAMING:
                return "streaming";
            default:
                return null;
        }
    }

    public static void finishedInit() {
        System.setProperty("tir.done", "true");
    }

    private static void client_side_logic(DataOutputStream out_bridge, DataInputStream in_bridge, String modulation)
            throws BridgeFailedException, IOException {

        if (modulation.equals("copy"))
            out_bridge.writeByte(MOD_COPY);

        if (modulation.equals("stunnel"))
            out_bridge.writeByte(MOD_STUNNEL);

        if (modulation.equals("streaming"))
            out_bridge.writeByte(MOD_STREAMING);
            
            
        out_bridge.flush();

        byte resp = in_bridge.readByte();

        if (resp != ACK_SUCC)
            throw new BridgeFailedException("Bridge invalid response.");
    }
}
