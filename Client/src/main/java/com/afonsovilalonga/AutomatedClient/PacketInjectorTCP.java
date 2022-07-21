package com.afonsovilalonga.AutomatedClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import com.afonsovilalonga.Utils.Stats;

public class PacketInjectorTCP {
    public static final String SPACE = " ";
    public static final int TIMEOUT = 10000;
    public static final int NUMBER_REQUEST = 15;
    public static int remote_port_secure = 2000;
    public static int remote_port_unsecure = 1234;
    public static String remote_host = "127.0.0.1"; // 172.28.0.5 or 127.0.0.1;
    public static final int BUF_SIZE = 1024;

    public static final List<String> files =
            List.of("/Files/large"); // "/Files/large", "/Files/small", "/Files/book.pdf",
    public static final List<String> protocols =
            List.of("tcp"); //"tls", "udp", "dtls",

    public static String command = "";

    public static void main(String[] argv) throws Exception {
        //TIRMMRT certificate for server side authentication
        System.setProperty("javax.net.ssl.trustStore", "./keystore/tirmmrts");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        readConfigurationFiles();
        nextCommandTriggeredTimer();

    }

    private static void executeCommand() throws Exception {
        String path = null;
        String protocol = null;
        try {
            String[] input = command.split(SPACE);
            path = input[0];
            protocol = input[1];
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        switch (protocol.toLowerCase()) {
            case "tcp":
                Socket tcp_socket = new Socket(remote_host, remote_port_unsecure);
                do_TCP_TLS(tcp_socket, path);
                tcp_socket.close();
                break;
        }

    }

    private static void nextCommand() {
        Random rand = new Random();
        String pickRandomFile = files.get(rand.nextInt(files.size()));
        String pickRandomProtocol = protocols.get(rand.nextInt(protocols.size()));
        command = pickRandomFile + SPACE + pickRandomProtocol;
        System.err.println("Selected command is: " + command);
    }

    private static void nextCommandTriggeredTimer() throws Exception {
        for (int i = 0; i < NUMBER_REQUEST; i++) {
            Thread.sleep(500);
            nextCommand();
            executeCommand();
        }
    }

    private static void readConfigurationFiles() {

        try (InputStream input = new FileInputStream("./configuration/config.properties")) {
            Properties prop = new Properties();

            prop.load(input);

            remote_host = prop.getProperty("remote_host");
            remote_port_unsecure = Integer.parseInt(prop.getProperty("remote_port_unsecure"));
            remote_port_secure = Integer.parseInt(prop.getProperty("remote_port_secure"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void do_TCP_TLS(Socket socket, String path) throws IOException {
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();
        Stats stats = new Stats();

        out.write(String.format("GET %s HTTP/1.1", path).getBytes());

        int n = 0;
        byte[] buffer = new byte[BUF_SIZE];
        while ((n = in.read(buffer, 0, buffer.length)) != -1) {
            stats.newRequest(n);
            System.out.write(buffer, 0, n);
        }
        stats.printReport();
    }

}