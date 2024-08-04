package io.github.wycst.wast.json;

import java.util.List;
import java.util.Map;

/**
 * JSON字符串中value可选类型约定为6种: String, Number, boolean, Map, List,null， 除了null外都可以使用valueOf进行显示类型的构造；
 *
 * @Date 2024/8/2 23:46
 * @Created by wangyc
 */
public final class JSONValue<T> {

    final T value;

    JSONValue(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public static JSONValue<Boolean> of(boolean value) {
        return new JSONValue<Boolean>(value);
    }

    public static JSONValue<Number> of(Number value) {
        return new JSONValue<Number>(value);
    }

    public static JSONValue<String> of(String value) {
        return new JSONValue<String>(value);
    }

    public static JSONValue<Map> of(Map value) {
        return new JSONValue<Map>(value);
    }

    public static JSONValue<List> of(List value) {
        return new JSONValue<List>(value);
    }
}
