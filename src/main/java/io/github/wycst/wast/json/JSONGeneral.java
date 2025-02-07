package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.DateTemplate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.compiler.MemoryClassLoader;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.ByteUtils;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.common.utils.NumberUtils;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author wangyunchao
 * @Date 2021/12/7 19:30
 */
class JSONGeneral {

    // Format output character pool
    static final char[] FORMAT_OUT_SYMBOL_TABS = "\n\t\t\t\t\t\t\t\t\t\t".toCharArray();
    static final char[] FORMAT_OUT_SYMBOL_SPACES = new char[32];
    static final int FONT_INDENT2_INT32 = EnvUtils.BIG_ENDIAN ? '\n' << 16 | '\t' : '\t' << 16 | '\n';
    static final short FONT_INDENT2_INT16 = EnvUtils.BIG_ENDIAN ? (short) ('\n' << 8 | '\t') : (short) ('\t' << 8 | '\n');
    static final int FOTT_INDENT2_INT32 = '\t' << 16 | '\t';
    static final short FOTT_INDENT2_INT16 = (short) ('\t' << 8 | '\t');
    static final long FO_INDENT4_INT64 = EnvUtils.BIG_ENDIAN ? ((long) FONT_INDENT2_INT32) << 32 | FOTT_INDENT2_INT32 : ((long) FOTT_INDENT2_INT32) << 32 | FONT_INDENT2_INT32;
    static final int FO_INDENT4_INT32 = EnvUtils.BIG_ENDIAN ? FONT_INDENT2_INT16 << 16 | FOTT_INDENT2_INT16 : FOTT_INDENT2_INT16 << 16 | FONT_INDENT2_INT16;

    static {
        Arrays.fill(FORMAT_OUT_SYMBOL_SPACES, ' ');
    }

    protected final static char[] EMPTY_ARRAY = new char[]{'[', ']'};
    protected final static char[] EMPTY_OBJECT = new char[]{'{', '}'};
    protected final static int TRUE_INT = JSONUnsafe.getInt(new byte[]{'t', 'r', 'u', 'e'}, 0);
    protected final static long TRUE_LONG = JSONUnsafe.getLong(new char[]{'t', 'r', 'u', 'e'}, 0);
    protected final static int ALSE_INT = JSONUnsafe.getInt(new byte[]{'a', 'l', 's', 'e'}, 0);
    protected final static long ALSE_LONG = JSONUnsafe.getLong(new char[]{'a', 'l', 's', 'e'}, 0);

    protected final static int NULL_INT = JSONUnsafe.getInt(new byte[]{'n', 'u', 'l', 'l'}, 0);
    protected final static long NULL_LONG = JSONUnsafe.getLong(new char[]{'n', 'u', 'l', 'l'}, 0);

    protected final static byte ZERO = 0;
    protected final static byte COMMA = ',';
    protected final static byte DOUBLE_QUOTATION = '"';
    protected final static byte COLON_SIGN = ':';
    protected final static byte END_ARRAY = ']';
    protected final static byte END_OBJECT = '}';
    protected final static byte WHITE_SPACE = ' ';
    protected final static byte ESCAPE_BACKSLASH = '\\';
    protected final static long DOUBLE_QUOTE_MASK = 0xDDDDDDDDDDDDDDDDL;
    protected final static long SINGLE_QUOTE_MASK = 0xD8D8D8D8D8D8D8D8L;
//    protected final static long BACKSLASH_MASK = 0xA3A3A3A3A3A3A3A3L;

    final static int TYPE_BIGDECIMAL = 1;
    final static int TYPE_BIGINTEGER = 2;
    final static int TYPE_FLOAT = 3;
    final static int TYPE_DOUBLE = 4;

    final static String[] ESCAPE_VALUES = new String[256];
    final static boolean[] NO_ESCAPE_FLAGS = new boolean[256];

    final static String MONTH_ABBR[] = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    final static int[] ESCAPE_CHARS = new int[160];

    // cache keys
    static final JSONKeyValueMap<String> KEY_32_TABLE = new JSONKeyValueMap<String>(4096);
    static final JSONKeyValueMap<String> KEY_8_TABLE = new JSONKeyValueMap<String>(2048);

    static {
        for (int i = 0; i < 160; i++) {
            ESCAPE_CHARS[i] = i;
        }
        ESCAPE_CHARS['n'] = '\n';
        ESCAPE_CHARS['r'] = '\r';
        ESCAPE_CHARS['t'] = '\t';
        ESCAPE_CHARS['b'] = '\b';
        ESCAPE_CHARS['f'] = '\f';
        ESCAPE_CHARS['u'] = -1;
    }

    public final static int DIRECT_READ_BUFFER_SIZE = 8192;
    final static Map<String, TimeZone> GMT_TIME_ZONE_MAP = new ConcurrentHashMap<String, TimeZone>();

    // zero zone
    public final static TimeZone ZERO_TIME_ZONE = TimeZone.getTimeZone("GMT+00:00");
    // 36
    final static ThreadLocal<char[]> CACHED_CHARS_36 = new ThreadLocal<char[]>() {
        @Override
        protected char[] initialValue() {
            return new char[36];
        }
    };

