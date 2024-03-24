package io.github.wycst.wast.json;

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

    /**
     * <p> L2 character array cache pool
     */
    final static int CACHE_BUFFER_SIZE;
    final static int MAX_CACHE_BUFFER_SIZE;
    final static int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    final static int CACHE_COUNT;
    final static AtomicInteger AUTO_SEQ = new AtomicInteger();

    static {
        CACHE_BUFFER_SIZE = EnvUtils.JDK_VERSION >= 1.8f ? 1 << 14 : 1 << 12;
        MAX_CACHE_BUFFER_SIZE = EnvUtils.JDK_VERSION >= 1.8f ? 1 << 23 : 1 << 21;

        int availableProcessors = AVAILABLE_PROCESSORS << 2;
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

    abstract void writeShortChars(char[] chars, int offset, int len) throws IOException;

    public abstract void writeLong(long numValue) throws IOException;

    abstract void writeUUID(UUID uuid) throws IOException;

    public abstract void writeDouble(double numValue) throws IOException;

    public abstract void writeFloat(float numValue) throws IOException;

    /**
     * "yyyy-MM-ddTHH:mm:ss.SSSSSSSSS"
     *
     * @param year
     * @param month
     * @param day
     * @param hour
     * @param minute
     * @param second
     * @param nano
     * @throws IOException
     */
    public abstract void writeLocalDateTime(int year, int month, int day, int hour, int minute, int second, int nano) throws IOException;

    public abstract void writeLocalDate(int year, int month, int day) throws IOException;

    /**
     * "HH:mm:ss"
     *
     * @param hourOfDay
     * @param minute
     * @param second
     * @throws IOException
     */
    public abstract void writeTime(int hourOfDay, int minute, int second) throws IOException;

    public abstract void writeTimeWithNano(int hourOfDay, int minute, int second, int nano) throws IOException;

    /**
     * "yyyy-MM-dd HH:mm:ss"
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

    abstract void writeJSONStringKey(String value) throws IOException;

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
}
