package com.afonsovilalonga.Common.Utils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;

import com.afonsovilalonga.Common.Socks.SocksProtocol;

public class Utilities {

    public static byte[] torRequest(byte[] bytes, String remoteAddress, int remotePort) throws IOException {
        String tor_host = Config.getInstance().getTor_ip();
        int tor_port = Config.getInstance().getTor_port();
        int tor_buffer_size = Config.getInstance().getTor_buffer_size();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String req = new String(bytes);

        Socket clientSocket;

        if(req.contains("HEAD")){
            System.out.println("oi");
            clientSocket = SocksProtocol.sendRequest((byte)0x04, remoteAddress, 10001, tor_host, tor_port);
        }else if(!req.contains("http") || !req.contains("HTTP")){
            clientSocket = SocksProtocol.sendRequest((byte)0x04, remoteAddress, 5001, tor_host, tor_port);
        }else{
            clientSocket = SocksProtocol.sendRequest((byte)0x04, remoteAddress, remotePort, tor_host, tor_port);
        }
                
        clientSocket.setReceiveBufferSize(tor_buffer_size);
        clientSocket.setSendBufferSize(tor_buffer_size);
        OutputStream out = clientSocket.getOutputStream();
        out.write(bytes);
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

    public static SSLSocket createSSLSocket(String remote_addr, int port) throws UnknownHostException, IOException {
        SSLSocket bridge_conn = null;

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        bridge_conn = (SSLSocket) factory.createSocket(remote_addr, port);
        bridge_conn.startHandshake();

        return bridge_conn;
    }

    public static ServerSocket getSecureSocketTLS(int port) throws IOException {
        ServerSocketFactory ssf = getServerSocketFactory();
        ServerSocket ss = ssf.createServerSocket(port);
        return ss;
    }

    private static ServerSocketFactory getServerSocketFactory() {
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
}
