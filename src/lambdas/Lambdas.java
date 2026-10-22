package lambdas;

import lambdas.interfaces.Printable;
import lambdas.interfaces.PrintableThatReturns;

public class Lambdas {
    static void printThing(Printable thing) {
        thing.print("!");
    }

    static void main() {
//        Note that we are able to implement the interface with just a lambda
//        this is because our interface is a functional interface
        Printable lambdaPrintable = s -> IO.println("Hello " + s);

        PrintableThatReturns lambdaPrintableReturning1 = s -> {
            IO.println("Hello " + s);
            return "Hello" + s;
        };

        PrintableThatReturns lambdaPrintableReturning2 = s -> "Hello " + s;

        printThing(lambdaPrintable);
    }
}
