package io.github.wycst.wast.json;

public abstract class JSONEndian {

    protected final static int getTwoDigitsValue(int val) {
        return JSONGeneral.TWO_DIGITS_VALUES[val];
    }

    protected final static short getTwoDigitsBitsValue(int val) {
        return JSONWriter.TWO_DIGITS_16_BITS[val];
    }

    protected final static int getFourDigitsBitsValue(int val) {
        return JSONWriter.FOUR_DIGITS_32_BITS[val];
    }

    //    final JSONEndian INSTANCE;
    public abstract int digits2Bytes(byte[] buf, int offset);

    public abstract int digits2Chars(char[] buf, int offset);

    public abstract int mergeInt32(short shortVal, char pre, char suff);

    public abstract long mergeInt64(long val, long pre, long suff);

    public abstract long mergeInt64(long h32, long l32);

    /**
     * 将年份(4位)和月份（2位）合并为8个字节的long值(yyyy-MM-)
     *
     * @param year
     * @param month
     * @return
     */
    public abstract long mergeYearAndMonth(int year, int month);

    /**
     * 将时分秒合并为8个字节的long值(HH:mm:ss)
     *
     * @param hour
     * @param minute
     * @param second
     * @return
     */
    public abstract long mergeHHMMSS(int hour, int minute, int second);

    public abstract long getLong(byte[] buf, int offset);

    public abstract long getLong(char[] buf, int offset);

    public abstract int getInt(byte[] buf, int offset);

    public abstract int getInt(char[] buf, int offset);

    public abstract short getShort(byte[] buf, int offset);

    public abstract int putLong(byte[] buf, int offset, long value);

    public abstract int putLong(char[] buf, int offset, long value);

    public abstract int putInt(byte[] buf, int offset, int value);

    public abstract int putInt(char[] buf, int offset, int value);

    public abstract int putShort(byte[] buf, int offset, short value);

    public abstract Object getStringValue(String value);

    public abstract String createStringJDK8(char[] buf);

    public abstract String createAsciiString(byte[] asciiBytes);

    public final static int getIntLE(char[] buf, int offset) {
        final int rem = buf.length - offset;
        if (rem < 2) {
            return rem == 0 ? 0 : buf[offset];
        }
        return buf[offset] | buf[offset + 1] << 16;
    }

    public final static int getIntBE(char[] buf, int offset) {
        final int rem = buf.length - offset;
        if (rem < 2) {
            return rem == 0 ? 0 : buf[offset] << 16;
        }
        return buf[offset + 1] | buf[offset] << 16;
    }


    public final static long getLongLE(char[] buf, int offset) {
        if (buf.length - offset < 4) {
            long val = 0;
            int bits = 0;
            for (int i = offset; i < buf.length; ++i) {
                val = val | (long) buf[i] << bits;
                bits += 16;
            }
            return val;
        }
        return buf[offset] | (long) buf[offset + 1] << 16 | (long) buf[offset + 2] << 32 | (long) buf[offset + 3] << 48;
    }

    public final static long getLongBE(char[] buf, int offset) {
        if (buf.length - offset < 4) {
            long val = 0;
            int bits = 48;
            for (int i = offset; i < buf.length; ++i) {
                val = val | (long) buf[i] << bits;
                bits -= 16;
            }
            return val;
        }
        return (long) buf[offset] << 48 | (long) buf[offset + 1] << 32 | (long) buf[offset + 2] << 16 | (long) buf[offset + 3];
    }

    public final static int getShortLE(byte[] buf, int offset) {
        final int rem = buf.length - offset;
        if (rem < 2) {
            return rem == 0 ? 0 : buf[offset] & 0xFF;
        }
        return (buf[offset] & 0xFF) | (buf[offset + 1] & 0xFF) << 8;
    }

    public final static int getShortBE(byte[] buf, int offset) {
        final int rem = buf.length - offset;
        if (rem < 2) {
            return rem == 0 ? 0 : (buf[offset] & 0xFF) << 8;
        }
        return (buf[offset + 1] & 0xFF) | (buf[offset] & 0xFF) << 8;
    }

    public final static int getIntLE(byte[] buf, int offset) {
        if (buf.length - offset < 4) {
            int val = 0;
            int bits = 0;
            for (int i = offset; i < buf.length; ++i) {
                val = val | (buf[i] & 0xFF) << bits;
                bits += 8;
            }
            return val;
        }
        return (buf[offset] & 0xFF) | (buf[offset + 1] & 0xFF) << 8 | (buf[offset + 2] & 0xFF) << 16 | (buf[offset + 3] & 0xFF) << 24;
    }

    public final static int getIntBE(byte[] buf, int offset) {
        if (buf.length - offset < 4) {
            int val = 0;
            int bits = 24;
            for (int i = offset; i < buf.length; ++i) {
                val = val | (buf[i] & 0xFF) << bits;
                bits -= 8;
            }
            return val;
        }
        return (buf[offset] & 0xFF) << 24 | (buf[offset + 1] & 0xFF) << 16 | (buf[offset + 2] & 0xFF) << 8 | (buf[offset + 3] & 0xFF);
    }

    public final static long getLongLE(byte[] buf, int offset) {
        if (buf.length - offset < 8) {
            long val = 0;
            int bits = 0;
            for (int i = offset; i < buf.length; ++i) {
                val = val | (buf[i] & 0xFFL) << bits;
                bits += 8;
            }
            return val;
        }
        return (buf[offset] & 0xFFL) | (buf[offset + 1] & 0xFFL) << 8 | (buf[offset + 2] & 0xFFL) << 16 | (buf[offset + 3] & 0xFFL) << 24 | (buf[offset + 4] & 0xFFL) << 32 | (buf[offset + 5] & 0xFFL) << 40 | (buf[offset + 6] & 0xFFL) << 48 | (buf[offset + 7] & 0xFFL) << 56;
    }

    public final static long getLongBE(byte[] buf, int offset) {
        if (buf.length - offset < 8) {
            long val = 0;
            int bits = 56;
            for (int i = offset; i < buf.length; ++i) {
                val = val | (buf[i] & 0xFFL) << bits;
                bits -= 8;
            }
            return val;
        }
        return (buf[offset] & 0xFFL) << 56 | (buf[offset + 1] & 0xFFL) << 48 | (buf[offset + 2] & 0xFFL) << 40 | (buf[offset + 3] & 0xFFL) << 32 | (buf[offset + 4] & 0xFFL) << 24 | (buf[offset + 5] & 0xFFL) << 16 | (buf[offset + 6] & 0xFFL) << 8 | (buf[offset + 7] & 0xFF);
    }
}