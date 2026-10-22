package collections.map;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JavaHashMap {
    static void main() {
        Map<String, String> map = new HashMap<>();
        Map<String, String> concurrentMap = new ConcurrentHashMap<>();

        map.put(null, null); // allowed
        concurrentMap.put("abc", null); // throws null pointer exception.

//        Note: We cannot insert null key or null value in concurrentHashMap

        IO.println(map.get(null));
        IO.println(concurrentMap.get("abc")); // throws null pointer exception
    }
}
