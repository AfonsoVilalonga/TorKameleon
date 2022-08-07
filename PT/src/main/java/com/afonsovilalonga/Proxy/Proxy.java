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
    public static final int PERTURBATION_DELAY_PERCENTAGE = 20;
    public static final int MAX_PERTURBATION_DELAY_TIME_MS = 5000;

    public static final int PING_PORT = 80;

    public static final byte NORMAL = 0x00;
    public static final byte IPERF = 0X02;

    private String bypassAddress;
    private ConcurrentLinkedQueue<Instant> arrival_times = new ConcurrentLinkedQueue<>();

    private WebSocketWrapperPT web_socket_server;
    private ChromeDriver browser;

    private Config config;

    public Proxy(WebSocketWrapperPT web_socket_server) {
        try {
            this.config = Config.getInstance();
            this.web_socket_server = web_socket_server;

            ChromeOptions option = new ChromeOptions();
            option.setAcceptInsecureCerts(true);
            option.addArguments("--silent");
            option.addArguments("--log-level=3");
            option.addArguments("--no-sandbox");

            if (!config.getWatchVideo().equals("proxy-client"))
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

        // DTLS
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
            ExecutorService executor = null;
            try (ServerSocket serverSocket = new ServerSocket(test_port_iperf)) {
                executor = Executors.newFixedThreadPool(N_THREADS);
                System.out.println("Iperf proxy is listening on port " + test_port_iperf);
                while (true) {
                    Socket socket = serverSocket.accept();
                    executor.execute(() -> measureTestIperf(socket));
                    socket.close();
                }
            } catch (IOException ex) {
                System.out.println("Iperf exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();

        // Streaming incoming connection
        new Thread(() -> {
            ExecutorService executor = null;
            try (ServerSocket ss = Utilities.getSecureSocketTLS(port_streaming)) {
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

    private void doStreaming(Socket socket) {
        byte val = Initialization.serverHandshake(socket);

        if (val != Initialization.ACK_FAILED) {
            WebSocket sock = null;
            PipedInputStream pin = new PipedInputStream();
            PipedOutputStream pout = new PipedOutputStream();

            try {
                pout.connect(pin);
            } catch (IOException e) {
            }

            synchronized (this) {
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

            Initialization.sendAccept(socket);

            // REVER
            byte[] buffer = new byte[config.getProxyBufferSize()];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte type = 0x00;

            try {
                int n = pin.read(buffer);
                ByteBuffer buf = ByteBuffer.wrap(buffer);
                type = buf.get();
                int num_of_byte_to_rcv = buf.getInt();
                baos.write(buffer, 5, n - 5);

                int num_of_bytes_rcv = n - 5;

                while (num_of_byte_to_rcv != num_of_bytes_rcv) {
                    n = pin.read(buffer, 0, buffer.length);
                    baos.write(buffer, 0, n);
                    num_of_bytes_rcv += n;
                }

                // addJitterPerturbation();
            } catch (IOException e) {}

            if (type == NORMAL) {
                byte[] data = bypass(baos.toByteArray());

                byte[] bytes = ByteBuffer.allocate(4).putInt(data.length).array();
                byte[] result = new byte[data.length + 4];
                System.arraycopy(bytes, 0, result, 0, 4);
                System.arraycopy(data, 0, result, 4, data.length);

                for (int i = 0; i < result.length; i += config.getProxyBufferSize()) {
                    WebSocketWrapperPT.send(
                            Arrays.copyOfRange(result, i, Math.min(result.length, i +
                                    config.getProxyBufferSize())),
                            sock);
                }
            } else if (type == IPERF) {
                try {
                    Socket socket_iperf = new Socket("localhost",
                            Config.getInstance().getTest_port_iperf());
                    socket_iperf.getOutputStream().write(baos.toByteArray());
                    socket_iperf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Initialization.sendFalied(socket);
        }
    }

    private void measureTestIperf(Socket serverSocket) {
        try {
            int tor_buffer_size = config.getTor_buffer_size();
            String local_host = "localhost";
            String remote_host = config.getRemote_host();
            String tor_host = config.getTor_ip();
            int tor_port = config.getTor_port();
            int stunnel_iperf = config.getStunnel_iperf();

            InputStream in = serverSocket.getInputStream();
            OutputStream out = serverSocket.getOutputStream();

            byte[] buffer = new byte[tor_buffer_size];
            String my_address = local_host;

            String[] addr_array = bypassAddress.split("-");
            ExecutorService executor;

            if (addr_array[0].equals(my_address)) {
                Socket clientSocket = SocksProtocol.sendRequest((byte) 0x04, remote_host, 10005, tor_host, tor_port);
                OutputStream out_tor = clientSocket.getOutputStream();
                InputStream in_tor = clientSocket.getInputStream();
                out_tor.flush();
                
                executor = Executors.newFixedThreadPool(2);
                
                executor.execute(()->{
                    try {
                        int n = 0;
                        while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
                            out_tor.write(buffer, 0 , n);
                            out_tor.flush();
                        }
                        out_tor.close();
                        in.close();
                    } catch (IOException e) {}
                });

                executor.execute(()->{
                    try {
                        int n = 0;
                        while ((n = in_tor.read(buffer, 0, buffer.length)) >= 0) {
                            out.write(buffer, 0 , n);
                            out.flush();
                        }
                        in_tor.close();
                        out.flush();
                    } catch (IOException e) {}
          
                });

            } else {
                if (addr_array[1].equals("s")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int n;

                    while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
                        baos.write(buffer, 0, n);
                        Thread.sleep(5);
                    }

                    bypassConnectionStremaing(baos.toByteArray(), IPERF);
                } else {
                    System.err.println("Test connection :" + my_address + " ---> " + bypassAddress);

                    SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    SSLSocket socket = (SSLSocket) factory.createSocket(bypassAddress.split("-")[0], stunnel_iperf);
                    socket.startHandshake();
                    OutputStream out_aux = socket.getOutputStream();
                    out_aux.flush();

                    int n;
                    while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
                        System.err.println("sending iperf to" + bypassAddress + ":" + new String(buffer));
                        out_aux.write(buffer, 0, n);
                        Thread.sleep(5); // Avoid traffic congestion
                    }
                    out_aux.flush();
                    socket.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private synchronized void addJitterPerturbation() throws InterruptedException {
        if (arrival_times == null || arrival_times.isEmpty())
            return;
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
            int i = in.read(buffer);

            System.out.println("Request from " + socket.getInetAddress() + ":" + socket.getPort());

            String req = new String(buffer);

            byte[] data = bypass(Arrays.copyOfRange(buffer, 0, i));
            out.write(data, 0, data.length);

            if (req.contains("HEAD")) {
                out.write("\r\n\r\n".getBytes());
            }
            out.flush();

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doUDP(DatagramSocket socket) throws Exception {
        // receive
        byte[] buf = new byte[socket.getReceiveBufferSize()];
        DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
        socket.receive(receivePacket);
        String filePath = new String(buf, StandardCharsets.UTF_8);
        System.out.println("File request path: " + filePath + " from " + receivePacket.getAddress() + ":"
                + receivePacket.getPort());

        // Add perturbation before send to Tor
        addJitterPerturbation();
        arrival_times.add(Instant.now());
        byte[] data = bypass(filePath.getBytes());

        // send
        ExecutorService executor;
        executor = Executors.newFixedThreadPool(N_THREADS);
        executor.execute(() -> {
            try {
                int bytesSent = 0;
                while (bytesSent <= data.length) {
                    byte[] sendData = Arrays.copyOfRange(data, bytesSent, bytesSent + config.getProxyBufferSize()); // prevent
                                                                                                                    // sending
                                                                                                                    // bytes
                                                                                                                    // overflow
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                            receivePacket.getAddress(), receivePacket.getPort());
                    socket.send(sendPacket);
                    bytesSent += config.getProxyBufferSize();
                    Thread.sleep(5); // Avoid traffic congestion
                }
                byte[] endTransmission = "terminate_packet_receive".getBytes();
                socket.send(new DatagramPacket(endTransmission, endTransmission.length, receivePacket.getAddress(),
                        receivePacket.getPort()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void doDTLS(DatagramSocket socket) {
        try {
            // Handshake and receive
            DTLSOverDatagram dtls = new DTLSOverDatagram();
            SSLEngine engine = dtls.createSSLEngine(false);
            InetSocketAddress isa = dtls.handshake(engine, socket, null, "Server");
            String filePath = dtls.receiveAppData(engine, socket);

            // Add perturbation before send to Tor
            addJitterPerturbation();
            arrival_times.add(Instant.now());

            byte[] data = bypass(filePath.getBytes());
            // deliver up to nThread clients
            ExecutorService executor = null;
            executor = Executors.newFixedThreadPool(N_THREADS);
            executor.execute(() -> dtls.deliverAppData(engine, socket, ByteBuffer.wrap(data), isa));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unable to do DTLS");
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

    private byte[] bypass(byte[] bytes) {
        try {
            String local_host = "localhost";
            String remote_host = config.getRemote_host();
            int remote_port = config.getRemote_port();

            String my_address = local_host;
            String[] addr_and_protocol = bypassAddress.split("-");

            if (addr_and_protocol[0].equals(my_address)) {
                return Utilities.torRequest(bytes, remote_host, remote_port);
            } else {
                System.err.println("Connection :" + my_address + " ---> " + bypassAddress);
                boolean isStreaming = true;

                if (!addr_and_protocol[1].equals("s"))
                    isStreaming = false;

                if (isStreaming) {
                    return bypassConnectionStremaing(bytes, NORMAL);
                } else {
                    return bypassConnection(bytes);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] bypassConnectionStremaing(byte[] bytes, byte type) throws Exception {
        String[] addr = bypassAddress.split("-");
        boolean result = Initialization.startHandshake(addr[0], Config.getInstance().getConnect_streaming());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (result) {
            WebSocket sock = null;
            PipedInputStream pin = new PipedInputStream();
            PipedOutputStream pout = new PipedOutputStream();
            String id_window = null;
            String first_window = null;

            try {
                pout.connect(pin);
            } catch (IOException e) {
            }

            synchronized (this) {
                CountDownLatch connectionWaiter = new CountDownLatch(1);
                web_socket_server.setMutexAndWaitConn(connectionWaiter);

                ((JavascriptExecutor) browser).executeScript(
                        "window.open('http://localhost:" + config.getClientPortStreaming() + "/?bridge=" + addr[2]
                                + "');");

                try {
                    connectionWaiter.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Set<String> windowHandles = browser.getWindowHandles();
                int x = 0;
                for (String aux : windowHandles) {
                    if (x == 0)
                        first_window = aux;
                    id_window = aux;
                    x++;
                }

                sock = web_socket_server.getLaSocket();
                web_socket_server.setPipe(pout, sock);
            }

            // Send packet with type (iperf, httping or normal); len of packet and the data
            byte[] bytes_len = ByteBuffer.allocate(4).putInt(bytes.length).array();
            byte[] type_bytes = ByteBuffer.allocate(1).put(type).array();
            byte[] bytes_with_len = new byte[bytes.length + 4 + 1];

            System.arraycopy(type_bytes, 0, bytes_with_len, 0, 1);
            System.arraycopy(bytes_len, 0, bytes_with_len, 1, 4);
            System.arraycopy(bytes, 0, bytes_with_len, 5, bytes.length);

            int bytes_to_send = 0;

            System.out.println(bytes.length + " " + bytes_to_send);

            // send from bytes_to_send to (bytes_to_send + the minimum between the data to
            // send length and the size of packets sent between proxys)
            do {
                WebSocketWrapperPT.send(Arrays.copyOfRange(bytes_with_len, bytes_to_send,
                        bytes_to_send + Math.min(config.getProxyBufferSize(), bytes_with_len.length)), sock);
                bytes_to_send = bytes_to_send + Math.min(config.getProxyBufferSize(), bytes_with_len.length);
            } while (bytes_to_send < bytes_with_len.length);

            if (type != IPERF) {
                // Receive packets from the stream
                int n;
                byte[] buffer = new byte[config.getProxyBufferSize()];

                n = pin.read(buffer, 0, buffer.length);
                baos.write(buffer, 4, n - 4);

                int num_of_byte_to_rcv = ByteBuffer.wrap(buffer).getInt();
                int num_of_bytes_rcv = n - 4;

                while (num_of_byte_to_rcv != num_of_bytes_rcv) {
                    n = pin.read(buffer, 0, buffer.length);
                    baos.write(buffer, 0, n);
                    num_of_bytes_rcv += n;
                }
            }

            synchronized (this) {
                sock.close();
                browser.switchTo().window(id_window);
                browser.close();
                browser.switchTo().window(first_window);
            }

        } else {
            System.err.println("Error while handshaking with the brdige using the Streaming protocol");
        }

        return baos.toByteArray();
    }

    private byte[] bypassConnection(byte[] bytes) throws Exception {
        String stunnel_port = config.getStunnel_port();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) factory.createSocket(bypassAddress.split("-")[0],
                Integer.parseInt(stunnel_port));
        socket.startHandshake();
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        out.write(bytes);

        int n;
        byte[] buffer = new byte[config.getProxyBufferSize()];
        while ((n = in.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, n);
        }

        return baos.toByteArray();
    }
}