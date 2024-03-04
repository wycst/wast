package io.github.wycst.wast.common.tools;

/**
 * FNV-1a Hash
 *
 * @Author wangyunchao
 * @Date 2023/5/18 21:38
 */
public class FNV {
    public static final long FNV_64_OFFSET_BASIS = 0xcbf29ce484222325L;
    public static final long FNV_64_PRIME = 0x100000001b3L;

//    public static final long FNV_32_OFFSET_BASIS = 0x811c9dc5;
//    public static final long FNV_32_PRIME = 0x1000193;

//    public static long hash64(char[] chars) {
//        long rv = FNV_64_OFFSET_BASIS;
//        int len = chars.length;
//        for (int i = 0; i < len; i++) {
//            rv ^= chars[i];
//            rv *= FNV_64_PRIME;
//        }
//        return rv;
//    }
//
//    public static long hash32(char[] chars) {
//        long rv = FNV_32_OFFSET_BASIS;
//        int len = chars.length;
//        for (int i = 0; i < len; i++) {
//            rv ^= chars[i];
//            rv *= FNV_32_PRIME;
//        }
//        return rv;
//    }

    public static long hash64(String chars) {
        long rv = FNV_64_OFFSET_BASIS;
        int len = chars.length();
        for (int i = 0; i < len; i++) {
            rv ^= chars.charAt(i);
            rv *= FNV_64_PRIME;
        }
        return rv;
    }

    public static long hash64(long rv, String chars) {
        int len = chars.length();
        for (int i = 0; i < len; i++) {
            rv ^= chars.charAt(i);
            rv *= FNV_64_PRIME;
        }
        return rv;
    }

//    public static long hash32(String chars) {
//        long rv = FNV_32_OFFSET_BASIS;
//        int len = chars.length();
//        for (int i = 0; i < len; i++) {
//            rv ^= chars.charAt(i);
//            rv *= FNV_32_PRIME;
//        }
//        return rv;
//    }
//
//    public static long hash32(long rv, String chars) {
//        int len = chars.length();
//        for (int i = 0; i < len; i++) {
//            rv ^= chars.charAt(i);
//            rv *= FNV_32_PRIME;
//        }
//        return rv;
//    }

    public static long hash64(long rv, int ch) {
        rv ^= ch;
        rv *= FNV_64_PRIME;
        return rv;
    }

//    public static long hash32(long rv, int ch) {
//        rv ^= ch;
//        rv *= FNV_32_PRIME;
//        return rv;
//    }
}
