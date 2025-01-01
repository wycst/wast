package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.json.annotations.JsonDeserialize;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.custom.JsonDeserializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性反序列化模型
 *
 * @Author wangyunchao
 */
public final class JSONPojoFieldDeserializer extends JSONTypeDeserializer implements Comparable<JSONPojoFieldDeserializer> {

    final String name;
    final int fieldIndex;
    final SetterInfo setterInfo;
    final JsonProperty jsonProperty;
    /**
     * 类型信息
     */
    GenericParameterizedType genericParameterizedType;

    /**
     * 类型分类
     */
    final ReflectConsts.ClassCategory classCategory;

    Class<?> implClass;

    /**
     * 反序列化器
     */
    JSONTypeDeserializer deserializer;

    /**
     * 是否自定义反序列器
     */
    boolean customDeserialize = false;

    final String pattern;
    final String timezone;
    boolean flag;

    /**
     * 自定义反序列化器
     */
    private final static Map<Class<? extends JsonDeserializer>, JsonDeserializer> customDeserializers = new ConcurrentHashMap<Class<? extends JsonDeserializer>, JsonDeserializer>();
    private boolean priority;

    JSONPojoFieldDeserializer(String name, SetterInfo setterInfo, JsonProperty jsonProperty) {
        this.name = name;
        this.setterInfo = setterInfo;
        this.fieldIndex = setterInfo.getIndex();
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
        if (!flag) {
            flag = true;
            Class impl;
            boolean unfixedType = false;
            if (jsonProperty != null) {
                if ((impl = jsonProperty.impl()) != Object.class) {
                    if (isAvailableImpl(impl)) {
                        implClass = impl;
                    }
                }
                unfixedType = jsonProperty.unfixedType();
            }
            if (implClass != null) {
                this.deserializer = getTypeDeserializer(implClass);
                this.genericParameterizedType = GenericParameterizedType.actualType(implClass);
            } else {
                if (setterInfo.isNonInstanceType()) {
                    this.deserializer = getCachedTypeDeserializer(genericParameterizedType.getActualType());
                } else {
                    if (genericParameterizedType.getActualClassCategory() == ReflectConsts.ClassCategory.ObjectCategory && unfixedType) {
                        this.deserializer = null;
                    } else {
                        this.deserializer = getDeserializer(genericParameterizedType);
                    }
                }
            }
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

    public boolean isCustomDeserialize() {
        return customDeserialize;
    }

//    public SetterInfo getSetterInfo() {
//        return setterInfo;
//    }

//    public GenericParameterizedType getGenericParameterizedType() {
//        return genericParameterizedType;
//    }

    protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    Object getDefaultFieldValue(Object instance) {
        return JSON_SECURE_TRUSTED_ACCESS.getSetterDefault(setterInfo, instance);
    }

    public int getIndex() {
        return setterInfo.getIndex();
    }

//    public void invoke(Object entity, Object value) {
//        JSON_SECURE_TRUSTED_ACCESS.set(setterInfo, entity, value); // setterInfo.invoke(entity, value);
//    }

    public Class<?> getImplClass() {
        return implClass;
    }

    public boolean isAvailableImpl(Class<?> cls) {
        boolean assignableFrom = genericParameterizedType.getActualType().isAssignableFrom(cls);
        if (!assignableFrom) return false;
        switch (classCategory) {
            case MapCategory:
            case CollectionCategory:
            case ObjectCategory:
                return true;
        }
        return false;
    }

    public boolean isInstance(Object value) {
        return value == null || genericParameterizedType.getActualType().isInstance(value);
    }

    public void setPriority(boolean priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(JSONPojoFieldDeserializer o) {
        if (setterInfo != o.setterInfo) return -1;
        return priority ? 1 : 0;
    }
}
