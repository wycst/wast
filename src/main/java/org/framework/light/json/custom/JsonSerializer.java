package org.framework.light.json.custom;

import org.framework.light.json.options.JsonConfig;

import java.io.Writer;

/**
 * 自定义序列化抽象类（Custom serialization abstract class）
 *
 * @Author: wangy
 * @Description:
 */
public abstract class JsonSerializer<E> {

    /***
     * 自定义序列化
     *
     * @param writer
     * @param e
     * @param jsonConfig
     */
    public abstract void serialize(Writer writer, E e, int indentLevel, JsonConfig jsonConfig) throws Exception;

}
