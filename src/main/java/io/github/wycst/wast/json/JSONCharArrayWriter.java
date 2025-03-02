package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.Base64Utils;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.common.utils.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

/**
 * <p> JSON CharArray writer
 * <p> Optimization of single character out of bounds check
 *
 * @Author: wangy
 * @Date: 2021/12/22 21:48
 * @Description:
 */
class JSONCharArrayWriter extends JSONWriter {

    // 缓冲字符数组
    char[] buf;

    // 长度
    int count;

    // EMPTY chars
    static final char[] EMPTY_BUF = new char[0];
    private final static BufCache[] CHAR_BUF_CACHES = new BufCache[CACHE_COUNT];
    private Charset charset;

    static {
        // init caches
        for (int i = 0; i < CACHE_COUNT; ++i) {
            BufCache bufCache = new BufCache();
            bufCache.index = i;
            if (i < AVAILABLE_PROCESSORS) {
                bufCache.cacheChars = new char[CACHE_BUFFER_SIZE];
            }
            CHAR_BUF_CACHES[i] = bufCache;
        }
    }

    private static BufCache getCharBufCache() {
        int cacheIndex = THREAD_CACHE_INDEX.get();
        BufCache cache = CHAR_BUF_CACHES[cacheIndex];
        synchronized (cache) {
            if (cache.inUse) return null;
            cache.inUse = true;
            if (cache.cacheChars == null) {
                cache.cacheChars = new char[CACHE_BUFFER_SIZE];
            }
        }
        return cache;
    }

    /**
     * The current instance uses a cached instance
     */
    private BufCache bufCache;

    void setCharAt(int index, char c) {
        buf[index] = c;
    }

    protected char[] toChars() {
        return Arrays.copyOf(buf, count);
    }

    private static class BufCache {
        char[] cacheChars;
        boolean inUse;
        int index;
    }

//    // per thread use 16kb init
//    // If using threadlocal, the cache size cannot be controlled
//    static ThreadLocal<CharBufCache> LOCAL_BUF = new ThreadLocal<CharBufCache>() {
//        @Override
//        protected CharBufCache initialValue() {
//            return new CharBufCache(new char[CACHE_CHAR_BUFFER_SIZE]);
//        }
//    };

    JSONCharArrayWriter(Charset charset) {
        this();
        this.charset = charset == null ? EnvUtils.CHARSET_DEFAULT : charset;
    }

    JSONCharArrayWriter() {
        // use pool
        BufCache bufCache = getCharBufCache();
        if (bufCache != null) {
            buf = bufCache.cacheChars;
            this.bufCache = bufCache;
        } else {
            buf = new char[512];
        }
    }

    JSONCharArrayWriter(int cap) {
        buf = new char[cap];
    }

    @Override
    public void write(int c) {
        ensureCapacity(1 + SECURITY_UNCHECK_SPACE);
        buf[count++] = (char) c;
    }

    /**
     * Skip out of bounds detection
     * @param c
     */
    public void writeDirect(char c) {
        buf[count++] = c;
    }

    /**
     * Write special token characters to skip expansion detection
     *
     * <p> 5 special JSON tokens such as '{', '}', '[', ']', ',' or other single character </p>
     * <p> Ensure to reserve 3 additional locations for each expansion </p>
     * <p> Avoid writing consecutive token characters such as "{}" or "[]", or "[{}, {}]" </p>
     *
     * @param c
     */
    @Override
    public void writeJSONToken(char c) {
        buf[count++] = c;
    }

    @Override
    public void write(char[] c, int off, int len) {
        if (len == 0) return;
        ensureCapacity(len + SECURITY_UNCHECK_SPACE);
        System.arraycopy(c, off, buf, count, len);
        count += len;
    }

    char[] ensureCapacity(int increment) {
        return expandCapacity(count + increment);
    }

