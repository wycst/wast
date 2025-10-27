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

//    final static long getQuoteOrBackslashMask(char[] buf, long offset, long quoteMask) {
//        long v64 = JSONUnsafe.UNSAFE.getLong(buf, JSONUnsafe.CHAR_ARRAY_OFFSET + (offset << 1));
//        return (((v64 ^ quoteMask) - 0x0001000100010001L) | ((v64 ^ 0x005C005C005C005CL) - 0x0001000100010001L)) & 0x8000800080008000L;
//    }
//
//    final static long getQuoteOrBackslashMask(long v64, long quoteMask) {
//        return (((v64 ^ quoteMask) - 0x0001000100010001L) | ((v64 ^ 0x005C005C005C005CL) - 0x0001000100010001L)) & 0x8000800080008000L;
//    }

    final static int offsetChars(long result) {
        if (EnvUtils.LITTLE_ENDIAN) {
            return Long.numberOfTrailingZeros(result) >> 4;
        } else {
            return Long.numberOfLeadingZeros(result) >> 4;
        }
    }

    /**
     * 模拟向量处理256位（一次方法调用，4次检测16个字符）
     * 查找引号或者反斜杠
     *
     * @param buf
     * @param offset
     * @param quoteMask
     * @return 与向量api Mask一致， 如果返回0 - 15 说明查找成功，否则返回16
     */
    final static int indexOfQuoteOrBackslashVector256(char[] buf, int offset, long quoteMask) {
        /*JSONUnsafe.CHAR_ARRAY_OFFSET + ((long) offset << 1)*/
        long v = JSONMemoryHandle.JSON_ENDIAN.getLong(buf, offset);
        long result = (((v ^ quoteMask) - 0x0001000100010001L) | ((v ^ 0x005C005C005C005CL) - 0x0001000100010001L)) & 0x8000800080008000L;
        if (result != 0) {
            return offsetChars(result);
        }
        v = JSONMemoryHandle.JSON_ENDIAN.getLong(buf, offset + 4);
        result = (((v ^ quoteMask) - 0x0001000100010001L) | ((v ^ 0x005C005C005C005CL) - 0x0001000100010001L)) & 0x8000800080008000L;
        if (result != 0) {
            return 4 + offsetChars(result);
        }
        v = JSONMemoryHandle.JSON_ENDIAN.getLong(buf, offset + 8);
        result = (((v ^ quoteMask) - 0x0001000100010001L) | ((v ^ 0x005C005C005C005CL) - 0x0001000100010001L)) & 0x8000800080008000L;
        if (result != 0) {
            return 8 + offsetChars(result);
        }
        v = JSONMemoryHandle.JSON_ENDIAN.getLong(buf, offset + 12);
        result = (((v ^ quoteMask) - 0x0001000100010001L) | ((v ^ 0x005C005C005C005CL) - 0x0001000100010001L)) & 0x8000800080008000L;
        if (result != 0) {
            return 12 + offsetChars(result);
        }
        return 16;
    }

    /**
     * 从字符数组的offset位置开始查找下一个引号(单引号或者双引号)或者转义符(\\)
     *
     * @param buf
     * @param offset
     * @param quote     引号
     * @param quoteMask
     * @return 从offset开始, 第一个quote或者'\\'的位置，如果找不到将抛出IndexOutOfBoundsException
     * @throws IndexOutOfBoundsException
     */
    public int ensureIndexOfQuoteOrBackslashChar(char[] buf, int offset, char quote, long quoteMask) {
        final int limit32 = buf.length - 32;
        if (offset <= limit32) {
            int firstPos;
            do {
                if ((firstPos = indexOfQuoteOrBackslashVector256(buf, offset, quoteMask)) == 16
                        && (firstPos = indexOfQuoteOrBackslashVector256(buf, offset += 16, quoteMask)) == 16
                ) {
                    offset += 16;
                    continue;
                }
                offset += firstPos;
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
        long v64 = JSONMemoryHandle.JSON_ENDIAN.getLong(buf, offset); // 92 5cc
        return (((v64 ^ quoteMask) - 0x0101010101010101L) | ((v64 ^ 0x5C5C5C5C5C5C5C5CL) - 0x0101010101010101L)) & 0x8080808080808080L;
    }

    // check: quote or \\(0x5C) or UTF8 begin
    final static long getQuoteOrBackslashOrUTF8Mask(long value, long quoteMask) {
        return (((value ^ quoteMask) - 0x0101010101010101L) | ((value ^ 0x5C5C5C5C5C5C5C5CL) - 0x0101010101010101L)) & 0x8080808080808080L;
    }

    /**
     * 模拟向量处理256位（一次方法调用，4次检测32个字节）
     * 查找引号或者反斜杠或者负字节；
     *
     * @param buf
     * @param offset
     * @param quoteMask
     * @return 与向量api Mask一致， 如果返回0 - 31说明查找成功，否则返回32
     */
    final static int indexOfQuoteOrBackslashOrUTF8Vector256(byte[] buf, int offset, long quoteMask) {
        /*JSONUnsafe.BYTE_ARRAY_OFFSET +*/
        long v = JSONMemoryHandle.JSON_ENDIAN.getLong(buf, offset);
        long result = (((v ^ quoteMask) - 0x0101010101010101L) | ((v ^ 0x5C5C5C5C5C5C5C5CL) - 0x0101010101010101L)) & 0x8080808080808080L;
        if (result != 0) {
            return offsetTokenBytes(result);
        }
        v = JSONMemoryHandle.JSON_ENDIAN.getLong(buf, offset + 8);
        result = (((v ^ quoteMask) - 0x0101010101010101L) | ((v ^ 0x5C5C5C5C5C5C5C5CL) - 0x0101010101010101L)) & 0x8080808080808080L;
        if (result != 0) {
            return 8 + offsetTokenBytes(result);
        }
        v = JSONMemoryHandle.JSON_ENDIAN.getLong(buf, offset + 16);
        result = (((v ^ quoteMask) - 0x0101010101010101L) | ((v ^ 0x5C5C5C5C5C5C5C5CL) - 0x0101010101010101L)) & 0x8080808080808080L;
        if (result != 0) {
            return 16 + offsetTokenBytes(result);
        }
        v = JSONMemoryHandle.JSON_ENDIAN.getLong(buf, offset + 24);
        result = (((v ^ quoteMask) - 0x0101010101010101L) | ((v ^ 0x5C5C5C5C5C5C5C5CL) - 0x0101010101010101L)) & 0x8080808080808080L;
        if (result != 0) {
            return 24 + offsetTokenBytes(result);
        }
        return 32;
    }

    final static int offsetTokenBytes(long result) {
        if (EnvUtils.LITTLE_ENDIAN) {
            return Long.numberOfTrailingZeros(result) >> 3;
        } else {
            return Long.numberOfLeadingZeros(result) >> 3;
        }
    }

    /**
     * <p> 确保从字节数组的offset位置开始查找下一个引号(单引号或者双引号)或者转义符(\\)或者负字节（utf-8编码，高位一定以11开头，以10开头的可以不考虑）；</p>
     * <p> 如果没有找到则抛出数组越界异常；</p>
     *
     * @param buf
     * @param offset
     * @param quote     引号(单引号或者双引号)
     * @param quoteMask 8个quote字节组成的long值
     * @return 首次引号或者转义符或者负字节位置
     * @throws IndexOutOfBoundsException if not found
     */
    public int ensureIndexOfQuoteOrBackslashOrUTF8Byte(byte[] buf, int offset, int quote, long quoteMask) {
        final int limit32 = buf.length - 32;
        long result = 0;
        int firstPos;
        boolean limitFlag;
        if (offset <= limit32) {
            if ((firstPos = indexOfQuoteOrBackslashOrUTF8Vector256(buf, offset, quoteMask)) != 32) {
                return offset + firstPos;
            } else {
                while ((limitFlag = (offset += 32) <= limit32) && (firstPos = indexOfQuoteOrBackslashOrUTF8Vector256(buf, offset, quoteMask)) == 32)
                    ;
                if (limitFlag) {
                    return offset + firstPos;
                }
            }
        }
        final int limit8 = buf.length - 8;
        while ((limitFlag = offset <= limit8) && (result = getQuoteOrBackslashOrUTF8Mask(buf, offset, quoteMask)) == 0) {
            offset += 8;
        }
        if (limitFlag) {
            return offset + offsetTokenBytes(result);
        }
        byte c;
        while ((c = buf[offset]) != quote && c != '\\' && c >= 0) {
            ++offset;
        }
        return offset;
    }

//    /**
//     * <p> 确保从字节数组的offset位置开始查找下一个引号(单引号或者双引号)或者转义符(\\)或者负字节（utf-8编码，高位一定以11开头，以10开头的可以不考虑）；</p>
//     * <p> 如果没有找到则抛出数组越界异常；</p>
//     *
//     * @param buf
//     * @param offset
//     * @param quote     引号(单引号或者双引号)
//     * @param quoteMask 8个quote字节组成的long值
//     * @return 首次引号或者转义符或者负字节位置
//     * @throws IndexOutOfBoundsException if not found
//     */
//    public final static int ensureIndexOfQuoteOrBackslashOrUTF8Vector256(byte[] buf, int offset, int quote, long quoteMask) {
//        final int limit32 = buf.length - 32;
//        long result = 0;
//        boolean limitFlag;
//        int firstPos;
//        if(offset <= limit32) {
//            if((firstPos = indexOfQuoteOrBackslashOrUTF8Vector256(buf, offset, quoteMask)) != 32) {
//                return offset + firstPos;
//            } else {
//                offset += 32;
//                while ((limitFlag = offset <= limit32)
//                        && (firstPos = indexOfQuoteOrBackslashOrUTF8Vector256(buf, offset, quoteMask)) == 32
//                ) {
//                    offset += 32;
//                }
//                if (limitFlag) {
//                    return offset + firstPos;
//                } else {
//                    final int limit8 = buf.length - 8;
//                    while ((limitFlag = offset <= limit8) && (result = getQuoteOrBackslashOrUTF8Mask(buf, offset, quoteMask)) == 0) {
//                        offset += 8;
//                    }
//                    if (limitFlag) {
//                        return offset + offsetTokenBytes(result);
//                    }
//                }
//            }
//        } else {
//            final int limit8 = buf.length - 8;
//            while ((limitFlag = offset <= limit8) && (result = getQuoteOrBackslashOrUTF8Mask(buf, offset, quoteMask)) == 0) {
//                offset += 8;
//            }
//            if (limitFlag) {
//                return offset + offsetTokenBytes(result);
//            }
//        }
//        byte c;
//        while ((c = buf[offset]) != quote && c != '\\' && c >= 0) {
//            ++offset;
//        }
//        return offset;
//    }

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

    /**
     * <p> 尽量查找没有需要转义的位置(判定: "或者'\\'或者小于32)</p>
     * <p> 内部调用 </p>
     *
     * @param buf    (len >= 32)
     * @param offset = 0
     * @return the first position（8n） of escaped
     */
    public int toNoEscapeOffset(byte[] buf, int offset) {
        if (JSONGeneral.isNoneEscaped16Bytes(JSONMemoryHandle.getLong(buf, offset), JSONMemoryHandle.getLong(buf, offset + 8))
                && JSONGeneral.isNoneEscaped16Bytes(JSONMemoryHandle.getLong(buf, offset = offset + 16), JSONMemoryHandle.getLong(buf, offset + 8))) {
            offset += 16;
            final int limit = buf.length - 32;
            while (offset <= limit
                    && JSONGeneral.isNoneEscaped8Bytes(JSONMemoryHandle.getLong(buf, offset))
                    && JSONGeneral.isNoneEscaped8Bytes(JSONMemoryHandle.getLong(buf, offset = offset + 8))
                    && JSONGeneral.isNoneEscaped8Bytes(JSONMemoryHandle.getLong(buf, offset = offset + 8))
                    && JSONGeneral.isNoneEscaped8Bytes(JSONMemoryHandle.getLong(buf, offset = offset + 8))) {
                offset += 8;
            }
        }
        return offset;
    }

    static final boolean isNoneEscaped4Chars(char[] buf, int offset) {
        long value = JSONMemoryHandle.getLong(buf, offset);
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

    public boolean isSupportVectorWellTest() {
        return false;
    }
}
