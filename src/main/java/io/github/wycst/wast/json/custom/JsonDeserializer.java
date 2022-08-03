package io.github.wycst.wast.json.custom;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.JSONTypeDeserializer;
import io.github.wycst.wast.json.options.JSONParseContext;

/**
 * 自定义反序列化抽象类（Custom deserialization abstract class）
 * <p> 注： 使用无参构造方法实例化
 *
 * @Author: wangy
 * @Description:
 */
public abstract class JsonDeserializer<T> extends JSONTypeDeserializer {

    private boolean useSource;

    public final void setUseSource(boolean useSource) {
        this.useSource = useSource;
    }

    @Override
    protected final Object deserialize(char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
        Object value = JSONTypeDeserializer.doDeserialize(ANY, buf, fromIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
        int endIndex = jsonParseContext.getEndIndex();
        return deserialize(value, useSource ? new String(buf, fromIndex, endIndex - fromIndex) : null, null);
    }

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
