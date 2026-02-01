package io.github.wycst.wast.json;

import io.github.wycst.wast.common.compiler.MemoryClassLoader;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.ByteUtils;
import io.github.wycst.wast.common.utils.EnvUtils;

// Provide memory API that only supports internal use and can bypass security checks
final class JSONMemoryHandle {

    static final JSONEndian JSON_ENDIAN;
    static final JSONEndian JSON_ENDIAN_RAW;
    static final JSONEndian JSON_ENDIAN_UNSAFE;

    static {
        JSON_ENDIAN_UNSAFE = EnvUtils.BIG_ENDIAN ? new JSONEndianBigUnsafe() : new JSONEndianLittleUnsafe();
        JSON_ENDIAN_RAW = new JSONEndianRawCode();
        JSONEndian jsonEndian = JSON_ENDIAN_UNSAFE;
        try {
            // -Dwast.json.required-memory-alignment=true
            if (JSONVmOptions.isRequiredMemoryAlignment()) {
                if (EnvUtils.JDK_9_PLUS) {
                    MemoryClassLoader memoryClassLoader = new MemoryClassLoader();
                    memoryClassLoader.loadClass("io.github.wycst.wast.json.JSONEndianVarHandle",
                            ByteUtils.hexString2Bytes("CAFEBABE0000003500750A001A0051053030303030303030090019005209001900530A00540055090019005609001900570A005400580500000000000000FF09001900590A0054005A07005B0A000E00510A0054005C0A0054005D0A0054005E0700220A005F00600A0061006207006307006409005F006507006607006701000E4C4F4E475F48414E444C455F424101001C4C6A6176612F6C616E672F696E766F6B652F56617248616E646C653B010011494E54454745525F48414E444C455F424101000F53484F52545F48414E444C455F424101000D4C4954544C455F454E4449414E0100015A01000850414444494E47530100025B4A0100063C696E69743E010003282956010004436F646501000F4C696E654E756D6265725461626C650100124C6F63616C5661726961626C655461626C650100047468697301002F4C696F2F6769746875622F77796373742F776173742F6A736F6E2F4A534F4E456E6469616E56617248616E646C653B01000867657453686F7274010006285B424929530100036275660100025B420100066F66667365740100014901000D74776F427974657356616C7565010006285B4249294901000372656D01000D537461636B4D61705461626C65010006676574496E740100016901000376616C010004626974730100076765744C6F6E67010006285B4249294A0100014A01000E676574537472696E6756616C7565010026284C6A6176612F6C616E672F537472696E673B294C6A6176612F6C616E672F4F626A6563743B01000576616C75650100124C6A6176612F6C616E672F537472696E673B010010637265617465537472696E674A444B38010016285B43294C6A6176612F6C616E672F537472696E673B0100025B430100116372656174654173636969537472696E67010016285B42294C6A6176612F6C616E672F537472696E673B01000A6173636969427974657301000870757453686F7274010007285B424953294901000153010006707574496E74010007285B4349492949010007285B42494929490100077075744C6F6E67010007285B43494A2949010007285B42494A29490100083C636C696E69743E01000A536F7572636546696C650100184A534F4E456E6469616E56617248616E646C652E6A6176610C002300240C002100220C001E001C0700680C0069002B0C001F00200C001D001C0C006900310C001B001C0C006900390100276A6176612F6C616E672F556E737570706F727465644F7065726174696F6E457863657074696F6E0C006A006B0C006A006C0C006A006D07006E0C006F00700700710C007200730100025B490100025B530C001F007401002D696F2F6769746875622F77796373742F776173742F6A736F6E2F4A534F4E456E6469616E56617248616E646C65010024696F2F6769746875622F77796373742F776173742F6A736F6E2F4A534F4E456E6469616E01001A6A6176612F6C616E672F696E766F6B652F56617248616E646C65010003676574010003736574010007285B4249532956010007285B4249492956010007285B42494A29560100126A6176612F6E696F2F427974654F7264657201000B6E61746976654F7264657201001628294C6A6176612F6E696F2F427974654F726465723B01001E6A6176612F6C616E672F696E766F6B652F4D6574686F6448616E646C65730100166279746541727261795669657756617248616E646C65010043284C6A6176612F6C616E672F436C6173733B4C6A6176612F6E696F2F427974654F726465723B294C6A6176612F6C616E672F696E766F6B652F56617248616E646C653B0100144C6A6176612F6E696F2F427974654F726465723B04210019001A000000050019001B001C00000019001D001C00000019001E001C00000018001F002000000000002100220000000E000100230024000100250000004600060001000000182AB700012A05BC0B590314000250590414000250B50004B10000000200260000000A0002000000070004001600270000000C0001000000180028002900000011002A002B00010025000000470003000300000009B200052B1CB60006AC0000000200260000000600010000003400270000002000030000000900280029000000000009002C002D000100000009002E002F000200100030003100010025000000AB000300040000003C2BBE1C643E1D05A2002CB200079900131D9A000703A7000A2B1C331100FF7EAC1D9A000703A7000D2B1C331100FF7E100878ACB200052B1CB60006AC0000000300260000001A00060000003800050039000A003A0010003B0020003D0033004000270000002A00040000003C0028002900000000003C002C002D00010000003C002E002F0002000500370032002F000300330000000D0006FC00180146010007490100001100340031000100250000013900030006000000662BBE1C6407A20058B2000799002B033E0336041C360515052BBEA2001A1D2B1505331100FF7E150478803E840408840501A7FFE51DAC033E101836041C360515052BBEA2001A1D2B1505331100FF7E150478803E8404F8840501A7FFE5B200082B1CB60009AC0000000300260000004200100000004500080046000E00470010004800130049001D004A002B004B002E00490034004D0036004F00380050003C0051004600520054005300570051005D005700270000005C00090016001E0035002F0005001000260036002F0003001300230037002F0004003F001E0035002F0005003800250036002F0003003C00210037002F00040000006600280029000000000066002C002D000100000066002E002F00020033000000170005FE0016010101FA001DF90001FE0008010101F8001D001100380039000100250000013C00060007000000692BBE1C641008A2005AB2000799002C09420336051C360615062BBEA2001B212B1506338514000A7F1505798142840508840601A7FFE421AD0942103836051C360615062BBEA2001B212B1506338514000A7F15057981428405F8840601A7FFE4B2000C2B1CB6000DAD0000000300260000004200100000005C0009005D000F005E0011005F00140060001E0061002D0062003000600036006400380066003A0067003E0068004800690057006A005A00680060006E00270000005C00090017001F0035002F0006001100270036003A0003001400240037002F00050041001F0035002F0006003A00260036003A0003003E00220037002F00050000006900280029000000000069002C002D000100000069002E002F00020033000000170005FE0017040101FA001EF90001FE0008040101F8001E0001003B003C000100250000003C0002000200000008BB000E59B7000FBF0000000200260000000600010000007400270000001600020000000800280029000000000008003D003E00010011003F0040000100250000003C0002000200000008BB000E59B7000FBF0000000200260000000600010000007900270000001600020000000800280029000000000008002C00410001000100420043000100250000003C0002000200000008BB000E59B7000FBF00000002002600000006000100000083002700000016000200000008002800290000000000080044002D00010011004500460001002500000057000400040000000BB200052B1C1DB6001005AC0000000200260000000A00020000008D0009008E00270000002A00040000000B0028002900000000000B002C002D00010000000B002E002F00020000000B003D00470003040100480049000000110048004A0001002500000057000400040000000BB200082B1C1DB6001107AC0000000200260000000A0002000000960009009700270000002A00040000000B0028002900000000000B002C002D00010000000B002E002F00020000000B003D002F00030401004B004C00000011004B004D0001002500000058000500050000000CB2000C2B1C21B600121008AC0000000200260000000A00020000009F000900A000270000002A00040000000C0028002900000000000C002C002D00010000000C002E002F00020000000C003D003A00030008004E0024000100250000007200020000000000331213B80014B80015B3000C1216B80014B80015B300081217B80014B80015B30005B80014B20018A6000704A7000403B30007B1000000020026000000220008000000090002000A000B000B000D000C0016000D0018000E002100100032002D00330000000500022E40010001004F000000020050"));
                    if (EnvUtils.BIG_ENDIAN) {
                        jsonEndian = (JSONEndian) memoryClassLoader.loadClass("io.github.wycst.wast.json.JSONBigEndianVarHandle", ByteUtils.hexString2Bytes("CAFEBABE0000003500550A000F00460A000E00470A000E004805000000002D00002D0500003A00003A00000A000E0049030000F0F00A000E004A03FFF0FFF003003000300A000E004B07004C07004D0100063C696E69743E010003282956010004436F646501000F4C696E654E756D6265725461626C650100124C6F63616C5661726961626C655461626C65010004746869730100324C696F2F6769746875622F77796373742F776173742F6A736F6E2F4A534F4E426967456E6469616E56617248616E646C653B01000A6D65726765496E74333201000628534343294901000873686F727456616C01000153010003707265010001430100047375666601000A6D65726765496E743634010006284A4A4A294A01000376616C0100014A010005284A4A294A0100036833320100036C33320100116D6572676559656172416E644D6F6E7468010005284949294A01000479656172010001490100056D6F6E746801000B6D6572676548484D4D535301000628494949294A010004686F75720100066D696E7574650100067365636F6E6401000C646967697473324279746573010006285B424929490100016C010001680100036275660100025B420100066F666673657401000B62696753686F727456616C01000D537461636B4D61705461626C6501000C646967697473324368617273010006285B434929490100025B43010009626967496E7456616C010006676574496E740100076765744C6F6E67010006285B4349294A010006707574496E74010007285B434949294901000576616C75650100077075744C6F6E67010007285B43494A294901000A536F7572636546696C6501001B4A534F4E426967456E6469616E56617248616E646C652E6A6176610C001000110C004E004F0C005000510C005200300C005300390C0054003E010030696F2F6769746875622F77796373742F776173742F6A736F6E2F4A534F4E426967456E6469616E56617248616E646C6501002D696F2F6769746875622F77796373742F776173742F6A736F6E2F4A534F4E456E6469616E56617248616E646C65010016676574466F75724469676974734269747356616C75650100042849294901001567657454776F4469676974734269747356616C75650100042849295301000D74776F427974657356616C7565010008676574496E7442450100096765744C6F6E6742450021000E000F00000000000C000100100011000100120000002F00010001000000052AB70001B10000000200130000000600010000000300140000000C0001000000050015001600000001001700180001001200000054000300040000000C1C1018781B100878801D80AC0000000200130000000600010000000700140000002A00040000000C0015001600000000000C0019001A00010000000C001B001C00020000000C001D001C00030001001E001F0001001200000055000500070000000D211030791F10107981160581AD0000000200130000000600010000000C00140000002A00040000000D0015001600000000000D0020002100010000000D001B002100030000000D001D002100050001001E0022000100120000004500040005000000071F1020792181AD000000020013000000060001000000110014000000200003000000070015001600000000000700230021000100000007002400210003000100250026000100120000005400040003000000161BB80002851020791CB80003100878858114000481AD0000000200130000000600010000001600140000002000030000001600150016000000000016002700280001000000160029002800020001002A002B0001001200000064000500040000001C1400061BB8000385103079811CB8000385101879811DB800038581AD0000000200130000000600010000001B00140000002A00040000001C0015001600000000001C002C002800010000001C002D002800020000001C002E002800030001002F003000010012000000C1000300060000003F2A2B1CB600083E1D12097E113030A0002F1D100F7E36041D10087A100F7E360515051009A3000A15041009A4000502AC150506781505047860150460AC02AC0000000300130000001A00060000002000070021001100220020002300300024003D002600140000003E0006001700260031002800040020001D0032002800050000003F0015001600000000003F0033003400010000003F0035002800020007003800360028000300370000000C0003FE002E01010101F9000C00010038003900010012000000BF000300060000003D2B1CB8000A3E1D120B7E120CA0002F1D100F7E36041D10107A100F7E360515051009A3000A15041009A4000502AC150506781505047860150460AC02AC0000000300130000001A00060000002F00060030000F0031001E0032002E0033003B003500140000003E000600150026003100280004001E001D0032002800050000003D0015001600000000003D0033003A00010000003D00350028000200060037003B0028000300370000000C0003FE002C01010101F9000C0001003C0039000100120000004400020003000000062B1CB8000AAC0000000200130000000600010000003B001400000020000300000006001500160000000000060033003A0001000000060035002800020001003D003E000100120000004400020003000000062B1CB8000DAD00000002001300000006000100000040001400000020000300000006001500160000000000060033003A0001000000060035002800020001003F0040000100120000006100040004000000112B1C1D10107A92552B1C04601D925505AC0000000200130000000E00030000004500080046000F004700140000002A000400000011001500160000000000110033003A00010000001100350028000200000011004100280003000100420043000100120000008100050005000000292B1C2110307B8892552B1C04602110207B8892552B1C05602110107B8892552B1C06602188925507AC0000000200130000001600050000004C0009004D0014004E001F004F0027005000140000002A000400000029001500160000000000290033003A0001000000290035002800020000002900410021000300010044000000020045")).newInstance();
                    } else {
                        jsonEndian = (JSONEndian) memoryClassLoader.loadClass("io.github.wycst.wast.json.JSONLittleEndianVarHandle", ByteUtils.hexString2Bytes("CAFEBABE0000003500560A00100045052D00002D000000000A000F00460A000F00470500003A00003A00000A000F004803FFF0FFF003003000300A000F00490A000F004A030000F0F00A000F004B07004C07004D0100063C696E69743E010003282956010004436F646501000F4C696E654E756D6265725461626C650100124C6F63616C5661726961626C655461626C65010004746869730100354C696F2F6769746875622F77796373742F776173742F6A736F6E2F4A534F4E4C6974746C65456E6469616E56617248616E646C653B01000A6D65726765496E74333201000628534343294901000873686F727456616C01000153010003707265010001430100047375666601000A6D65726765496E743634010006284A4A4A294A01000376616C0100014A010005284A4A294A0100036833320100036C33320100116D6572676559656172416E644D6F6E7468010005284949294A01000479656172010001490100056D6F6E746801000B6D6572676548484D4D535301000628494949294A010004686F75720100066D696E7574650100067365636F6E6401000C646967697473324368617273010006285B43492949010001680100026C380100036275660100025B430100066F666673657401000576616C756501000D537461636B4D61705461626C6501000C646967697473324279746573010006285B424929490100025B420100076765744C6F6E67010006285B4349294A010006676574496E74010006707574496E74010007285B43494929490100077075744C6F6E67010007285B43494A294901000A536F7572636546696C6501001E4A534F4E4C6974746C65456E6469616E56617248616E646C652E6A6176610C001100120C004E004F0C005000510C005200310C005300510C0054003A0C0055003D010033696F2F6769746875622F77796373742F776173742F6A736F6E2F4A534F4E4C6974746C65456E6469616E56617248616E646C6501002D696F2F6769746875622F77796373742F776173742F6A736F6E2F4A534F4E456E6469616E56617248616E646C6501001567657454776F4469676974734269747356616C756501000428492953010016676574466F75724469676974734269747356616C756501000428492949010008676574496E744C4501001167657454776F44696769747356616C756501000D74776F427974657356616C75650100096765744C6F6E674C450021000F001000000000000C000100110012000100130000002F00010001000000052AB70001B10000000200140000000600010000000300150000000C0001000000050016001700000001001800190001001300000054000300040000000C1D1018781B100878801C80AC0000000200140000000600010000000600150000002A00040000000C0016001700000000000C001A001B00010000000C001C001D00020000000C001E001D00030001001F00200001001300000055000500070000000D16051030791F101079812181AD0000000200140000000600010000000B00150000002A00040000000D0016001700000000000D0021002200010000000D001C002200030000000D001E002200050001001F002300010013000000450004000500000007211020791F81AD000000020014000000060001000000100015000000200003000000070016001700000000000700240022000100000007002500220003000100260027000100130000005100050003000000131400021CB8000485102879811BB800058581AD000000020014000000060001000000160015000000200003000000130016001700000000001300280029000100000013002A002900020001002B002C0001001300000064000500040000001C1400061DB8000485103079811CB8000485101879811BB800048581AD0000000200140000000600010000001C00150000002A00040000001C0016001700000000001C002D002900010000001C002E002900020000001C002F0029000300010030003100010013000000A2000200060000002A2B1CB800083E1D12097E120AA0001C1D103F7E36041D100C7A1100F07E36051504150582B8000BAC02AC0000000300140000001600050000002100060023000F0024001F00250028002700150000003E000600150013003200290004001F00090033002900050000002A0016001700000000002A0034003500010000002A003600290002000600240037002900030038000000060001FC00280100010039003A00010013000000A3000300060000002B2A2B1CB6000C3E1D120D7E113030A0001B1D103F7E36041D077A1100F07E36051504150582B8000BAC02AC0000000300140000001600050000002D0007002E00110030002000310029003300150000003E000600170012003200290004002000090033002900050000002B0016001700000000002B0034003B00010000002B003600290002000700240037002900030038000000060001FC0029010001003C003D000100130000004400020003000000062B1CB8000EAD0000000200140000000600010000003900150000002000030000000600160017000000000006003400350001000000060036002900020001003E0031000100130000004400020003000000062B1CB80008AC0000000200140000000600010000003E00150000002000030000000600160017000000000006003400350001000000060036002900020001003F0040000100130000006100040004000000112B1C1D92552B1C04601D10107A925505AC0000000200140000000E00030000004300050044000F004500150000002A000400000011001600170000000000110034003500010000001100360029000200000011003700290003000100410042000100130000008100050005000000292B1C218892552B1C04602110107B8892552B1C05602110207B8892552B1C06602110307B88925507AC0000000200140000001600050000004A0006004B0011004C001C004D0027004E00150000002A00040000002900160017000000000029003400350001000000290036002900020000002900370022000300010043000000020044")).newInstance();
                    }
                } else {
                    jsonEndian = JSON_ENDIAN_RAW;
                }
            }
        } catch (Exception e) {
            // fallback use UNSAFE
        }
        JSON_ENDIAN = jsonEndian;
    }

