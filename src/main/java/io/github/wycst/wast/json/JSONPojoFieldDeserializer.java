package io.github.wycst.wast.json;

import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.SetterInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性反序列化模型
 *
 * @Author wangyunchao
 */
public final class JSONPojoFieldDeserializer extends JSONTypeDeserializer implements Comparable<JSONPojoFieldDeserializer> {
    final JSONStore store;
    final String name;
    final int fieldIndex;
    final SetterInfo setterInfo;
    final JSONPropertyDefinition propertyDefinition;
    /**
     * 类型信息
     */
    GenericParameterizedType<?> genericParameterizedType;

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

    // 自定义反序列化器
    private final static Map<Class<? extends JSONTypeFieldMapper>, JSONTypeDeserializer> customDeserializers = new ConcurrentHashMap<Class<? extends JSONTypeFieldMapper>, JSONTypeDeserializer>();
    private boolean priority;

    JSONPojoFieldDeserializer(JSONStore store, String name, SetterInfo setterInfo, JSONPropertyDefinition propertyDefinition) {
        this.store = store;
        this.name = name;
        this.setterInfo = setterInfo;
        this.fieldIndex = setterInfo.getIndex();
        this.propertyDefinition = propertyDefinition;
        this.genericParameterizedType = setterInfo.getGenericParameterizedType();

        Class<?> actualClass = genericParameterizedType.getActualType();
        this.classCategory = ReflectConsts.getClassCategory(actualClass);

        String pattern = null;
        String timezone = null;
        if (propertyDefinition != null) {
            pattern = propertyDefinition.pattern();
            timezone = propertyDefinition.timezone();
            if (pattern.isEmpty()) {
                pattern = null;
            }
            if (timezone.isEmpty()) {
                timezone = null;
            }
        }
        this.pattern = pattern;
        this.timezone = timezone;
    }

