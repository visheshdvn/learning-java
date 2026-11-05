package multithreading.synchronization.C.betterWayOfClientSideSynchronization;

/*
 * 1. EXTERNAL LIBRARY CLASS (Untouchable)
 * Assume this comes from a third-party JAR. It is NOT thread-safe.
 */
class Callme {
    void call(String msg) {
        IO.print("[" + msg);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            IO.println("interrupted");
        }
        IO.println("]");
    }
}

/*
 * 2. THREAD-SAFE WRAPPER (Decorator Pattern)
 * Instead of forcing every thread to remember `synchronized(target)`,
 * we wrap Callme and enforce the lock internally in one place.
 */
class SafeCallme {
    private final Callme delegate;

    // Private lock object so external code cannot accidentally lock on it and cause deadlocks
    private final Object lock = new Object();

    // Default constructor for safety: creates and hides Callme internally
    public SafeCallme() {
        this(new Callme());
    }

    // Package-private or public constructor for unit testing / mocking
    public SafeCallme(Callme delegate) {
        this.delegate = delegate;
    }

    public void call(String msg) {
        synchronized (lock) {
            delegate.call(msg);
        }
    }
}

/*
 * 3. PURE TASK (No Thread inside constructor)
 * Caller only defines the task (Runnable). It does NOT create `new Thread(this)`
 * inside its constructor, preventing 'this' escape and keeping it decoupled.
 */
class CallerTask implements Runnable {
    private final SafeCallme safeTarget;
    private final String msg;

    CallerTask(SafeCallme safeTarget, String msg) {
        this.safeTarget = safeTarget;
        this.msg = msg;
        // Notice: No `t = new Thread(this)` here!
    }

    @Override
    public void run() {
        // Notice how clean this is! CallerTask doesn't even need to know
        // about synchronization anymore; SafeCallme guarantees it.
        safeTarget.call(msg);
    }
}

public class SynchronizedObjectAccess {
    static void main() {
//        // 1. Instantiate the unsafe external object
//        Callme rawLibraryObject = new Callme();
//
//        // 2. Wrap it in our thread-safe decorator
//        SafeCallme sharedSafeTarget = new SafeCallme(rawLibraryObject);

        SafeCallme sharedSafeTarget = new SafeCallme();

        // 3. Create pure tasks (Runnables)
        CallerTask task1 = new CallerTask(sharedSafeTarget, "Hello");
        CallerTask task2 = new CallerTask(sharedSafeTarget, "Synchronized");
        CallerTask task3 = new CallerTask(sharedSafeTarget, "World");

        // 4. Manage Threads separately from the task constructors
        Thread t1 = new Thread(task1, "Thread-1");
        Thread t2 = new Thread(task2, "Thread-2");
        Thread t3 = new Thread(task3, "Thread-3");

        t1.start();
        t2.start();
        t3.start();

        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            IO.println("Main thread interrupted");
        }
    }
}