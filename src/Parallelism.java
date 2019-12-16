import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * requires the number of CPU cores greater than 1
 */
public class Parallelism {
    String user1 = "Alice";
    String user2 = "Bob";
    public static void main(String[] args) {
        Parallelism p = new Parallelism();
        //p.threeThreadsForThreeTasksExample();
        p.threadPoolExample();
    }


    // multi-threading way
    public void threeThreadsForThreeTasksExample() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                processTax("Bob");
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                processTax("Alice");
            }
        }).start();

        haevyCalculation(); // on main thread

    }

    /**
     * Thread pool way
     * @return
     */
    public void threadPoolExample() {
        ExecutorService es = Executors.newFixedThreadPool(4);
        es.submit(() -> processTax(user1));
        es.submit(() -> processTax(user2));
        haevyCalculation();

    }


    // example of heavy calculation
    public long haevyCalculation() {
        System.out.println("heavy ops on thread " + Thread.currentThread().getName());
        long sum = 0;
        for (long i = 0; i <= Integer.MAX_VALUE; i++)
            sum += i;
        return sum;
    }

    // example tasks
    public void processTax(String name) {
        System.out.println("processing tax related task for " + name + " at Thread " + Thread.currentThread().getName());
    }
}
