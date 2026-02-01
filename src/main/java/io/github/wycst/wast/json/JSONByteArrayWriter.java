package io.github.wycst.wast.json;

import io.github.wycst.wast.common.utils.Base64Utils;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.json.options.WriteOption;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

/**
 * <p> JSON ByteArray writer
 * <p> Optimization of single character out of bounds check
 *
 * @Author: wangy
 * @Date: 2023/12/22 21:48
 * @Description:
 * @see JSON#toJsonBytes(Object, Charset, WriteOption...)
 * @see JSON#writeJsonTo(Object, OutputStream, Charset, WriteOption...)
 */
class JSONByteArrayWriter extends JSONWriter {

    private final Charset charset;
    private final boolean utf8;
    // buff
    byte[] buf;

    // 长度
    int count;

    // EMPTY buf
    static final byte[] EMPTY_BUF = new byte[0];
    final static BufCache[] BYTE_BUF_CACHES = new BufCache[CACHE_COUNT];

    static {
        // init caches
        for (int i = 0; i < CACHE_COUNT; ++i) {
            BufCache bufCache = new BufCache();
            bufCache.index = i;
            if (i < INIT_CACHE_COUNT) {
                bufCache.cacheBytes = new byte[CACHE_BUFFER_SIZE];
            }
            BYTE_BUF_CACHES[i] = bufCache;
        }
    }

    JSONByteArrayWriter(Charset charset) {
        this.charset = charset == null ? Charset.defaultCharset() : charset;
        utf8 = charset == EnvUtils.CHARSET_UTF_8;
        // use pool
        BufCache bufCache = getByteBufCache(); // getOrReturnCache(null);
        if (bufCache != null) {
            buf = bufCache.cacheBytes;
            this.bufCache = bufCache;
        } else {
            buf = new byte[512];
        }
    }

    private static BufCache getByteBufCache() {
        int cacheIndex = THREAD_CACHE_INDEX.get();
        BufCache cache = BYTE_BUF_CACHES[cacheIndex];
        synchronized (cache) {
            if (cache.inUse) return null;
            cache.inUse = true;
            if (cache.cacheBytes == null) {
                cache.cacheBytes = new byte[CACHE_BUFFER_SIZE];
            }
        }
        return cache;
    }

    /**
     * The current instance uses a cached instance
     */
    private BufCache bufCache;

    private static class BufCache {
        byte[] cacheBytes;
        boolean inUse;
        int index;
    }

    /**
     * There are no double byte characters in the JSON scenario
     *
     * @param c int specifying a character to be written
     */
    @Override
    public void write(int c) {
        ensureCapacity(1 + SECURITY_UNCHECK_SPACE);
        buf[count++] = (byte) c;
    }

    /**
     * Write special token characters to skip expansion detection
     *
     * <p> 5 special JSON tokens such as '{', '}', '[', ']', ',' or other single character </p>
     * <p> Ensure to reserve 3 additional locations for each expansion </p>
     * <p> Avoid writing consecutive token characters such as "{}" or "[]", or "[{}, {}]" </p>
     */
    @Override
    public void writeJSONToken(char c) {
        buf[count++] = (byte) c;
    }

    // no escape
    @Override
    public void write(char[] chars, int off, int len) {
        if (len == 0) return;
        ensureCapacity((len << 2) + SECURITY_UNCHECK_SPACE);
        int count = this.count;
        for (int i = off, end = off + len; i < end; ++i) {
            char ch = chars[i];
            if (ch < 0x80) {
                buf[count++] = (byte) ch;
            } else {
                if (utf8) {
                    if (ch <= 0x7FF) {
                        // 2字节
                        buf[count++] = (byte) (ch >> 6 | 0xC0);
                        buf[count++] = (byte) (ch & 0x3F | 0x80);
                        continue;
                    }
                    int code = Character.codePointAt(chars, i);
                    if (code <= 0xFFFF) {
                        // 3字节
                        buf[count++] = (byte) (code >> 12 | 0xE0);
                        buf[count++] = (byte) ((code >> 6) & 0x3F | 0x80);
                        buf[count++] = (byte) (code & 0x3F | 0x80);
                        continue;
                    }
                }
                count = encodeCharBuffer(CharBuffer.wrap(chars, i, end - i), count);
                break;
            }
        }
        this.count = count;
    }

