package io.github.wycst.wast.common.reflect;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 反射常量
 *
 * @Author: wangy
 * @Date: 2019/12/21 15:27
 * @Description:
 */
public class ReflectConsts {

    // 日期类型
    public static final int CLASS_TYPE_DATE = 1;

    // 基本类型，number类型，布尔值类型统一归纳为CLASS_TYPE_NUMBER
    public static final int CLASS_TYPE_NUMBER = 100;
    public static final int CLASS_TYPE_NUMBER_INTEGER = 101;
    public static final int CLASS_TYPE_NUMBER_FLOAT = 102;
    public static final int CLASS_TYPE_NUMBER_LONG = 103;
    public static final int CLASS_TYPE_NUMBER_DOUBLE = 104;
    public static final int CLASS_TYPE_NUMBER_BOOLEAN = 105;
    public static final int CLASS_TYPE_NUMBER_BIGDECIMAL = 106;
    public static final int CLASS_TYPE_NUMBER_BYTE = 107;
    public static final int CLASS_TYPE_NUMBER_CHARACTER = 108;
    public static final int CLASS_TYPE_NUMBER_ATOMIC_INTEGER = 109;
    public static final int CLASS_TYPE_NUMBER_ATOMIC_LONG = 110;
    public static final int CLASS_TYPE_NUMBER_ATOMIC_BOOLEAN = 111;
    public static final int CLASS_TYPE_NUMBER_BIG_INTEGER = 112;

    public static final int CLASS_TYPE_STRING = 200;
    public static final int CLASS_TYPE_STRING_BUFFER = 201;
    public static final int CLASS_TYPE_STRING_BUILDER = 202;
    public static final int CLASS_TYPE_CHAR_ARRAY = 203;
    public static final int CLASS_TYPE_BYTE_ARRAY = 204;

    public static final int CLASS_TYPE_JSON = 1000;
    public static final int CLASS_TYPE_ARRAY = 2000;

    public static int getParamClassType(Class<?> clazz) {
        if (clazz != null) {
            if (Date.class.isAssignableFrom(clazz)) {
                return CLASS_TYPE_DATE;
            } else if (clazz == String.class) {
                return CLASS_TYPE_STRING;
            } else if (isNumberType(clazz)) {
                return CLASS_TYPE_NUMBER;
            } else if (clazz == StringBuffer.class) {
                return CLASS_TYPE_STRING_BUFFER;
            } else if (clazz == StringBuilder.class) {
                return CLASS_TYPE_STRING_BUILDER;
            } else if (clazz == char[].class) {
                return CLASS_TYPE_CHAR_ARRAY;
            } else if (clazz == byte[].class) {
                return CLASS_TYPE_BYTE_ARRAY;
            } else if (Collection.class.isAssignableFrom(clazz) || clazz.isArray()) {
                return CLASS_TYPE_ARRAY;
            } else {

            }
        }
        return CLASS_TYPE_JSON;
    }

    public static boolean isNumberType(Class<?> clazz) {
        return clazz.isPrimitive() || Number.class.isAssignableFrom(clazz) || clazz == Boolean.class;
    }

    public static int getParamClassNumberType(Class<?> clazz) {
        if (isNumberType(clazz)) {
            if (clazz == int.class || clazz == Integer.class) {
                return CLASS_TYPE_NUMBER_INTEGER;
            } else if (clazz == float.class || clazz == Float.class) {
                return CLASS_TYPE_NUMBER_FLOAT;
            } else if (clazz == long.class || clazz == Long.class) {
                return CLASS_TYPE_NUMBER_LONG;
            } else if (clazz == double.class || clazz == Double.class) {
                return CLASS_TYPE_NUMBER_DOUBLE;
            } else if (clazz == boolean.class || clazz == Boolean.class) {
                return CLASS_TYPE_NUMBER_BOOLEAN;
            } else if (clazz == BigDecimal.class) {
                return CLASS_TYPE_NUMBER_BIGDECIMAL;
            } else if (clazz == byte.class || clazz == Byte.class) {
                return CLASS_TYPE_NUMBER_BYTE;
            } else if (clazz == char.class || clazz == Character.class) {
                return CLASS_TYPE_NUMBER_CHARACTER;
            } else if (clazz == AtomicInteger.class) {
                return CLASS_TYPE_NUMBER_ATOMIC_INTEGER;
            } else if (clazz == AtomicLong.class) {
                return CLASS_TYPE_NUMBER_ATOMIC_LONG;
            } else if (clazz == AtomicBoolean.class) {
                return CLASS_TYPE_NUMBER_ATOMIC_BOOLEAN;
            } else if (clazz == BigInteger.class) {
                return CLASS_TYPE_NUMBER_BIG_INTEGER;
            }
        }
        return 0;
    }

