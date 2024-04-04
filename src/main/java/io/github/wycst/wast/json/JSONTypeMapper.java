package io.github.wycst.wast.json;

/**
 * 提供更方便的类型拓展注册机制
 *
 * @Created by wangyc
 */
public interface JSONTypeMapper<T> {

    /**
     * 将解析的类型对象（String/Number/boolean/null/Map/List）转化为指定对象T
     *
     * @param any
     * @return
     * @see io.github.wycst.wast.json.custom.JsonDeserializer
     */
    T read(Object any);

    /**
     * 将T个性化写入
     *
     * @param writer
     * @param t
     * @param jsonConfig
     * @param indent
     * @see io.github.wycst.wast.json.custom.JsonSerializer
     */
    void write(JSONWriter writer, T t, JSONConfig jsonConfig, int indent);
}
