package io.github.wycst.wast.common.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class SetterInfo {

    // field of class
    private Field field;
    // field memory offset
    long fieldOffset = -1;
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

    public static SetterInfo fromField(Field field) {
        boolean primitive = field.getType().isPrimitive();
        SetterInfo setterInfo = primitive ? new SetterInfo.PrimitiveSetterInfo() : new SetterInfo();
        setterInfo.setField(field);
        return setterInfo;
    }

    /**
     * 反射动作
     */
    public void invoke(Object target, Object value) {
        UnsafeHelper.putObjectValue(target, fieldOffset, value);
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

    Map<Class<? extends Annotation>, Annotation> getAnnotations() {
        return annotations;
    }

    void setAnnotations(Map<Class<? extends Annotation>, Annotation> annotations) {
        this.annotations = annotations;
    }

    void setField(Field field) {
        this.field = field;
        try {
            this.fieldOffset = UnsafeHelper.objectFieldOffset(field);
        } catch (Throwable throwable) {
            this.fieldOffset = -1;
        }
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
            if (existDefault == Boolean.FALSE) {
                return null;
            }
            Object fieldValue = getFieldValue(instance);
            this.existDefault = fieldValue != null;
            if (fieldValue != null) {
                return fieldValue;
            }
        } catch (Exception e) {
            existDefault = Boolean.FALSE;
        }
        return null;
    }

    Object getFieldValue(Object instance) {
        return UnsafeHelper.getObjectValue(instance, fieldOffset);
    }

    public Annotation getAnnotation(Class<? extends Annotation> annotationType) {
        return annotations.get(annotationType);
    }

    public boolean isMethod() {
        return false;
    }

    public boolean isPrivate() {
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

    static final class PrimitiveSetterInfo extends SetterInfo {
        private ReflectConsts.PrimitiveType primitiveType;

        @Override
        public void invoke(Object target, Object value) {
            primitiveType.put(target, fieldOffset, value);
        }

        Object getFieldValue(Object instance) {
            return primitiveType.get(instance, fieldOffset);
        }

        void setField(Field field) {
            super.setField(field);
            this.primitiveType = ReflectConsts.PrimitiveType.typeOf(field.getType());
        }
    }
}

