package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.lang.reflect.Field;
import java.nio.ByteOrder;

public class JSONEndianRawCode extends JSONEndian {

    final static boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    final static Field STRING_VALUE_FIELD;

    static {
        Field stringValueField = null;
        try {
            stringValueField = String.class.getDeclaredField("value");
            UnsafeHelper.setAccessible(stringValueField);
        } catch (Exception e) {
        }
        STRING_VALUE_FIELD = stringValueField;
    }

    @Override
    public int mergeInt32(short shortVal, char pre, char suff) {
        return LITTLE_ENDIAN ? (suff << 24) | (shortVal << 8) | pre : (pre << 24) | (shortVal << 8) | suff;
    }

    @Override
    public long mergeInt64(long val, long pre, long suff) {
        return LITTLE_ENDIAN ? suff << 48 | val << 16 | pre : pre << 48 | val << 16 | suff;
    }

    @Override
    public long mergeInt64(long h32, long l32) {
        return LITTLE_ENDIAN ? l32 << 32 | h32 : h32 << 32 | l32;
    }

    // ‘-’ -> 0x2d
    @Override
    public long mergeYearAndMonth(int year, int month) {
        if (LITTLE_ENDIAN) {
            return 0x2d00002d00000000L | (long) JSONWriter.TWO_DIGITS_16_BITS[month] << 40 | JSONWriter.FOUR_DIGITS_32_BITS[year];
        } else {
            short[] TWO_DIGITS_16_BITS = JSONWriter.TWO_DIGITS_16_BITS;
            return (long) TWO_DIGITS_16_BITS[year] << 32 | TWO_DIGITS_16_BITS[month] << 8 | 0x2d00002d;
        }
    }

    // ':' -> 0x3a
    @Override
    public long mergeHHMMSS(int hour, int minute, int second) {
        if (LITTLE_ENDIAN) {
            short[] TWO_DIGITS_16_BITS = JSONWriter.TWO_DIGITS_16_BITS;
            return 0x00003a00003a0000L | (long) TWO_DIGITS_16_BITS[second] << 48 | (long) TWO_DIGITS_16_BITS[minute] << 24 | TWO_DIGITS_16_BITS[hour];
        } else {
            short[] TWO_DIGITS_16_BITS = JSONWriter.TWO_DIGITS_16_BITS;
            return 0x00003a00003a0000L | (long) TWO_DIGITS_16_BITS[hour] << 48 | (long) TWO_DIGITS_16_BITS[minute] << 24 | TWO_DIGITS_16_BITS[second];
        }
    }

    @Override
    public long getLong(byte[] buf, int offset) {
        return LITTLE_ENDIAN ? getLongLE(buf, offset) : getLongBE(buf, offset);
    }

    @Override
    public long getLong(char[] buf, int offset) {
        return LITTLE_ENDIAN ? getLongLE(buf, offset) : getLongBE(buf, offset);
    }

    @Override
    public int getInt(byte[] buf, int offset) {
        return LITTLE_ENDIAN ? getIntLE(buf, offset) : getIntBE(buf, offset);
    }

    @Override
    public int getInt(char[] buf, int offset) {
        return LITTLE_ENDIAN ? getIntLE(buf, offset) : getIntBE(buf, offset);
    }

    @Override
    public short getShort(byte[] buf, int offset) {
        return LITTLE_ENDIAN ? (short) getShortLE(buf, offset) : (short) getShortBE(buf, offset);
    }

    @Override
    public int putLong(byte[] buf, int offset, long value) {
        if (LITTLE_ENDIAN) {
            buf[offset++] = (byte) value;
            buf[offset++] = (byte) (value >> 8);
            buf[offset++] = (byte) (value >> 16);
            buf[offset++] = (byte) (value >> 24);
            buf[offset++] = (byte) (value >> 32);
            buf[offset++] = (byte) (value >> 40);
            buf[offset++] = (byte) (value >> 48);
            buf[offset] = (byte) (value >> 56);
        } else {
            buf[offset++] = (byte) (value >> 56);
            buf[offset++] = (byte) (value >> 48);
            buf[offset++] = (byte) (value >> 40);
            buf[offset++] = (byte) (value >> 32);
            buf[offset++] = (byte) (value >> 24);
            buf[offset++] = (byte) (value >> 16);
            buf[offset++] = (byte) (value >> 8);
            buf[offset] = (byte) value;
        }
        return 8;
    }

