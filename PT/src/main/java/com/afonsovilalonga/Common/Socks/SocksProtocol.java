package com.afonsovilalonga.Common.Socks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import com.afonsovilalonga.Common.Socks.Exceptions.SocksException;

/**
 * Has the necessary implementation for a Server Side SocksV5 protocol (used for the TOR PT when establishing 
 *  the connection with TOR); 
 * Also has the client implementation for SOCKSV5, SOCKVSV4a
 * Inspired by the goptlib lib written in go 
 */
public class SocksProtocol {
    public static final byte VERSION_5 = 0x05;
    public static final byte VERSION_4 = 0x04;

    public static final byte NO_AUTH = 0x00;
    public static final byte USER_PASS = 0x02;
    public static final byte NO_ACCEPT_METHOD = -1;

    public static final byte AUTH_SUCCESS = 0x00;
    public static final byte AUTH_FAILED = 0x01;

    public static final byte CONNECT = 0x01;

    public static final byte RESERVED = 0x00;

    public static final byte IPV4 = 0x01;
    public static final byte IPV6 = 0x04;
    public static final byte DOMAIN = 0x03;

    // Used in SOCKS PROTOCOL
    public static final byte SOCK_SUCCESS = 0x00;
    public static final byte SOCK_GENERAL_FAILURE = 0x01;
    public static final byte COMMAND_NOT_SUPPORTED = 0x07;
    public static final byte ADDR_TYPE_NOT_SUPPORTED = 0x08;

    // Other errors called by the pluggable transport
    public static final byte CONN_NOT_ALLOWED = 0x02;
    public static final byte NETWORK_UNREACHABLE = 0x04;
    public static final byte CONN_REFUSED = 0x05;
    public static final byte TTL_EXPIRED = 0x06;

    //SOCSKV4 RESP VERSION
    public static final byte VERSION_RESP = 0x00;
    
    //SOCKSV4 RESP GRANTED
    public static final byte RESP_GRANTED = 0x5a;

    //SOCKSV4 CMDS
    public static final byte TCP_STREAM = 0x01;

    private Socket SOCKSV5;

    private SocksReq sockReq;

    public SocksProtocol() {
        this.SOCKSV5 = null;
        this.sockReq = new SocksReq();
    }

