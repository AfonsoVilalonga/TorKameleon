package com.afonsovilalonga.Common.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class Config {
    private String local_host;
    private int local_port_unsecure;
    private int local_port_secure;

    private String remote_host;
    private int remote_port;

    private String tor_host;
    private int tor_port;

    private int bypass_timer;

    private List<String> tirmmrt_network;

    private String stunnel_port;

    private int tor_buffer_size;
    private int test_port_iperf;
    private int test_stunnel_port_iperf;

    private int test_port_httping;
    private int test_stunnel_port_httping;

    private int test_port_analytics;
    private int test_stunnel_port_analytics;

    private int number_of_tirmmrt;

    private int pt_client_port;
    private int pt_server_port;
    private String pt_server_host;
    private int or_port;

    private String keystore;
    private String password;
    private String key;

    private String mod;

    private String client_streaming;
    private String client_streaming_port;

    private String bridge_streaming;
    private String bridge_streaming_port;

    private String webdriver_location;

    private static Config instance; 

    public static Config getInstance(){
        if(instance == null)
            instance = new Config();
        return instance;
    }

    public Config(){
        try {
            this.readConfigurationFiles();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getWebdriverLocation() {
        return webdriver_location;
    }

    public String getClientLocationStreaming() {
        return client_streaming;
    }

    public String getBridgeLocationStreaming() {
        return bridge_streaming;
    }

    public String getBridgePortStreaming() {
        return bridge_streaming_port;
    }
    
    public String getClientPortStreaming() {
        return client_streaming_port;
    }
    
    
    public String getLocal_host() {
        return local_host;
    }

    public String getModulation() {
        return mod;
    }

    public String getKey() {
        return key;
    }

    public String getKeystore() {
        return keystore;
    }

    public String getPTServerHost() {
        return pt_server_host;
    }

    public String getPassword() {
        return password;
    }

    public int getOrPort() {
        return or_port;
    }

    public int getLocal_port_unsecure() {
        return local_port_unsecure;
    }

    public int getLocal_port_secure() {
        return local_port_secure;
    }

    public String getRemote_host() {
        return remote_host;
    }

    public int getRemote_port() {
        return remote_port;
    }

    public String getTor_host() {
        return tor_host;
    }

    public int getTor_port() {
        return tor_port;
    }

    public int getBypass_timer() {
        return bypass_timer;
    }

    public List<String> getTirmmrt_network() {
        return tirmmrt_network;
    }

    public String getStunnel_port() {
        return stunnel_port;
    }

    public int getTor_buffer_size() {
        return tor_buffer_size;
    }

    public int getTest_port_iperf() {
        return test_port_iperf;
    }

    public int getTest_stunnel_port_iperf() {
        return test_stunnel_port_iperf;
    }

    public int getTest_port_httping() {
        return test_port_httping;
    }

    public int getTest_stunnel_port_httping() {
        return test_stunnel_port_httping;
    }

    public int getTest_port_analytics() {
        return test_port_analytics;
    }

    public int getTest_stunnel_port_analytics() {
        return test_stunnel_port_analytics;
    }

    public int getNumber_of_tirmmrt() {
        return number_of_tirmmrt;
    }

    public int getPt_client_port() {
        return pt_client_port;
    }

    public int getPt_server_port() {
        return pt_server_port;
    }

    private void readConfigurationFiles() throws FileNotFoundException {
        tirmmrt_network = new ArrayList<>();

        try (InputStream input = new FileInputStream("../Config/config.properties")) {
            Properties prop = new Properties();

            prop.load(input);

            //TIR VARS
            // local_host = prop.getProperty("local_host");
            // local_port_unsecure = Integer.parseInt(prop.getProperty("local_port_unsecure"));
            // local_port_secure = Integer.parseInt(prop.getProperty("local_port_secure"));
            // remote_host = prop.getProperty("remote_host");
            // remote_port = Integer.parseInt(prop.getProperty("remote_port"));
            // tor_host = prop.getProperty("tor_host");
            // tor_port = Integer.parseInt(prop.getProperty("tor_port"));
            // stunnel_port = prop.getProperty("stunnel_port");
            // bypass_timer = Integer.parseInt(prop.getProperty("bypass_timer"));
            // test_port_iperf = Integer.parseInt(prop.getProperty("test_port_iperf"));
            // test_stunnel_port_iperf = Integer.parseInt(prop.getProperty("test_stunnel_port_iperf"));
            // test_port_httping = Integer.parseInt(prop.getProperty("test_port_httping"));
            // test_stunnel_port_httping = Integer.parseInt(prop.getProperty("test_stunnel_port_httping"));
            // number_of_tirmmrt = Integer.parseInt(prop.getProperty("number_of_tirmmrt"));
            // tor_buffer_size = Integer.parseInt(prop.getProperty("tor_buffer_size"));
            // test_port_analytics = Integer.parseInt(prop.getProperty("test_port_analytics"));
            // test_stunnel_port_analytics = Integer.parseInt(prop.getProperty("test_stunnel_port_analytics"));

            //PT CLIENT VARS
            pt_client_port = Integer.parseInt(prop.getProperty("pt_client_port"));
            pt_server_port = Integer.parseInt(prop.getProperty("pt_server_port"));
            pt_server_host = prop.getProperty("pt_server_host");
            or_port = Integer.parseInt(prop.getProperty("or_port"));

            keystore = prop.getProperty("keystore");
            password = prop.getProperty("password");
            key = prop.getProperty("key");

            mod = prop.getProperty("modulation");

            client_streaming = prop.getProperty("clientstreaming");
            bridge_streaming = prop.getProperty("bridgestreaming");

            client_streaming_port = prop.getProperty("client_streaming_port");
            bridge_streaming_port = prop.getProperty("bridge_streaming_port");

            webdriver_location = prop.getProperty("webdriver_location");

        } catch (IOException e) {
            e.printStackTrace();
        }
        // File file = new File("./configuration/TIR-MMRT_network");
        // Scanner sc = new Scanner(file);
        // int tirmmrts = 0;
        // while (sc.hasNextLine() && tirmmrts < number_of_tirmmrt) {
        //     tirmmrt_network.add(sc.nextLine());
        //     tirmmrts++;
        // }
        // sc.close();
    }
}
