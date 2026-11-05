# Java Concurrency: Private Lock Pattern, Object Confinement & Client-Side Locking

**Tags:** `#java` `#concurrency` `#multithreading` `#lld` `#synchronization` `#design-patterns`

---

## 1. Core Misconception: What Does `synchronized` Actually Lock?

A common misconception in Java concurrency is believing that you must synchronize on the exact object whose method is being executed (e.g., `synchronized (delegate) { delegate.call(); }`).

**Reality:** Java does **not** lock an object's fields or methods. Java only locks the **`synchronized` code block itself**.

### The Turnstile Mental Model (Not a Forcefield)
When you write `synchronized (someObject)`, the JVM does **not** place a protective forcefield around `someObject` that stops other threads from calling its methods.

Instead, every Java object has a built-in, invisible monitor (Intrinsic Lock) in its object header (specifically within the Mark Word):
* `synchronized (x)` simply means: *"Before a thread can step across this `{` curly brace, it must acquire `x`'s monitor flag. If another thread already holds `x`'s monitor, the incoming thread must wait outside the `{` brace in the `BLOCKED` state until the active thread hits `}` and releases it."*
* The lock object (`lock`) does not execute any business logic. It acts strictly as a **bouncer / turnstile standing at the door** of the `synchronized` block.

```text
Thread-1 ──┐                         ┌─────────────────────────────────┐
Thread-2 ──┼──► [ synchronized(lock) ] ──► { delegate.call(msg); }     │
Thread-3 ──┘      (The Turnstile)    │   (Only 1 thread inside at a time)
                                     └─────────────────────────────────┘
```

---

## 2. How `private` + `synchronized(lock)` Work Together

Why can't other threads bypass our `private final Object lock` and execute `delegate.call(msg)` directly? Because **Java access modifiers (`private`)** and **Java monitors (`synchronized`)** work as a two-part team:

```java
class SafeCallme {
    // 1. OBJECT CONFINEMENT (private final)
    // Created internally and hidden. No outside class can see or reference this variable!
    private final Callme delegate = new Callme(); 
    
    // 2. DEDICATED MONITOR (private final)
    // Our dedicated bouncer / turnstile
    private final Object lock = new Object();

    // 3. SINGLE ENTRY POINT (public)
    // The ONLY door into this class from the outside world
    public void call(String msg) {
        synchronized (lock) {       // <-- Every thread MUST pass this turnstile
            delegate.call(msg);     // <-- before it can reach delegate!
        }
    }
}
```

1. **Confinement via `private`:** Outside in `main()` or `CallerTask`, `delegate` is completely invisible (`sharedSafeTarget.delegate` will not compile). Furthermore, because `SafeCallme` instantiates `new Callme()` internally rather than accepting an existing reference from `main()`, no unguarded reference to the raw object exists anywhere on the heap.
2. **Mutual Exclusion via `synchronized (lock)`:** The **only** way for any thread in the application to invoke `delegate.call(msg)` is by calling the public method `sharedSafeTarget.call(msg)`. And the very first line inside that method is guarded by `synchronized (lock)`.

---

## 3. Why Use `private final Object lock` Instead of `synchronized(this)`?

Any Java object can act as a monitor. So why create a dummy `new Object()` instead of making the method `public synchronized void call(String msg)` (which locks on `this`)?

**Answer:** Using `synchronized (this)` exposes your lock to external code, opening the door to **Lock Starvation / Denial of Service / Deadlocks**.

### The Rogue Thread Hazard (When Locking on `this`)
If `SafeCallme.call()` synchronizes on `this` (`sharedSafeTarget`), the lock is publicly accessible to any code that has a reference to `sharedSafeTarget`:

```java
SafeCallme sharedSafeTarget = new SafeCallme();

// Rogue or buggy code in another module locks on the public wrapper reference:
new Thread(() -> {
    synchronized (sharedSafeTarget) {
        try {
            Thread.sleep(Long.MAX_VALUE); // Holds sharedSafeTarget's monitor forever!
        } catch (InterruptedException ignored) {}
    }
}).start();

// Meanwhile, your legitimate worker threads try to run:
sharedSafeTarget.call("Hello"); // <-- BLOCKED FOREVER!
```

