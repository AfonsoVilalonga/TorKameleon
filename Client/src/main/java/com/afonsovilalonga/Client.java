package com.afonsovilalonga;
{[
public class Client {
    
    public static void main(String[] args){
        System.setProperty("javax.net.ssl.trustStore", "./keystore/tirmmrts");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");


    }

}

