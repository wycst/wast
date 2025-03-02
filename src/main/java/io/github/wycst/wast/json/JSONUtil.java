package io.github.wycst.wast.json;

import io.github.wycst.wast.common.utils.EnvUtils;

/**
 * quickly find APIs using mask
 *
 * @Date 2024/6/14 19:08
 * @Created by wangyc
 */
public class JSONUtil {

    JSONUtil() {
    }

    final static long getQuoteOrBackslashMask(char[] buf, long offset, long quoteMask) {
        long v64 = JSONUnsafe.UNSAFE.getLong(buf, JSONUnsafe.CHAR_ARRAY_OFFSET + (offset << 1));
        return ((v64 ^ quoteMask) + 0x0001000100010001L) & ((v64 ^ 0xFFA3FFA3FFA3FFA3L) + 0x0001000100010001L) & 0x8000800080008000L;
    }

    /**
     * 从字符数组的offset位置开始查找下一个引号(单引号或者双引号)或者转义符(\\)
     *
     * @param buf
     * @param offset
     * @param quote     引号
     * @param quoteMask 引号按位反码（~）
     * @return 从offset开始, 第一个quote或者'\\'的位置，如果找不到将抛出IndexOutOfBoundsException
     * @throws IndexOutOfBoundsException
     */
    public int ensureIndexOfQuoteOrBackslashChar(char[] buf, int offset, char quote, long quoteMask) {
        final int limit32 = buf.length - 32;
        long result;
        if (offset <= limit32) {
            do {
                if ((result = getQuoteOrBackslashMask(buf, offset, quoteMask)) == 0x8000800080008000L
                        && (result = getQuoteOrBackslashMask(buf, offset = offset + 4, quoteMask)) == 0x8000800080008000L
                        && (result = getQuoteOrBackslashMask(buf, offset = offset + 4, quoteMask)) == 0x8000800080008000L
                        && (result = getQuoteOrBackslashMask(buf, offset = offset + 4, quoteMask)) == 0x8000800080008000L
                        && (result = getQuoteOrBackslashMask(buf, offset = offset + 4, quoteMask)) == 0x8000800080008000L
                        && (result = getQuoteOrBackslashMask(buf, offset = offset + 4, quoteMask)) == 0x8000800080008000L
                        && (result = getQuoteOrBackslashMask(buf, offset = offset + 4, quoteMask)) == 0x8000800080008000L
                        && (result = getQuoteOrBackslashMask(buf, offset = offset + 4, quoteMask)) == 0x8000800080008000L
                ) {
                    offset += 4;
                    continue;
                }
//                if (EnvUtils.BIG_ENDIAN) {
//                    result = Long.reverseBytes(result);
//                }
//                int r32 = (int) result;
//                if (r32 == 0x80008000) {
//                    offset += (result == 0x0000800080008000L ? 3 : 2);
//                } else if (r32 == 0x00008000) {
//                    ++offset;
//                }
                if (EnvUtils.LITTLE_ENDIAN) {
                    offset += Long.numberOfTrailingZeros(result ^ 0x8000800080008000L) >> 4;
                } else {
                    offset += Long.numberOfLeadingZeros(result ^ 0x8000800080008000L) >> 4;
                }
                char c;
                // 此处需要判断是否返回预期字符确保结果正确（当字符值大于0x8000(中文字符)时会导致快速命中错误）
                if ((c = buf[offset]) == quote || c == '\\') {
                    return offset;
                }
                // next
                ++offset;
            } while (offset <= limit32);
        }
        char c;
        while ((c = buf[offset]) != quote && c != '\\') {
            ++offset;
        }
        return offset;
    }

    final static long getQuoteOrBackslashOrUTF8Mask(byte[] buf, int offset, long quoteMask) {
        long v64 = JSONUnsafe.UNSAFE.getLong(buf, JSONUnsafe.BYTE_ARRAY_OFFSET + offset);
        return ((v64 ^ quoteMask) + 0x0101010101010101L) & ((v64 ^ 0xA3A3A3A3A3A3A3A3L) + 0x0101010101010101L) & 0x8080808080808080L;
    }

