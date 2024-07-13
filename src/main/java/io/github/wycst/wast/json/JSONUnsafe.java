package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.UnsafeHelper;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

// Provide an unsafe API that only supports internal use and can bypass security checks
final class JSONUnsafe {

    static final Unsafe UNSAFE;

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
    }

    static abstract class MemoryOptimizer {

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
    }

    final static MemoryOptimizer[] SIZE_INSTANCES = new MemoryOptimizer[]{
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
    final static MemoryOptimizer SIZE_INSTANCE_8 = SIZE_INSTANCES[8];

    static MemoryOptimizer s0() {
        return new MemoryOptimizer() {
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
        };
    }

    static MemoryOptimizer s1() {
        return new MemoryOptimizer() {
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
        };
    }

    static MemoryOptimizer s2() {
        return new MemoryOptimizer() {

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
                UNSAFE.putShort(target, UnsafeHelper.BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getShort(source, UnsafeHelper.BYTE_ARRAY_OFFSET + sourceOff));
            }
        };
    }

    static MemoryOptimizer s3() {
        return new MemoryOptimizer() {

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
                UNSAFE.putShort(target, UnsafeHelper.BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getShort(source, UnsafeHelper.BYTE_ARRAY_OFFSET + sourceOff));
                target[targetOff + 2] = source[sourceOff + 2];
            }
        };
    }

    static MemoryOptimizer s4() {
        return new MemoryOptimizer() {

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
                UNSAFE.putInt(target, UnsafeHelper.BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getInt(source, UnsafeHelper.BYTE_ARRAY_OFFSET + sourceOff));
            }
        };
    }

    static MemoryOptimizer s5() {
        return new MemoryOptimizer() {

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
                UNSAFE.putInt(target, UnsafeHelper.BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getInt(source, UnsafeHelper.BYTE_ARRAY_OFFSET + sourceOff));
                target[targetOff + 4] = source[sourceOff + 4];
            }
        };
    }

    static MemoryOptimizer s6() {
        return new MemoryOptimizer() {

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
                UNSAFE.putInt(target, UnsafeHelper.BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getInt(source, UnsafeHelper.BYTE_ARRAY_OFFSET + sourceOff));
                UNSAFE.putShort(target, UnsafeHelper.BYTE_ARRAY_OFFSET + targetOff + 4, UNSAFE.getShort(source, UnsafeHelper.BYTE_ARRAY_OFFSET + sourceOff + 4));
            }
        };
    }

    static MemoryOptimizer s7() {
        return new MemoryOptimizer() {
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
                UNSAFE.putInt(target, UnsafeHelper.BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getInt(source, UnsafeHelper.BYTE_ARRAY_OFFSET + sourceOff));
                UNSAFE.putShort(target, UnsafeHelper.BYTE_ARRAY_OFFSET + targetOff + 4, UNSAFE.getShort(source, UnsafeHelper.BYTE_ARRAY_OFFSET + sourceOff + 4));
                target[targetOff + 6] = source[sourceOff + 6];
            }
        };
    }

    static MemoryOptimizer s8() {
        return new MemoryOptimizer() {

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
                UNSAFE.putLong(target, UnsafeHelper.BYTE_ARRAY_OFFSET + targetOff, UNSAFE.getLong(source, UnsafeHelper.BYTE_ARRAY_OFFSET + sourceOff));
            }
        };
    }

    static MemoryOptimizer s9() {
        return new MemoryOptimizer() {
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
        };
    }

    static MemoryOptimizer s10() {
        return new MemoryOptimizer() {
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
        };
    }

    static MemoryOptimizer s11() {
        return new MemoryOptimizer() {
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
        };
    }

    static MemoryOptimizer s12() {
        return new MemoryOptimizer() {
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
        };
    }

    static MemoryOptimizer s13() {
        return new MemoryOptimizer() {
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
                putLong(bytes, 5, getLong(buf, offset + 5));
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
        };
    }

    static MemoryOptimizer s14() {
        return new MemoryOptimizer() {
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
        };
    }

    static MemoryOptimizer s15() {
        return new MemoryOptimizer() {
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
        };
    }

    static MemoryOptimizer s16() {
        return new MemoryOptimizer() {
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
        };
    }

    static MemoryOptimizer s17() {
        return new MemoryOptimizer() {
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
        };
    }

    static MemoryOptimizer s18() {
        return new MemoryOptimizer() {
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
        };
    }

    static MemoryOptimizer s19() {
        return new MemoryOptimizer() {
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
        };
    }

    static MemoryOptimizer s20() {
        return new MemoryOptimizer() {
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
        };
    }

    static long getLong(byte[] buf, int offset) {
        return UNSAFE.getLong(buf, UnsafeHelper.BYTE_ARRAY_OFFSET + offset);
    }

    static long getLong(char[] buf, int offset) {
        return UNSAFE.getLong(buf, UnsafeHelper.CHAR_ARRAY_OFFSET + (offset << 1));
    }

    static int getInt(byte[] buf, int offset) {
        return UNSAFE.getInt(buf, UnsafeHelper.BYTE_ARRAY_OFFSET + offset);
    }

    static int getInt(char[] buf, int offset) {
        return UNSAFE.getInt(buf, UnsafeHelper.CHAR_ARRAY_OFFSET + (offset << 1));
    }

    static short getShort(byte[] buf, int offset) {
        return UNSAFE.getShort(buf, UnsafeHelper.BYTE_ARRAY_OFFSET + offset);
    }

    static int putInt(char[] buf, int offset, int value) {
        UNSAFE.putInt(buf, UnsafeHelper.CHAR_ARRAY_OFFSET + (offset << 1), value);
        return 2;
    }

    static int putLong(char[] buf, int offset, long value) {
        UNSAFE.putLong(buf, UnsafeHelper.CHAR_ARRAY_OFFSET + (offset << 1), value);
        return 4;
    }

    static int putLong(byte[] buf, int offset, long value) {
        UNSAFE.putLong(buf, UnsafeHelper.BYTE_ARRAY_OFFSET + offset, value);
        return 8;
    }

    static int putInt(byte[] buf, int offset, int value) {
        UNSAFE.putInt(buf, UnsafeHelper.BYTE_ARRAY_OFFSET + offset, value);
        return 4;
    }

    static int putShort(byte[] buf, int offset, short value) {
        UNSAFE.putShort(buf, UnsafeHelper.BYTE_ARRAY_OFFSET + offset, value);
        return 2;
    }

    public static char[] copyChars(char[] buf, int offset, int len) {
        if (len < 16) {
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
            if(len < 72) {
                int rem = len & 7;
                int t = len >> 3;
                long sourceOff = UnsafeHelper.BYTE_ARRAY_OFFSET + offset;
                SIZE_INSTANCES[t].multipleCopyMemory(buf, sourceOff, bytes, UnsafeHelper.BYTE_ARRAY_OFFSET);
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
            UNSAFE.putObject(result, UnsafeHelper.STRING_VALUE_OFFSET, asciiBytes);
            return result;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
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

    public static void main(String[] args) {
        char[] chars = "hellohelulollqwewewe".toCharArray();
        char[] results = copyChars(chars, 0, chars.length);
        System.out.println(new String(chars));
        System.out.println(new String(results));
    }
}
