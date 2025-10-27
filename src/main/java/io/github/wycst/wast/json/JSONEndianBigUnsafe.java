package io.github.wycst.wast.json;

final class JSONEndianBigUnsafe extends JSONEndianUnsafe {
    @Override
    public int mergeInt32(short shortVal, char pre, char suff) {
        return (pre << 24) | (shortVal << 8) | suff;
    }

    @Override
    public long mergeInt64(long val, long pre, long suff) {
        return pre << 48 | val << 16 | suff;
    }

    @Override
    public long mergeInt64(long h32, long l32) {
        return h32 << 32 | l32;
    }

    @Override
    public long mergeYearAndMonth(int year, int month) {
        short[] TWO_DIGITS_16_BITS = JSONWriter.TWO_DIGITS_16_BITS;
        return (long) TWO_DIGITS_16_BITS[year] << 32 | TWO_DIGITS_16_BITS[month] << 8 | 0x2d00002d;
    }

    @Override
    public long mergeHHMMSS(int hour, int minute, int second) {
        short[] TWO_DIGITS_16_BITS = JSONWriter.TWO_DIGITS_16_BITS;
        return 0x00003a00003a0000L | (long) TWO_DIGITS_16_BITS[hour] << 48 | (long) TWO_DIGITS_16_BITS[minute] << 24 | TWO_DIGITS_16_BITS[second];
    }

    @Override
    public int digits2Bytes(byte[] buf, int offset) {
        int bigShortVal = UNSAFE.getShort(buf, BYTE_ARRAY_OFFSET + offset);
        if ((bigShortVal & 0xF0F0) == 0x3030) {
            int l = bigShortVal & 0xf, h = (bigShortVal >> 8) & 0xf;
            if (h > 9 || l > 9) return -1;
            return (h << 3) + (h << 1) + l;
        } else {
            return -1;
        }
    }

    @Override
    public int digits2Chars(char[] buf, int offset) {
        // 0-9 (0000000000110000 ~ 0000000000111001)
        // 00000000 00110001 00000000 00110001 & 11111111 11110000 11111111 11110000 (0xFFF0FFF0)
        // 00000000 00110000 00000000 00110000 （0x300030）
        int bigIntVal = UNSAFE.getInt(buf, CHAR_ARRAY_OFFSET + (offset << 1));
        if ((bigIntVal & 0xFFF0FFF0) == 0x300030) {
            int l = bigIntVal & 0xf, h = (bigIntVal >> 16) & 0xf;
            if (h > 9 || l > 9) return -1;
            return (h << 3) + (h << 1) + l;
        } else {
            return -1;
        }
    }
}
