package io.github.wycst.wast.json.util;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 固定容量的map结构
 *
 * @Author: wangy
 * @Date: 2022/7/9 11:57
 * @Description:
 */
public class FixedNameValueMap<T> {

    final int capacity;
    final int mask;
    final NameValueEntryNode<T>[] valueEntryNodes;
    int count;
    final int maxCount;

    int primeValue;

    boolean collision;

    static final int[] PRIMES = new int[] {5, 7, 11, 13, 17, 19, 23, 29, 31};

    public static <E> FixedNameValueMap<E> build(Map<String, E> values) {
        int size = values.size();
        BHFixedNameValueMap<E> bhNameValueMap = new BHFixedNameValueMap<E>(size << 1);
        final int capacity = bhNameValueMap.capacity;
        NameValueEntryNode<E>[] valueEntryNodes = bhNameValueMap.valueEntryNodes;

        String[] names = values.keySet().toArray(new String[size]);
        long[] hashValues = new long[size];
        int[] remValues = new int[size];
        Set<Integer> remSet = new LinkedHashSet<Integer>();
        int bits = 0;
        while (bits < 15) {
            remSet.clear();
            for (int i = 0 ; i < size; ++i) {
                String name = names[i];
                long hashValue = bitHash(name, bits);
                int rem = (int) (hashValue & capacity - 1);
                hashValues[i] = hashValue;
                remValues[i] = rem;
                remSet.add(rem);
            }
            if (remSet.size() == size) {
                for (int i = 0 ; i < size; ++i) {
                    String name = names[i];
                    long hashValue = hashValues[i];
                    E value = values.get(name);
                    valueEntryNodes[remValues[i]] = new NameValueEntryNode<E>(name.toCharArray(), value, hashValue);
                }
                if(bits == 0) {
                    return new PHFixedNameValueMap<E>(size, capacity, valueEntryNodes);
                }
                bhNameValueMap.bits = bits;
                bhNameValueMap.count = size;
                return bhNameValueMap;
            }
            ++bits;
        }

        // use default
        // Find the most suitable prime number to minimize conflicts
        FixedNameValueMap<E> dftNameValueMap = new FixedNameValueMap<E>(size << 1);
        Set<Long> hashValueSet = new LinkedHashSet<Long>();
        int primeValue = 5;
        for (int prime : PRIMES) {
            primeValue= prime;
            hashValueSet.clear();
            remSet.clear();
            for (int i = 0 ; i < size; ++i) {
                String name = names[i];
                long hashValue = primeHash(name, prime);
                int rem = (int) (hashValue & capacity - 1);
                hashValues[i] = hashValue;
                remValues[i] = rem;
                hashValueSet.add(hashValue);
                remSet.add(rem);
            }
            if (remSet.size() == size) {
                valueEntryNodes = dftNameValueMap.valueEntryNodes;
                dftNameValueMap.primeValue = primeValue;
                for (int i = 0 ; i < size; ++i) {
                    String name = names[i];
                    long hashValue = hashValues[i];
                    E value = values.get(name);
                    valueEntryNodes[remValues[i]] = new NameValueEntryNode<E>(name.toCharArray(), value, hashValue);
                }
                return dftNameValueMap;
            }
        }

        dftNameValueMap.collision = hashValueSet.size() < size;
        dftNameValueMap.primeValue = primeValue;
        for (int i = 0 ; i < size; ++i) {
            String name = names[i];
            long hashValue = hashValues[i];
            E value = values.get(name);
            dftNameValueMap.putValue(name, hashValue, value);
        }
        return dftNameValueMap;
    }

    public FixedNameValueMap(int size) {
        int capacity = tableSizeFor(size) << 1;
        if (size > 1) {
            capacity = Math.max(capacity, 16);
        }
        this.maxCount = size;
        this.capacity = capacity;
        this.mask = capacity - 1;
        valueEntryNodes = new NameValueEntryNode[capacity];
    }

    FixedNameValueMap(int capacity, NameValueEntryNode[] valueEntryNodes) {
        this.maxCount = capacity << 1;
        this.capacity = capacity;
        this.mask = capacity - 1;
        this.valueEntryNodes = valueEntryNodes;
    }

    public void putValue(String name, long keyHash, T value) {
        synchronized (this) {
            if (count >= maxCount) {
                return;
            }
            count++;
        }
        char[] keyChars = name.toCharArray();
        int index = (int) (keyHash & mask);
        NameValueEntryNode valueEntryNode = new NameValueEntryNode(keyChars, value, keyHash);
        NameValueEntryNode oldEntryNode = valueEntryNodes[index];
        valueEntryNodes[index] = valueEntryNode;
        if (oldEntryNode != null) {
            // use link table
            valueEntryNode.next = oldEntryNode;
        }
    }