    char[] expandCapacity(long minCap) {
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

    /**
     * 写入 bytes
     *
     * @param bytes
     * @param offset
     * @param len
     */
    public void writeBytes(byte[] bytes, int offset, int len) {
        if (len == 0) return;
        String str = new String(bytes, offset, len);
        // reset len maybe utf8 bytes
        len = str.length();
        ensureCapacity(len);
        str.getChars(0, len, buf, count);
        count += len;
    }

    // short string
    @Override
    public void write(String str, int off, int len) {
        if (len == 0) return;
        ensureCapacity(len + SECURITY_UNCHECK_SPACE);
        int count = this.count, i = 0;
        for (; i < len; ++i) {
            buf[count++] = str.charAt(off++);
        }
        this.count = count;
    }

    // long string
    public void writeString(String str, int off, int len) {
        if (len == 0) return;
        ensureCapacity(len + SECURITY_UNCHECK_SPACE);
        str.getChars(off, off + len, buf, count);
        count += len;
    }

    @Override
    public final void flush() throws IOException {
    }

    @Override
    public final void close() throws IOException {
    }

    protected StringBuffer toStringBuffer() {
        StringBuffer stringBuffer = new StringBuffer(count);
        stringBuffer.append(buf, 0, count);
        return stringBuffer;
    }

    protected StringBuilder toStringBuilder() {
        StringBuilder stringBuilder = new StringBuilder(count);
        stringBuilder.append(buf, 0, count);
        return stringBuilder;
    }

    @Override
    protected void toOutputStream(OutputStream os) throws IOException {
        boolean isByteArrayOs = os.getClass() == ByteArrayOutputStream.class;
        boolean emptyByteArrayOs = false;
        if (isByteArrayOs) {
            ByteArrayOutputStream byteArrayOutputStream = (ByteArrayOutputStream) os;
            emptyByteArrayOs = byteArrayOutputStream.size() == 0;
        }
        String source = new String(buf, 0, count);
        byte[] bytes = (byte[]) JSONUnsafe.getStringValue(source);
        if (bytes.length == count) {
            if (emptyByteArrayOs) {
                JSONUnsafe.UNSAFE.putObject(os, UnsafeHelper.BAO_BUF_OFFSET, bytes);
                JSONUnsafe.UNSAFE.putInt(os, UnsafeHelper.BAO_COUNT_OFFSET, count);
            } else {
                os.write(bytes, 0, count);
            }
        } else {
            if (charset == null || charset == EnvUtils.CHARSET_UTF_8) {
                byte[] output = new byte[count * 3];
                int length = IOUtils.encodeUTF8(buf, 0, count, output);
                if (emptyByteArrayOs) {
                    JSONUnsafe.UNSAFE.putObject(os, UnsafeHelper.BAO_BUF_OFFSET, output);
                    JSONUnsafe.UNSAFE.putInt(os, UnsafeHelper.BAO_COUNT_OFFSET, length);
                } else {
                    os.write(output, 0, length);
                }
            } else {
                bytes = source.getBytes(charset);
                if (emptyByteArrayOs) {
                    JSONUnsafe.UNSAFE.putObject(os, UnsafeHelper.BAO_BUF_OFFSET, bytes);
                    JSONUnsafe.UNSAFE.putInt(os, UnsafeHelper.BAO_COUNT_OFFSET, bytes.length);
                } else {
                    os.write(bytes);
                }
            }
        }
        os.flush();
    }

    // JDK9+
    @Override
    protected byte[] toBytes(Charset charset) {
        String source = new String(buf, 0, count);
        byte[] bytes = (byte[]) JSONUnsafe.getStringValue(source);
        if (bytes.length == count) {
            return bytes;
        } else {
            if (charset == EnvUtils.CHARSET_UTF_8) {
                byte[] output = new byte[count * 3];
                int length = IOUtils.encodeUTF8(buf, 0, count, output);
                return Arrays.copyOf(output, length);
            } else {
                bytes = source.getBytes(charset);
                return bytes;
            }
        }
    }

    @Override
    protected boolean endsWith(int c) {
        return count == 0 ? false : buf[count - 1] == c;
    }

    @Override
    public String toString() {
        return new String(buf, 0, count);
    }

    public int size() {
        return count;
    }

    /**
     * <p> directly write short character arrays use for loop (Do not use System.arraycopy) </p>
     *
     * @param chars
     * @param offset
     * @param len
     */
    @Override
    public final void writeShortChars(char[] chars, int offset, int len) {
        ensureCapacity(len + SECURITY_UNCHECK_SPACE);
        // 这里使用临时变量count的诡异之处
        int count = this.count;
        for (int i = 0; i < len; ++i) {
            buf[count++] = chars[offset++];
        }
        this.count = count;
    }

    /**
     * <p> Need to handle escape characters in JSON strings </p>
     *
     * @param chars
     */
    final void writeShortJSONChars(char[] chars) {
        int len = chars.length;
        ensureCapacity(len + (2 + SECURITY_UNCHECK_SPACE));
        int count = this.count;
        buf[count++] = '"';

        final int limit = len - 4;
        int offset = 0;
        long v64;
        while (offset <= limit
                && JSONGeneral.isNoneEscaped4Chars(v64 = JSONUnsafe.getLong(chars, offset))) {
            JSONUnsafe.putLong(buf, count, v64);
            offset += 4;
            count += 4;
        }
        for (; offset < len; ++offset) {
            char ch = chars[offset];
            String escapeStr;
            if ((ch > '"' && ch != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[ch]) == null) {
                buf[count++] = ch;
                continue;
            }
            int escapesLen = escapeStr.length();
            ensureCapacity(escapesLen + SECURITY_UNCHECK_SPACE);
            for (int j = 0; j < escapesLen; ++j) {
                buf[count++] = escapeStr.charAt(j);
            }
        }
//        for (char ch : chars) {
//            String escapeStr;
//            if ((ch > '"' && ch != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[ch]) == null) {
//                buf[count++] = ch;
//                continue;
//            }
//            int escapesLen = escapeStr.length();
//            ensureCapacity(escapesLen + SECURITY_UNCHECK_SPACE);
//            for (int j = 0; j < escapesLen; ++j) {
//                buf[count++] = escapeStr.charAt(j);
//            }
//        }
        buf[count++] = '"';
        this.count = count;
    }

    @Override
    public final void writeLong(long numValue) throws IOException {
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

    @Override
    protected final void writeCommaLongValues(long val1, long val2) throws IOException {
        ensureCapacity(42 + SECURITY_UNCHECK_SPACE);
        int off = count;
        if (val1 < 0) {
            if (val1 == Long.MIN_VALUE) {
                write(",-9223372036854775808");
                off = count;
            } else {
                val1 = -val1;
                buf[off++] = ',';
                buf[off++] = '-';
                off += writeLong(val1, buf, off);
            }
        } else {
            buf[off++] = ',';
            off += writeLong(val1, buf, off);
        }
        if (val2 < 0) {
            if (val2 == Long.MIN_VALUE) {
                write(",-9223372036854775808");
                off = count;
            } else {
                val2 = -val2;
                buf[off++] = ',';
                buf[off++] = '-';
                off += writeLong(val2, buf, off);
            }
        } else {
            buf[off++] = ',';
            off += writeLong(val2, buf, off);
        }
        count = off;
    }

    @Override
    public final void writeUUID(UUID uuid) {
        ensureCapacity(38 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        off += writeUUID(uuid, buf, off);
        buf[off++] = '"';
        count = off;
    }

    @Override
    public final void writeDouble(double numValue) {
        ensureCapacity(24 + SECURITY_UNCHECK_SPACE);
        int off = this.count;
        off += writeDouble(numValue, buf, off);
        count = off;
    }

    @Override
    public final void writeFloat(float numValue) {
        ensureCapacity(24 + SECURITY_UNCHECK_SPACE);
        count += writeFloat(numValue, buf, count);
    }

    @Override
    public final void writeJSONLocalDateTime(int year, int month, int day, int hour, int minute, int second, int nano, String zoneId) throws IOException {
        ensureCapacity(36 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        if (year < 0) {
            buf[off++] = '-';
            year = -year;
        }
        if (year < 10000) {
            off += JSONUnsafe.putLong(buf, off, FOUR_DIGITS_64_BITS[year]);
        } else {
            off += writeLong(year, buf, off);
        }
        off += JSONUnsafe.putLong(buf, off, ((long) '-') << 48 | ((long) TWO_DIGITS_32_BITS[month]) << 16 | '-');
        off += JSONUnsafe.putInt(buf, off, TWO_DIGITS_32_BITS[day]);
        off += JSONUnsafe.putLong(buf, off, mergeInt64(hour, 'T', ':'));
        off += JSONUnsafe.putInt(buf, off, TWO_DIGITS_32_BITS[minute]);
        buf[off++] = ':';
        off += JSONUnsafe.putInt(buf, off, TWO_DIGITS_32_BITS[second]);
        if (nano > 0) {
            off = writeNano(nano, buf, off);
        }
        if (zoneId.length() == 1) {
            off += JSONUnsafe.putInt(buf, off, Z_QUOT_INT);
            count = off;
        } else {
            count = off;
            writeZoneId(zoneId);
            buf[count++] = '"';
        }
    }

//    final int writeNano(int nano, int off) {
//        // 4 + 2 + 3
//        buf[off++] = '.';
//        // seg1 4
//        int div1 = (int) EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(nano, 0x4189374bc6a7f0L); // nano / 1000;
//        // 4
//        int seg1 = (int) EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(div1, 0x28f5c28f5c28f5dL); // div1 / 100;
//        // 2
//        int seg2 = div1 - 100 * seg1;
//        // 3
//        int seg3 = nano - div1 * 1000;
//        off += JSONUnsafe.putLong(buf, off, FOUR_DIGITS_64_BITS[seg1]);
//        if (seg3 > 0) {
//            off += JSONUnsafe.putInt(buf, off, TWO_DIGITS_32_BITS[seg2]);
//            int pos = --off;
//            char last = buf[pos];
//            off += JSONUnsafe.putLong(buf, pos, FOUR_DIGITS_64_BITS[seg3]); // writeFourDigits(seg3, buf, pos);
//            buf[pos] = last;
//        } else {
//            if (seg2 == 0 && (seg1 & 1) == 0 && seg1 % 5 == 0) {
//                // 4 - 1
//                --off;
//            } else {
//                // 4 + 2
//                off += JSONUnsafe.putInt(buf, off, TWO_DIGITS_32_BITS[seg2]);
//            }
//        }
//        return off;
//    }

    @Override
    public final void writeJSONLocalDate(int year, int month, int day) {
        ensureCapacity(13 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        if (year < 0) {
            buf[off++] = '-';
            year = -year;
        }
        if (year < 10000) {
            off += JSONUnsafe.putLong(buf, off, FOUR_DIGITS_64_BITS[year]);
        } else {
            off += writeLong(year, buf, off);
        }
        off += JSONUnsafe.putLong(buf, off, ((long) '-') << 48 | ((long) TWO_DIGITS_32_BITS[month]) << 16 | '-');
        off += JSONUnsafe.putInt(buf, off, TWO_DIGITS_32_BITS[day]);
        buf[off++] = '"';
        count = off;
    }

    @Override
    public final void writeTime(int hourOfDay, int minute, int second) {
        ensureCapacity(10 + SECURITY_UNCHECK_SPACE);
        int off = count;
        off += JSONUnsafe.putInt(buf, off, TWO_DIGITS_32_BITS[hourOfDay]);
        off += JSONUnsafe.putLong(buf, off, mergeInt64(minute, ':', ':'));
        off += JSONUnsafe.putInt(buf, off, TWO_DIGITS_32_BITS[second]);
        count = off;
    }

    @Override
    public final void writeJSONTimeWithNano(int hourOfDay, int minute, int second, int nano) {
        ensureCapacity(22 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        off += JSONUnsafe.putInt(buf, off, TWO_DIGITS_32_BITS[hourOfDay]);
        off += JSONUnsafe.putLong(buf, off, mergeInt64(minute, ':', ':')); // writeTwoDigitsAndPreSuffix(minute, ':', ':', buf, off);
        off += JSONUnsafe.putInt(buf, off, TWO_DIGITS_32_BITS[second]);
        if (nano > 0) {
            off = writeNano(nano, buf, off);
        }
        buf[off++] = '"';
        count = off;
    }

    @Override
    public final void writeDate(int year, int month, int day, int hourOfDay, int minute, int second) {
        ensureCapacity(24 + SECURITY_UNCHECK_SPACE);
        int off = count;
        if (year < 0) {
            buf[off++] = '-';
            year = -year;
        }
        if (year < 10000) {
            off += JSONUnsafe.putLong(buf, off, FOUR_DIGITS_64_BITS[year]);
        } else {
            off += writeLong(year, buf, off);
        }
        off += JSONUnsafe.putLong(buf, off, mergeInt64(month, '-', '-'));
        off += JSONUnsafe.putInt(buf, off, TWO_DIGITS_32_BITS[day]);
        off += JSONUnsafe.putLong(buf, off, mergeInt64(hourOfDay, ' ', ':'));
        off += JSONUnsafe.putInt(buf, off, TWO_DIGITS_32_BITS[minute]);
        buf[off++] = ':';
        off += JSONUnsafe.putInt(buf, off, TWO_DIGITS_32_BITS[second]);
        count = off;
    }

    @Override
    public final void writeBigInteger(BigInteger bigInteger) {
        int increment = ((bigInteger.bitLength() / 60) + 1) * 18;
        ensureCapacity(increment + SECURITY_UNCHECK_SPACE);
        count += writeBigInteger(bigInteger, buf, count);
    }

    @Override
    public void writeJSONChars(char[] chars) throws IOException {
        int len = chars.length;
        if (len < 64) {
            writeShortJSONChars(chars);
        } else {
            ensureCapacity(len + (2 + SECURITY_UNCHECK_SPACE));
            int count = this.count, beginIndex = 0, i = JSONGeneral.JSON_UTIL.toNoEscapeOffset(chars, 0);
            buf[count++] = '"';
            for (; i < len; ++i) {
                char ch = chars[i];
                String escapeStr;
                if ((ch > '"' && ch != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[ch]) == null) continue;
                int length = i - beginIndex;
                expandCapacity(length + count + (5 + SECURITY_UNCHECK_SPACE));
                // 很诡异的问题
                if (length > 0) {
                    System.arraycopy(chars, beginIndex, buf, count, length);
                    count += length;
                }
                int escapesLen = escapeStr.length();
                escapeStr.getChars(0, escapesLen, buf, count);
                count += escapesLen;
                beginIndex = i + 1;
            }
            int length = len - beginIndex;
            System.arraycopy(chars, beginIndex, buf, count, length);
            count += length;
            buf[count++] = '"';
            this.count = count;
        }
    }

    final static int escapeBytesToChars(byte[] bytes, int begin, char[] buf, int offset) {
        for (int i = begin, len = bytes.length; i < len; ++i) {
            int b = bytes[i] & 0xFF;
            if (isNoEscape(b)) {
                buf[offset++] = (char) b;
            } else {
                String escapeStr = JSONGeneral.ESCAPE_VALUES[b];
                int escapesLen = escapeStr.length();
                escapeStr.getChars(0, escapesLen, buf, offset);
                offset += escapesLen;
            }
        }
        return offset;
    }

    @Override
    public void writeLatinJSONString(String value, byte[] bytes) throws IOException {
        int len = bytes.length;
        ensureCapacity(len + (2 + SECURITY_UNCHECK_SPACE));
        int count = this.count;
        buf[count++] = '"';
        if (len > 15) {
            int beginIndex = 0, i = 0;
            if(JSONGeneral.isNoneEscaped8Bytes(JSONUnsafe.getLong(bytes, i))
                    && JSONGeneral.isNoneEscaped8Bytes(JSONUnsafe.getLong(bytes, i = i + 8))) {
                i += 8;
            }
            for (; i < len; ++i) {
                int b = bytes[i] & 0xFF;
                if (JSONGeneral.NO_ESCAPE_FLAGS[b]) continue;
                String escapeStr = JSONGeneral.ESCAPE_VALUES[b];
                int length = i - beginIndex;
                expandCapacity(length + count + 8);
                if (length > 0) {
                    value.getChars(beginIndex, i, buf, count);
                    count += length;
                }
                int escapesLen = escapeStr.length();
                escapeStr.getChars(0, escapesLen, buf, count);
                count += escapesLen;
                beginIndex = i + 1;
            }
            int length = len - beginIndex;
            if (length > 0) {
                value.getChars(beginIndex, len, buf, count);
                count += length;
            }
        } else {
            int i = 0, b, b1, b2, b3, b4, b5, b6, b7;
            do {
                if (i <= len - 8) {
                    if (JSONGeneral.isNoneEscaped8Bytes(JSONUnsafe.getLong(bytes, i))) {
                        buf[count] = (char) bytes[i];
                        buf[count + 1] = (char) bytes[i + 1];
                        buf[count + 2] = (char) bytes[i + 2];
                        buf[count + 3] = (char) bytes[i + 3];
                        buf[count + 4] = (char) bytes[i + 4];
                        buf[count + 5] = (char) bytes[i + 5];
                        buf[count + 6] = (char) bytes[i + 6];
                        buf[count + 7] = (char) bytes[i + 7];
                        count += 8;
                        i += 8;
                    } else {
                        count = escapeBytesToChars(bytes, i, buf, count);
                        break;
                    }
                }
                if (i <= len - 4) {
                    if (isNoEscape(b = bytes[i] & 0xFF)
                            && isNoEscape(b1 = bytes[i + 1] & 0xFF)
                            && isNoEscape(b2 = bytes[i + 2] & 0xFF)
                            && isNoEscape(b3 = bytes[i + 3] & 0xFF)) {
                        buf[count] = (char) b;
                        buf[count + 1] = (char) b1;
                        buf[count + 2] = (char) b2;
                        buf[count + 3] = (char) b3;
                        count += 4;
                        i += 4;
                    } else {
                        count = escapeBytesToChars(bytes, i, buf, count);
                        break;
                    }
                }
                if (i <= len - 2) {
                    if (isNoEscape(b = bytes[i] & 0xFF)
                            && isNoEscape(b1 = bytes[i + 1] & 0xFF)) {
                        buf[count] = (char) b;
                        buf[count + 1] = (char) b1;
                        count += 2;
                        i += 2;
                    } else {
                        count = escapeBytesToChars(bytes, i, buf, count);
                        break;
                    }
                }
                if (i < len) {
                    if (isNoEscape((b = bytes[i] & 0xFF))) {
                        buf[count++] = (char) b;
                    } else {
                        String escapeStr = JSONGeneral.ESCAPE_VALUES[b];
                        int escapesLen = escapeStr.length();
                        for (int j = 0; j < escapesLen; ++j) {
                            buf[count++] = escapeStr.charAt(j);
                        }
                        break;
                    }
                }
            } while (false);
        }
        buf[count++] = '"';
        this.count = count;
    }

    public final void writeLatinString(String str) throws IOException {
        int len = str.length();
        ensureCapacity(len + SECURITY_UNCHECK_SPACE);
        str.getChars(0, len, buf, count);
        count += len;
    }

    @Override
    final void writeMemory(long fourChars, int fourBytes, int len) throws IOException {
        JSONUnsafe.putLong(buf, count, fourChars);
        count += len;
    }

    @Override
    void writeMemory(long fourChars1, long fourChars2, long fourBytes, int len) throws IOException {
        JSONUnsafe.putLong(buf, count, fourChars1);
        JSONUnsafe.putLong(buf, count + 4, fourChars2);
        count += len;
    }

    @Override
    final void writeMemory(long[] fourChars, long[] fourBytes, int totalCount) throws IOException {
        int n = fourChars.length;
        ensureCapacity((n << 2) + SECURITY_UNCHECK_SPACE);
        int count = this.count;
        for (long fourChar : fourChars) {
            JSONUnsafe.putLong(buf, count, fourChar);
            count += 4;
        }
        this.count += totalCount;
    }

    @Override
    public final void writeEmptyArray() throws IOException {
        ensureCapacity(2 + SECURITY_UNCHECK_SPACE);
        count += JSONUnsafe.putInt(buf, count, EMPTY_ARRAY_INT);
    }

    @Override
    public void writeAsBase64String(byte[] src) throws IOException {
        ensureCapacity((src.length << 1) + 2);
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
            count += JSONUnsafe.putInt(buf, count, HEX_DIGITS_INT32[b & 0xff]);
        }
        buf[count++] = '"';
        this.count = count;
    }

    @Override
    public final void writeTo(Writer writer) throws IOException {
        writer.write(buf, 0, count);
        writer.flush();
    }

    void clearCache() {
        if (bufCache != null) {
            if (buf.length <= MAX_CACHE_BUFFER_SIZE) {
                bufCache.cacheChars = buf;
            }
            bufCache.inUse = false;
            // getOrReturnCache(charBufCache);
            bufCache = null;
        }
    }

    public void clear() {
        count = 0;
    }

    void reset() {
        clear();
        clearCache();
        buf = EMPTY_BUF;
    }

    static class IgnoreEscapeWriter extends JSONCharArrayWriter {
        IgnoreEscapeWriter() {
        }

        @Override
        public void writeJSONChars(char[] chars) throws IOException {
            int len = chars.length;
            ensureCapacity(len + (2 + SECURITY_UNCHECK_SPACE));
            int count = this.count;
            buf[count++] = '"';
            System.arraycopy(chars, 0, buf, count, len);
            count += len;
            buf[count++] = '"';
            this.count = count;
        }

        @Override
        public void writeLatinJSONString(String value, byte[] bytes) throws IOException {
            int len = bytes.length;
            ensureCapacity(len + (2 + SECURITY_UNCHECK_SPACE));
            int count = this.count;
            buf[count++] = '"';
            value.getChars(0, len, buf, count);
            count += len;
            buf[count++] = '"';
            this.count = count;
        }
    }

}
