package io.github.wycst.wast.json;

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
public class JSONStringWriter extends CharArrayWriter {

    /**
     * 字符数组缓存池长度
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
     * Resident memory usage: 32KB - 64KB
     */
    private final static CharBufCache[] BUF_CACHES = new CharBufCache[CACHE_COUNT];

    /**
     * Current number of available caches
     */
    private static int availableCount = CACHE_COUNT;

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

    private static class CharBufCache {
        final char[] cacheBuffers;
        boolean inUse;
        int index;

        CharBufCache(char[] cacheBuffers) {
            this.cacheBuffers = cacheBuffers;
        }
    }

    static {
        for (int i = 0; i < CACHE_COUNT; i++) {
            CharBufCache charArrayCache = new CharBufCache(new char[CACHE_CHAR_BUFFER_SIZE]);
            charArrayCache.index = i;
            BUF_CACHES[i] = charArrayCache;
        }
    }

    /**
     * Get a cache object from the pool
     *
     * @return
     */
    private static CharBufCache getArrayCache() {
        if (availableCount == 0) {
            return null;
        }
        CharBufCache charBufCache = BUF_CACHES[--availableCount];
        charBufCache.inUse = true;
        charBufCache.index = availableCount;
        return charBufCache;
    }

    /**
     * Return to cache pool
     *
     * @param charBufCache
     */
    private static void returnArrayCache(CharBufCache charBufCache) {
        charBufCache.inUse = false;
        CharBufCache oldCache = BUF_CACHES[availableCount];
        int oldIndex = charBufCache.index;
        BUF_CACHES[availableCount] = charBufCache;
        BUF_CACHES[oldIndex] = oldCache;
        // 更新位置
        oldCache.index = oldIndex;
        charBufCache.index = availableCount++;
    }

    /***
     * Ensure thread safety to access availableCount(Currently available cache)
     *
     * @param charBufCache
     * @return
     */
    static synchronized CharBufCache getOrReturnCache(CharBufCache charBufCache) {
        if (charBufCache == null) {
            return getArrayCache();
        } else {
            returnArrayCache(charBufCache);
        }
        return null;
    }

    JSONStringWriter() {
        charBufCache = getOrReturnCache(null); //getArrayCache();
        if (charBufCache != null) {
            buf = charBufCache.cacheBuffers;
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
        if (len == 0) return;
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
        if (charBufCache != null) {
            getOrReturnCache(charBufCache);
            charBufCache = null;
        }
    }

    @Override
    public void write(String str, int off, int len) {
        if(len == 0) return;
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
        if (charBufCache != null) {
            getOrReturnCache(charBufCache);
            charBufCache = null;
        }
    }
}
