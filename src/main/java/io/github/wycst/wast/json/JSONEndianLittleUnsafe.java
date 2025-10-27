package io.github.wycst.wast.json;

final class JSONEndianLittleUnsafe extends JSONEndianUnsafe {
    @Override
    public int mergeInt32(short shortVal, char pre, char suff) {
        return (suff << 24) | (shortVal << 8) | pre;
    }

    @Override
    public long mergeInt64(long val, long pre, long suff) {
        return suff << 48 | val << 16 | pre;
    }

    @Override
    public long mergeInt64(long h32, long l32) {
        return l32 << 32 | h32;
    }

    // ‘-’ -> 0x2d
    @Override
    public long mergeYearAndMonth(int year, int month) {
        return 0x2d00002d00000000L | (long) JSONWriter.TWO_DIGITS_16_BITS[month] << 40 | JSONWriter.FOUR_DIGITS_32_BITS[year];
    }

    // ':' -> 0x3a
    @Override
    public long mergeHHMMSS(int hour, int minute, int second) {
        short[] TWO_DIGITS_16_BITS = JSONWriter.TWO_DIGITS_16_BITS;
        return 0x00003a00003a0000L | (long) TWO_DIGITS_16_BITS[second] << 48 | (long) TWO_DIGITS_16_BITS[minute] << 24 | TWO_DIGITS_16_BITS[hour];
    }

    @Override
    public int digits2Chars(char[] buf, int offset) {
        int value = UNSAFE.getInt(buf, CHAR_ARRAY_OFFSET + ((long) offset << 1));
        // 00000000 00110101 00000000 00110001 (eg: 15)
        if ((value & 0xFFF0FFF0) == 0x300030) {
            int h = value & 0x3f, l8 = (value >> 12) & 0xF0;
            return JSONGeneral.TWO_DIGITS_VALUES[h ^ l8];
        } else {
            return -1;
        }
    }

    @Override
    public int digits2Bytes(byte[] buf, int offset) {
        int value = UNSAFE.getShort(buf, BYTE_ARRAY_OFFSET + offset);
        if ((value & 0xF0F0) == 0x3030) {
            // 00110101 00110001 (eg: 15)
            int h = value & 0x3f, l8 = (value >> 4) & 0xF0;
            return JSONGeneral.TWO_DIGITS_VALUES[h ^ l8];
        } else {
            return -1;
        }
    }
}
