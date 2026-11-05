package multithreading.synchronization.synchronizedObjectAccess;

/*
Assume Call me is class in a library/external dependency.
Here we cannot use the synchronized keyword.
 */
class Callme {
    void call(String msg) {
        IO.print("[" + msg);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            IO.println("interrupted");
        }
        IO.println("]");
    }
}

/*
Notice that inside the thread class we've created below we
have used the synchronized block with target object as the resource
we want to synchronized.
The target object is shared between threads via the main thread's heap memory
so we can create as many threads, they will only access the caller sequentially
 */
class Caller implements Runnable {
    private final String msg;
    private final Callme target;
    Thread t;

    Caller(Callme target, String msg) {
        this.target = target;
        this.msg = msg;
        t = new Thread(this);
    }

    public void start() {
        this.t.start();
    }

    @Override
    public void run() {
        synchronized (target) {
            target.call(msg);
        }
    }
}

public class SynchronizedObjectAccess {
    static void main() {
        Callme target = new Callme();
        Caller caller1 = new Caller(target, "Hello");
        Caller caller2 = new Caller(target, "Synchronized");
        Caller caller3 = new Caller(target, "World");

        caller1.start();
        caller2.start();
        caller3.start();

        try {
            caller1.t.join();
            caller2.t.join();
            caller3.t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            IO.println("Interrupted");
        }
    }
}
