package com.afonsovilalonga;

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

    public static void main(String[] args) {
        shutdown();
        isBridge = true;

        Config config = Config.getInstance();
        
        System.setProperty("webdriver.chrome.driver", config.getWebdriverLocation());
        WebDriverManager.chromedriver().setup();

        System.setProperty("javax.net.ssl.trustStore", config.getKeystore());
        System.setProperty("javax.net.ssl.trustStorePassword", config.getPassword());

        switch (args[0]) {
            case "pt-client":
                pt = new PT();
                pt.run();
                break;
            case "pt-server":
                isBridge = false;
                server = new Server();
                server.run();
                break;
            case "proxy":
                proxy = new Proxy();
                pt = new PT();
                pt.run();
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
                    pt.shutdown();
                }else{
                    server.shutdown();
                }
            }
        }); 
    }
}
