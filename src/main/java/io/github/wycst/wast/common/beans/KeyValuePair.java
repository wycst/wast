package io.github.wycst.wast.common.beans;

/**
 * @Author: wangy
 * @Date: 2022/11/13 10:23
 * @Description:
 */
public class KeyValuePair<K, V> {

    final K key;
    final V value;

    public KeyValuePair(K k, V v) {
        this.key = k;
        this.value = v;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}
