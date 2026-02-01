package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.utils.EnvUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wangy
 * @Description:
 */
public class JSONPojoFieldSerializer {
    final JSONStore store;
    final GetterInfo getterInfo;
    final JSONPropertyDefinition propertyDefinition;
    final String name;
    final ReflectConsts.ClassCategory classCategory;
    JSONTypeSerializer serializer;
    private char[] fieldNameTokenChars;
    private String fieldNameToken;
    private int fieldNameTokenOffset;
    private long[] fieldNameCharLongs;
    private long[] fieldNameByteLongs;
    private boolean flag;
    private boolean customSerialize;

    // 自定义序列化器
    private final static Map<Class<? extends JSONTypeFieldMapper>, JSONTypeSerializer> customSerializers = new ConcurrentHashMap<Class<? extends JSONTypeFieldMapper>, JSONTypeSerializer>();

    JSONPojoFieldSerializer(JSONStore store, GetterInfo getterInfo, String name, JSONPropertyDefinition propertyDefinition) {
        this.store = store;
        this.getterInfo = getterInfo;
        this.classCategory = getterInfo.getClassCategory();
        this.name = name;
        this.propertyDefinition = propertyDefinition;
        setFieldNameToken();
    }

    void initSerializer() {
        if (flag) return;
        if (this.serializer == null) {
            flag = true;
            this.serializer = createSerializer();
        }
    }

    private JSONTypeSerializer createSerializer() {
        // check custom Serializer
        if (propertyDefinition != null) {
            try {
                Class<? extends JSONTypeFieldMapper> fieldMapperClass = propertyDefinition.mapper();
                if (fieldMapperClass != JSONTypeFieldMapper.class) {
                    JSONTypeSerializer serializer = customSerializers.get(fieldMapperClass);
                    if (serializer != null) {
                        customSerialize = true;
                        return serializer;
                    }
                    JSONTypeFieldMapper<?> fieldMapper = fieldMapperClass.newInstance();
                    if (fieldMapper.serialize()) {
                        serializer = store.buildSerializer(fieldMapper);
                        if (fieldMapper.singleton()) {
                            customSerializers.put(fieldMapperClass, serializer);
                        }
                        customSerialize = true;
                        return serializer;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        Class<?> returnType = getterInfo.getReturnType();
        if (returnType == String.class) {
            return store.STRING_SER;
        } else if (classCategory == ReflectConsts.ClassCategory.NumberCategory) {
            // From cache by different number types (int/float/double/long...)
            return store.getTypeSerializer(returnType);
        } else {
            GenericParameterizedType<?> genericParameterizedType = getterInfo.getGenericParameterizedType();
            if (classCategory == ReflectConsts.ClassCategory.CollectionCategory && genericParameterizedType != null) {
                GenericParameterizedType<?> valueType = genericParameterizedType.getValueType();
                if (valueType != null) {
                    Class<?> valueClass = valueType.getActualType();
                    if (Modifier.isFinal(valueClass.getModifiers())) {
                        return store.createCollectionSerializer(valueClass);
                    }
                }
            }
            return store.getFieldTypeSerializer(classCategory, returnType, propertyDefinition);
        }
    }

    public boolean isStringCollection() {
        if (classCategory == ReflectConsts.ClassCategory.CollectionCategory) {
            GenericParameterizedType<?> parameterizedType = getterInfo.getGenericParameterizedType();
            return parameterizedType != null && parameterizedType.getValueType() == GenericParameterizedType.StringType;
        }
        return false;
    }

    private void setFieldNameToken() {
        int len = name.length();
        // "${name}":null
        fieldNameTokenChars = new char[len + 7];
        int i = 0;
        fieldNameTokenChars[i++] = '"';
        name.getChars(0, len, fieldNameTokenChars, i);
        fieldNameTokenChars[len + 1] = '"';
        fieldNameTokenChars[len + 2] = ':';
        fieldNameTokenOffset = len + 3;
        fieldNameTokenChars[len + 3] = 'n';
        fieldNameTokenChars[len + 4] = 'u';
        fieldNameTokenChars[len + 5] = 'l';
        fieldNameTokenChars[len + 6] = 'l';
        if (EnvUtils.JDK_9_PLUS) {
            fieldNameToken = new String(fieldNameTokenChars);
        }
        // optimize use unsafe(适用JDK8，JDK9+提升不明显)
        if (name.getBytes().length == len) {
            String stringForUnsafe = new String(fieldNameTokenChars, 0, fieldNameTokenOffset);
            fieldNameCharLongs = JSONMemoryHandle.getCharLongs(stringForUnsafe);
            fieldNameByteLongs = JSONMemoryHandle.getByteLongs(stringForUnsafe);
        }
    }

//    Object invoke(Object pojo) {
//        // return getterInfo.invoke(pojo);
//        return JSON_SECURE_TRUSTED_ACCESS.get(getterInfo, pojo);
//    }
//
//    <T> T invoke(Object pojo, Class<T> tClass) {
//        return (T) JSON_SECURE_TRUSTED_ACCESS.get(getterInfo, pojo);
//    }

    void writeFieldNameAndColonTo(JSONWriter writer) throws IOException {
        if (fieldNameCharLongs != null) {
            writer.writeMemory(fieldNameCharLongs, fieldNameByteLongs, fieldNameTokenOffset);
        } else {
            if (fieldNameToken != null) {
                writer.write(fieldNameToken, 0, fieldNameTokenOffset);
            } else {
                writer.writeShortChars(fieldNameTokenChars, 0, fieldNameTokenOffset);
            }
        }
    }

    void writeJSONFieldNameWithNull(JSONWriter writer) throws IOException {
        if (fieldNameToken != null) {
            writer.write(fieldNameToken, 0, fieldNameTokenChars.length);
        } else {
            writer.writeShortChars(fieldNameTokenChars, 0, fieldNameTokenChars.length);
        }
    }

    public JSONTypeSerializer getSerializer() {
        return serializer;
    }

    public boolean isCustomSerialize() {
        return customSerialize;
    }

    public String getName() {
        return name;
    }

    public JSONPropertyDefinition getPropertyDefinition() {
        return propertyDefinition;
    }
}