    byte[] ensureCapacity(int increment) {
        return expandCapacity(count + increment);
    }

    byte[] expandCapacity(long minCap) {
        if (minCap > buf.length) {
            if (minCap >= MAX_ALLOW_ALLOCATION_SIZE) {
                throw new UnsupportedOperationException("Expansion failed, data is too large : " + minCap);
            }
            long newCap = Math.min((minCap >> 1) * 3, MAX_ALLOW_ALLOCATION_SIZE);
            buf = Arrays.copyOf(buf, (int) newCap);
        } else {
            if (minCap < 0) {
                throw new UnsupportedOperationException("The data length is too large and has overflowed");
            }
        }
        return buf;
    }

    // no escape
    @Override
    public void write(String str, int off, int len) {
        if (len == 0) return;
        Object value = JSONMemoryHandle.getStringValue(str.toString());
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) value;
            if (bytes.length == str.length()) {
                ensureCapacity(len + SECURITY_UNCHECK_SPACE);
                // LATIN
                System.arraycopy(bytes, off, buf, count, len);
                count += len;
            } else {
                ensureCapacity(len * 4 + SECURITY_UNCHECK_SPACE);
                // UTF16 need compress
                int count = this.count;
                for (int i = off, end = off + len; i < end; ++i) {
                    char c = str.charAt(i);
                    if (c < 0x80) {
                        buf[count++] = (byte) c;
                    } else {
                        if (utf8) {
                            if (c <= 0x7FF) {
                                // 2字节
                                buf[count++] = (byte) (c >> 6 | 0xC0);
                                buf[count++] = (byte) (c & 0x3F | 0x80);
                                continue;
                            }
                            int code = Character.codePointAt(str, i);
                            if (code <= 0xFFFF) {
                                // 3字节
                                buf[count++] = (byte) (code >> 12 | 0xE0);
                                buf[count++] = (byte) ((code >> 6) & 0x3F | 0x80);
                                buf[count++] = (byte) (code & 0x3F | 0x80);
                                continue;
                            }
                        }
                        count = encodeCharBuffer(CharBuffer.wrap(str, i, end), count);
                        break;
                    }
                }
                this.count = count;
            }
        } else {
            char[] chars = (char[]) value;
            write(chars, off, len);
        }
    }

