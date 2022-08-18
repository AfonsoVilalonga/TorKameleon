package com.afonsovilalonga;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import com.afonsovilalonga.Common.Modulators.WebSocketWrapperPT;
import com.afonsovilalonga.Common.Utils.Config;
import com.afonsovilalonga.PluggableTransport.Client.PT;
import com.afonsovilalonga.PluggableTransport.Server.Server;
import com.afonsovilalonga.Proxy.Proxy;

import io.github.bonigarcia.wdm.WebDriverManager;

public class Solution {

    private static PT pt;
    private static Server server;

    private static boolean isBridge;

    private static Process client_process;
    private static Process bridge_process;
    private static WebSocketWrapperPT web_socket_server;

    private static Process signalling_process;

    public static void main(String[] args) {
        shutdown();
        isBridge = true;

        Config config = Config.getInstance();
        
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);
        System.setProperty("webdriver.chrome.silentOutput", "true");
        System.setProperty("webdriver.chrome.driver", config.getWebdriverLocation());
        System.setProperty("webdriver.chrome.silentOutput", "true");
        WebDriverManager.chromedriver().setup();

        System.setProperty("javax.net.ssl.trustStore", config.getKeystore());
        System.setProperty("javax.net.ssl.trustStorePassword", config.getPassword());

        if(args[0].equals("pt-server"))
            isBridge = false;

        if (!args[0].equals("pt-client") && !args[0].equals("proxy-client")) {
            try {
                ProcessBuilder pb2 = new ProcessBuilder("node", config.getWebRTCLocation() + "/Bridge/index.js",
                        config.getBridgePortStreaming());
                pb2.directory(new File(config.getWebRTCLocation() + "/Bridge"));
                bridge_process = pb2.start();

                ProcessBuilder pb3 = new ProcessBuilder("node", config.getWebRTCLocation() + "/Signalling/index.js");
                pb3.directory(new File(config.getWebRTCLocation() + "/Signalling"));
                signalling_process = pb3.start();

            } catch (IOException e1) {}
        }

        if (!args[0].equals("pt-server")) {
            try {
                ProcessBuilder pb = new ProcessBuilder("node", config.getWebRTCLocation() + "/Client/index.js",
                        config.getClientPortStreaming());
                pb.directory(new File(config.getWebRTCLocation() + "/Client"));
                client_process = pb.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(args[0].equals("proxy") || args[0].equals("pt-proxy"))
            web_socket_server = new WebSocketWrapperPT(true);
        else    
            web_socket_server = new WebSocketWrapperPT(false);
        
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
                System.out.println("oi");
                new Proxy(web_socket_server);
                break;
            case "proxy":
                new Proxy(web_socket_server);
                break;
            case "proxy-client":
                new Proxy(web_socket_server);
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

                if (isBridge) {
                    if (pt != null)
                        pt.shutdown();
                } else {
                    if (server != null)
                        server.shutdown();
                }

                if(client_process != null)
                    client_process.destroy();
                if(signalling_process != null)
                    signalling_process.destroy();
                if(bridge_process != null)
                    bridge_process.destroy();

                try {
                    if(web_socket_server != null)
                        web_socket_server.stop();
                } catch (IOException e) {} 
                catch (InterruptedException e) {}

                System.out.println("Shutting down.");
            }
        });
    }
}
