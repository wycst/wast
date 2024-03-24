package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.common.utils.IOUtils;
import io.github.wycst.wast.common.utils.NumberUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

/**
 * <p> json chararray writer
 * <p> Override removes the method block in writer that uses synchronous security
 * <p> Partial optimization
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
    private final static CharBufCache[] CHAR_BUF_CACHES = new CharBufCache[CACHE_COUNT];

    static {
        // init caches
        for (int i = 0; i < CACHE_COUNT; ++i) {
            CharBufCache charBufCache = new CharBufCache();
            charBufCache.index = i;
            if (i < AVAILABLE_PROCESSORS) {
                charBufCache.cacheChars = new char[CACHE_BUFFER_SIZE];
            }
            CHAR_BUF_CACHES[i] = charBufCache;
        }
    }

    private static CharBufCache getCharBufCache() {
        int cacheIndex = THREAD_CACHE_INDEX.get();
        CharBufCache cache = CHAR_BUF_CACHES[cacheIndex];
        synchronized (cache) {
            if (cache.inUse) return null;
            cache.inUse = true;
            if (cache.cacheChars == null) {
                cache.cacheChars = new char[CACHE_BUFFER_SIZE];
            }
        }
        return cache;
    }
//    private static int availableCount = CACHE_COUNT;
//    /**
//     * Get a cache object from the pool, the index of cache is always --availableCount
//     *
//     * @return
//     */
//    private static CharBufCache applyCharBufCache() {
//        if (availableCount == 0) {
//            return null;
//        }
//        CharBufCache charBufCache = CHAR_BUF_CACHES[--availableCount];
//        charBufCache.inUse = true;
//        charBufCache.index = availableCount;
//        if (charBufCache.cacheChars == null) {
//            charBufCache.cacheChars = new char[CACHE_BUFFER_SIZE];
//        }
//        return charBufCache;
//    }
//
//    /**
//     * Return to cache pool
//     *
//     * @param charBufCache
//     */
//    private static void returnCharBufCache(CharBufCache charBufCache) {
//        charBufCache.inUse = false;
//        // CharBufCache is not null, availableCount must be less than CHAR_BUF_CACHES.length(CACHE_COUNT) and must be the first used(inUse = true) position in the pool
//        if (charBufCache.index == availableCount) {
//            ++availableCount;
//        } else {
//            CharBufCache oldCache = CHAR_BUF_CACHES[availableCount];
//            int returnIndex = charBufCache.index;
//            // return back and swap index
//            CHAR_BUF_CACHES[availableCount] = charBufCache;
//            CHAR_BUF_CACHES[returnIndex] = oldCache;
//            // update pos
//            oldCache.index = returnIndex;
//            charBufCache.index = availableCount++;
//        }
//    }
//
//    /***
//     * <p> Cache exits and entrances </p>
//     *
//     * Ensure thread safety to access availableCount(Currently available cache)
//     *
//     * @param charBufCache
//     * @return
//     */
//    static synchronized CharBufCache getOrReturnCache(CharBufCache charBufCache) {
//        if (charBufCache == null) {
//            return applyCharBufCache();
//        } else {
//            returnCharBufCache(charBufCache);
//            return null;
//        }
//    }

    /**
     * The current instance uses a cached instance
     */
    private CharBufCache charBufCache;

    void setCharAt(int index, char c) {
        buf[index] = c;
    }

    protected char[] toChars() {
        return Arrays.copyOf(buf, count);
    }

    private static class CharBufCache {
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

    JSONCharArrayWriter() {
//        CharBufCache charBufCache = LOCAL_BUF.get();
//        if (charBufCache.inUse) {
//            buf = new char[256];
//        } else {
//            charBufCache.inUse = true;
//            this.charBufCache = charBufCache;
//            buf = charBufCache.cacheBuffers;
//        }
        // use pool
        CharBufCache charBufCache = getCharBufCache(); // getOrReturnCache(null);
        if (charBufCache != null) {
            buf = charBufCache.cacheChars;
            this.charBufCache = charBufCache;
        } else {
            buf = new char[256];
        }
    }

    @Override
    public void write(int c) {
        ensureCapacity(1);
        buf[count++] = (char) c;
    }

    @Override
    public void write(char[] c, int off, int len) {
        if (len == 0) return;
        ensureCapacity(len);
        System.arraycopy(c, off, buf, count, len);
        count += len;
    }

    char[] ensureCapacity(int increment) {
        int newcount = count + increment;
        if (newcount > buf.length) {
            buf = Arrays.copyOf(buf, Math.max(buf.length << 1, buf.length + newcount));
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
    void writeBytes(byte[] bytes, int offset, int len) {
        if (len == 0) return;
        String str = new String(bytes, offset, len);
        // reset len maybe utf8 bytes
        len = str.length();
        ensureCapacity(len);
        str.getChars(0, len, buf, count);
        count += len;
    }

    @Override
    public void write(String str, int off, int len) {
        if (len == 0) return;
        ensureCapacity(len);
        str.getChars(off, off + len, buf, count);
        count += len;
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
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
        // buf count -> bytes
        throw new UnsupportedOperationException();
    }

    // JDK9+
    @Override
    protected byte[] toBytes(Charset charset) {
        String source = new String(buf, 0, count);
        byte[] bytes = (byte[]) UnsafeHelper.getStringValue(source);
        if (bytes.length == count) {
            return bytes;
        } else {
            if (charset == EnvUtils.UTF_8) {
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
    public String toString() {
        return new String(buf, 0, count);
    }

    public int size() {
        return count;
    }

    public void clear() {
        count = 0;
    }

    /**
     * 写入短字符数组
     *
     * @param chars
     * @param offset
     * @param len
     */
    @Override
    final void writeShortChars(char[] chars, int offset, int len) {
        ensureCapacity(len);
        // 这里使用临时变量count的诡异之处
        int count = this.count;
        for (int i = 0; i < len; ++i) {
            buf[count++] = chars[offset++];
        }
        this.count = count;
    }

    void writeShortJSONChars(char[] chars) {
        int len = chars.length;
        ensureCapacity(len * 6);
        int count = this.count;
        buf[count++] = '"';
        for (char ch : chars) {
            String escapeStr;
            if ((ch > '"' && ch != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[ch]) == null) {
                buf[count++] = ch;
                continue;
            }
            int escapesLen = escapeStr.length();
            for (int j = 0; j < escapesLen; ++j) {
                buf[count++] = escapeStr.charAt(j);
            }
        }
        buf[count++] = '"';
        this.count = count;
    }

//    @Override
//    public void writeShortLatinJSONBytes(byte[] bytes) {
//        int len = bytes.length;
//        ensureCapacity(len * 6);
//        int count = this.count;
//        // ascii
//        buf[count++] = '"';
//        for (byte b : bytes) {
//            String escapeStr;
//            if ((b > '"' && b != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[b & 0xFF]) == null) {
//                buf[count++] = (char) b;
//                continue;
//            }
//            for (int j = 0; j < escapeStr.length(); ++j) {
//                buf[count++] = escapeStr.charAt(j);
//            }
//        }
//        buf[count++] = '"';
//        this.count = count;
//    }

//    @Override
//    public void writeLatinBytes(String source, byte[] bytes, int offset, int len) {
////        ensureCapacity(len);
////        int count = this.count;
////        for (int i = offset, end = offset + len; i < end; ++i) {
////            buf[count++] = (char) bytes[i];
////        }
////        this.count = count;
//        write(source, offset, len);
//    }

    @Override
    public void writeLong(long numValue) throws IOException {
        if (numValue == 0) {
            ensureCapacity(1);
            buf[count++] = '0';
            return;
        }
        ensureCapacity(20);
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
    void writeUUID(UUID uuid) {
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();
        ensureCapacity(38);
        int off = count;
        buf[off++] = '"';
        off += NumberUtils.writeUUIDMostSignificantBits(mostSigBits, buf, off);
        off += NumberUtils.writeUUIDLeastSignificantBits(leastSigBits, buf, off);
        buf[off++] = '"';
        count = off;
    }

    @Override
    public void writeDouble(double numValue) {
        ensureCapacity(24);
        count += NumberUtils.writeDouble(numValue, buf, count);
    }

    @Override
    public void writeFloat(float numValue) {
        ensureCapacity(24);
        count += NumberUtils.writeFloat(numValue, buf, count);
    }

    @Override
    public void writeLocalDateTime(int year, int month, int day, int hour, int minute, int second, int nano) {
        ensureCapacity(30);
        int off = count;
        if (year < 0) {
            buf[off++] = '-';
            year = -year;
        }
        if (year < 10000) {
            off += NumberUtils.writeFourDigits(year, buf, off);
        } else {
            off += NumberUtils.writePositiveLong(year, buf, off);
        }
        off += NumberUtils.writeTwoDigitsAndPreSuffix(month, '-', '-', buf, off);
        off += NumberUtils.writeTwoDigits(day, buf, off);
        off += NumberUtils.writeTwoDigitsAndPreSuffix(hour, 'T', ':', buf, off);
        off += NumberUtils.writeTwoDigits(minute, buf, off);
        buf[off++] = ':';
        off += NumberUtils.writeTwoDigits(second, buf, off);
        if (nano > 0) {
            nano += 1000000000;
            while (nano % 1000 == 0) {
                nano = nano / 1000;
            }
            int pointIndex = off;
            off += NumberUtils.writePositiveLong(nano, buf, off);
            buf[pointIndex] = '.';
        }
        count = off;
    }

    @Override
    public void writeLocalDate(int year, int month, int day) {
        ensureCapacity(10 + 1);
        int off = count;
        if (year < 0) {
            buf[off++] = '-';
            year = -year;
        }
        if (year < 10000) {
            off += NumberUtils.writeFourDigits(year, buf, off);
        } else {
            off += NumberUtils.writePositiveLong(year, buf, off);
        }
        off += NumberUtils.writeTwoDigitsAndPreSuffix(month, '-', '-', buf, off);
        off += NumberUtils.writeTwoDigits(day, buf, off);
        count = off;
    }

    @Override
    public void writeTime(int hourOfDay, int minute, int second) {
        ensureCapacity(10);
        int off = count;
        off += NumberUtils.writeTwoDigits(hourOfDay, buf, off);
        off += NumberUtils.writeTwoDigitsAndPreSuffix(minute, ':', ':', buf, off);
        off += NumberUtils.writeTwoDigits(second, buf, off);
        count = off;
    }

    @Override
    public void writeTimeWithNano(int hourOfDay, int minute, int second, int nano) {
        ensureCapacity(20);
        int off = count;
        off += NumberUtils.writeTwoDigits(hourOfDay, buf, off);
        off += NumberUtils.writeTwoDigitsAndPreSuffix(minute, ':', ':', buf, off);
        off += NumberUtils.writeTwoDigits(second, buf, off);
        if (nano > 0) {
            nano += 1000000000;
            while (nano % 1000 == 0) {
                nano = nano / 1000;
            }
            int pointIndex = off;
            off += NumberUtils.writePositiveLong(nano, buf, off);
            buf[pointIndex] = '.';
        }
        count = off;
    }

    @Override
    public void writeDate(int year, int month, int day, int hourOfDay, int minute, int second) {
        ensureCapacity(24);
        int off = count;
        if (year < 0) {
            buf[off++] = '-';
            year = -year;
        }
        if (year < 10000) {
            off += NumberUtils.writeFourDigits(year, buf, off);
        } else {
            off += NumberUtils.writePositiveLong(year, buf, off);
        }
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
        ensureCapacity(increment);
        count += NumberUtils.writeBigInteger(bigInteger, buf, count);
    }

    @Override
    void writeJSONStringKey(String value) {
        int length = value.length();
        ensureCapacity(length + 3);
        int off = count;
        buf[off++] = '"';
        if (!EnvUtils.JDK_9_PLUS) {
            char[] chars = UnsafeHelper.getChars(value);
            for (char c : chars) {
                buf[off++] = c;
            }
        } else {
            value.getChars(0, length, buf, off);
            off += length;
        }
        buf[off++] = '"';
        buf[off++] = ':';
        count = off;
    }

    @Override
    public void writeJSONChars(char[] chars) throws IOException {
        int len = chars.length;
        if (len <= 64) {
            writeShortJSONChars(chars);
        } else {
            ensureCapacity(len * 6 + 2);
            int count = this.count, beginIndex = 0;
            buf[count++] = '"';
            for (int i = 0; i < len; ++i) {
                char ch = chars[i];
                String escapeStr;
                if ((ch > '"' && ch != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[ch]) == null) continue;
                int length = i - beginIndex;
                // 很诡异的问题
                if (length > 0) {
                    System.arraycopy(chars, beginIndex, buf, count, length);
                    count += length;
                }
                for (int j = 0; j < escapeStr.length(); ++j) {
                    buf[count++] = escapeStr.charAt(j);
                }
                beginIndex = i + 1;
            }
            int length = len - beginIndex;
            System.arraycopy(chars, beginIndex, buf, count, length);
            count += length;
            buf[count++] = '"';
            this.count = count;
        }
    }

    //    @Override
//    public void writeLongJSONChars(char[] chars) throws IOException {
//        int len = chars.length;
//        ensureCapacity(len * 6 + 2);
//        int count = this.count, beginIndex = 0;
//        buf[count++] = '"';
//        for (int i = 0; i < len; ++i) {
//            char ch = chars[i];
//            String escapeStr;
//            if ((ch > '"' && ch != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[ch]) == null) continue;
//            int length = i - beginIndex;
//            // 很诡异的问题
//            if (length > 0) {
//                System.arraycopy(chars, beginIndex, buf, count, length);
//                count += length;
//            }
//            for (int j = 0 ; j < escapeStr.length(); ++j) {
//                buf[count++] = escapeStr.charAt(j);
//            }
//            beginIndex = i + 1;
//        }
//        int length = len - beginIndex;
//        System.arraycopy(chars, beginIndex, buf, count, length);
//        count += length;
//        buf[count++] = '"';
//        this.count = count;
//    }

    @Override
    public void writeLatinJSONString(String value, byte[] bytes) throws IOException {
        int len = bytes.length;
        ensureCapacity(len * 6 + 2);
        int count = this.count;
        buf[count++] = '"';
        int beginIndex = 0;
        for (int i = 0; i < len; ++i) {
            byte b = bytes[i];
            String escapeStr;
            if ((escapeStr = JSONGeneral.ESCAPE_VALUES[b & 0xFF]) != null) {
                int length = i - beginIndex;
                if(length > 0) {
                    value.getChars(beginIndex, i, buf, count);
                    count += length;
                }
                int escapeLen = escapeStr.length();
                escapeStr.getChars(0, escapeLen, buf, count);
                count += escapeLen;
                beginIndex = i + 1;
            }
        }
        int length = len - beginIndex;
        if(length > 0) {
            value.getChars(beginIndex, len, buf, count);
            count += length;
        }
        buf[count++] = '"';
        this.count = count;
    }

    //    @Override
//    public void writeShortBytes(String source, byte[] bytes) {
//        write(source, 0, source.length());
//    }

    void clearCache() {
        if (charBufCache != null) {
            if (buf.length <= MAX_CACHE_BUFFER_SIZE) {
                charBufCache.cacheChars = buf;
            }
            charBufCache.inUse = false;
            // getOrReturnCache(charBufCache);
            charBufCache = null;
        }
    }

    void reset() {
        clear();
        clearCache();
        buf = EMPTY_BUF;
    }
}
