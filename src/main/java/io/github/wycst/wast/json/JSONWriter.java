package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.common.utils.NumberUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class JSONWriter extends Writer {

    final static int CACHE_BUFFER_SIZE;
    final static int MAX_CACHE_BUFFER_SIZE;
    final static int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    // Safe skip over boundary check space
    final static int SECURITY_UNCHECK_SPACE = 128;
    final static int CACHE_COUNT;
    final static AtomicInteger AUTO_SEQ = new AtomicInteger();

    static {
        // init memory:  16KB * 2 * 8 -> 256KB
        CACHE_BUFFER_SIZE = EnvUtils.JDK_VERSION >= 1.8f ? 1 << 14 : 1 << 12;
        // max memory:   3MB * 2 * 16 -> 96MB
        MAX_CACHE_BUFFER_SIZE = (1 << 20) * 3;

        int availableProcessors = AVAILABLE_PROCESSORS << 1;
        int cacheCount = 16;
        while (availableProcessors > cacheCount) {
            cacheCount <<= 1;
        }
        CACHE_COUNT = cacheCount;
    }

    JSONWriter() {
    }

    static ThreadLocal<Integer> THREAD_CACHE_INDEX = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return AUTO_SEQ.incrementAndGet() & CACHE_COUNT - 1;
        }

        // disable update
        @Override
        public void set(Integer value) {
        }
    };

    static JSONWriter forStringWriter() {
        return new JSONCharArrayWriter();
    }

    /**
     * only use for toJsonBytes
     *
     * @param charset
     * @return
     */
    static JSONWriter forBytesWriter(Charset charset) {
        return new JSONByteArrayWriter(charset);
//        if (EnvUtils.JDK_9_PLUS) {
//            return new JSONCharArrayWriter();
//        } else {
//            // The efficiency of using character stream writing is extremely low when outputting as a byte array for jdk8 and below
//            return new JSONByteArrayWriter(charset);
//        }
    }

    static JSONWriter forStreamWriter(Charset charset) {
        if (EnvUtils.JDK_9_PLUS) {
            return new JSONCharArrayStreamWriter(charset);
        } else {
            // The efficiency of using character stream writing is extremely low when outputting as a byte array for jdk8 and below
            return new JSONByteArrayWriter(charset);
        }
    }

    static JSONWriter wrap(Writer writer) {
        return JSONWrapWriter.wrap(writer);
    }

    public abstract String toString();

    abstract StringBuffer toStringBuffer();

    abstract StringBuilder toStringBuilder();

    protected byte[] toBytes() {
        return toBytes(Charset.defaultCharset());
    }

    protected abstract void toOutputStream(OutputStream os) throws IOException;

    protected byte[] toBytes(Charset charset) {
        return toString().getBytes();
    }

    void reset() {
    }

    public void clear() {
    }

    final static void writeNano(int nano, Writer content) throws IOException {
        if (nano > 0) {
            content.write('.');
            int stringSize = NumberUtils.stringSize(nano);
            int n = 9 - stringSize;
            while (n > 0) {
                content.write('0');
                --n;
            }
            while (nano % 1000 == 0) {
                nano = nano / 1000;
            }
            NumberUtils.writePositiveLong(nano, content);
        }
    }

    /**
     * write zoneId(zoneOffset)
     *
     * @param zoneId
     * @throws IOException
     */
    public final void writeZoneId(String zoneId) throws IOException {
        if (zoneId == null) return;
        // zoneID
        if (zoneId.length() > 0) {
            char c = zoneId.charAt(0);
            switch (c) {
                case 'Z': {
                    writeJSONToken('Z');
                    break;
                }
                case '+':
                case '-': {
                    write(zoneId);
                    break;
                }
                default: {
                    write('[');
                    write(zoneId);
                    write(']');
                }
            }
        }
    }

    /**
     * Ensure the call is secure.
     *
     * @param c
     * @throws IOException
     * @throws IndexOutOfBoundsException
     */
    public void writeJSONToken(char c) throws IOException {
        write(c);
    }

    public abstract void writeShortChars(char[] chars, int offset, int len) throws IOException;

    public abstract void writeLong(long numValue) throws IOException;

    public abstract void writeUUID(UUID uuid) throws IOException;

    public abstract void writeDouble(double numValue) throws IOException;

    public abstract void writeFloat(float numValue) throws IOException;

    public final void writeJSONInstant(long epochSeconds, int nanos) throws IOException {
        GeneralDate generalDate = new GeneralDate(epochSeconds * 1000, JSONGeneral.ZERO_TIME_ZONE);
        int year = generalDate.getYear();
        int month = generalDate.getMonth();
        int day = generalDate.getDay();
        int hour = generalDate.getHourOfDay();
        int minute = generalDate.getMinute();
        int second = generalDate.getSecond();
        writeJSONLocalDateTime(year, month, day, hour, minute, second, nanos, "Z");
    }

    /**
     * format "yyyy-MM-ddTHH:mm:ss.SSSSSSSSS"
     * format "yyyy-MM-ddTHH:mm:ss.SSSSSSSSSZ"
     * format "yyyy-MM-ddTHH:mm:ss.SSSSSSSSS+08:00"
     *
     * @param year
     * @param month
     * @param day
     * @param hour
     * @param minute
     * @param second
     * @param nano
     * @param zoneId
     * @throws IOException
     */
    public abstract void writeJSONLocalDateTime(int year, int month, int day, int hour, int minute, int second, int nano, String zoneId) throws IOException;

    /**
     * write default format "yyyy-MM-dd"
     *
     * @param year
     * @param month
     * @param day
     * @throws IOException
     */
    public abstract void writeJSONLocalDate(int year, int month, int day) throws IOException;

    /**
     * HH:mm:ss
     *
     * @param hourOfDay
     * @param minute
     * @param second
     * @throws IOException
     */
    public abstract void writeTime(int hourOfDay, int minute, int second) throws IOException;

    /**
     * "HH:mm:ss.SSSS"
     *
     * @param hourOfDay
     * @param minute
     * @param second
     * @param nano
     * @throws IOException
     */
    public abstract void writeJSONTimeWithNano(int hourOfDay, int minute, int second, int nano) throws IOException;

    /**
     * yyyy-MM-dd HH:mm:ss
     *
     * @param year
     * @param month
     * @param day
     * @param hourOfDay
     * @param minute
     * @param second
     * @throws IOException
     */
    public abstract void writeDate(int year, int month, int day, int hourOfDay, int minute, int second) throws IOException;

    public abstract void writeBigInteger(BigInteger bigInteger) throws IOException;

    // only for map key
    public final void writeJSONStringKey(String value) throws IOException {
        writeJSONString(value);
        writeJSONToken(':');
    }

    public void writeJSONChars(char[] chars) throws IOException {
        int beginIndex = 0, len = chars.length;
        write('"');
        for (int i = 0; i < len; ++i) {
            char ch = chars[i];
            String escapeStr;
            if ((ch > '"' && ch != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[ch]) == null) continue;
            int length = i - beginIndex;
            // 很诡异的问题
            if (length > 0) {
                write(chars, beginIndex, length);
            }
            write(escapeStr);
            beginIndex = i + 1;
        }
        int size = len - beginIndex;
        write(chars, beginIndex, size);
        write('"');
    }

