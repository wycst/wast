package io.github.wycst.wast.common.beans;

import io.github.wycst.wast.common.utils.CollectionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p> 固定长度的队列Map,先进先出
 *
 * @Date 2024/10/22 18:29
 * @Created by wangyc
 */
public final class ArrayQueueMap<K, V> {

    final Object[] keys;
    final Map<K, V> map;
    final int size;
    int count;

    public ArrayQueueMap(int size) {
        keys = new Object[size];
        map = new ConcurrentHashMap<K, V>(size);
        this.size = size;
    }

    public V get(K k) {
        return map.get(k);
    }

    public void put(K k, V v) {
        V oldValue = map.get(k);
        if (oldValue != null) {
            map.put(k, v);
        } else {
            synchronized (this) {
                int index = count < size ? count++ : (count = 0);
                Object oldKey = keys[index];
                if (oldKey != null) {
                    map.remove(oldKey);
                }
                keys[index] = k;
                map.put(k, v);
            }
        }
    }

    public V remove(K k) {
        return map.remove(k);
    }

    public void clear() {
        synchronized (this) {
            count = 0;
            map.clear();
            CollectionUtils.clear(keys);
        }
    }
}