    @Override
    public int putLong(char[] buf, int offset, long value) {
        if (LITTLE_ENDIAN) {
            buf[offset++] = (char) value;
            buf[offset++] = (char) (value >> 16);
            buf[offset++] = (char) (value >> 32);
            buf[offset] = (char) (value >> 48);
        } else {
            buf[offset++] = (char) (value >> 48);
            buf[offset++] = (char) (value >> 32);
            buf[offset++] = (char) (value >> 16);
            buf[offset] = (char) value;
        }
        return 4;
    }

    @Override
    public int putInt(byte[] buf, int offset, int value) {
        if (LITTLE_ENDIAN) {
            buf[offset++] = (byte) value;
            buf[offset++] = (byte) (value >> 8);
            buf[offset++] = (byte) (value >> 16);
            buf[offset] = (byte) (value >> 24);
        } else {
            buf[offset++] = (byte) (value >> 24);
            buf[offset++] = (byte) (value >> 16);
            buf[offset++] = (byte) (value >> 8);
            buf[offset] = (byte) value;
        }
        return 4;
    }

    @Override
    public int putInt(char[] buf, int offset, int value) {
        if (LITTLE_ENDIAN) {
            buf[offset++] = (char) value;
            buf[offset] = (char) (value >> 16);
        } else {
            buf[offset++] = (char) (value >> 16);
            buf[offset] = (char) value;
        }
        return 2;
    }

    @Override
    public int putShort(byte[] buf, int offset, short value) {
        if (LITTLE_ENDIAN) {
            buf[offset++] = (byte) value;
            buf[offset] = (byte) (value >> 8);
        } else {
            buf[offset++] = (byte) (value >> 8);
            buf[offset] = (byte) value;
        }
        return 2;
    }

    @Override
    public Object getStringValue(String value) {
        try {
            return STRING_VALUE_FIELD.get(value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String createStringJDK8(char[] buf) {
        return new String(buf);
    }

    @Override
    public String createAsciiString(byte[] asciiBytes) {
        return new String(asciiBytes);
    }

    @Override
    public int digits2Chars(char[] buf, int offset) {
        if (LITTLE_ENDIAN) {
            int value = getIntLE(buf, offset);
            // 00000000 00110101 00000000 00110001 (eg: 15)
            if ((value & 0xFFF0FFF0) == 0x300030) {
                int h = value & 0x3f, l8 = (value >> 12) & 0xF0;
                return JSONGeneral.TWO_DIGITS_VALUES[h ^ l8];
            } else {
                return -1;
            }
        } else {
            int bigIntVal = getIntBE(buf, offset);
            if ((bigIntVal & 0xFFF0FFF0) == 0x300030) {
                int l = bigIntVal & 0xf, h = (bigIntVal >> 16) & 0xf;
                if (h > 9 || l > 9) return -1;
                return (h << 3) + (h << 1) + l;
            } else {
                return -1;
            }
        }
    }

    @Override
    public int digits2Bytes(byte[] buf, int offset) {
        if (LITTLE_ENDIAN) {
            int value = getShortLE(buf, offset);
            if ((value & 0xF0F0) == 0x3030) {
                // 00110101 00110001 (eg: 15)
                int h = value & 0x3f, l8 = (value >> 4) & 0xF0;
                return JSONGeneral.TWO_DIGITS_VALUES[h ^ l8];
            } else {
                return -1;
            }
        } else {
            int bigShortVal = getShortBE(buf, offset);
            if ((bigShortVal & 0xF0F0) == 0x3030) {
                int l = bigShortVal & 0xf, h = (bigShortVal >> 8) & 0xf;
                if (h > 9 || l > 9) return -1;
                return (h << 3) + (h << 1) + l;
            } else {
                return -1;
            }
        }
    }
}