    final static ThreadLocal<double[]> DOUBLE_ARRAY_TL = new ThreadLocal<double[]>() {
        @Override
        protected double[] initialValue() {
            return new double[32];
        }
    };
    final static ThreadLocal<long[]> LONG_ARRAY_TL = new ThreadLocal<long[]>() {
        @Override
        protected long[] initialValue() {
            return new long[32];
        }
    };
    final static ThreadLocal<int[]> INT_ARRAY_TL = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[32];
        }
    };
    final static long[] EMPTY_LONGS = new long[0];
    final static int[] EMPTY_INTS = new int[0];
    final static double[] EMPTY_DOUBLES = new double[0];
    final static String[] EMPTY_STRINGS = new String[0];
    // Default interface or abstract class implementation class configuration
    static final Map<Class<?>, JSONImplInstCreator> DEFAULT_IMPL_INST_CREATOR_MAP = new ConcurrentHashMap<Class<?>, JSONImplInstCreator>();
    protected final static JSONSecureTrustedAccess JSON_SECURE_TRUSTED_ACCESS = new JSONSecureTrustedAccess();
    final static int[] TWO_DIGITS_VALUES = new int[256];
    protected final static int[] THREE_DIGITS_MUL10 = new int[10 << 8];  // 2560

    protected final static JSONUtil JSON_UTIL;
    protected final static boolean ENABLE_VECTOR;
    protected static boolean ENABLE_JIT;
    static {
        for (int i = 0; i < ESCAPE_VALUES.length; ++i) {
            switch (i) {
                case '\n':
                    ESCAPE_VALUES[i] = "\\n";
                    break;
                case '\t':
                    ESCAPE_VALUES[i] = "\\t";
                    break;
                case '\r':
                    ESCAPE_VALUES[i] = "\\r";
                    break;
                case '\b':
                    ESCAPE_VALUES[i] = "\\b";
                    break;
                case '\f':
                    ESCAPE_VALUES[i] = "\\f";
                    break;
                case '"':
                    ESCAPE_VALUES[i] = "\\\"";
                    break;
                case '\\':
                    ESCAPE_VALUES[i] = "\\\\";
                    break;
                default:
                    if (i < 32) {
                        ESCAPE_VALUES[i] = toEscapeString(i);
                    }
            }
            NO_ESCAPE_FLAGS[i] = !(i < 32 || i == '"' || i == '\\');
        }

        String[] availableIDs = TimeZone.getAvailableIDs();
        for (String availableID : availableIDs) {
            GMT_TIME_ZONE_MAP.put(availableID, TimeZone.getTimeZone(availableID));
        }
        TimeZone timeZone = ZERO_TIME_ZONE;
        GMT_TIME_ZONE_MAP.put("GMT+00:00", timeZone);
        GMT_TIME_ZONE_MAP.put("+00:00", timeZone);
        GMT_TIME_ZONE_MAP.put("-00:00", timeZone);
        GMT_TIME_ZONE_MAP.put("+0", timeZone);
        GMT_TIME_ZONE_MAP.put("-0", timeZone);
        try {
            GMT_TIME_ZONE_MAP.put("+08:00", timeZone = TimeZone.getTimeZone("GMT+08:00"));
            GMT_TIME_ZONE_MAP.put("GMT+08:00", timeZone);
        } catch (Throwable throwable) {
        }

        // i（十位） j（个位） k = (i | 0x30) ^ j << 4
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                TWO_DIGITS_VALUES[(i | 0x30) ^ j << 4] = (i < 10 && j < 10) ? i * 10 + j : -1;
            }
        }

        // THREE_DIGITS_MUL10
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                for (int k = 0; k < 10; ++k) {
                    THREE_DIGITS_MUL10[i << 8 | j << 4 | k] = i * 1000 + j * 100 + k * 10 - 48;
                }
            }
        }

        JSONUtil envUtil = new JSONUtil();
        boolean enableVector = false;
        if (EnvUtils.SUPPORTED_VECTOR) {
            // jdk17 supported jdk.incubator.vector
            try {
                MemoryClassLoader memoryClassLoader = new MemoryClassLoader();
                Class<?> utilClass = memoryClassLoader.loadClass("io.github.wycst.wast.json.JSONUtilVectorImpl", ByteUtils.hexString2Bytes("CAFEBABE0000003D008C0A000200030700040C00050006010022696F2F6769746875622F77796373742F776173742F6A736F6E2F4A534F4E5574696C0100063C696E69743E010003282956090008000907000A0C000B000C01002C696F2F6769746875622F77796373742F776173742F6A736F6E2F4A534F4E5574696C566563746F72496D706C01001D425954455F535045434945535F5052454645525245445F4C454E47544801000149090008000E0C000F0010010016425954455F535045434945535F5052454645525245440100244C6A646B2F696E63756261746F722F766563746F722F566563746F72537065636965733B0A001200130700140C0015001601001F6A646B2F696E63756261746F722F766563746F722F42797465566563746F7201000966726F6D417272617901004A284C6A646B2F696E63756261746F722F766563746F722F566563746F72537065636965733B5B4249294C6A646B2F696E63756261746F722F766563746F722F42797465566563746F723B0A001200180C0019001A01000265710100242842294C6A646B2F696E63756261746F722F766563746F722F566563746F724D61736B3B0A001C001D07001E0C001F002001001F6A646B2F696E63756261746F722F766563746F722F566563746F724D61736B0100096669727374547275650100032829490A002200230700240C002500260100106A6176612F6C616E672F537472696E67010007696E6465784F6601000528494929490A001C00280C0029002A0100026F72010044284C6A646B2F696E63756261746F722F766563746F722F566563746F724D61736B3B294C6A646B2F696E63756261746F722F766563746F722F566563746F724D61736B3B0A0012002C0C002D001A0100026C74090008002F0C0030000C01001E425954455F535045434945535F5052454645525245445F4C454E475448320A000800320C00330034010024676574496E6465784F6651756F74654F724261636B736C6173684F724E65676174697665010007285B42494929490A000200360C00370038010027656E73757265496E6465784F6651756F74654F724261636B736C6173684F725554463842797465010008285B4249494A2949090008003A0C003B001001001753484F52545F535045434945535F5052454645525245440A003D003E07003F0C004000410100206A646B2F696E63756261746F722F766563746F722F53686F7274566563746F7201000D66726F6D43686172417272617901004B284C6A646B2F696E63756261746F722F766563746F722F566563746F72537065636965733B5B4349294C6A646B2F696E63756261746F722F766563746F722F53686F7274566563746F723B0A003D00430C001900440100242853294C6A646B2F696E63756261746F722F766563746F722F566563746F724D61736B3B09000800460C0047000C01001F53484F52545F535045434945535F5052454645525245445F4C454E475448320A000800490C004A004B01001E676574496E6465784F6651756F74654F724261636B736C61736843686172010007285B4349492949090008004D0C004E000C01001E53484F52545F535045434945535F5052454645525245445F4C454E4754480A000200500C00510052010021656E73757265496E6465784F6651756F74654F724261636B736C61736843686172010008285B4349434A294909001200540C0055001001000B535045434945535F32353609001200570C00580010010011535045434945535F5052454645525245440B005A005B07005C0C005D00200100226A646B2F696E63756261746F722F766563746F722F566563746F72537065636965730100066C656E67746809003D00570100095369676E61747572650100364C6A646B2F696E63756261746F722F766563746F722F566563746F72537065636965733C4C6A6176612F6C616E672F427974653B3E3B0100045A45524F0100014201000D436F6E7374616E7456616C756503000000000100094241434B534C415348030000005C0100374C6A646B2F696E63756261746F722F766563746F722F566563746F72537065636965733C4C6A6176612F6C616E672F53686F72743B3E3B010004436F646501000F4C696E654E756D6265725461626C650100124C6F63616C5661726961626C655461626C650100047468697301002E4C696F2F6769746875622F77796373742F776173742F6A736F6E2F4A534F4E5574696C566563746F72496D706C3B010019284C6A6176612F6C616E672F537472696E673B5B4249492949010006766563746F720100214C6A646B2F696E63756261746F722F766563746F722F42797465566563746F723B01000A766563746F724D61736B0100214C6A646B2F696E63756261746F722F766563746F722F566563746F724D61736B3B0100086669727374506F73010006736F757263650100124C6A6176612F6C616E672F537472696E673B0100036275660100025B420100066F6666736574010005746F6B656E0100056C696D69740100164C6F63616C5661726961626C65547970655461626C650100334C6A646B2F696E63756261746F722F766563746F722F566563746F724D61736B3C4C6A6176612F6C616E672F427974653B3E3B01000D537461636B4D61705461626C6507007601000571756F746501000971756F74654D61736B0100014A0100066C696D6974320100096C696D6974466C61670100015A0100025B430100224C6A646B2F696E63756261746F722F766563746F722F53686F7274566563746F723B01000143010010746F4E6F4573636170654F6666736574010006285B424929490100083C636C696E69743E01000A536F7572636546696C650100174A534F4E5574696C566563746F72496D706C2E6A617661003000080002000000080018000F00100001005F0000000200600018000B000C000000180030000C000000180061006200010063000000020064001800650062000100630000000200660018003B00100001005F0000000200670018004E000C000000180047000C00000008000000050006000100680000002F00010001000000052AB70001B100000002006900000006000100000015006A0000000C000100000005006B006C000000010025006D000100680000011500030009000000442CBEB200076436051D1505A20031B2000D2C1DB800113A061906150491B600173A071907B6001B36081508B200079F000815081D60AC1DB20007603E2B15041DB60021AC000000040069000000260009000000190008001A000E001B0018001C0022001D0029001E0031001F00360021003C0023006A0000005C000900180024006E006F00060022001A007000710007002900130072000C000800000044006B006C00000000004400730074000100000044007500760002000000440077000C0003000000440078000C00040008003C0079000C0005007A0000000C00010022001A0070007B0007007C0000001F0002FF0036000907000807002207007D01010107001207001C010000F8000500080033003400010068000000700003000400000024B2000D2A1BB800114E2D1C91B600172D105CB60017B600272D03B6002BB60027B6001BAC0000000200690000000A00020000002700090028006A0000002A000400000024007500760000000000240077000C000100000024007E000C00020009001B006E006F000300010037003800010068000001B20006000A000000D32BBEB2002E6436060336071C1506A3000704A700040359360899006B2B1C1DB80031593607B20007A0005C2B1CB2000760593D1DB80031593607B20007A000471CB20007603D1C1506A3000704A70004035936089900302B1C1DB80031593607B20007A000212B1CB2000760593D1DB80031593607B20007A0000C1CB20007603DA7FFC515089900081C150760AC2BBEB200076436091C1509A3000704A700040359360899001B2B1C1DB80031593607B20007A0000C1CB20007603DA7FFDA15089900081C150760AC2A2B1C1D1604B70035AC0000000300690000004A00120000002D0008002E000B0030001F0031003400320040003400460035005A0036006F0037007B00390084003C0089003D008E003F0096004000B6004100BF004300C4004400C90046006A0000005C00090096003D0079000C0009000000D3006B006C0000000000D3007500760001000000D30077000C0002000000D3007E000C0003000000D3007F00800004000800CB0081000C0006000B00C80072000C0007001900BA008200830008007C0000001B000CFD001501014001FC002F010940013209FC0007010940011D090008004A004B0001006800000068000300040000001CB200392A1BB8003C4E2D1C93B600422D105CB60042B60027B6001BAC0000000200690000000A00020000004B0009004C006A0000002A00040000001C0075008400000000001C0077000C00010000001C007E000C000200090013006E0085000300010051005200010068000001B20006000A000000D32BBEB200456436060336071C1506A3000704A700040359360899006B2B1C1DB80048593607B2004CA0005C2B1CB2004C60593D1DB80048593607B2004CA000471CB2004C603D1C1506A3000704A70004035936089900302B1C1DB80048593607B2004CA000212B1CB2004C60593D1DB80048593607B2004CA0000C1CB2004C603DA7FFC515089900081C150760AC2BBEB2004C6436091C1509A3000704A700040359360899001B2B1C1DB80048593607B2004CA0000C1CB2004C603DA7FFDA15089900081C150760AC2A2B1C1D1604B7004FAC0000000300690000004A00120000005100080052000B0054001F0055003400560040005800460059005A005A006F005B007B005D0084006000890061008E00630096006400B6006500BF006700C4006800C9006A006A0000005C00090096003D0079000C0009000000D3006B006C0000000000D3007500840001000000D30077000C0002000000D3007E00860003000000D3007F00800004000800CB0081000C0006000B00C80072000C0007001900BA008200830008007C0000001B000CFD001501014001FC002F010940013209FC0007010940011D09000100870088000100680000011D000300060000007CB200532B1CB800114E2D1022B600172D105CB60017B600272D1020B6002BB60027B6001B360415041020A0004D8402202BBE10206436051C1505A3003BB200532B1CB800114E2D1022B600172D105CB60017B600272D1020B6002BB60027B6001B3604150410209F00081C150460AC840220A7FFC51CAC1C150460AC00000003006900000036000D000000700009007100260072002D00730030007400370075003D00760046007700630078006A0079006F007B0075007D0077007F006A0000003E0006003700400079000C00050000007C006B006C00000000007C0075007600010000007C0077000C000200090073006E006F0003002600560072000C0004007C0000000F0004FE003707001201013705FA0001000800890006000100680000005F0002000000000033B20056B3000DB2000DB900590100B30007B200070478B3002EB2005EB30039B20039B900590100B3004CB2004C0478B30045B10000000100690000001A00060000000B0006000C0011000D00190011001F0012002A00130001008A00000002008B"));
                envUtil = (JSONUtil) UnsafeHelper.newInstance(utilClass);
                enableVector = true;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        JSON_UTIL = envUtil;
        ENABLE_VECTOR = enableVector;
        // use vmargs to disabled jit:  -Dwastjson.disable-jit=true
        ENABLE_JIT = !"true".equalsIgnoreCase(System.getProperty("wastjson.disable-jit"));

        registerImplCreator(EnumSet.class, new JSONImplInstCreator<EnumSet>() {
            @Override
            public EnumSet create(GenericParameterizedType<EnumSet> parameterizedType) {
                Class actualType = parameterizedType.getValueType().getActualType();
                return EnumSet.noneOf(actualType);
            }
        });
        registerImplCreator(EnumMap.class, new JSONImplInstCreator<EnumMap>() {
            @Override
            public EnumMap create(GenericParameterizedType<EnumMap> parameterizedType) {
                Class mapKeyClass = parameterizedType.getMapKeyClass();
                return new EnumMap(mapKeyClass);
            }
        });
    }

    /**
     * 获取2个字节组成的两位数
     *
     * @param h （48-57）
     * @param l （48-57）
     * @return
     */
    protected static final int twoDigitsValue(int h, int l) {
        return TWO_DIGITS_VALUES[h ^ (l & 0xf) << 4];
    }

    /**
     * 获取4个字节组成的4位数
     *
     * @param i （0-9）
     * @param j （0-9）
     * @param k （0-9）
     * @param l （48-57） The cache has been reduced by 48
     * @return
     */
    protected static final int fourDigitsValue(int i, int j, int k, int l) {
        return THREE_DIGITS_MUL10[i << 8 | j << 4 | k] + l;
    }

    public static String toEscapeString(int ch) {
        return String.format("\\u%04x", ch);
    }

    public final static <T> void registerImplCreator(Class<? extends T> parentClass, JSONImplInstCreator<T> creator) {
        DEFAULT_IMPL_INST_CREATOR_MAP.put(parentClass, creator);
    }

    final static JSONImplInstCreator getJSONImplInstCreator(Class<?> targetClass) {
        return DEFAULT_IMPL_INST_CREATOR_MAP.get(targetClass);
    }

    final static String getCacheKey(char[] buf, int offset, int len, long hashCode, JSONKeyValueMap<String> table) {
        if (len > 32) {
            return new String(buf, offset, len);
        }
        //  len > 0
        String value = table.getValue(buf, offset, offset + len, hashCode);
        if (value == null) {
            value = new String(buf, offset, len);
            table.putValue(value, hashCode, value);
        }
        return value;
    }

    final static String getCacheKey(byte[] bytes, int offset, int len, long hashCode, JSONKeyValueMap<String> table) {
        if (len > 32) {
            return new String(bytes, offset, len);
        }
        String value = table.getValue(bytes, offset, offset + len, hashCode);
        if (value == null) {
            value = new String(bytes, offset, len);
            table.putValue(value, hashCode, value);
        }
        return value;
    }

    final static String getCacheEightCharsKey(char[] buf, int offset, int len, long hashCode, JSONKeyValueMap<String> table) {
        String value = table.getValueByHash(hashCode);
        if (value == null) {
            value = new String(buf, offset, len);
            table.putExactHashValue(hashCode, value);
        }
        return value;
    }

    final static String getCacheEightBytesKey(byte[] bytes, int offset, int len, long hashCode, JSONKeyValueMap<String> table) {
        String value = table.getValueByHash(hashCode);
        if (value == null) {
            value = new String(bytes, offset, len);
            table.putExactHashValue(hashCode, value);
        }
        return value;
    }

    /**
     * 转化为2位int数字
     *
     * @param buf
     * @param offset
     * @return
     */
    protected final static int parseInt2(char[] buf, int offset)
            throws NumberFormatException {
        char c1, c2;
        int i = offset;
        if (NumberUtils.isDigit(c1 = buf[i]) && NumberUtils.isDigit(c2 = buf[++i])) {
            return twoDigitsValue(c1, c2);
        }
        throw new NumberFormatException("2-digit parsing error: \"" + new String(buf, offset, 2));
    }

    /**
     * 转化为2位int数字
     *
     * @param buf
     * @param offset
     * @return
     */
    protected final static int parseInt2(byte[] buf, int offset)
            throws NumberFormatException {
        byte c1, c2;
        int i = offset;
        if (NumberUtils.isDigit(c1 = buf[i]) && NumberUtils.isDigit(c2 = buf[++i])) {
            return twoDigitsValue(c1, c2);
        }
        throw new NumberFormatException("2-digit parsing error: \"" + new String(buf, offset, 2));
    }

    /**
     * 转化为4位int数字
     *
     * @param buf
     * @param offset
     * @return
     */
    protected final static int parseInt4(char[] buf, int offset)
            throws NumberFormatException {
        char c1, c2, c3, c4;
        int i = offset;
        if (NumberUtils.isDigit(c1 = buf[i]) && NumberUtils.isDigit(c2 = buf[++i]) && NumberUtils.isDigit(c3 = buf[++i]) && NumberUtils.isDigit(c4 = buf[++i])) {
            return fourDigitsValue(c1 & 0xf, c2 & 0xf, c3 & 0xf, c4);
        }
        throw new NumberFormatException("4-digit parsing error: \"" + new String(buf, offset, 4));
    }

    /**
     * 转化为4位int数字
     *
     * @param buf
     * @param offset
     * @return
     */
    protected final static int parseInt4(byte[] buf, int offset)
            throws NumberFormatException {
        byte c1, c2, c3, c4;
        int i = offset;
        if (NumberUtils.isDigit(c1 = buf[i]) && NumberUtils.isDigit(c2 = buf[++i]) && NumberUtils.isDigit(c3 = buf[++i]) && NumberUtils.isDigit(c4 = buf[++i])) {
            return fourDigitsValue(c1 & 0xf, c2 & 0xf, c3 & 0xf, c4);
        }
        throw new NumberFormatException("4-digit parsing error: \"" + new String(buf, offset, 4));
    }

    /**
     * n 位数字（ 0 < n < 4）
     *
     * @param bytes
     * @param offset
     * @param n
     * @return
     * @throws NumberFormatException
     */
    public static int parseIntWithin3(byte[] bytes, int offset, int n)
            throws NumberFormatException {
        switch (n) {
            case 1:
                byte b = bytes[offset];
                if (NumberUtils.isDigit(b)) {
                    return b & 0xf;
                }
                break;
            case 2:
                return parseInt2(bytes, offset);
            case 3:
                int i = offset;
                byte b1 = bytes[i], b2 = bytes[++i], b3 = bytes[++i];
                if (NumberUtils.isDigit(b1) && NumberUtils.isDigit(b2) && NumberUtils.isDigit(b3)) {
                    return fourDigitsValue(0, b1 & 0xf, b2 & 0xf, b3);
                }
        }
        throw new NumberFormatException(n + "-digit parsing error: \"" + new String(bytes, offset, n));
    }

    /**
     * n 位数字（ 0 < n < 4）
     *
     * @param buf
     * @param offset
     * @param n
     * @return
     * @throws NumberFormatException
     */
    public static int parseIntWithin3(char[] buf, int offset, int n)
            throws NumberFormatException {
        switch (n) {
            case 1:
                char c = buf[offset];
                if (NumberUtils.isDigit(c)) {
                    return c & 0xf;
                }
                break;
            case 2:
                return parseInt2(buf, offset);
            case 3:
                int i = offset;
                char c1 = buf[i], c2 = buf[++i], c3 = buf[++i];
                if (NumberUtils.isDigit(c1) && NumberUtils.isDigit(c2) && NumberUtils.isDigit(c3)) {
                    return fourDigitsValue(0, c1 & 0xf, c2 & 0xf, c3);
                }
        }
        throw new NumberFormatException(n + "-digit parsing error: \"" + new String(buf, offset, n));
    }

//    final static boolean isNoEscape32Bits(byte[] bytes, int offset) {
//        return NO_ESCAPE_FLAGS[bytes[offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff];
//    }
//
//    final static boolean isNoEscape64Bits(byte[] bytes, int offset) {
//        return NO_ESCAPE_FLAGS[bytes[offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff]
//                && NO_ESCAPE_FLAGS[bytes[++offset] & 0xff];
//    }

    /**
     * 通过unsafe一次性判断4个字节是否需要转义
     *
     * @param value 限于ascii字节编码（所有字节的高位都为0）
     * @return
     */
    final static boolean isNoneEscaped4Bytes(int value) {
//        return ((value + 0x60606060) & 0x80808080) == 0x80808080  // all >= 32
//                && ((value ^ 0xDDDDDDDD) + 0x01010101 & 0x80808080) == 0x80808080   // != 34
//                && ((value ^ 0xA3A3A3A3) + 0x01010101 & 0x80808080) == 0x80808080;  // all != 92

        // if high-order bits of 4 bytes are all 1 return true, otherwise, return false
        // Not considering negative numbers
        return ((value + 0x60606060) & ((value ^ 0xDDDDDDDD) + 0x01010101) & ((value ^ 0xA3A3A3A3) + 0x01010101) & 0x80808080) == 0x80808080;
    }

    /**
     * 通过unsafe获取的8个字节值一次性判断是否需要转义(包含'"'或者‘\\’或者存在小于32的字节)，如果需要转义返回false,否则返回true
     *
     * @param value 限于ascii字节编码（所有字节的高位都为0）
     * @return
     */
    final static boolean isNoneEscaped8Bytes(long value) {
//        return ((value + 0x6060606060606060L) & 0x8080808080808080L) == 0x8080808080808080L // all >= 32
//                && ((value ^ 0xDDDDDDDDDDDDDDDDL) + 0x0101010101010101L & 0x8080808080808080L) == 0x8080808080808080L // != 34
//                && ((value ^ 0xA3A3A3A3A3A3A3A3L) + 0x0101010101010101L & 0x8080808080808080L) == 0x8080808080808080L; // all != 92

        // if high-order bits of 8 bytes are all 1 return true, otherwise, return false
        // Not considering negative numbers
        return ((value + 0x6060606060606060L) & ((value ^ 0xDDDDDDDDDDDDDDDDL) + 0x0101010101010101L) & ((value ^ 0xA3A3A3A3A3A3A3A3L) + 0x0101010101010101L) & 0x8080808080808080L) == 0x8080808080808080L;
    }

    /**
     * 一次性判断4个字符是否不存在需要转义的字符
     *
     * @param value
     * @return
     */
    final static boolean isNoneEscaped4Chars(long value) {
//        return ((value + 0x7FE07FE07FE07FE0L) & 0x8000800080008000L) == 0x8000800080008000L // all >= 32
//                && ((value ^ 0xFFDDFFDDFFDDFFDDL) + 0x0001000100010001L & 0x8000800080008000L) == 0x8000800080008000L // != 34
//                && ((value ^ 0xFFA3FFA3FFA3FFA3L) + 0x0001000100010001L & 0x8000800080008000L) == 0x8000800080008000L; // all != 92
        long mask = (value + 0x7FE07FE07FE07FE0L) & ((value ^ 0xFFDDFFDDFFDDFFDDL) + 0x0001000100010001L) & ((value ^ 0xFFA3FFA3FFA3FFA3L) + 0x0001000100010001L);
        return (mask & 0x8000800080008000L) == 0x8000800080008000L;
    }

    /**
     * escape next
     *
     * @param buf
     * @param next
     * @param escapeIndex escape slash index
     * @param writer
     * @return
     */
    protected final static int escapeNextChars(char[] buf, char next, int escapeIndex, JSONCharArrayWriter writer) {
        if (next < ESCAPE_CHARS.length) {
            int escapeChar = ESCAPE_CHARS[next];
            if (escapeChar > -1) {
                writer.write((char) escapeChar);
                return escapeIndex + 2;
            } else {
                // \\u
                int c = hex4(buf, escapeIndex + 2);
                writer.write((char) c);
                return escapeIndex + 6;
            }
        } else {
            writer.write(next);
            return escapeIndex + 2;
        }
    }

    /**
     * escape next
     *
     * @param bytes
     * @param next
     * @param escapeIndex escape slash index
     * @param writer
     * @return 返回转义内容处理完成后的下一个未知字符位置
     */
    final static int escapeNextBytes(byte[] bytes, byte next, int escapeIndex, JSONCharArrayWriter writer) {
        if (next == 'u') {
            try {
                int c1 = hex(bytes[escapeIndex + 2]);
                int c2 = hex(bytes[escapeIndex + 3]);
                int c3 = hex(bytes[escapeIndex + 4]);
                int c4 = hex(bytes[escapeIndex + 5]);
                int c = (c1 << 12) | (c2 << 8) | (c3 << 4) | c4;
                writer.write((char) c);
                return escapeIndex + 6;
            } catch (Throwable throwable) {
                // \\u parse error
                String errorContextTextAt = createErrorContextText(bytes, escapeIndex + 1);
                throw new JSONException("Syntax error, from pos " + (escapeIndex + 1) + ", context text by '" + errorContextTextAt + "', " + throwable.getMessage());
            }
        } else {
            writer.write((char) ESCAPE_CHARS[next & 0xFF]);
            return escapeIndex + 2;
        }
    }

    /**
     * 将\\u后面的4个16进制字符转化为int值
     *
     * @param i1
     * @param i2
     * @param i3
     * @param i4
     * @return
     */
    protected final static int hex4(int i1, int i2, int i3, int i4) {
        return (hex(i1) << 12) | (hex(i2) << 8) | (hex(i3) << 4) | hex(i4);
    }

    // without validate
    protected final static int hex(int c) {
        return NumberUtils.hexDigitAt(c);
    }

    /**
     * 将\\u后面的4个16进制字符转化为int值
     *
     * @param buf
     * @param offset \\u位置后一位
     * @return
     */
    protected static int hex4(char[] buf, int offset) {
        int j = offset;
        try {
            int c1 = hex(buf[j++]);
            int c2 = hex(buf[j++]);
            int c3 = hex(buf[j++]);
            int c4 = hex(buf[j++]);
            return (c1 << 12) | (c2 << 8) | (c3 << 4) | c4;
        } catch (Throwable throwable) {
            // \\u parse error
            String errorContextTextAt = createErrorContextText(buf, j - 1);
            throw new JSONException("Syntax error, from pos " + offset + ", context text by '" + errorContextTextAt + "', hex unicode parse error ");
        }
    }

    /**
     * 十进制字符转数字
     *
     * @param ch
     * @return
     */
    protected static int digitDecimal(int ch) {
        return NumberUtils.digitDecimal(ch);
    }

    /**
     * 匹配日期
     *
     * @param buf
     * @param from
     * @param to
     * @param dateCls
     * @return
     */
    final static Date matchDate(char[] buf, int from, int to, String timezone, Class<? extends Date> dateCls) {

        int len = to - from;
        String timezoneIdAt = timezone;
        if (len > 19) {
            // yyyy-MM-ddTHH:mm:ss.SSS+XX:YY
            int j = to, ch;
            while (j > from) {
                // Check whether the time zone ID exists in the date
                // Check for '+' or '-' or 'Z'
                if ((ch = buf[--j]) == '.' || ch == ' ') break;
                if (ch == '+' || ch == '-' || ch == 'Z') {
                    timezoneIdAt = new String(buf, j, to - j);
                    to = j;
                    len = to - from;
                    break;
                }
            }
        }
        switch (len) {
            case 8: {
                // yyyyMMdd
                // HH:mm:ss
                try {
                    if (dateCls != null && Time.class.isAssignableFrom(dateCls)) {
                        int hour = parseInt2(buf, from);
                        int minute = parseInt2(buf, from + 3);
                        int second = parseInt2(buf, from + 6);
                        return parseDate(1970, 1, 1, hour, minute, second, 0, timezoneIdAt, dateCls);
                    } else {
                        int year = parseInt4(buf, from);
                        int month = parseInt2(buf, from + 4);
                        int day = parseInt2(buf, from + 6);
                        return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
                    }
                } catch (Throwable throwable) {
                }
                return null;
            }
            case 10: {
                // yyyy-MM-dd yyyy/MM/dd
                // \d{4}[-/]\d{2}[-/]\d{2}
                try {
                    int year = parseInt4(buf, from);
                    int month = parseInt2(buf, from + 5);
                    int day = parseInt2(buf, from + 8);
                    return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
                } catch (Throwable throwable) {
                }
                return null;
            }
            case 14:
            case 15:
            case 16:
            case 17: {
                // yyyyMMddHHmmss or yyyyMMddhhmmssSSS
                try {
                    int year = parseInt4(buf, from);
                    int month = parseInt2(buf, from + 4);
                    int day = parseInt2(buf, from + 6);
                    int hour = parseInt2(buf, from + 8);
                    int minute = parseInt2(buf, from + 10);
                    int second = parseInt2(buf, from + 12);
                    int millsecond = 0;
                    if (len > 14) {
                        millsecond = parseIntWithin3(buf, from + 14, len - 14);
                    }
                    return parseDate(year, month, day, hour, minute, second, millsecond, timezoneIdAt, dateCls);
                } catch (Throwable throwable) {
                }
                return null;
            }
            case 19:
            case 21:
            case 22:
            case 23: {
                // yyyy-MM-dd HH:mm:ss yyyy/MM/dd HH:mm:ss 19位
                // yyyy-MM-dd HH:mm:ss.SSS? yyyy/MM/dd HH:mm:ss.SSS? 23位
                // \\d{4}[-/]\\d{2}[-/]\\d{2} \\d{2}:\\d{2}:\\d{2}
                try {
                    int year = parseInt4(buf, from);
                    int month = parseInt2(buf, from + 5);
                    int day = parseInt2(buf, from + 8);
                    int hour = parseInt2(buf, from + 11);
                    int minute = parseInt2(buf, from + 14);
                    int second = parseInt2(buf, from + 17);
                    int millsecond = 0;
                    if (len > 20) {
                        millsecond = parseIntWithin3(buf, from + 20, len - 20);
                    }
                    return parseDate(year, month, day, hour, minute, second, millsecond, timezoneIdAt, dateCls);
                } catch (Throwable throwable) {
                }
                return null;
            }
            case 28: {
                /***
                 * dow mon dd hh:mm:ss zzz yyyy 28位
                 * example Sun Jan 02 21:51:14 CST 2020
                 * @see Date#toString()
                 */
                try {
                    int year = parseInt4(buf, from + 24);
                    String monthAbbr = new String(buf, from + 4, 3);
                    int month = getMonthAbbrIndex(monthAbbr) + 1;
                    int day = parseInt2(buf, from + 8);
                    int hour = parseInt2(buf, from + 11);
                    int minute = parseInt2(buf, from + 14);
                    int second = parseInt2(buf, from + 17);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                } catch (Throwable throwable) {
                }
                return null;
            }
            default:
                if (len > 28) {
                    // yyyy-MM-ddTHH:mm:ss.000000000 -> yyyy-MM-ddTHH:mm:ss.000
                    try {
                        int year = parseInt4(buf, from);
                        int month = parseInt2(buf, from + 5);
                        int day = parseInt2(buf, from + 8);
                        int hour = parseInt2(buf, from + 11);
                        int minute = parseInt2(buf, from + 14);
                        int second = parseInt2(buf, from + 17);
                        int millsecond = parseIntWithin3(buf, from + 20, 3);
                        return parseDate(year, month, day, hour, minute, second, millsecond, timezoneIdAt, dateCls);
                    } catch (Throwable throwable) {
                    }
                }
                return null;
        }
    }

    /**
     * 字符串转日期
     *
     * @param buf
     * @param from         开始引号位置
     * @param to           结束引号位置后一位
     * @param pattern      日期格式
     * @param patternType  格式分类
     * @param dateTemplate 日期模板
     * @param timezone     时间钟
     * @param dateCls      日期类型
     * @return
     */
    protected static Date parseDateValueOfString(char[] buf, int from, int to, String pattern, int patternType, DateTemplate dateTemplate, String timezone,
                                                 Class<? extends Date> dateCls) {
        int realFrom = from;
        String timezoneIdAt = timezone;
        try {
            switch (patternType) {
                case 1: {
                    // yyyy-MM-dd HH:mm:ss or yyyy/MM/dd HH:mm:ss
                    int year = parseInt4(buf, from + 1);
                    int month = parseInt2(buf, from + 6);
                    int day = parseInt2(buf, from + 9);
                    int hour = parseInt2(buf, from + 12);
                    int minute = parseInt2(buf, from + 15);
                    int second = parseInt2(buf, from + 18);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                }
                case 2: {
                    // yyyy-MM-dd yyyy/MM/dd
                    int year = parseInt4(buf, from + 1);
                    int month = parseInt2(buf, from + 6);
                    int day = parseInt2(buf, from + 9);
                    return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
                }
                case 3: {
                    // yyyyMMddHHmmss
                    int year = parseInt4(buf, from + 1);
                    int month = parseInt2(buf, from + 5);
                    int day = parseInt2(buf, from + 7);
                    int hour = parseInt2(buf, from + 9);
                    int minute = parseInt2(buf, from + 11);
                    int second = parseInt2(buf, from + 13);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                }
                case 4: {
                    TimeZone timeZone = getTimeZone(timezoneIdAt);
                    long time = dateTemplate.parseTime(buf, from + 1, to - from - 2, timeZone);
                    return parseDate(time, dateCls);
                }
                default: {
                    return matchDate(buf, from + 1, to - 1, timezone, dateCls);
                }
            }
        } catch (Throwable throwable) {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            String dateSource = new String(buf, from + 1, to - from - 2);
            if (patternType > 0) {
                throw new JSONException("Syntax error, at pos " + realFrom + ", dateStr " + dateSource + " mismatch date pattern '" + pattern + "'");
            } else {
                throw new JSONException("Syntax error, at pos " + realFrom + ", dateStr " + dateSource + " mismatch any date format.");
            }
        }
    }

    /**
     * 匹配日期
     *
     * @param buf
     * @param from
     * @param to
     * @param dateCls
     * @return
     */
    final static Date matchDate(byte[] buf, int from, int to, String timezone, Class<? extends Date> dateCls) {

        int len = to - from;
        String timezoneIdAt = timezone;
        if (len > 19) {
            // yyyy-MM-ddTHH:mm:ss.SSS+XX:YY
            int j = to, ch;
            while (j > from) {
                // Check whether the time zone ID exists in the date
                // Check for '+' or '-' or 'Z'
                if ((ch = buf[--j]) == '.' || ch == ' ') break;
                if (ch == '+' || ch == '-' || ch == 'Z') {
                    timezoneIdAt = new String(buf, j, to - j);
                    to = j;
                    len = to - from;
                    break;
                }
            }
        }
        switch (len) {
            case 8: {
                // yyyyMMdd
                // HH:mm:ss
                try {
                    if (dateCls != null && Time.class.isAssignableFrom(dateCls)) {
                        int hour = parseInt2(buf, from);
                        int minute = parseInt2(buf, from + 3);
                        int second = parseInt2(buf, from + 6);
                        return parseDate(1970, 1, 1, hour, minute, second, 0, timezoneIdAt, dateCls);
                    } else {
                        int year = parseInt4(buf, from);
                        int month = parseInt2(buf, from + 4);
                        int day = parseInt2(buf, from + 6);
                        return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
                    }
                } catch (Throwable throwable) {
                }
                return null;
            }
            case 10: {
                // yyyy-MM-dd yyyy/MM/dd
                // \d{4}[-/]\d{2}[-/]\d{2}
                try {
                    int year = parseInt4(buf, from);
                    int month = parseInt2(buf, from + 5);
                    int day = parseInt2(buf, from + 8);
                    return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
                } catch (Throwable throwable) {
                }
                return null;
            }
            case 14:
            case 15:
            case 16:
            case 17: {
                // yyyyMMddHHmmss or yyyyMMddhhmmssSSS
                try {
                    int year = parseInt4(buf, from);
                    int month = parseInt2(buf, from + 4);
                    int day = parseInt2(buf, from + 6);
                    int hour = parseInt2(buf, from + 8);
                    int minute = parseInt2(buf, from + 10);
                    int second = parseInt2(buf, from + 12);
                    int millsecond = 0;
                    if (len > 14) {
                        millsecond = parseIntWithin3(buf, from + 14, len - 14);
                    }
                    return parseDate(year, month, day, hour, minute, second, millsecond, timezoneIdAt, dateCls);
                } catch (Throwable throwable) {
                }
                return null;
            }
            case 19:
            case 21:
            case 22:
            case 23: {
                // yyyy-MM-dd HH:mm:ss yyyy/MM/dd HH:mm:ss 19位
                // yyyy-MM-dd HH:mm:ss.SSS? yyyy/MM/dd HH:mm:ss.SSS? 23位
                // \\d{4}[-/]\\d{2}[-/]\\d{2} \\d{2}:\\d{2}:\\d{2}
                try {
                    int year = parseInt4(buf, from);
                    int month = parseInt2(buf, from + 5);
                    int day = parseInt2(buf, from + 8);
                    int hour = parseInt2(buf, from + 11);
                    int minute = parseInt2(buf, from + 14);
                    int second = parseInt2(buf, from + 17);
                    int millsecond = 0;
                    if (len > 20) {
                        millsecond = parseIntWithin3(buf, from + 20, len - 20);
                    }
                    return parseDate(year, month, day, hour, minute, second, millsecond, timezoneIdAt, dateCls);
                } catch (Throwable throwable) {
                }
                return null;
            }
            case 28: {
                /***
                 * dow mon dd hh:mm:ss zzz yyyy 28位
                 * example Sun Jan 02 21:51:14 CST 2020
                 * @see Date#toString()
                 */
                try {
                    int year = parseInt4(buf, from + 24);
                    String monthAbbr = new String(buf, from + 4, 3);
                    int month = getMonthAbbrIndex(monthAbbr) + 1;
                    int day = parseInt2(buf, from + 8);
                    int hour = parseInt2(buf, from + 11);
                    int minute = parseInt2(buf, from + 14);
                    int second = parseInt2(buf, from + 17);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                } catch (Throwable throwable) {
                }
                return null;
            }
            default:
                if (len > 28) {
                    // yyyy-MM-ddTHH:mm:ss.000000000 -> yyyy-MM-ddTHH:mm:ss.000
                    try {
                        int year = parseInt4(buf, from);
                        int month = parseInt2(buf, from + 5);
                        int day = parseInt2(buf, from + 8);
                        int hour = parseInt2(buf, from + 11);
                        int minute = parseInt2(buf, from + 14);
                        int second = parseInt2(buf, from + 17);
                        int millsecond = parseIntWithin3(buf, from + 20, 3);
                        return parseDate(year, month, day, hour, minute, second, millsecond, timezoneIdAt, dateCls);
                    } catch (Throwable throwable) {
                    }
                }
                return null;
        }
    }

    /**
     * 字符串转日期
     *
     * @param bytes        字节数组
     * @param from         开始引号位置
     * @param to           结束引号位置后一位
     * @param pattern      日期格式
     * @param patternType  格式分类
     * @param dateTemplate 日期模板
     * @param timezone     时间钟
     * @param dateCls      日期类型
     * @return
     */
    protected static Object parseDateValueOfString(byte[] bytes, int from, int to, String pattern, int patternType, DateTemplate dateTemplate, String timezone,
                                                   Class<? extends Date> dateCls) {
        int realFrom = from;
        String timezoneIdAt = timezone;
        try {
            switch (patternType) {
                case 0: {
                    return matchDate(bytes, from + 1, to - 1, timezone, dateCls);
                }
                case 1: {
                    // yyyy-MM-dd HH:mm:ss or yyyy/MM/dd HH:mm:ss
                    int year = parseInt4(bytes, from + 1);
                    int month = parseInt2(bytes, from + 6);
                    int day = parseInt2(bytes, from + 9);
                    int hour = parseInt2(bytes, from + 12);
                    int minute = parseInt2(bytes, from + 15);
                    int second = parseInt2(bytes, from + 18);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                }
                case 2: {
                    // yyyy-MM-dd yyyy/MM/dd
                    int year = parseInt4(bytes, from + 1);
                    int month = parseInt2(bytes, from + 6);
                    int day = parseInt2(bytes, from + 9);
                    return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
                }
                case 3: {
                    // yyyyMMddHHmmss
                    int year = parseInt4(bytes, from + 1);
                    int month = parseInt2(bytes, from + 5);
                    int day = parseInt2(bytes, from + 7);
                    int hour = parseInt2(bytes, from + 9);
                    int minute = parseInt2(bytes, from + 11);
                    int second = parseInt2(bytes, from + 13);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                }
                default: {
                    TimeZone timeZone = getTimeZone(timezoneIdAt);
                    long time = dateTemplate.parseTime(bytes, from + 1, to - from - 2, timeZone);
                    return parseDate(time, dateCls);
                }
            }
        } catch (Throwable throwable) {
            if (throwable instanceof JSONException) {
                throw (JSONException) throwable;
            }
            String dateSource = new String(bytes, from + 1, to - from - 2);
            if (patternType > 0) {
                throw new JSONException("Syntax error, at pos " + realFrom + ", dateStr " + dateSource + " mismatch date pattern '" + pattern + "'");
            } else {
                throw new JSONException("Syntax error, at pos " + realFrom + ", dateStr " + dateSource + " mismatch any date format.");
            }
        }
    }

    /**
     * 清除注释和空白,返回第一个非空字符 （Clear comments and whitespace and return the first non empty character）
     * 开启注释支持后，支持//.*\n 和 /* *\/ （After enabling comment support, support / /* \N and / **\/）
     *
     * @param buf
     * @param beginIndex   开始位置
     * @param parseContext 上下文配置
     * @return 去掉注释后的第一个非空字符位置（Non empty character position after removing comments）
     * @see ReadOption#AllowComment
     */
    protected final static int clearCommentAndWhiteSpaces(char[] buf, int beginIndex, JSONParseContext parseContext) {
        int i = beginIndex, toIndex = parseContext.toIndex;
        if (i >= toIndex) {
            throw new JSONException("Syntax error, unexpected '/', position " + (beginIndex - 1));
        }
        // 注释和 /*注释
        // / or *
        char ch = buf[beginIndex];
        if (ch == '/') {
            // End with newline \ n
            while (i < toIndex && buf[i] != '\n') {
                ++i;
            }
            // continue clear WhiteSpaces
            ch = '\0';
            while (i + 1 < toIndex && (ch = buf[++i]) <= ' ') ;
            if (ch == '/') {
                // 递归清除
                i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext);
            }
        } else if (ch == '*') {
            // End with */
            char prev = '\0';
            boolean matched = false;
            while (i + 1 < toIndex) {
                ch = buf[++i];
                if (ch == '/' && prev == '*') {
                    matched = true;
                    break;
                }
                prev = ch;
            }
            if (!matched) {
                throw new JSONException("Syntax error, not found the close comment '*/' util the end ");
            }
            // continue clear WhiteSpaces
            ch = '\0';
            while (i + 1 < toIndex && (ch = buf[++i]) <= ' ') ;
            if (ch == '/') {
                // 递归清除
                i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext);
            }
        } else {
            throw new JSONException("Syntax error, unexpected '" + ch + "', position " + beginIndex);
        }
        return i;
    }

    /**
     * 清除注释和空白
     *
     * @param bytes
     * @param beginIndex   开始位置
     * @param parseContext 上下文配置
     * @return 去掉注释后的第一个非空字节位置（Non empty character position after removing comments）
     * @see ReadOption#AllowComment
     */
    protected static int clearCommentAndWhiteSpaces(byte[] bytes, int beginIndex, JSONParseContext parseContext) {
        int i = beginIndex, toIndex = parseContext.toIndex;
        if (i >= toIndex) {
            throw new JSONException("Syntax error, unexpected '/', position " + (beginIndex - 1));
        }
        // 注释和 /*注释
        // / or *
        byte b = bytes[beginIndex];
        if (b == '/') {
            // End with newline \ n
            while (i < toIndex && bytes[i] != '\n') {
                ++i;
            }
            // continue clear WhiteSpaces
            b = '\0';
            while (i + 1 < toIndex && (b = bytes[++i]) <= ' ') ;
            if (b == '/') {
                // 递归清除
                i = clearCommentAndWhiteSpaces(bytes, i + 1, parseContext);
            }
        } else if (b == '*') {
            // End with */
            byte prev = 0;
            boolean matched = false;
            while (i + 1 < toIndex) {
                b = bytes[++i];
                if (b == '/' && prev == '*') {
                    matched = true;
                    break;
                }
                prev = b;
            }
            if (!matched) {
                throw new JSONException("Syntax error, not found the close comment '*/' util the end ");
            }
            // continue clear WhiteSpaces
            b = '\0';
            while (i + 1 < toIndex && (b = bytes[++i]) <= ' ') ;
            if (b == '/') {
                // 递归清除
                i = clearCommentAndWhiteSpaces(bytes, i + 1, parseContext);
            }
        } else {
            throw new JSONException("Syntax error, unexpected '" + (char) b + "', position " + beginIndex);
        }
        return i;
    }

    /**
     * 格式化缩进,默认使用\t来进行缩进
     *
     * @param content
     * @param level
     * @param formatOut
     * @throws IOException
     */
    protected final static void writeFormatOutSymbols(JSONWriter content, int level, boolean formatOut, JSONConfig jsonConfig) throws IOException {
        if (formatOut && level > -1) {
            writeFormatOutSymbols(content, level, jsonConfig);
        }
    }

    protected final static void writeFormatOutSymbols(JSONWriter content, int level, JSONConfig jsonConfig) throws IOException {
        boolean formatIndentUseSpace = jsonConfig.isFormatIndentUseSpace();
        if (formatIndentUseSpace) {
            content.writeJSONToken('\n');
            if (level == 0) return;
            int totalSpaceNum = level * jsonConfig.getFormatIndentSpaceNum();
            int symbolSpaceNum = FORMAT_OUT_SYMBOL_SPACES.length;
            while (totalSpaceNum >= symbolSpaceNum) {
                content.write(FORMAT_OUT_SYMBOL_SPACES);
                totalSpaceNum -= symbolSpaceNum;
            }
            while (totalSpaceNum-- > 0) {
                content.write(' ');
            }
        } else {
            char[] symbol = FORMAT_OUT_SYMBOL_TABS;
            final int symbolLen = 11; // symbol.length;
            if (level < symbolLen - 1) {
                switch (level) {
                    case 5:
                        content.writeMemory(FO_INDENT4_INT64, FO_INDENT4_INT32, 4);
                        content.writeMemory2(FOTT_INDENT2_INT32, FOTT_INDENT2_INT16, symbol, 1);
                        break;
                    case 4:
                        content.writeMemory(FO_INDENT4_INT64, FO_INDENT4_INT32, 4);
                        content.writeJSONToken('\t');
                        break;
                    case 3:
                        content.writeMemory(FO_INDENT4_INT64, FO_INDENT4_INT32, 4);
                        break;
                    case 2:
                        content.writeJSONToken('\n');
                        content.writeMemory2(FOTT_INDENT2_INT32, FOTT_INDENT2_INT16, symbol, 1);
                        break;
                    case 1:
                        content.writeMemory2(FONT_INDENT2_INT32, FONT_INDENT2_INT16, symbol, 0);
                        break;
                    case 0:
                        content.writeJSONToken('\n');
                        break;
                    default: {
                        content.write(symbol, 0, level + 1);
                    }
                }
            } else {
                content.write(symbol);
                int appendTabLen = level - (symbolLen - 1);
                while (appendTabLen-- > 0) {
                    content.write('\t');
                }
            }
        }
    }

    private static int getMonthAbbrIndex(String monthAbbr) {
        for (int i = 0, len = MONTH_ABBR.length; i < len; ++i) {
            if (MONTH_ABBR[i].equals(monthAbbr)) {
                return i;
            }
        }
        return -1;
    }

    private static Date parseDate(int year, int month, int day, int hour, int minute, int second, int millsecond, String timeZoneId, Class<? extends Date> dateCls) {
        TimeZone timeZone = getTimeZone(timeZoneId);
        long timeInMillis = GregorianDate.getTime(year, month, day, hour, minute, second, millsecond, timeZone);
        return parseDate(timeInMillis, dateCls);
    }

    // 获取时钟，默认GMT
    static TimeZone getTimeZone(String timeZoneId) {
        if (timeZoneId != null && timeZoneId.trim().length() > 0) {
            TimeZone timeZone;
            if (GMT_TIME_ZONE_MAP.containsKey(timeZoneId)) {
                timeZone = GMT_TIME_ZONE_MAP.get(timeZoneId);
            } else {
                if (timeZoneId.startsWith("GMT")) {
                    timeZone = TimeZone.getTimeZone(timeZoneId);
                } else {
                    timeZone = TimeZone.getTimeZone("GMT" + timeZoneId);
                }
                if (timeZone != null && timeZone.getRawOffset() != 0) {
                    GMT_TIME_ZONE_MAP.put(timeZoneId, timeZone);
                }
            }
            return timeZone;
        } else {
            return UnsafeHelper.getDefaultTimeZone();
        }
    }

    // 将时间戳转化为指定类型的日期对象
    protected static Date parseDate(long timeInMillis, Class<? extends Date> dateCls) {
        if (dateCls == Date.class) {
            return new Date(timeInMillis);
        } else if (dateCls == java.sql.Date.class) {
            return new java.sql.Date(timeInMillis);
        } else if (dateCls == java.sql.Timestamp.class) {
            return new java.sql.Timestamp(timeInMillis);
        } else {
            try {
                Constructor<? extends Date> constructor = dateCls.getConstructor(long.class);
                UnsafeHelper.setAccessible(constructor);
                return constructor.newInstance(timeInMillis);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @param from
     * @param to
     * @param buf
     * @param isUnquotedFieldName
     * @return
     */
    final static Serializable parseKeyOfMap(char[] buf, int from, int to, boolean isUnquotedFieldName) {
        if (isUnquotedFieldName) {
            while ((from < to) && ((buf[from]) <= ' ')) {
                from++;
            }
            while ((to > from) && ((buf[to - 1]) <= ' ')) {
                to--;
            }
            int count = to - from;
            if (count == 4) {
                long lv = JSONUnsafe.getLong(buf, from);
                if (lv == NULL_LONG) {
                    return null;
                }
                if (lv == TRUE_LONG) {
                    return true;
                }
            }
            if (count == 5 && buf[from] == 'f' && JSONUnsafe.getLong(buf, from + 1) == ALSE_LONG) {
                return false;
            }
            boolean numberFlag = true;
            int pointFlag = 0;
            for (int i = from; i < to; ++i) {
                int c = buf[i];
                if (c == '.') {
                    ++pointFlag;
                } else {
                    if (i != from || c != '-') {
                        if (!NumberUtils.isDigit(c)) {
                            numberFlag = false;
                            break;
                        }
                    }
                }
            }
            String result = new String(buf, from, count);
            if (numberFlag && pointFlag <= 1) {
                if (pointFlag == 1) {
                    return Double.parseDouble(result);
                } else {
                    long val = Long.parseLong(result);
                    if (val <= Integer.MAX_VALUE && val >= Integer.MIN_VALUE) {
                        return (int) val;
                    }
                    return val;
                }
            }
            return result;
        } else {
            int len = to - from - 2;
            return new String(buf, from + 1, len);
        }
    }

    final static Serializable parseKeyOfMap(byte[] buf, int from, int to, boolean isUnquotedFieldName) {
        if (isUnquotedFieldName) {
            while ((from < to) && ((buf[from]) <= ' ')) {
                from++;
            }
            while ((to > from) && ((buf[to - 1]) <= ' ')) {
                to--;
            }
            int count = to - from;
            if (count == 4) {
                int iv = JSONUnsafe.getInt(buf, from);
                if (iv == NULL_INT) {
                    return null;
                }
                if (iv == TRUE_INT) {
                    return true;
                }
            }
            if (count == 5 && buf[from] == 'f' && JSONUnsafe.getInt(buf, from + 1) == ALSE_INT) {
                return false;
            }
            boolean numberFlag = true;
            int pointFlag = 0;
            for (int i = from; i < to; ++i) {
                int c = buf[i];
                if (c == '.') {
                    ++pointFlag;
                } else {
                    if (i != from || c != '-') {
                        if (!NumberUtils.isDigit(c)) {
                            numberFlag = false;
                            break;
                        }
                    }
                }
            }
            String result = new String(buf, from, count);
            if (numberFlag && pointFlag <= 1) {
                if (pointFlag == 1) {
                    return Double.parseDouble(result);
                } else {
                    long val = Long.parseLong(result);
                    if (val <= Integer.MAX_VALUE && val >= Integer.MIN_VALUE) {
                        return (int) val;
                    }
                    return val;
                }
            }
            return result;
        } else {
            int len = to - from - 2;
            return new String(buf, from + 1, len);
        }
    }

    // 16进制字符数组（字符串）转化字节数组
    protected static byte[] hexString2Bytes(char[] chars, int offset, int len) {
        byte[] bytes = new byte[len / 2];
        int byteLength = 0;
        int b = -1;
        for (int i = offset, count = offset + len; i < count; ++i) {
            char ch = Character.toUpperCase(chars[i]);
            int numIndex = ch > '9' ? ch - 55 : ch - 48;
            if (numIndex < 0 || numIndex >= 16) continue;
            if (b == -1) {
                b = numIndex << 4;
            } else {
                b += numIndex;
                bytes[byteLength++] = (byte) b;
                b = -1;
            }
        }
        if (byteLength == bytes.length) {
            return bytes;
        }

        byte[] buffer = new byte[byteLength];
        System.arraycopy(bytes, 0, buffer, 0, byteLength);
        return buffer;
    }

    // 16进制字符数组（字符串）转化字节数组
    protected static byte[] hexString2Bytes(byte[] buf, int offset, int len) {
        byte[] bytes = new byte[len / 2];
        int byteLength = 0;
        int b = -1;
        for (int i = offset, count = offset + len; i < count; ++i) {
            char ch = Character.toUpperCase((char) buf[i]);
            int numIndex = ch > '9' ? ch - 55 : ch - 48;
            if (numIndex < 0 || numIndex >= 16) continue;
            if (b == -1) {
                b = numIndex << 4;
            } else {
                b += numIndex;
                bytes[byteLength++] = (byte) b;
                b = -1;
            }
        }
        if (byteLength == bytes.length) {
            return bytes;
        }

        byte[] buffer = new byte[byteLength];
        System.arraycopy(bytes, 0, buffer, 0, byteLength);
        return buffer;
    }

    protected final static int digits2Bytes(byte[] buf, int offset) {
        return JSONUnsafe.UNSAFE_ENDIAN.digits2Bytes(buf, offset);
    }

    // 读取流中的最大限度（maxLen）的字符数组(只读取一次)
    protected final static char[] readOnceInputStream(InputStream is, int maxLen) throws IOException {
        try {
            char[] buf = new char[maxLen];
            InputStreamReader streamReader = new InputStreamReader(is);
            int len = streamReader.read(buf);
            if (len != maxLen) {
                char[] tmp = new char[len];
                System.arraycopy(buf, 0, tmp, 0, len);
                buf = tmp;
            }
            streamReader.close();
            return buf;
        } catch (RuntimeException rx) {
            throw rx;
        } finally {
            is.close();
        }
    }

    protected final static void handleCatchException(Throwable ex, char[] buf, int toIndex) {
        // There is only one possibility to control out of bounds exceptions when indexing toindex
        if (ex instanceof IndexOutOfBoundsException) {
            String errorContextTextAt = createErrorContextText(buf, toIndex);
            throw new JSONException("Syntax error, context text by '" + errorContextTextAt + "', JSON format error, and the end token may be missing, such as '\"' or ', ' or '}' or ']'.", ex);
        }
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
    }

    protected final static void handleCatchException(Throwable ex, byte[] bytes, int toIndex) {
        // There is only one possibility to control out of bounds exceptions when indexing toindex
        if (ex instanceof IndexOutOfBoundsException) {
            String errorContextTextAt = createErrorContextText(bytes, toIndex);
            throw new JSONException("Syntax error, context text by '" + errorContextTextAt + "', JSON format error, and the end token may be missing, such as '\"' or ', ' or '}' or ']'.", ex);
        }
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
    }

    // 常规日期格式分类
    protected final static int getPatternType(String pattern) {
        if (pattern != null) {
            if (pattern.equalsIgnoreCase("yyyy-MM-dd HH:mm:ss")
                    || pattern.equalsIgnoreCase("yyyy/MM/dd HH:mm:ss")
                    || pattern.equalsIgnoreCase("yyyy-MM-ddTHH:mm:ss")
            ) {
                return 1;
            } else if (pattern.equalsIgnoreCase("yyyy-MM-dd") || pattern.equalsIgnoreCase("yyyy/MM/dd")) {
                return 2;
            } else if (pattern.equalsIgnoreCase("yyyyMMddHHmmss")) {
                return 3;
            } else {
                return 4;
            }
        }
        return 0;
    }

    protected static final int COLLECTION_ARRAYLIST_TYPE = 1;
    protected static final int COLLECTION_HASHSET_TYPE = 2;
    protected static final int COLLECTION_OTHER_TYPE = 3;

    protected final static int getCollectionType(Class<?> actualType) {
        if (actualType == List.class || actualType == ArrayList.class || actualType.isAssignableFrom(ArrayList.class)) {
            return COLLECTION_ARRAYLIST_TYPE;
        } else if (actualType == Set.class || actualType == HashSet.class || actualType.isAssignableFrom(HashSet.class)) {
            return COLLECTION_HASHSET_TYPE;
        } else {
            return COLLECTION_OTHER_TYPE;
        }
    }

    protected final static Collection createCollectionInstance(Class<?> collectionCls) {
        return createCollectionInstance(collectionCls, 0);
    }

    protected final static Collection createCollectionInstance(Class<?> collectionCls, int capacityIfSupported) {
        if (collectionCls.isInterface()) {
            if (collectionCls == List.class || collectionCls == Collection.class) {
                return new ArrayList<Object>(capacityIfSupported);
            } else if (collectionCls == Set.class) {
                return new HashSet<Object>();
            } else {
                throw new UnsupportedOperationException("Unsupported for collection type '" + collectionCls + "', Please specify an implementation class");
            }
        } else {
            if (collectionCls == HashSet.class) {
                return new HashSet<Object>();
            } else if (collectionCls == Vector.class) {
                return new Vector<Object>();
            } else if (collectionCls == ArrayList.class || collectionCls == Object.class) {
                return new ArrayList<Object>(capacityIfSupported);
            } else {
                try {
                    return (Collection<Object>) collectionCls.newInstance();
                } catch (Exception e) {
                    throw new JSONException("create Collection instance error, class " + collectionCls);
                }
            }
        }
    }

    // create map
    final static Map createMapInstance(GenericParameterizedType genericParameterizedType) {
        Class<? extends Map> mapCls = genericParameterizedType.getActualType();
        Map map = createCommonMapInstance(mapCls);
        if (map != null) return map;
        JSONImplInstCreator implInstCreator = getJSONImplInstCreator(mapCls);
        if (implInstCreator != null) {
            return (Map) implInstCreator.create(genericParameterizedType);
        }
        try {
            return (Map) UnsafeHelper.newInstance(mapCls);
        } catch (Exception e) {
            throw new JSONException("create map error for " + mapCls);
        }
    }

    final static Map createMapInstance(Class<? extends Map> mapCls) {
        Map map = createCommonMapInstance(mapCls);
        if (map != null) return map;
        try {
            return (Map) UnsafeHelper.newInstance(mapCls);
        } catch (Exception e) {
            throw new JSONException("create map error for " + mapCls);
        }
    }

    final static Map createCommonMapInstance(Class<? extends Map> targetCls) {
        Class<?> mapCls = targetCls;
        if (mapCls == Map.class || mapCls == null || mapCls == LinkedHashMap.class) {
            return new LinkedHashMap();
        }
        if (mapCls == HashMap.class) {
            return new HashMap();
        }
        if (mapCls == Hashtable.class || mapCls == Dictionary.class) {
            return new Hashtable();
        }
        if (mapCls == AbstractMap.class) {
            return new LinkedHashMap();
        }
        if (mapCls == TreeMap.class || mapCls == SortedMap.class) {
            return new TreeMap();
        }
        return null;
    }

    protected static String createErrorContextText(char[] buf, int at) {
        try {
            int len = buf.length;
            char[] text = new char[40];
            int count;
            int begin = Math.max(at - 18, 0);
            System.arraycopy(buf, begin, text, 0, count = at - begin);
            text[count++] = '^';
            int end = Math.min(len, at + 18);
            System.arraycopy(buf, at, text, count, end - at);
            count += end - at;
            return new String(text, 0, count);
        } catch (Throwable throwable) {
            return "";
        }
    }

    protected static String createErrorContextText(byte[] bytes, int at) {
        try {
            int len = bytes.length;
            byte[] text = new byte[40];
            int count;
            int begin = Math.max(at - 18, 0);
            System.arraycopy(bytes, begin, text, 0, count = at - begin);
            text[count++] = '^';
            int end = Math.min(len, at + 18);
            System.arraycopy(bytes, at, text, count, end - at);
            count += end - at;
            return new String(text, 0, count);
        } catch (Throwable throwable) {
            return "";
        }
    }

    protected final static JSONCharArrayWriter getContextWriter(JSONParseContext parseContext) {
        JSONCharArrayWriter jsonWriter = parseContext.getContextWriter();
        if (jsonWriter == null) {
            parseContext.setContextWriter(jsonWriter = new JSONCharArrayWriter());
        }
        return jsonWriter;
    }

    protected final static char[] getChars(String value) {
        return UnsafeHelper.getChars(value);
    }

    /**
     * get value
     *
     * @param value
     * @return
     */
    protected final static Object getStringValue(String value) {
        return JSONUnsafe.getStringValue(value.toString());
    }


}