//    public void writeLongJSONChars(char[] chars) throws IOException {
//        int beginIndex = 0, len = chars.length;
//        write('"');
//        for (int i = 0; i < len; ++i) {
//            char ch = chars[i];
//            String escapeStr;
//            if ((ch > '"' && ch != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[ch]) == null) continue;
//            int length = i - beginIndex;
//            // 很诡异的问题
//            if (length > 0) {
//                write(chars, beginIndex, length);
//            }
//            write(escapeStr);
//            beginIndex = i + 1;
//        }
//        int size = len - beginIndex;
//        write(chars, beginIndex, size);
//        write('"');
//    }

    /**
     * @param value
     * @throws IOException
     */
    public final void writeJSONString(String value) throws IOException {
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) UnsafeHelper.getStringValue(value);
            writeJSONStringBytes(value, bytes);
        } else {
            writeJSONChars(UnsafeHelper.getChars(value));
        }
    }

    /**
     * When the bytes are determined, one reflection can be reduced
     *
     * @param value
     * @param bytes
     * @throws IOException
     */
    public final void writeJSONStringBytes(String value, byte[] bytes) throws IOException {
        if (bytes.length == value.length()) {
            writeLatinJSONString(value, bytes);
        } else {
            writeUTF16JSONString(value, bytes);
        }
    }

    public void writeLatinJSONString(String value, byte[] bytes) throws IOException {
        int len = bytes.length;
        write('"');
        int beginIndex = 0;
        for (int i = 0; i < len; ++i) {
            byte b = bytes[i];
//            if (b > '"' && b != '\\') continue;
            String escapeStr;
            if ((escapeStr = JSONGeneral.ESCAPE_VALUES[b & 0xFF]) != null) {
                write(value, beginIndex, i - beginIndex);
                write(escapeStr);
                beginIndex = i + 1;
            }
        }
        int size = len - beginIndex;
        write(value, beginIndex, size);
        write('"');
    }

    public void writeUTF16JSONString(String value, byte[] bytes) throws IOException {
        write('"');
        int beginIndex = 0, strlen = value.length();
        for (int i = 0; i < strlen; ++i) {
            char b = value.charAt(i);
            if (b > '"' && b != '\\') continue;
            String escapeStr;
            if ((escapeStr = JSONGeneral.ESCAPE_VALUES[b]) != null) {
                write(value, beginIndex, i - beginIndex);
                write(escapeStr);
                beginIndex = i + 1;
            }
        }
        int size = strlen - beginIndex;
        write(value, beginIndex, size);
        write('"');
    }

    public void writeFieldString(String value, int offset, int len) throws IOException {
        write(value, offset, len);
    }

    public final void writeFieldString(String value) throws IOException {
        writeFieldString(value, 0, value.length());
    }

    /**
     * 通过unsafe写入4 * n个字符（4 * n 字节）
     *
     * @param fourChars
     * @param fourBytes
     * @param len
     * @throws IOException
     */
    public void writeUnsafe(long fourChars, int fourBytes, int len) throws IOException {
        char[] chars = new char[4];
        UnsafeHelper.putLong(chars, 0, fourChars);
        write(chars, 0, len);
    }

    /**
     * 通过unsafe写入4 * n个字符（4 * n 字节）
     *
     * @param fourChars
     * @param fourBytes
     * @param totalCount 实际长度
     * @throws IOException
     */
    public void writeUnsafe(long[] fourChars, int[] fourBytes, int totalCount) throws IOException {
        int n = fourChars.length;
        char[] chars = new char[n << 2];
        int offset = 0;
        for (long fourChar : fourChars) {
            UnsafeHelper.putLong(chars, offset, fourChar);
            offset += 4;
        }
        write(chars, 0, totalCount);
    }
}
