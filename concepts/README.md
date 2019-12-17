### Concurrency in Java

> notes for https://www.youtube.com/watch?v=N0mMm5PF5Ow&list=PLhfHPmPYPPRk6yMrcbfafFGSbE2EPK_A6&index=10 series

### Out of order execution

> Compiler, JVM, or CPU might change the order of instructions for performance

```
load a 
set to 3
set to 4
store a

load b
set to 2
store b

---------
```


### Field Visibility (issue)

> In presence of multi-thread application

```
core1 core2 core3 core4
regs  regs  regs  regs
L1    L1    L1    L1   cache
    L2     |     L2  
          L3
          RAM          shared chache

```

> solution: use keyword **volatile** to flush (push) the change from local chache to shared chache
```java
// runs in two threads and the readThread reads the updated value
public class VolatileVisibility {
    volatile int x = 0;
    public void writeThread() {
        x = 1;
    }
    public void readThread() {
        int r2 = x;
    }
}
```


### JMM, Java Memory Model

> a set of rules that guarantee visibility of fields (aka happends before) amidst reordering of instructions


### Happends before relationship

```java
public class VolatileFieldVisibility {
    int a = 0, b = 0, c = 0; // regular
    volatile int x = 0; 

    public void writerThread() {
        a = 1;
        b = 1;
        c = 1;

        x = 1; // write of x that is volatile
    }

    public void readerThread() {
        int r2 = x; // read of x that is volatile
        // after reading the value x, we are reading a,b,c
        int d1 = a;
        int d2 = b;
        int d3 = c;
    }
}


// rule: any fileds that are written to before you write the volatile x should be updated and visible to any other thread after it has read the value of x
```

> also applicable to: synchronized, Locks, Concurrent collections, Thread ops (join, start)

*Synchronized Field Visibility*
```java
public class SynchronizedFieldVisibility {
    int a = 0, b = 0, c = 0;
    int x = 0;

    public void writerThread() {
        a = 1;
        b = 1;
        c = 1;

        synchronized(this) {
            x = 1;
        }
    }

    public void readerThread() {
        synchronized(this) {
            int r2 = x;
        }
        
        int d1 = a;
        int d2 = b;
        int d3 = c;
    }
}

// happends before relationship

// should synchronized on the same obj
```

```java
// or put everythinh within the synchronized block 

public class SynchronizedFieldVisibility {
    int a = 0, b = 0, c = 0;
    int x = 0;

    public void writerThread() {
        synchronized(this) {
            a = 1;
            b = 1;
            c = 1;
            x = 1;
        }
    }

    public void readerThread() {
        synchronized(this) {
            int r2 = x;        
            int d1 = a;
            int d2 = b;
            int d3 = c;
        }
    }
}

// still should synchronized on the same object
```

> similarly, we can also use Lock operations

```java
public class LockVisibility {
    int a = 0, b = 0, c = 0, x = 0;
    Lock lock = new ReentrantLock();

    public void writerThread() {
        lock.lock();
        a = 1;
        b = 1;
        c = 1;
        x = 1;
        lock.unlock();
    }

    public void readerThread() {
        lock.lock();
        int r2 = x;
        int d1 = a;
        int d2 = b;
        int d3 = c;
        lock.unlock();
    }
}
```


> interview question

```java
// wrong
public class VolatileVisibility {
    boolean flag = true;

    public void writerThread() {
        flag = false; // might never flush into the shared memory and visible to other thread
    }

    public void readerThread() {
        while (flag) {
            // ...
        }
    }
}

// correct, add volatile keyword to flag variable
public class VolatileVisibility {
    volatile boolean flag = true;

    public void writerThread() {
        flag = false; // might never flush into the shared memory and visible to other thread
    }

    public void readerThread() {
        while (flag) {
            // ...
        }
    }
}

```


### volatile and Atomic

```
volatile solves the visiblity issue (flush into shared cache)

synchronization problen like val++ is a different problem
https://youtu.be/WH5UvQJizH0?list=PLhfHPmPYPPRk6yMrcbfafFGSbE2EPK_A6&t=262

```

> volatile can force the happends before relationship and solve the visibility issue, while the synchronized issue is to do with component instructions like val++