### How the Private Lock Fixes This
When `SafeCallme` uses `private final Object lock = new Object();`:
* Even if an external rogue thread writes `synchronized (sharedSafeTarget)` and sleeps forever, **`sharedSafeTarget.call("Hello")` continues executing without any blockage**.
* `SafeCallme.call()` ignores `sharedSafeTarget`'s intrinsic monitor and instead acquires the monitor of its secret internal `lock` object, which external code can never see or acquire.

---

## 4. Avoiding Constructor "`this` Escape" & Coupling

### Bad Practice: Creating and Starting/Binding Threads Inside a Constructor
```java
class Caller implements Runnable {
    Callme target;
    String msg;
    Thread t;

    Caller(Callme target, String msg) {
        this.target = target;
        this.msg = msg;
        t = new Thread(this); // <-- HAZARD: 'this' escapes before constructor finishes!
    }
}
```
1. **`this` Escape Hazard:** Under the Java Memory Model (JMM), an object's fields (even `final` fields) are only guaranteed to be safely published and visible to other threads *after* the constructor finishes executing. Passing `this` to `new Thread(this)` inside the constructor leaks a reference to an incompletely constructed object.
2. **SRP Violation:** A `Runnable` should strictly define *what task to run*, not *how threads are created*. Decoupling `Thread` from `CallerTask` allows the task to be submitted to thread pools (`ExecutorService`) later without modification.

---

## 5. Complete Production / LLD Interview Code

```java
package multithreading.synchronization.synchronizedObjectAccess;

/**
 * 1. EXTERNAL LIBRARY CLASS (Untouchable)
 * Assume this comes from a third-party JAR. It is NOT thread-safe.
 */
class Callme {
    void call(String msg) {
        System.out.print("[" + msg);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Always restore the interrupt flag!
            System.out.println("interrupted");
        }
        System.out.println("]");
    }
}

/**
 * 2. THREAD-SAFE WRAPPER (Strict Confinement + Private Lock Pattern)
 */
class SafeCallme {
    private final Callme delegate;
    private final Object lock = new Object();

    // Default constructor: creates and strictly confines Callme internally
    public SafeCallme() {
        this(new Callme());
    }

    // Package-private constructor: allows injecting a mock Callme during unit tests
    SafeCallme(Callme delegate) {
        this.delegate = delegate;
    }

    public void call(String msg) {
        synchronized (lock) {
            delegate.call(msg);
        }
    }
}

/**
 * 3. PURE TASK (Decoupled from Thread creation, no 'this' escape)
 */
class CallerTask implements Runnable {
    private final SafeCallme safeTarget;
    private final String msg;

    public CallerTask(SafeCallme safeTarget, String msg) {
        this.safeTarget = safeTarget;
        this.msg = msg;
    }

    @Override
    public void run() {
        safeTarget.call(msg);
    }
}

/**
 * 4. DRIVER CLASS
 */
public class SynchronizedObjectAccess {
    public static void main(String[] args) {
        // Raw Callme never leaks into main(); strictly confined inside SafeCallme
        SafeCallme sharedSafeTarget = new SafeCallme();

        CallerTask task1 = new CallerTask(sharedSafeTarget, "Hello");
        CallerTask task2 = new CallerTask(sharedSafeTarget, "Synchronized");
        CallerTask task3 = new CallerTask(sharedSafeTarget, "World");

        Thread t1 = new Thread(task1, "Worker-1");
        Thread t2 = new Thread(task2, "Worker-2");
        Thread t3 = new Thread(task3, "Worker-3");

        t1.start();
        t2.start();
        t3.start();

        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Main thread interrupted");
        }
    }
}
```

---

## 6. Quick Reference Cheat Sheet

| Concept | What It Actually Does | Real-World Analogy |
| :--- | :--- | :--- |
| **`private final Callme delegate = new Callme()`** | Confines the unsafe object inside the wrapper so external code cannot reference it directly. | Putting the VIP room inside a locked building with no windows. |
| **`synchronized (lock)`** | Forces threads to line up one-by-one before executing the statements inside `{ ... }`. | A single-person turnstile at the only hallway leading to the VIP room. |
| **`private final Object lock = new Object()`** | Ensures no external code can acquire the lock and cause starvation or deadlocks. | Only the building's internal security has access to the turnstile mechanism. |
| **No `new Thread(this)` in Constructor** | Prevents "`this` escape" and decouples task logic (`Runnable`) from thread management. | Writing down a recipe on a card instead of gluing the recipe card to a specific chef. |