# Comprehensive Guide: HashMap vs. ConcurrentHashMap in Java

---

## 1. Executive Summary

| Feature / Dimension | `java.util.HashMap` | `java.util.concurrent.ConcurrentHashMap` |
| :--- | :--- | :--- |
| **Package** | `java.util` | `java.util.concurrent` |
| **Thread-Safety** | **No** (Unsynchronized) | **Yes** (Thread-safe without whole-table locking) |
| **Null Key Support** | Exactly **one** `null` key allowed (mapped to bucket 0) | **No** `null` keys permitted (`NullPointerException`) |
| **Null Value Support** | **Yes** (multiple `null` values allowed) | **No** `null` values permitted (`NullPointerException`) |
| **Synchronization Scope** | None | Fine-grained (CAS on empty buckets + per-bin lock on collisions) |
| **Iterator Consistency** | **Fail-Fast** (`ConcurrentModificationException`) | **Weakly Consistent** (reflects state at/since creation; no exception) |
| **Compound Operations** | Non-atomic (requires manual external synchronization) | Native atomic primitives (`putIfAbsent`, `computeIfAbsent`, etc.) |
| **Read Operations** | Unsynchronized, fast, unsafe under mutations | Lock-free, volatile-read semantics (`get()` never blocks) |
| **Primary Use Case** | Single-threaded flows or thread-confined local variables | High-concurrency multi-threaded read/write workloads |

---

## 2. Deep Dive: Architectural Evolution

### 2.1 Standard `HashMap` Architecture (Java 8+)
* **Data Structure:** An array of buckets (`Node<K, V>[] table`).
* **Collision Resolution:**
  * Chaining via singly linked lists.
  * **Treeification:** When a bucket's linked list length exceeds `TREEIFY_THRESHOLD` (default: 8) AND the total table capacity $\ge$ `MIN_TREEIFY_CAPACITY` (default: 64), the linked list converts to a Red-Black Tree (`TreeNode<K, V>`), reducing search time from $\mathcal{O}(n)$ to $\mathcal{O}(\log n)$.
  * **Untreeification:** When shrinking/resizing causes bucket element count to fall to `UNTREEIFY_THRESHOLD` (default: 6), it converts back to a linked list.
* **Resizing:** When `size > threshold` (where `threshold = capacity * loadFactor`), the bucket array doubles in size ($2^k$). Elements are rehashed and repositioned using bitwise masking (`hash & (newCapacity - 1)`).

### 2.2 `ConcurrentHashMap` in Java 7: Segment-Based Locking
* **Design:** Divided internally into a fixed array of independent partitions called `Segment<K, V>[]`.
* **Mechanism:** Each `Segment` extended `ReentrantLock`.
* **Default Concurrency Level:** Defaulted to 16 segments. This allowed up to 16 simultaneous writers, provided they hashed to different segments.
* **Limitation:** Writers to the same segment still blocked each other. Segment overhead increased memory footprint and restricted dynamic scalability.

### 2.3 `ConcurrentHashMap` in Java 8+: Fine-Grained Locking & CAS
Java 8 completely removed the `Segment` class hierarchy in favor of a design structurally mirroring `HashMap`:
* **Underlying Array:** A transient, volatile array of nodes (`Node<K, V>[] table`).
* **Lock-Free Inserts (CAS):** If target index `i` is empty (`table[i] == null`), the thread attempts insertion using a hardware-level **Compare-And-Swap** (`Unsafe.compareAndSwapObject` / `VarHandle`). No lock is acquired.
* **Bucket-Level Synchronization (`synchronized`):** If a hash collision occurs (target bucket already contains a node), the thread acquires an intrinsic lock **strictly on the first node of that specific bucket chain/tree**:
  ```java
  synchronized (node) {
      // Traverse list or insert into Red-Black Tree
  }
  ```
* **Reads are Lock-Free:** Value and next-pointers in `Node<K, V>` are declared `volatile`:
  ```java
  static class Node<K,V> implements Map.Entry<K,V> {
      final int hash;
      final K key;
      volatile V val;
      volatile Node<K,V> next;
      // ...
  }
  ```
  This guarantees that reads always observe the most recent committed write without needing to acquire locks.

---

## 3. Why `ConcurrentHashMap` Forbids `null`

### The Ambiguity Problem
In a single-threaded `HashMap`, if `map.get(key)` returns `null`, you can disambiguate the state via:
```java
if (map.get(key) == null) {
    if (map.containsKey(key)) {
        // The key exists and is explicitly mapped to null
    } else {
        // The key is completely absent
    }
}
```

In a multi-threaded context with `ConcurrentHashMap`, this check is inherently broken due to race conditions:
1. Thread A calls `map.get(key)` and receives `null`.
2. Before Thread A can call `map.containsKey(key)`, Thread B removes the key or updates it.
3. Thread A calls `map.containsKey(key)`, returning `false`.
4. Result: Thread A cannot reliably tell whether the key was mapped to `null` or absent at the time of evaluation.