```java
// with synchronized, only one thread is allowed to access the critic section

/* Synchronization solutions - #1
 * use synchronized keyword
 */

volatile int value = 1;
// thread-1
synchronized (obj) {
    value++;
}
// thread-2
synchronized (obj) {
    value++;
}


/*
 * Synchronization solution -#2 
 * use AtomicInteger type, use atomic variables
 */
AtomicInteger value = new AtomicInteger(1);
// thread-1
value.increment();
// thread-2
value.increment();
// ensure both read and write are done atomically
 
```


> usually the *volatile* is used for flags, *AtomicInteger* for counters and *AtomicReference* operate on cache



### Adder and Accumulator

```java
// use AtomicLong to achieve synchronization
class Example {
    public static void main(String[] args) throws InterruptedException {
        AtomicLong counter = new AtomicLong(0); // for long to be thread safe
        ExecutorService service = Executors.newFixedThreadPool(16);
        for (int i = 0; i < 100; i++) {
            service.submit(new Task(counter));         
        }
        Thread.sleep(2000); // not the right way to wait until all threads finish, don't do this in practice
        System.out.println(counter.get());
    }
    private static class Task implements Runnable {
        private final AtomicLong counter;
        public Task(AtomicLong counter) {this.counter = counter; }

        @Override
        public void run() {
            // some processing
            counter.incrementAndGet();
        }
    }
}


// more efficient way LongAdder that incremnts it's own var on each thread, no contention for increment operations
// utilize the thread-local varianle rather than the shared one like volatile or AtomicLong
class ExampleUseAdder {
    public static void main(String[] args) throws InterruptedException {
        LongAdder counter = new LongAdder();

        ExecutorService service = Executors.newFixedThreadPool(16);
        for (int i = 0; i < 100; i++) {
            service.submit(new Task(counter));
        }
        Thread.sleep(2000); // don't do this in practice
        System.out.println(counter.sum()); // all thread-local variables added, sync only happends here once
    }

    private static class Task implements Runnable {
        private final LongAdder counter;

        public Task(LongAdder counter) {this.counter = counter;}

        @Override
        public void run() {
            // some processing
            counter.increment(); // Thread-local variable incremented.
        }
    }
}
```

> Throughput of LongAdder is much better than the AtomicLong


### Accumulator

> more generic version of Adder, a reduce function

> (x, y) -> x + y, init x = 0 . And x is the temp, the y will comsume the input one by one, sum with x and save the result back to x again

```java
class AccumulatorExample {
    public static void main(String[] args) throws InterruptedException{
        LongAccumulator counter = new LongeAccumulator((x, y) -> x + y, 0);
        ExecutorService service = Executors.newFixedThreadPool(16);
        for (int i = 0; i < 100; i++) {
            service.submit(new Task(counter));
        }
        Thread.sleep(2000); // don't do this in practice
        System.out.println(counter.get());
    }

    private static class Task implements Runnable {
        private final LongAccumulator counter;

        public Task(LongAccumulator counter) {this.counter = counter; }

        @Override
        public void run() {
            // some processing
            counter.accumulate(1); // asigned to y
        }
    }
}


```


```java
// sum
LongAccumulator sum = new LongAccumulator((x, y) -> x + y, 0);
// product
LongAccumulator prod = new LongAccumulator((x, y) -> x * y, 1);
// max
LongAccumulator max = new LongAccumulator((x, y) -> Math.max(x, y), // some init);
// min
LongAccumulator min = new LongAccumulator((x, y) -> Math.min(x, y), // some init);
```


> reduce function should not have any side effect, don't have its own state or change external variables

> Map Reduce, functional programming


### ForkJoinPool

> callable can get the return value from the task

*different from execuor service*
1. tasks producing sub-tasks, aka ForkJoin



```java
// Example
if (n <= 1) return n;
Fib fib1 = new Fib(n - 1); // sub-task 1
Fib fib2 = new Fib(n - 2); // sub-task 2
fib1.solve();
fib2.solve();
number = fib1.number + fib2.number; // join results
return number;
```


```java
public Result solve(Task t) {
    split t into smaller tasks
    for each of these tasks
        solve(t_i)
    wait for all tasks to complete
    join all individual results
    return result
}
```

2. Each thread has its own deque called deck, double ended queue


### work stealing

> a thread help another thread out


*submit task in ForkJoinPool*

### ForkJoinClass

