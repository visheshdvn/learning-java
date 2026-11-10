package concurrencyutils.synchronizationObjects.semaphore;

import java.util.concurrent.Semaphore;

/**
 * Holds mutable state shared by the worker threads in this demo.
 */
class Shared
{
	static int count = 0;
}

/**
 * Runnable that increments {@link Shared#count} while holding a semaphore permit.
 *
 * <p>The semaphore limits access to the shared counter so only one worker can
 * modify it at a time.</p>
 */
class IncThread implements Runnable
{
	String name;
	Semaphore sem;

	public IncThread(String name, Semaphore sem)
	{
		this.name = name;
		this.sem = sem;
	}

	@Override
	public void run()
	{
		try {
			IO.println(name + " is waiting for a permit.");

			// Blocks until a permit becomes available.
			sem.acquire();
			IO.println(name + " gets a permit.");

			// The permit is held for the whole update loop, so another worker
			// cannot modify Shared.count concurrently.
			for (int i = 0; i < 5; i++) {
				Shared.count++;
				IO.println(name + ": " + Shared.count);

				// Pause briefly to make thread interleaving visible in the output.
				Thread.sleep(10);
			}
		}
		catch (InterruptedException e) {
			IO.println(e);
		}

//		Release the permit
		IO.println(name + " releases the permit.");
		sem.release();
	}
}

/**
 * Runnable that decrements {@link Shared#count} while holding a semaphore permit.
 *
 * <p>The semaphore limits access to the shared counter so only one worker can
 * modify it at a time.</p>
 */
class DecThread implements Runnable
{
	String name;
	Semaphore sem;

	public DecThread(String name, Semaphore sem)
	{
		this.name = name;
		this.sem = sem;
	}

	@Override
	public void run()
	{
		try {
			IO.println(name + " is waiting for a permit.");

			// Blocks until a permit becomes available.
			sem.acquire();
			IO.println(name + " gets a permit.");

			// The permit is held for the whole update loop, so another worker
			// cannot modify Shared.count concurrently.
			for (int i = 0; i < 5; i++) {
				Shared.count--;
				IO.println(name + ": " + Shared.count);

				// Pause briefly to make thread interleaving visible in the output.
				Thread.sleep(10);
			}
		}
		catch (InterruptedException e) {
			IO.println(e);
		}

//		Release the permit
		IO.println(name + " releases the permit.");
		sem.release();
	}
}

/**
 * Demonstrates using a {@link Semaphore} to serialize access to shared mutable state.
 *
 * <p>A semaphore initialized with one permit acts like a mutex: only one of the
 * two worker threads can update {@link Shared#count} at a time.</p>
 */
public class SemaphoreDemo
{
	static void main()
	{
		// One permit means only one thread can enter the critical section at a time.
		Semaphore sem = new Semaphore(1);

		new Thread(new IncThread("A", sem)).start();
		new Thread(new DecThread("B", sem)).start();
	}
}
