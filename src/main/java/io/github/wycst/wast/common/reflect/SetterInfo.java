package io.github.wycst.wast.common.reflect;

import io.github.wycst.wast.common.exceptions.InvokeReflectException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class SetterInfo {

    // field of class
    private Field field;
    // field memory offset
    private long fieldOffset = -1;
    // is primitive
    private boolean fieldPrimitive;
    // primitive type
    private ReflectConsts.PrimitiveType primitiveType;

    private String name;
    private Class<?> parameterType;

    private Class<?> actualTypeArgument;

    // 索引位置, 当宿主类型为record时或者通过构造方法设置对象的属性值时使用
    private int index;

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

    // 注解集合
    private Map<Class<? extends Annotation>, Annotation> annotations;

    // 是否存在默认值
    private Boolean existDefault;

    // invoke时禁用field
    private boolean fieldDisabled;

    /**
     * 反射动作
     */
    public final void invoke(Object target, Object value) {
        if (fieldOffset > -1) {
            if (fieldPrimitive) {
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

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
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

    public Map<Class<? extends Annotation>, Annotation> getAnnotations() {
        return annotations;
    }

    void setAnnotations(Map<Class<? extends Annotation>, Annotation> annotations) {
        this.annotations = annotations;
    }

    void setField(Field field) {
        this.field = field;
        try {
            this.fieldOffset = UnsafeHelper.objectFieldOffset(field);
            this.fieldPrimitive = getFieldType().isPrimitive();
            if (this.fieldPrimitive) {
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
        if (field == null) return null;
        try {
            if (existDefault == Boolean.FALSE) {
                return null;
            }
            Object fieldValue = field.get(instance);
            this.existDefault = fieldValue != null;
            if (fieldValue != null) {
                return fieldValue;
            }
        } catch (Exception e) {
            existDefault = Boolean.FALSE;
        }
        return null;
    }

    public Annotation getAnnotation(Class<? extends Annotation> annotationType) {
        return annotations.get(annotationType);
    }

    public int getParamClassType() {
        return genericParameterizedType.getParamClassType();
    }

    public int getParamClassNumberType() {
        return genericParameterizedType.getParamClassNumberType();
    }

    public boolean isMethod() {
        return false;
    }

    public boolean isPrimate() {
        return Modifier.isPrivate(field.getModifiers());
    }

    public int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }

    void setFieldDisabled(boolean disabled) {
        this.fieldDisabled = disabled;
    }

    public boolean isFieldDisabled() {
        return fieldDisabled;
    }
}

