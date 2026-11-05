package multithreading.creatingThreads;

class NewThread implements Runnable {
    String name;
    Thread t;

    public NewThread(String name) {
        this.name = name;
        t = new Thread(this, name);
        IO.println("New Thread created: " + t.getName());
    }

    @Override
    public void run() {
        try {
            int i = 5;
            while (i-->0) {
                IO.println(this.name + " : " + i);
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

public class JoinTest {
    public static void main() {
        long startTime = System.currentTimeMillis();

        NewThread nt1 = new NewThread("First thread");
        NewThread nt2 = new NewThread("Second thread");
        NewThread nt3 = new NewThread("Third thread");

        nt1.t.start();
        nt2.t.start();
        nt3.t.start();

        IO.println(nt1.t.getName() + " is alive " + nt1.t.isAlive());
        IO.println(nt2.t.getName() + " is alive " + nt2.t.isAlive());
        IO.println(nt3.t.getName() + " is alive " + nt3.t.isAlive());

//        join method forces the main thread to wait until the thread it is called on finishes.
        try {
            IO.println("Waiting for threads to finish.");
            nt1.t.join();
            nt2.t.join();
            nt3.t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

//        isAlive method doesn't wait, it just returns current status and proceed
//        nt1.t.isAlive();
//        nt2.t.isAlive();
//        nt3.t.isAlive();

//        you can do this to wait for all child threads to finish before parent
//        but join method is cleaner for this purpose
//        while (nt1.t.isAlive() || nt2.t.isAlive() || nt3.t.isAlive()) {
//
//        }

        IO.println("Main thread exiting in time " + (System.currentTimeMillis() - startTime));
    }
}
