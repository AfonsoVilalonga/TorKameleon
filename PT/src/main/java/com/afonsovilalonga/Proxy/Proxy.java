package com.afonsovilalonga.Proxy;


import javax.net.ServerSocketFactory;
import javax.net.ssl.*;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.afonsovilalonga.Common.Initialization.ProxyStreamingHanshake.Initialization;
import com.afonsovilalonga.Common.Modulators.WebSocketWrapper;
import com.afonsovilalonga.Common.Socks.SocksProtocol;
import com.afonsovilalonga.Common.Utils.Config;
import com.afonsovilalonga.Proxy.Utils.DTLSOverDatagram;
import com.afonsovilalonga.Proxy.Utils.Http;
import org.openqa.selenium.JavascriptExecutor;
import org.java_websocket.WebSocket;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Proxy {

    public static final int BUF_SIZE = 1024;
    public static final int N_THREADS = 8;
    public static final int secureHttpingRequestPort = 2238;
    public static final int PACKET_ANALYSIS_PORT = 3238;
    public static final int PERTURBATION_DELAY_PERCENTAGE = 20;
    public static final int MAX_PERTURBATION_DELAY_TIME_MS = 5000;
    public static final int PING_PORT = 80;

    private String bypassAddress;
    private ConcurrentLinkedQueue<Instant> arrival_times = new ConcurrentLinkedQueue<>();
    private WebSocketWrapper web_socket_server;
    private ChromeDriver browser;

    private Config config;

    public Proxy(WebSocketWrapper web_socket_server){
        try {
            this.web_socket_server = web_socket_server;

            ChromeOptions option = new ChromeOptions();
            option.setAcceptInsecureCerts(true);
            option.addArguments("headless");

            this.browser = new ChromeDriver(option);

            run();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void run() throws FileNotFoundException {
        config = Config.getInstance();

        int local_port_unsecure = config.getLocal_port_unsecure();
        int local_port_secure = config.getLocal_port_secure();
        int test_port_iperf = config.getTest_port_iperf();
        int test_port_httping = config.getTest_port_httping();
        int test_port_analytics = config.getTest_port_analytics();
        

        bypassTriggeredTimer();
        arrival_times.add(Instant.now());

        // TCP
        new Thread(() -> {
            ExecutorService executor = null;
            try (ServerSocket server = new ServerSocket(local_port_unsecure)) {
                executor = Executors.newFixedThreadPool(N_THREADS);
                System.out.println("Listening on TCP port " + local_port_unsecure + ", waiting for file request!");
                while (true) {
                    final Socket socket = server.accept();
                    System.out.println("TCP connection " + socket.getInetAddress() + ":" + socket.getPort());
                    executor.execute(() -> doTCP_TLS(socket));
                }
            } catch (IOException ioe) {
                System.err.println("Cannot open the port on TCP");
                ioe.printStackTrace();
            } finally {
                System.out.println("Closing TCP server");
                if (executor != null) {
                    executor.shutdown();
                }
            }
        }).start();

        // TLS
        new Thread(() -> {
            ExecutorService executor = null;
            try (ServerSocket server = getSecureSocketTLS(local_port_secure)) {
                executor = Executors.newFixedThreadPool(N_THREADS);
                System.out.println("Listening on TLS port " + local_port_secure + ", waiting for file request!");
                while (true) {
                    final Socket socket = server.accept();
                    System.out.println("TLS connection " + socket.getInetAddress() + ":" + socket.getPort());
                    executor.execute(() -> doTCP_TLS(socket));
                }
            } catch (IOException ioe) {
                System.err.println("Cannot open the port on TLS");
                ioe.printStackTrace();
            } finally {
                System.out.println("Closing TLS server");
                if (executor != null) {
                    executor.shutdown();
                }
            }
        }).start();

        // UDP
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(local_port_unsecure)) {
                System.out.println("Listening on UDP port " + local_port_unsecure + ", waiting for file request!");
                while (true) {
                    doUDP(socket);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Cannot open the port on UDP");

            } finally {
                System.out.println("Closing UDP server");
            }
        }).start();

        //DTLS
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(local_port_secure)) {
                System.out.println("Listening on DTLS port " + local_port_secure + ", waiting for file request!");
                while (true) {
                    doDTLS(socket);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Cannot open the port on DTLS");

            } finally {
                System.out.println("Closing DTLS server");
            }
        }).start();

        // Iperf test
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(test_port_iperf)) {

                System.out.println("Iperf proxy is listening on port " + test_port_iperf);

                while (true) {
                    Socket socket = serverSocket.accept();
                    measureTestIperf(socket);
                    socket.close();
                }

            } catch (IOException ex) {
                System.out.println("Iperf exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();

        // HTTPing test
        new Thread(() -> {
            ExecutorService executor = null;
            try (ServerSocket ss = new ServerSocket(test_port_httping)) {
                executor = Executors.newFixedThreadPool(N_THREADS);
                System.out.println("Httping proxy is listening on port " + test_port_httping);
                while (true) {
                    Socket clientSock = ss.accept();
                    measureTestHttping(clientSock);
                    clientSock.close();
                }
            } catch (IOException ioe) {
                System.err.println("Cannot open the port on Httping");
                ioe.printStackTrace();
            } finally {
                System.out.println("Closing Httping server");
                if (executor != null) {
                    executor.shutdown();
                }
            }
        }).start();

        // Packet Analysis test
        new Thread(() -> {
            ExecutorService executor = null;
            try (ServerSocket ss = new ServerSocket(test_port_analytics)) {
                executor = Executors.newFixedThreadPool(N_THREADS);
                System.out.println("Packet Analysis proxy is listening on port " + test_port_analytics);
                while (true) {
                    Socket clientSock = ss.accept();
                    packetAnalysis(clientSock);
                    clientSock.close();
                }
            } catch (IOException ioe) {
                System.err.println("Cannot open the port on Packet Analysis");
                ioe.printStackTrace();
            } finally {
                System.out.println("Closing Packet Analysis server");
                if (executor != null) {
                    executor.shutdown();
                }
            }
        }).start();

        //Streaming incoming connection, TODO METER O PORT CERTO
        new Thread(() -> {
            ExecutorService executor = null;
            try(ServerSocket ss = new ServerSocket(2999)){
                executor = Executors.newFixedThreadPool(N_THREADS);
                System.out.println("Streaming protocol is listening on port " + local_port_secure);
                while (true) {
                    Socket socket = ss.accept();
                    executor.execute(() -> doStreaming(socket));
                    socket.close();
                }
            } catch (IOException ioe) {
                System.err.println("Cannot open the port on Streaming protocol");
                ioe.printStackTrace();
            } finally {
                System.out.println("Closing Streaming protocol server");
            }
        }).start();
    }

    //To receive streaming
    private void doStreaming(Socket socket){
        byte val = Initialization.serverHandshake(socket);

        if(val != Initialization.ACK_FAILED){
            Initialization.sendAccept(socket);
            
            WebSocket sock = null;
            PipedInputStream pin = new PipedInputStream();
            PipedOutputStream pout = new PipedOutputStream();
    
            try {
                pout.connect(pin);
            } catch (IOException e) {}
            
            synchronized(this){    
                CountDownLatch connectionWaiter = new CountDownLatch(1);
                web_socket_server.setMutexAndWaitConn(connectionWaiter);
    
                ((JavascriptExecutor) browser).executeScript(
                    "window.open('http://localhost:" + config.getBridgePortStreaming() + "');");
    
                try {
                    connectionWaiter.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
    
                sock = web_socket_server.getLaSocket();
                web_socket_server.setTorConnToConn(pout, sock);
            }

            byte[] buffer = new byte[BUF_SIZE];
            try {
                pin.read(buffer);
                addJitterPerturbation();
            } catch (IOException | InterruptedException e) {}

            arrival_times.add(Instant.now());

            String filePath = Http.parseHttpReply(new String(buffer))[1];
            System.out.println("File request path: " + filePath + " from " + socket.getInetAddress() + ":" + socket.getPort());
            byte[] data = bypass(filePath);

            WebSocketWrapper.send(Arrays.copyOfRange(data, 0, data.length), sock); 
        } else{
            Initialization.sendFalied(socket);
        }
    }

    public void shutdown(){
        
    }

    
    private void measureTestHttping(Socket socket) {
        try {
            String local_host = config.getLocal_host();
            int tor_buffer_size = config.getTor_buffer_size();
            String remote_host = config.getRemote_host();
            String tor_host = config.getTor_host();
            int tor_port = config.getTor_port();
            int test_stunnel_port_httping = config.getTest_stunnel_port_httping();

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            out.flush();

            byte[] buffer = new byte[BUF_SIZE];
            String my_address = local_host;

            if (bypassAddress.equals(my_address)) {

                OutputStream outTor;
                InputStream inTor;
                Socket socketTor;

                byte[] bufferTor = new byte[tor_buffer_size];
                int n = in.read(buffer, 0, buffer.length);


                if (new String(buffer).contains("https")) {
                    socketTor = SocksProtocol.sendRequest((byte)0x04, remote_host, secureHttpingRequestPort, tor_host, tor_port);
                } else if (new String(buffer).contains("ping")) {
                    socketTor = SocksProtocol.sendRequest((byte)0x04, remote_host, PING_PORT, tor_host, tor_port);
                } else {
                    socketTor = SocksProtocol.sendRequest((byte)0x04, remote_host, 5000, tor_host, tor_port);
                }
                socketTor.setReceiveBufferSize(tor_buffer_size);
                socketTor.setSendBufferSize(tor_buffer_size);
                outTor = socketTor.getOutputStream();
                outTor.flush();
                inTor = socketTor.getInputStream();

                System.err.println("Sending to httping: " + new String(buffer));
                outTor.write(buffer, 0, n);
                outTor.flush();

                inTor.read(bufferTor, 0, bufferTor.length);

                out.write(bufferTor, 0, bufferTor.length);
                //out.write("\r\n\r\n".getBytes());

                out.flush();
                socketTor.close();
            } else {
                System.err.println("TIR-MMRT test connection :" + my_address + " ---> " + bypassAddress);

                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket socketStunnel = (SSLSocket) factory.createSocket(bypassAddress.split(":")[0], test_stunnel_port_httping);
                socketStunnel.startHandshake();
                OutputStream outStunnel = socketStunnel.getOutputStream();
                InputStream inStunnel = socketStunnel.getInputStream();

                byte[] bufferTStunnel = new byte[tor_buffer_size];
                in.read(buffer, 0, buffer.length);
                System.err.println("Received from httping: " + new String(buffer));
                outStunnel.write(buffer);
                inStunnel.read(bufferTStunnel, 0, bufferTStunnel.length);
                out.write(bufferTStunnel);
                out.write("\r\n\r\n".getBytes());

                out.flush();
                outStunnel.flush();
                socketStunnel.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }


    private void measureTestIperf(Socket serverSocket) {
        try {
            int tor_buffer_size = config.getTor_buffer_size();
            String local_host = config.getLocal_host();
            String remote_host = config.getRemote_host();
            String tor_host = config.getTor_host();
            int tor_port = config.getTor_port();
            int test_stunnel_port_iperf = config.getTest_stunnel_port_iperf();

            InputStream in = serverSocket.getInputStream();

            byte[] buffer = new byte[tor_buffer_size];
            String my_address = local_host;

            if (bypassAddress.equals(my_address)) {

                Socket clientSocket = SocksProtocol.sendRequest((byte)0x04, remote_host, 5001, tor_host, tor_port);
                clientSocket.setReceiveBufferSize(tor_buffer_size);
                clientSocket.setSendBufferSize(tor_buffer_size);
                OutputStream out = clientSocket.getOutputStream();
                out.flush();

                int n;
                while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
                    System.err.println("Sending iperf throughout Tor: " + new String(buffer));
                    out.write(buffer, 0, n);
                }
                out.write("\r\n\r\n".getBytes());
                out.flush();
                clientSocket.close();
            } else {
                System.err.println("TIR-MMRT test connection :" + my_address + " ---> " + bypassAddress);

                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket socket = (SSLSocket) factory.createSocket(bypassAddress.split(":")[0], test_stunnel_port_iperf);
                socket.startHandshake();
                OutputStream out = socket.getOutputStream();
                out.flush();

                int n;
                while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
                    System.err.println("sending iperf to" + bypassAddress + ":" + new String(buffer));
                    out.write(buffer, 0, n);
                    Thread.sleep(5); // Avoid traffic congestion
                }
                out.flush();
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private synchronized void addJitterPerturbation() throws InterruptedException {
        if (arrival_times == null || arrival_times.isEmpty()) return;
        Instant arrival_time = arrival_times.poll();
        double delay_percentage = (new Random().nextInt(PERTURBATION_DELAY_PERCENTAGE) / 100.0) + 1;
        Duration interval_arrival_time = Duration.between(arrival_time, Instant.now());
        double interval_arrival_time_percentage = interval_arrival_time.toMillis() * delay_percentage;
        if (interval_arrival_time_percentage < MAX_PERTURBATION_DELAY_TIME_MS) {
            Thread.sleep((int) interval_arrival_time_percentage);
        }
    }

    private ServerSocket getSecureSocketTLS(int port) throws IOException {
        ServerSocketFactory ssf = getServerSocketFactory();
        ServerSocket ss = ssf.createServerSocket(port);
        return ss;
    }

    private void doTCP_TLS(Socket socket) {
        try {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[BUF_SIZE];
            in.read(buffer);

            //Add perturbation before send to Tor
            addJitterPerturbation();
            arrival_times.add(Instant.now());

            String filePath = Http.parseHttpReply(new String(buffer))[1];
            System.out.println("File request path: " + filePath + " from " + socket.getInetAddress() + ":" + socket.getPort());
            byte[] data = bypass(filePath);
            out.write(data, 0, data.length);
            out.flush();

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doUDP(DatagramSocket socket) throws Exception {
        //receive
        byte[] buf = new byte[socket.getReceiveBufferSize()];
        DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
        socket.receive(receivePacket);
        String filePath = new String(buf, StandardCharsets.UTF_8);
        System.out.println("File request path: " + filePath + " from " + receivePacket.getAddress() + ":" + receivePacket.getPort());

        //Add perturbation before send to Tor
        addJitterPerturbation();
        arrival_times.add(Instant.now());
        byte[] data = bypass(filePath);

        //send
        ExecutorService executor;
        executor = Executors.newFixedThreadPool(N_THREADS);
        executor.execute(() -> {
            try {
                int bytesSent = 0;
                while (bytesSent <= data.length) {
                    byte[] sendData = Arrays.copyOfRange(data, bytesSent, bytesSent + BUF_SIZE); //prevent sending bytes overflow
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
                    socket.send(sendPacket);
                    bytesSent += BUF_SIZE;
                    Thread.sleep(5); // Avoid traffic congestion
                }
                byte[] endTransmission = "terminate_packet_receive".getBytes();
                socket.send(new DatagramPacket(endTransmission, endTransmission.length, receivePacket.getAddress(), receivePacket.getPort()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private void doDTLS(DatagramSocket socket) {
        try {
            //Handshake and receive
            DTLSOverDatagram dtls = new DTLSOverDatagram();
            SSLEngine engine = dtls.createSSLEngine(false);
            InetSocketAddress isa = dtls.handshake(engine, socket, null, "Server");
            String filePath = dtls.receiveAppData(engine, socket);

            //Add perturbation before send to Tor
            addJitterPerturbation();
            arrival_times.add(Instant.now());

            byte[] data = bypass(filePath);
            //deliver up to nThread clients
            ExecutorService executor = null;
            executor = Executors.newFixedThreadPool(N_THREADS);
            executor.execute(() -> dtls.deliverAppData(engine, socket, ByteBuffer.wrap(data), isa));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unable to do DTLS");
        }

    }

    private void packetAnalysis(Socket socket) {
        try {
            int tor_buffer_size = config.getTor_buffer_size();
            String local_host = config.getLocal_host();
            String remote_host = config.getRemote_host();
            String tor_host = config.getTor_host();
            int tor_port = config.getTor_port();
            List<String> tirmmrt_network = config.getTirmmrt_network(); 
            int test_stunnel_port_analytics = config.getTest_stunnel_port_analytics();

            InputStream in = socket.getInputStream();

            byte[] buffer = new byte[tor_buffer_size];
            String my_address = local_host;

            //Send through Tor
            Socket clientSocket = SocksProtocol.sendRequest((byte)0x04, remote_host, PACKET_ANALYSIS_PORT, tor_host, tor_port);
            OutputStream outTor = clientSocket.getOutputStream();
            outTor.flush();

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

            Map<String, SSLSocket> stunnelSockets = new HashMap<>();
            for (String tir : tirmmrt_network) {
                if (tir.equals("localhost") || tir.equals("127.0.0.1")) continue;
                SSLSocket socketStunnel = (SSLSocket) factory.createSocket(tir.split(":")[0], test_stunnel_port_analytics);
                stunnelSockets.put(tir, socketStunnel);
                socketStunnel.startHandshake();
            }

            while ((in.read(buffer, 0, buffer.length)) >= 0) {
                Thread.sleep(15); // Avoid traffic congestion

                //Add perturbation before send to Tor
                addJitterPerturbation();
                arrival_times.add(Instant.now());

                if (bypassAddress.equals(my_address)) {
                    outTor.write(buffer);
                } else {
                    System.err.println("TIR-MMRT test connection :" + my_address + " ---> " + bypassAddress);
                    OutputStream outStunnel = stunnelSockets.get(bypassAddress).getOutputStream();
                    outStunnel.write(buffer);
                }
            }
            outTor.flush();
            outTor.close();
            clientSocket.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void randomlyChooseBypassAddress() {
        List<String> tirmmrt_network = config.getTirmmrt_network(); 

        bypassAddress = tirmmrt_network.get(new Random().nextInt(tirmmrt_network.size()));
        System.err.println("Selected new bypass address is " + bypassAddress);
    }

    private void bypassTriggeredTimer() {
        int bypass_timer = config.getBypass_timer();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                randomlyChooseBypassAddress();
            }
        }, 0, bypass_timer);
    }

    private byte[] bypass(String path) {
        try {
            String local_host = config.getLocal_host();
            String remote_host = config.getRemote_host();
            int remote_port = config.getRemote_port();

            String my_address = local_host;
            if (bypassAddress.equals(my_address)) {
                return torRequest(path, remote_host, remote_port);
            } else {
                System.err.println("TIR-MMRT connection :" + my_address + " ---> " + bypassAddress);
                boolean isStreaming = true;
                if(isStreaming){
                    return bypassConnectionStremaing(path);
                }else{
                    return bypassConnection(path);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //TODO PORTS MESMO PORT QUE AQUELE LA DE CIMA DA TRHEAD 
    private byte[] bypassConnectionStremaing(String path) throws Exception{
        boolean result = Initialization.startHandshake(bypassAddress.split(":")[0], 2999);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        if(result){
            WebSocket sock = null;
            PipedInputStream pin = new PipedInputStream();
            PipedOutputStream pout = new PipedOutputStream();

            try {
                pout.connect(pin);
            } catch (IOException e) {}
            
            synchronized(this){    
                CountDownLatch connectionWaiter = new CountDownLatch(1);
                web_socket_server.setMutexAndWaitConn(connectionWaiter);
    
                ((JavascriptExecutor) browser).executeScript(
                    "window.open('http://localhost:" + config.getClientPortStreaming() + "');");
    
                try {
                    connectionWaiter.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                sock = web_socket_server.getLaSocket();
                web_socket_server.setTorConnToConn(pout, sock);
            }
            
            WebSocketWrapper.send(String.format("GET %s HTTP/1.1", path.trim()).getBytes(), sock);         

            int n;
            byte[] buffer = new byte[BUF_SIZE];
            while ((n = pin.read(buffer, 0, buffer.length)) != -1) {
                //System.out.write(buffer, 0, n);
                baos.write(buffer, 0, n);
            }
            System.out.println("acabou");

            //GARBAGGE COLLECTION QUANDO CHEGAR AO -1 PORTANTO ELIMINAR A PAGINA E TAL 
        }else{
            System.err.println("Error while handshaking with the brdige using the Streaming protocol");
        }

        return baos.toByteArray();
    }

    private byte[] bypassConnection(String path) throws Exception {
        String stunnel_port = config.getStunnel_port();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SSLSocketFactory factory =
                (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket =
                (SSLSocket) factory.createSocket(bypassAddress.split(":")[0], Integer.parseInt(stunnel_port));
        socket.startHandshake();
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        out.write(String.format("GET %s HTTP/1.1", path.trim()).getBytes());

        int n;
        byte[] buffer = new byte[BUF_SIZE];
        while ((n = in.read(buffer, 0, buffer.length)) != -1) {
            //System.out.write(buffer, 0, n);
            baos.write(buffer, 0, n);

        }

        return baos.toByteArray();
    }

    private ServerSocketFactory getServerSocketFactory() {
        Config config = Config.getInstance();
        SSLServerSocketFactory ssf;
        try {
            // set up key manager to do server.key authentication
            SSLContext ctx;
            KeyManagerFactory kmf;
            KeyStore ks;
            
            char[] passphrase = config.getPassword().toCharArray();

            ctx = SSLContext.getInstance("TLS");
            kmf = KeyManagerFactory.getInstance("SunX509");
            ks = KeyStore.getInstance("JKS");

            ks.load(new FileInputStream(config.getKey()), passphrase);
            kmf.init(ks, passphrase);
            ctx.init(kmf.getKeyManagers(), null, null);

            ssf = ctx.getServerSocketFactory();
            return ssf;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private byte[] torRequest(String path, String remoteAddress, int remotePort) throws IOException {
        String tor_host = config.getTor_host();
        int tor_port = config.getTor_port();
        int tor_buffer_size = config.getTor_buffer_size();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Socket clientSocket = SocksProtocol.sendRequest((byte)0x04, remoteAddress, remotePort, tor_host, tor_port);
        clientSocket.setReceiveBufferSize(tor_buffer_size);
        clientSocket.setSendBufferSize(tor_buffer_size);
        OutputStream out = clientSocket.getOutputStream();
        out.flush();

        out.write(String.format("GET %s HTTP/1.1\r\n\r\n", path).getBytes());
        out.flush();

        InputStream in = clientSocket.getInputStream();
        int n;
        byte[] buffer = new byte[tor_buffer_size];
        while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
            baos.write(buffer, 0, n);
        }
        clientSocket.close();
        return baos.toByteArray();
    }

}