package com.afonsovilalonga.Common.ObserversCleanup;

public class Monitor {
    private final static Object MONITOR = new Object();
    
    private static Observer observer_obj; 

    public static void registerObserver(Observer observer){
        if (observer_obj != null) 
            return;
        
        observer_obj = observer;
    }

    public void notifyObserver(){
        synchronized(MONITOR){
            if(observer_obj != null){
                ((ObserverClient) observer_obj).onStateChange();
            }
        }
    }

    public void notifyObserver(String id){
        synchronized(MONITOR){
            if(observer_obj != null){
                ((ObserverServer) observer_obj).onStateChange(id);
            }
        }
    }
}
