package io.github.wycst.wast.common.utils;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public final class EnvUtils {

    public static final float JDK_VERSION;

    public static final boolean JDK_16_PLUS;
    public static final boolean JDK_9_PLUS;
    public static final boolean JDK_20_PLUS;

    public static final boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    public static final int HI_BYTE_SHIFT;
    public static final int LO_BYTE_SHIFT;

    // 'java.lang.String' hashcode
    public final static int STRING_HV = 1195259493;
    // 'int' hash
    public final static int INT_HV = 104431;
    // 'java.lang.Integer' hash
    public final static int INTEGER_HV = -2056817302;
    // 'long'
    public final static int LONG_PRI_HV = 3327612;
    // 'java.lang.Long'
    public final static int LONG_HV = 398795216;
    // 'java.util.HashMap'
    public final static int HASHMAP_HV = -1402722386;
    // 'java.util.LinkHashMap'
    public final static int LINK_HASHMAP_HV = 1258621781;

    // 'java.util.ArrayList'
    public final static int ARRAY_LIST_HV = -1114099497;
    // 'java.util.HashSet'
    public final static int HASH_SET_HV = -1402716492;

    public final static Charset CHARSET_DEFAULT = Charset.defaultCharset();
    public final static Charset CHARSET_ISO_8859_1 = forCharsetName("ISO_8859_1");
    public final static Charset CHARSET_UTF_8 = forCharsetName("UTF-8");

    public static final Method SC_HAS_NEGATIVES_METHOD;
//    public static final Method SL_INDEX_OF_METHOD;

    static {
        float jdkVersion = 1.8f;
        try {
            // 规范版本号
            String version = System.getProperty("java.specification.version");
            if (version != null) {
                jdkVersion = Float.parseFloat(version);
            }
        } catch (Throwable throwable) {
        }
        JDK_VERSION = jdkVersion;
        JDK_9_PLUS = JDK_VERSION >= 9;
        JDK_16_PLUS = JDK_VERSION >= 16;
        JDK_20_PLUS = JDK_VERSION >= 20;

        if (BIG_ENDIAN) {
            HI_BYTE_SHIFT = 8;
            LO_BYTE_SHIFT = 0;
        } else {
            HI_BYTE_SHIFT = 0;
            LO_BYTE_SHIFT = 8;
        }

        Method scHasNegatives = null;
        if (JDK_9_PLUS) {
            try {
                Class<?> scClass = Class.forName("java.lang.StringCoding");
                scHasNegatives = scClass.getMethod("hasNegatives", new Class[]{byte[].class, int.class, int.class});
                UnsafeHelper.setAccessible(scHasNegatives);
            } catch (Exception e) {
                scHasNegatives = null;
            }
        }
        SC_HAS_NEGATIVES_METHOD = scHasNegatives;

//        Method slIndexOfMethod = null;
//        if (JDK_9_PLUS) {
//            try {
//                // public static int indexOf(byte[] value, int valueCount, byte[] str, int strCount, int fromIndex)
//                Class<?> scClass = Class.forName("java.lang.StringLatin1");
//                slIndexOfMethod = scClass.getMethod("indexOf", new Class[]{byte[].class, int.class, byte[].class, int.class, int.class});
//                UnsafeHelper.setAccessible(slIndexOfMethod);
//            } catch (Exception e) {
//                slIndexOfMethod = null;
//            }
//        }
//        SL_INDEX_OF_METHOD = slIndexOfMethod;
    }

    private static Charset forCharsetName(String charsetName) {
        try {
            return Charset.forName(charsetName);
        } catch (Throwable throwable) {
            return null;
        }
    }

    public static final long NEGATIVE_MASK = 0x8080808080808080L;

    public static boolean hasNegatives(byte[] bytes, int offset, int len) {
        try {
            if (SC_HAS_NEGATIVES_METHOD != null) {
                return (Boolean) SC_HAS_NEGATIVES_METHOD.invoke(null, bytes, offset, len);
            }
        } catch (Exception e) {
        }
        if (len > 7) {
            do {
                long val = UnsafeHelper.getLong(bytes, offset);
                if ((val & NEGATIVE_MASK) != 0) return true;
                offset += 8;
                len -= 8;
            } while (len > 7);
            if (len == 0) return false;
            return (UnsafeHelper.getLong(bytes, offset + len - 8) & NEGATIVE_MASK) != 0;
        } else {
            for (int i = offset, end = offset + len; i < end; ++i) {
                if (bytes[i] < 0) return true;
            }
            return false;
        }
    }
}
