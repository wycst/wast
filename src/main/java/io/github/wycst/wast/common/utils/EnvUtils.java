package io.github.wycst.wast.common.utils;

import io.github.wycst.wast.common.compiler.MemoryClassLoader;
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

    public static final JdkApiAgent JDK_AGENT_INSTANCE;

    static {
        JdkApiAgent apiAgent = null;
        if (EnvUtils.JDK_9_PLUS) {
            try {
                MemoryClassLoader memoryClassLoader = new MemoryClassLoader();
                Class agentClass = memoryClassLoader.loadClass("io.github.wycst.wast.common.utils.JdkApiAgentJdk9Plus", ByteUtils.hexString2Bytes("CAFEBABE00000035001A0A000400140A001500160700170700180100063C696E69743E010003282956010004436F646501000F4C696E654E756D6265725461626C650100124C6F63616C5661726961626C655461626C65010004746869730100374C696F2F6769746875622F77796373742F776173742F636F6D6D6F6E2F7574696C732F4A646B4170694167656E744A646B39506C75733B01000C6D756C7469706C7948696768010005284A4A294A010001780100014A010001790100156D756C7469706C79486967684B617261747375626101000A536F7572636546696C650100184A646B4170694167656E744A646B39506C75732E6A6176610C000500060700190C000C000D010035696F2F6769746875622F77796373742F776173742F636F6D6D6F6E2F7574696C732F4A646B4170694167656E744A646B39506C757301002D696F2F6769746875622F77796373742F776173742F636F6D6D6F6E2F7574696C732F4A646B4170694167656E7401000E6A6176612F6C616E672F4D617468003100030004000000000003000100050006000100070000002F00010001000000052AB70001B10000000200080000000600010000000700090000000C000100000005000A000B00000001000C000D000100070000004400040005000000061F21B80002AD0000000200080000000600010000000A000900000020000300000006000A000B000000000006000E000F0001000000060010000F000300010011000D000100070000004400040005000000061F21B80002AD0000000200080000000600010000000E000900000020000300000006000A000B000000000006000E000F0001000000060010000F000300010012000000020013"));
                apiAgent = (JdkApiAgent) UnsafeHelper.getUnsafe().allocateInstance(agentClass);
            } catch (Throwable throwable) {
            }
        }
        if (apiAgent == null) {
            apiAgent = new JdkApiAgent() {
            };
        }
        JDK_AGENT_INSTANCE = apiAgent;
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
