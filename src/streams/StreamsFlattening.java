package streams;

import java.util.ArrayList;
import java.util.List;

class Business {
    String name;
    List<Person> employees;

    Business(String name, List<Person> employees) {
        this.name = name;
        this.employees = employees;
    }
}

public class StreamsFlattening {
    static void main() {
        List<Business> businesses = new ArrayList<>();
        businesses.add(new Business("Google", new ArrayList<Person>() {{
            add(new Person("Lara", 35));
            add(new Person("Peter", 21));
        }}));

        businesses.add(new Business("Apple", new ArrayList<Person>() {{
            add(new Person("Zack", 45));
            add(new Person("Karl", 50));
            add(new Person("Barbara", 17));
            add(new Person("Louis", 16));
        }}));

        businesses.add(new Business("Amazon", new ArrayList<Person>() {{
            add(new Person("Michael", 41));
            add(new Person("Jessica", 28));
            add(new Person("Laura", 13));
        }}));

//        Above is a list of businesses and their employees
//        The task is to
        businesses.stream()
                .filter(b -> b.name.startsWith("A"))
                .flatMap(b -> b.employees.stream())
                .sorted()
                .filter(p -> p.getAge() >= 30)
                .map(Person::getName)
                .forEach(IO::println);
//        IO.println();
    }
}