```java
private class Fibonacci extends RecursiveTask<Integer> {
    final int n;
    Fibonacci(int n) {this.n = n;}

    public Integer compute() {
        if (n <= 1) return n;
        Fibonacci f1 = new Fibonacci(n - 1);
        f1.fork();
        Fibonacci f2 = new Fibonacci(n - 2);
        f2.fork();
        return f2.join() + f1.join();
    }
}
```

> a functional progamming way will have ideal performance


### BlockingQueue

```
thread-safe

producer thread(s) -> BlockingQueue -> consumer(s)

the consumer thread will be blocked if the current queue is empty

same when the queue is full and the producer trys to put
```

### SynchronousQueue

```
~~single item slot~~ actually has not size at all

every time the producer puts it will be blocked until the consumer trys to get

There is a direct handoff between Producer and Consumer thread

no peek method, no iterate method, perfect for direct handoffs
```


###  ThreadLocal in Java

```java
public class UserService {
    public static void main(String[] args) {
        new Thread(() -> {
        String birthDate = new UserService().birthDate(100);
        System.out.println(birthDate);
    }).start();
    new Thread(() -> {
        String birthDate = new UserService().birthDate(100);
        System.out.println(birthDate);
    }).start();

    Thread.sleep(1000);
    }

    public String birthDate(int userId) {
        Date birthDate = birthDateFromDB(userId);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(birthDate);
    }
}


// if the number of threads is too large we will have a lot of duplicate SimpaleDateFormat obj that we can reuse to be more efficient in memory

```

> one solution of it could be use global variable that is shared among all threads, but the variable may not be thread-safe class

> so we can use lock which slow performance


```
# ThreadLocal use case 1
use a thread pool and each thread has its own local variable which will never have synchronization problem
```

> **ThreadLocal-1** Per thread instances for memory efficiency and thread-safety

```java
class ThreadSafeFormatter {
    public static ThreadLocal<SimpleDateFormat> dateFormatter = new ThreadLocal<>(SimpleDateFormat) {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }

        @Override
        public SimpleDateFormat get() {
            return super.get();
        }
    };
}

public class UserService {
    public static void main(String[] args) {
        // ...
    }

    public String birthDate(int userId) {
        Date birthDate = birthDateFromDB(userId);
        final SimpleDateFormat df = ThreadSafeFormatter.dateFormatter.get();
        returnn df.format(birthDate);
    }
}


```

```java
// example in Java8

class ThreadSafeFormatter {
    public static ThreadLocal<SimpleDateFormat> df = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));    
}

public class UserService {
    public static void main(String[] args) {
        // ...
    }

    public String birthDate(int userId) {
        Date birthDate = birthDateFromDB(userId);
        final SimpleDateFormat df = ThreadSafeFormatter.df,get();
        return df.format(birthDate);
    }
}

```

#### use case 2, Per thread context

```
still, synchronization is not required with ThreadLocal

example : a request to a web server through multiple services, need to return the user

```

```java
// 'passing' infomation without use a concurrent DS or lock
// create a seperate class
public class UserContextHolder {
    public static ThreadLocal<User> holder = new ThreadLocal();
}

class Service1 {
    public void process() {
        User user = getUser();
        UserContextHolder.holder.set(user); // set it for this thread (for other services in a row)
    }
}

class Service2 {
    public void process() {
        User user = UserContextHolder.holder.get(); // get user for this thread
    }
}

class Service3 {
    public void process() {
        User user = UserContextHolder.holder.get(); // get user for this thread
    }
}

// last service, clean up
class Service4 {
    public void process() {
        User user = UserContextHolder.holder.get();
        UserContextHolder.holder.remove();
    }
}

// feel it is a good practtice for web service
```


> can be delegate to frameworks like Spring


### Phaser

#### CountDownLatch

```java
class CountDownLatchExample {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4);

        CountDownLatch latch = new CountDownLatch(3);
        executor.submit(new DependentService(latch));
        executor.submit(new DependentService(latch));
        executor.submit(new DependentService(latch));

        latch.await();

        System.out.println("All dependant services initialized");

        // program initialized, perform other operations
    }

    public static class DependentService implements Runnable {

        private CountDownLatch latch;
        public DependentService(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            // startup task, init
            latch.countDown();
            // continue w/ other operations
        }
    }
}
```


