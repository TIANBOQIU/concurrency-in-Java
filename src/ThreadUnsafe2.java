import java.util.Random;

/**
 * Race Condition and Critical Section
 * thread unsafe for the test-then-act coding pattern
 */
public class ThreadUnsafe2 {
    public static void main(String[] args) throws InterruptedException {
        RaceCoindition rc = new RaceCoindition();
        rc.runTest();
    }
}

class RaceCoindition {
    int randInt;
    Random random = new Random(System.currentTimeMillis());

    void printer() {
        int i = 1000000;
        while (i != 0) {
            if (randInt % 5 == 0) {
                // a different thread might change the predicate
                if (randInt % 5 != 0)
                    System.out.println(randInt);
            }
            i--;
        }
    }

    void modifier() {
        int i = 1000000;
        while (i != 0) {
            randInt = random.nextInt(1000);
            i--;
        }
    }

    public static void runTest() throws InterruptedException {
        final RaceCoindition rc = new RaceCoindition();
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                rc.printer();
            }
        });
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                rc.modifier();
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
    }
}
