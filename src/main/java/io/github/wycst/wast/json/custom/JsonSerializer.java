package io.github.wycst.wast.json.custom;

import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONTypeSerializer;
import io.github.wycst.wast.json.JSONWriter;

import java.io.Writer;

/**
 * 自定义序列化抽象类（Custom serialization abstract class）
 *
 * @Author: wangy
 * @Description:
 */
public abstract class JsonSerializer<E> extends JSONTypeSerializer {

    @Override
    protected final void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        serialize(writer, (E) value, indent, jsonConfig);
    }

    /***
     * 自定义序列化
     *
     * @param writer
     * @param e
     * @param jsonConfig
     */
    public abstract void serialize(JSONWriter writer, E e, int indentLevel, JSONConfig jsonConfig) throws Exception;

}
