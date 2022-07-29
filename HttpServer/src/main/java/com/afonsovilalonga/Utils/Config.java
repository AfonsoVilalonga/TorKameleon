package com.afonsovilalonga.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private int normal_port;
    private int echo_port;
    private int test_port;


    private static Config instance; 

    public static Config getInstance(){
        if(instance == null)
            instance = new Config();
        return instance;
    }

    public Config(){
        this.readConfigurationFiles();
    }

    public int getNormal_port(){
        return this.normal_port;
    }

    public int getTest_port(){
        return this.test_port;
    }

    public int getEcho_port(){
        return this.echo_port;
    }
    
    private void readConfigurationFiles() {

        try (InputStream input = new FileInputStream("../Config/config.properties")) {
            Properties prop = new Properties();

            prop.load(input);

            this.normal_port = Integer.parseInt(prop.getProperty("normal_port"));
            this.echo_port = Integer.parseInt(prop.getProperty("echo_port"));
            this.test_port = Integer.parseInt(prop.getProperty("test_port"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