    final static int offsetTokenBytes(long result) {
        if (EnvUtils.LITTLE_ENDIAN) {
            return Long.numberOfTrailingZeros(result ^ 0x8080808080808080L) >> 3;
        } else {
            return Long.numberOfLeadingZeros(result ^ 0x8080808080808080L) >> 3;
        }
//        if(EnvUtils.BIG_ENDIAN) {
//            result = Long.reverseBytes(result);
//        }
//        int offset = 0;
//        int r32 = (int) result;
//        if (r32 == 0x80808080) {
//            offset += 4;
//            r32 = (int) (result >> 32);
//        }
//        switch (r32) {
//            case 0x00808080:
//                return offset + 3;
//            case 0x00008080:
//            case 0x80008080:
//                return offset + 2;
//            case 0x00000080:
//            case 0x00800080:
//            case 0x80000080:
//            case 0x80800080:
//                return offset + 1;
//            default:
//                return offset;
//        }
    }

    /**
     * <p> 确保从字节数组的offset位置开始查找下一个引号(单引号或者双引号)或者转义符(\\)或者负字节（utf-8编码，高位一定以11开头，以10开头的可以不考虑）；</p>
     * <p> 如果没有找到则抛出数组越界异常；</p>
     *
     * @param buf
     * @param offset
     * @param quote     引号(单引号或者双引号)
     * @param quoteMask 8个quote字节组成的long进行~运算值
     * @return throws IndexOutOfBoundsException if not found
     */
    public int ensureIndexOfQuoteOrBackslashOrUTF8Byte(byte[] buf, int offset, int quote, long quoteMask) {
        final int limit32 = buf.length - 32;
        long result = 0;
        boolean limitFlag;
        if ((limitFlag = offset <= limit32)
                && (result = getQuoteOrBackslashOrUTF8Mask(buf, offset, quoteMask)) == 0x8080808080808080L
                && (result = getQuoteOrBackslashOrUTF8Mask(buf, offset = offset + 8, quoteMask)) == 0x8080808080808080L
                && (result = getQuoteOrBackslashOrUTF8Mask(buf, offset = offset + 8, quoteMask)) == 0x8080808080808080L
                && (result = getQuoteOrBackslashOrUTF8Mask(buf, offset = offset + 8, quoteMask)) == 0x8080808080808080L
        ) {
            offset += 8;
            while ((limitFlag = offset <= limit32)
                    && (result = getQuoteOrBackslashOrUTF8Mask(buf, offset, quoteMask)) == 0x8080808080808080L
                    && (result = getQuoteOrBackslashOrUTF8Mask(buf, offset = offset + 8, quoteMask)) == 0x8080808080808080L
                    && (result = getQuoteOrBackslashOrUTF8Mask(buf, offset = offset + 8, quoteMask)) == 0x8080808080808080L
                    && (result = getQuoteOrBackslashOrUTF8Mask(buf, offset = offset + 8, quoteMask)) == 0x8080808080808080L
            ) {
                offset += 8;
            }
        }
        if (limitFlag) {
            return offset + offsetTokenBytes(result);
        } else {
            final int limit8 = buf.length - 8;
            while ((limitFlag = offset <= limit8) && (result = getQuoteOrBackslashOrUTF8Mask(buf, offset, quoteMask)) == 0x8080808080808080L) {
                offset += 8;
            }
            if (limitFlag) {
                return offset + offsetTokenBytes(result);
            }
        }
        byte c;
        while ((c = buf[offset]) != quote && c != '\\' && c >= 0) {
            ++offset;
        }
        return offset;
    }

