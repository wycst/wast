package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.common.utils.NumberUtils;
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
 * <p> JSON CharArray writer
 * <p> Optimization of single character out of bounds check
 * <p> only used for JSON.toJsonBytes api </p>
 *
 * @Author: wangy
 * @Date: 2023/12/22 21:48
 * @Description:
 * @see JSON#toJsonBytes(Object, Charset, WriteOption...)
 */
class JSONByteArrayWriter extends JSONWriter {

    private final Charset charset;
    private final boolean utf8;
    // buff
    byte[] buf;

    // 长度
    int count;

    // EMPTY chars
    static final byte[] EMPTY_BUF = new byte[0];
    final static ByteBufCache[] BYTE_BUF_CACHES = new ByteBufCache[CACHE_COUNT];

    static {
        // init caches
        for (int i = 0; i < CACHE_COUNT; ++i) {
            ByteBufCache byteBufCache = new ByteBufCache();
            byteBufCache.index = i;
            if (i < AVAILABLE_PROCESSORS) {
                byteBufCache.cacheBytes = new byte[CACHE_BUFFER_SIZE];
            }
            BYTE_BUF_CACHES[i] = byteBufCache;
        }
    }

    JSONByteArrayWriter(Charset charset) {
        this.charset = charset;
        utf8 = charset == EnvUtils.CHARSET_UTF_8;
        // use pool
        ByteBufCache byteBufCache = getByteBufCache(); // getOrReturnCache(null);
        if (byteBufCache != null) {
            buf = byteBufCache.cacheBytes;
            this.byteBufCache = byteBufCache;
        } else {
            buf = new byte[512];
        }
    }

    JSONByteArrayWriter(int cap, Charset charset) {
        buf = new byte[cap];
        this.charset = charset;
        utf8 = charset == EnvUtils.CHARSET_UTF_8;
    }

    private static ByteBufCache getByteBufCache() {
        int cacheIndex = THREAD_CACHE_INDEX.get();
        ByteBufCache cache = BYTE_BUF_CACHES[cacheIndex];
        synchronized (cache) {
            if (cache.inUse) return null;
            cache.inUse = true;
            if (cache.cacheBytes == null) {
                cache.cacheBytes = new byte[CACHE_BUFFER_SIZE];
            }
        }
        return cache;
    }
//    static int availableCount = CACHE_COUNT;
//    /**
//     * Get a cache object from the pool, the index of cache is always --availableCount
//     *
//     * @return
//     */
//    private static ByteBufCache applyCharBufCache() {
//        if (availableCount == 0) {
//            return null;
//        }
//        ByteBufCache byteBufCache = BYTE_BUF_CACHES[--availableCount];
//        byteBufCache.inUse = true;
//        byteBufCache.index = availableCount;
//        if (byteBufCache.cacheBytes == null) {
//            byteBufCache.cacheBytes = new byte[CACHE_BUFFER_SIZE];
//        }
//        return byteBufCache;
//    }
//
//    /**
//     * Return to cache pool
//     *
//     * @param byteBufCache
//     */
//    private static void returnCharBufCache(ByteBufCache byteBufCache) {
//        byteBufCache.inUse = false;
//        // CharBufCache is not null, availableCount must be less than CHAR_BUF_CACHES.length(CACHE_COUNT) and must be the first used(inUse = true) position in the pool
//        if (byteBufCache.index == availableCount) {
//            ++availableCount;
//        } else {
//            ByteBufCache oldCache = BYTE_BUF_CACHES[availableCount];
//            int returnIndex = byteBufCache.index;
//            // return back and swap index
//            BYTE_BUF_CACHES[availableCount] = byteBufCache;
//            BYTE_BUF_CACHES[returnIndex] = oldCache;
//            // update pos
//            oldCache.index = returnIndex;
//            byteBufCache.index = availableCount++;
//        }
//    }
//
//    /***
//     * <p> Cache exits and entrances </p>
//     *
//     * Ensure thread safety to access availableCount(Currently available cache)
//     *
//     * @param byteBufCache
//     * @return
//     */
//    static synchronized ByteBufCache getOrReturnCache(ByteBufCache byteBufCache) {
//        if (byteBufCache == null) {
//            return applyCharBufCache();
//        } else {
//            returnCharBufCache(byteBufCache);
//            return null;
//        }
//    }

    /**
     * The current instance uses a cached instance
     */
    private ByteBufCache byteBufCache;

