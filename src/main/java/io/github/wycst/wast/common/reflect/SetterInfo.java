package io.github.wycst.wast.common.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class SetterInfo {

    // field of class
    Field field;
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
    private boolean fieldDisabled;

    public static SetterInfo fromField(Field field) {
        boolean primitive = field.getType().isPrimitive();
        SetterInfo setterInfo = primitive ? new PrimitiveImpl() : new SetterInfo();
        setterInfo.setField(field);
        return setterInfo;
    }

    public final boolean isInstance(Object target) {
        return field.getDeclaringClass().isInstance(target);
    }

    public void invoke(Object target, Object value) {
        target.getClass();
        if ((parameterType.isInstance(value) || value == null) && isInstance(target)) {
            invokeInternal(target, value);
        } else {
            throw new SecurityException("invoke error: parameter mismatch");
        }
    }

    void invokeInternal(Object target, Object value) {
        UnsafeHelper.UNSAFE.putObject(target, fieldOffset, value);
    }

    public final String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public final Class<?> getParameterType() {
        return parameterType;
    }

    void setParameterType(Class<?> parameterType) {
        this.parameterType = parameterType;
    }

    public final Class<?> getActualTypeArgument() {
        return actualTypeArgument;
    }

    void setActualTypeArgument(Class<?> actualTypeArgument) {
        this.actualTypeArgument = actualTypeArgument;
    }

    public final boolean isNonInstanceType() {
        return nonInstanceType;
    }

    void setNonInstanceType(boolean nonInstanceType) {
        this.nonInstanceType = nonInstanceType;
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

    public final GenericParameterizedType getGenericParameterizedType() {
        return genericParameterizedType;
    }

    void setGenericParameterizedType(GenericParameterizedType genericParameterizedType) {
        this.genericParameterizedType = genericParameterizedType;
    }

    Object getDefaultFieldValue(Object instance) {
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

    Object getFieldValue(Object instance) throws IllegalAccessException {
        return UnsafeHelper.getObjectValue(instance, fieldOffset);
    }

    public final Annotation getAnnotation(Class<? extends Annotation> annotationType) {
        return annotations.get(annotationType);
    }

    public boolean isMethod() {
        return false;
    }

    public boolean isPrivate() {
        return Modifier.isPrivate(field.getModifiers());
    }

    public final int getIndex() {
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

    static final class FieldImpl extends SetterInfo {
        void setField(Field field) {
            this.field = field;
        }

        public void invoke(Object target, Object value) {
            invokeInternal(target, value);
        }

        Object getFieldValue(Object instance) throws IllegalAccessException {
            return field.get(instance);
        }

        void invokeInternal(Object target, Object value) {
            try {
                field.set(target, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static final class PrimitiveImpl extends SetterInfo {
        private ReflectConsts.PrimitiveType primitiveType;

        public void invoke(Object target, Object value) {
            if (isInstance(target)) {
                primitiveType.put(target, fieldOffset, value);
            } else {
                throw new SecurityException("invoke error: parameter mismatch");
            }
        }

        @Override
        void invokeInternal(Object target, Object value) {
            primitiveType.putValue(target, fieldOffset, value);
        }

        Object getFieldValue(Object instance) {
            return primitiveType.get(instance, fieldOffset);
        }

        void setField(Field field) {
            super.setField(field);
            this.primitiveType = ReflectConsts.PrimitiveType.typeOf(field.getType());
        }

        public ReflectConsts.PrimitiveType getPrimitiveType() {
            return primitiveType;
        }
    }

    public final long getFieldOffset() {
        return fieldOffset;
    }

    public ReflectConsts.PrimitiveType getPrimitiveType() {
        return null;
    }
}

