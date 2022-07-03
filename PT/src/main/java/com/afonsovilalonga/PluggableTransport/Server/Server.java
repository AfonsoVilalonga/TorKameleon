package com.afonsovilalonga.PluggableTransport.Server;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.java_websocket.WebSocket;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.JavascriptExecutor;

import com.afonsovilalonga.Common.Initialization.InitializationPT;
import com.afonsovilalonga.Common.Modulators.ModulatorServerInterface;
import com.afonsovilalonga.Common.Modulators.WebSocketWrapper;
import com.afonsovilalonga.Common.Modulators.Server.CopyMod;
import com.afonsovilalonga.Common.Modulators.Server.Streaming;
import com.afonsovilalonga.Common.Modulators.Server.StunnelMod;
import com.afonsovilalonga.Common.ObserversCleanup.Monitor;
import com.afonsovilalonga.Common.ObserversCleanup.ObserverServer;
import com.afonsovilalonga.Common.Utils.Config;

public class Server implements ObserverServer {
    private Config config;
    private boolean exit;
    private List<ServerReqConnection> running_conns;
    private boolean bootstraped;

    private WebSocketWrapper web_socket_server;
    private ServerSocket conns;

    private ChromeDriver browser;

    public Server(WebSocketWrapper web_socket_server) {
        this.config = Config.getInstance();
        this.running_conns = new LinkedList<>();
        this.web_socket_server = web_socket_server;
        this.bootstraped = false;
        this.exit = false;

        Monitor.registerObserver(this);

        ChromeOptions option = new ChromeOptions();
        option.setAcceptInsecureCerts(true);
        option.addArguments("headless");

        this.browser = new ChromeDriver(option);
    }

    public void run() {
        int pt_port = config.getPt_server_port();
        int or_port = config.getOrPort();
        String pt_host = config.getPTServerHost();

        try {
            conns = new ServerSocket(pt_port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (!exit) {
            try {
                String id = Integer.toString(running_conns.size());
                String id_window = null;
                ModulatorServerInterface copyloop = null;
                Socket tor_sock = connectToTor(pt_host, or_port);
                Socket conn = conns.accept();

                conn.setSoTimeout(10000);

                byte modByte = InitializationPT.bridge_protocol_server_side(conn);
                String mod = InitializationPT.mapper(modByte);

                conn.setSoTimeout(0);

                if (mod != null) {
                    if (mod.equals("copy"))
                        copyloop = new CopyMod(tor_sock, conn, id);
                    else if (mod.equals("stunnel"))
                        copyloop = new StunnelMod(tor_sock, conn, id);

                    else if (mod.equals("streaming")) {
                        PipedInputStream pin = new PipedInputStream();
                        PipedOutputStream pout = new PipedOutputStream();

                        try {
                            pout.connect(pin);
                        } catch (IOException e) {}

                        CountDownLatch connectionWaiter = new CountDownLatch(1);
                        web_socket_server.setMutexAndWaitConn(connectionWaiter);

                        ((JavascriptExecutor) browser).executeScript(
                                "window.open('http://localhost:" + config.getBridgePortStreaming() + "');");

                        try {
                            connectionWaiter.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        Set<String> windowHandles = browser.getWindowHandles();
                        for (String aux : windowHandles)
                            id_window = aux;

                        WebSocket sock = web_socket_server.getLaSocket();
                        web_socket_server.setTorConnToConn(pout, sock);

                        copyloop = new Streaming(tor_sock, sock, id, pin);
                    }

                    copyloop.run();

                    ServerReqConnection req = new ServerReqConnection(id_window, copyloop, mod, id);
                    running_conns.add(req);

                    InitializationPT.bridge_protocol_server_side_send_ack(conn, modByte);
                }

            } catch (IOException e) {}
        }
    }

    public void shutdown() {
        exit = true;
        for (ServerReqConnection i : running_conns)
            i.shutdown();
        try {
            conns.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Socket connectToTor(String pt_host, int or_port) {
        if (!bootstraped && !InitializationPT.tor_init(1000)) {
            System.err.println("Could not connect to Tor.");
            System.err.println("Exiting...");
            System.exit(-1);
        }

        try {
            Socket conn = new Socket(pt_host, or_port);
            return conn;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void onStateChange(String id) {
        ServerReqConnection aux = null;
        for(ServerReqConnection i: running_conns){
            if(i.getId().equals(id)){
                aux = i;
                break;
            }
        }
        aux.shutdown();
        running_conns.remove(aux);
    }
}
