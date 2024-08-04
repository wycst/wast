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
     * @param value
     * @return
     * @see io.github.wycst.wast.json.custom.JsonDeserializer
     * @see JSONValue#get()
     */
    T readOf(Object value) throws Exception;

    /**
     * 反序列化时将T转化为基本对象
     *
     * @param t
     * @param jsonConfig
     * @return
     * @see io.github.wycst.wast.json.custom.JsonSerializer
     * @see JSONValue#of(Map)
     * @see JSONValue#of(List)
     * @see JSONValue#of(Number)
     * @see JSONValue#of(boolean)
     * @see JSONValue#of(String)
     */
    JSONValue<?> writeAs(T t, JSONConfig jsonConfig) throws Exception;
}
