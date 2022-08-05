package com.afonsovilalonga;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.afonsovilalonga.Utils.Config;
import com.afonsovilalonga.Utils.Http;
import com.afonsovilalonga.Utils.HttpsClient;

public class HttpServer {
    public static final int PORT = Config.getInstance().getNormal_port();
    static final int MAX_BYTES = 102400000;
    public static final int TEST_PORT = Config.getInstance().getTest_port();
    public static final int ECHO_PORT = Config.getInstance().getEcho_port();

    public static void main(String[] args) throws IOException {

        //TCP
        new Thread(() -> {
            ExecutorService executor = null;
            try (ServerSocket ss = new ServerSocket(PORT)) {
                executor = Executors.newFixedThreadPool(50);
                System.out.println("Http server ready at port " + PORT + " waiting for request ...");
                while (true) {
                    Socket clientSock = ss.accept();
                    System.err.println("New client ---->" + clientSock.getRemoteSocketAddress());
                    executor.execute(() -> {
                        processClientRequest(clientSock);
                        try {
                            clientSock.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                
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

        new Thread(() -> {
            ExecutorService executor = null;
            try (ServerSocket ss = new ServerSocket(TEST_PORT)) {
                executor = Executors.newFixedThreadPool(5);
                System.out.println("Http server ready at port " + TEST_PORT + " waiting for request ...");
                while (true) {
                    Socket clientSock = ss.accept();
                    System.err.println("New client ---->" + clientSock.getRemoteSocketAddress());
                    executor.execute(()-> {
                        try {
                            HttpsClient.httpsRequest(clientSock);
                            clientSock.close();
                        } catch (IOException e) {}
                    });
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

        //echo packets for correlation
        new Thread(() -> {
            ExecutorService executor = null;
            try (ServerSocket ss = new ServerSocket(ECHO_PORT)) {
                executor = Executors.newFixedThreadPool(5);
                System.out.println("Http server ready at port " + ECHO_PORT + " waiting for request ...");
                while (true) {
                    Socket clientSock = ss.accept();
                    System.err.println("New client ---->" + clientSock.getRemoteSocketAddress());
                    // repeatedly wait for connections, and process

                    // open up IO streams
                    InputStream in = clientSock.getInputStream();
                    OutputStream out = clientSock.getOutputStream();

                    // waits for data and reads it in until connection dies
                    // readLine() blocks until the server receives a new line from client
                    int n;
                    byte[] buffer = new byte[clientSock.getReceiveBufferSize()];
                    while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
                    }
                    // close IO streams, then socket
                    System.err.println("Closing connection with client");
                    in.close();
                    clientSock.close();
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
    }


    private static void processClientRequest(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            String line = Http.readLine(in);
            System.out.println("\nGot: \n\n" + line);
            String[] request = Http.parseHttpRequest(line);
            // ignore, but print the header of the http message
            line = Http.readLine(in);
            while (!line.equals("")) {
                System.out.println(line);
                line = Http.readLine(in);
            }
            
            if (request[0].equalsIgnoreCase("GET") || request[0].equalsIgnoreCase("HEAD") && !request[1].equals("")) {
                sendFile(request[1], out);
            } else {
                sendsNotSupportedPage(out);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void sendsNotSupportedPage(OutputStream out)
            throws IOException {
        String page =
                "<HTML><BODY>HTTP server: request not supported</BODY></HTML>";
        int length = page.length();
        String header = "HTTP/1.0 501 Not Implemented\r\n";
        header += "Date: " + new Date().toString() + "\r\n";
        header += "Content-type: text/html\r\n";
        header += "Server: " + "X-HttpServer" + "\r\n";
        header += "XAlmost-Accept-Ranges: bytes\r\n";
        header += "Content-Length: " + length + " \r\n\r\n";
        header += page;
        out.write(header.getBytes());
    }

    /**
     * Sends a simple valid page with the text of the parameter simplePage
     */
    private static void sendsSimplePage(String simplePage, OutputStream out)
            throws IOException {
        String page =
                "<HTML><BODY>HTTP server: " + simplePage + "</BODY></HTML>\r\n";
        int length = page.length();
        String header = "HTTP/1.0 200 OK\r\n";
        header += "Date: " + new Date().toString() + "\r\n";
        header += "Content-type: text/html\r\n";
        header += "Server: " + "X-HttpServer" + "\r\n";
        header += "X-Almost-Accept-Ranges: bytes\r\n";
        header += "Content-Length: " + length + " \r\n\r\n";
        header += page;
        out.write(header.getBytes());
    }

    private static void sendFile(String fileName, OutputStream out)
            throws IOException {
        // strips the leading "/"
        String name = fileName;
        File f = new File(name);
        System.out.println("I will try to send file: \"" + name + "\"");
        if (name.equals("")) sendsSimplePage("The empty name is not a file", out);
        else if (!f.exists()) sendsSimplePage("File \"" + fileName + "\" does not exist", out);
        else if (!f.isFile()) sendsSimplePage("File \"" + fileName + "\" is not a file", out);
        else if (!f.canRead()) sendsSimplePage("File \"" + fileName + "\" cannot be read", out);
        else {
            // we are going to send something
            long fileSize = f.length();
            long rest;
            rest = fileSize;     // never sends more then available
            if (rest > MAX_BYTES) rest = MAX_BYTES; // never sends more then MAX_BYTES

            // rest is negative or 0 if fileSize < ranges[0] or if ranges[1] < ranges[0]
            // rest is <= still available && <= MAX_BYTES && <= demanded
            long size = rest <= 0 ? 0 : rest; // number of bytes to send

            RandomAccessFile file = new RandomAccessFile(f, "r");
            StringBuilder header = new StringBuilder();

            if (size == fileSize) {
                header.append("HTTP/1.0 200 OK\r\n");
                header.append("Date: ").append(new Date().toString()).append("\r\n");
                header.append("Server: " + "X-HttpServer" + "\r\n");
                header.append("Content-type: ").append(getContentType(fileName)).append("\r\n");
            } else { // there are ranges and something to send
                header.append("HTTP/1.0 206 Partial Content\r\n");
                header.append("Date: ").append(new Date().toString()).append("\r\n");
                header.append("Server: " + "X-HttpServer" + "\r\n");
                header.append("Content-type: ").append(getContentType(fileName)).append("\r\n");
                header.append("XAlmost-Accept-Ranges: bytes\r\n");
                header.append("Content-Range: bytes ").append(size - 1).append("/*\r\n"); // "/"+fileSize+
            }
            header.append("Content-Length: ").append(size).append(" \r\n\r\n");
            out.write(header.toString().getBytes());
            // size > 0 since there is something to send
            long bufferSize = (size <= 4096) ? size : 4096;
            byte[] buffer = new byte[(int) bufferSize];
            int totalSent = 0;
            for (; ; ) {
                int n = file.read(buffer, 0, (int) bufferSize);
                if (n == -1) break;
                out.write(buffer, 0, n);
                totalSent += n;
                if (size - totalSent < bufferSize) bufferSize = size - totalSent;
                if (bufferSize == 0) break;
            }
            file.close();
        }
    }

    private static String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html"))
            return "text/html";
        else if (fileRequested.endsWith(".jpeg") || fileRequested.endsWith(".png"))
            return "text/jpeg";
        else
            return "text/plain";
    }
}