    static abstract class Optimizer {

        public String[] copy(String[] buf, int offset, int len) {
            String[] result = new String[len];
            System.arraycopy(buf, offset, result, 0, len);
            return result;
        }

        public double[] copy(double[] buf, int offset, int len) {
            double[] result = new double[len];
            System.arraycopy(buf, offset, result, 0, len);
            return result;
        }

        public long[] copy(long[] buf, int offset, int len) {
            long[] result = new long[len];
            System.arraycopy(buf, offset, result, 0, len);
            return result;
        }

        public char[] copyChars(char[] buf, int offset, int len) {
            char[] result = new char[len];
            System.arraycopy(buf, offset, result, 0, len);
            return result;
        }

        public byte[] copyBytes(byte[] buf, int offset, int len) {
            byte[] result = new byte[len];
            System.arraycopy(buf, offset, result, 0, len);
            return result;
        }

//        /**
//         * 8个字节以内的数据拷贝（注意sOff和dOff都大于0）
//         *
//         * @param src
//         * @param sOff
//         * @param dst
//         * @param dOff
//         * @param len (len < 8)
//         */
//        protected void copyRemBytes(byte[] src, int sOff, byte[] dst, int dOff, int len) {
//            putLong(dst, dOff - 8 + len, getLong(src, sOff - 8 + len));
//        }

