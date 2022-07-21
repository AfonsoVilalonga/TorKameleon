package com.afonsovilalonga.Utils;

public class Stats {

    private long totalBytes = 0L;
    private long startTime;
    private int totalRequests = 0;


    /**
     * Create a Stats object to do some statistics and print it
     */
    public Stats() {
        startTime = System.currentTimeMillis();
    }

    /**
     * count a new request sent and file's bytes received
     *
     * @param: bytes - number of bytes received
     */
    public synchronized void newRequest(int bytes) {
        totalRequests++;
        totalBytes += bytes;
    }


    /**
     * Print statistics report
     */
    public void printReport() {
        // compute time spent receiving bytes
        long milliSeconds = System.currentTimeMillis() - startTime;
        double speed = totalBytes / (double) milliSeconds; // K bytes/s
        System.out.println("\n==========================================================");
        System.out.println("Transfer stats:");
        System.out.println("Total time elapsed (s):\t\t" + (milliSeconds / 1000.0));
        System.out.println("Download size (bytes):\t\t" + totalBytes);
        System.out.printf("End-to-end debit (Kbytes/s):\t%.1f\n", speed);
        System.out.println("Received packets:\t\t" + totalRequests);
        System.out.printf("Avg. request duration (ms):\t%.1f", milliSeconds / (double) totalRequests);
        System.out.println("\n==========================================================\n");

    }
}