    /**
     * Accepts connections, server side in socksV5 protocol
     * @param port port for the connection
     * @throws SocksException If failed connection
     */
    public void acceptConns(ServerSocket server) throws SocksException  {
        Socket conn = null;
        try {
            conn = server.accept();
            this.SOCKSV5 = conn;
            this.socks5Handshake(conn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 
     * Send to client rejected connection with error code
    */
    public void sendSocksResponseRejected(byte err) {
        this.sendSockResponse(err);
    }

    /**
     * Send to client accepted connection
     */
    public void sendSocksResponseAccepted() {
        this.sendSockResponse(SOCK_SUCCESS);
    }

    /**
     * Socks request contains all the information about the client request (addr, auth method, addr type)
     * @return socks request
     */
    public SocksReq getReq() {
        return this.sockReq;
    }

    /**
     * The socket to the client 
     * @return socket
     */
    public Socket getSocks() {
        return this.SOCKSV5;
    }

    /**
     * Close the connection and the serve socket 
     */
    private void close() {
        try {
            this.SOCKSV5.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends to the client a Socks response with a given code of error or success
     * @param code error code or success code 
     */
    private void sendSockResponse(byte code) {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(this.SOCKSV5.getOutputStream()));
            out.writeByte(VERSION_5);
            out.writeByte(code);
            out.writeByte(RESERVED);
            out.writeByte(this.sockReq.getAddrType());

            int len = 4;
            if (this.sockReq.getAddrType() == IPV6)
                len = 16;

            if (this.sockReq.getAddrType() == DOMAIN)
                len = this.sockReq.getAddr().getBytes().length;

            for (int i = 0; i < len; i++)
                out.writeByte(0);

            out.writeShort(0);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initiates the socksv5 handshake 
     * @param conn connection to the client 
     * @throws IOException 
     * @throws SocksException
     */
    private void socks5Handshake(Socket conn) throws IOException, SocksException{
        DataInputStream in = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(conn.getOutputStream()));

        int result = this.socksNegotiateAuth(in, out);
        if (result != 0) {
            this.close();
            throw new SocketException("Socks initial negotiation failed.");
        }

        if (this.socksAuthenticate(this.sockReq.getMethod(), in, out) != 0) {
            this.close();
            throw new SocketException("Socks authentication failed.");
        }

        if (this.socksReadCommand(in) != 0) {
            this.close();
            throw new SocketException("Socks read command failed.");
        }
    }

    /**
     * Negotiating the authentication method. Only implemented no auth since we do not authenticate with client
     * @param in
     * @param out
     * @return status, 0 success and -1 error
     * @throws IOException
     */
    private int socksNegotiateAuth(DataInputStream in, DataOutputStream out) throws IOException {
        byte version = in.readByte();
        if (!this.verifyByte(version, VERSION_5)) {
            return -1;
        }

        byte len_methods = in.readByte();

        // Choose the most suitable method, if the user sends both no method
        // and user pass, choose no auth
        byte[] methods = in.readNBytes(len_methods);
        byte method = -1;
        for (int i = 0; i < methods.length; i++) {
            switch (methods[i]) {
                case USER_PASS:
                    if (NO_AUTH != method)
                        method = methods[i];
                    break;

                case NO_AUTH:
                    method = methods[i];
                    break;

                default:
                    method = NO_ACCEPT_METHOD;
            }
        }

        // Write response message with socks version and choosen method
        out.writeByte(VERSION_5);
        out.writeByte(method);
        out.flush();

        if (method == NO_ACCEPT_METHOD)
            return -1;

        this.sockReq.setMethod(method);
        return 0;
    }

    /**
     * Type of method choosen by the server with regards to the methods suported by the client. In this case our server
     *          only accepts no auth.
     * @param method method type
     * @param in
     * @param out
     * @return status, 0 success and -1 error
     */
    private int socksAuthenticate(byte method, DataInputStream in, DataOutputStream out) {
        switch (method) {
            case NO_AUTH:
                return 0;

            case NO_ACCEPT_METHOD:
                System.err.println("SOCKS methods had no compatible method with server");
                return -1;

            default:
                System.err.println("Invalid method");
                return -1;
        }
    }

    /**
     * Command phase of socksV5 handshake, only command supported is CONNECT. 
     * @param in
     * @return status, 0 success and -1 error
     * @throws IOException
     */
    private int socksReadCommand(DataInputStream in) throws IOException {
        byte version = in.readByte();
        if (!this.verifyByte(version, VERSION_5)) {
            sendSocksResponseRejected(SOCK_GENERAL_FAILURE);
            return -1;
        }

        byte cmd = in.readByte();
        if (!this.verifyByte(cmd, CONNECT)) {
            sendSocksResponseRejected(COMMAND_NOT_SUPPORTED);
            return -1;
        }

        byte reserved = in.readByte();
        if (!this.verifyByte(reserved, RESERVED)) {
            sendSocksResponseRejected(SOCK_GENERAL_FAILURE);
            return -1;
        }

        byte atyp = in.readByte();
        if (!this.verifyByte(atyp, IPV4) && !this.verifyByte(atyp, IPV6)
                && !this.verifyByte(atyp, DOMAIN)) {

            sendSocksResponseRejected(ADDR_TYPE_NOT_SUPPORTED);
            return -1;
        }

        this.sockReq.setAddrType(atyp);

        switch (atyp) {
            case IPV4:
                String ipv4 = this.getIPV4(in);
                if(ipv4 == null){
                    sendSocksResponseRejected(SOCK_GENERAL_FAILURE);
                    return -1;
                }
                this.sockReq.setAdrr(ipv4);
                break;

            case IPV6:
                String ipv6 = this.getIPV6(in);
                if(ipv6 == null){
                    sendSocksResponseRejected(SOCK_GENERAL_FAILURE);
                    return -1;
                }
                this.sockReq.setAdrr(ipv6);
                break;

            case DOMAIN:
                String domain = this.getDomain(in);
                if(domain == null){
                    sendSocksResponseRejected(SOCK_GENERAL_FAILURE);
                    return -1;
                }
                this.sockReq.setAdrr(domain);
                break;

            default:
                sendSocksResponseRejected(SOCK_GENERAL_FAILURE);
                break;
        }

        int port = (in.readShort() & 0xffff);
        this.sockReq.setPort(port);
        return 0;
    }
    /**
     * Aux method to map byte[] with domain to string with domain
     * @param in
     * @return domain or null
     */
    private String getDomain(DataInputStream in) {
        try {
            int addressLength = in.readByte();
            if(addressLength <= 0)
                return null;
            
            StringBuilder aux = new StringBuilder();
            for (int i = 0; i < addressLength; ++i) {
                aux.append((char) in.readByte());
            }
            return aux.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Aux method to map byte[] with IPV4 to string with IPV4
     * @param in
     * @return IPV4 or null
     */
    private String getIPV4(DataInputStream in) {
        try {
            byte[] addr = in.readNBytes(4);
            StringBuilder aux = new StringBuilder();
            for (int i = 0; i < addr.length; i++) {
                aux.append(Byte.toUnsignedInt(addr[i]));

                if (i != addr.length - 1)
                    aux.append(".");
            }
            return aux.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Aux method to map byte[] with IPV6 to string with IPV6
     * @param in
     * @return IPV6 or null
     */
    private String getIPV6(DataInputStream in) {
        try {
            StringBuilder aux = new StringBuilder();
            for (int i = 0; i < 16; ++i) {

                if (i != 0 && i % 2 == 0)
                    aux.append(":");

                aux.append(String.valueOf(Byte.toUnsignedInt(in.readByte())));
            }
            return aux.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Aux method to verify a given received byte with the expected byte 
     * @param by
     * @param expected
     * @return true if byte is expected otherwise false
     */
    private boolean verifyByte(byte by, byte expected) {
        if (by != expected) {
            System.err.println("Socks message field was " + by + " not " + expected);
            return false;
        }

        return true;
    }
    
    //CLIENT SIDE METHODS
    public static Socket sendRequest(byte version, String remote_host, int remote_port, String tor_host, int tor_port){
        switch(version){
            case VERSION_4:
                return socksv4SendRequest(remote_host, remote_port, tor_host, tor_port);
            case VERSION_5:
                return socksv5SendRequest(remote_host, remote_port, tor_host, tor_port);
            default:
                return null;
        }
    }

    private static Socket socksv5SendRequest(String remote_host, int remote_port, String tor_host, int tor_port){
        Socket socket = new Socket();   

        try{
            socket.connect(new InetSocketAddress(tor_host, tor_port));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            out.writeByte(VERSION_5);
            out.writeByte(0x01);
            out.writeByte(NO_AUTH);

            out.flush();

            DataInputStream in = new DataInputStream(socket.getInputStream());
            byte version = in.readByte();
            byte choosen = in.readByte();

            if(version != VERSION_5 || choosen != 0x00){
                socket.close();
                return null;
            }            

            out.writeByte(VERSION_5);
            out.writeByte(CONNECT);
            out.writeByte(RESERVED);

            out.writeByte(IPV4);
            out.write(remote_host.getBytes());

            out.writeShort((short) remote_port);

            out.flush();

            version = in.readByte();
            byte status = in.readByte();
            byte reserverd = in.readByte();
            
            if(version != VERSION_5 || status != SOCK_SUCCESS || reserverd != RESERVED){
                socket.close();
                return null;
            }

            in.readByte();
            in.readInt();

            in.readShort();
            
            return socket;

        } catch(IOException e){
            e.printStackTrace();
        }

        return null;
    }   

    private static Socket socksv4SendRequest(String remote_host, int remote_port,  String tor_host, int tor_port){
        Socket socket = new Socket();   
        try { 
            socket.connect(new InetSocketAddress(tor_host, tor_port));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            
            out.writeByte(VERSION_4);
            out.writeByte(TCP_STREAM);
            out.writeShort((short) remote_port);

	        for (int i = 0; i < 3; i++){  
                out.writeByte(0x00);
            }
            out.writeByte(0x01);

            out.writeByte(0x00);
            out.write(remote_host.getBytes());
            out.writeByte(0x00);

            out.flush();

            DataInputStream in = new DataInputStream(socket.getInputStream());
            
            byte version = in.readByte();
            byte resp = in.readByte();

            if(version != VERSION_RESP || resp != RESP_GRANTED){
                socket.close();
                return null;
            }

            in.readShort();
            in.readInt();
            
            return socket;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    } 

}