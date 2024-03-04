package io.github.wycst.wast.common.utils;

import java.nio.ByteOrder;
import java.nio.charset.Charset;

public final class EnvUtils {

    public static final float JDK_VERSION;

    public static final boolean JDK_16_ABOVE;
    public static final boolean JDK_9_ABOVE;

    public static final boolean LE;
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

    public final static Charset ISO_8859_1;

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
        JDK_9_ABOVE = JDK_VERSION >= 9;
        JDK_16_ABOVE = JDK_VERSION >= 16;

        LE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
        if (LE) {
            HI_BYTE_SHIFT = 0;
            LO_BYTE_SHIFT = 8;
        } else {
            HI_BYTE_SHIFT = 8;
            LO_BYTE_SHIFT = 0;
        }

        // ASCII charset
        Charset iso_8859_1 = null;
        try {
            iso_8859_1 = Charset.forName("ISO_8859_1");
//            if(JDK_16_ABOVE) {
//                // jdk17
//                iso_8859_1 = (Charset) UnsafeHelper.getStaticFieldValue("sun.nio.cs.ISO_8859_1", "INSTANCE");
//            } else if(JDK_9_ABOVE) {
//
//            }
        } catch (Throwable throwable) {
        }
        ISO_8859_1 = iso_8859_1;
    }
}
