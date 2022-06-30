package com.afonsovilalonga.PluggableTransport.Client;

import java.io.IOException;
import java.net.ServerSocket;

import com.afonsovilalonga.Common.Modulators.ModulatorClientInterface;
import com.afonsovilalonga.Common.Modulators.WebSocketWrapper;
import com.afonsovilalonga.Common.Modulators.Client.CopyMod;
import com.afonsovilalonga.Common.Modulators.Client.Streaming;
import com.afonsovilalonga.Common.ObserversCleanup.Monitor;
import com.afonsovilalonga.Common.ObserversCleanup.ObserverClient;
import com.afonsovilalonga.Common.Modulators.Client.StunnelMod;
import com.afonsovilalonga.Common.Socks.SocksProtocol;
import com.afonsovilalonga.Common.Socks.Exceptions.SocksException;
import com.afonsovilalonga.Common.Utils.Config;

public class PT implements ObserverClient{
    private Config config;
    private ModulatorClientInterface modulator;

    private SocksProtocol socks_protocol;
    
    private ServerSocket tor_server;
    private WebSocketWrapper web_socket_server;

    public PT(WebSocketWrapper web_socket_server) {
        this.config = Config.getInstance();
        this.web_socket_server = web_socket_server;
        this.modulator = null;
        this.socks_protocol = null;
     
        Monitor.registerObserver(this);

        try {
            this.tor_server = new ServerSocket(config.getPt_client_port());
        } catch (IOException e) {}
    }

    public void run() {
        ModulatorClientInterface copyloop = null;
        String mod = config.getModulation();
        boolean result = true;

        this.socks_protocol = new SocksProtocol();
        
        try {
            socks_protocol.acceptConns(tor_server);
        } catch (SocksException e) {
            System.exit(-1);
        }

        if (mod.equals("copy")) {
            copyloop = new CopyMod(socks_protocol.getSocks());
        } else if (mod.equals("stunnel")) {
            copyloop = new StunnelMod(socks_protocol.getSocks());
        } else if (mod.equals("streaming")) {
            copyloop = new Streaming(socks_protocol.getSocks(), web_socket_server);
        }

        result = copyloop.initialize(socks_protocol.getReq().getAddr(), socks_protocol.getReq().getPort(), socks_protocol, mod);
       
        if (!result)
            System.exit(-1);

        copyloop.run();
        modulator = copyloop;
    }

    public void shutdown() {
        if(modulator != null)
            modulator.shutdown();
        else    
            socks_protocol.close();
        
        try {
            tor_server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStateChange() {
        shutdown();
    }
}