        void multipleCopyMemory(byte[] source, long sourceOff, byte[] target, long targetOff) {
        }

        void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
        }

        public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
            char[] result = new char[len];
            for (int j = 0; j < len; ++j) {
                result[j] = (char) buf[offset + j];
            }
            return result;
        }
    }

    final static Optimizer[] SIZE_INSTANCES = new Optimizer[]{
            s0(),
            s1(),
            s2(),
            s3(),
            s4(),
            s5(),
            s6(),
            s7(),
            s8(),
            s9(),
            s10(),
            s11(),
            s12(),
            s13(),
            s14(),
            s15(),
            s16(),
            s17(),
            s18(),
            s19(),
            s20(),
    };
    final static int SIZE_LEN = SIZE_INSTANCES.length;

    static Optimizer s0() {
        return new Optimizer() {
            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                return JSONGeneral.EMPTY_CHARS;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                return JSONGeneral.EMPTY_BYTES;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return JSONGeneral.EMPTY_STRINGS;
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return JSONGeneral.EMPTY_DOUBLES;
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return JSONGeneral.EMPTY_LONGS;
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return JSONGeneral.EMPTY_CHARS;
            }
        };
    }

    static Optimizer s1() {
        return new Optimizer() {
            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                return new char[]{buf[offset]};
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset]};
            }

