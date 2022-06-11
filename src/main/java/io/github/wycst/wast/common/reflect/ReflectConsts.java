package io.github.wycst.wast.common.reflect;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
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
            } else if(clazz == char[].class) {
                return CLASS_TYPE_CHAR_ARRAY;
            } else if(clazz == byte[].class) {
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

}
