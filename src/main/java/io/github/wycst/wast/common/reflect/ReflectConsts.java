package io.github.wycst.wast.common.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    public static final int CLASS_TYPE_NUMBER_BYTE = 101;
    public static final int CLASS_TYPE_NUMBER_SHORT = 102;
    public static final int CLASS_TYPE_NUMBER_INTEGER = 103;
    public static final int CLASS_TYPE_NUMBER_LONG = 104;
    public static final int CLASS_TYPE_NUMBER_FLOAT = 105;
    public static final int CLASS_TYPE_NUMBER_DOUBLE = 106;
    public static final int CLASS_TYPE_NUMBER_ATOMIC_INTEGER = 107;
    public static final int CLASS_TYPE_NUMBER_ATOMIC_LONG = 108;
    public static final int CLASS_TYPE_NUMBER_BIGDECIMAL = 109;
    public static final int CLASS_TYPE_NUMBER_BIG_INTEGER = 110;
    public static final int CLASS_TYPE_NUMBER_CHARACTER = 111;
    public static final int CLASS_TYPE_NUMBER_BOOLEAN = 112;
    public static final int CLASS_TYPE_NUMBER_ATOMIC_BOOLEAN = 113;

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
            } else if (clazz == short.class || clazz == Short.class) {
                return CLASS_TYPE_NUMBER_SHORT;
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
     * Cache type class classification
     */
    private final static Map<Class, ClassCategory> classClassCategoryMap = new ConcurrentHashMap<Class, ClassCategory>();

    static {

        // 字节数组(将Byte除去)
        putAllType(ClassCategory.Binary, byte[].class/*, Byte[].class*/);

        /** 预置常用集合类型 */
        putAllType(ClassCategory.CollectionCategory, ArrayList.class, HashSet.class);

        /** 预置常用Map类型 */
        putAllType(ClassCategory.MapCategory, HashMap.class, LinkedHashMap.class);

        /** 预置常用CharSequence类型 */
        putAllType(ClassCategory.CharSequence, String.class, char[].class, StringBuilder.class, StringBuffer.class, char.class, Character.class);

        /** 预置常用Number类型 */
        putAllType(ClassCategory.NumberCategory, BigDecimal.class, BigInteger.class, int.class, Integer.class, byte.class, Byte.class, short.class, Short.class, float.class, Float.class, long.class, Long.class, double.class, Double.class);

        /** boolean */
        putAllType(ClassCategory.BoolCategory, boolean.class, Boolean.class);

        /** Date */
        putAllType(ClassCategory.DateCategory, Date.class, Timestamp.class, java.sql.Date.class);

        /** 任意类型 */
        putAllType(ClassCategory.ANY, Object.class);
    }

    // Batch put
    private static void putAllType(ClassCategory classCategory, Class<?>... classList) {
        for (Class cls : classList) {
            classClassCategoryMap.put(cls, classCategory);
        }
    }

    private synchronized static void putType(Class<?> cls, ClassCategory classCategory) {
        // Prevent accidental explosion
        if (classClassCategoryMap.size() >= 1 << 12) return;
        classClassCategoryMap.put(cls, classCategory);
    }

    public static ClassCategory getClassCategory(Class cls) {
        if (cls == null) return ClassCategory.ANY;
        ClassCategory classCategory = classClassCategoryMap.get(cls);
        if (classCategory != null) {
            return classCategory;
        }
        if (CharSequence.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.CharSequence);
            return classCategory;
        }
        if (Number.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.NumberCategory);
            return classCategory;
        }
        if (Date.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.DateCategory);
            return classCategory;
        }
        if (cls.isEnum()) {
            putType(cls, classCategory = ClassCategory.EnumCategory);
            return classCategory;
        }
        if (Collection.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.CollectionCategory);
            return classCategory;
        }
        if (Map.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.MapCategory);
            return classCategory;
        }
        if (cls.isArray()) {
            putType(cls, classCategory = ClassCategory.ArrayCategory);
            return classCategory;
        }
        if (Class.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.ClassCategory);
            return classCategory;
        }
        if (Annotation.class.isAssignableFrom(cls)) {
            putType(cls, classCategory = ClassCategory.AnnotationCategory);
            return classCategory;
        }
        if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers())) {
            putType(cls, classCategory = ClassCategory.NonInstance);
            return classCategory;
        }
        putType(cls, classCategory = ClassCategory.ObjectCategory);
        return classCategory;
    }

    /**
     * class类别
     */
    public enum ClassCategory {
        /**
         * 字符串（String, StringBuffer, StringBuild, Character）
         */
        CharSequence,
        /**
         * 数字类型
         */
        NumberCategory,
        /**
         * boolean类型（boolean, Boolean）
         */
        BoolCategory,
        /**
         * 日期类型
         */
        DateCategory,
        /**
         * 类类型
         */
        ClassCategory,
        /**
         * 枚举类型
         */
        EnumCategory,
        /**
         * 注释类型
         */
        AnnotationCategory,
        /**
         * 二进制数组
         */
        Binary,
        /**
         * 数组类型
         */
        ArrayCategory,
        /**
         * 集合类型(Set,List)
         */
        CollectionCategory,
        /**
         * Map类型(Map,HashMap,LinkHashMap...)
         */
        MapCategory,
        /**
         * 实体对象类型
         */
        ObjectCategory,
        /**
         * 任意类型
         */
        ANY,
        /**
         * 不可实例化类型（接口和抽象类，不包括基础类型）
         */
        NonInstance
    }

    /***
     * 基本类型枚举
     */
    enum PrimitiveType {
        PrimitiveByte(byte[].class, arrayBaseOffset(byte[].class), arrayIndexScale(byte[].class)) {
            @Override
            void putValue(Object target, long fieldOffset, Object value) {
                UnsafeHelper.getUnsafe().putByte(target, fieldOffset, (Byte) value);
            }
            @Override
            Object getValue(Object target, long fieldOffset) {
                return UnsafeHelper.getUnsafe().getByte(target, fieldOffset);
            }
        },
        PrimitiveShort(short[].class, arrayBaseOffset(short[].class), arrayIndexScale(short[].class)) {
            @Override
            void putValue(Object target, long fieldOffset, Object value) {
                UnsafeHelper.getUnsafe().putShort(target, fieldOffset, (Short) value);
            }
            @Override
            Object getValue(Object target, long fieldOffset) {
                return UnsafeHelper.getUnsafe().getShort(target, fieldOffset);
            }
        },
        PrimitiveInt(int[].class, arrayBaseOffset(int[].class), arrayIndexScale(int[].class)) {
            @Override
            void putValue(Object target, long fieldOffset, Object value) {
                UnsafeHelper.getUnsafe().putInt(target, fieldOffset, (Integer) value);
            }
            @Override
            Object getValue(Object target, long fieldOffset) {
                return UnsafeHelper.getUnsafe().getInt(target, fieldOffset);
            }
        },
        PrimitiveFloat(float[].class, arrayBaseOffset(float[].class), arrayIndexScale(float[].class)) {
            @Override
            void putValue(Object target, long fieldOffset, Object value) {
                UnsafeHelper.getUnsafe().putFloat(target, fieldOffset, (Float) value);
            }
            @Override
            Object getValue(Object target, long fieldOffset) {
                return UnsafeHelper.getUnsafe().getFloat(target, fieldOffset);
            }
        },
        PrimitiveLong(long[].class, arrayBaseOffset(long[].class), arrayIndexScale(long[].class)) {
            @Override
            void putValue(Object target, long fieldOffset, Object value) {
                UnsafeHelper.getUnsafe().putLong(target, fieldOffset, (Long) value);
            }
            @Override
            Object getValue(Object target, long fieldOffset) {
                return UnsafeHelper.getUnsafe().getLong(target, fieldOffset);
            }
        },
        PrimitiveDouble(double[].class, arrayBaseOffset(double[].class), arrayIndexScale(double[].class)) {
            @Override
            void putValue(Object target, long fieldOffset, Object value) {
                UnsafeHelper.getUnsafe().putDouble(target, fieldOffset, (Double) value);
            }
            @Override
            Object getValue(Object target, long fieldOffset) {
                return UnsafeHelper.getUnsafe().getDouble(target, fieldOffset);
            }
        },
        PrimitiveBoolean(boolean[].class, arrayBaseOffset(boolean[].class), arrayIndexScale(boolean[].class)) {
            @Override
            void putValue(Object target, long fieldOffset, Object value) {
                UnsafeHelper.getUnsafe().putBoolean(target, fieldOffset, (Boolean) value);
            }
            @Override
            Object getValue(Object target, long fieldOffset) {
                return UnsafeHelper.getUnsafe().getBoolean(target, fieldOffset);
            }
        },
        PrimitiveCharacter(char[].class, arrayBaseOffset(char[].class), arrayIndexScale(char[].class)) {
            @Override
            void putValue(Object target, long fieldOffset, Object value) {
                UnsafeHelper.getUnsafe().putChar(target, fieldOffset, (Character) value);
            }
            @Override
            Object getValue(Object target, long fieldOffset) {
                return UnsafeHelper.getUnsafe().getChar(target, fieldOffset);
            }
        };

        final Class genericArrayType;
        final int arrayBaseOffset;
        final int arrayIndexScale;

        PrimitiveType(Class genericArrayType, int arrayBaseOffset, int arrayIndexScale) {
            this.genericArrayType = genericArrayType;
            this.arrayBaseOffset = arrayBaseOffset;
            this.arrayIndexScale = arrayIndexScale;
        }

        void put(Object target, long fieldOffset, Object value) {
            target.getClass();
            if (value == null) return;
            putValue(target, fieldOffset, value);
        }

        Object get(Object target, long fieldOffset) {
            target.getClass();
            return getValue(target, fieldOffset);
        }

        abstract Object getValue(Object target, long fieldOffset);

        abstract void putValue(Object target, long fieldOffset, Object value);

        final static int DoubleNameHash = -1325958191;
        final static int IntNameHash = 104431;
        final static int ByteNameHash = 3039496;
        final static int CharNameHash = 3052374;
        final static int LongNameHash = 3327612;
        final static int BooleanNameHash = 64711720;
        final static int FloatNameHash = 97526364;
        final static int ShortNameHash = 109413500;

        public static PrimitiveType typeOf(Class<?> fieldType) {
            if (!fieldType.isPrimitive()) return null;
            int hash = fieldType.getName().hashCode();
            switch (hash) {
                case DoubleNameHash:
                    return PrimitiveDouble;
                case IntNameHash:
                    return PrimitiveInt;
                case ByteNameHash:
                    return PrimitiveByte;
                case CharNameHash:
                    return PrimitiveCharacter;
                case LongNameHash:
                    return PrimitiveLong;
                case BooleanNameHash:
                    return PrimitiveBoolean;
                case FloatNameHash:
                    return PrimitiveFloat;
                case ShortNameHash:
                    return PrimitiveShort;
            }
            return null;
        }

        public Class getGenericArrayType() {
            return genericArrayType;
        }
    }

    static int arrayBaseOffset(Class arrayCls) {
        return UnsafeHelper.arrayBaseOffset(arrayCls);
    }

    static int arrayIndexScale(Class arrayCls) {
        return UnsafeHelper.arrayIndexScale(arrayCls);
    }
}
