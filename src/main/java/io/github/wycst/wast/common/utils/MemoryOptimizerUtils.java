package io.github.wycst.wast.common.utils;

public final class MemoryOptimizerUtils {

    private MemoryOptimizerUtils() {
    }

    public static class LengthOptimizer {

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

        public char[] copy(char[] buf, int offset, int len) {
            char[] result = new char[len];
            System.arraycopy(buf, offset, result, 0, len);
            return result;
        }

        public byte[] copy(byte[] buf, int offset, int len) {
            byte[] result = new byte[len];
            System.arraycopy(buf, offset, result, 0, len);
            return result;
        }

        public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
            System.arraycopy(source, sOff, target, tOff, len);
        }

        public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
            System.arraycopy(source, sOff, target, tOff, len);
        }
    }

    final static LengthOptimizer[] SIZE_INSTANCES = new LengthOptimizer[]{
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

    @Deprecated
    public static void arraycopy(byte[] source, int sOff, byte[] target, int tOff, int len) {
        if (len < SIZE_LEN) {
            SIZE_INSTANCES[len].copyTo(source, sOff, target, tOff, len);
        } else {
            System.arraycopy(source, sOff, target, tOff, len);
        }
    }

    @Deprecated
    public static void arraycopy(char[] source, int sOff, char[] target, int tOff, int len) {
        if (len < SIZE_LEN) {
            SIZE_INSTANCES[len].copyTo(source, sOff, target, tOff, len);
        } else {
            System.arraycopy(source, sOff, target, tOff, len);
        }
    }

    /**
     * 拷贝字符片段
     *
     * @param buf
     * @param offset
     * @param len
     * @return
     */
    public static char[] copyOfRange(char[] buf, int offset, int len) {
        if (len < SIZE_LEN) {
            return SIZE_INSTANCES[len].copy(buf, offset, len);
        } else {
            char[] chars = new char[len];
            System.arraycopy(buf, offset, chars, 0, len);
            return chars;
        }
    }

    /**
     * 拷贝字节片段
     *
     * @param buf
     * @param offset
     * @param len
     * @return
     */
    public static byte[] copyOfRange(byte[] buf, int offset, int len) {
        if (len < SIZE_LEN) {
            return SIZE_INSTANCES[len].copy(buf, offset, len);
        } else {
            byte[] bytes = new byte[len];
            System.arraycopy(buf, offset, bytes, 0, len);
            return bytes;
        }
    }

    /**
     * 拷贝字符串数组
     *
     * @param buf
     * @param offset
     * @param len
     * @return
     */
    public static String[] copyOfRange(String[] buf, int offset, int len) {
        if (len < SIZE_LEN) {
            return SIZE_INSTANCES[len].copy(buf, offset, len);
        } else {
            String[] result = new String[len];
            System.arraycopy(buf, offset, result, 0, len);
            return result;
        }
    }

    /**
     * 拷贝double数组
     *
     * @param buf
     * @param offset
     * @param len
     * @return
     */
    public static double[] copyOfRange(double[] buf, int offset, int len) {
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
    public static long[] copyOfRange(long[] buf, int offset, int len) {
        if (len < SIZE_LEN) {
            return SIZE_INSTANCES[len].copy(buf, offset, len);
        } else {
            long[] result = new long[len];
            System.arraycopy(buf, offset, result, 0, len);
            return result;
        }
    }

    static LengthOptimizer s0() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{};
            }

            @Override
            public String[] copy(String[] buf, int offset, int len) {
                return new String[] {};
            }

            @Override
            public double[] copy(double[] buf, int offset, int len) {
                return new double[] {};
            }

            @Override
            public long[] copy(long[] buf, int offset, int len) {
                return new long[] {};
            }

            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
            }

            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
            }
        };
    }

    static LengthOptimizer s1() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
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

            @Override
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s2() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset]};
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

            @Override
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s3() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset]};
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

            @Override
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }


    static LengthOptimizer s4() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

            @Override
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s5() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

            @Override
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s6() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

            @Override
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s7() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

            @Override
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s8() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

            @Override
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s9() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s10() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s11() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s12() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s13() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s14() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s15() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s16() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s17() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s18() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s19() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }

    static LengthOptimizer s20() {
        return new LengthOptimizer() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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
            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }

            @Override
            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff++] = source[sOff++];
                target[tOff] = source[sOff];
            }
        };
    }
}