//            @Override
//            protected void copyRemBytes(byte[] src, int sOff, byte[] dst, int dOff, int len) {
//                dst[dOff] = src[sOff];
//            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset]};
            }

            void multipleCopyMemory(byte[] source, long sourceOff, byte[] target, long targetOff) {
                // TODO
//                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                target[targetOff] = source[sourceOff];
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset]};
            }
        };
    }

    static Optimizer s2() {
        return new Optimizer() {

            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[2];
                putInt(chars, 0, getInt(buf, offset));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[2];
                putShort(bytes, 0, getShort(buf, offset));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset]};
            }

            void multipleCopyMemory(byte[] source, long sourceOff, byte[] target, long targetOff) {
                // TODO
//                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
//                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
//                UNSAFE.putShort(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getShort(source, BYTE_ARRAY_OFFSET + sourceOff));
                JSON_ENDIAN.putShort(target, targetOff, JSON_ENDIAN.getShort(source, sourceOff));
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s3() {
        return new Optimizer() {

            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[3];
                putInt(chars, 0, getInt(buf, offset));
                chars[2] = buf[offset + 2];
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[3];
                putShort(bytes, 0, getShort(buf, offset));
                bytes[2] = buf[offset + 2];
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset]};
            }

            void multipleCopyMemory(byte[] source, long sourceOff, byte[] target, long targetOff) {
                // TODO
//                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
//                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
//                UNSAFE.putLong(target, targetOff + 16, UNSAFE.getLong(source, sourceOff + 16));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                JSON_ENDIAN.putShort(target, targetOff, JSON_ENDIAN.getShort(source, sourceOff));
//                UNSAFE.putShort(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getShort(source, BYTE_ARRAY_OFFSET + sourceOff));
                target[targetOff + 2] = source[sourceOff + 2];
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s4() {
        return new Optimizer() {

            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[4];
                putLong(chars, 0, getLong(buf, offset));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[4];
                putInt(bytes, 0, getInt(buf, offset));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            void multipleCopyMemory(byte[] source, long sourceOff, byte[] target, long targetOff) {
                // todo
//                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
//                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
//                UNSAFE.putLong(target, targetOff + 16, UNSAFE.getLong(source, sourceOff + 16));
//                UNSAFE.putLong(target, targetOff + 24, UNSAFE.getLong(source, sourceOff + 24));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                JSON_ENDIAN.putInt(target, targetOff, JSON_ENDIAN.getInt(source, sourceOff));
//                UNSAFE.putInt(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getInt(source, BYTE_ARRAY_OFFSET + sourceOff));
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s5() {
        return new Optimizer() {

            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[5];
                putLong(chars, 0, getLong(buf, offset));
                chars[4] = buf[offset + 4];
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[5];
                putInt(bytes, 0, getInt(buf, offset));
                bytes[4] = buf[offset + 4];
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            void multipleCopyMemory(byte[] source, long sourceOff, byte[] target, long targetOff) {
                // TODO
//                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
//                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
//                UNSAFE.putLong(target, targetOff + 16, UNSAFE.getLong(source, sourceOff + 16));
//                UNSAFE.putLong(target, targetOff + 24, UNSAFE.getLong(source, sourceOff + 24));
//                UNSAFE.putLong(target, targetOff + 32, UNSAFE.getLong(source, sourceOff + 32));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                JSON_ENDIAN.putInt(target, targetOff, JSON_ENDIAN.getInt(source, sourceOff));
//                UNSAFE.putInt(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getInt(source, BYTE_ARRAY_OFFSET + sourceOff));
                target[targetOff + 4] = source[sourceOff + 4];
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s6() {
        return new Optimizer() {

            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[6];
                putLong(chars, 0, getLong(buf, offset));
                putInt(chars, 4, getInt(buf, offset + 4));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[6];
                putInt(bytes, 0, getInt(buf, offset));
                putShort(bytes, 4, getShort(buf, offset + 4));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            void multipleCopyMemory(byte[] source, long sourceOff, byte[] target, long targetOff) {
                // TODO
//                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
//                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
//                UNSAFE.putLong(target, targetOff + 16, UNSAFE.getLong(source, sourceOff + 16));
//                UNSAFE.putLong(target, targetOff + 24, UNSAFE.getLong(source, sourceOff + 24));
//                UNSAFE.putLong(target, targetOff + 32, UNSAFE.getLong(source, sourceOff + 32));
//                UNSAFE.putLong(target, targetOff + 40, UNSAFE.getLong(source, sourceOff + 40));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                JSON_ENDIAN.putInt(target, targetOff, JSON_ENDIAN.getInt(source, sourceOff));
                JSON_ENDIAN.putShort(target, targetOff + 4, JSON_ENDIAN.getShort(source, sourceOff + 4));
//                UNSAFE.putInt(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getInt(source, BYTE_ARRAY_OFFSET + sourceOff));
//                UNSAFE.putShort(target, BYTE_ARRAY_OFFSET + targetOff + 4, UNSAFE.getShort(source, BYTE_ARRAY_OFFSET + sourceOff + 4));
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s7() {
        return new Optimizer() {
            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[7];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 3, getLong(buf, offset + 3));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[7];
                putInt(bytes, 0, getInt(buf, offset));
                putInt(bytes, 3, getInt(buf, offset + 3));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            void multipleCopyMemory(byte[] source, long sourceOff, byte[] target, long targetOff) {
                // TODO
//                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
//                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
//                UNSAFE.putLong(target, targetOff + 16, UNSAFE.getLong(source, sourceOff + 16));
//                UNSAFE.putLong(target, targetOff + 24, UNSAFE.getLong(source, sourceOff + 24));
//                UNSAFE.putLong(target, targetOff + 32, UNSAFE.getLong(source, sourceOff + 32));
//                UNSAFE.putLong(target, targetOff + 40, UNSAFE.getLong(source, sourceOff + 40));
//                UNSAFE.putLong(target, targetOff + 48, UNSAFE.getLong(source, sourceOff + 48));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                JSON_ENDIAN.putInt(target, targetOff, JSON_ENDIAN.getInt(source, sourceOff));
                JSON_ENDIAN.putShort(target, targetOff + 4, JSON_ENDIAN.getShort(source, sourceOff + 4));
//                UNSAFE.putInt(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getInt(source, BYTE_ARRAY_OFFSET + sourceOff));
//                UNSAFE.putShort(target, BYTE_ARRAY_OFFSET + targetOff + 4, UNSAFE.getShort(source, BYTE_ARRAY_OFFSET + sourceOff + 4));
                target[targetOff + 6] = source[sourceOff + 6];
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s8() {
        return new Optimizer() {

            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[8];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 4, getLong(buf, offset + 4));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[8];
                putLong(bytes, 0, getLong(buf, offset));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            void multipleCopyMemory(byte[] source, long sourceOff, byte[] target, long targetOff) {
                // TODO
//                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
//                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
//                UNSAFE.putLong(target, targetOff + 16, UNSAFE.getLong(source, sourceOff + 16));
//                UNSAFE.putLong(target, targetOff + 24, UNSAFE.getLong(source, sourceOff + 24));
//                UNSAFE.putLong(target, targetOff + 32, UNSAFE.getLong(source, sourceOff + 32));
//                UNSAFE.putLong(target, targetOff + 40, UNSAFE.getLong(source, sourceOff + 40));
//                UNSAFE.putLong(target, targetOff + 48, UNSAFE.getLong(source, sourceOff + 48));
//                UNSAFE.putLong(target, targetOff + 56, UNSAFE.getLong(source, sourceOff + 56));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                JSON_ENDIAN.putLong(target, targetOff, JSON_ENDIAN.getLong(source, sourceOff));
//                UNSAFE.putLong(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getLong(source, BYTE_ARRAY_OFFSET + sourceOff));
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s9() {
        return new Optimizer() {
            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[9];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 4, getLong(buf, offset + 4));
                chars[8] = buf[offset + 8];
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[9];
                putLong(bytes, 0, getLong(buf, offset));
                bytes[8] = buf[offset + 8];
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s10() {
        return new Optimizer() {
            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[10];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 4, getLong(buf, offset + 4));
                putInt(chars, 8, getInt(buf, offset + 8));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[10];
                putLong(bytes, 0, getLong(buf, offset));
                putShort(bytes, 8, getShort(buf, offset + 8));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s11() {
        return new Optimizer() {
            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[11];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 4, getLong(buf, offset + 4));
                putLong(chars, 7, getLong(buf, offset + 7));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[11];
                putLong(bytes, 0, getLong(buf, offset));
                putInt(bytes, 7, getInt(buf, offset + 7));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s12() {
        return new Optimizer() {
            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[12];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 4, getLong(buf, offset + 4));
                putLong(chars, 8, getLong(buf, offset + 8));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[12];
                putLong(bytes, 0, getLong(buf, offset));
                putInt(bytes, 8, getInt(buf, offset + 8));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s13() {
        return new Optimizer() {
            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[13];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 4, getLong(buf, offset + 4));
                putLong(chars, 8, getLong(buf, offset + 8));
                chars[12] = buf[offset + 12];
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[13];
                putLong(bytes, 0, getLong(buf, offset));
                putInt(bytes, 8, getInt(buf, offset + 8));
                bytes[12] = buf[offset + 12];
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s14() {
        return new Optimizer() {
            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[14];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 4, getLong(buf, offset + 4));
                putLong(chars, 8, getLong(buf, offset + 8));
                putInt(chars, 12, getInt(buf, offset + 12));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[14];
                putLong(bytes, 0, getLong(buf, offset));
                putLong(bytes, 6, getLong(buf, offset + 6));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s15() {
        return new Optimizer() {
            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[15];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 4, getLong(buf, offset + 4));
                putLong(chars, 8, getLong(buf, offset + 8));
                putLong(chars, 11, getLong(buf, offset + 11));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[15];
                putLong(bytes, 0, getLong(buf, offset));
                putLong(bytes, 7, getLong(buf, offset + 7));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s16() {
        return new Optimizer() {
            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[16];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 4, getLong(buf, offset + 4));
                putLong(chars, 8, getLong(buf, offset + 8));
                putLong(chars, 12, getLong(buf, offset + 12));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[16];
                putLong(bytes, 0, getLong(buf, offset));
                putLong(bytes, 8, getLong(buf, offset + 8));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s17() {
        return new Optimizer() {

            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[17];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 4, getLong(buf, offset + 4));
                putLong(chars, 8, getLong(buf, offset + 8));
                putLong(chars, 12, getLong(buf, offset + 12));
                chars[16] = buf[offset + 16];
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[17];
                putLong(bytes, 0, getLong(buf, offset));
                putLong(bytes, 8, getLong(buf, offset + 8));
                bytes[16] = buf[offset + 16];
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s18() {
        return new Optimizer() {

            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[18];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 4, getLong(buf, offset + 4));
                putLong(chars, 8, getLong(buf, offset + 8));
                putLong(chars, 12, getLong(buf, offset + 12));
                putInt(chars, 16, getInt(buf, offset + 16));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[18];
                putLong(bytes, 0, getLong(buf, offset));
                putLong(bytes, 8, getLong(buf, offset + 8));
                putShort(bytes, 16, getShort(buf, offset + 16));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s19() {
        return new Optimizer() {

            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[19];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 4, getLong(buf, offset + 4));
                putLong(chars, 8, getLong(buf, offset + 8));
                putLong(chars, 12, getLong(buf, offset + 12));
                putLong(chars, 15, getLong(buf, offset + 15));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[19];
                putLong(bytes, 0, getLong(buf, offset));
                putLong(bytes, 8, getLong(buf, offset + 8));
                putInt(bytes, 15, getInt(buf, offset + 15));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static Optimizer s20() {
        return new Optimizer() {

            @Override
            public char[] copyChars(char[] buf, int offset, int len) {
                char[] chars = new char[20];
                putLong(chars, 0, getLong(buf, offset));
                putLong(chars, 4, getLong(buf, offset + 4));
                putLong(chars, 8, getLong(buf, offset + 8));
                putLong(chars, 12, getLong(buf, offset + 12));
                putLong(chars, 16, getLong(buf, offset + 16));
                return chars;
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[20];
                putLong(bytes, 0, getLong(buf, offset));
                putLong(bytes, 8, getLong(buf, offset + 8));
                putInt(bytes, 16, getInt(buf, offset + 16));
                return bytes;
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return new char[]{(char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset++], (char) buf[offset]};
            }
        };
    }

    static long getLong(byte[] buf, int offset) {
        return JSON_ENDIAN.getLong(buf, offset);
    }

    static long getLong(char[] buf, int offset) {
        return JSON_ENDIAN.getLong(buf, offset);
    }

    static int getInt(byte[] buf, int offset) {
        return JSON_ENDIAN.getInt(buf, offset);
    }

    static int getInt(char[] buf, int offset) {
        return JSON_ENDIAN.getInt(buf, offset);
    }

    static short getShort(byte[] buf, int offset) {
        return JSON_ENDIAN.getShort(buf, offset);
    }

    static int putInt(char[] buf, int offset, int value) {
        JSON_ENDIAN.putInt(buf, offset, value);
        return 2;
    }

    static int putLong(char[] buf, int offset, long value) {
        JSON_ENDIAN.putLong(buf, offset, value);
        return 4;
    }

    static int putLong(byte[] buf, int offset, long value) {
        JSON_ENDIAN.putLong(buf, offset, value);
        return 8;
    }

    static int putInt(byte[] buf, int offset, int value) {
        JSON_ENDIAN.putInt(buf, offset, value);
        return 4;
    }

    static int putShort(byte[] buf, int offset, short value) {
        JSON_ENDIAN.putShort(buf, offset, value);
        return 2;
    }

    static int[] getByteInts(String value) {
        byte[] bytes = value.getBytes();
        int byteLen = bytes.length;
        int l = byteLen >> 2;
        int rem = byteLen & 3;
        if (rem > 0) {
            ++l;
        }
        byte[] buf = new byte[l << 2];
        System.arraycopy(bytes, 0, buf, 0, byteLen);
        int[] results = new int[l];
        int offset = 0;
        for (int i = 0; i < l; ++i) {
            results[i] = getInt(buf, offset);
            offset += 4;
        }
        return results;
    }

    static long[] getCharLongs(String value) {
        char[] chars = UnsafeHelper.getChars(value);
        int strLength = chars.length;
        int l = strLength >> 2;
        int rem = strLength & 3;
        if (rem > 0) {
            ++l;
        }
        char[] buf = new char[l << 2];
        value.getChars(0, strLength, buf, 0);

        long[] results = new long[l];
        int offset = 0;
        for (int i = 0; i < l; ++i) {
            results[i] = getLong(buf, offset);
            offset += 4;
        }
        return results;
    }

    static long[] getByteLongs(String value) {
        byte[] bytes = value.getBytes();
        int byteLen = bytes.length;
        int l = byteLen >> 3;
        int rem = byteLen & 7;
        if (rem > 0) {
            ++l;
        }
        byte[] buf = new byte[l << 3];
        System.arraycopy(bytes, 0, buf, 0, byteLen);
        long[] results = new long[l];
        int offset = 0;
        for (int i = 0; i < l; ++i) {
            results[i] = getLong(buf, offset);
            offset += 8;
        }
        return results;
    }

    public static Object getStringValue(String value) {
//        return JSON_ENDIAN.getStringValue(value);
        return JSON_ENDIAN_UNSAFE.getStringValue(value);
    }

    static String createStringJDK8(char[] buf, int beginIndex, int len) {
        return JSON_ENDIAN_UNSAFE.createStringJDK8(copyChars(buf, beginIndex, len));
    }

    static String createStringByAsciiBytesJDK8(byte[] buf, int beginIndex, int endIndex) {
        int len = endIndex - beginIndex;
        char[] result;
        if (len < SIZE_LEN) {
            result = SIZE_INSTANCES[len].asciiBytesToChars(buf, beginIndex, len);
        } else {
            result = new char[len];
            for (int j = 0; j < len; ++j) {
                result[j] = (char) buf[beginIndex + j];
            }
        }
        return JSON_ENDIAN_UNSAFE.createStringJDK8(result);
    }

//    /**
//     * 8个字节以内的数据拷贝（注意sOff和dOff都大于0）
//     *
//     * @param src
//     * @param sOff
//     * @param dst
//     * @param dOff
//     * @param len (len < 8)
//     */
//    static void copyRemBytes(byte[] src, int sOff, byte[] dst, int dOff, int len) {
//        // SIZE_INSTANCES[len].copyRemBytes(src, sOff, dst, dOff, len);
//        putLong(dst, dOff - 8 + len, getLong(src, sOff - 8 + len));
//    }

    static int asciiBytesToChars(byte[] buf, int beginIndex, int endIndex, char[] chars, int offset) {
        int len = endIndex - beginIndex;
        for (int j = 0; j < len; ++j) {
            chars[offset + j] = (char) buf[beginIndex + j];
        }
        return len;
    }

    public static char[] copyChars(char[] buf, int offset, int len) {
        if (len < SIZE_LEN) {
            return SIZE_INSTANCES[len].copyChars(buf, offset, len);
        } else {
            char[] chars = new char[len];
            System.arraycopy(buf, offset, chars, 0, len);
            return chars;
        }
    }

    public static byte[] copyBytes(byte[] buf, int offset, int len) {
        if (len < SIZE_LEN) {
            return SIZE_INSTANCES[len].copyBytes(buf, offset, len);
        } else {
            byte[] bytes = new byte[len];
//            if (len < 72) {
//                int rem = len & 7;
//                int t = len >> 3;
//                long sourceOff = BYTE_ARRAY_OFFSET + offset;
//                SIZE_INSTANCES[t].multipleCopyMemory(buf, sourceOff, bytes, BYTE_ARRAY_OFFSET);
//                if(rem > 4) {
//                    int remOffset = (t << 3) - 8 + rem;
//                    putLong(bytes, remOffset, getLong(buf, offset + remOffset));
//                } else if(rem > 0) {
//                    int remOffset = (t << 3) - 4 + rem;
//                    putInt(bytes, remOffset, getInt(buf, offset + remOffset));
//                }
//                return bytes;
//            } else {
//            }
            System.arraycopy(buf, offset, bytes, 0, len);
            return bytes;
        }
    }

    public static String[] copyStrings(String[] buf, int offset, int len) {
        if (len < SIZE_LEN) {
            return SIZE_INSTANCES[len].copy(buf, offset, len);
        } else {
            String[] result = new String[len];
            System.arraycopy(buf, offset, result, 0, len);
            return result;
        }
    }

    public static double[] copyDoubles(double[] buf, int offset, int len) {
        if (len < SIZE_LEN) {
            return SIZE_INSTANCES[len].copy(buf, offset, len);
        } else {
            double[] result = new double[len];
            System.arraycopy(buf, offset, result, 0, len);
            return result;
        }
    }

    /**
     * 拷贝long数组
     *
     * @param buf
     * @param offset
     * @param len
     * @return
     */
    public static long[] copyLongs(long[] buf, int offset, int len) {
        if (len < SIZE_LEN) {
            return SIZE_INSTANCES[len].copy(buf, offset, len);
        } else {
            long[] result = new long[len];
            System.arraycopy(buf, offset, result, 0, len);
            return result;
        }
    }

    static String createAsciiString(byte[] bytes, int offset, int len) {
        return JSON_ENDIAN_UNSAFE.createAsciiString(copyBytes(bytes, offset, len));
    }

    static String createAsciiString(byte[] asciiBytes) {
        return JSON_ENDIAN_UNSAFE.createAsciiString(asciiBytes);
    }

    static byte[] getStringUTF8Bytes(String value) {
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) getStringValue(value.toString());
            if (bytes.length == value.length()) {
                return bytes;
            }
        }
        return value.getBytes(EnvUtils.CHARSET_UTF8_OR_DEF);
    }

    /**
     * <p> 高效比较两个字节数组片段 </p>
     * <p>
     * Refer to ArraysSupport # vectorizedMismatch
     *
     * @param a
     * @param aOffset
     * @param b
     * @param bOffset
     * @param len
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static boolean equals(byte[] a, int aOffset, byte[] b, int bOffset, int len, long remValueForBytes) {
        if (len >= 8) {
            do {
                long la = getLong(a, aOffset);
                long lb = getLong(b, bOffset);
                if (la != lb) return false;
                len -= 8;
                aOffset += 8;
                bOffset += 8;
            } while (len >= 8);
            if (len == 0) return true;
            int padd = 8 - len;
            aOffset -= padd;
            bOffset -= padd;
            return getLong(a, aOffset) == getLong(b, bOffset);
        }
        if (len >= 4) {
            int la = getInt(a, aOffset);
            int lb = getInt(b, bOffset);
            if (la != lb) return false;
            int v = len - 4;
            if (v == 0) return true;
            aOffset += v;
            bOffset += v;
            return getInt(a, aOffset) == getInt(b, bOffset);
        }
        // 1 2 3
        switch (len) {
            case 1:
                return a[aOffset] == remValueForBytes;
            case 2:
                return getShort(a, aOffset) == remValueForBytes;
            default:
                return a[aOffset++] == b[bOffset] && getShort(a, aOffset) == remValueForBytes;
        }
    }

    /**
     * <p> 高效比较两个字符数组片段 </p>
     * <p>
     * Refer to ArraysSupport # vectorizedMismatch
     *
     * @param a
     * @param aOffset
     * @param b
     * @param bOffset
     * @param len
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static boolean equals(char[] a, int aOffset, char[] b, int bOffset, int len, long remValueForChars) {
        if (len >= 4) {
            do {
                long la = getLong(a, aOffset);
                long lb = getLong(b, bOffset);
                if (la != lb) return false;
                len -= 4;
                aOffset += 4;
                bOffset += 4;
            } while (len >= 4);
            if (len == 0) return true;
            int v = 4 - len;
            aOffset -= v;
            bOffset -= v;
            return getLong(a, aOffset) == getLong(b, bOffset);
        }
        // 1 2 3
        switch (len) {
            case 1:
                return a[aOffset] == remValueForChars;
            case 2:
                return getInt(a, aOffset) == remValueForChars;
            default:
                return a[aOffset++] == b[bOffset] && getInt(a, aOffset) == remValueForChars;
        }
    }
}