//    protected int encodeSingleChar(char c, int offset) {
//        if (utf8) {
//            return encodeUTF8(c, offset);
//        } else {
//            ByteBuffer buffer = charset.encode(CharBuffer.wrap(new char[]{c}));
//            int remaining = buffer.remaining();
//            byte[] arr = buffer.array();
//            for (int j = 0; j < remaining; ++j) {
//                buf[offset++] = arr[j];
//            }
//            return offset;
//        }
//    }
//    protected final int encodeUTF8(char c, int offset) {
//        // utf-8 code
//        // 1 0000 0000-0000 007F | 0xxxxxxx
//        // 2 0000 0080-0000 07FF | 110xxxxx 10xxxxxx
//        // 3 0000 0800-0000 FFFF | 1110xxxx 10xxxxxx 10xxxxxx
//        // 4 0001 0000-0010 FFFF | 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
//        if (c <= 0x7FF) {
//            int h = c >> 6, l = c & 0x3F;
//            buf[offset++] = (byte) (0xC0 | h);
//            buf[offset++] = (byte) (0x80 | l);
//        } else {
//            // 3字节 (char字符最大0xFFFF占16位,编码不考虑4个字节的范围场景)
//            int h = c >> 12, m = (c >> 6) & 0x3F, l = c & 0x3F;
//            buf[offset++] = (byte) (0xE0 | h);
//            buf[offset++] = (byte) (0x80 | m);
//            buf[offset++] = (byte) (0x80 | l);
//        }
//        return offset;
//    }

    @Override
    public final void flush() throws IOException {
    }

    @Override
    public final void close() throws IOException {
    }

    protected StringBuffer toStringBuffer() {
        throw new UnsupportedOperationException();
    }

    protected StringBuilder toStringBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected byte[] toBytes() {
        return Arrays.copyOf(buf, count);
    }

    @Override
    protected void toOutputStream(OutputStream os) throws IOException {
        os.write(buf, 0, count);
        os.flush();
    }

    @Override
    protected boolean endsWith(int c) {
        return count != 0 && buf[count - 1] == c;
    }

    @Override
    public String toString() {
        return new String(buf, 0, count);
    }

    public int size() {
        return count;
    }

    void reset() {
        clear();
        clearCache();
        buf = EMPTY_BUF;
    }

    public void clear() {
        count = 0;
    }

    @Override
    public void writeShortChars(char[] chars, int offset, int len) {
        write(chars, offset, len);
    }

    /**
     * run on jdk8 output is bytes
     */
    @Override
    public void writeJSONChars(char[] chars) throws IOException {
        int len = chars.length;
        ensureCapacity(len * 6 + (2 + SECURITY_UNCHECK_SPACE));
        int count = this.count;
        buf[count++] = '"';
        for (int i = 0; i < len; ++i) {
            char ch = chars[i];
            String escapeStr;
            if ((ch > '"' && ch != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[ch]) == null) {
                if (ch < 0x80) {
                    buf[count++] = (byte) ch;
                } else {
                    if (utf8) {
                        if (ch <= 0x7FF) {
                            // 2字节
                            buf[count++] = (byte) (ch >> 6 | 0xC0);
                            buf[count++] = (byte) (ch & 0x3F | 0x80);
                            continue;
                        }
                        int code = Character.codePointAt(chars, i);
                        if (code <= 0xFFFF) {
                            // 3字节
                            buf[count++] = (byte) (code >> 12 | 0xE0);
                            buf[count++] = (byte) ((code >> 6) & 0x3F | 0x80);
                            buf[count++] = (byte) (code & 0x3F | 0x80);
                            continue;
                        }
                    }
                    // utf8 4字节或者其他编码使用charset处理
                    int k = i + 1;
                    while (k < len && chars[k] >= 0x80) {
                        ++k;
                    }
                    count = encodeCharBuffer(CharBuffer.wrap(chars, i, k - i), count);
                    i = k - 1;
                }
                continue;
            }
            int escapesLen = escapeStr.length();
            for (int j = 0; j < escapesLen; ++j) {
                buf[count++] = (byte) escapeStr.charAt(j);
            }
        }
        buf[count++] = '"';
        this.count = count;
    }

    final int encodeCharBuffer(CharBuffer charBuffer, int offset) {
        ByteBuffer buffer = charset.encode(charBuffer);
        int remaining = buffer.remaining();
        byte[] arr = buffer.array();
        for (int j = 0; j < remaining; ++j) {
            buf[offset++] = arr[j];
        }
        return offset;
    }

    final static int escapeBytesToBytes(byte[] source, int sourceOff, byte[] target, int targetOff) {
        for (int i = sourceOff, len = source.length; i < len; ++i) {
            byte b;
            int index;
            if (isNoEscape(index = (b = source[i]) & 0xFF)) {
                target[targetOff++] = b;
            } else {
                String escapeStr = JSONGeneral.ESCAPE_VALUES[index];
                int escapesLen = escapeStr.length();
                for (int j = 0; j < escapesLen; ++j) {
                    target[targetOff++] = (byte) escapeStr.charAt(j);
                }
            }
        }
        return targetOff;
    }

    public void writeLatinJSONString(String value, byte[] bytes) throws IOException {
        int len = bytes.length;
        ensureCapacity(len + (2 + SECURITY_UNCHECK_SPACE));
        int count = this.count;
        buf[count++] = '"';
        if (len < 32) {
            int i = 0;
            do {
                if (len > 15) {
                    long v1 = JSONMemoryHandle.getLong(bytes, 0), v2 = JSONMemoryHandle.getLong(bytes, 8);
                    if (JSONGeneral.isNoneEscaped16Bytes(v1, v2)) {
                        JSONMemoryHandle.putLong(buf, count, v1);
                        JSONMemoryHandle.putLong(buf, count + 8, v2);
                        count += 16;
                        i = 16;
                    } else {
                        count = escapeBytesToBytes(bytes, i, buf, count);
                        break;
                    }
                }
                if (i <= len - 8) { // i <= len - 8
                    long value64;
                    if (JSONGeneral.isNoneEscaped8Bytes(value64 = JSONMemoryHandle.getLong(bytes, i))) {
                        JSONMemoryHandle.putLong(buf, count, value64);
                        count += 8;
                        i += 8;
                    } else {
                        count = escapeBytesToBytes(bytes, i, buf, count);
                        break;
                    }
                }
                if (i <= len - 4) {
                    int value32 = JSONMemoryHandle.getInt(bytes, i);
                    if (JSONGeneral.isNoneEscaped4Bytes(value32)) {
                        JSONMemoryHandle.putInt(buf, count, value32);
                        count += 4;
                        i += 4;
                    } else {
                        count = escapeBytesToBytes(bytes, i, buf, count);
                        break;
                    }
                }
                if (i <= len - 2) {
                    short value16 = JSONMemoryHandle.getShort(bytes, i);
                    if (JSONGeneral.isNoneEscaped2Bytes(value16)) {
                        JSONMemoryHandle.putShort(buf, count, value16);
                        count += 2;
                        i += 2;
                    } else {
                        count = escapeBytesToBytes(bytes, i, buf, count);
                        break;
                    }
                }
                if (i < len) { // i <= len - 1
                    byte b;
                    int index;
                    if (JSONGeneral.NO_ESCAPE_FLAGS[index = (b = bytes[i]) & 0xFF]) {
                        buf[count++] = b;
                    } else {
                        String escapeStr = JSONGeneral.ESCAPE_VALUES[index];
                        int escapesLen = escapeStr.length();
                        for (int j = 0; j < escapesLen; ++j) {
                            buf[count++] = (byte) escapeStr.charAt(j);
                        }
                    }
                }
            } while (false);
        } else {
            // len >= 32
            int beginIndex = 0, i = JSONGeneral.JSON_UTIL.toNoEscapeOffset(bytes, 0);
            for (; i < len; ++i) {
                byte b = bytes[i];
                String escapeStr;
                if ((escapeStr = JSONGeneral.ESCAPE_VALUES[b & 0xFF]) != null) {
                    int length = i - beginIndex;
                    expandCapacity(count + length + (5 + SECURITY_UNCHECK_SPACE));
                    if (length > 0) {
                        System.arraycopy(bytes, beginIndex, buf, count, length);
                        count += length;
                    }
                    int escapesLen = escapeStr.length();
                    for (int j = 0; j < escapesLen; ++j) {
                        buf[count++] = (byte) escapeStr.charAt(j);
                    }
                    beginIndex = i + 1;
                }
            }
            int length = len - beginIndex;
            if (length > 0) {
                System.arraycopy(bytes, beginIndex, buf, count, length);
                count += length;
            }
        }
        buf[count++] = '"';
        this.count = count;
    }

    public final void writeLatinString(String str) throws IOException {
        final int len = str.length();
        ensureCapacity(len + SECURITY_UNCHECK_SPACE);
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(str);
            if (len <= 32) {
                int count = this.count, remOff;
                if (len > 24) {
                    // 25-32
                    JSONMemoryHandle.putLong(buf, count, JSONMemoryHandle.getLong(bytes, 0));
                    JSONMemoryHandle.putLong(buf, count + 8, JSONMemoryHandle.getLong(bytes, 8));
                    JSONMemoryHandle.putLong(buf, count + 16, JSONMemoryHandle.getLong(bytes, 16));
                    JSONMemoryHandle.putLong(buf, count + (remOff = len - 8), JSONMemoryHandle.getLong(bytes, remOff));
                    this.count = count + len;
                    return;
                }
                if (len > 16) {
                    // 17-24
                    JSONMemoryHandle.putLong(buf, count, JSONMemoryHandle.getLong(bytes, 0));
                    JSONMemoryHandle.putLong(buf, count + 8, JSONMemoryHandle.getLong(bytes, 8));
                    JSONMemoryHandle.putLong(buf, count + (remOff = len - 8), JSONMemoryHandle.getLong(bytes, remOff));
                    this.count = count + len;
                    return;
                }
                if (len >= 8) {
                    // 8-16
                    JSONMemoryHandle.putLong(buf, count, JSONMemoryHandle.getLong(bytes, 0));
                    if (len > 8) {
                        JSONMemoryHandle.putLong(buf, count + (remOff = len - 8), JSONMemoryHandle.getLong(bytes, remOff));
                    }
                    this.count = count + len;
                    return;
                }
                if (len >= 4) {
                    JSONMemoryHandle.putInt(buf, count, JSONMemoryHandle.getInt(bytes, 0));
                    if (len > 4) {
                        JSONMemoryHandle.putInt(buf, count + (remOff = len - 4), JSONMemoryHandle.getInt(bytes, remOff));
                    }
                    this.count = count + len;
                    return;
                }
                if (len >= 2) {
                    JSONMemoryHandle.putShort(buf, count, JSONMemoryHandle.getShort(bytes, 0));
                    if (len == 3) {
                        buf[count + 2] = bytes[2];
                    }
                    this.count = count + len;
                    return;
                }
                if (len == 1) {
                    buf[this.count++] = bytes[0];
                }
            } else {
                System.arraycopy(bytes, 0, buf, count, len);
                this.count += len;
            }
        } else {
            char[] chars = (char[]) JSONMemoryHandle.getStringValue(str);
            int count = this.count;
            for (char c : chars) {
                buf[count++] = (byte) c;
            }
            this.count = count;
        }
    }

    public void writeUTF16JSONString(String value, byte[] bytes) throws IOException {
        int len = value.length();
        ensureCapacity(len * 6 + (2 + SECURITY_UNCHECK_SPACE));
        int count = this.count;
        buf[count++] = '"';
        for (int i = 0; i < len; ++i) {
            char c = value.charAt(i);
            String escapeStr;
            if ((c > '"' && c != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[c & 0xFF]) == null) {
                if (c < 0x80) {
                    buf[count++] = (byte) c;
                } else {
                    if (utf8) {
                        if (c <= 0x7FF) {
                            // 2字节
                            buf[count++] = (byte) (c >> 6 | 0xC0);
                            buf[count++] = (byte) (c & 0x3F | 0x80);
                            continue;
                        }
                        int code = Character.codePointAt(value, i);
                        if (code <= 0xFFFF) {
                            // 3字节
                            buf[count++] = (byte) (code >> 12 | 0xE0);
                            buf[count++] = (byte) ((code >> 6) & 0x3F | 0x80);
                            buf[count++] = (byte) (code & 0x3F | 0x80);
                            continue;
                        }
                    }
                    int k = i + 1;
                    while (k < len && value.charAt(k) >= 0x80) {
                        ++k;
                    }
                    count = encodeCharBuffer(CharBuffer.wrap(value, i, k), count);
                    i = k - 1;
                }
                continue;
            }
            for (int j = 0; j < escapeStr.length(); ++j) {
                buf[count++] = (byte) escapeStr.charAt(j);
            }
        }
        buf[count++] = '"';
        this.count = count;
    }

    @Override
    public void writeLong(long numValue) throws IOException {
        ensureCapacity(20 + SECURITY_UNCHECK_SPACE);
        if (numValue < 1) {
            if (numValue == 0) {
                buf[count++] = '0';
                return;
            } else if (numValue == Long.MIN_VALUE) {
                write("-9223372036854775808");
                return;
            }
            numValue = -numValue;
            buf[count++] = '-';
        }
        count += writeLong(numValue, buf, count);
    }

    @Override
    public void writeInt(int numValue) throws IOException {
        ensureCapacity(11 + SECURITY_UNCHECK_SPACE);
        if (numValue < 1) {
            if (numValue == 0) {
                buf[count++] = '0';
                return;
            } else if (numValue == Integer.MIN_VALUE) {
                write("-2147483648");
                return;
            }
            numValue = -numValue;
            buf[count++] = '-';
        }
        count += writeInteger(numValue, buf, count);
    }

