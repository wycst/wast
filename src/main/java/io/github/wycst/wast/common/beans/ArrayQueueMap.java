package io.github.wycst.wast.common.beans;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p> 固定长度的队列Map,先进先出
 *
 * @Date 2024/10/22 18:29
 * @Created by wangyc
 */
public final class ArrayQueueMap<K, V> extends LinkedHashMap<K, V> {

    final int limit;

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > limit;
    }

    public ArrayQueueMap(int limit) {
        super(limit, 0.75F, true);
        this.limit = limit;
    }
}
