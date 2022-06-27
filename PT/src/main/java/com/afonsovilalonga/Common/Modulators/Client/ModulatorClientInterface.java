package com.afonsovilalonga.Common.Modulators.Client;

import com.afonsovilalonga.Common.Modulators.Server.ModulatorServerInterface;
import com.afonsovilalonga.Common.Socks.SocksProtocol;

public interface ModulatorClientInterface extends ModulatorServerInterface{
    
    public boolean initialize(String host, int port, SocksProtocol s, String mod);

}