    private static class ByteBufCache {
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
        Object value = UnsafeHelper.getStringValue(str);
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) value;
            if (bytes.length == str.length()) {
                ensureCapacity(len + SECURITY_UNCHECK_SPACE);
                // LATIN
                System.arraycopy(bytes, off, buf, count, len);
                count += len;
            } else {
                ensureCapacity(len * 4 + SECURITY_UNCHECK_SPACE);
                // UTF16
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
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
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

    public void writeLatinJSONString(String value, byte[] bytes) throws IOException {
        int len = bytes.length;
        ensureCapacity(len + (2 + SECURITY_UNCHECK_SPACE));
        int count = this.count;
        buf[count++] = '"';
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

//    public void writeLatinJSONBytes(byte[] bytes) {
//        int len = bytes.length;
//        ensureCapacity(len * 6);
//        if (len <= 64) {
//            writeShortLatinJSONBytes(bytes);
//        } else {
//            writeLongLatinJSONBytes(bytes);
//        }
//    }

//    void writeShortLatinJSONBytes(byte[] bytes) {
//        int count = this.count;
//        // ascii
//        buf[count++] = '"';
//        for (byte b : bytes) {
//            String escapeStr;
//            if ((b > '"' && b != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[b & 0xFF]) == null) {
//                buf[count++] = b;
//                continue;
//            }
//            for (int j = 0; j < escapeStr.length(); ++j) {
//                buf[count++] = (byte) escapeStr.charAt(j);
//            }
//        }
//        buf[count++] = '"';
//        this.count = count;
//    }

//    void writeLongLatinJSONBytes(byte[] bytes) {
//        int beginIndex = 0, len = bytes.length, count = this.count;
//        buf[count++] = '"';
//        for (int i = 0; i < len; ++i) {
//            byte b = bytes[i];
//            if (b > '"' && b != '\\') continue;
//            String escapeStr;
//            if ((escapeStr = JSONGeneral.ESCAPE_VALUES[b & 0xFF]) != null) {
//                int length = i - beginIndex;
//                if (length > 0) {
//                    System.arraycopy(bytes, beginIndex, buf, count, length);
//                    count += length;
//                }
//                for (int j = 0; j < escapeStr.length(); ++j) {
//                    buf[count++] = (byte) escapeStr.charAt(j);
//                }
//                beginIndex = i + 1;
//            }
//        }
//        int length = len - beginIndex;
//        if (length > 0) {
//            System.arraycopy(bytes, beginIndex, buf, count, length);
//            count += length;
//        }
//        buf[count++] = '"';
//        this.count = count;
//    }


//    public void writeUTF16JSONBytes(String value) {
//        int len = value.length();
//        ensureCapacity(len << 2);
//        int count = this.count;
//        // ascii
//        buf[count++] = '"';
//        for (int i = 0; i < len; ++i) {
//            char c = value.charAt(i);
//            String escapeStr;
//            if ((c > '"' && c != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[c & 0xFF]) == null) {
//                if (c < 0x80) {
//                    buf[count++] = (byte) c;
//                } else {
//                    count = encode(c, count);
//                }
//                continue;
//            }
//            for (int j = 0; j < escapeStr.length(); ++j) {
//                buf[count++] = (byte) escapeStr.charAt(j);
//            }
//        }
//        buf[count++] = '"';
//        this.count = count;
//    }

//    @Override
//    public void writeLatinBytes(String source, byte[] bytes, int offset, int len) {
//        ensureCapacity(len);
//        System.arraycopy(bytes, offset, buf, count, len);
//        count += len;
//    }

//    public void writeShortLatinBytes(byte[] bytes, int offset, int len) {
//        ensureCapacity(len);
//        int count = this.count;
//        for (int i = offset, end = offset + len; i < end; ++i) {
//            buf[count++] = bytes[i];
//        }
//        this.count = count;
//    }

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
        count += NumberUtils.writePositiveLong(numValue, buf, count);
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
                off += NumberUtils.writePositiveLong(val1, buf, off);
            }
        } else {
            buf[off++] = ',';
            off += NumberUtils.writePositiveLong(val1, buf, off);
        }
        if (val2 < 0) {
            if (val2 == Long.MIN_VALUE) {
                write(",-9223372036854775808");
                off = count;
            } else {
                val2 = -val2;
                buf[off++] = ',';
                buf[off++] = '-';
                off += NumberUtils.writePositiveLong(val2, buf, off);
            }
        } else {
            buf[off++] = ',';
            off += NumberUtils.writePositiveLong(val2, buf, off);
        }
        count = off;
    }

    @Override
    public void writeUUID(UUID uuid) {
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();
        ensureCapacity(38 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        off += NumberUtils.writeUUIDMostSignificantBits(mostSigBits, buf, off);
        off += NumberUtils.writeUUIDLeastSignificantBits(leastSigBits, buf, off);
        buf[off++] = '"';
        count = off;
    }

    @Override
    public void writeDouble(double numValue) {
        ensureCapacity(24 + SECURITY_UNCHECK_SPACE);
        count += NumberUtils.writeDouble(numValue, buf, count);
    }

    @Override
    public void writeFloat(float numValue) {
        ensureCapacity(24 + SECURITY_UNCHECK_SPACE);
        count += NumberUtils.writeFloat(numValue, buf, count);
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
        off += NumberUtils.writeFourDigits(year, buf, off);
        off += NumberUtils.writeTwoDigitsAndPreSuffix(month, '-', '-', buf, off);
        off += NumberUtils.writeTwoDigits(day, buf, off);
        off += NumberUtils.writeTwoDigitsAndPreSuffix(hour, 'T', ':', buf, off);
        off += NumberUtils.writeTwoDigits(minute, buf, off);
        buf[off++] = ':';
        off += NumberUtils.writeTwoDigits(second, buf, off);
        if (nano > 0) {
            off = writeNano(nano, off);
        }
        if (zoneId.length() == 1) {
            off += UnsafeHelper.putShort(buf, off, Z_QUOT_SHORT);
            count = off;
        } else {
            count = off;
            writeZoneId(zoneId);
            buf[count++] = '"';
        }
    }

    int writeNano(int nano, int off) {
        // 4 + 2 + 3
        buf[off++] = '.';
        // seg1 4
        int div1 = nano / 1000;
        int seg1 = div1 / 100;
        int seg2 = div1 - 100 * seg1;
        int seg3 = nano - div1 * 1000;
        off += NumberUtils.writeFourDigits(seg1, buf, off);
        off += NumberUtils.writeTwoDigits(seg2, buf, off);
        if (seg3 > 0) {
            int pos = --off;
            byte last = buf[pos];
            off += NumberUtils.writeFourDigits(seg3, buf, pos);
            buf[pos] = last;
        } else {
            if (seg2 == 0) {
                off -= 2;
                if ((seg1 & 1) == 0 && seg1 % 5 == 0) {
                    --off;
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
        off += NumberUtils.writeFourDigits(year, buf, off);
        off += NumberUtils.writeTwoDigitsAndPreSuffix(month, '-', '-', buf, off);
        off += NumberUtils.writeTwoDigits(day, buf, off);
        buf[off++] = '"';
        count = off;
    }

    @Override
    public void writeTime(int hourOfDay, int minute, int second) {
        ensureCapacity(10 + SECURITY_UNCHECK_SPACE);
        int off = count;
        off += NumberUtils.writeTwoDigits(hourOfDay, buf, off);
        off += NumberUtils.writeTwoDigitsAndPreSuffix(minute, ':', ':', buf, off);
        off += NumberUtils.writeTwoDigits(second, buf, off);
        count = off;
    }

    @Override
    public void writeJSONTimeWithNano(int hourOfDay, int minute, int second, int nano) {
        ensureCapacity(22 + SECURITY_UNCHECK_SPACE);
        int off = count;
        buf[off++] = '"';
        off += NumberUtils.writeTwoDigits(hourOfDay, buf, off);
        off += NumberUtils.writeTwoDigitsAndPreSuffix(minute, ':', ':', buf, off);
        off += NumberUtils.writeTwoDigits(second, buf, off);
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
        off += NumberUtils.writeFourDigits(year, buf, off);
        off += NumberUtils.writeTwoDigitsAndPreSuffix(month, '-', '-', buf, off);
        off += NumberUtils.writeTwoDigits(day, buf, off);
        off += NumberUtils.writeTwoDigitsAndPreSuffix(hourOfDay, ' ', ':', buf, off);
        off += NumberUtils.writeTwoDigits(minute, buf, off);
        buf[off++] = ':';
        off += NumberUtils.writeTwoDigits(second, buf, off);
        count = off;
    }

    @Override
    public void writeBigInteger(BigInteger bigInteger) {
        int increment = ((bigInteger.bitLength() / 60) + 1) * 18;
        ensureCapacity(increment + SECURITY_UNCHECK_SPACE);
        count += NumberUtils.writeBigInteger(bigInteger, buf, count);
    }

    @Override
    public void writeEmptyArray() throws IOException {
        ensureCapacity(2 + SECURITY_UNCHECK_SPACE);
        UnsafeHelper.putShort(buf, count, EMPTY_ARRAY_SHORT);
        count += 2;
    }

    @Override
    public void writeUnsafe(long fourChars, int fourBytes, int len) throws IOException {
        // ensureCapacity(4 + SECURITY_UNCHECK_SPACE);
        UnsafeHelper.putInt(buf, count, fourBytes);
        count += len;
    }

    @Override
    public void writeUnsafe(long[] fourChars, int[] fourBytes, int totalCount) throws IOException {
        int n = fourChars.length;
        ensureCapacity((n << 2) + SECURITY_UNCHECK_SPACE);
        int count = this.count;
        for (int fourByte : fourBytes) {
            UnsafeHelper.putInt(buf, count, fourByte);
            count += 4;
        }
        this.count += totalCount;
    }

    //    @Override
//    public void writeShortBytes(String source, byte[] bytes) {
//        if (defaultCharset) {
//            int len = bytes.length;
//            ensureCapacity(len);
//            System.arraycopy(bytes, 0, buf, count, len);
//            count += len;
//        } else {
//            write(source, 0, source.length());
//        }
//    }

    public void writeBytes(byte[] bytes, int offset, int len) {
        ensureCapacity(len);
        System.arraycopy(bytes, offset, buf, count, len);
        count += len;
    }

    void clearCache() {
        if (byteBufCache != null) {
            if (buf.length <= MAX_CACHE_BUFFER_SIZE) {
                byteBufCache.cacheBytes = buf;
            }
            byteBufCache.inUse = false;
            // getOrReturnCache(byteBufCache);
            byteBufCache = null;
        }
    }
}
