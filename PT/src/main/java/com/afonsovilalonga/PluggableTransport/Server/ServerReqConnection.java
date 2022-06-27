package com.afonsovilalonga.PluggableTransport.Server;

import java.io.IOException;
import java.net.Socket;

import com.afonsovilalonga.Common.Modulators.Server.ModulatorServerInterface;

public class ServerReqConnection {

    private Socket tor_sock;
    private ModulatorServerInterface copyloop;

    public ServerReqConnection(Socket tor_sock, ModulatorServerInterface copyloop){
        this.tor_sock = tor_sock;
        this.copyloop = copyloop;
    }

    public void shutdown(){
        copyloop.shutdown();
        try {
            tor_sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }       
    }
   
}
