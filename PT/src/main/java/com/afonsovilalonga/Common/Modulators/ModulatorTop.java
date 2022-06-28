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

    private boolean shutdown;

    protected ModulatorTop(Socket tor_socket) {
        this.tor_socket = tor_socket;
        this.executor = Executors.newFixedThreadPool(2);

        this.shutdown = false;
    }

    protected boolean getShutdown() {
        return this.shutdown;
    }

    protected Socket gettor_socket() {
        return this.tor_socket;
    }

    protected ExecutorService getExecutor() {
        return this.executor;
    }

    protected <T> boolean reTry(Supplier<Boolean> func) {
        for (int i = 0; i < RETRIES; i++) {
            boolean result = func.get();

            if(result)
                return result;
            
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }

    protected void serviceShutdow() {
        this.shutdown = true;

        executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }

            this.tor_socket.close();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
