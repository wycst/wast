package io.github.wycst.wast.common.reflect;

import sun.misc.Unsafe;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.TimeZone;

/**
 * <p> 请谨慎调用，真的不安全！
 * <p> 内部使用
 *
 * @Author: wangy
 * @Date: 2022/6/12
 * @Description:
 */
public final class UnsafeHelper {

    public static final long STRING_VALUE_OFFSET;
    public static final long STRING_CODER_OFFSET;
    public static final long DEFAULT_TIME_ZONE_OFFSET;
    public static final long BIGINTEGER_MAG_OFFSET;
    public static final long ARRAYLIST_ELEMENT_DATA_OFFSET;
    public static final long ARRAYLIST_SIZE_OFFSET;
    private static final long OVERRIDE_OFFSET;

    static final Unsafe UNSAFE;
    static {
        Field theUnsafeField;
        try {
            theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
        } catch (NoSuchFieldException exception) {
            theUnsafeField = null;
        }

        Unsafe instance = null;
        if (theUnsafeField != null) {
            try {
                instance = (Unsafe) theUnsafeField.get(null);
            } catch (IllegalAccessException exception) {
                throw new RuntimeException(exception);
            }
        }
        UNSAFE = instance;
    }

    public static final long CHAR_ARRAY_OFFSET = arrayBaseOffset(char[].class);
    public static final long BYTE_ARRAY_OFFSET = arrayBaseOffset(byte[].class);

    public final static long BAO_BUF_OFFSET = UnsafeHelper.getDeclaredFieldOffset(ByteArrayOutputStream.class, "buf");
    public final static long BAO_COUNT_OFFSET = UnsafeHelper.getDeclaredFieldOffset(ByteArrayOutputStream.class, "count");

    // String
    static {
        Field valueField;
        long valueOffset = -1;
        long coderOffset = -1;
        try {
            valueField = String.class.getDeclaredField("value");
            valueOffset = objectFieldOffset(valueField);
            Object emptyValue = getObjectValue("", valueOffset);
            if (!char[].class.isInstance(emptyValue)) {
                Field coderField = String.class.getDeclaredField("coder");
                coderOffset = objectFieldOffset(coderField);
            }
        } catch (Exception e) {
        }
        STRING_VALUE_OFFSET = valueOffset;
        STRING_CODER_OFFSET = coderOffset;

        long defaultTimeZoneOff = -1;
        try {
            Field timeZoneField = TimeZone.class.getDeclaredField("defaultTimeZone");
            defaultTimeZoneOff = UNSAFE.staticFieldOffset(timeZoneField);
        } catch (Throwable throwable) {
        }
        DEFAULT_TIME_ZONE_OFFSET = defaultTimeZoneOff;
    }

    // BigInteger
    static {
        long magOffset = -1;
        try {
            Field magField = BigInteger.class.getDeclaredField("mag");
            magOffset = objectFieldOffset(magField);
        } catch (Exception e) {
        }
        BIGINTEGER_MAG_OFFSET = magOffset;
    }

    // ArrayList
    static {
        long elementDataOffset = -1;
        long sizeOffset = -1;
        try {
            Field elementDataField = ArrayList.class.getDeclaredField("elementData");
            elementDataOffset = objectFieldOffset(elementDataField);
        } catch (Exception e) {
        }
        try {
            Field sizeField = ArrayList.class.getDeclaredField("size");
            sizeOffset = objectFieldOffset(sizeField);
        } catch (Exception e) {
        }
        if (elementDataOffset == -1 || sizeOffset == -1) {
            elementDataOffset = -1;
            sizeOffset = -1;
        }
        ARRAYLIST_ELEMENT_DATA_OFFSET = elementDataOffset;
        ARRAYLIST_SIZE_OFFSET = sizeOffset;
    }