    /**
     * 缓存类型class分类
     */
    private final static Map<Class, ClassCategory> classClassCategoryMap = new HashMap<Class, ClassCategory>();

    static {

        // 字节数组（特殊的数组）
        putAllType(ClassCategory.Binary, byte[].class, Byte[].class);

        /** 预置常用集合类型 */
        putAllType(ClassCategory.Collection, ArrayList.class, HashSet.class);

        /** 预置常用Map类型 */
        putAllType(ClassCategory.Map, HashMap.class, LinkedHashMap.class);

        /** 预置常用CharSequence类型 */
        putAllType(ClassCategory.CharSequence, String.class, char[].class, StringBuilder.class, StringBuffer.class, char.class, Character.class);

        /** 预置常用Number类型 */
        putAllType(ClassCategory.Number, BigDecimal.class, BigInteger.class, int.class, Integer.class, byte.class, Byte.class, short.class, Short.class, float.class, Float.class, long.class, Long.class, double.class, Double.class);

        /** boolean */
        putAllType(ClassCategory.Bool, boolean.class, Boolean.class);

        /** Date */
        putAllType(ClassCategory.Date, Date.class, Timestamp.class, java.sql.Date.class);
    }

    private static void putAllType(ClassCategory classCategory, Class<?>... classList) {
        for (Class cls : classList) {
            classClassCategoryMap.put(cls, classCategory);
        }
    }

    private synchronized static void putType(Class<?> cls, ClassCategory classCategory) {
        // 防止意外动态类
        if (classClassCategoryMap.size() >= 1 << 12) return;
        classClassCategoryMap.put(cls, classCategory);
    }

    public static ClassCategory getClassCategory(Class cls) {
        ClassCategory classCategory = classClassCategoryMap.get(cls);
        if (classCategory != null) {
            return classCategory;
        }
        if (cls.isArray()) {
            putType(cls, classCategory = ClassCategory.Array);
            return classCategory;
        }
        if (cls.isEnum()) {
            putType(cls, classCategory = ClassCategory.Enum);
            return classCategory;
        }
        if (Collection.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.Collection);
            return classCategory;
        }
        if (Map.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.Map);
            return classCategory;
        }
        if (CharSequence.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.CharSequence);
            return classCategory;
        }
        if (Number.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.Number);
            return classCategory;
        }
        if (Date.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.Date);
            return classCategory;
        }
        if (Class.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.Class);
            return classCategory;
        }
        if (Annotation.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.Annotation);
            return classCategory;
        }
        if (Annotation.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.Annotation);
            return classCategory;
        }
        putType(cls, classCategory = ClassCategory.Object);
        return classCategory;
    }

    /**
     * class分类
     */
    public enum ClassCategory {
        /**
         * 字符串（String, StringBuffer, StringBuild, Character）
         */
        CharSequence,
        /**
         * 数字类型
         */
        Number,
        /**
         * boolean类型（boolean, Boolean）
         */
        Bool,
        /**
         * 日期类型
         */
        Date,
        /**
         * 类类型
         */
        Class,
        /**
         * 枚举实例
         */
        Enum,
        /**
         * 注释类型
         */
        Annotation,
        /**
         * 二进制数组
         */
        Binary,
        /**
         * 数组类型
         */
        Array,
        /**
         * 集合类型(Set,List)
         */
        Collection,
        /**
         * Map类型(Map,HashMap,LinkHashMap...)
         */
        Map,
        /**
         * 实体对象类型
         */
        Object
    }

    /***
     * 基本类型枚举
     */
    enum PrimitiveType {
        Byte,
        Short,
        Int,
        Float,
        Long,
        Double,
        Boolean,
        Character;

        public static PrimitiveType typeOf(Class<?> fieldType) {
            if (fieldType == int.class) {
                return Int;
            } else if (fieldType == long.class) {
                return Long;
            } else if (fieldType == float.class) {
                return Float;
            } else if (fieldType == double.class) {
                return Double;
            } else if (fieldType == boolean.class) {
                return Boolean;
            } else if (fieldType == short.class) {
                return Short;
            } else if (fieldType == char.class) {
                return Character;
            } else if (fieldType == byte.class) {
                return Byte;
            }
            return null;
        }
    }

}
