package io.github.wycst.wast.jdbc.transform;

import io.github.wycst.wast.json.JSON;

/**
 * JSON转换器
 *
 */
public class JSONTypeTransformer extends TypeTransformer {

    @Override
    public Object fromJavaField(Object value) {
        return JSON.toJsonString(value);
    }

    @Override
    public Object toJavaField(Object value) {
        return value == null ? null : JSON.parse((String) value, getParameterizedType());
    }
}