    public void reset() {
        synchronized (this) {
            count = 0;
            for (int i = 0; i < capacity; ++i) {
                valueEntryNodes[i] = null;
            }
        }
    }

    public T getValue(String field) {
        char[] buf = field.toCharArray();
        int hashValue = field.hashCode();
        return getValue(buf, 0, buf.length, hashValue);
    }

    /**
     * default
     *
     * @param buf
     * @param beginIndex
     * @param endIndex
     * @param hashValue
     * @return
     */
    public T getValue(char[] buf, int beginIndex, int endIndex, long hashValue) {
        int len = endIndex - beginIndex;
        int index = (int) (hashValue & mask);
        NameValueEntryNode<T> entryNode = valueEntryNodes[index];
        if (entryNode == null) {
            return null;
        }
        // Is there an efficient logic that can determine the match?
        while (!equalsKey(buf, beginIndex, len, entryNode.key)) {
            entryNode = entryNode.next;
            if (entryNode == null) {
                return null;
            }
        }

        return entryNode.value;
    }

    /**
     * default
     *
     * @param bytes
     * @param beginIndex
     * @param endIndex
     * @param hashValue
     * @return
     */
    public T getValue(byte[] bytes, int beginIndex, int endIndex, long hashValue) {
        int len = endIndex - beginIndex;
        int index = (int) (hashValue & mask);
        NameValueEntryNode<T> entryNode = valueEntryNodes[index];
        if (entryNode == null) {
            return null;
        }
        // Is there an efficient logic that can determine the match?
        while (!equalsKey(bytes, beginIndex, len, entryNode.key)) {
            entryNode = entryNode.next;
            if (entryNode == null) {
                return null;
            }
        }

        return entryNode.value;
    }

    /**
     * Ensure that the hash does not collide, otherwise do not use it
     *
     * @param hashValue
     * @return
     */
    public final T getValueByHash(long hashValue) {
        int index = (int) (hashValue & mask);
        NameValueEntryNode<T> entryNode = valueEntryNodes[index];
        if(entryNode != null && entryNode.hash == hashValue) {
            return entryNode.value;
        }
        if(entryNode != null) {
            entryNode = entryNode.next;
            while (entryNode != null) {
                if(entryNode.hash == hashValue) {
                    return entryNode.value;
                }
                entryNode = entryNode.next;
            }
        }
        return null;
    }

    public boolean isCollision() {
        return collision;
    }

    public long hash(long hv, int c1, int c2) {
        hv = hash(hv, c1);
        return hash(hv, c2);
    }

    static class NameValueEntryNode<T> {
        public NameValueEntryNode(char[] key, T value, long keyHash) {
            this.key = key;
            this.hash = keyHash;
            this.value = value;
        }
        long hash;
        char[] key;
        T value;
        NameValueEntryNode<T> next;
    }

    /**
     * @param cap
     * @return
     * @See java.util.HashMap#tableSizeFor(int)
     */
    private static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= 1 << 30) ? 1 << 30 : n + 1;
    }

    /**
     * equalsKey
     *
     * @param buf
     * @param offset
     * @param len
     * @param key
     * @return
     */
    private static boolean equalsKey(char[] buf, int offset, int len, char[] key) {
        if (len != key.length) return false;
        for (int j = 0; j < len; ++j) {
            if (buf[offset++] != key[j]) return false;
        }
        return true;
    }

    /**
     * equalsKey
     *
     * @param bytes
     * @param offset
     * @param len
     * @param key
     * @return
     */
    private static boolean equalsKey(byte[] bytes, int offset, int len, char[] key) {
        if (len != key.length) return false;
        for (int j = 0; j < len; ++j) {
            if (bytes[offset++] != key[j]) return false;
        }
        return true;
    }

    public long hash(long rv, int c) {
        return rv * primeValue + c;
    }

    static final long bitHash(String name, int bits) {
        long val = 0;
        for (int i = 0 ; i < name.length(); ++i) {
            val = (val << bits) + name.charAt(i);
        }
        return val;
    }

    static final long primeHash(String name, int primeValue) {
        long val = 0;
        for (int i = 0 ; i < name.length(); ++i) {
            val = val * primeValue + name.charAt(i);
        }
        return val;
    }
    static class BHFixedNameValueMap<E> extends FixedNameValueMap {
        private int bits;
        public BHFixedNameValueMap(int size) {
            super(size);
        }
        @Override
        public final long hash(long rv, int c) {
            return (rv << bits) + c;
        }
    }
    static class PHFixedNameValueMap<E> extends FixedNameValueMap {
        public PHFixedNameValueMap(int count, int capacity, NameValueEntryNode[] valueEntryNodes) {
            super(capacity, valueEntryNodes);
            this.count = count;
        }
        @Override
        public final long hash(long rv, int c) {
            return rv + c;
        }

        @Override
        public long hash(long rv, int c1, int c2) {
            return rv + c1 + c2;
        }
    }
}
