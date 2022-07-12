package com.afonsovilalonga.Common.Modulators;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.afonsovilalonga.Common.ObserversCleanup.Monitor;

public abstract class ModulatorTop extends Monitor{
    private static final int RETRIES = 35;
    private static final int SLEEP_TIME = 1000;

    private Socket tor_socket;
    private ExecutorService executor;
    private boolean started_notifier;

    protected ModulatorTop(Socket tor_socket) {
        this.tor_socket = tor_socket;
        this.executor = Executors.newFixedThreadPool(2);
        this.started_notifier = false;
    }

    protected Socket gettor_socket() {
        return this.tor_socket;
    }

    protected ExecutorService getExecutor() {
        return this.executor;
    }

    protected synchronized void execNotifier(String id){
        if(!this.started_notifier){
            this.started_notifier = true;
            notifyObserver(id);
        }
    }

    protected <T> boolean reTry(Supplier<Boolean> func) {
        for (int i = 0; i < RETRIES; i++) {
            boolean result = func.get();

            if(result)
                return result;
            
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e1) {}
        }
        return false;
    }

    protected void serviceShutdow() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow(); 
            }

            this.tor_socket.close();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {}
    }
}
