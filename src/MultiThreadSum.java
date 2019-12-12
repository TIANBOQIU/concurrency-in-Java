public class MultiThreadSum {
        long startRange;
        long endRange;
        long counter = 0;
        static long MAX_NUM = Integer.MAX_VALUE;

        public MultiThreadSum(long startRange, long endRange) {
            this.startRange = startRange;
            this.endRange = endRange;
        }

        public void add() {
            for (long i = startRange; i<= endRange; i++) {
                counter += i;
            }
        }

        static public void sumInSingleThread() {
            long startTime = System.currentTimeMillis();
            MultiThreadSum s = new MultiThreadSum(1, MAX_NUM);
            s.add();
            long endTime = System.currentTimeMillis();
            System.out.println("Single thread | sum(1, " + Integer.MAX_VALUE + ") = " + s.counter + " took " + (endTime- startTime));
        }

        static public void sumInTwoThreads() throws InterruptedException {
            long startTime = System.currentTimeMillis();

            MultiThreadSum s1 = new MultiThreadSum(1, MAX_NUM / 2);
            MultiThreadSum s2 = new MultiThreadSum(MAX_NUM / 2 + 1, MAX_NUM);
            Thread t1 = new Thread(() -> {
                //System.out.println("thread 1 starts");
                s1.add();
            });
            Thread t2 = new Thread(() -> {
                //System.out.println("thread 2 starts");
                s2.add();
            });
            t1.start();
            //t1.join();
            //System.out.println("t1 join()");
            t2.start();
            t1.join();
            t2.join();
            long count = s1.counter + s2.counter;

            long endTime = System.currentTimeMillis();
            System.out.println("Two threads | sum(1, " + Integer.MAX_VALUE + ") = " + count + " took " + (endTime- startTime));
        }

        public static void runTest() throws InterruptedException {
            sumInSingleThread();
            sumInTwoThreads();
        }

        public static void main(String[] args) throws InterruptedException{
            runTest();
        }
}