    void initDeserializer() {
        if (!flag) {
            flag = true;
            Class<?> impl;
            boolean unfixedType = false;
            Class<?>[] possibleTypes = null;
            String possibleExpression = null;
            if (propertyDefinition != null) {
                possibleTypes = propertyDefinition.possibleTypes();
                possibleExpression = propertyDefinition.possibleExpression();
                if ((impl = propertyDefinition.impl()) != Object.class) {
                    if (isAvailableImpl(impl)) {
                        implClass = impl;
                    }
                }
                unfixedType = propertyDefinition.unfixedType();
            }
            if (possibleTypes != null && possibleTypes.length > 0) {
                this.deserializer = store.getPossibleTypesTypeDeserializer(genericParameterizedType.getActualType(), possibleTypes, possibleExpression.isEmpty() ? null : Expression.parse(possibleExpression));
            } else if (implClass != null) {
                this.deserializer = store.getTypeDeserializer(implClass);
                this.genericParameterizedType = GenericParameterizedType.actualType(implClass);
            } else {
                if (setterInfo.isNonInstanceType()) {
                    this.deserializer = store.getCachedTypeDeserializer(genericParameterizedType.getActualType());
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

    boolean ensuredTypeDeserializable() {
        if (setterInfo.isNonInstanceType()) {
            JSONTypeDeserializer deserializer = store.getCachedTypeDeserializer(genericParameterizedType.getActualType());
            if (deserializer == null) {
                return false;
            }
            if (propertyDefinition == null || propertyDefinition.possibleTypes().length == 0) {
                this.deserializer = deserializer;
                flag = true;
            }
        }
        boolean unfixedType = propertyDefinition != null && propertyDefinition.unfixedType();
        if (unfixedType && genericParameterizedType.getActualClassCategory() == ReflectConsts.ClassCategory.ObjectCategory) {
            return false;
        }
        return true;
    }

    private JSONTypeDeserializer getDeserializer(GenericParameterizedType<?> genericParameterizedType) {
        // check custom Deserializer
        if (propertyDefinition != null) {
            try {
                Class<? extends JSONTypeFieldMapper> fieldMapperClass = propertyDefinition.mapper();
                if (fieldMapperClass != JSONTypeFieldMapper.class) {
                    JSONTypeDeserializer customDeserializer = customDeserializers.get(fieldMapperClass);
                    if (customDeserializer != null) {
                        this.customDeserialize = true;
                        return customDeserializer;
                    }
                    JSONTypeFieldMapper<?> fieldMapper = fieldMapperClass.newInstance();
                    if (fieldMapper.deserialize()) {
                        customDeserializer = store.buildDeserializer(fieldMapper);
                        if (fieldMapper.singleton()) {
                            customDeserializers.put(fieldMapperClass, customDeserializer);
                        }
                        this.customDeserialize = true;
                        return customDeserializer;
                    }
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        return store.getFieldDeserializer(genericParameterizedType, propertyDefinition);
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

    protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object defaultValue, int endToken, JSONParseContext jsonParseContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object defaultValue, int endToken, JSONParseContext jsonParseContext) throws Exception {
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
        // only supported ObjectCategory, CollectionCategory, MapCategory
        switch (classCategory) {
            case MapCategory:
            case CollectionCategory:
            case ObjectCategory:
            case NonInstance:
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

//    static JSONPojoFieldDeserializer createFieldDeserializer(String name, final SetterInfo setterInfo, JsonProperty jsonProperty) {
//        final long fieldOffset = setterInfo.getFieldOffset();
//        if (fieldOffset != -1) {
//            ReflectConsts.PrimitiveType primitiveType = setterInfo.getPrimitiveType();
//            if (primitiveType != null) {
//                switch (primitiveType) {
//                    case PrimitiveByte: {
//                        return new JSONPojoFieldDeserializer(name, setterInfo, jsonProperty) {
//                            @Override
//                            public void invoke(Object entity, Object value) {
//                                JSONUnsafe.UNSAFE.putByte(entity, fieldOffset, (Byte) value);
//                            }
//                        };
//                    }
//                    case PrimitiveShort: {
//                        return new JSONPojoFieldDeserializer(name, setterInfo, jsonProperty) {
//                            @Override
//                            public void invoke(Object entity, Object value) {
//                                JSONUnsafe.UNSAFE.putShort(entity, fieldOffset, (Short) value);
//                            }
//                        };
//                    }
//                    case PrimitiveInt: {
//                        return new JSONPojoFieldDeserializer(name, setterInfo, jsonProperty) {
//                            @Override
//                            public void invoke(Object entity, Object value) {
//                                JSONUnsafe.UNSAFE.putInt(entity, fieldOffset, (Integer) value);
//                            }
//                        };
//                    }
//                    case PrimitiveFloat: {
//                        return new JSONPojoFieldDeserializer(name, setterInfo, jsonProperty) {
//                            @Override
//                            public void invoke(Object entity, Object value) {
//                                JSONUnsafe.UNSAFE.putFloat(entity, fieldOffset, (Float) value);
//                            }
//                        };
//                    }
//                    case PrimitiveLong: {
//                        return new JSONPojoFieldDeserializer(name, setterInfo, jsonProperty) {
//                            @Override
//                            public void invoke(Object entity, Object value) {
//                                JSONUnsafe.UNSAFE.putLong(entity, fieldOffset, (Long) value);
//                            }
//                        };
//                    }
//                    case PrimitiveDouble: {
//                        return new JSONPojoFieldDeserializer(name, setterInfo, jsonProperty) {
//                            @Override
//                            public void invoke(Object entity, Object value) {
//                                JSONUnsafe.UNSAFE.putDouble(entity, fieldOffset, (Double) value);
//                            }
//                        };
//                    }
//                    case PrimitiveBoolean: {
//                        return new JSONPojoFieldDeserializer(name, setterInfo, jsonProperty) {
//                            @Override
//                            public void invoke(Object entity, Object value) {
//                                JSONUnsafe.UNSAFE.putBoolean(entity, fieldOffset, (Boolean) value);
//                            }
//                        };
//                    }
//                    default: {
//                        return new JSONPojoFieldDeserializer(name, setterInfo, jsonProperty) {
//                            @Override
//                            public void invoke(Object entity, Object value) {
//                                JSONUnsafe.UNSAFE.putChar(entity, fieldOffset, (Character) value);
//                            }
//                        };
//                    }
//                }
//            } else {
//                return new JSONPojoFieldDeserializer(name, setterInfo, jsonProperty) {
//                    @Override
//                    public void invoke(Object entity, Object value) {
//                        JSONUnsafe.UNSAFE.putObject(entity, fieldOffset, value);
//                    }
//                };
//            }
//        } else {
//            return new JSONPojoFieldDeserializer(name, setterInfo, jsonProperty) {
//                @Override
//                public void invoke(Object entity, Object value) {
//                    JSON_SECURE_TRUSTED_ACCESS.set(setterInfo, entity, value);
//                }
//            };
//        }
//    }
}
