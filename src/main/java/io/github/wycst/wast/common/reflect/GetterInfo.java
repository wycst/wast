package io.github.wycst.wast.common.reflect;

import io.github.wycst.wast.common.annotations.Property;
import io.github.wycst.wast.common.annotations.Serialize;
import io.github.wycst.wast.common.exceptions.InvokeReflectException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

public class GetterInfo {

    private Field field;
    // field 偏移
    private long fieldOffset = -1;
    // 是否基本类型
    private boolean fieldPrimitive;
    // 基本类型类别
    private ReflectConsts.PrimitiveType primitiveType;

    private String fieldName;

    private String mappingName;

    private String underlineName;

    /**
     * 是否序列化
     */
    private boolean serialize = true;

    /**
     * 自定义序列化注解
     */
    private Annotation serializeAnnotation;

    /**
     * 固化的双引号标记
     */
    private char[] fixedQuotBuffers;

    /**
     * 日期序列化格式
     */
    private String pattern;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 日期序列化为时间搓
     */
    private boolean writeDateAsTime;

    public String getFieldName() {
        if (fieldName == null) {
            if (field == null) {
                return mappingName;
            } else {
                fieldName = field.getName();
            }
        }
        return fieldName;
    }

    /**
     * 返回类型
     */
    private Class<?> returnType;

    // 注解集合
    private Map<Class<? extends Annotation>, Annotation> annotations;

    // 输出值分类
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
        return classCategory = ReflectConsts.getClassCategory(returnType);
    }

    /**
     * 反射属性
     */
    public final Object invoke(Object target) {
        if (fieldOffset > -1) {
            if (fieldPrimitive) {
                return UnsafeHelper.getPrimitiveValue(target, fieldOffset, primitiveType);
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

    public boolean existField() {
        return field != null;
    }

    public char[] getFixedQuotBuffers() {
        return fixedQuotBuffers;
    }

    void setMappingName(String mappingName) {
        this.mappingName = mappingName;
    }

    public String getPattern() {
        return pattern;
    }

    public String getTimezone() {
        return timezone;
    }

    public boolean isWriteDateAsTime() {
        return writeDateAsTime;
    }

    public boolean isSerialize() {
        return serialize;
    }

    public Map<Class<? extends Annotation>, Annotation> getAnnotations() {
        return annotations;
    }

    void setAnnotations(Map<Class<? extends Annotation>, Annotation> annotations) {
        this.annotations = annotations;
        parsePropertyAnnotation((Property) annotations.get(Property.class));
        // 检查是否配置了自定义序列化
        checkSerializer();
    }

    public Annotation getAnnotation(Class<? extends Annotation> annotationType) {
        return annotations.get(annotationType);
    }

    /**
     * 解析序列化属性
     */
    private void parsePropertyAnnotation(Property property) {

        if (property == null)
            return;

        String name = property.name();
        if (name != null && name.length() > 0) {
            mappingName = name.trim();
        }
        // 是否序列化
        this.serialize = property.serialize();

        // 日期格式序列化
        if (returnType != null && Date.class.isAssignableFrom(returnType)) {
            String timezone = property.timezone();
            if (timezone != null && timezone.trim().length() > 0) {
                this.timezone = timezone;
            }

            String pattern = property.pattern();
            if (pattern != null && pattern.trim().length() > 0) {
                this.pattern = pattern;
            }

            this.writeDateAsTime = property.asTimestamp();
        }
    }

    private void checkSerializer() {
        for (Class<? extends Annotation> annotationType : annotations.keySet()) {
            if (annotationType.isAnnotationPresent(Serialize.class)) {
                Annotation annotation = annotations.get(annotationType);
                this.serializeAnnotation = annotation;
                break;
            }
        }
    }

//    public Annotation getOrSetSerialize(Class<? extends Annotation> annotationType) {
//        if(annotationType.isInstance(this.serializeAnnotation)) {
//            return this.serializeAnnotation;
//        }
//        Annotation annotation = getAnnotation(annotationType);
//        if(annotation != null && annotationType.isAnnotationPresent(Serialize.class)) {
//            this.serializeAnnotation = annotation;
//        }
//        return annotation;
//    }

    public Annotation getSerializeAnnotation(Class<? extends Annotation> annotationType) {
        if (serializeAnnotation == null) return null;
        return annotationType.isInstance(this.serializeAnnotation) ? serializeAnnotation : null;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public void fixed() {
        int len = mappingName.length();
        // len = 属性len  + 前后逗号（2） +  前后 引号 （2） + 冒号（1）
        // 双引号
        fixedQuotBuffers = new char[len + 9];
        fixedQuotBuffers[0] = ',';
        fixedQuotBuffers[1] = '"';

        mappingName.getChars(0, len, fixedQuotBuffers, 2);

        fixedQuotBuffers[len + 2] = '"';
        fixedQuotBuffers[len + 3] = ':';

        // null
        fixedQuotBuffers[len + 4] = 'n';
        fixedQuotBuffers[len + 5] = 'u';
        fixedQuotBuffers[len + 6] = 'l';
        fixedQuotBuffers[len + 7] = 'l';
        fixedQuotBuffers[len + 8] = ',';
    }

    public void setUnderlineName(String underlineName) {
        this.underlineName = underlineName;
    }

    public String getUnderlineName() {
        return underlineName;
    }
}
