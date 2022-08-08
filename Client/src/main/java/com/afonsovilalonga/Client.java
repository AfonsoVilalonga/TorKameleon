package com.afonsovilalonga;

import com.afonsovilalonga.AutomatedClient.AutomatedClient;
import com.afonsovilalonga.AutomatedClient.Injector;
import com.afonsovilalonga.ChaffClient.ChaffClient;
import com.afonsovilalonga.Httping.Ping;
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
        } else if(args[0].equals("async")){
            for(int i = 0; i < Integer.parseInt(args[1]); i++){
                Thread t = new Thread(new AutomatedClient());
                t.start();
            }
        } else if(args[0].equals("async/notimer")){
            String protocol = args[1];
            String file = args[2];
            int num_threads = Integer.parseInt(args[3]);
            int num_reqs = Integer.parseInt(args[4]);

            for(int i = 0; i < num_threads; i++){
                Thread t = new Thread(new Injector(protocol, file, num_reqs));
                t.start();
            }
        } else if(args[0].equals("chaff")){
            new ChaffClient();
        } else if(args[0].equals("ping")){
            new Ping();
        }
        else{
            System.out.println("Invalid arguments: interactive or async x (where x is the number of threads to spawn) or async/notimer protocol file x (where x is the number of threads to spawn) y (where y is the number of requests per thread)");
            System.exit(-1);
        }
    }

}