    // reflect
    static {
        long overrideOffset = 12;
        try {
            //note: jdk18 not supported
            Field overrideField = AccessibleObject.class.getDeclaredField("override");
            overrideOffset = objectFieldOffset(overrideField);
        } catch (NoSuchFieldException e) {
        }
        OVERRIDE_OFFSET = overrideOffset;
    }

//    /**
//     * 获取静态属性的值
//     *
//     * @param targetClass
//     * @param fieldName
//     * @return
//     */
//    public static Object getStaticFieldValue(String targetClass, String fieldName) {
//        try {
//            Class target = Class.forName(targetClass);
//            Field field = target.getDeclaredField(fieldName);
//            long offset = UNSAFE.staticFieldOffset(field);
//            return UNSAFE.getObject(target, offset);
//        } catch (Exception e) {
//            return null;
//        }
//    }

    /***
     * jdk version 9+ use toCharArray
     * jdk version <= 8 use unsafe
     *
     * @param string
     * @return
     */
    public static char[] getChars(String string) {
        // note: jdk9+ value is byte[] and stringValueField will set to null
        if (STRING_CODER_OFFSET > -1) {
            return string.toCharArray();
        }
        string.getClass();
        return (char[]) getObjectValue(string, STRING_VALUE_OFFSET);
    }

    /**
     * 获取字符串的coder的结构偏移
     *
     * @return -1 if version <= 8
     */
    public static long getStringCoderOffset() {
        return STRING_CODER_OFFSET;
    }

    public static long getDeclaredFieldOffset(Class<?> targetClass, String fieldName) {
        try {
            Field field = targetClass.getDeclaredField(fieldName);
            return objectFieldOffset(field);
        } catch (Throwable throwable) {
            return -1;
        }
    }

    /**
     * 获取字符串的value
     *
     * @param source
     * @return
     */
    public static Object getStringValue(String source) {
        source.getClass();
        return getObjectValue(source, STRING_VALUE_OFFSET);
    }

