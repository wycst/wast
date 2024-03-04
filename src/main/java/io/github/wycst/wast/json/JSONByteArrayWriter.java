//package io.github.wycst.wast.json;
//
//import io.github.wycst.wast.common.reflect.UnsafeHelper;
//import io.github.wycst.wast.common.utils.IOUtils;
//
//import java.io.IOException;
//import java.io.Writer;
//import java.nio.ByteOrder;
//import java.util.Arrays;
//
///**
// * Override removes the method block in writer that uses synchronous security
// * Partial optimization
// *
// * @Author: wangy
// * @Date: 2021/12/22 21:48
// * @Description:
// */
//public class JSONByteArrayWriter extends JSONStringWriter {
//
//    // 缓冲字符数组
//    byte[] buf;
//
//    // 长度
//    int count;
//
//    boolean latin1;
//
//    int step;
//
//    // EMPTY chars
//    static final byte[] EMPTY_BUF = new byte[0];
//
//    /**
//     * 字符数组缓存池长度
//     * <p>
//     * L2 character array cache pool
//     */
//    final static int CACHE_BYTE_BUFFER_SIZE = 4096;
//
//    static final boolean LE;
//    static final int HI_BYTE_SHIFT;
//    static final int LO_BYTE_SHIFT;
//
//    static {
//        // JDK9+ 可以直接使用地址判断
//        LE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
//        if (LE) {
//            HI_BYTE_SHIFT = 0;
//            LO_BYTE_SHIFT = 8;
//        } else {
//            HI_BYTE_SHIFT = 8;
//            LO_BYTE_SHIFT = 0;
//        }
//    }
//
//    /**
//     * The current instance uses a cached instance
//     */
//    private ByteBufCache byteBufCache;
//
//    private static class ByteBufCache {
//        final byte[] cacheBytes;
//        boolean inUse;
//
//        ByteBufCache(byte[] cacheBytes) {
//            this.cacheBytes = cacheBytes;
//        }
//    }
//
//    // per thread use 4kb
//    static ThreadLocal<ByteBufCache> LOCAL_BUF = new ThreadLocal<ByteBufCache>() {
//        @Override
//        protected ByteBufCache initialValue() {
//            return new ByteBufCache(new byte[CACHE_BYTE_BUFFER_SIZE]);
//        }
//    };
//
//    JSONByteArrayWriter() {
//        ByteBufCache byteBufCache = LOCAL_BUF.get();
//        if (byteBufCache.inUse) {
//            // 如果发生嵌套使用直接new构建（自定义序列化中又调用的序列化）
//            buf = new byte[32];
//        } else {
//            // 安全处理
//            byteBufCache.inUse = true;
//            this.byteBufCache = byteBufCache;
//            buf = byteBufCache.cacheBytes;
//        }
//        // default latin1 = true
//        latin1 = true;
//        step = 1;
//    }
//
//    public Writer append(char c) {
//        write(c);
//        return this;
//    }
//
//    @Override
//    public void write(int c) {
//        int newcount = count + step ;
//        if (newcount > buf.length) {
//            expandCapacity(Math.max(buf.length << 1, newcount));
//        }
//        if(latin1) {
//            buf[count++] = (byte) c;
//        } else {
//            buf[count++] = (byte) (c >> HI_BYTE_SHIFT);
//            buf[count++] = (byte) (c >> LO_BYTE_SHIFT);
//        }
//    }
//
//    /**
//     * 获取内部字符数组
//     *
//     * @return
//     */
//    byte[] internal() {
//        return buf;
//    }
//
//    /**
//     *
//     * @param c
//     *         Array of characters (ascii chars)
//     *
//     * @param off
//     *         Offset from which to start writing characters
//     *
//     * @param len
//     *         Number of characters to write
//     *
//     */
//    @Override
//    public void write(char[] c, int off, int len) {
//        if (len == 0) return;
//        int newcount = count + len * step;
//        if (newcount > buf.length) {
//            expandCapacity(Math.max(buf.length << 1, buf.length + newcount));
//        }
//        if(latin1) {
//            for (int i = 0 ; i < len; ++i) {
//                buf[count++] = (byte) c[off++];
//            }
//        } else {
//            // copy c to utf16 bytes
//            UnsafeHelper.copyMemory(c, off, len, buf, count);
//            count = newcount;
//        }
//    }
//
//    void expandCapacity(int capacity) {
//        buf = Arrays.copyOf(buf, capacity);
//    }
//
//    /**
//     * @param str A String encode is asscii
//     * @param off Offset from which to start writing characters
//     * @param len Number of characters to write
//     */
//    @Override
//    public void write(String str, int off, int len) {
//        if (len == 0) return;
//        int newcount = count + len * step;
//        if (newcount > buf.length) {
//            expandCapacity(Math.max(buf.length << 1, buf.length + newcount));
//        }
//        byte[] bytes = (byte[]) UnsafeHelper.getStringValue(str);
//        if(latin1) {
//            System.arraycopy(bytes, off, buf, count, len);
//            count = newcount;
//        } else {
//            for (int i = 0 ; i < len; ++i) {
//                byte c = bytes[off++];
//                buf[count++] = (byte) (c >> HI_BYTE_SHIFT);
//                buf[count++] = (byte) (c >> LO_BYTE_SHIFT);
//            }
//        }
//    }
//
//    @Override
//    public void flush() throws IOException {
//    }
//
//    @Override
//    public void close() throws IOException {
//    }
//
//    StringBuffer toStringBuffer() {
//        throw new UnsupportedOperationException();
//    }
//
//    StringBuilder toStringBuilder() {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public String toString() {
//        byte[] values = Arrays.copyOf(buf, count);
//        if (latin1) {
//            return UnsafeHelper.getAsciiString(values);
//        } else {
//            return UnsafeHelper.getUTF16String(values);
//        }
//    }
//
//    public int size() {
//        return count;
//    }
//
//    public void clear() {
//        count = 0;
//    }
//
//    public void reset() {
//        clear();
//        if (byteBufCache != null) {
//            byteBufCache.inUse = false;
//            byteBufCache = null;
//        }
//        buf = EMPTY_BUF;
//    }
//}
