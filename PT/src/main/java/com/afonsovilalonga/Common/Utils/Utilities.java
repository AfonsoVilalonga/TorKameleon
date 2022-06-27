package com.afonsovilalonga.Common.Utils;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class Utilities {

    public static SSLSocket createSSLSocket(String remote_addr, int port) throws UnknownHostException, IOException {
        SSLSocket bridge_conn = null;

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        bridge_conn = (SSLSocket) factory.createSocket(remote_addr, port);
        bridge_conn.startHandshake();

        return bridge_conn;
    }
}
