package com.afonsovilalonga.Common.Modulators.Client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.SSLSocket;

import org.java_websocket.WebSocket;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.afonsovilalonga.Common.Initialization.Exceptions.BridgeFailedException;
import com.afonsovilalonga.Common.Initialization.PluggableTransportHanshake.InitializationPT;
import com.afonsovilalonga.Common.Modulators.ModulatorClientInterface;
import com.afonsovilalonga.Common.Modulators.ModulatorTop;
import com.afonsovilalonga.Common.Modulators.WebSocketWrapperPT;
import com.afonsovilalonga.Common.Socks.SocksProtocol;
import com.afonsovilalonga.Common.Utils.Config;
import com.afonsovilalonga.Common.Utils.Utilities;


public class Streaming extends ModulatorTop implements ModulatorClientInterface{

    private SSLSocket bridge_conn;

    private WebSocket bridge_sock;
    private WebSocketWrapperPT web_server;
    private ChromeDriver browser;

    private PipedInputStream pin;
    private PipedOutputStream pout;
    
    public Streaming(Socket tor_socket, WebSocketWrapperPT web_server) {
        super(tor_socket);
        this.web_server = web_server;
        this.pin = new PipedInputStream();
        this.pout = new PipedOutputStream();
        
        try {
            pout.connect(pin);
        } catch (IOException e) {}
        
        this.browser = null;
        this.bridge_sock = null;
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
                
                option.addArguments("--log-level=3");
                option.addArguments("--silent");

                if(!config.getWatchVideo().equals("pt-client"))
                    option.addArguments("headless");

                browser = new ChromeDriver(option);
                browser.get("http://localhost:" + config.getClientPortStreaming() + "/?bridge=0");

                //Wait for connection between local browser running client side webrtc and java websocket server
                try {
                    connectionWaiter.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                bridge_sock = web_server.getLaSocket();
                web_server.setPipe(pout, bridge_sock);

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
            DataOutputStream out_tor = new DataOutputStream(new BufferedOutputStream(tor_socket.getOutputStream()));
            
            byte[] send = new byte[config.getPTBufferSize()];
            byte[] recv = new byte[config.getPTBufferSize()];

            executor.execute(() -> {
                try {
                    int i = 0;
                    while ((i = in_Tor.read(send)) != -1) {
                        System.out.println(i + " ola");
                        WebSocketWrapperPT.send(Arrays.copyOfRange(send, 0, i), bridge_sock);            
                    }
                } catch (Exception e) {}
               
                System.exit(-1);
            });

            executor.execute(() -> {
                try {
                    int i = 0;
                    while ((i = pin.read(recv)) != -1) {
                        System.out.println(i + " adeus");
                        out_tor.write(recv, 0, i);
                        out_tor.flush();            
                    }
                } catch (Exception e) {}
                
                System.exit(-1);
            });

        } catch (IOException e) {
            System.exit(-1);
        }
    }

    @Override
    public void shutdown() {
        try {
            serviceShutdow();
            if(this.bridge_conn != null)
                this.bridge_conn.close();

            if(browser != null)
                browser.quit();
            pin.close();
            pout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean connectToBridge(String host, int port) {
        try {
            SSLSocket socket = Utilities.createSSLSocket(host, port);
            this.bridge_conn = socket;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }    
}
