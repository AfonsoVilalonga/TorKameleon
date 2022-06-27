package com.afonsovilalonga.PluggableTransport.Client;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;

import com.afonsovilalonga.Common.Initialization.InitializationPT;
import com.afonsovilalonga.Common.Modulators.Client.CopyMod;
import com.afonsovilalonga.Common.Modulators.Client.ModulatorClientInterface;
import com.afonsovilalonga.Common.Modulators.Client.Streaming.Streaming;
import com.afonsovilalonga.Common.Modulators.Client.StunnelMod;
import com.afonsovilalonga.Common.Socks.SocksProtocol;
import com.afonsovilalonga.Common.Socks.Exceptions.SocksException;
import com.afonsovilalonga.Common.Utils.Config;

public class PT {
    private Config config;
    private List<ModulatorClientInterface> objs;

    private Process client_process;
    private ServerSocket tor_server;

    public PT() {
        config = Config.getInstance();
     
        objs = new LinkedList<>();

        try {
            this.tor_server = new ServerSocket(config.getPt_client_port());
            
            ProcessBuilder pb = new ProcessBuilder("node", config.getClientLocationStreaming(), config.getClientPortStreaming());
            client_process = pb.start();
        
            Thread.sleep(500);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        ModulatorClientInterface copyloop = null;
        String mod = config.getModulation();

        SocksProtocol s = new SocksProtocol();
        boolean result = true;

        try {
            s.acceptConns(tor_server);
        } catch (SocksException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        if (mod.equals("copy")) {
            copyloop = new CopyMod(s.getSocks());
        } else if (mod.equals("stunnel")) {
            copyloop = new StunnelMod(s.getSocks());
        } else if (mod.equals("streaming")) {
            copyloop = new Streaming(s.getSocks());
        }

        result = copyloop.initialize(s.getReq().getAddr(), s.getReq().getPort(), s, mod);
    
        if (result) {
            InitializationPT.finishedInit();

            copyloop.run();
            objs.add(copyloop);
        }

    }

    public void shutdown() {
        for (ModulatorClientInterface i : objs)
            i.shutdown();

        try {
            tor_server.close();
            client_process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