    /**
     * scan token
     *
     * @param source
     * @param buf
     * @param beginIndex
     * @param token
     * @return
     */
    public int indexOf(String source, byte[] buf, int beginIndex, int token) {
        return source.indexOf(token, beginIndex);
    }

//    static final boolean isNoneEscaped8Bytes(byte[] buf, int offset) {
//        long value = JSONUnsafe.UNSAFE.getLong(buf, JSONUnsafe.BYTE_ARRAY_OFFSET + offset);
////        return ((value + 0x6060606060606060L) & ((value ^ 0xDDDDDDDDDDDDDDDDL) + 0x0101010101010101L) & ((value ^ 0xA3A3A3A3A3A3A3A3L) + 0x0101010101010101L) & 0x8080808080808080L) == 0x8080808080808080L;
//
//        // if high-order bits of 8 bytes are all 1 return true, otherwise, return false
//        // Not considering negative numbers
//        long notBackslashMask = (value ^ 0xA3A3A3A3A3A3A3A3L) + 0x0101010101010101L & 0x8080808080808080L;
//
//        // Based on the probability of occurrence, first determine whether it is greater than '"' and not equal to '\\', and return true in advance
//        if((notBackslashMask & value + 0x5D5D5D5D5D5D5D5DL) == 0x8080808080808080L) {
//            return true;
//        }
//
//        // complete standard verification
//        return ((value + 0x6060606060606060L) & ((value ^ 0xDDDDDDDDDDDDDDDDL) + 0x0101010101010101L) & notBackslashMask & 0x8080808080808080L) == 0x8080808080808080L;
//    }

//    /**
//     * <p>通过unsafe获取的32个字节值一次性判断是否需要转义(包含'"'或者‘\\’或者存在小于32的字节)，如果返回true则代表一定不存在待转义字节；</p>
//     * <p>内部调用并确保不会越界</p>
//     *
//     * @param buf
//     * @param offset
//     * @return
//     */
//    final static boolean isNoneEscaped32Bytes(byte[] buf, int offset) {
//        long v1 = JSONUnsafe.UNSAFE.getLong(buf, JSONUnsafe.BYTE_ARRAY_OFFSET + offset);
//        long v2 = JSONUnsafe.UNSAFE.getLong(buf, JSONUnsafe.BYTE_ARRAY_OFFSET + offset + 8);
//        long v3 = JSONUnsafe.UNSAFE.getLong(buf, JSONUnsafe.BYTE_ARRAY_OFFSET + offset + 16);
//        long v4 = JSONUnsafe.UNSAFE.getLong(buf, JSONUnsafe.BYTE_ARRAY_OFFSET + offset + 24);
//
//        long notBackslashMask = ((v1 ^ 0xA3A3A3A3A3A3A3A3L) + 0x0101010101010101L) & ((v2 ^ 0xA3A3A3A3A3A3A3A3L) + 0x0101010101010101L) & ((v3 ^ 0xA3A3A3A3A3A3A3A3L) + 0x0101010101010101L) & ((v4 ^ 0xA3A3A3A3A3A3A3A3L) + 0x0101010101010101L) & 0x8080808080808080L;
//        long geQuoteMask = (v1 + 0x5D5D5D5D5D5D5D5DL) & (v2 + 0x5D5D5D5D5D5D5D5DL) & (v3 + 0x5D5D5D5D5D5D5D5DL) & (v4 + 0x5D5D5D5D5D5D5D5DL);
//
//        // Based on prediction, prioritize whether all values are greater than '"' and not equal to '\\', which may be faster
//        if((notBackslashMask & geQuoteMask) == 0x8080808080808080L) { // > '"' && != '\\'
//            return true;
//        }
//
//        // complete standard verification
//        long ge32Mask = (v1 + 0x6060606060606060L) & (v2 + 0x6060606060606060L) & (v3 + 0x6060606060606060L) & (v4 + 0x6060606060606060L);
//        long notQuoteMask = ((v1 ^ 0xDDDDDDDDDDDDDDDDL) + 0x0101010101010101L) & ((v2 ^ 0xDDDDDDDDDDDDDDDDL) + 0x0101010101010101L) & ((v3 ^ 0xDDDDDDDDDDDDDDDDL) + 0x0101010101010101L) & ((v4 ^ 0xDDDDDDDDDDDDDDDDL) + 0x0101010101010101L);
//        return (ge32Mask & notQuoteMask & notBackslashMask) == 0x8080808080808080L; // >= 32 && != '"' && != '\\'
//    }

