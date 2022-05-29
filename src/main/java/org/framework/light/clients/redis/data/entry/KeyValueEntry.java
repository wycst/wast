package org.framework.light.clients.redis.data.entry;

import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2020/5/28 9:54
 * @Description:
 */
public class KeyValueEntry implements Map.Entry<String,String> {

    private final String key;
    private String value;

    public KeyValueEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String setValue(String value) {
        return this.value = value;
    }

    @Override
    public String toString() {
        return "KeyValueEntry{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
