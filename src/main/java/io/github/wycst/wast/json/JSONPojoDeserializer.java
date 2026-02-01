package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;

public abstract class JSONPojoDeserializer extends JSONTypeDeserializer {

    abstract Object deserializePojo(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object entity, JSONParseContext parseContext) throws Exception;

    abstract Object deserializePojo(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object entity, JSONParseContext parseContext) throws Exception;

    protected abstract Object createPojo() throws Exception;

    protected abstract Object pojo(Object value);
}
