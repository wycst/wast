package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.UnsafeHelper;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

abstract class JSONEndianUnsafe extends JSONEndian {
    protected static final Unsafe UNSAFE;
    static final long BYTE_ARRAY_OFFSET = UnsafeHelper.BYTE_ARRAY_OFFSET;
    static final long CHAR_ARRAY_OFFSET = UnsafeHelper.CHAR_ARRAY_OFFSET;
    static final long STRING_VALUE_OFFSET = UnsafeHelper.STRING_VALUE_OFFSET;

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

    @Override
    public final short getShort(byte[] buf, int offset) {
        return UNSAFE.getShort(buf, BYTE_ARRAY_OFFSET + offset);
    }

    @Override
    public final int getInt(char[] buf, int offset) {
        return UNSAFE.getInt(buf, CHAR_ARRAY_OFFSET + ((long) offset << 1));
    }

    @Override
    public final int getInt(byte[] buf, int offset) {
        return UNSAFE.getInt(buf, BYTE_ARRAY_OFFSET + offset);
    }

    @Override
    public final long getLong(char[] buf, int offset) {
        return UNSAFE.getLong(buf, CHAR_ARRAY_OFFSET + ((long) offset << 1));
    }

    @Override
    public final long getLong(byte[] buf, int offset) {
        return UNSAFE.getLong(buf, BYTE_ARRAY_OFFSET + offset);
    }

    @Override
    public int putShort(byte[] buf, int offset, short value) {
        UNSAFE.putShort(buf, BYTE_ARRAY_OFFSET + offset, value);
        return 2;
    }

    @Override
    public int putInt(char[] buf, int offset, int value) {
        UNSAFE.putInt(buf, CHAR_ARRAY_OFFSET + ((long) offset << 1), value);
        return 2;
    }

    @Override
    public int putInt(byte[] buf, int offset, int value) {
        UNSAFE.putInt(buf, BYTE_ARRAY_OFFSET + offset, value);
        return 4;
    }

    @Override
    public int putLong(char[] buf, int offset, long value) {
        UNSAFE.putLong(buf, CHAR_ARRAY_OFFSET + ((long) offset << 1), value);
        return 4;
    }

    @Override
    public int putLong(byte[] buf, int offset, long value) {
        UNSAFE.putLong(buf, BYTE_ARRAY_OFFSET + offset, value);
        return 8;
    }

    @Override
    public Object getStringValue(String value) {
        return UNSAFE.getObject(value, STRING_VALUE_OFFSET);
    }

    @Override
    public final String createAsciiString(byte[] asciiBytes) {
        String result = new String();
        UNSAFE.putObject(result, STRING_VALUE_OFFSET, asciiBytes);
        return result;
    }

    @Override
    public final String createStringJDK8(char[] buf) {
        String target = new String();
        UNSAFE.putObject(target, STRING_VALUE_OFFSET, buf);
        return target;
    }

//    @Override
//    public void putStringValue(String str, Object value) {
//        UNSAFE.putObject(str, STRING_VALUE_OFFSET, value);
//    }
}
