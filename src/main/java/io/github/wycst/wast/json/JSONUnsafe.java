package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.EnvUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

// Provide an unsafe API that only supports internal use and can bypass security checks
final class JSONUnsafe {

    static final Unsafe UNSAFE;
    static final long BYTE_ARRAY_OFFSET = UnsafeHelper.BYTE_ARRAY_OFFSET;
    static final long CHAR_ARRAY_OFFSET = UnsafeHelper.CHAR_ARRAY_OFFSET;
    static final long STRING_VALUE_OFFSET = UnsafeHelper.STRING_VALUE_OFFSET;
    static final Endian UNSAFE_ENDIAN;

    static abstract class Endian {
        abstract int digits2Bytes(byte[] buf, int offset);

        //        abstract int digits2Chars(char[] buf, int offset);
        abstract int mergeInt32(short shortVal, char pre, char suff);

        abstract long mergeInt64(long val, long pre, long suff);

        abstract long mergeInt64(long h32, long l32);

        abstract long mergeInt64(long s1, long c, long s2, long c1, long s3);
    }

    final static class BigEndian extends Endian {

        @Override
        int digits2Bytes(byte[] buf, int offset) {
            int bigShortVal = UNSAFE.getShort(buf, BYTE_ARRAY_OFFSET + offset);
            if ((bigShortVal & 0xF0F0) == 0x3030) {
                int l = bigShortVal & 0xf, h = (bigShortVal >> 8) & 0xf;
                if (h > 9 || l > 9) return -1;
                return (h << 3) + (h << 1) + l;
            } else {
                return -1;
            }
        }

        @Override
        int mergeInt32(short shortVal, char pre, char suff) {
            return (pre << 24) | (shortVal << 8) | suff;
        }

        @Override
        long mergeInt64(long val, long pre, long suff) {
            return pre << 48 | val << 16 | suff;
        }

        @Override
        long mergeInt64(long h32, long l32) {
            return h32 << 32 | l32;
        }

        @Override
        long mergeInt64(long s1, long c, long s2, long c1, long s3) {
            return s1 << 48 | c << 40 | s2 << 24 | c1 << 16 | s3;
        }
//        @Override
//        int digits2Chars(char[] buf, int offset) {
//            // 0-9 (0000000000110000 ~ 0000000000111001)
//            // 00000000 00110001 00000000 00110001 & 11111111 11110000 11111111 11110000 (0xFFF0FFF0)
//            // 00000000 00110000 00000000 00110000 （0x300030）
//            int bigIntVal = UNSAFE.getInt(buf, CHAR_ARRAY_OFFSET + (offset << 1));
//            if ((bigIntVal & 0xFFF0FFF0) == 0x300030) {
//                int l = bigIntVal & 0xf, h = (bigIntVal >> 16) & 0xf;
//                if (h > 9 || l > 9) return -1;
//                return (h << 3) + (h << 1) + l;
//            } else {
//                return -1;
//            }
//        }
    }

    final static class LittleEndian extends Endian {
        @Override
        int digits2Bytes(byte[] buf, int offset) {
            int littleShortVal = UNSAFE.getShort(buf, BYTE_ARRAY_OFFSET + offset);
            if ((littleShortVal & 0xF0F0) == 0x3030) {
                int h = littleShortVal & 0x3f, l8 = (littleShortVal >> 4) & 0xF0;
                return JSONGeneral.TWO_DIGITS_VALUES[h ^ l8];
            } else {
                return -1;
            }
        }

        @Override
        int mergeInt32(short shortVal, char pre, char suff) {
            return (suff << 24) | (shortVal << 8) | pre;
        }

        @Override
        long mergeInt64(long val, long pre, long suff) {
            return suff << 48 | val << 16 | pre;
        }

        @Override
        long mergeInt64(long h32, long l32) {
            return l32 << 32 | h32;
        }

        @Override
        long mergeInt64(long s1, long c, long s2, long c1, long s3) {
            return s3 << 48 | c1 << 40 | s2 << 24 | c << 16 | s1;
        }
//        @Override
//        int digits2Chars(char[] buf, int offset) {
//            int littleIntVal = UNSAFE.getInt(buf, CHAR_ARRAY_OFFSET + (offset << 1));
//            // 11 0101 00000000 0011 0001 (eg: 15)
//            if ((littleIntVal & 0xFFF0FFF0) == 0x300030) {
//                int h = littleIntVal & 0xf, l = (littleIntVal >> 16) & 0xf;
//                if (h > 9 || l > 9) return -1;
//                return (h << 3) + (h << 1) + l;
//            } else {
//                return -1;
//            }
//        }
    }

