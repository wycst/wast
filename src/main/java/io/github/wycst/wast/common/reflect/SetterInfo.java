package io.github.wycst.wast.common.reflect;

import io.github.wycst.wast.common.annotations.Deserialize;
import io.github.wycst.wast.common.annotations.Property;
import io.github.wycst.wast.common.exceptions.InvokeReflectException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

public class SetterInfo {

    private Field field;
    // field 偏移
    private long fieldOffset = -1;
    // 是否基本类型
    private boolean fieldPrimitive;
    // 基本类型类别
    private ReflectConsts.PrimitiveType primitiveType;

    private String mappingName;

    private Class<?> parameterType;

    private Class<?> actualTypeArgument;

    /**
     * 泛型信息
     * Generic information
     */
    private GenericParameterizedType genericParameterizedType;

    /**
     * 非实例化类型
     * （排除集合或者map后的接口或者抽象类，无法通过new创建实例对象）
     */
    private boolean nonInstanceType;

    /**
     * 是否反序列化
     */
    private boolean deserialize = true;

    /**
     * 反序列化注解
     */
    private Annotation deserializeAnnotation;

    /**
     * 日期序列化格式
     */
    private String pattern;

    /**
     * 时区
     */
    private String timezone;

    // 注解集合
    private Map<Class<? extends Annotation>, Annotation> annotations;

    // 是否存在默认值
    private Boolean hasDefaultValue;

    /**
     * 代理字段
     */
    private boolean fieldAgent;

    // 分类
    private ReflectConsts.ClassCategory classCategory;

    /**
     * 获取Getter类型分类
     */
    public ReflectConsts.ClassCategory getClassCategory() {
        if (classCategory != null) {
            return classCategory;
        }
        if (fieldOffset > -1) {
            // field
            return classCategory = ReflectConsts.getClassCategory(field.getType());
        }
        // getter method
        return classCategory = ReflectConsts.getClassCategory(parameterType);
    }

    /**
     * 反射设置
     */
    public final void invoke(Object target, Object value) {
        if (fieldOffset > -1) {
            if(fieldPrimitive) {
                UnsafeHelper.putPrimitiveValue(target, fieldOffset, value, primitiveType);
            } else {
                UnsafeHelper.putObjectValue(target, fieldOffset, value);
            }
            return;
        }
        invokeObjectValue(target, value);
    }

    protected void invokeObjectValue(Object target, Object value) {
        try {
            field.set(target, value);
        } catch (Exception e) {
            throw new InvokeReflectException(e);
        }
    }

    public String getMappingName() {
        return mappingName;
    }

    void setMappingName(String mappingName) {
        this.mappingName = mappingName;
    }

    public Class<?> getParameterType() {
        return parameterType;
    }

    void setParameterType(Class<?> parameterType) {
        this.parameterType = parameterType;
    }

    public Class<?> getActualTypeArgument() {
        return actualTypeArgument;
    }

    void setActualTypeArgument(Class<?> actualTypeArgument) {
        this.actualTypeArgument = actualTypeArgument;
    }

    public boolean isNonInstanceType() {
        return nonInstanceType;
    }

    void setNonInstanceType(boolean nonInstanceType) {
        this.nonInstanceType = nonInstanceType;
    }

    public boolean isDeserialize() {
        return deserialize;
    }

    public String getPattern() {
        return pattern;
    }

    public String getTimezone() {
        return timezone;
    }

    public Map<Class<? extends Annotation>, Annotation> getAnnotations() {
        return annotations;
    }

    void setAnnotations(Map<Class<? extends Annotation>, Annotation> annotations) {
        this.annotations = annotations;
        // 反序列Property
        parsePropertyAnnotation((Property) annotations.get(Property.class));
        // 检查是否配置了自定义反序列化
        checkDeserializer();
    }

    void setField(Field field) {
        this.field = field;
        try {
            this.fieldOffset = UnsafeHelper.objectFieldOffset(field);
            this.fieldPrimitive = getFieldType().isPrimitive();
            if(this.fieldPrimitive) {
                this.primitiveType = ReflectConsts.PrimitiveType.typeOf(getFieldType());
            }
        } catch (Throwable throwable) {
            this.fieldOffset = -1;
        }
    }

    public Class<?> getFieldType() {
        return field == null ? null : field.getType();
    }

    public GenericParameterizedType getGenericParameterizedType() {
        return genericParameterizedType;
    }

    void setGenericParameterizedType(GenericParameterizedType genericParameterizedType) {
        this.genericParameterizedType = genericParameterizedType;
    }

    /**
     * 获取默认值
     */
    public Object getDefaultFieldValue(Object instance) {
        try {
            if (hasDefaultValue == Boolean.FALSE) {
                return null;
            }
            Object fieldValue = field.get(instance);
            this.hasDefaultValue = fieldValue != null;
            if (fieldValue != null) {
                return fieldValue;
            }
        } catch (Exception e) {
            hasDefaultValue = Boolean.FALSE;
        }
        return null;
    }

    private void parsePropertyAnnotation(Property property) {

        if (property == null)
            return;

        String name = property.name();
        if (name != null && name.length() > 0) {
            mappingName = name.trim();
        }
        // 是否序列化
        this.deserialize = property.deserialize();

        // 日期格式序列化
        if (parameterType != null && Date.class.isAssignableFrom(parameterType)) {
            String pattern = property.pattern();
            if (pattern != null && pattern.trim().length() > 0) {
                this.pattern = pattern;
            }

            String timezone = property.timezone();
            if (timezone != null && timezone.trim().length() > 0) {
                this.timezone = timezone;
            }
        }
    }

    private void checkDeserializer() {
        for (Class<? extends Annotation> annotationType : annotations.keySet()) {
            if (annotationType.isAnnotationPresent(Deserialize.class)) {
                Annotation annotation = annotations.get(annotationType);
                this.deserializeAnnotation = annotation;
                break;
            }
        }
    }

    public Annotation getDeserializeAnnotation(Class<? extends Annotation> annotationType) {
        if (deserializeAnnotation == null) return null;
        return annotationType.isInstance(this.deserializeAnnotation) ? deserializeAnnotation : null;
    }

    public Annotation getAnnotation(Class<? extends Annotation> annotationType) {
        return annotations.get(annotationType);
    }

    private int paramClassType;
    private int paramClassNumberType;

    public int getParamClassType() {
        return paramClassType;
    }

    public int getParamClassNumberType() {
        return paramClassNumberType;
    }

    void initParamClassType() {
        this.paramClassType = ReflectConsts.getParamClassType(parameterType);
        this.paramClassNumberType = ReflectConsts.getParamClassNumberType(parameterType);
    }
}

