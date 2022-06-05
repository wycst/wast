package org.framework.light.json;

import java.io.CharArrayWriter;
import java.util.Arrays;

/**
 * Override removes the method block in chararraywriter that uses synchronous security
 * Partial optimization
 *
 * @Author: wangy
 * @Date: 2021/12/22 21:48
 * @Description:
 */
public class JSONWriter extends CharArrayWriter {

//    /**
//     * 一级缓存本地线程 (L1 cache local thread)
//     * <p>
//     * 每个线程1m的缓存 (About 1MB cache per thread)
//     */
//    private static ThreadLocal<char[]> cacheChars = new ThreadLocal<char[]>();

    /**
     * 二级字符数组缓存池
     * <p>
     * L2 character array cache pool
     */
    private final static int CACHE_CHAR_BUFFER_SIZE = 4096;

    /**
     * Number of caches
     */
    private final static int CACHE_COUNT = 8;

    /**
     * Cache pool
     * Resident memory usage: 32MB - 64MB
     */
    private final static CharBufferCache[] BUFFER_CACHES = new CharBufferCache[CACHE_COUNT];

    /**
     * Current number of available caches
     */
    private static int availableCount = CACHE_COUNT;

    /**
     * The current instance uses a cached instance
     */
    private CharBufferCache charBufferCache;

    void setCharAt(int index, char c) {
        buf[index] = c;
    }

    void getChars(int s, int len, char[] chars, int t) {
        System.arraycopy(buf, s, chars, t, len);
    }

    private static class CharBufferCache {
        final char[] cacheBuffers;
        boolean inUse;
        int index;

        CharBufferCache(char[] cacheBuffers) {
            this.cacheBuffers = cacheBuffers;
        }
    }

    static {
        for (int i = 0; i < CACHE_COUNT; i++) {
            CharBufferCache charArrayCache = new CharBufferCache(new char[CACHE_CHAR_BUFFER_SIZE]);
            charArrayCache.index = i;
            BUFFER_CACHES[i] = charArrayCache;
        }
    }

    /**
     * Get a cache object from the pool
     *
     * @return
     */
    private static CharBufferCache getArrayCache() {
        if (availableCount == 0) {
            return null;
        }
        CharBufferCache charBufferCache = BUFFER_CACHES[--availableCount];
        charBufferCache.inUse = true;
        charBufferCache.index = availableCount;
        return charBufferCache;
    }

    /**
     * Return to cache pool
     *
     * @param charBufferCache
     */
    private static void returnArrayCache(CharBufferCache charBufferCache) {
        charBufferCache.inUse = false;
        CharBufferCache oldCache = BUFFER_CACHES[availableCount];
        int oldIndex = charBufferCache.index;
        BUFFER_CACHES[availableCount] = charBufferCache;
        BUFFER_CACHES[oldIndex] = oldCache;
        // 更新位置
        oldCache.index = oldIndex;
        charBufferCache.index = availableCount++;
    }

    /***
     * Ensure thread safety to access availableCount(Currently available cache)
     *
     * @param charBufferCache
     * @return
     */
    static synchronized CharBufferCache getOrReturnCache(CharBufferCache charBufferCache) {
        if (charBufferCache == null) {
            return getArrayCache();
        } else {
            returnArrayCache(charBufferCache);
        }
        return null;
    }

    JSONWriter() {
        charBufferCache = getOrReturnCache(null); //getArrayCache();
        if (charBufferCache == null) {
            // If no cache is found, use the default

//            char[] chars = cacheChars.get();
//            if (chars == null) {
//                chars = new char[1 << 10];
//                cacheChars.set(chars);
//            }
//            buf = chars;
        } else {
            buf = charBufferCache.cacheBuffers;
        }
    }

    @Override
    public void write(int c) {
        int newcount = count + 1;
        if (newcount > buf.length) {
            expandCapacity(Math.max(buf.length << 1, newcount));
        }
        buf[count] = (char) c;
        count = newcount;
    }

    @Override
    public void write(char[] c, int off, int len) {
        if (len == 0) {
            return;
        }
        int newcount = count + len;
        if (newcount > buf.length) {
            expandCapacity(Math.max(buf.length << 1, buf.length + newcount));
        }
        if (len < 6) {
            int i = off;
            switch (len) {
                case 5: {
                    buf[count++] = c[i++];
                }
                case 4: {
                    buf[count++] = c[i++];
                }
                case 3: {
                    buf[count++] = c[i++];
                }
                case 2: {
                    buf[count++] = c[i++];
                }
                case 1: {
                    buf[count++] = c[i++];
                }
            }
            return;
        }
        System.arraycopy(c, off, buf, count, len);
        count = newcount;
    }

    void expandCapacity(int capacity) {
        buf = Arrays.copyOf(buf, capacity);
        if (charBufferCache != null) {
            getOrReturnCache(charBufferCache);
            charBufferCache = null;
        }
    }

    @Override
    public void write(String str, int off, int len) {
        int newcount = count + len;
        if (newcount > buf.length) {
            expandCapacity(Math.max(buf.length << 1, buf.length + newcount));
        }

        if (len < 5) {
            int i = off;
            switch (len) {
                case 4: {
                    buf[count++] = str.charAt(i++);
                }
                case 3: {
                    buf[count++] = str.charAt(i++);
                }
                case 2: {
                    buf[count++] = str.charAt(i++);
                }
                case 1: {
                    buf[count++] = str.charAt(i++);
                }
            }
            return;
        }
        str.getChars(off, off + len, buf, count);
        count = newcount;
    }

    @Override
    public String toString() {
        return new String(buf, 0, count);
    }

    @Override
    public void reset() {
        super.reset();
        if (charBufferCache != null) {
            getOrReturnCache(charBufferCache);
            charBufferCache = null;
        }
    }
}
