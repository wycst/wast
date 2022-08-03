package io.github.wycst.wast.common.reflect;

import sun.misc.Unsafe;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

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
    private static final Field stringToChars;
    private static final long stringValueOffset;

    // maybe jdk9+ not supported
    private static final long OVERRIDE_OFFSET;

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
        try {
            valueField = String.class.getDeclaredField("value");
            // jdk18 not support setAccessible
            setAccessible(valueField);
            // JDK_VERSION <= jdk8
            valueOffset = objectFieldOffset(valueField);
            Object emptyValue = getObjectValue("", valueOffset);
            // jdk9+ type is byte[] not char[]
            if (!char[].class.isInstance(emptyValue)) {
                valueOffset = -1;
                valueField = null;
            }
        } catch (Exception e) {
            valueField = null;
        }
        stringToChars = valueField;
        stringValueOffset = valueOffset;
    }

    // reflect
    static {
        long overrideOffset = -1;
        try {
            Field overrideField = AccessibleObject.class.getDeclaredField("override");
            overrideOffset = objectFieldOffset(overrideField);
        } catch (NoSuchFieldException e) {
        }
        OVERRIDE_OFFSET = overrideOffset;
    }

    /***
     * jdk9+ use toCharArray
     * jdk8- use unsafe
     *
     * @param stringValue
     * @return
     */
    public static char[] getChars(String stringValue) {
        if (stringValue == null) {
            throw new NullPointerException();
        }
        if (stringToChars == null) {
            return stringValue.toCharArray();
        }
        try {
            // note: jdk9+ value is byte[]
            if (stringValueOffset > -1) {
                return (char[]) getObjectValue(stringValue, stringValueOffset);
            }
            return (char[]) stringToChars.get(stringValue);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Build a string according to the character array to reduce the copy of the character array once
     */
    public static String getString(char[] buf) throws Exception {
        if (stringToChars == null) {
            return new String(buf);
        }
        try {
            String result = new String();
            if (stringValueOffset > -1) {
                putObjectValue(result, stringValueOffset, buf);
                return result;
            }
            stringToChars.set(result, buf);
            return result;
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 内部使用
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
        if (target == null) throw new NullPointerException();
        unsafe.putObject(target, fieldOffset, value);
    }

    static Object getObjectValue(Object target, long fieldOffset) {
        if (target == null) throw new NullPointerException();
        return unsafe.getObject(target, fieldOffset);
    }

    /**
     * 基本类型设置
     */
    static void putPrimitiveValue(Object target, long fieldOffset, Object value, ReflectConsts.PrimitiveType primitiveType) {
        if (target == null) throw new NullPointerException();
        if (value == null) return;
        switch (primitiveType) {
            case PrimitiveInt:
                unsafe.putInt(target, fieldOffset, (Integer) value);
                break;
            case PrimitiveByte:
                unsafe.putByte(target, fieldOffset, (Byte) value);
                break;
            case PrimitiveLong:
                unsafe.putLong(target, fieldOffset, (Long) value);
                break;
            case PrimitiveShort:
                unsafe.putShort(target, fieldOffset, (Short) value);
                break;
            case PrimitiveDouble:
                unsafe.putDouble(target, fieldOffset, (Double) value);
                break;
            case PrimitiveBoolean:
                unsafe.putBoolean(target, fieldOffset, (Boolean) value);
                break;
            case PrimitiveFloat:
                unsafe.putFloat(target, fieldOffset, (Float) value);
                break;
            case PrimitiveCharacter:
                unsafe.putChar(target, fieldOffset, (Character) value);
                break;
            default: {
            }
        }
    }

    /**
     * 基本类型设置
     */
    static Object getPrimitiveValue(Object target, long fieldOffset, ReflectConsts.PrimitiveType primitiveType) {
        if (target == null) throw new NullPointerException();
        switch (primitiveType) {
            case PrimitiveInt:
                return unsafe.getInt(target, fieldOffset);
            case PrimitiveByte:
                return unsafe.getByte(target, fieldOffset);
            case PrimitiveLong:
                return unsafe.getLong(target, fieldOffset);
            case PrimitiveShort:
                return unsafe.getShort(target, fieldOffset);
            case PrimitiveDouble:
                return unsafe.getDouble(target, fieldOffset);
            case PrimitiveBoolean:
                return unsafe.getBoolean(target, fieldOffset);
            case PrimitiveFloat:
                return unsafe.getFloat(target, fieldOffset);
            case PrimitiveCharacter:
                return unsafe.getChar(target, fieldOffset);
            default: {
                return 0;
            }
        }
    }

    static boolean setAccessible(AccessibleObject field) {
        if (OVERRIDE_OFFSET > -1) {
            unsafe.putBoolean(field, OVERRIDE_OFFSET, true);
            return true;
        }
        return false;
    }
}
