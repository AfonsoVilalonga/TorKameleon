package com.afonsovilalonga.Common.ObserversCleanup;

public class Monitor {

    private static Observer observer_obj;

    public static void registerObserver(Observer observer) {
        if (observer_obj != null)
            return;

        observer_obj = observer;
    }

    public void notifyObserver(String id) {
        if (observer_obj != null) {
            ((ObserverServer) observer_obj).onStateChange(id);
        }

    }
}
