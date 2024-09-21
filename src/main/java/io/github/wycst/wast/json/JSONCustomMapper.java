package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.json.options.ReadOption;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 针对pojo提供定制化输出和解析
 *
 * @Date 2024/8/31 17:23
 * @Created by wangyc
 */
public final class JSONCustomMapper {

    final Map<Class<?>, JSONTypeSerializer> customSerializerMap;
    final Map<Class<?>, JSONTypeMapper> typeMapperMap;

    public JSONCustomMapper() {
        customSerializerMap = new ConcurrentHashMap<Class<?>, JSONTypeSerializer>();
        typeMapperMap = new ConcurrentHashMap<Class<?>, JSONTypeMapper>();
    }

    public <E> void register(Class<E> pojoClsss, JSONTypeMapper<E> typeMapper) {
        register(pojoClsss, typeMapper, true, true);
    }

    /**
     * 针对pojo类的个性化定制序列化和反序列化
     *
     * @param pojoClsss
     * @param typeMapper
     * @param customSerialize
     * @param customDeserialize
     * @param <E>
     */
    public <E> void register(Class<E> pojoClsss, JSONTypeMapper<E> typeMapper, boolean customSerialize, boolean customDeserialize) {
        ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(pojoClsss);
        if (classCategory != ReflectConsts.ClassCategory.ObjectCategory) {
            throw new IllegalArgumentException("custom only supported pojo class, but " + pojoClsss);
        }
        if (customSerialize) {
            customSerializerMap.put(pojoClsss, JSON.buildCustomizedSerializer(typeMapper));
        }
        if (customDeserialize) {
            typeMapperMap.put(pojoClsss, typeMapper);
        }
    }

    JSONTypeSerializer getCustomizedSerializer(Class<?> pojoClass) {
        JSONTypeSerializer typeSerializer = customSerializerMap.get(pojoClass);
        if (typeSerializer != null) {
            return typeSerializer;
        }
        return JSONTypeSerializer.getTypeSerializer(pojoClass);
    }

    void serializeCustomized(Object obj, JSONWriter content, JSONConfig jsonConfig, int indentLevel) throws Exception {
        getCustomizedSerializer(obj.getClass()).serializeCustomized(obj, content, jsonConfig, indentLevel, this);
    }

    <T> T parseCustomObject(String json, Class<T> actualType, ReadOption[] readOptions) throws Exception {
        JSONTypeMapper<T> typeMapper = typeMapperMap.get(actualType);
        if(typeMapper == null) {
            return JSON.parseObject(json, actualType, readOptions);
        }
        Object result = JSONDefaultParser.parse(json, readOptions);
        return typeMapper.readOf(result);
    }
}
