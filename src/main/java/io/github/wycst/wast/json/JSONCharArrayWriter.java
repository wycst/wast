package io.github.wycst.wast.json;

import io.github.wycst.wast.common.utils.IOUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * Override removes the method block in writer that uses synchronous security
 * Partial optimization
 *
 * @Author: wangy
 * @Date: 2021/12/22 21:48
 * @Description:
 */
public class JSONCharArrayWriter extends JSONStringWriter {

    // 缓冲字符数组
    char[] buf;

    // 长度
    int count;

    // EMPTY chars
    static final char[] EMPTY_BUF = new char[0];

    /**
     * 字符数组缓存池长度
     * <p>
     * L2 character array cache pool
     */
    private final static int CACHE_CHAR_BUFFER_SIZE = 4096;

    /**
     * The current instance uses a cached instance
     */
    private CharBufCache charBufCache;

    void setCharAt(int index, char c) {
        buf[index] = c;
    }

    void getChars(int s, int len, char[] chars, int t) {
        System.arraycopy(buf, s, chars, t, len);
    }

    char[] toChars() {
        return Arrays.copyOf(buf, count);
    }

    private static class CharBufCache {
        final char[] cacheBuffers;
        boolean inUse;
        CharBufCache(char[] cacheBuffers) {
            this.cacheBuffers = cacheBuffers;
        }
    }

    // per thread use 4kb
    static ThreadLocal<CharBufCache> LOCAL_BUF = new ThreadLocal<CharBufCache>() {
        @Override
        protected CharBufCache initialValue() {
            return new CharBufCache(new char[CACHE_CHAR_BUFFER_SIZE]);
        }
    };

    JSONCharArrayWriter() {
        CharBufCache charBufCache = LOCAL_BUF.get();
        if (charBufCache.inUse) {
            // 如果发生嵌套使用直接new构建（自定义序列化中又调用的序列化）
            buf = new char[32];
        } else {
            // 安全处理
            charBufCache.inUse = true;
            this.charBufCache = charBufCache;
            buf = charBufCache.cacheBuffers;
        }
    }

    public Writer append(char c) {
        write(c);
        return this;
    }

    @Override
    public void write(int c) {
        int newcount = count + 1;
        if (newcount > buf.length) {
            expandCapacity(Math.max(buf.length << 1, newcount));
        }
        buf[count++] = (char) c;
    }

    /**
     * 获取内部字符数组
     *
     * @return
     */
    char[] internal() {
        return buf;
    }

    /**
     * 直接写入一个字符，跳过越界检查
     *
     * @param c
     */
    void writeDirectly(int c) {
        buf[count++] = (char) c;
    }

//    /**
//     * 直接写入一个字符，跳过越界检查
//     *
//     * @param chars
//     */
//    void writeDirectly(char... chars) {
//        int len = chars.length;
//        System.arraycopy(chars, 0, buf, count, len);
//        count += len;
//    }

    @Override
    public void write(char[] c, int off, int len) {
        if (len == 0) return;
        int newcount = count + len;
        if (newcount > buf.length) {
            expandCapacity(Math.max(buf.length << 1, buf.length + newcount));
        }
        System.arraycopy(c, off, buf, count, len);
        count = newcount;
    }

    char[] ensureCapacity(int increment) {
        int newcount = count + increment;
        if (newcount > buf.length) {
            expandCapacity(Math.max(buf.length << 1, buf.length + newcount));
        }
        return buf;
    }

    void expandCapacity(int capacity) {
        buf = Arrays.copyOf(buf, capacity);
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
        int newcount = count + len;
        if (newcount > buf.length) {
            expandCapacity(Math.max(buf.length << 1, buf.length + newcount));
        }
        str.getChars(0, len, buf, count);
        count = newcount;
    }

    /**
     * 将utf编码的字节写入writer（实际上将字节解码为字符）
     * 场景： jdk9以下（jdk9+不要使用，直接使用writeBytes即可）
     *
     * @param bytes
     * @param offset
     * @param len
     */
    public void writeUTFBytes(byte[] bytes, int offset, int len) {
        if (len == 0) return;
        // 越界处理
        int maxCount = count + len;
        if (maxCount > buf.length) {
            expandCapacity(Math.max(buf.length << 1, buf.length + maxCount));
        }
        // 读取utf8
        count = IOUtils.readUTF8Bytes(bytes, offset, len, buf, count);
    }

    /**
     * 写入string use source which contain the bytes
     *
     * @param source
     * @param offset
     * @param len
     */
    void writeString(String source, int offset, int len) {
        if (len == 0) return;
        int newcount = count + len;
        if (newcount > buf.length) {
            expandCapacity(Math.max(buf.length << 1, buf.length + newcount));
        }
        source.getChars(offset, offset + len, buf, count);
        count = newcount;
    }

    @Override
    public void write(String str, int off, int len) {
        if (len == 0) return;
        int newcount = count + len;
        if (newcount > buf.length) {
            expandCapacity(Math.max(buf.length << 1, buf.length + newcount));
        }
        str.getChars(off, off + len, buf, count);
        count = newcount;
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    StringBuffer toStringBuffer() {
        StringBuffer stringBuffer = new StringBuffer(count);
        stringBuffer.append(buf, 0, count);
        return stringBuffer;
    }

    StringBuilder toStringBuilder() {
        StringBuilder stringBuilder = new StringBuilder(count);
        stringBuilder.append(buf, 0, count);
        return stringBuilder;
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

    public void reset() {
        clear();
        if (charBufCache != null) {
            charBufCache.inUse = false;
            charBufCache = null;
        }
        buf = EMPTY_BUF;
    }
}
