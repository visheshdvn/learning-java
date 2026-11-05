package multithreading.synchronization.createSynchronizedBlock;


/*
First option is to use the synchronized keyword on the code block that we want the threads to access
sequentially.

But this process has a drawback - What if we want to synchronize call to a method inside a library
or somewhere, where we cannot edit the code?
Synchronized object access will help us to tackle cases like these.
 */
class Callme {
    synchronized void call(String msg) {
        IO.print("[" + msg);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            IO.println("interrupted");
        }
        IO.println("]");
    }
}

class Caller implements Runnable {
    String msg;
    Callme target;
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
        target.call(msg);
    }
}

public class CreateSynchronizeBlock {
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
            IO.println("Interrupted");
        }
    }
}