#### CyclicBarrier example
```java
public static void main(String[] args) throws interruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(4);

    CyclicBarrier barrier = new CyclicBarrier(3);
    executor.submit(new Task(barrier));
    executor.submit(new Task(barrier));
    executor.submit(new Task(barrier));

    Thread.sleep(2000);
}

public static class Task implements Runnable {
    private CyclicBarrier barrier;
    public Task(CyclicBarrier barrier) {this.barrier = barrier; }

    @Override
    public void run() {
        while (true) {
            try {
                barrier.await();
            } catch(InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
            // send meesgae to corresponding system
        }
    }
}
```


### Phaser

> similar to CountDownLatch and Cyclic Barrier

```java
public static void main(String[] args) {
    ExecutorService executor = Executor.newFixedThreadPool(4);
    Phaser phaser = new Phaser(3); // parties = 3
    execuor.submit(new DependentService(phaser));
    execuor.submit(new DependentService(phaser));
    execuor.submit(new DependentService(phaser));

    phaser.awaitAdvance(1); // phase = 1, block the main thread similar to await
}

// ...
void run() {
    // startup
    phaser.arrive();
}
```

```java
public static void main(String[] args) {
    ExecutorService executor = Executor.newFixedThreadPool(4);
    Phaser phaser = new Phaser(3); // parties = 3
    execuor.submit(new DependentService(phaser));
    execuor.submit(new DependentService(phaser));
    execuor.submit(new DependentService(phaser));

    Thread.sleep(3000);
}

// ...
void run() {
    while (true) {
        phaser.arriveAndAwaitAdvance(); // similar to barrier.await()
        // send the message aat once 
    }
}
```

### native thread, aka kernel thread

```
thread is expensive 

we want non-blocking IO

-> Asynchrous API 
```

```java
// Asynchronous API - Callbacks
for (Integer id : employeeIds) {
    CompletableFuture.supplyAsync(() -> fetchEmployee(id))
    .thenApplyAsync(employee -> fetchTaxRate(employee))
    .thenApplyAsync(taxRate -> calculateTax(taxRate))
    .thenAcceptAsync(taxValue -> sendEmai(taxValue));
}

// while in sync API future.get() will block
```


### NIO

listener based callbacks, the main thread is not blocked

### Webflux in Spring 5 

### project loom, light weight threads


### Condition Class

```
thread-1 condition.await() -> into wait-sate(block)

thread-2 condition.signal() -> all wait-state for this condition into runnale state

```

```java
class ConditionClassExample {
    private Lock lock = new ReentrantLock();
    private Condition conditionMet = lock.newCondition();

    public void method1() throws InterruptedException {
        lock.lock();
        try {
            conditionMet.await(); // suspend here
            // can now do ops <- resume here
        } finally {
            lock.unlock();
        }
    }

    public void method2() {
        lock.lock();
        try {
            // do some ops
            conditionMet.signal();
        } finally {
            lock.unlock();
        }
    }
}

```

> wait, notify in synchronized block 

```java
class WaitNotifyExample {
    // sync on the same instance in this example
    // only one thread can execute this part
    public synchronized void execute() {
        try {
            monitor.wait(); // monitor could be any obj
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
    }

    // notify thread waiting on the monitor
    monitor.notify();
    // notify all threads
    monitor.notifyAll();
}


// 
// also have signalAll()
```


> signalAll, wait-sets, fairness

```
JVM scheduling
```


### Spurious wake ups

> perform await in loop always 

```java
public String consume() throws InterruptedException {
    lock.lock();
    try {
        while (count == 0)
            added.await();
        return getDate();
    } finally {
        lock.unlock();
    }
}
```

## Lock's condition

### producer consumer example

```java
class ProducerConsumerExample {
    private Lock lock = new ReentrantLock();
    private Condition added = lock.newCondition();
    private Condition removed = lock.newCondition();

    public void produce() throws InterruptedException {
        lock.lock();
        try {
            while (count == LIMIT) {
                remove.await();
            }
            addData();
            added.signal();
        } finally {
            lock.unlock();
        }
    }

    public String consume() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) { // queue is empty
                added.await();
            }
            String data = getData();
            removed.signal();
            return data;
        } finally {
            lock.unlock();
        }
    }
}

```

## Semaphore 

```
use case -#1
restrict the number of concurrent calls from out application to a service (e.g. a slow service)



```

