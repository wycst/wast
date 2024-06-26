package io.github.wycst.wast.common.reflect;

import io.github.wycst.wast.common.utils.EnvUtils;
import sun.misc.Unsafe;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
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

    public static final Unsafe UNSAFE;

    public static final long STRING_VALUE_OFFSET;
    public static final long STRING_CODER_OFFSET;
    public static final long DEFAULT_TIME_ZONE_OFFSET;
    public static final long BIGINTEGER_MAG_OFFSET;
    public static final long ARRAYLIST_ELEMENT_DATA_OFFSET;
    public static final long ARRAYLIST_SIZE_OFFSET;
    public static final long OVERRIDE_OFFSET;
    public static final long INT64_MASK_7_BITS = 0x00FFFFFFFFFFFFFFL;
    public static final long INT64_MASK_6_BITS = 0x0000FFFFFFFFFFFFL;
    public static final long INT64_MASK_5_BITS = 0x000000FFFFFFFFFFL;
    public static final int INT32_MASK_3_BITS = 0x00FFFFFF;

    static {
        Field theUnsafeField = null;
        try {
            theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
        } catch (NoSuchFieldException exception) {
            theUnsafeField = null;
        }

        Unsafe instance = null;
        if (theUnsafeField != null) {
            try {
                instance = (Unsafe) theUnsafeField.get((Object) null);
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
            // jdk18 not support setAccessible
//            setAccessible(valueField);
            valueOffset = objectFieldOffset(valueField);
            Object emptyValue = getObjectValue("", valueOffset);
            // jdk9+ type is byte[] not char[]
            if (!char[].class.isInstance(emptyValue)) {
//                valueField = null;
                Field coderField = String.class.getDeclaredField("coder");
                coderOffset = objectFieldOffset(coderField);
            }
        } catch (Exception e) {
//            valueField = null;
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

    /**
     * 获取静态属性的值
     *
     * @param targetClass
     * @param fieldName
     * @return
     */
    public static Object getStaticFieldValue(String targetClass, String fieldName) {
        try {
            Class target = Class.forName(targetClass);
            Field field = target.getDeclaredField(fieldName);
            long offset = UNSAFE.staticFieldOffset(field);
            return UNSAFE.getObject(target, offset);
        } catch (Exception e) {
            return null;
        }
    }

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

//    /***
//     * 将utf16 byte[]转化为char[](2个字节转一个字符)
//     *
//     * @param bytes
//     * @return
//     */
//    public static char[] getChars(byte[] bytes) {
//        int len = bytes.length;
//        char[] chars = new char[len >> 1];
//        UNSAFE.copyMemory(bytes, ReflectConsts.PrimitiveType.PrimitiveByte.arrayBaseOffset, chars, ReflectConsts.PrimitiveType.PrimitiveCharacter.arrayBaseOffset, ReflectConsts.PrimitiveType.PrimitiveByte.arrayIndexScale * len);
//        return chars;
//    }

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
            // note: Suitable for version <= jdk8, value is char[]
            buf.getClass();
            String result = new String();
            putObjectValue(result, STRING_VALUE_OFFSET, buf);
            return result;
        }
        // note: jdk9+ value is byte[]
        // If manual coding is required, check the coder is LATIN1 or UTF16
        return new String(buf);
    }

//    /**
//     * @param value
//     * @return
//     */
//    public static void clearString(String value) {
//        // note: jdk9+ value is byte[]
//        value.getClass();
//        if (stringCoderOffset > -1) {
//            unsafe.putObject(value, stringValueOffset, new byte[0]);
//            unsafe.putByte(value, stringCoderOffset, (byte) 0);
//        } else {
//            unsafe.putObject(value, stringValueOffset, new char[0]);
//        }
//    }

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
            String result = null;
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

    /**
     * <p> Building an ArrayList based on an array </p>
     *
     * @param elementData
     * @param len
     * @return
     */
    public static ArrayList fromArrayListData(Object[] elementData, int len) {
        ArrayList arrayList = new ArrayList(len);
        elementData.getClass();
        if (ARRAYLIST_ELEMENT_DATA_OFFSET > -1 && len > -1) {
            UNSAFE.putObject(arrayList, ARRAYLIST_ELEMENT_DATA_OFFSET, elementData);
            UNSAFE.putInt(arrayList, ARRAYLIST_SIZE_OFFSET, len);
        } else {
            for (int i = 0; i < len; ++i) {
                arrayList.add(elementData[i]);
            }
        }
        return arrayList;
    }

    public static void copyMemory(char[] chars, int cOff, byte[] bytes, int bOff, int cLen) {
        chars.getClass();
        bytes.getClass();
        int arrayBaseOffset = ReflectConsts.PrimitiveType.PrimitiveCharacter.arrayBaseOffset;
        int arrayIndexScale = ReflectConsts.PrimitiveType.PrimitiveCharacter.arrayIndexScale;
        int targetArrayBaseOffset = ReflectConsts.PrimitiveType.PrimitiveByte.arrayBaseOffset;
        int targetIndexScale = ReflectConsts.PrimitiveType.PrimitiveByte.arrayIndexScale;
        UNSAFE.copyMemory(chars, arrayBaseOffset + arrayIndexScale * cOff, bytes, targetArrayBaseOffset + targetIndexScale * bOff, cLen * arrayIndexScale);
    }

    public static void copyMemory(byte[] bytes, int bOff, char[] chars, int cOff, int bLen) {
        chars.getClass();
        bytes.getClass();
        int arrayBaseOffset = ReflectConsts.PrimitiveType.PrimitiveByte.arrayBaseOffset;
        int arrayIndexScale = ReflectConsts.PrimitiveType.PrimitiveByte.arrayIndexScale;
        int targetArrayBaseOffset = ReflectConsts.PrimitiveType.PrimitiveCharacter.arrayBaseOffset;
        int targetIndexScale = ReflectConsts.PrimitiveType.PrimitiveCharacter.arrayIndexScale;
        UNSAFE.copyMemory(bytes, arrayBaseOffset + arrayIndexScale * bOff, chars, targetArrayBaseOffset + targetIndexScale * cOff, bLen * arrayIndexScale);
    }

    /**
     * 集合转化为指定组件的数组
     *
     * @param collection
     * @param componentType
     * @return
     */
    public static Object toArray(Collection collection, Class<?> componentType) {
        collection.getClass();
        componentType.getClass();
        Object array = Array.newInstance(componentType, collection.size());
        int base, scale, k = 0;
        if (UNSAFE != null) {
            ReflectConsts.PrimitiveType primitiveType = ReflectConsts.PrimitiveType.typeOf(componentType);
            if (primitiveType != null) {
                base = primitiveType.arrayBaseOffset;
                scale = primitiveType.arrayIndexScale;
                for (Object obj : collection) {
                    long valueOffset = base + scale * k++;
                    primitiveType.put(array, valueOffset, obj);
                }
            } else {
                Object[] objects = (Object[]) array;
                for (Object obj : collection) {
                    objects[k++] = obj;
                }
            }
        } else {
            for (Object obj : collection) {
                Array.set(array, k++, obj);
            }
        }
        return array;
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
                int base, scale;
                base = primitiveType.arrayBaseOffset;
                scale = primitiveType.arrayIndexScale;
                long valueOffset = base + scale * index;
                return primitiveType.get(arr, valueOffset);
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
     * Internal use
     */
    public static Unsafe getUnsafe() {
        return UNSAFE;
    }

    /**
     * <p> 高效比较两个字节数组片段 </p>
     * <p>
     * Refer to ArraysSupport # vectorizedMismatch
     *
     * @param a
     * @param aOffset
     * @param b
     * @param bOffset
     * @param len
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static boolean equals(byte[] a, int aOffset, byte[] b, int bOffset, int len, long remValueForBytes) {
        if (len >= 8) {
            do {
                long la = UnsafeHelper.getLong(a, aOffset);
                long lb = UnsafeHelper.getLong(b, bOffset);
                if (la != lb) return false;
                len -= 8;
                aOffset += 8;
                bOffset += 8;
            } while (len >= 8);
            if (len == 0) return true;
            int padd = 8 - len;
            aOffset -= padd;
            bOffset -= padd;
            return UnsafeHelper.getLong(a, aOffset) == UnsafeHelper.getLong(b, bOffset);
        }
        if (len >= 4) {
            int la = UnsafeHelper.getInt(a, aOffset);
            int lb = UnsafeHelper.getInt(b, bOffset);
            if (la != lb) return false;
            int v = len - 4;
            if (v == 0) return true;
            aOffset += v;
            bOffset += v;
            return UnsafeHelper.getInt(a, aOffset) == UnsafeHelper.getInt(b, bOffset);
        }
        // 1 2 3
        switch (len) {
            case 1:
                return a[aOffset] == remValueForBytes;
            case 2:
                return UnsafeHelper.getShort(a, aOffset) == remValueForBytes;
            default:
                return a[aOffset++] == b[bOffset] && UnsafeHelper.getShort(a, aOffset) == remValueForBytes;
        }
    }

    /**
     * <p> 高效比较两个字符数组片段 </p>
     * <p>
     * Refer to ArraysSupport # vectorizedMismatch
     *
     * @param a
     * @param aOffset
     * @param b
     * @param bOffset
     * @param len
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static boolean equals(char[] a, int aOffset, char[] b, int bOffset, int len, long remValueForChars) {
        if (len >= 4) {
            do {
                long la = UnsafeHelper.getLong(a, aOffset);
                long lb = UnsafeHelper.getLong(b, bOffset);
                if (la != lb) return false;
                len -= 4;
                aOffset += 4;
                bOffset += 4;
            } while (len >= 4);
            if (len == 0) return true;
            int v = 4 - len;
            aOffset -= v;
            bOffset -= v;
            return UnsafeHelper.getLong(a, aOffset) == UnsafeHelper.getLong(b, bOffset);
        }
        // 1 2 3
        switch (len) {
            case 1:
                return a[aOffset] == remValueForChars;
            case 2:
                return UnsafeHelper.getInt(a, aOffset) == remValueForChars;
            default:
                return a[aOffset++] == b[bOffset] && UnsafeHelper.getInt(a, aOffset) == remValueForChars;
        }
    }

    /**
     * 从offset开始读取2个字符
     *
     * @param buf
     * @param offset
     * @return
     */
    public static int getInt(char[] buf, int offset) {
        return UNSAFE.getInt(buf, CHAR_ARRAY_OFFSET + (offset << 1));
    }

    /**
     * 从offset开始读取4个字符
     *
     * @param buf
     * @param offset
     * @return
     */
    public static long getLong(char[] buf, int offset) {
        buf.getClass();
        return UNSAFE.getLong(buf, CHAR_ARRAY_OFFSET + (offset << 1));
    }

    public static int putLong(char[] buf, int offset, long value) {
        buf.getClass();
        long off = CHAR_ARRAY_OFFSET + (offset << 1);
        UNSAFE.putLong(buf, off, value);
        return 4;
    }

    public static int putLong(byte[] buf, int offset, long value) {
        buf.getClass();
        long off = BYTE_ARRAY_OFFSET + offset;
        UNSAFE.putLong(buf, off, value);
        return 8;
    }

    public static int putInt(char[] buf, int offset, int value) {
        buf.getClass();
        long off = CHAR_ARRAY_OFFSET + (offset << 1);
        UNSAFE.putInt(buf, off, value);
        return 2;
    }

    /**
     * 从offset开始读取2个字节
     *
     * @param buf
     * @param offset
     * @return
     */
    public static short getShort(byte[] buf, int offset) {
        buf.getClass();
        return UNSAFE.getShort(buf, BYTE_ARRAY_OFFSET + offset);
    }

    /**
     * 从offset开始读取4个字节
     *
     * @param buf
     * @param offset
     * @return
     */
    public static int getInt(byte[] buf, int offset) {
        buf.getClass();
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
        buf.getClass();
        return UNSAFE.getLong(buf, BYTE_ARRAY_OFFSET + offset);
    }

//    /**
//     * 字节数组的long值组成的数组
//     *
//     * @param buf
//     * @return
//     */
//    public static long[] getUnsafeLongs(byte[] buf) {
//        int byteLen = buf.length;
//        int rem = byteLen & 7;
//        int len = rem == 0 ? byteLen >> 3 : (byteLen >> 3) + 1;
//        long[] longValues = new long[len];
//        for (int i = 0; i < len; ++i) {
//            int offset = i << 3;
//            if(offset + 8 > byteLen) {
//                offset = byteLen - 8;
//                if(offset < 0) {
//                    offset = 0;
//                }
//            }
//            longValues[i] = getUnsafeLong(buf, offset, 8);
//        }
//        return longValues;
//    }

//    /**
//     * 字符数组的long值组成的数组
//     *
//     * @param buf
//     * @return
//     */
//    public static long[] getUnsafeLongs(char[] buf) {
//        int byteLen = buf.length;
//        int rem = byteLen & 3;
//        int len = rem == 0 ? byteLen >> 2 : (byteLen >> 2) + 1;
//        long[] longValues = new long[len];
//        for (int i = 0; i < len; ++i) {
//            longValues[i] = getUnsafeLong(buf, i * 4, 4);
//        }
//        return longValues;
//    }

    /**
     * 从offset开始读取len个字节的long值,最多读取8个字节
     *
     * @param buf
     * @param offset
     * @param len
     * @return
     */
    public static long getUnsafeLong(byte[] buf, int offset, int len) {
        int remSize = buf.length - offset;
        if (remSize > 8) {
            switch (len) {
                case 1:
                    return buf[offset];
                case 2:
                    return UNSAFE.getShort(buf, BYTE_ARRAY_OFFSET + offset);
                case 3: {
                    int val = UNSAFE.getInt(buf, BYTE_ARRAY_OFFSET + offset);
                    return EnvUtils.BIG_ENDIAN ? val >> 8 : val & INT32_MASK_3_BITS;
                }
                case 4:
                    return UNSAFE.getInt(buf, BYTE_ARRAY_OFFSET + offset);
                case 5: {
                    long val = UNSAFE.getLong(buf, BYTE_ARRAY_OFFSET + offset);
                    return EnvUtils.BIG_ENDIAN ? val >> 24 : val & INT64_MASK_5_BITS;
                }
                case 6: {
                    long val = UNSAFE.getLong(buf, BYTE_ARRAY_OFFSET + offset);
                    return EnvUtils.BIG_ENDIAN ? val >> 16 : val & INT64_MASK_6_BITS;
                }
                case 7: {
                    long val = UNSAFE.getLong(buf, BYTE_ARRAY_OFFSET + offset);
                    return EnvUtils.BIG_ENDIAN ? val >> 8 : val & INT64_MASK_7_BITS;
                }
                default: {
                    return UNSAFE.getLong(buf, BYTE_ARRAY_OFFSET + offset);
                }
            }
        } else {
            // use bits
            long val = 0;
            if (offset + len > buf.length) {
                len = buf.length - offset;
            }
            for (int i = 0; i < len; ++i) {
                long b = buf[i + offset];
                if (EnvUtils.BIG_ENDIAN) {
                    val = val << 8 | b;
                } else {
                    val = val | (b << (i << 3));
                }
            }
            return val;
        }
    }

    /**
     * 从offset开始读取len个字符的long值,最多读取4个字符
     *
     * @param buf
     * @param offset
     * @param len
     * @return
     */
    public static long getUnsafeLong(char[] buf, int offset, int len) {
        int remSize = buf.length - offset;
        if (remSize > 4) {
            switch (len) {
                case 1:
                    return buf[offset];
                case 2:
                    return UNSAFE.getInt(buf, CHAR_ARRAY_OFFSET + offset);
                case 3: {
                    long val = UNSAFE.getLong(buf, CHAR_ARRAY_OFFSET + offset);
                    return EnvUtils.BIG_ENDIAN ? val >> 16 : val & INT64_MASK_6_BITS;
                }
                default: {
                    return UNSAFE.getLong(buf, CHAR_ARRAY_OFFSET + offset);
                }
            }
        } else {
            // use bits
            long val = 0;
            if (offset + len > buf.length) {
                len = buf.length - offset;
            }
            for (int i = 0; i < len; ++i) {
                long b = buf[i + offset];
                if (EnvUtils.BIG_ENDIAN) {
                    val = val << 16 | b;
                } else {
                    val = val | (b << (i << 4));
                }
            }
            return val;
        }
    }

    public static int putInt(byte[] buf, int offset, int value) {
        buf.getClass();
        long off = BYTE_ARRAY_OFFSET + offset;
        UNSAFE.putInt(buf, off, value);
        return 4;
    }

    public static int putShort(byte[] buf, int offset, short value) {
        buf.getClass();
        long off = BYTE_ARRAY_OFFSET + offset;
        UNSAFE.putShort(buf, off, value);
        return 2;
    }

    public static long[] getLongs(String value) {
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

    public static int[] getInts(String value) {
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

    public static void writeLongsToChars(long[] longs, char[] chars, int offset) {
        for (long l : longs) {
            putLong(chars, offset, l);
            offset += 4;
        }
    }

    public static void writeIntsToBytes(int[] ints, byte[] bytes, int offset) {
        for (int val : ints) {
            putInt(bytes, offset, val);
            offset += 4;
        }
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

    public static void setAccessibleList(AccessibleObject... accessibleList) {
        for (AccessibleObject accessibleObject : accessibleList) {
            setAccessible(accessibleObject);
        }
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
