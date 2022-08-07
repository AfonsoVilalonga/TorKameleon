package com.afonsovilalonga.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Date;

public class HttpsClient {

    public static void httpsRequest(Socket socket) throws IOException {
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
            System.out.println();
            if (request[0].equalsIgnoreCase("HEAD") && !request[1].equals("")) {

                // url = new URL(https_url);
                // HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                // System.out.println(con.getResponseMessage());

                // dump all the content
                print_content(out);

            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }

    private static void print_content(OutputStream out) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.0 200 OK\r\n");
        header.append("Date: ").append(new Date().toString()).append("\r\n");
        header.append("Server: " + "X-HttpServer" + "\r\n");
        header.append("Content-type: ").append("text").append("\r\n");

        header.append("Content-Length: ").append(baos.size() + "\r\n").append("\r\n\r\n");
        try {
            out.write(header.toString().getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