    /**
     * 获取字符串的value
     *
     * @param source
     * @return
     */
    public static byte getStringCoder(String source) {
        source.getClass();
        if (STRING_CODER_OFFSET > -1) {
            return UNSAFE.getByte(source, STRING_CODER_OFFSET);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Build a string according to the character array to reduce the copy of the character array once
     *
     * @param buf
     * @return
     */
    public static String getString(char[] buf) {
        if (STRING_CODER_OFFSET == -1) {
            buf.getClass();
            String result = new String();
            putObjectValue(result, STRING_VALUE_OFFSET, buf);
            return result;
        }
        return new String(buf);
    }

    /**
     * ensure that buf is an ASCII byte array, no any check
     *
     * @param bytes ascii bytes
     * @return
     */
    public static String getAsciiString(byte[] bytes) {
        // note: jdk9+ value is byte[] and direct setting
        if (STRING_CODER_OFFSET > -1) {
            String result = null;
            try {
                result = (String) UNSAFE.allocateInstance(String.class);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
            // String coder is LATIN1(0)
            putObjectValue(result, STRING_VALUE_OFFSET, bytes);
            return result;
        }
        // if version <= jdk8, encoding byte[] to char[] even if it is ascii mode
        return new String(bytes);
    }

    /**
     * 通过utf16的字节构建字符串
     * support by jdk9+
     *
     * @param utf16Bytes
     * @return
     */
    public static String getUTF16String(byte[] utf16Bytes) {
        if (STRING_CODER_OFFSET > -1) {
            utf16Bytes.getClass();
            String result;
            try {
                result = (String) UNSAFE.allocateInstance(String.class);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
            UNSAFE.putObject(result, STRING_VALUE_OFFSET, utf16Bytes);
            UNSAFE.putByte(result, STRING_CODER_OFFSET, (byte) 1);
            return result;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * get mag of BigInteger
     *
     * @param value
     * @return
     */
    public static int[] getMag(BigInteger value) {
        if (BIGINTEGER_MAG_OFFSET > -1) {
            value.getClass();
            return (int[]) UNSAFE.getObject(value, BIGINTEGER_MAG_OFFSET);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * <p> 获取ArrayList的elementData属性</p>
     * <p> 注： 如果要对返回的数组遍历必须使用for(; i < size; )语法</p>
     *
     * @param value
     * @return
     */
    public static Object[] getArrayListData(ArrayList value) {
        if (ARRAYLIST_ELEMENT_DATA_OFFSET > -1) {
            value.getClass();
            return (Object[]) UNSAFE.getObject(value, ARRAYLIST_ELEMENT_DATA_OFFSET);
        } else {
            return value.toArray();
        }
    }

    public static void copyMemory(char[] chars, int cOff, byte[] bytes, int bOff, int cLen) {
        if (cLen <= 0) return;
        if (cOff < -1 || cLen > chars.length - cOff) {
            throw new IndexOutOfBoundsException("source offset = " + cOff + ", len = " + cLen);
        }
        if (bOff < -1 || cLen * 2 > bytes.length - bOff) {
            throw new IndexOutOfBoundsException("target offset = " + bOff + ", len = " + cLen);
        }
        int arrayBaseOffset = ReflectConsts.PrimitiveType.PrimitiveCharacter.arrayBaseOffset;
        int arrayIndexScale = ReflectConsts.PrimitiveType.PrimitiveCharacter.arrayIndexScale;
        int targetArrayBaseOffset = ReflectConsts.PrimitiveType.PrimitiveByte.arrayBaseOffset;
        int targetIndexScale = ReflectConsts.PrimitiveType.PrimitiveByte.arrayIndexScale;
        UNSAFE.copyMemory(chars, arrayBaseOffset + arrayIndexScale * cOff, bytes, targetArrayBaseOffset + targetIndexScale * bOff, cLen * arrayIndexScale);
    }

    public static void copyMemory(byte[] bytes, int bOff, char[] chars, int cOff, int bLen) {
        if (bLen <= 0) return;
        if (bOff < -1 || bLen > bytes.length - bOff) {
            throw new IndexOutOfBoundsException("source offset = " + bOff + ", len = " + bLen);
        }
        if (cOff < -1 || bLen / 2 > chars.length - cOff) {
            throw new IndexOutOfBoundsException("target offset = " + cOff + ", len = " + bLen);
        }
        int arrayBaseOffset = ReflectConsts.PrimitiveType.PrimitiveByte.arrayBaseOffset;
        int arrayIndexScale = ReflectConsts.PrimitiveType.PrimitiveByte.arrayIndexScale;
        int targetArrayBaseOffset = ReflectConsts.PrimitiveType.PrimitiveCharacter.arrayBaseOffset;
        int targetIndexScale = ReflectConsts.PrimitiveType.PrimitiveCharacter.arrayIndexScale;
        UNSAFE.copyMemory(bytes, arrayBaseOffset + arrayIndexScale * bOff, chars, targetArrayBaseOffset + targetIndexScale * cOff, bLen * arrayIndexScale);
    }

    /**
     * 获取数组指定下标的元素
     *
     * @param arr
     * @param index
     * @return
     */
    public static Object arrayValueAt(Object arr, int index) {
        if (UNSAFE != null) {
            if (index == -1) throw new ArrayIndexOutOfBoundsException(-1);
            Class<?> arrCls = arr.getClass();
            if (!arrCls.isArray()) {
                throw new UnsupportedOperationException("Non array object do not support get value by index");
            }
            Class<?> componentType = arrCls.getComponentType();
            ReflectConsts.PrimitiveType primitiveType = ReflectConsts.PrimitiveType.typeOf(componentType);
            if (primitiveType != null) {
                return primitiveType.elementAt(arr, index);
            } else {
                Object[] objects = (Object[]) arr;
                return objects[index];
            }
        } else {
            return Array.get(arr, index);
        }
    }

    public static TimeZone getDefaultTimeZone() {
        if (DEFAULT_TIME_ZONE_OFFSET > -1) {
            try {
                TimeZone timeZone = (TimeZone) UNSAFE.getObject(TimeZone.class, DEFAULT_TIME_ZONE_OFFSET);
                if (timeZone != null) {
                    return timeZone;
                }
            } catch (Throwable throwable) {
            }
        }
        return TimeZone.getDefault();
    }

    /**
     * 从offset开始读取4个字符
     *
     * @param buf
     * @param offset
     * @return
     */
    public static long getLong(char[] buf, int offset) {
        if (offset > -1 && offset < buf.length) {
            return UNSAFE.getLong(buf, CHAR_ARRAY_OFFSET + (offset << 1));
        }
        throw new IndexOutOfBoundsException("offset " + offset);
    }

    /**
     * 从offset开始读取4个字节
     *
     * @param buf
     * @param offset
     * @return
     */
    static int getInt(byte[] buf, int offset) {
        return UNSAFE.getInt(buf, BYTE_ARRAY_OFFSET + offset);
    }

    /**
     * 从offset开始读取8个字节
     *
     * @param buf
     * @param offset
     * @return
     */
    public static long getLong(byte[] buf, int offset) {
        if (offset > -1 && offset < buf.length) {
            return UNSAFE.getLong(buf, BYTE_ARRAY_OFFSET + offset);
        }
        throw new IndexOutOfBoundsException("offset " + offset);
    }

    public static long[] getCharLongs(String value) {
        char[] chars = getChars(value);
        int strLength = chars.length;
        int l = strLength >> 2;
        int rem = strLength & 3;
        if (rem > 0) {
            ++l;
        }
        char[] buf = new char[l << 2];
        value.getChars(0, strLength, buf, 0);

        long[] results = new long[l];
        int offset = 0;
        for (int i = 0; i < l; ++i) {
            results[i] = getLong(buf, offset);
            offset += 4;
        }
        return results;
    }

    public static long[] getByteLongs(String value) {
        byte[] bytes = value.getBytes();
        int byteLen = bytes.length;
        int l = byteLen >> 3;
        int rem = byteLen & 7;
        if (rem > 0) {
            ++l;
        }
        byte[] buf = new byte[l << 3];
        System.arraycopy(bytes, 0, buf, 0, byteLen);
        long[] results = new long[l];
        int offset = 0;
        for (int i = 0; i < l; ++i) {
            results[i] = getLong(buf, offset);
            offset += 8;
        }
        return results;
    }

    public static int[] getByteInts(String value) {
        byte[] bytes = value.getBytes();
        int byteLen = bytes.length;
        int l = byteLen >> 2;
        int rem = byteLen & 3;
        if (rem > 0) {
            ++l;
        }
        byte[] buf = new byte[l << 2];
        System.arraycopy(bytes, 0, buf, 0, byteLen);
        int[] results = new int[l];
        int offset = 0;
        for (int i = 0; i < l; ++i) {
            results[i] = getInt(buf, offset);
            offset += 4;
        }
        return results;
    }

    static long objectFieldOffset(Field field) {
        if (UNSAFE != null) {
            return UNSAFE.objectFieldOffset(field);
        }
        return -1;
    }

    static void putObjectValue(Object target, long fieldOffset, Object value) {
        target.getClass();
        UNSAFE.putObject(target, fieldOffset, value);
    }

    static Object getObjectValue(Object target, long fieldOffset) {
        target.getClass();
        return UNSAFE.getObject(target, fieldOffset);
    }

    public static Object newInstance(Class<?> targetClass) {
        try {
            return targetClass.newInstance();
        } catch (Throwable throwable) {
            try {
                targetClass.getClass();
                return UNSAFE.allocateInstance(targetClass);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static boolean setAccessible(AccessibleObject accessibleObject) {
        if (OVERRIDE_OFFSET > -1) {
            accessibleObject.getClass();
            UNSAFE.putBoolean(accessibleObject, OVERRIDE_OFFSET, true);
            return true;
        }
        return false;
    }

    static int arrayBaseOffset(Class arrayCls) {
        if (UNSAFE != null) {
            return UNSAFE.arrayBaseOffset(arrayCls);
        }
        return -1;
    }

    static int arrayIndexScale(Class arrayCls) {
        if (UNSAFE != null) {
            return UNSAFE.arrayIndexScale(arrayCls);
        }
        return -1;
    }
}
