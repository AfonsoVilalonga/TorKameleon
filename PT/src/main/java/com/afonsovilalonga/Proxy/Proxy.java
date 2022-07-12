package com.afonsovilalonga.Proxy;

import javax.net.ssl.*;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.afonsovilalonga.Common.Initialization.ProxyStreamingHanshake.Initialization;
import com.afonsovilalonga.Common.Modulators.WebSocketWrapperPT;
import com.afonsovilalonga.Common.Socks.SocksProtocol;
import com.afonsovilalonga.Common.Utils.Config;
import com.afonsovilalonga.Common.Utils.Utilities;
import com.afonsovilalonga.Proxy.Utils.DTLSOverDatagram;
import com.afonsovilalonga.Proxy.Utils.Http;
import org.openqa.selenium.JavascriptExecutor;
import org.java_websocket.WebSocket;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Proxy {

    public static final int N_THREADS = 8;
    public static final int secureHttpingRequestPort = 2238;
    public static final int PACKET_ANALYSIS_PORT = 3238;
    public static final int PERTURBATION_DELAY_PERCENTAGE = 20;
    public static final int MAX_PERTURBATION_DELAY_TIME_MS = 5000;
    public static final int PING_PORT = 80;

    private String bypassAddress;
    private ConcurrentLinkedQueue<Instant> arrival_times = new ConcurrentLinkedQueue<>();
    
    private WebSocketWrapperPT web_socket_server;
    private ChromeDriver browser;

    private Config config;

    public Proxy(WebSocketWrapperPT web_socket_server){
        try {
            this.web_socket_server = web_socket_server;

            ChromeOptions option = new ChromeOptions();
            option.setAcceptInsecureCerts(true);

            if(!config.getWatchVideo().equals("proxy-client"))
                option.addArguments("headless");

            this.browser = new ChromeDriver(option);
            this.web_socket_server.setBrowser(browser);
            this.web_socket_server.setFirstWindow(browser.getWindowHandle());

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
        int port_streaming = config.getStreaming_port_proxy();
        

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
            try (ServerSocket server = Utilities.getSecureSocketTLS(local_port_secure)) {
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

        //Streaming incoming connection
        new Thread(() -> {
            ExecutorService executor = null;
            try(ServerSocket ss =  Utilities.getSecureSocketTLS(port_streaming)){
                executor = Executors.newFixedThreadPool(N_THREADS);
                System.out.println("Streaming protocol is listening on port " + port_streaming);
                while (true) {
                    Socket socket = ss.accept();
                    executor.execute(() -> doStreaming(socket));
                }
            } catch (IOException ioe) {
                System.err.println("Cannot open the port on Streaming protocol");
                ioe.printStackTrace();
            } finally {
                System.out.println("Closing Streaming protocol server");
            }
        }).start();
    }

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
                web_socket_server.setPipe(pout, sock);
            }

            byte[] buffer = new byte[config.getProxyBufferSize()];
            try {
                pin.read(buffer);
                addJitterPerturbation();
            } catch (IOException | InterruptedException e) {}

            arrival_times.add(Instant.now());

            String filePath = Http.parseHttpReply(new String(buffer))[1];
            System.out.println("File request path: " + filePath + " from " + socket.getInetAddress() + ":" + socket.getPort());
            byte[] data = bypass(filePath);
            
            byte[] bytes = ByteBuffer.allocate(4).putInt(data.length).array();
            byte[] result = new byte[data.length + 4];
            System.arraycopy(bytes, 0, result, 0, 4);
            System.arraycopy(data,0, result, 4, data.length);
            
            for(int i = 0; i < result.length; i += config.getProxyBufferSize()){
                WebSocketWrapperPT.send(Arrays.copyOfRange(result, i, Math.min(result.length,i+config.getProxyBufferSize())), sock); 
            }
        } else{
            Initialization.sendFalied(socket);
        }
    }
    
    private void measureTestHttping(Socket socket) {
        try {
            String local_host = "localhost";
            int tor_buffer_size = config.getTor_buffer_size();
            String remote_host = config.getRemote_host();
            String tor_host = "127.0.0.1";
            int tor_port = config.getTor_port();
            int test_stunnel_port_httping = config.getTest_stunnel_port_httping();

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            out.flush();

            byte[] buffer = new byte[config.getProxyBufferSize()];
            String my_address = local_host;

            if (bypassAddress.split("-")[0].equals(my_address)) {

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
                System.err.println("Test connection :" + my_address + " ---> " + bypassAddress);

                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket socketStunnel = (SSLSocket) factory.createSocket(bypassAddress.split("-")[0], test_stunnel_port_httping);
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
            String local_host = "localhost";
            String remote_host = config.getRemote_host();
            String tor_host = "127.0.0.1";
            int tor_port = config.getTor_port();
            int test_stunnel_port_iperf = config.getTest_stunnel_port_iperf();

            InputStream in = serverSocket.getInputStream();

            byte[] buffer = new byte[tor_buffer_size];
            String my_address = local_host;

            if (bypassAddress.split("-")[0].equals(my_address)) {

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
                System.err.println("Test connection :" + my_address + " ---> " + bypassAddress);

                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket socket = (SSLSocket) factory.createSocket(bypassAddress.split("-")[0], test_stunnel_port_iperf);
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

    private void doTCP_TLS(Socket socket) {
        try {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[config.getProxyBufferSize()];
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
                    byte[] sendData = Arrays.copyOfRange(data, bytesSent, bytesSent + config.getProxyBufferSize()); //prevent sending bytes overflow
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
                    socket.send(sendPacket);
                    bytesSent += config.getProxyBufferSize();
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
            String local_host = "localhost";
            String remote_host = config.getRemote_host();
            String tor_host = "127.0.0.1";
            int tor_port = config.getTor_port();
            List<String> network = config.getNodes(); 
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
            for (String node : network) {
                if (node.equals("localhost") || node.equals("127.0.0.1")) continue;
                SSLSocket socketStunnel = (SSLSocket) factory.createSocket(node.split("-")[0], test_stunnel_port_analytics);
                stunnelSockets.put(node, socketStunnel);
                socketStunnel.startHandshake();
            }

            while ((in.read(buffer, 0, buffer.length)) >= 0) {
                Thread.sleep(15); // Avoid traffic congestion

                //Add perturbation before send to Tor
                addJitterPerturbation();
                arrival_times.add(Instant.now());

                if (bypassAddress.split("-")[0].equals(my_address)) {
                    outTor.write(buffer);
                } else {
                    System.err.println("Test connection :" + my_address + " ---> " + bypassAddress);
                    OutputStream outStunnel = stunnelSockets.get(bypassAddress.split("-")[0]).getOutputStream();
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
        List<String> network = config.getNodes(); 

        bypassAddress = network.get(new Random().nextInt(network.size()));
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
            String local_host = "localhost";
            String remote_host = config.getRemote_host();
            int remote_port = config.getRemote_port();

            String my_address = local_host;
            String[] addr_and_protocol = bypassAddress.split("-");

            if (addr_and_protocol[0].equals(my_address)) {
                return Utilities.torRequest(path, remote_host, remote_port);
            } else {
                System.err.println("Connection :" + my_address + " ---> " + bypassAddress);
                boolean isStreaming = true;
                
                if(!addr_and_protocol[1].equals("s"))
                    isStreaming = false;
                
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

    private byte[] bypassConnectionStremaing(String path) throws Exception{
        String[] addr = bypassAddress.split("-");
        boolean result = Initialization.startHandshake(addr[0], Config.getInstance().getStreaming_port_proxy());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        if(result){
            WebSocket sock = null;
            PipedInputStream pin = new PipedInputStream();
            PipedOutputStream pout = new PipedOutputStream();
            String id_window = null;
            String first_window = null;

            try {
                pout.connect(pin);
            } catch (IOException e) {}
            
            synchronized(this){    
                CountDownLatch connectionWaiter = new CountDownLatch(1);
                web_socket_server.setMutexAndWaitConn(connectionWaiter);
    
                ((JavascriptExecutor) browser).executeScript(
                    "window.open('http://localhost:" + config.getClientPortStreaming() + "/?bridge=" + addr[2] + "');");
    
                try {
                    connectionWaiter.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Set<String> windowHandles = browser.getWindowHandles();
                int x = 0;
                for (String aux : windowHandles){
                    if(x == 0)
                        first_window = aux;
                    id_window = aux;
                    x++;
                }
                   

                sock = web_socket_server.getLaSocket();
                web_socket_server.setPipe(pout, sock);
            }
            
            WebSocketWrapperPT.send(String.format("GET %s HTTP/1.1", path.trim()).getBytes(), sock);         

            int n;
            byte[] buffer = new byte[config.getProxyBufferSize()];
            
            n = pin.read(buffer, 0, buffer.length);
            baos.write(buffer, 4, n-4);

            int num_of_byte_to_rcv = ByteBuffer.wrap(buffer).getInt();
            int num_of_bytes_rcv = n - 4;

            while (num_of_byte_to_rcv != num_of_bytes_rcv) {
                n = pin.read(buffer, 0, buffer.length);
                baos.write(buffer, 0, n);
                num_of_bytes_rcv += n;
            }
  
            sock.close();
            browser.switchTo().window(id_window);
            browser.close();
            browser.switchTo().window(first_window);
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
                (SSLSocket) factory.createSocket(bypassAddress.split("-")[0], Integer.parseInt(stunnel_port));
        socket.startHandshake();
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        out.write(String.format("GET %s HTTP/1.1", path.trim()).getBytes());

        int n;
        byte[] buffer = new byte[config.getProxyBufferSize()];
        while ((n = in.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, n);
        }

        return baos.toByteArray();
    }
}