```java
// unlimited calls example
class Example {
    public static void main(String[] args) {
        ExecutorService service = Execuors.newFixedThreadPool(50);
        IntStream.of(1000).forEach(i -> service.execute(new Task()));

        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES); // timeout
    }

    public static class implements Runnable {
        @Override
        public void run() {
            // some processing 
            // IO call to the slow request, this might be called 50 times concurrently
            // rest of processing
        }
    }
}

```

> use Semaphore to limit # of permits

```
# permits = 3
thread-1 require(), permits--
thread-2 require(), permits--
thread-3 require(), permits--
thread-4 require(), blocked

thread-1 release(), permits++
thread-4 unblocked(), permits--

```

```java
class SemaphoreExample {
    public static void main(String[] args) throws InterruptedException {
        Semaphore semaphore = new Semaphore(3); // permits
        ExecutorService service = Executors.newFixedThreadPool(50);
        IntStream.of(1000).forEach(i -> service.execute(new Task(semaphore)));

        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);
    }

    static class Task implements Runnable {
        @Override
        public void run() {
            // some processing 
            semaphore.acquire(); // will throw exception
            // IO call to the slow service
            semaphore.release();
            // rest of processing
        }
    }
}
```

> semaphore.acquire(); // will throw exception
 replace into acquireUninterruptibly()


```java
// can take and release multiple permits but should be the same
```

> semaphore -> restrict / manage the use of limied resources

### ReentrantLock
```
lock has extra features
```


```java
// in case of that exception happens before unlock use try-finally

private static ReentrantLock lock = new ReentrantLock();

private static void accessResource() {
    lock.lock();
    try {
        // access the resource
    } finally {
        lock.unlock();
    }
}

```

```java
// reentrant-what?
lock.lock();
lock.lock(); // multiple lock called without calling unlock
int number = lock.getHoldCount();
lock.unlock();
lock.unlock();


// handle recursion
class ReentrantLockExample {
    private static ReentrantLock lock = new ReentrantLock();
    private static void accessResource() {
        lock.lock();
        // update shared resource
        if (someCondition()) {
            accessResource(); // recursion, increse getHeldCount
        }
        lock.unlock();
    }
}

// fair lock
// in wait queue, 
```

```
by default, lock is unfair, you can pass lock = new ReentrantLock(true) to make it fair so that the thread that have waited the longest amount of time will be unlock first

# fair lock, equal chance for all threads but is slower because of the queue

# unfair lock, more faster (higher throughput) but possible thread starvation (never unlock or wait a long time)
```


*tryLock*
```java
// tryLock
class TryLockExample {
    private static ReentrantLock lock = new ReentrantLock();

    private static void accessResource() {
        boolean lockAcquired = lock.tryLock();

        if (lockAcquired {
            try {
                // access resource
            } finally {
                lock.unlock();
            }
        } else {
            // do something else rather than block and wait for unlock
        }
    }
}

// boolean lockAcquired = lock.tryLock(5, TimeUnit.SECONDS);
```


### ReadAndWrite Lock
```
more efficient
multiple owners of read-lock
allowed to proceed to view

threads which wanted to write/update go into wait state
```

```java
class ReentrantReadWriteLockExample {
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock.ReadLock readLock= lock.readLock();
    private ReentrantReadWriteLock.writeLock writeLock=
    lock.writeLock();

    private void readResource() {
        readLock.lock();
        // view resource
        readLock.unlock();
    }

    private void writeResource() {
        writeLock.lock();
        // update resource
        writeLock.unlock();
    }

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> obj.readResource());
        t1.start();
        Thread t2 = new Thread(() -> obj.readResource());
        t2.start();
        Thread t3 = new Thread(() -> obj.readResource());
        t3.start();
        Thread t4 = new Thread(() -> obj.readResource());
        t4.start();
    }
}

// good for frequent reads and infrequen writes
```


### Exchanger

> SynchronousQueue -> direct handoff

> Exchanger -> handoffs in both direction

```
thread-1 exchange
thread-2 exchange


// buffer

producer-thread : full buffer <--exchange--> consumer-thread : empty buffer

```

### Data Integrity Issue

> use lock, mutual exclusion

```
striped lock, group items

Lock # = obj's hashcode % total number of locks
```


### Java fibers, project loom

> light weight kB comparing to ~MB for thread

> not in Java yet. we have Go(goroutines), Python (Trio), Kotlin(coroutines)

> unblocking IO, mount and unmount