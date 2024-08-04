package io.github.wycst.wast.common.utils;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

/**
 * @author wangyc
 */
public final class Base64Utils {

    // 63
    private static final char[] BASE64_CHARS = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };

    // 128
    private static final int[] BASE64_VALUES = new int[]{
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -2, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1
    };

    public static byte[] encode(byte[] src) {
        int tlen = ((src.length + 2) / 3) << 2;
        byte[] dst = new byte[tlen];
        encode(src, dst, 0);
        return dst;
    }

    public static String encodeToString(byte[] src) {
        int tlen = ((src.length + 2) / 3) << 2;
        if (EnvUtils.JDK_9_PLUS) {
            byte[] dst = new byte[tlen];
            encode(src, dst, 0);
            return UnsafeHelper.getAsciiString(dst);
        } else {
            char[] dst = new char[tlen];
            encode(src, dst, 0);
            return UnsafeHelper.getString(dst);
        }
    }

    public static int encode(byte[] src, byte[] dst, int offset) {
        int n = src.length / 3, n3 = n * 3, rem = src.length - n3, begin = offset, srcOff = 0;
        for (int i = 0; i < n; ++i) {
            int b1 = src[srcOff++] & 0xff, b2 = src[srcOff++] & 0xff, b3 = src[srcOff++] & 0xff;
            int bits = b1 << 16 | b2 << 8 | b3;
            dst[offset++] = (byte) BASE64_CHARS[b1 >> 2];
            dst[offset++] = (byte) BASE64_CHARS[(bits >> 12) & 0x3f];
            dst[offset++] = (byte) BASE64_CHARS[(bits >> 6) & 0x3f];
            dst[offset++] = (byte) BASE64_CHARS[bits & 0x3f];
        }
        if (rem == 1) {
            int b = src[n3] & 0xff;
            dst[offset++] = (byte) BASE64_CHARS[b >> 2];
            dst[offset++] = (byte) BASE64_CHARS[(b & 3) << 4];
            dst[offset++] = '=';
            dst[offset++] = '=';
        } else if (rem == 2) {
            int b1 = src[n3] & 0xff, b2 = src[n3 + 1] & 0xff;
            dst[offset++] = (byte) BASE64_CHARS[b1 >> 2];
            dst[offset++] = (byte) BASE64_CHARS[(b1 & 3) << 4 | b2 >> 4];
            dst[offset++] = (byte) BASE64_CHARS[(b2 & 0xf) << 2];
            dst[offset++] = '=';
        }
        return offset - begin;
    }

    public static int encode(byte[] src, char[] dst, int offset) {
        int n = src.length / 3, n3 = n * 3, rem = src.length - n3, begin = offset, srcOff = 0;
        for (int i = 0; i < n; ++i) {
            int b1 = src[srcOff++] & 0xff, b2 = src[srcOff++] & 0xff, b3 = src[srcOff++] & 0xff;
            int bits = b1 << 16 | b2 << 8 | b3;
            dst[offset++] = BASE64_CHARS[b1 >> 2];
            dst[offset++] = BASE64_CHARS[(bits >> 12) & 0x3f];
            dst[offset++] = BASE64_CHARS[(bits >> 6) & 0x3f];
            dst[offset++] = BASE64_CHARS[bits & 0x3f];
        }
        if (rem == 1) {
            int b = src[n3] & 0xff;
            dst[offset++] = BASE64_CHARS[b >> 2];
            dst[offset++] = BASE64_CHARS[(b & 3) << 4];
            dst[offset++] = '=';
            dst[offset++] = '=';
        } else if (rem == 2) {
            int b1 = src[n3] & 0xff, b2 = src[n3 + 1] & 0xff;
            dst[offset++] = BASE64_CHARS[b1 >> 2];
            dst[offset++] = BASE64_CHARS[(b1 & 3) << 4 | b2 >> 4];
            dst[offset++] = BASE64_CHARS[(b2 & 0xf) << 2];
            dst[offset++] = '=';
        }
        return offset - begin;
    }

    public static byte[] decode(String src) {
        if (EnvUtils.JDK_9_PLUS) {
            byte[] buf = (byte[]) UnsafeHelper.getStringValue(src);
            return decode(buf, 0, buf.length);
        } else {
            char[] buf = (char[]) UnsafeHelper.getStringValue(src);
            return decode(buf, 0, buf.length);
        }
    }

    public static byte[] decode(byte[] src) {
        return decode(src, 0, src.length);
    }

    public static byte[] decode(byte[] src, int offset, int len) {
        if ((len & 3) > 0) {
            throw new IllegalArgumentException("The length error of base64 should be an integer multiple of 4");
        }
        try {
            int n = len >> 2, tlen = n * 3, srcOff = offset;
            int endOffset = offset + len;
            byte[] dst;
            if (src[--endOffset] == '=') {
                --n;
                --tlen;
                int v;
                if ((v = src[--endOffset]) == '=') {
                    // padding 2: (6 + 2) -> 6 + 2 -> 8 * 1
                    --tlen;
                    dst = new byte[tlen];
                    int v2 = BASE64_VALUES[src[--endOffset]], v1 = BASE64_VALUES[src[--endOffset]];
                    if (v1 > -1 && v2 > -1) {
                        dst[tlen - 1] = (byte) (v1 << 2 | (v2 >> 4));
                    } else {
                        throw new IllegalArgumentException("Base64 data error: " + new String(src, offset, len));
                    }
                } else {
                    // padding 1: (6 + 6 + 4) -> 16(6 + 6 + 4) -> 8 * 2
                    dst = new byte[tlen];
                    int v3 = BASE64_VALUES[v], v2 = BASE64_VALUES[src[--endOffset]], v1 = BASE64_VALUES[src[--endOffset]];
                    if (v1 > -1 && v2 > -1 && v3 > -1) {
                        int bits = v1 << 10 | v2 << 4 | (v3 >> 2);
                        dst[tlen - 2] = (byte) (bits >> 8);
                        dst[tlen - 1] = (byte) bits;
                    } else {
                        throw new IllegalArgumentException("Base64 data error: " + new String(src, offset, len));
                    }
                }
            } else {
                dst = new byte[tlen];
            }
            int dstOff = 0;
            for (int i = 0; i < n; ++i) {
                // 4 * 6 bits -> 3 * 8 bits
                int v1 = BASE64_VALUES[src[srcOff++]],
                        v2 = BASE64_VALUES[src[srcOff++]],
                        v3 = BASE64_VALUES[src[srcOff++]],
                        v4 = BASE64_VALUES[src[srcOff++]];
                if (v1 > -1 && v2 > -1 && v3 > -1 && v4 > -1) {
                    int bits = v1 << 18 | v2 << 12 | v3 << 6 | v4;
                    dst[dstOff++] = (byte) (bits >> 16);
                    dst[dstOff++] = (byte) (bits >> 8);
                    dst[dstOff++] = (byte) bits;
                } else {
                    throw new IllegalArgumentException("Base64 data error: " + new String(src, offset, len));
                }
            }
            return dst;
        } catch (Throwable throwable) {
            throw new IllegalArgumentException("Base64 data error: " + new String(src, offset, len));
        }
    }

    public static byte[] decode(char[] src, int offset, int len) {
        if ((len & 3) > 0) {
            throw new IllegalArgumentException("The length error of base64 should be an integer multiple of 4");
        }
        try {
            int n = len >> 2, tlen = n * 3, srcOff = offset;
            int endOffset = offset + len;
            byte[] dst;
            if (src[--endOffset] == '=') {
                --n;
                --tlen;
                int v;
                if ((v = src[--endOffset]) == '=') {
                    // padding 2: (6 + 2) -> 6 + 2 -> 8 * 1
                    --tlen;
                    dst = new byte[tlen];
                    int v2 = BASE64_VALUES[src[--endOffset]], v1 = BASE64_VALUES[src[--endOffset]];
                    if (v1 > -1 && v2 > -1) {
                        dst[tlen - 1] = (byte) (v1 << 2 | (v2 >> 4));
                    } else {
                        throw new IllegalArgumentException("Base64 data error: " + new String(src, offset, len));
                    }
                } else {
                    // padding 1: (6 + 6 + 4) -> 16(6 + 6 + 4) -> 8 * 2
                    dst = new byte[tlen];
                    int v3 = BASE64_VALUES[v], v2 = BASE64_VALUES[src[--endOffset]], v1 = BASE64_VALUES[src[--endOffset]];
                    if (v3 > -1 && v2 > -1 && v1 > -1) {
                        int bits = v1 << 10 | v2 << 4 | (v3 >> 2);
                        dst[tlen - 2] = (byte) (bits >> 8);
                        dst[tlen - 1] = (byte) bits;
                    } else {
                        throw new IllegalArgumentException("Base64 data error: " + new String(src, offset, len));
                    }
                }
            } else {
                dst = new byte[tlen];
            }
            int dstOff = 0;
            for (int i = 0; i < n; ++i) {
                // 4 * 6 bits -> 3 * 8 bits
                int v1 = BASE64_VALUES[src[srcOff++]],
                        v2 = BASE64_VALUES[src[srcOff++]],
                        v3 = BASE64_VALUES[src[srcOff++]],
                        v4 = BASE64_VALUES[src[srcOff++]];
                if (v1 > -1 && v2 > -1 && v3 > -1 && v4 > -1) {
                    int bits = v1 << 18 | v2 << 12 | v3 << 6 | v4;
                    dst[dstOff++] = (byte) (bits >> 16);
                    dst[dstOff++] = (byte) (bits >> 8);
                    dst[dstOff++] = (byte) bits;
                } else {
                    throw new IllegalArgumentException("Base64 data error: " + new String(src, offset, len));
                }
            }
            return dst;
        } catch (Throwable throwable) {
            throw new IllegalArgumentException("Base64 data error: " + new String(src, offset, len));
        }
    }

}
