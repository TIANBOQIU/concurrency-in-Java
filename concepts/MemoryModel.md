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


###  