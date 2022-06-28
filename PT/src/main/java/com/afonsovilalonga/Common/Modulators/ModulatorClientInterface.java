package com.afonsovilalonga.Common.Modulators;

import com.afonsovilalonga.Common.Socks.SocksProtocol;

public interface ModulatorClientInterface extends ModulatorServerInterface{
    
    public boolean initialize(String host, int port, SocksProtocol s, String mod);

}
