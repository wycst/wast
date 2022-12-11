package io.github.wycst.wast.common.reflect;

import sun.misc.Unsafe;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;

/**
 * <p> 请谨慎调用，真的不安全！
 * <p> 内部使用
 *
 * @Author: wangy
 * @Date: 2022/6/12
 * @Description:
 */
public class UnsafeHelper {

    private static final Unsafe unsafe;
    private static final Field stringValueField;
    private static final long stringValueOffset;
    private static final long stringCoderOffset;

    // maybe jdk9+ not supported
    private static final long OVERRIDE_OFFSET;
//    private static final Map<Class<?>, Long> ObjectArrayOffsetScales = new ConcurrentHashMap<Class<?>, Long>();

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
        unsafe = instance;

    }

    // String
    static {
        Field valueField;
        long valueOffset = -1;
        long coderOffset = -1;
        try {
            valueField = String.class.getDeclaredField("value");
            // jdk18 not support setAccessible
            setAccessible(valueField);
            valueOffset = objectFieldOffset(valueField);
            Object emptyValue = getObjectValue("", valueOffset);
            // jdk9+ type is byte[] not char[]
            if (!char[].class.isInstance(emptyValue)) {
                valueField = null;
                Field coderField = String.class.getDeclaredField("coder");
                coderOffset = objectFieldOffset(coderField);
            }
        } catch (Exception e) {
            valueField = null;
        }
        stringValueField = valueField;
        stringValueOffset = valueOffset;
        stringCoderOffset = coderOffset;
    }

    // reflect
    static {
        long overrideOffset = -1;
        try {
            //note: jdk18 not supported
            Field overrideField = AccessibleObject.class.getDeclaredField("override");
            overrideOffset = objectFieldOffset(overrideField);
        } catch (NoSuchFieldException e) {
        }
        OVERRIDE_OFFSET = overrideOffset;
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
        if (stringCoderOffset > -1) {
            return string.toCharArray();
        }
        string.getClass();
        return (char[]) getObjectValue(string, stringValueOffset);
    }

    /**
     * 获取字符串的coder的结构偏移
     *
     * @return -1 if version <= 8
     */
    public static long getStringCoderOffset() {
        return stringCoderOffset;
    }

    /**
     * 获取字符串的value
     *
     * @param source
     * @return
     */
    public static Object getStringValue(String source) {
        source.getClass();
        return getObjectValue(source, stringValueOffset);
    }

    /**
     * 获取字符串的value
     *
     * @param source
     * @return
     */
    public static byte getStringCoder(String source) {
        source.getClass();
        if (stringCoderOffset > -1) {
            return unsafe.getByte(source, stringCoderOffset);
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
        // note: jdk9+ value is byte[]
        if (stringCoderOffset > -1) {
            // If manual coding is required, check the coder is LATIN1 or UTF16
            return new String(buf);
        }
        // note: Suitable for version <= jdk8, value is char[]
        buf.getClass();
        String result = new String();
        putObjectValue(result, stringValueOffset, buf);
        return result;
    }

    /**
     * @param value
     * @return
     */
    public static void clearString(String value) {
        // note: jdk9+ value is byte[]
        value.getClass();
        if (stringCoderOffset > -1) {
            unsafe.putObject(value, stringValueOffset, new byte[0]);
            unsafe.putByte(value, stringCoderOffset, (byte) 0);
        } else {
            unsafe.putObject(value, stringValueOffset, new char[0]);
        }
    }

    /**
     * ensure that buf is an ASCII byte array, no any check
     *
     * @param bytes ascii bytes
     * @return
     */
    public static String getAsciiString(byte[] bytes) {
        // note: jdk9+ value is byte[] and direct setting
        if (stringCoderOffset > -1) {
            bytes.getClass();
            String result = new String();
            // String coder is LATIN1(0)
            putObjectValue(result, stringValueOffset, bytes);
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
        if (stringCoderOffset > -1) {
            utf16Bytes.getClass();
            String result = new String();
            unsafe.putObject(result, stringValueOffset, utf16Bytes);
            unsafe.putByte(result, stringCoderOffset, (byte) 1);
            return result;
        }
        throw new UnsupportedOperationException();
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
//        Class arrayCls = array.getClass();
        int base, scale, k = 0;
        if (unsafe != null) {
            ReflectConsts.PrimitiveType primitiveType = ReflectConsts.PrimitiveType.typeOf(componentType);
            if (primitiveType != null) {
                base = primitiveType.arrayBaseOffset;
                scale = primitiveType.arrayIndexScale;
                for (Object obj : collection) {
                    long valueOffset = base + scale * k++;
                    putPrimitiveValue(array, valueOffset, obj, primitiveType);
                }
            } else {
//                long objectArrOffsetScale = getObjectArrOffsetScale(arrayCls);
//                base = (int) (objectArrOffsetScale >> 32);
//                scale = (int) objectArrOffsetScale;
                Object[] objects = (Object[]) array;
                for (Object obj : collection) {
//                    long valueOffset = base + scale * k++;
//                    if (componentType.isInstance(obj)) {
//                        unsafe.putObject(array, valueOffset, obj);
//                    }
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

        if (unsafe != null) {
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
//                long objectArrOffsetScale = getObjectArrOffsetScale(arrCls);
//                base = (int) (objectArrOffsetScale >> 32);
//                scale = (int) objectArrOffsetScale;
//                long valueOffset = base + scale * index;
//                return unsafe.getObject(arr, valueOffset);
                Object[] objects = (Object[]) arr;
                return objects[index];
            }
        } else {
            return Array.get(arr, index);
        }
    }

//    private static long getObjectArrOffsetScale(Class<?> arrCls) {
//        Long objectArrOffsetScale = ObjectArrayOffsetScales.get(arrCls);
//        if (objectArrOffsetScale == null) {
//            int base = unsafe.arrayBaseOffset(arrCls);
//            int scale = unsafe.arrayIndexScale(arrCls);
//            objectArrOffsetScale = (long) base << 32 | scale;
//            ObjectArrayOffsetScales.put(arrCls, objectArrOffsetScale);
//        }
//        return objectArrOffsetScale;
//    }

    /**
     * Internal use
     */
    static Unsafe getUnsafe() {
        return unsafe;
    }

    static long objectFieldOffset(Field field) {
        if (unsafe != null) {
            return unsafe.objectFieldOffset(field);
        }
        return -1;
    }

    static void putObjectValue(Object target, long fieldOffset, Object value) {
        target.getClass();
        unsafe.putObject(target, fieldOffset, value);
    }

    static Object getObjectValue(Object target, long fieldOffset) {
        target.getClass();
        return unsafe.getObject(target, fieldOffset);
    }

    /**
     * 基本类型设置
     */
    static void putPrimitiveValue(Object target, long fieldOffset, Object value, ReflectConsts.PrimitiveType primitiveType) {
        primitiveType.put(target, fieldOffset, value);
//        target.getClass();
//        if (value == null) return;
//        switch (primitiveType) {
//            case PrimitiveInt:
//                unsafe.putInt(target, fieldOffset, (Integer) value);
//                break;
//            case PrimitiveByte:
//                unsafe.putByte(target, fieldOffset, (Byte) value);
//                break;
//            case PrimitiveLong:
//                unsafe.putLong(target, fieldOffset, (Long) value);
//                break;
//            case PrimitiveShort:
//                unsafe.putShort(target, fieldOffset, (Short) value);
//                break;
//            case PrimitiveDouble:
//                unsafe.putDouble(target, fieldOffset, (Double) value);
//                break;
//            case PrimitiveBoolean:
//                unsafe.putBoolean(target, fieldOffset, (Boolean) value);
//                break;
//            case PrimitiveFloat:
//                unsafe.putFloat(target, fieldOffset, (Float) value);
//                break;
//            case PrimitiveCharacter:
//                unsafe.putChar(target, fieldOffset, (Character) value);
//                break;
//            default: {
//            }
//        }
    }

    /**
     * 基本类型设置
     */
    static Object getPrimitiveValue(Object target, long fieldOffset, ReflectConsts.PrimitiveType primitiveType) {
        return primitiveType.get(target, fieldOffset);
//        switch (primitiveType) {
//            case PrimitiveInt:
//                return unsafe.getInt(target, fieldOffset);
//            case PrimitiveByte:
//                return unsafe.getByte(target, fieldOffset);
//            case PrimitiveLong:
//                return unsafe.getLong(target, fieldOffset);
//            case PrimitiveShort:
//                return unsafe.getShort(target, fieldOffset);
//            case PrimitiveDouble:
//                return unsafe.getDouble(target, fieldOffset);
//            case PrimitiveBoolean:
//                return unsafe.getBoolean(target, fieldOffset);
//            case PrimitiveFloat:
//                return unsafe.getFloat(target, fieldOffset);
//            case PrimitiveCharacter:
//                return unsafe.getChar(target, fieldOffset);
//            default: {
//                return 0;
//            }
//        }
    }

    public static boolean setAccessible(AccessibleObject accessibleObject) {
        if (OVERRIDE_OFFSET > -1) {
            unsafe.putBoolean(accessibleObject, OVERRIDE_OFFSET, true);
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
        if (unsafe != null) {
            return unsafe.arrayBaseOffset(arrayCls);
        }
        return -1;
    }

    static int arrayIndexScale(Class arrayCls) {
        if (unsafe != null) {
            return unsafe.arrayIndexScale(arrayCls);
        }
        return -1;
    }
}
