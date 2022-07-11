package com.afonsovilalonga.Common.Modulators;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.xml.bind.DatatypeConverter;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.openqa.selenium.chrome.ChromeDriver;

import com.afonsovilalonga.Common.Utils.Config;
import com.afonsovilalonga.Common.Utils.TupleWebServer;


public class WebSocketWrapperPT extends WebSocketServer{

    private Map<Integer, TupleWebServer> pipes;

    private CountDownLatch cl;
    private WebSocket lastConn;
    private boolean isProxy;
    private ChromeDriver browser;
    private String first_window;

    public WebSocketWrapperPT(boolean isProxy) {
        super(new InetSocketAddress(Config.getInstance().getWebsocket_port()));
        
        this.isProxy = isProxy;
        this.pipes = new HashMap<>();
        this.cl = null;
        this.lastConn = null;
        this.browser = null;
        this.first_window = null;

        start();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        lastConn = conn;
        cl.countDown();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if(isProxy){
            closeWindow(conn);
        }
        onCloseOrError(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        TupleWebServer tuple = pipes.get(conn.hashCode());
        PipedOutputStream pipe = tuple.getPipe();

        try {
            byte[] recv = decodeBase64(message);
            pipe.write(recv);
            pipe.flush();
        } catch (IOException e) {}
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if(isProxy){
            closeWindow(conn);
        }
        onCloseOrError(conn);
    }

    public void setBrowser(ChromeDriver browser){
        if(isProxy)
            this.browser = browser;
    }

    public void setMutexAndWaitConn(CountDownLatch cl){
        this.cl = cl;
    }

    public void setPipe(PipedOutputStream pipe, WebSocket conn){
        pipes.put(conn.hashCode(), new TupleWebServer(pipe, null));
    }

    public void setPipe(PipedOutputStream pipe, WebSocket conn, String window_id){
        pipes.put(conn.hashCode(), new TupleWebServer(pipe, window_id));
    }

    public WebSocket getLaSocket(){
        return this.lastConn;
    }

    public static void send(byte[] message, WebSocket conn){
        conn.send(encodeBase64(message));
    }

    public byte[] decodeBase64(String base64){
        return DatatypeConverter.parseBase64Binary(base64);
    }

    public static String encodeBase64(byte[] bytes){
        String result = DatatypeConverter.printBase64Binary(bytes);
        return result;
    }

    public void setFirstWindow(String firstWindow){
        if(isProxy)
            this.first_window = firstWindow;
    }

    private void onCloseOrError(WebSocket conn){
        TupleWebServer tuple = pipes.get(conn.hashCode());

        if(conn != null){
            if(tuple.getPipe() != null){
                try {
                    tuple.getPipe().close();
                } catch (IOException e) {}
            }
    
            pipes.remove(conn.hashCode());
        }
            
    }

    private void closeWindow(WebSocket conn){
        if(conn != null){
            TupleWebServer tuple = pipes.get(conn.hashCode());

            if(tuple != null && tuple.getWindownId() != null){
                browser.switchTo().window(tuple.getWindownId());
                browser.close();
                browser.switchTo().window(this.first_window);
            }
        }
    }   
    
}
