package com.afonsovilalonga.Common.Modulators.Client;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import org.java_websocket.WebSocket;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.afonsovilalonga.Common.Initialization.InitializationPT;
import com.afonsovilalonga.Common.Initialization.Exceptions.BridgeFailedException;
import com.afonsovilalonga.Common.Modulators.ModulatorClientInterface;
import com.afonsovilalonga.Common.Modulators.ModulatorTop;
import com.afonsovilalonga.Common.Modulators.WebSocketWrapper;
import com.afonsovilalonga.Common.Socks.SocksProtocol;
import com.afonsovilalonga.Common.Utils.Config;


public class Streaming extends ModulatorTop implements ModulatorClientInterface{

    private Socket bridge_conn;

    private WebSocket bridge_sock;
    private WebSocketWrapper web_server;
    private ChromeDriver browser;
    
    public Streaming(Socket tor_socket, WebSocketWrapper web_server) {
        super(tor_socket);
        this.web_server = web_server;
        browser = null;
        bridge_sock = null;
    }

    @Override
    public boolean initialize(String host, int port, SocksProtocol s, String mod) {
        boolean can_connect = reTry(() -> connectToBridge(host, port));
        Config config = Config.getInstance();

        if(can_connect){
            try {
                //Wait for bridge to start up streaming protocol
                InitializationPT.bridge_protocol_client_side(bridge_conn, mod);
                
                CountDownLatch connectionWaiter = new CountDownLatch(1);
                web_server.setMutexAndWaitConn(connectionWaiter);

                ChromeOptions option = new ChromeOptions();
                option.setAcceptInsecureCerts(true);            
                    
                browser = new ChromeDriver(option);
                browser.get("http://localhost:" + config.getClientPortStreaming());

                //Wait for connection between local browser running client side webrtc and java websocket server
                try {
                    connectionWaiter.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                bridge_sock = web_server.getLaSocket();
                web_server.setTorConnToConn(gettor_socket(), bridge_sock);

                //everthing ready to start sending tor traffic
                s.sendSocksResponseAccepted();

                try {
                    bridge_conn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

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
                        WebSocketWrapper.send(Arrays.copyOfRange(send, 0, i), bridge_sock);            
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
