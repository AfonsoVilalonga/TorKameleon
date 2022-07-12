package com.afonsovilalonga.Common.Modulators.Client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.SSLSocket;

import com.afonsovilalonga.Common.Initialization.Exceptions.BridgeFailedException;
import com.afonsovilalonga.Common.Initialization.PluggableTransportHanshake.InitializationPT;
import com.afonsovilalonga.Common.Modulators.ModulatorClientInterface;
import com.afonsovilalonga.Common.Modulators.ModulatorTop;
import com.afonsovilalonga.Common.Socks.SocksProtocol;
import com.afonsovilalonga.Common.Utils.Config;
import com.afonsovilalonga.Common.Utils.Utilities;

public class StunnelMod extends ModulatorTop implements ModulatorClientInterface{
    
    private SSLSocket bridge_conn;

    public StunnelMod(Socket tor_socket){
        super(tor_socket);
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
        Socket tor_socket = gettor_socket();
        ExecutorService executor = getExecutor();

		try {

            DataInputStream in_Tor = new DataInputStream(new BufferedInputStream(tor_socket.getInputStream()));
            DataOutputStream out_Tor = new DataOutputStream(new BufferedOutputStream(tor_socket.getOutputStream()));
			
		    DataInputStream in_stunnel = new DataInputStream(new BufferedInputStream(bridge_conn.getInputStream()));
		    DataOutputStream out_stunnel = new DataOutputStream(new BufferedOutputStream(bridge_conn.getOutputStream()));
	        

            byte[] recv = new byte[config.getPTBufferSize()];
            byte[] send = new byte[config.getPTBufferSize()];
		          
            executor.execute(() -> {
                try {
                    int i = 1;
                    while( (i = in_Tor.read(send)) != -1){
                    	out_stunnel.write(send, 0, i);
                        out_stunnel.flush();	
                    }    
                } catch (Exception e) {}
                System.exit(-1);
            });

            executor.execute(() -> {
                try{
                    int i = 1;
                    while((i = in_stunnel.read(recv)) != -1){
                    	out_Tor.write(recv, 0, i);
                        out_Tor.flush();
                    }
                } catch (Exception e){}
                
                System.exit(-1);
            });    
        } catch (IOException e) {
            System.exit(-1);
        }
    }

    @Override
    public void shutdown(){
        try {
            serviceShutdow();
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
}
