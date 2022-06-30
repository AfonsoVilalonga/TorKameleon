package com.afonsovilalonga.Common.Modulators.Client.Streaming;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.afonsovilalonga.Common.Initialization.InitializationPT;
import com.afonsovilalonga.Common.Initialization.Exceptions.BridgeFailedException;
import com.afonsovilalonga.Common.Modulators.ModulatorClientInterface;
import com.afonsovilalonga.Common.Modulators.ModulatorTop;
import com.afonsovilalonga.Common.Socks.SocksProtocol;
import com.afonsovilalonga.Common.Utils.Config;


public class Streaming extends ModulatorTop implements ModulatorClientInterface{

    private Socket bridge_conn;
    private WebSocketWrapperClient web_sock;
    private ChromeDriver browser;
    
    public Streaming(Socket tor_socket) {
        super(tor_socket);
       
        web_sock = new WebSocketWrapperClient(tor_socket);
        browser = null;
    }

    @Override
    public boolean initialize(String host, int port, SocksProtocol s, String mod) {
        boolean can_connect = reTry(() -> connectToBridge(host, port));
        Config config = Config.getInstance();

        if(can_connect){
            try {
                InitializationPT.bridge_protocol_client_side(bridge_conn, mod);
                                        
                ChromeOptions option = new ChromeOptions();
                //option.addArguments("headless");
                option.setAcceptInsecureCerts(true);            
                    
                browser = new ChromeDriver(option);
                browser.get("http://localhost:" + config.getClientPortStreaming());

            } catch (BridgeFailedException e) {
                e.printStackTrace();
                try {
                    bridge_conn.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                s.sendSocksResponseRejected(SocksProtocol.CONN_NOT_ALLOWED);
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

            byte[] send = new byte[config.getBufferSize()];

            executor.execute(() -> {
                try {
                    int i = 0;
                    while ((i = in_Tor.read(send)) != -1) {
                        web_sock.send(Arrays.copyOfRange(send, 0, i));           
                    }
                } catch (Exception e) {
                    notifyObserver();
                }
            });
        } catch (IOException e) {
            notifyObserver();
        }
    }

    @Override
    public void shutdown() {
        try {
            serviceShutdow();
            if(this.bridge_conn != null)
                this.bridge_conn.close();

            browser.quit();
            web_sock.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean connectToBridge(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            this.bridge_conn = socket;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }    
}
