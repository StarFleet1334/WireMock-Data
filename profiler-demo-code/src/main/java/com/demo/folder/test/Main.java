package com.demo.folder.test;

public class Main {
    // MicroBenchMark -> Measuring the performance of a small piece of code
    public Boolean isPrime1(Integer testNumber) {
        for (int i = 2; i < testNumber; i++) {
            if (testNumber % i == 0) return false;
        }
        return true;
    }

    public Boolean isPrime2(Integer testNumber) {
        int maxToCheck = (int)Math.sqrt(testNumber);
        for (int i = 2; i < maxToCheck; i++) {
            if (testNumber % i == 0) return false;
        }
        return true;
    }

    public static void main(String[] args) throws InterruptedException {
        Main main = new Main();

        for (int i  = 1; i < 10000; i++) {
            main.isPrime2(i);
        }

        Thread.sleep(2000);
        System.out.println("Warm up finished, now measuring...");

        // Start time
        long start = System.currentTimeMillis();

        for (int i = 1; i < 50000; i++) {
            System.out.println(main.isPrime2(i));

        }

        // End time
        long end = System.currentTimeMillis();
        System.out.println("Time Taken: " + (end - start));
    }
}
