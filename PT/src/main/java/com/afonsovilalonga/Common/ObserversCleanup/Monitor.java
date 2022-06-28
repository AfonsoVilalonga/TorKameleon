package com.afonsovilalonga.Common.ObserversCleanup;

public class Monitor {
    private final static Object MONITOR = new Object();
    
    private Observer observer_obj; 

    public void registerObserver(Observer observer){
        if (observer == null || this.observer_obj != null) 
            return;
        
        this.observer_obj = observer;
    }

    public void notifyObserver(){
        synchronized(MONITOR){
            if(this.observer_obj != null){
                ((ObserverClient) observer_obj).onStateChange();
            }
        }
    }

    public void notifyObserver(String id){
        synchronized(MONITOR){
            if(this.observer_obj != null){
                ((ObserverServer) observer_obj).onStateChange(id);
            }
        }
    }
}
