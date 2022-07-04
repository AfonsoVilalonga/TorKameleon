package com.afonsovilalonga;

import java.io.File;
import java.io.IOException;

import com.afonsovilalonga.Common.Modulators.WebSocketWrapper;
import com.afonsovilalonga.Common.Utils.Config;
import com.afonsovilalonga.PluggableTransport.Client.PT;
import com.afonsovilalonga.PluggableTransport.Server.Server;
import com.afonsovilalonga.Proxy.Proxy;

import io.github.bonigarcia.wdm.WebDriverManager;

public class Solution {

    private static PT pt; 
    private static Server server;
    private static Proxy proxy;

    private static boolean isBridge;

    private static Process client_process;
    private static Process bridge_process;
    private static WebSocketWrapper web_socket_server;

    private static Process signalling_process;

    public static void main(String[] args) {
        shutdown();
        isBridge = true;

        Config config = Config.getInstance();
        
        System.setProperty("webdriver.chrome.driver", config.getWebdriverLocation());
        WebDriverManager.chromedriver().setup();

        System.setProperty("javax.net.ssl.trustStore", config.getKeystore());
        System.setProperty("javax.net.ssl.trustStorePassword", config.getPassword());

        try {
            ProcessBuilder pb2 = new ProcessBuilder("node", config.getWebRTCLocation() + "/Bridge/index.js", config.getBridgePortStreaming());
            pb2.directory(new File(config.getWebRTCLocation() + "/Bridge"));
            bridge_process = pb2.start();
        } catch (IOException e1) {}


        if(args[0].equals("pt-client") || args[0].equals("proxy")){
            try {
                ProcessBuilder pb = new ProcessBuilder("node", config.getWebRTCLocation() + "/Client/index.js", config.getClientPortStreaming());
                pb.directory(new File(config.getWebRTCLocation() + "/Client"));
                client_process = pb.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            isBridge = false;
            try {
                ProcessBuilder pb = new ProcessBuilder("node", config.getWebRTCLocation() + "/Signalling/index.js");
                pb.directory(new File(config.getWebRTCLocation() + "/Signalling"));
                signalling_process = pb.start();
            } catch (IOException e) {}
        }

        web_socket_server = new WebSocketWrapper(isBridge);

        switch (args[0]) {
            case "pt-client":
                pt = new PT(web_socket_server);
                pt.run();
                break;
            case "pt-server":
                server = new Server(web_socket_server);
                server.run();
                break;
            case "pt-proxy":
                pt = new PT(web_socket_server);
                pt.run();
                proxy = new Proxy(web_socket_server);
                break;
            case "proxy":
                proxy = new Proxy(web_socket_server);
                break;
            default:
                System.err.println("Invalid running command");
                break;
        }
    }

    private static void shutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.clearProperty("tir.done");

                if(isBridge){
                    if(pt != null)
                        pt.shutdown();
                    client_process.destroy();
                }else{
                    if(proxy != null)
                        proxy.shutdown();
                    
                    if(server != null)
                        server.shutdown();
                    signalling_process.destroy();
                }

                bridge_process.destroy();

                try {
                    web_socket_server.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("Shutting down.");
            }
        }); 
    }
}
