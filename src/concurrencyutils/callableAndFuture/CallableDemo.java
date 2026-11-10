package concurrencyutils.callableAndFuture;

import java.util.Map;
import java.util.concurrent.*;

public class CallableDemo
{
	static void main()
	{
		ExecutorService es = Executors.newFixedThreadPool(3);
		Future<Integer> f1;
		Future<Double> f2;
		Future<Integer> f3;

		IO.println("Starting");

		f1 = es.submit(new Sum(10));
		f2 = es.submit(new Hypot(3, 4));
		f3 = es.submit(new Factorial(6));

		try {
			IO.println(f1.get());
			IO.println(f2.get());
			IO.println(f3.get());
		}
		catch (InterruptedException | ExecutionException e) {
			IO.println(e.getMessage());
		}

		es.shutdown();
		IO.println("Done.");
	}
}

class Sum implements Callable<Integer>
{
	int stop;

	Sum(int stop)
	{
		this.stop = stop;
	}

	@Override
	public Integer call() throws Exception
	{
		int sum = 0;
		for (int i = 1; i < stop; i++) {
			sum += i;
		}

		return sum;
	}
}

class Hypot implements Callable<Double>
{
	int side1, side2;

	Hypot(int side1, int side2)
	{
		this.side1 = side1;
		this.side2 = side2;
	}

	@Override
	public Double call() throws Exception
	{
		return Math.sqrt((side1*side1) + (side2*side2));
	}
}

class Factorial implements Callable<Integer>
{
	int stop;

	Factorial(int stop)
	{
		this.stop = stop;
	}

	@Override
	public Integer call() throws Exception
	{
		int factorial = 1;
		for (int i = 2; i <= stop; i++) {
			factorial *= i;
		}
		return factorial;
	}
}