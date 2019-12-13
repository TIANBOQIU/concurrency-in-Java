/**
 * actually not that obvious according to the theory
 * 1. int counter = 0;
 * 2.
 * 3. void incrementCounter() {
 * 4.   counter++;
 * 5. }
 */

import java.util.Random;

public class ThreadUnsafe {
    public static void main(String[] args) throws InterruptedException{
        test();
    }

    public static void test() throws InterruptedException{
        Random rand = new Random();
        for (int i = 0; i < 10; i++) {
            Count count = new Count();
            Thread t1 = new Thread(() -> {
                count.addThreadUnsafe();
                try {
                    Thread.sleep(rand.nextInt(10));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            Thread t2 = new Thread(() -> {
                count.addThreadUnsafe();
                try {
                    Thread.sleep(rand.nextInt(10));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            System.out.println("add twice from 0 == " + count.counter);
        }
    }
}

class Count {
    int counter = 0;
    void addThreadUnsafe() {
        counter++;
    }
}
