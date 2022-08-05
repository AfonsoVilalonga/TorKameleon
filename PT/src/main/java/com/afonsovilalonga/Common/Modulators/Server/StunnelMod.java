package com.afonsovilalonga.Common.Modulators.Server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.afonsovilalonga.Common.Modulators.ModulatorServerInterface;
import com.afonsovilalonga.Common.Utils.Config;


public class StunnelMod implements ModulatorServerInterface{
    
    private Socket bridge_conn; 
    private Socket tor_socket;

    public StunnelMod(Socket tor_socket, Socket conn){
        this.tor_socket = tor_socket;
        this.bridge_conn = conn;
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
                        int i = 0;
                        while( (i = in_Tor.read(send)) != -1){
                            out_stunnel.write(send, 0, i);
                            out_stunnel.flush();	
                        }    
                    } catch (Exception e) {}
                }
            };
            thread_sender.start();

            Thread thread_receiver = new Thread(){
                public void run(){
                    try {
                        int i = 0;
                        while((i = in_stunnel.read(recv)) != -1){
                            out_Tor.write(recv, 0, i);
                            out_Tor.flush();
                        }
                    } catch (Exception e) {}
                }
            };
            thread_receiver.start();
        } catch (IOException e) {}
    }

    @Override
    public void shutdown(){
        try {
            this.bridge_conn.close();
            this.tor_socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
