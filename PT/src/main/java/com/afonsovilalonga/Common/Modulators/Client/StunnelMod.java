package com.afonsovilalonga.Common.Modulators.Client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

import com.afonsovilalonga.Common.Initialization.Exceptions.BridgeFailedException;
import com.afonsovilalonga.Common.Initialization.PluggableTransportHanshake.InitializationPT;
import com.afonsovilalonga.Common.Modulators.ModulatorClientInterface;
import com.afonsovilalonga.Common.Socks.SocksProtocol;
import com.afonsovilalonga.Common.Utils.Config;
import com.afonsovilalonga.Common.Utils.Utilities;
import com.google.common.base.Supplier;

public class StunnelMod implements ModulatorClientInterface{
    private static final int RETRIES = 35;
    private static final int SLEEP_TIME = 1000;
    
    private SSLSocket bridge_conn;
    private Socket tor_socket;

    public StunnelMod(Socket tor_socket){
        this.tor_socket = tor_socket;
    }

    @Override
    public boolean initialize(String host, int port, SocksProtocol s, String mod) {
        boolean result = reTry(() -> connectToBridge(host, port));
        if(result){
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
        }
        else{
            s.sendSocksResponseRejected(SocksProtocol.CONN_NOT_ALLOWED);
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        Config config = Config.getInstance();

		try {
            DataInputStream in_Tor = new DataInputStream(new BufferedInputStream(tor_socket.getInputStream()));
            DataOutputStream out_Tor = new DataOutputStream(new BufferedOutputStream(tor_socket.getOutputStream()));
			
		    DataInputStream in_stunnel = new DataInputStream(new BufferedInputStream(bridge_conn.getInputStream()));
		    DataOutputStream out_stunnel = new DataOutputStream(new BufferedOutputStream(bridge_conn.getOutputStream()));
	        

            byte[] recv = new byte[config.getPTBufferSize()];
            byte[] send = new byte[config.getPTBufferSize()];
		          
            Thread thread_sender = new Thread(){
                public void run(){
                    try {
                        int i = 1;
                        while( (i = in_Tor.read(send)) != -1){
                            out_stunnel.write(send, 0, i);
                            out_stunnel.flush();	
                        }    
                    } catch (Exception e) {}
                    System.exit(-1);
                }
            };
            thread_sender.start();

            Thread thread_receiver = new Thread(){
                public void run(){
                    try{
                        int i = 1;
                        while((i = in_stunnel.read(recv)) != -1){
                            out_Tor.write(recv, 0, i);
                            out_Tor.flush();
                        }
                    } catch (Exception e){}
                    System.exit(-1);
                }
            };
            thread_receiver.start();
        } catch (IOException e) {
            System.exit(-1);
        }
    }

    @Override
    public void shutdown(){
        try {
            this.tor_socket.close();
            this.bridge_conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean connectToBridge(String host, int port) {
        try {
            SSLSocket socket = Utilities.createSSLSocket(host, port);
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

            if(result)
                return result;
            
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e1) {}
        }
        return false;
    }
}
