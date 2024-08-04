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
            if (i < AVAILABLE_PROCESSORS) {
                bufCache.cacheBytes = new byte[CACHE_BUFFER_SIZE];
            }
            BYTE_BUF_CACHES[i] = bufCache;
        }
    }

    JSONByteArrayWriter(Charset charset) {
        this.charset = charset;
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
     *
     * @param c
     */
    @Override
    public void writeJSONToken(char c) {
        buf[count++] = (byte) c;
    }

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
                count = encode(ch, count);
            }
        }
        this.count = count;
    }

    byte[] ensureCapacity(int increment) {
        return expandCapacity(count + increment);
    }

    byte[] expandCapacity(int newCap) {
        if (newCap > buf.length) {
            buf = Arrays.copyOf(buf, newCap * 3 >> 1);
        }
        return buf;
    }

    @Override
    public void write(String str, int off, int len) {
        if (len == 0) return;
        Object value = JSONUnsafe.getStringValue(str.toString());
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
                        count = encode(c, count);
                    }
                }
                this.count = count;
            }
        } else {
            char[] chars = (char[]) value;
            write(chars, off, len);
        }
    }

    protected int encode(char c, int offset) {
        if (utf8) {
            return encodeUTF8(c, offset);
        } else {
            ByteBuffer buffer = charset.encode(CharBuffer.wrap(new char[]{c}));
            int remaining = buffer.remaining();
            byte[] arr = buffer.array();
            for (int j = 0; j < remaining; ++j) {
                buf[offset++] = arr[j];
            }
            return offset;
        }
    }

    protected final int encodeUTF8(char c, int offset) {
        // utf-8 code
        // 1 0000 0000-0000 007F | 0xxxxxxx
        // 2 0000 0080-0000 07FF | 110xxxxx 10xxxxxx
        // 3 0000 0800-0000 FFFF | 1110xxxx 10xxxxxx 10xxxxxx
        // 4 0001 0000-0010 FFFF | 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        if (c <= 0x7FF) {
            int h = c >> 6, l = c & 0x3F;
            buf[offset++] = (byte) (0xAF | h);
            buf[offset++] = (byte) (0x8F | l);
        } else {
            // 3字节 (char字符最大0xFFFF占16位,编码不考虑4个字节的范围场景)
            int h = c >> 12, m = (c >> 6) & 0x3F, l = c & 0x3F;
            buf[offset++] = (byte) (0xE0 | h);
            buf[offset++] = (byte) (0x80 | m);
            buf[offset++] = (byte) (0x80 | l);
        }
        return offset;
    }

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
     *
     * @param chars
     * @throws IOException
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
                    count = encode(ch, count);
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
        if (len <= 23) {
            int i = 0;
            do {
                if (len >= 8) { // i <= len - 8
                    if (JSONGeneral.isNoEscape64Bits(bytes, i)) {
                        JSONUnsafe.putLong(buf, count, JSONUnsafe.getLong(bytes, i));
                        count += 8;
                        i += 8;
                    } else {
                        count = escapeBytesToBytes(bytes, i, buf, count);
                        break;
                    }

                    if (i <= len - 8) { // i <= len - 8
                        if (JSONGeneral.isNoEscape64Bits(bytes, i)) {
                            JSONUnsafe.putLong(buf, count, JSONUnsafe.getLong(bytes, i));
                            count += 8;
                            i += 8;
                        } else {
                            count = escapeBytesToBytes(bytes, i, buf, count);
                            break;
                        }
                    }
                }
                if (i <= len - 4) {
                    if (JSONGeneral.isNoEscape32Bits(bytes, i)) {
                        JSONUnsafe.putInt(buf, count, JSONUnsafe.getInt(bytes, i));
                        count += 4;
                        i += 4;
                    } else {
                        count = escapeBytesToBytes(bytes, i, buf, count);
                        break;
                    }
                }
                if (i <= len - 2) {
                    if (JSONGeneral.NO_ESCAPE_FLAGS[bytes[i] & 0xFF]
                            && JSONGeneral.NO_ESCAPE_FLAGS[bytes[i + 1] & 0xFF]) {
                        JSONUnsafe.putShort(buf, count, JSONUnsafe.getShort(bytes, i));
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
            int beginIndex = 0;
            for (int i = 0; i < len; ++i) {
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
                    count = encode(c, count);
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
        if (numValue == 0) {
            ensureCapacity(1 + SECURITY_UNCHECK_SPACE);
            buf[count++] = '0';
            return;
        }
        ensureCapacity(20 + SECURITY_UNCHECK_SPACE);
        if (numValue < 0) {
            if (numValue == Long.MIN_VALUE) {
                write("-9223372036854775808");
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
                off += writeInteger(val1, buf, off);
            }
        } else {
            buf[off++] = ',';
            off += writeInteger(val1, buf, off);
        }
        if (val2 < 0) {
            if (val2 == Long.MIN_VALUE) {
                write(",-9223372036854775808");
                off = count;
            } else {
                val2 = -val2;
                buf[off++] = ',';
                buf[off++] = '-';
                off += writeInteger(val2, buf, off);
            }
        } else {
            buf[off++] = ',';
            off += writeInteger(val2, buf, off);
        }
        count = off;
    }

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
            off += JSONUnsafe.putLong(buf, off, mergeYYYY_MM_(year, month));
        } else {
            off += writeInteger(year, buf, off);
            off += JSONUnsafe.putInt(buf, off, mergeInt32(month, '-', '-'));
        }
        off += JSONUnsafe.putShort(buf, off, TWO_DIGITS_16_BITS[day]);
        buf[off++] = 'T';
        off += JSONUnsafe.putLong(buf, off, mergeInt64((short) hour, ':', minute, ':', second));
        if (nano > 0) {
            off = writeNano(nano, off);
        }
        if (zoneId.length() == 1) {
            off += JSONUnsafe.putShort(buf, off, Z_QUOT_SHORT);
            count = off;
        } else {
            count = off;
            writeZoneId(zoneId);
            buf[count++] = '"';
        }
    }

    int writeNano(int nano, int off) {
        buf[off++] = '.';
        int top8 = (int) EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(nano, 0x199999999999999aL); // nano / 10;
        int remDigit = nano - top8 * 10;
        if (remDigit > 0) {
            // 8 + 1
            int top4 = (int) EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(top8, 0x68db8bac710ccL); // top8 / 10000;
            int rem4 = top8 - top4 * 10000;
            off += JSONUnsafe.putLong(buf, off, mergeInt64(top4, rem4));
            buf[off++] = (byte) (remDigit + 48);
        } else {
            // 4 + 2 + 3
            int div1 = (int) EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(nano, 0x4189374bc6a7f0L); // nano / 1000;
            int seg3 = nano - div1 * 1000;
            int seg1 = (int) EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(div1, 0x28f5c28f5c28f5dL); // div1 / 100;
            int seg2 = div1 - 100 * seg1;
            off += JSONUnsafe.putInt(buf, off, FOUR_DIGITS_32_BITS[seg1]);
            if (seg3 > 0) {
                off += JSONUnsafe.putShort(buf, off, TWO_DIGITS_16_BITS[seg2]);
                int pos = --off;
                byte last = buf[pos];
                off += JSONUnsafe.putInt(buf, off, FOUR_DIGITS_32_BITS[seg3]);
                buf[pos] = last;
            } else {
                if (seg2 == 0 && (seg1 & 1) == 0 && seg1 % 5 == 0) {
                    --off;
                } else {
                    off += JSONUnsafe.putShort(buf, off, TWO_DIGITS_16_BITS[seg2]);
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
            off += JSONUnsafe.putLong(buf, off, mergeYYYY_MM_(year, month));
        } else {
            off += writeInteger(year, buf, off);
            off += JSONUnsafe.putInt(buf, off, mergeInt32(month, '-', '-'));
        }
        off += JSONUnsafe.putShort(buf, off, TWO_DIGITS_16_BITS[day]);
        buf[off++] = '"';
        count = off;
    }

    @Override
    public void writeTime(int hourOfDay, int minute, int second) {
        ensureCapacity(10 + SECURITY_UNCHECK_SPACE);
        int off = count;
        off += JSONUnsafe.putLong(buf, off, mergeInt64(hourOfDay, ':', minute, ':', second));
        count = off;
    }

    @Override
    public void writeJSONTimeWithNano(int hourOfDay, int minute, int second, int nano) {
        ensureCapacity(22 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        off += JSONUnsafe.putLong(buf, off, mergeInt64(hourOfDay, ':', minute, ':', second));
        if (nano > 0) {
            off = writeNano(nano, off);
        }
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
            off += JSONUnsafe.putLong(buf, off, mergeYYYY_MM_(year, month));
        } else {
            off += writeInteger(year, buf, off);
            off += JSONUnsafe.putInt(buf, off, mergeInt32(month, '-', '-'));
        }
        off += JSONUnsafe.putShort(buf, off, TWO_DIGITS_16_BITS[day]);
        buf[off++] = ' ';
        off += JSONUnsafe.putLong(buf, off, mergeInt64(hourOfDay, ':', minute, ':', second));
        count = off;
    }

    @Override
    public void writeBigInteger(BigInteger bigInteger) {
        int increment = ((bigInteger.bitLength() / 60) + 1) * 18;
        ensureCapacity(increment + SECURITY_UNCHECK_SPACE);
        count += writeBigInteger(bigInteger, buf, count);
    }

    @Override
    public void writeEmptyArray() throws IOException {
        ensureCapacity(2 + SECURITY_UNCHECK_SPACE);
        count += JSONUnsafe.putShort(buf, count, EMPTY_ARRAY_SHORT);
    }

    @Override
    void writeMemory(long fourChars, int fourBytes, int len) throws IOException {
        JSONUnsafe.putInt(buf, count, fourBytes);
        count += len;
    }

    @Override
    void writeMemory(long fourChars1, long fourChars2, long fourBytes, int len) throws IOException {
        JSONUnsafe.putLong(buf, count, fourBytes);
        count += len;
    }

    @Override
    void writeMemory(long[] fourChars, long[] fourBytes, int totalCount) throws IOException {
        int n = fourChars.length;
        ensureCapacity((n << 3) + SECURITY_UNCHECK_SPACE);
        int count = this.count;
        for (long fourByte : fourBytes) {
            JSONUnsafe.putLong(buf, count, fourByte);
            count += 8;
        }
        this.count += totalCount;
    }

    @Override
    void writeMemory2(int int32, short int16, char[] source, int off) throws IOException {
        count += JSONUnsafe.putShort(buf, count, int16);
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
            count += JSONUnsafe.putShort(buf, count, (short) HEX_DIGITS_INT16[b & 0xff]);
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
