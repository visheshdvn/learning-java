package streams;

import java.util.ArrayList;
import java.util.List;

class Person implements Comparable {
    String name;
    int age;

    Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    String getName() {
        return name;
    }

    int getAge() {
        return age;
    }

    @Override
    public int compareTo(Object o) {
        return this.age - ((Person) o).age;
    }
}

public class Streams {
    static void main() {
        List<Person> people = new ArrayList<>();
        people.add(new Person("John", 30));
        people.add(new Person("Jane", 25));
        people.add(new Person("Bob", 45));
        people.add(new Person("Alice", 16));
        people.add(new Person("Lara", 14));
        people.add(new Person("Liam", 37));


        // Filter people who are 18 or older
        List<Person> adults = people
                .stream()
                .filter(person -> person.age >= 18)
                .toList();

        // Print the names of adults
        adults.forEach(person -> IO.println(person.name));
        IO.println();

        // Print the name of top 5 people
        people.stream()
                .limit(5)
                .forEach(person -> IO.println(person.name));
        IO.println();

        // Print the name of adults sorted by age
        adults.stream()
                .sorted((p1, p2) -> p1.age - p2.age)
                .forEach(person -> IO.println(person.name));
        IO.println();

        // Print the name of adults sorted by name
        adults.stream()
                .sorted((p1, p2) -> p1.name.compareTo(p2.name))
                .forEach(person -> IO.println(person.name));
    }
}