As Doug Lea (author of `java.util.concurrent`) noted, allowing `null` values introduces hidden semantic ambiguities that degrade concurrent correctness.

---

## 4. Iterator Semantics: Fail-Fast vs. Weakly Consistent

### `HashMap`: Fail-Fast
* Relies on an internal counter: `modCount`.
* Every mutation (`put`, `remove`, `clear`) increments `modCount`.
* When an iterator is initialized, it caches `expectedModCount = modCount`.
* On every call to `next()` or `hasNext()`, it checks:
  $$\text{modCount} \stackrel{?}{=} \text{expectedModCount}$$
* If any external modification occurs while iterating, it immediately throws `ConcurrentModificationException`.
* **Important:** Fail-fast behavior is not guaranteed under concurrent data races; it is intended as a bug-detection mechanism.

### `ConcurrentHashMap`: Weakly Consistent
* Iterators do **not** lock the table.
* They traverse elements as they existed at the time of iterator construction.
* Elements added or removed after iterator creation may or may not be reflected in the ongoing traversal.
* Guaranteed **never** to throw `ConcurrentModificationException`.
* Guaranteed to traverse each item present at construction at most once.

---

## 5. Compound Atomic Operations

In concurrent programming, checking a condition and then updating a map is a classic **check-then-act** race condition.

### The Problem with Standard Maps
```java
// Thread-unsafe even with Collections.synchronizedMap() without external locking
if (!map.containsKey(key)) {
    map.put(key, value); // Another thread could write in between!
}
```

### The `ConcurrentHashMap` Solution
`ConcurrentHashMap` exposes atomic compound operations implemented directly at the bucket-synchronization level:

```java
ConcurrentMap<String, Integer> map = new ConcurrentHashMap<>();

// 1. Put if absent (returns existing value, or null if inserted)
map.putIfAbsent("counter", 1);

// 2. Compute if absent (computes value lazily only if key does not exist)
map.computeIfAbsent("heavyKey", k -> loadExpensivePayload(k));

// 3. Compute if present (updates atomically if key exists)
map.computeIfPresent("counter", (k, v) -> v + 1);

// 4. Merge (combines values atomically)
map.merge("counter", 1, Integer::sum);

// 5. Conditional replacement
map.replace("counter", 2, 5); // Replaces only if current value is 2

// 6. Conditional removal
map.remove("counter", 5);     // Removes only if currently mapped to 5
```

---

## 6. Sizing and Performance Mechanics

### Resizing in Multi-Threaded Environments
* In `HashMap`, resizing is single-threaded. Multiple threads resizing a `HashMap` concurrently historically led to infinite loops (circular linked lists in Java 7) or memory leaks/dropped writes (Java 8+).
* In `ConcurrentHashMap`, resizing is **cooperative and parallel**:
  * Resizing creates a new table (`nextTable`) twice the size.
  * A thread initiating resize assigns sub-ranges (strides) of the bucket array using a transfer mechanism.
  * If other threads attempt a `put()` while a resize is ongoing, instead of blocking entirely, they invoke `helpTransfer()`, actively assisting in moving bucket nodes to the new table.

### Concurrent Counters (`size()`)
* `HashMap` maintains a simple `int size` field incremented/decremented directly.
* `ConcurrentHashMap` cannot use a single `volatile int size` counter because concurrent writes would bottleneck on updating that single memory location.
* Instead, it uses a mechanism similar to `LongAdder`:
  * A `baseCount` field for uncontended updates via CAS.
  * A `CounterCell[] counterCells` array when contention occurs.
  * `size()` and `mappingCount()` aggregate the `baseCount` plus all values in `CounterCell[]`.

---

## 7. Comparison with Synchronized Alternatives

| Feature | `HashMap` | `Collections.synchronizedMap(map)` | `Hashtable` | `ConcurrentHashMap` |
| :--- | :--- | :--- | :--- | :--- |
| **Lock Granularity** | None | Whole map object lock | Whole method/object lock (`this`) | Bucket-level lock + CAS |
| **Concurrent Reads** | Unsafe | Blocked by writers & other readers | Blocked by writers & other readers | **Unblocked & Concurrent** |
| **Concurrent Writes** | Unsafe | Sequential (1 writer at a time) | Sequential (1 writer at a time) | **Concurrent (Multiple across different buckets)** |
| **Modern Viability** | High (for local use) | Low (legacy synchronization) | Deprecated / Obsolete | **Standard for Concurrent Use** |

---

## 8. Decision Matrix: When to Use Which

```
Is the map shared across multiple threads with at least one writer?
│
├── NO ───► Use java.util.HashMap
│
└── YES ──► Do you need to allow 'null' keys or values?
            │
            ├── YES ──► Redesign data model (recommended), OR
            │           use Collections.synchronizedMap(new HashMap<>())
            │           with explicit synchronization on blocks.
            │
            └── NO ───► Use java.util.concurrent.ConcurrentHashMap
```