    /**
     * <p> 尽量查找没有需要转义的位置(判定: "或者'\\'或者小于32)</p>
     * <p> 内部调用 </p>
     *
     * @param buf    (len >= 32)
     * @param offset = 0
     * @return the first position（8n） of escaped
     */
    public int toNoEscapeOffset(byte[] buf, int offset) {
        if (JSONGeneral.isNoneEscaped16Bytes(JSONUnsafe.getLong(buf, offset), JSONUnsafe.getLong(buf, offset + 8))
                && JSONGeneral.isNoneEscaped16Bytes(JSONUnsafe.getLong(buf, offset = offset + 16), JSONUnsafe.getLong(buf, offset + 8))) {
            offset += 16;
            final int limit = buf.length - 32;
            while (offset <= limit
                    && JSONGeneral.isNoneEscaped8Bytes(JSONUnsafe.getLong(buf, offset))
                    && JSONGeneral.isNoneEscaped8Bytes(JSONUnsafe.getLong(buf, offset = offset + 8))
                    && JSONGeneral.isNoneEscaped8Bytes(JSONUnsafe.getLong(buf, offset = offset + 8))
                    && JSONGeneral.isNoneEscaped8Bytes(JSONUnsafe.getLong(buf, offset = offset + 8))) {
                offset += 8;
            }
        }
        return offset;
    }

    static final boolean isNoneEscaped4Chars(char[] buf, int offset) {
        long value = JSONUnsafe.getLong(buf, offset);
        return (((value + 0x7FE07FE07FE07FE0L) & (value ^ 0xFFDDFFDDFFDDFFDDL) + 0x0001000100010001L & (value ^ 0xFFA3FFA3FFA3FFA3L) + 0x0001000100010001L) & 0x8000800080008000L) == 0x8000800080008000L;
    }

    /**
     * <p> 尽量查找没有需要转义的位置(判定: "或者'\\'或者小于32)</p>
     * <p> 内部调用 </p>
     *
     * @param buf    （buf.len >= 64）
     * @param offset 0
     * @return the first position（4n） of escaped
     */
    public final int toNoEscapeOffset(char[] buf, int offset) {
        // 64
        if (isNoneEscaped4Chars(buf, offset)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
                && isNoneEscaped4Chars(buf, offset = offset + 4)
        ) {
            offset += 4;
            final int limit = buf.length - 16;
            while (offset <= limit
                    && isNoneEscaped4Chars(buf, offset)
                    && isNoneEscaped4Chars(buf, offset = offset + 4)
                    && isNoneEscaped4Chars(buf, offset = offset + 4)
                    && isNoneEscaped4Chars(buf, offset = offset + 4)
            ) {
                offset += 4;
            }
        }
        return offset;
    }

//    /**
//     * Starting from offset, search for the first visible byte (>32).
//     * <p>
//     * Considering that spaces are generally not very long, here we check 4 bytes per batch
//     *
//     * @param buf
//     * @param offset
//     * @return
//     */
//    public int skipWhiteSpaces(byte[] buf, int offset) {
//        final int limit = buf.length - 16;
//        while (offset <= limit) {
//            int val = JSONUnsafe.getInt(buf, offset);
//            int result = (val + 0x5F5F5F5F & 0x80808080);
//            if (result != 0) {
//                int rem = EnvUtils.LITTLE_ENDIAN ? Integer.numberOfTrailingZeros(result) >> 3 : Integer.numberOfLeadingZeros(result) >> 3;
//                return offset + rem;
//            }
//            offset += 4;
//        }
//
//        return 0;
//    }

    public boolean isSupportVectorWellTest() {
        return false;
    }
}
