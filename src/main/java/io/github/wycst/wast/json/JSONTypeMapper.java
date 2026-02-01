package io.github.wycst.wast.json;

import java.util.List;
import java.util.Map;

/**
 * <p> 提供更方便的类型拓展注册机制;
 *
 * @Created by wangyc
 */
public interface JSONTypeMapper<T> {

    /**
     * 反序列化时将解析的基本对象(JSONType#value), 转化为指定对象T <br>
     *
     * @param value 基本对象（String, Number, boolean, Map, List,null）
     * @return 转化后的指定对象类型T
     * @see JSONValue#get()
     */
    T readOf(Object value) throws Exception;

    /**
     * 反序列化时将T转化为基本对象
     *
     * @param value      指定对象T
     * @param jsonConfig json配置
     * @return 基本对象（JSON6大类型JSONValue）
     * @see JSONValue#of(Map)
     * @see JSONValue#of(List)
     * @see JSONValue#of(Number)
     * @see JSONValue#of(boolean)
     * @see JSONValue#of(String)
     */
    JSONValue<?> writeAs(T value, JSONConfig jsonConfig) throws Exception;
}
