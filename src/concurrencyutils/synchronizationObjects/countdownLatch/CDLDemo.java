package concurrencyutils.synchronizationObjects.countdownLatch;

import java.util.concurrent.CountDownLatch;

public class CDLDemo
{
	static void main()
	{
		CountDownLatch latch = new CountDownLatch(5);

		IO.println("Starting...");

		new Thread(new MyThread(latch)).start();

		try {
			latch.await();
		}
		catch (InterruptedException e) {
			IO.println("Interrupted! : " + e);
		}

		IO.println("Done.");
	}
}

class MyThread implements Runnable {
	CountDownLatch latch;

	public MyThread(CountDownLatch latch)
	{
		this.latch = latch;
	}

	@Override
	public void run()
	{
		for (int i = 0; i < 5; i++) {
			IO.println(i);
			latch.countDown();
		}
	}
}