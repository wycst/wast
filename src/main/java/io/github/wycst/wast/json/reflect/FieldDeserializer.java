package io.github.wycst.wast.json.reflect;

import io.github.wycst.wast.common.beans.CharSource;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.json.JSONTypeDeserializer;
import io.github.wycst.wast.json.annotations.JsonDeserialize;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.custom.JsonDeserializer;
import io.github.wycst.wast.json.options.JSONParseContext;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性反序列化模型
 *
 * @Author wangyunchao
 */
public class FieldDeserializer extends JSONTypeDeserializer {

    /**
     * name
     */
    private final String name;

    /**
     * hash
     */
    private final int hash;

    /**
     * setter信息
     */
    private final SetterInfo setterInfo;

    private final JsonProperty jsonProperty;

    /**
     * 类型信息
     */
    private final GenericParameterizedType genericParameterizedType;

    /**
     * 类型分类
     */
    private final ReflectConsts.ClassCategory classCategory;

    private Class<?> implClass;

    /**
     * 反序列化器
     */
    private JSONTypeDeserializer deserializer;

    /**
     * 是否自定义反序列器
     */
    private boolean customDeserialize = false;

    private final String pattern;
    private final String timezone;

    /**
     * 自定义反序列化器
     */
    private final static Map<Class<? extends JsonDeserializer>, JsonDeserializer> customDeserializers = new ConcurrentHashMap<Class<? extends JsonDeserializer>, JsonDeserializer>();

    FieldDeserializer(String name, SetterInfo setterInfo, JsonProperty jsonProperty) {
        if (setterInfo == null) {
            throw new IllegalArgumentException("setterInfo is null");
        }
        this.name = name;
        this.hash = name.hashCode();
        this.setterInfo = setterInfo;
        this.jsonProperty = jsonProperty;
        this.genericParameterizedType = setterInfo.getGenericParameterizedType();

        Class<?> actualClass = genericParameterizedType.getActualType();
        this.classCategory = ReflectConsts.getClassCategory(actualClass);

        String pattern = null;
        String timezone = null;
        if (jsonProperty != null) {
            pattern = jsonProperty.pattern().trim();
            timezone = jsonProperty.timezone().trim();
            if (pattern.length() == 0) {
                pattern = null;
            }
            if (timezone.length() == 0) {
                timezone = null;
            }
        }
        this.pattern = pattern;
        this.timezone = timezone;
    }

    void initDeserializer() {
        if (setterInfo.isNonInstanceType()) {
            if (genericParameterizedType.getActualType() == Serializable.class) {
                this.deserializer = SERIALIZABLE_DESERIALIZER;
            } else {
                this.deserializer = null;
            }
            Class impl;
            if (jsonProperty != null && (impl = jsonProperty.impl()) != Object.class) {
                if (isAvailableImpl(impl)) {
                    implClass = impl;
                }
            }
        } else {
            this.deserializer = getDeserializer(genericParameterizedType);
        }
    }

    private JSONTypeDeserializer getDeserializer(GenericParameterizedType genericParameterizedType) {
        // check custom Deserializer
        JsonDeserialize jsonDeserialize = (JsonDeserialize) setterInfo.getAnnotation(JsonDeserialize.class);
        if (jsonDeserialize != null) {
            Class<? extends JsonDeserializer> jsonDeserializerClass = jsonDeserialize.value();
            this.customDeserialize = true;
            try {
                if (jsonDeserialize.singleton()) {
                    JsonDeserializer jsonDeserializer = customDeserializers.get(jsonDeserializerClass);
                    if (jsonDeserializer == null) {
                        jsonDeserializer = jsonDeserializerClass.newInstance();
                        customDeserializers.put(jsonDeserializerClass, jsonDeserializer);
                    }
                    return jsonDeserializer;
                }
                return jsonDeserializerClass.newInstance();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return JSONTypeDeserializer.getFieldDeserializer(genericParameterizedType, jsonProperty);
    }

    public String getName() {
        return name;
    }

    public int getHash() {
        return hash;
    }

    public boolean isCustomDeserialize() {
        return customDeserialize;
    }

    public SetterInfo getSetterInfo() {
        return setterInfo;
    }

    public GenericParameterizedType getGenericParameterizedType() {
        return genericParameterizedType;
    }

    protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    public ReflectConsts.ClassCategory getClassCategory() {
        return classCategory;
    }

    public JSONTypeDeserializer getDeserializer() {
        return deserializer;
    }

    public Object getDefaultFieldValue(Object instance) {
        return setterInfo.getDefaultFieldValue(instance);
    }

    public boolean isNonInstanceType() {
        return setterInfo.isNonInstanceType();
    }

    public int getIndex() {
        return setterInfo.getIndex();
    }

    public void invoke(Object entity, Object value) {
        setterInfo.invoke(entity, value);
    }

    public String getDatePattern() {
        return pattern;
    }

    public String getDateTimezone() {
        return timezone;
    }

    public Class<?> getImplClass() {
        return implClass;
    }

    public boolean isAvailableImpl(Class<?> cls) {
        return genericParameterizedType.getActualType().isAssignableFrom(cls) && ReflectConsts.getClassCategory(cls) == ReflectConsts.ClassCategory.ObjectCategory;
    }

    public boolean isInstance(Object value) {
        return value == null || genericParameterizedType.getActualType().isInstance(value);
    }
}
