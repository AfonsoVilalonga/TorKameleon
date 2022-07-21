package com.afonsovilalonga;

import com.afonsovilalonga.AutomatedClient.AutomatedClient;
import com.afonsovilalonga.InteractiveClient.InteractiveClient;
import com.afonsovilalonga.Utils.Config;

public class Client {
    
    public static void main(String[] args){
        Config config = Config.getInstance();
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStore", config.get_keystore());
        System.setProperty("javax.net.ssl.trustStorePassword", config.get_keystore_password());

        if(args[0].equals("interactive")){
            try {
                InteractiveClient.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            for(int i = 0; i < Integer.parseInt(args[1]); i++){
                Thread t = new Thread(new AutomatedClient());
                t.start();
            }
        }
    }

}

