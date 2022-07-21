package com.afonsovilalonga.Utils;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
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
                String https_url = "https://www.nytimes.com/";
                URL url;
                try {

                    url = new URL(https_url);
                    HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                    System.out.println(con.getResponseMessage());

                    //dump all the content
                    print_content(con, out);

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }

    private static void print_content(HttpsURLConnection con, OutputStream out) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.0 200 OK\r\n");
        header.append("Date: ").append(new Date().toString()).append("\r\n");
        header.append("Server: " + "X-HttpServer" + "\r\n");
        header.append("Content-type: ").append("text").append("\r\n");

        if (con != null) {
            try {
                System.out.println("****** Content of the URL ********");
                BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(con.getInputStream()));

                String input;
                int fileSize = 0;
                while ((input = br.readLine()) != null && fileSize <= 50000) {
                    baos.write(input.getBytes());
                    fileSize += input.getBytes().length;
                }
                header.append("Content-Length: ").append(baos.size() + "\r\n").append("\r\n\r\n");
                out.write(header.toString().getBytes());
                out.flush();
                br.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
