package com.afonsovilalonga.PluggableTransport.Server;

import com.afonsovilalonga.Common.Modulators.ModulatorServerInterface;

public class ServerReqConnection {

    private ModulatorServerInterface copyloop;
    private String id_window;
    private String mod;
    private String id;

    public ServerReqConnection(String id_window, ModulatorServerInterface copyloop, String mod, String id){
        this.copyloop = copyloop;
        this.id_window = id_window;
        this.mod = mod;
        this.id = id;
    }

    public String getId(){
        return this.id;
    }

    public String getMod(){
        return this.mod;
    }

    public String getId_Window(){
        return this.id_window;
    }

    public void shutdown(){
        copyloop.shutdown();    
    }
   
}
