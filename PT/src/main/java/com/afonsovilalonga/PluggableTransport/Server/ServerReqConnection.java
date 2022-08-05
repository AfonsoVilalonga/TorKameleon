package com.afonsovilalonga.PluggableTransport.Server;

import com.afonsovilalonga.Common.Modulators.ModulatorServerInterface;

public class ServerReqConnection {

    private ModulatorServerInterface copyloop;

    public ServerReqConnection(ModulatorServerInterface copyloop){
       this.copyloop = copyloop;
    }

    public void shutdown(){
        copyloop.shutdown();    
    }
   
}
