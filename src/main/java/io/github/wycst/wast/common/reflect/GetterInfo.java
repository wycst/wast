package io.github.wycst.wast.common.reflect;

import io.github.wycst.wast.common.exceptions.InvokeReflectException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class GetterInfo {

    private Field field;
    private long fieldOffset = -1;
    private boolean fieldPrimitive;
    private ReflectConsts.PrimitiveType primitiveType;
    private GenericParameterizedType<?> genericParameterizedType;
    private String name;
    private String underlineName;
    private Map<Class<? extends Annotation>, Annotation> annotations;
    private ReflectConsts.ClassCategory classCategory;
    private boolean record;

    public ReflectConsts.ClassCategory getClassCategory() {
        if (classCategory != null) {
            return classCategory;
        }
        return classCategory = ReflectConsts.getClassCategory(getReturnType());
    }

    public final boolean isInstance(Object target) {
        return field.getDeclaringClass().isInstance(target);
    }

    public final Object invoke(Object target) {
        if (fieldOffset > -1) {
            if (isInstance(target)) {
                if (fieldPrimitive) {
                    return primitiveType.get(target, fieldOffset);
                } else {
                    return UnsafeHelper.getObjectValue(target, fieldOffset);
                }
            } else {
                throw new SecurityException("invoke error: parameter mismatch");
            }
        }
        return invokeObjectValue(target);
    }

    protected Object invokeObjectValue(Object target) {
        try {
            return field.get(target);
        } catch (Exception e) {
            throw new InvokeReflectException(e);
        }
    }

    Object invokeInternal(Object target) {
        if (fieldOffset > -1) {
            if (fieldPrimitive) {
                return primitiveType.getValue(target, fieldOffset);
            } else {
                return UnsafeHelper.UNSAFE.getObject(target, fieldOffset);
            }
        }
        return invokeObjectValue(target);
    }

    void setField(Field field) {
        this.field = field;
        try {
            this.fieldPrimitive = field.getType().isPrimitive();
            this.fieldOffset = UnsafeHelper.objectFieldOffset(field);
            if (this.fieldPrimitive) {
                this.primitiveType = ReflectConsts.PrimitiveType.typeOf(field.getType());
            }
        } catch (Throwable throwable) {
            this.fieldOffset = -1;
        }
    }

    Field getField() {
        return this.field;
    }

    public boolean existField() {
        return field != null;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<Class<? extends Annotation>, Annotation> getAnnotations() {
        return annotations;
    }

    void setAnnotations(Map<Class<? extends Annotation>, Annotation> annotations) {
        this.annotations = annotations;
    }

    public Annotation getAnnotation(Class<? extends Annotation> annotationType) {
        return annotations.get(annotationType);
    }

    public Class<?> getReturnType() {
        return field.getType();
    }

    void setUnderlineName(String underlineName) {
        this.underlineName = underlineName;
    }

    public String getUnderlineName() {
        return underlineName;
    }

    // 是否通过getter方法
    public boolean isMethod() {
        return false;
    }

    // 是否可访问
    public boolean isAccess() {
        return !Modifier.isPrivate(field.getModifiers());
    }

    // 是否public
    public boolean isPublic() {
        return Modifier.isPublic(field.getModifiers());
    }

    public boolean isPrimitive() {
        return fieldPrimitive;
    }

    public GenericParameterizedType<?> getGenericParameterizedType() {
        return genericParameterizedType;
    }

    void setGenericParameterizedType(GenericParameterizedType<?> genericParameterizedType) {
        this.genericParameterizedType = genericParameterizedType;
    }

    public String getMethodName() {
        return null;
    }

    public final boolean isSupportedUnsafe() {
        return fieldOffset > -1;
    }

    public final ReflectConsts.PrimitiveType getPrimitiveType() {
        return primitiveType;
    }

    public final long getFieldOffset() {
        return fieldOffset;
    }

    public String generateCode() {
        if (record) {
            // use by Record access
            return field.getName() + "()";
        }
        return field.getName();
    }

    void setRecord(boolean record) {
        this.record = record;
    }


}
