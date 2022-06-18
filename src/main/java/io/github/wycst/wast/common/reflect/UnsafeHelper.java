package io.github.wycst.wast.common.reflect;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @Author: wangy
 * @Date: 2022/6/12 23:00
 * @Description:
 */
public class UnsafeHelper {

    private static final Unsafe unsafe;

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

    public static Unsafe getUnsafe() {
        return unsafe;
    }

    public static long objectFieldOffset(Field field) {
        if (unsafe != null) {
            return unsafe.objectFieldOffset(field);
        }
        return -1;
    }

    public static void putObjectValue(Object target, long fieldOffset, Object value) {
        unsafe.putObject(target, fieldOffset, value);
    }

    public static Object getObjectValue(Object target, long fieldOffset) {
        return unsafe.getObject(target, fieldOffset);
    }

    /**
     * 基本类型设置
     */
    public static void putPrimitiveValue(Object target, long fieldOffset, Object value, ReflectConsts.PrimitiveType primitiveType) {
        switch (primitiveType) {
            case Int:
                unsafe.putInt(target, fieldOffset, (Integer) value);
                break;
            case Byte:
                unsafe.putByte(target, fieldOffset, (Byte) value);
                break;
            case Long:
                unsafe.putLong(target, fieldOffset, (Long) value);
                break;
            case Short:
                unsafe.putShort(target, fieldOffset, (Short) value);
                break;
            case Double:
                unsafe.putDouble(target, fieldOffset, (Double) value);
                break;
            case Boolean:
                unsafe.putBoolean(target, fieldOffset, (Boolean) value);
                break;
            case Float:
                unsafe.putFloat(target, fieldOffset, (Float) value);
                break;
            case Character:
                unsafe.putChar(target, fieldOffset, (Character) value);
                break;
            default: {
            }
        }
    }

    /**
     * 基本类型设置
     */
    public static Object getPrimitiveValue(Object target, long fieldOffset, ReflectConsts.PrimitiveType primitiveType) {
        switch (primitiveType) {
            case Int:
                return unsafe.getInt(target, fieldOffset);
            case Byte:
                return unsafe.getByte(target, fieldOffset);
            case Long:
                return unsafe.getLong(target, fieldOffset);
            case Short:
                return unsafe.getShort(target, fieldOffset);
            case Double:
                return unsafe.getDouble(target, fieldOffset);
            case Boolean:
                return unsafe.getBoolean(target, fieldOffset);
            case Float:
                return unsafe.getFloat(target, fieldOffset);
            case Character:
                return unsafe.getChar(target, fieldOffset);
            default: {
                return 0;
            }
        }
    }
}
