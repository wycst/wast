package io.github.wycst.wast.json;

import io.github.wycst.wast.common.utils.EnvUtils;

/**
 * use for jdk16+ scan token
 *
 * @Date 2024/6/14 19:08
 * @Created by wangyc
 */
public class JSONUtil {

    JSONUtil() {}

    final static long getQuoteOrBackslashMask(char[] buf, int offset, long quoteMask) {
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
                if (EnvUtils.BIG_ENDIAN) {
                    result = Long.reverseBytes(result);
                }
                int r32 = (int) result;
                if (r32 == 0x80008000) {
                    offset += (result == 0x0000800080008000L ? 3 : 2);
                } else if (r32 == 0x00008000) {
                    ++offset;
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
        if (EnvUtils.BIG_ENDIAN) {
            result = Long.reverseBytes(result);
        }
        int offset = 0;
        int r32 = (int) result;
        if (r32 == 0x80808080) {
            offset += 4;
            r32 = (int) (result >> 32);
        }
        switch (r32) {
            case 0x00808080:
                return offset + 3;
            case 0x00008080:
            case 0x80008080:
                return offset + 2;
            case 0x00000080:
            case 0x00800080:
            case 0x80000080:
            case 0x80800080:
                return offset + 1;
            default:
                return offset;
        }
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

    static final boolean isNoneEscaped8Bytes(byte[] buf, int offset) {
        long value = JSONUnsafe.UNSAFE.getLong(buf, JSONUnsafe.BYTE_ARRAY_OFFSET + offset);
        return ((value + 0x6060606060606060L) & ((value ^ 0xDDDDDDDDDDDDDDDDL) + 0x0101010101010101L) & ((value ^ 0xA3A3A3A3A3A3A3A3L) + 0x0101010101010101L) & 0x8080808080808080L) == 0x8080808080808080L;
    }

    /**
     * <p> 尽量查找没有需要转义的位置(判定: "或者'\\'或者小于32)</p>
     * <p> 内部调用 </p>
     *
     * @param buf (len >= 32)
     * @param offset = 0
     * @return the first position（8n） of escaped
     */
    public int toNoEscapeOffset(byte[] buf, int offset) {
        if (isNoneEscaped8Bytes(buf, offset)
                && isNoneEscaped8Bytes(buf, offset = offset + 8)
                && isNoneEscaped8Bytes(buf, offset = offset + 8)
                && isNoneEscaped8Bytes(buf, offset = offset + 8)
        ) {
            offset += 8;
            final int limit = buf.length - 32;
            while (offset <= limit
                    && isNoneEscaped8Bytes(buf, offset)
                    && isNoneEscaped8Bytes(buf, offset = offset + 8)
                    && isNoneEscaped8Bytes(buf, offset = offset + 8)
                    && isNoneEscaped8Bytes(buf, offset = offset + 8)) {
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
     * @param buf （buf.len >= 64）
     * @param offset = 0
     * @return the first position（8n） of escaped
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

//    boolean isNoEscapeMemory32Bits(byte[] bytes, int offset) {
//        return NO_ESCAPE_FLAGS[bytes[offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff];
//    }
//
//    boolean isNoEscapeMemory64Bits(byte[] bytes, int offset) {
//        return NO_ESCAPE_FLAGS[bytes[offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                ;
//    }
//
//    int checkNeedEscapeIndex128Bits(byte[] bytes, int offset) {
//        boolean flag =  NO_ESCAPE_FLAGS[bytes[offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                        && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff];
//        return flag ? -1 : offset;
//    }
}
