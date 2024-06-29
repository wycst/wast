package io.github.wycst.wast.common.reflect;

import io.github.wycst.wast.common.exceptions.InvokeReflectException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class GetterInfo {

    private Field field;
    // field 偏移
    private long fieldOffset = -1;
    // 是否基本类型
    private boolean fieldPrimitive;
    // 基本类型类别
    private ReflectConsts.PrimitiveType primitiveType;
    private GenericParameterizedType genericParameterizedType;
    // 名称
    private String name;

    private String underlineName;

    // 注解集合
    private Map<Class<? extends Annotation>, Annotation> annotations;

    // 输出值分类
    private ReflectConsts.ClassCategory classCategory;
    private boolean record;

    public static GetterInfo fromField(Field field) {
        GetterInfo getterInfo = new GetterInfo();
        getterInfo.setField(field);
        return getterInfo;
    }

    /**
     * 获取Getter类型分类
     */
    public ReflectConsts.ClassCategory getClassCategory() {
        if (classCategory != null) {
            return classCategory;
        }
        if (fieldOffset > -1) {
            // field
            return classCategory = ReflectConsts.getClassCategory(getReturnType());
        }
        // getter method
        return classCategory = ReflectConsts.getClassCategory(getReturnType());
    }

    /**
     * 反射属性
     */
    public final Object invoke(Object target) {
        if (fieldOffset > -1) {
            if (fieldPrimitive) {
                return primitiveType.get(target, fieldOffset);
            } else {
                return UnsafeHelper.getObjectValue(target, fieldOffset);
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

    void setField(Field field) {
        this.field = field;
        try {
            this.fieldOffset = UnsafeHelper.objectFieldOffset(field);
            this.fieldPrimitive = field.getType().isPrimitive();
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

    public void setUnderlineName(String underlineName) {
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

    public boolean isPrimitive() {
        return fieldPrimitive;
    }

    public GenericParameterizedType getGenericParameterizedType() {
        return genericParameterizedType;
    }

    void setGenericParameterizedType(GenericParameterizedType genericParameterizedType) {
        this.genericParameterizedType = genericParameterizedType;
    }

    public String getMethodName() {
        return null;
    }

    public String generateCode() {
        if (record) {
            // use by Record access
            return field.getName() + "()";
        }
        return field.getName();
    }

    public void setRecord(boolean record) {
        this.record = record;
    }
}