    static {
        Field theUnsafeField;
        try {
            theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
        } catch (NoSuchFieldException exception) {
            theUnsafeField = null;
        }
        Unsafe instance = null;
        if (theUnsafeField != null) {
            try {
                instance = (Unsafe) theUnsafeField.get(null);
            } catch (IllegalAccessException exception) {
                throw new RuntimeException(exception);
            }
        }
        UNSAFE = instance;
        UNSAFE_ENDIAN = EnvUtils.BIG_ENDIAN ? new BigEndian() : new LittleEndian();
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
                return new char[]{};
            }

            @Override
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                return new byte[]{};
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[]{};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[]{};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[]{};
            }

            @Override
            public char[] asciiBytesToChars(byte[] buf, int offset, int len) {
                return JSONGeneral.EMPTY_ARRAY;
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
                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
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
                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                UNSAFE.putShort(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getShort(source, BYTE_ARRAY_OFFSET + sourceOff));
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
                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
                UNSAFE.putLong(target, targetOff + 16, UNSAFE.getLong(source, sourceOff + 16));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                UNSAFE.putShort(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getShort(source, BYTE_ARRAY_OFFSET + sourceOff));
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
                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
                UNSAFE.putLong(target, targetOff + 16, UNSAFE.getLong(source, sourceOff + 16));
                UNSAFE.putLong(target, targetOff + 24, UNSAFE.getLong(source, sourceOff + 24));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                UNSAFE.putInt(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getInt(source, BYTE_ARRAY_OFFSET + sourceOff));
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
                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
                UNSAFE.putLong(target, targetOff + 16, UNSAFE.getLong(source, sourceOff + 16));
                UNSAFE.putLong(target, targetOff + 24, UNSAFE.getLong(source, sourceOff + 24));
                UNSAFE.putLong(target, targetOff + 32, UNSAFE.getLong(source, sourceOff + 32));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                UNSAFE.putInt(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getInt(source, BYTE_ARRAY_OFFSET + sourceOff));
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
                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
                UNSAFE.putLong(target, targetOff + 16, UNSAFE.getLong(source, sourceOff + 16));
                UNSAFE.putLong(target, targetOff + 24, UNSAFE.getLong(source, sourceOff + 24));
                UNSAFE.putLong(target, targetOff + 32, UNSAFE.getLong(source, sourceOff + 32));
                UNSAFE.putLong(target, targetOff + 40, UNSAFE.getLong(source, sourceOff + 40));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                UNSAFE.putInt(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getInt(source, BYTE_ARRAY_OFFSET + sourceOff));
                UNSAFE.putShort(target, BYTE_ARRAY_OFFSET + targetOff + 4, UNSAFE.getShort(source, BYTE_ARRAY_OFFSET + sourceOff + 4));
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
                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
                UNSAFE.putLong(target, targetOff + 16, UNSAFE.getLong(source, sourceOff + 16));
                UNSAFE.putLong(target, targetOff + 24, UNSAFE.getLong(source, sourceOff + 24));
                UNSAFE.putLong(target, targetOff + 32, UNSAFE.getLong(source, sourceOff + 32));
                UNSAFE.putLong(target, targetOff + 40, UNSAFE.getLong(source, sourceOff + 40));
                UNSAFE.putLong(target, targetOff + 48, UNSAFE.getLong(source, sourceOff + 48));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                UNSAFE.putInt(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getInt(source, BYTE_ARRAY_OFFSET + sourceOff));
                UNSAFE.putShort(target, BYTE_ARRAY_OFFSET + targetOff + 4, UNSAFE.getShort(source, BYTE_ARRAY_OFFSET + sourceOff + 4));
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
                UNSAFE.putLong(target, targetOff, UNSAFE.getLong(source, sourceOff));
                UNSAFE.putLong(target, targetOff + 8, UNSAFE.getLong(source, sourceOff + 8));
                UNSAFE.putLong(target, targetOff + 16, UNSAFE.getLong(source, sourceOff + 16));
                UNSAFE.putLong(target, targetOff + 24, UNSAFE.getLong(source, sourceOff + 24));
                UNSAFE.putLong(target, targetOff + 32, UNSAFE.getLong(source, sourceOff + 32));
                UNSAFE.putLong(target, targetOff + 40, UNSAFE.getLong(source, sourceOff + 40));
                UNSAFE.putLong(target, targetOff + 48, UNSAFE.getLong(source, sourceOff + 48));
                UNSAFE.putLong(target, targetOff + 56, UNSAFE.getLong(source, sourceOff + 56));
            }

            void copyMemory(byte[] source, int sourceOff, byte[] target, int targetOff) {
                UNSAFE.putLong(target, BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getLong(source, BYTE_ARRAY_OFFSET + sourceOff));
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
                bytes[8] = buf[offset + 8];
                bytes[9] = buf[offset + 9];
//                putShort(bytes, 8, getShort(buf, offset + 8));
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
            public byte[] copyBytes(byte[] buf, int offset, int len) {
                byte[] bytes = new byte[18];
                putLong(bytes, 0, getLong(buf, offset));
                putLong(bytes, 8, getLong(buf, offset + 8));
//                putShort(bytes, 16, getShort(buf, offset + 16));
                bytes[16] = buf[offset + 16];
                bytes[17] = buf[offset + 17];
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
                char[] chars = new char[16];
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
        return UNSAFE.getLong(buf, BYTE_ARRAY_OFFSET + offset);
    }

    static long getLong(char[] buf, int offset) {
        return UNSAFE.getLong(buf, CHAR_ARRAY_OFFSET + (offset << 1));
    }

    static int getInt(byte[] buf, int offset) {
        return UNSAFE.getInt(buf, BYTE_ARRAY_OFFSET + offset);
    }

    static int getInt(char[] buf, int offset) {
        return UNSAFE.getInt(buf, CHAR_ARRAY_OFFSET + (offset << 1));
    }

    static short getShort(byte[] buf, int offset) {
        return UNSAFE.getShort(buf, BYTE_ARRAY_OFFSET + offset);
    }

    static int putInt(char[] buf, int offset, int value) {
        UNSAFE.putInt(buf, CHAR_ARRAY_OFFSET + (offset << 1), value);
        return 2;
    }

    static int putLong(char[] buf, int offset, long value) {
        UNSAFE.putLong(buf, CHAR_ARRAY_OFFSET + (offset << 1), value);
        return 4;
    }

    static int putLong(byte[] buf, int offset, long value) {
        UNSAFE.putLong(buf, BYTE_ARRAY_OFFSET + offset, value);
        return 8;
    }

    static int putInt(byte[] buf, int offset, int value) {
        UNSAFE.putInt(buf, BYTE_ARRAY_OFFSET + offset, value);
        return 4;
    }

    static int putShort(byte[] buf, int offset, short value) {
        UNSAFE.putShort(buf, BYTE_ARRAY_OFFSET + offset, value);
        return 2;
    }

    public static Object getStringValue(String value) {
        return UNSAFE.getObject(value, STRING_VALUE_OFFSET);
    }

    static String createStringJDK8(char[] buf, int beginIndex, int len) {
        String target = new String();
        UNSAFE.putObject(target, STRING_VALUE_OFFSET, copyChars(buf, beginIndex, len));
        return target;
    }

    static String createStringByAsciiBytesJDK8(byte[] buf, int beginIndex, int endIndex) {
        int len = endIndex - beginIndex;
        char[] result;
        if(len < SIZE_LEN) {
            result = SIZE_INSTANCES[len].asciiBytesToChars(buf, beginIndex, len);
        } else {
            result = new char[len];
            for (int j = 0; j < len; ++j) {
                result[j] = (char) buf[beginIndex + j];
            }
        }
        String target = new String();
        UNSAFE.putObject(target, STRING_VALUE_OFFSET, result);
        return target;
    }

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
            if (len < 72) {
                int rem = len & 7;
                int t = len >> 3;
                long sourceOff = BYTE_ARRAY_OFFSET + offset;
                SIZE_INSTANCES[t].multipleCopyMemory(buf, sourceOff, bytes, BYTE_ARRAY_OFFSET);
                if (rem > 0) {
                    int remOffset = t << 3;
                    putLong(bytes, remOffset - 8 + rem, getLong(buf, offset + remOffset - 8 + rem));
                }
                return bytes;
            } else {
                System.arraycopy(buf, offset, bytes, 0, len);
                return bytes;
            }
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
        try {
            byte[] asciiBytes = copyBytes(bytes, offset, len);
            String result = (String) UNSAFE.allocateInstance(String.class);
            UNSAFE.putObject(result, STRING_VALUE_OFFSET, asciiBytes);
            return result;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    static String createAsciiString(byte[] asciiBytes) {
        try {
            String result = (String) UNSAFE.allocateInstance(String.class);
            UNSAFE.putObject(result, STRING_VALUE_OFFSET, asciiBytes);
            return result;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    static byte[] getStringUTF8Bytes(String value) {
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) getStringValue(value.toString());
            if (bytes.length == value.length()) {
                return bytes;
            }
        }
        return value.getBytes(EnvUtils.CHARSET_UTF_8);
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
