package com.afonsovilalonga.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private int remote_port_secure;
    private int remote_port_unsecure;

    private int command_max_timer;
    private int command_min_timer;

    private String remote_host; 
    private String keystore;
    private String password;
    private String key;

    private String protocols;
    private String files;

    private static Config instance; 

    public static Config getInstance(){
        if(instance == null)
            instance = new Config();
        return instance;
    }

    public Config(){
        this.readConfigurationFiles();
    }

    public int getRemote_port_secure(){
        return this.remote_port_secure;
    }

    public int getRemote_port_unsecure(){
        return this.remote_port_unsecure;
    }

    public int get_command_min_timer(){
        return this.command_min_timer;
    }

    public int get_command_max_timer(){
        return this.command_max_timer;
    }

    public String get_remote_host(){
        return this.remote_host;
    }

    public String get_keystore(){
        return this.keystore;
    }

    public String get_keystore_password(){
        return this.password;
    }

    public String get_key(){
        return this.key;
    }

    public String get_protocols(){
        return this.protocols;
    }

    public String get_files(){
        return this.files;
    }

    private void readConfigurationFiles() {

        try (InputStream input = new FileInputStream("../Config/config.properties")) {
            Properties prop = new Properties();

            prop.load(input);

            this.remote_host = prop.getProperty("remote_host");
            this.remote_port_unsecure = Integer.parseInt(prop.getProperty("remote_port_unsecure"));
            this.remote_port_secure = Integer.parseInt(prop.getProperty("remote_port_secure"));

            this.password = prop.getProperty("password");
            this.keystore = prop.getProperty("keystore");
            this.key = prop.getProperty("key");

            this.command_max_timer = Integer.parseInt(prop.getProperty("COMMAND_MAX_TIMER"));
            this.command_min_timer = Integer.parseInt(prop.getProperty("COMMAND_MIN_TIMER"));

            this.files = prop.getProperty("files");
            this.protocols = prop.getProperty("protocols");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
