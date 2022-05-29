package org.framework.light.json.custom;

import org.framework.light.json.options.JSONParseContext;

/**
 * 自定义反序列化抽象类（Custom deserialization abstract class）
 * <p> 注： 使用无参构造方法实例化
 *
 * @Author: wangy
 * @Description:
 */
public abstract class JsonDeserializer<T> {

    /**
     * 自定义反序列化
     *
     * @param value
     * @param source
     * @param parseContext
     * @return
     */
    public abstract T deserialize(Object value, String source, JSONParseContext parseContext) throws Exception;

}
