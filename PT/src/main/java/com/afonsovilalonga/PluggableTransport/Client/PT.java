package com.afonsovilalonga.PluggableTransport.Client;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import com.afonsovilalonga.Common.Initialization.InitializationPT;
import com.afonsovilalonga.Common.Modulators.ModulatorClientInterface;
import com.afonsovilalonga.Common.Modulators.Client.CopyMod;
import com.afonsovilalonga.Common.Modulators.Client.Streaming.Streaming;
import com.afonsovilalonga.Common.ObserversCleanup.ObserverClient;
import com.afonsovilalonga.Common.Modulators.Client.StunnelMod;
import com.afonsovilalonga.Common.Socks.SocksProtocol;
import com.afonsovilalonga.Common.Socks.Exceptions.SocksException;
import com.afonsovilalonga.Common.Utils.Config;

public class PT implements ObserverClient{
    private Config config;
    private ModulatorClientInterface modulator;

    private SocksProtocol socks_protocol;
    private Process client_process;
    private ServerSocket tor_server;

    public PT() {
        config = Config.getInstance();
        modulator = null;
        socks_protocol = null;

        try {
            this.tor_server = new ServerSocket(config.getPt_client_port());
            
            ProcessBuilder pb = new ProcessBuilder("node", config.getWebRTCLocation() + "/Client/index.js", config.getClientPortStreaming());
            pb.directory(new File(config.getWebRTCLocation() + "/Client"));
            client_process = pb.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        ModulatorClientInterface copyloop = null;
        String mod = config.getModulation();

        socks_protocol = new SocksProtocol();
        boolean result = true;

        try {
            socks_protocol.acceptConns(tor_server);
        } catch (SocksException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        if (mod.equals("copy")) {
            copyloop = new CopyMod(socks_protocol.getSocks());
        } else if (mod.equals("stunnel")) {
            copyloop = new StunnelMod(socks_protocol.getSocks());
        } else if (mod.equals("streaming")) {
            copyloop = new Streaming(socks_protocol.getSocks());
        }

        result = copyloop.initialize(socks_protocol.getReq().getAddr(), socks_protocol.getReq().getPort(), socks_protocol, mod);
    
        if (result) {
            InitializationPT.finishedInit();

            copyloop.run();
            modulator = copyloop;
        }

    }

    public void shutdown() {
        if(modulator != null)
            modulator.shutdown();
        else    
            socks_protocol.close();
        
        try {
            client_process.destroy();
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
