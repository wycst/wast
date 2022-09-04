package io.github.wycst.wast.json.util;

/**
 * 固定长度的map结构
 *
 * @Author: wangy
 * @Date: 2022/7/9 11:57
 * @Description:
 */
public class FixedNameValueMap<T> {

    private final int capacity;
    private final NameValueEntryNode<T>[] valueEntryNodes;
    private int count;
    private final int maxCount;

    public FixedNameValueMap(int size) {
        int capacity = tableSizeFor(size) << 1;
        if (size > 1) {
            capacity = Math.max(capacity, 16);
        }
        this.maxCount = size;
        this.capacity = capacity;
        valueEntryNodes = new NameValueEntryNode[capacity];
    }

    public void putValue(String name, T value) {
        synchronized (this) {
            if (count >= maxCount) {
                return;
            }
            count++;
        }
        char[] keyChars = name.toCharArray();
        int keyHash = name.hashCode();
        int index = keyHash & capacity - 1;
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
    public T getValue(char[] buf, int beginIndex, int endIndex, int hashValue) {
        int len = endIndex - beginIndex;
        int index = hashValue & capacity - 1;
        NameValueEntryNode<T> entryNode = valueEntryNodes[index];
        if (entryNode == null) {
            return null;
        }
        // Is there an efficient logic that can determine the match?
        while (!matchKey(buf, beginIndex, len, entryNode.key)) {
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
    public T getValue(byte[] bytes, int beginIndex, int endIndex, int hashValue) {
        int len = endIndex - beginIndex;
        int index = hashValue & capacity - 1;
        NameValueEntryNode<T> entryNode = valueEntryNodes[index];
        if (entryNode == null) {
            return null;
        }
        // Is there an efficient logic that can determine the match?
        while (!matchKey(bytes, beginIndex, len, entryNode.key)) {
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
    public T getValueByHash(int hashValue) {
        int index = hashValue & capacity - 1;
        NameValueEntryNode<T> entryNode = valueEntryNodes[index];
        if (entryNode == null) {
            return null;
        }
        while (hashValue != entryNode.hash) {
            entryNode = entryNode.next;
            if (entryNode == null) {
                return null;
            }
        }
        return entryNode.value;
    }

    static class NameValueEntryNode<T> {
        public NameValueEntryNode(char[] key, T value, int keyHash) {
            this.key = key;
            this.hash = keyHash;
            this.value = value;
        }

        int hash;
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
     * matchKey
     *
     * @param buf
     * @param offset
     * @param len
     * @param key
     * @return
     */
    private static boolean matchKey(char[] buf, int offset, int len, char[] key) {
        if (len != key.length) return false;
        for (int j = 0; j < len; j++) {
            if (buf[offset + j] != key[j]) return false;
        }
        return true;
    }

    /**
     * matchKey
     *
     * @param bytes
     * @param offset
     * @param len
     * @param key
     * @return
     */
    private static boolean matchKey(byte[] bytes, int offset, int len, char[] key) {
        if (len != key.length) return false;
        for (int j = 0; j < len; j++) {
            if (bytes[offset + j] != key[j]) return false;
        }
        return true;
    }


}