//    @Override
//    protected final void writeCommaLongValues(long val1, long val2) throws IOException {
//        ensureCapacity(42 + SECURITY_UNCHECK_SPACE);
//        int off = count;
//        if (val1 < 0) {
//            if (val1 == Long.MIN_VALUE) {
//                write(",-9223372036854775808");
//                off = count;
//            } else {
//                val1 = -val1;
//                buf[off++] = ',';
//                buf[off++] = '-';
//                off += writeLong(val1, buf, off);
//            }
//        } else {
//            buf[off++] = ',';
//            off += writeLong(val1, buf, off);
//        }
//        if (val2 < 0) {
//            if (val2 == Long.MIN_VALUE) {
//                write(",-9223372036854775808");
//                off = count;
//            } else {
//                val2 = -val2;
//                buf[off++] = ',';
//                buf[off++] = '-';
//                off += writeLong(val2, buf, off);
//            }
//        } else {
//            buf[off++] = ',';
//            off += writeLong(val2, buf, off);
//        }
//        count = off;
//    }

    @Override
    public void writeUUID(UUID uuid) {
        ensureCapacity(38 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        off += writeUUID(uuid, buf, off);
        buf[off++] = '"';
        count = off;
    }

    @Override
    public void writeDouble(double numValue) {
        ensureCapacity(24 + SECURITY_UNCHECK_SPACE);
        count += writeDouble(numValue, buf, count);
    }

    @Override
    public void writeFloat(float numValue) {
        ensureCapacity(24 + SECURITY_UNCHECK_SPACE);
        count += writeFloat(numValue, buf, count);
    }

    @Override
    public void writeJSONLocalDateTime(int year, int month, int day, int hour, int minute, int second, int nano, String zoneId) throws IOException {
        ensureCapacity(36 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        if (year < 0) {
            buf[off++] = '-';
            year = -year;
        }
        if (year < 10000) {
            off += JSONMemoryHandle.putLong(buf, off, JSONMemoryHandle.JSON_ENDIAN.mergeYearAndMonth(year, month));
        } else {
            off += writeInteger(year, buf, off);
            off += JSONMemoryHandle.putInt(buf, off, mergeInt32(month, '-', '-'));
        }
        off += JSONMemoryHandle.putShort(buf, off, TWO_DIGITS_16_BITS[day]);
        buf[off++] = 'T';
        off += JSONMemoryHandle.putLong(buf, off, JSONMemoryHandle.JSON_ENDIAN.mergeHHMMSS(hour, minute, second));
        if (nano > 0) {
            off = writeNano(nano, off);
        }
        if (zoneId.length() == 1) {
            off += JSONMemoryHandle.putShort(buf, off, Z_QUOT_SHORT);
            count = off;
        } else {
            count = off;
            writeZoneId(zoneId);
            buf[count++] = '"';
        }
    }

    int writeNano(int nano, int off) {
        buf[off++] = '.';
        int top8 = (int) (nano * 1717986919L >> 34); // EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(nano, 0x199999999999999aL); // nano / 10;
        int remDigit = nano - top8 * 10;
        if (remDigit > 0) {
            // 8 + 1
            int top4 = (int) (top8 * 1759218605L >> 44); // EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(top8, 0x68db8bac710ccL); // top8 / 10000;
            int rem4 = top8 - top4 * 10000;
            off += JSONMemoryHandle.putLong(buf, off, mergeInt64(top4, rem4));
            buf[off++] = (byte) (remDigit + 48);
        } else {
            // 4 + 2 + 3
            int div1 = (int) (nano * 274877907L >> 38); // EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(nano, 0x4189374bc6a7f0L); // nano / 1000;
            int seg3 = nano - div1 * 1000;
            int seg1 = (int) (div1 * 1374389535L >> 37); // EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(div1, 0x28f5c28f5c28f5dL); // div1 / 100;
            int seg2 = div1 - 100 * seg1;
            off += JSONMemoryHandle.putInt(buf, off, FOUR_DIGITS_32_BITS[seg1]);
            if (seg3 > 0) {
                off += JSONMemoryHandle.putShort(buf, off, TWO_DIGITS_16_BITS[seg2]);
                int pos = --off;
                byte last = buf[pos];
                off += JSONMemoryHandle.putInt(buf, off, FOUR_DIGITS_32_BITS[seg3]);
                buf[pos] = last;
            } else {
                if (seg2 == 0 && (seg1 & 1) == 0 && seg1 % 5 == 0) {
                    --off;
                } else {
                    off += JSONMemoryHandle.putShort(buf, off, TWO_DIGITS_16_BITS[seg2]);
                }
            }
        }
        return off;
    }

    @Override
    public void writeJSONLocalDate(int year, int month, int day) {
        ensureCapacity(13 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        if (year < 0) {
            buf[off++] = '-';
            year = -year;
        }
        if (year < 10000) {
            // yyyy-MM-
            off += JSONMemoryHandle.putLong(buf, off, JSONMemoryHandle.JSON_ENDIAN.mergeYearAndMonth(year, month));
        } else {
            off += writeLong(year, buf, off);
            off += JSONMemoryHandle.putInt(buf, off, mergeInt32(month, '-', '-'));
        }
        off += JSONMemoryHandle.putShort(buf, off, TWO_DIGITS_16_BITS[day]);
        buf[off++] = '"';
        count = off;
    }

    @Override
    public void writeTime(int hourOfDay, int minute, int second) {
        ensureCapacity(10 + SECURITY_UNCHECK_SPACE);
        int off = count;
        off += JSONMemoryHandle.putLong(buf, off, JSONMemoryHandle.JSON_ENDIAN.mergeHHMMSS(hourOfDay, minute, second));
        count = off;
    }

    @Override
    public void writeJSONTimeWithNano(int hourOfDay, int minute, int second, int nano) {
        ensureCapacity(22 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        off += JSONMemoryHandle.putLong(buf, off, JSONMemoryHandle.JSON_ENDIAN.mergeHHMMSS(hourOfDay, minute, second));
        if (nano > 0) {
            off = writeNano(nano, off);
        }
        buf[off++] = '"';
        count = off;
    }

    /**
     * 序列化java.time.Period类
     */
    public void writeJSONPeriod(int years, int months, int days) throws IOException {
        ensureCapacity(24 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        buf[off++] = 'P';
        if (years == 0 && months == 0 && days == 0) {
            buf[off++] = '0';
            buf[off++] = 'D';
            buf[off++] = '"';
            count = off;
            return;
        }
        if (years != 0) {
            if (years < 0) {
                buf[off++] = '-';
                years = -years;
            }
            off += writeInteger(years, buf, off);
            buf[off++] = 'Y';
        }
        if (months != 0) {
            if (months < 0) {
                buf[off++] = '-';
                months = -months;
            }
            off += writeInteger(months, buf, off);
            buf[off++] = 'M';
        }
        if (days != 0) {
            if (days < 0) {
                buf[off++] = '-';
                days = -days;
            }
            off += writeInteger(days, buf, off);
            buf[off++] = 'D';
        }
        buf[off++] = '"';
        count = off;
    }

    @Override
    public void writeJSONDuration(long seconds, int nano) {
        ensureCapacity(24 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        if (seconds == 0 && nano == 0) {
            off += JSONMemoryHandle.putInt(buf, off, JSONGeneral.DURATION_ZERO_INT); // PT0S
            buf[off++] = '"';
            count = off;
            return;
        }
        off += JSONMemoryHandle.putShort(buf, off, JSONGeneral.DURATION_PT_SHORT); // PT
        final boolean negative = seconds < 0;
        if (negative) {
            seconds = -seconds;
        }
        long hour = seconds / 3600; // 暂不优化: seconds < 5146971002711999L ? EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(seconds, 0x123456789abce0L) : seconds / 3600 ( // ((2^64 - 3599 * 0x123456789abce0L) / 3584) * 3600 + 3599 * 2 + 1 -> 5146971002711999L)
        if (hour > 0) {
            if (negative) {
                buf[off++] = '-';
            }
            off += writeLong(hour, buf, off);
            buf[off++] = 'H';
        }
        int secondsOfDay = (int) (seconds - hour * 3600);
        int minute = (int) EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(secondsOfDay, 0x444444444444445L); // secondsOfDay / 60;
        if (minute > 0) {
            if (negative) {
                buf[off++] = '-';
            }
            off += writeInteger(minute, buf, off);
            buf[off++] = 'M';
        }
        int second = secondsOfDay - minute * 60;
        if (second == 0 && nano == 0) {
            buf[off++] = '"';
            count = off;
            return;
        }
        if (negative) {
            buf[off++] = '-';
        }
        off += writeInteger(second, buf, off);
        if (nano > 0) {
            off = writeNano(nano, off);
            while (buf[off - 1] == '0') {
                --off;
            }
        }
        buf[off++] = 'S';
        buf[off++] = '"';
        count = off;
    }

    public void writeJSONYearMonth(int year, int monthValue) throws IOException {
        ensureCapacity(9 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';

        if (year < 0) {
            buf[off++] = '-';
            year = -year;
        }
        if (year < 10000) {
            off += JSONMemoryHandle.putLong(buf, off, JSONMemoryHandle.JSON_ENDIAN.mergeYearAndMonth(year, monthValue)) - 1;
        } else {
            off += writeLong(year, buf, off);
            off += JSONMemoryHandle.putInt(buf, off, mergeInt32(monthValue, '-', '-')) - 1;
        }

        buf[off++] = '"';
        count = off;
    }

    public void writeJSONDefaultMonthDay(int monthValue, int dayOfMonth) throws IOException {
        ensureCapacity(8 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        buf[off++] = '-';
        off += JSONMemoryHandle.putInt(buf, off, mergeInt32(monthValue, '-', '-'));
        off += JSONMemoryHandle.putShort(buf, off, TWO_DIGITS_16_BITS[dayOfMonth]);
        buf[off++] = '"';
        count = off;
    }

    @Override
    public void writeDate(int year, int month, int day, int hourOfDay, int minute, int second) {
        ensureCapacity(24 + SECURITY_UNCHECK_SPACE);
        int off = count;
        if (year < 0) {
            buf[off++] = '-';
            year = -year;
        }
        if (year < 10000) {
            off += JSONMemoryHandle.putLong(buf, off, JSONMemoryHandle.JSON_ENDIAN.mergeYearAndMonth(year, month));
        } else {
            off += writeLong(year, buf, off);
            off += JSONMemoryHandle.putInt(buf, off, mergeInt32(month, '-', '-'));
        }
        off += JSONMemoryHandle.putShort(buf, off, TWO_DIGITS_16_BITS[day]);
        buf[off++] = ' ';
        off += JSONMemoryHandle.putLong(buf, off, JSONMemoryHandle.JSON_ENDIAN.mergeHHMMSS(hourOfDay, minute, second));
        count = off;
    }

    @Override
    public final void writeBigInteger(BigInteger bigInteger) {
        int increment = ((bigInteger.bitLength() / 60) + 1) * 18;
        ensureCapacity(increment + SECURITY_UNCHECK_SPACE);
        count += writeBigInteger(bigInteger, buf, count);
    }

    @Override
    public void writeEmptyArray() throws IOException {
        ensureCapacity(2 + SECURITY_UNCHECK_SPACE);
        count += JSONMemoryHandle.putShort(buf, count, EMPTY_ARRAY_SHORT);
    }

    @Override
    void writeMemory(long fourChars, int fourBytes, int len) throws IOException {
        JSONMemoryHandle.putInt(buf, count, fourBytes);
        count += len;
    }

    @Override
    void writeMemory(long fourChars1, long fourChars2, long fourBytes, int len) throws IOException {
        JSONMemoryHandle.putLong(buf, count, fourBytes);
        count += len;
    }

    @Override
    void writeMemory(long[] fourChars, long[] fourBytes, int totalCount) throws IOException {
        int n = fourChars.length;
        ensureCapacity((n << 3) + SECURITY_UNCHECK_SPACE);
        int count = this.count;
        for (long fourByte : fourBytes) {
            JSONMemoryHandle.putLong(buf, count, fourByte);
            count += 8;
        }
        this.count += totalCount;
    }

    @Override
    void writeMemory2(int int32, short int16, char[] source, int off) throws IOException {
        count += JSONMemoryHandle.putShort(buf, count, int16);
    }

    public void writeBytes(byte[] bytes, int offset, int len) {
        ensureCapacity(len);
        System.arraycopy(bytes, offset, buf, count, len);
        count += len;
    }

    @Override
    public void writeAsBase64String(byte[] src) throws IOException {
        ensureCapacity((src.length << 1) + SECURITY_UNCHECK_SPACE);
        int count = this.count;
        buf[count++] = '"';
        count += Base64Utils.encode(src, buf, count);
        buf[count++] = '"';
        this.count = count;
    }

    @Override
    public void writeAsHexString(byte[] src) throws IOException {
        ensureCapacity((src.length << 1) + SECURITY_UNCHECK_SPACE);
        int count = this.count;
        buf[count++] = '"';
        for (byte b : src) {
            count += JSONMemoryHandle.putShort(buf, count, (short) HEX_DIGITS_INT16[b & 0xff]);
        }
        buf[count++] = '"';
        this.count = count;
    }

    void clearCache() {
        if (bufCache != null) {
            if (buf.length <= MAX_CACHE_BUFFER_SIZE) {
                bufCache.cacheBytes = buf;
            }
            bufCache.inUse = false;
            // getOrReturnCache(byteBufCache);
            bufCache = null;
        }
    }
}
