package io.github.wycst.wast.common.utils;

public class MemoryCopyUtils {

    private MemoryCopyUtils() {
    }

    final static MemoryCopyUtils[] SIZE_INSTANCES = new MemoryCopyUtils[]{
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
    };

    final static int SIZE_LEN = SIZE_INSTANCES.length;

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

    public static char[] copyOfRange(char[] buf, int offset, int len) {
        if (len < SIZE_LEN) {
            return SIZE_INSTANCES[len].copy(buf, offset, len);
        } else {
            char[] chars = new char[len];
            System.arraycopy(buf, offset, chars, 0, len);
            return chars;
        }
    }

    public static byte[] copyOfRange(byte[] buf, int offset, int len) {
        if (len < SIZE_LEN) {
            return SIZE_INSTANCES[len].copy(buf, offset, len);
        } else {
            byte[] bytes = new byte[len];
            System.arraycopy(buf, offset, bytes, 0, len);
            return bytes;
        }
    }

    static MemoryCopyUtils s0() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{};
            }

            public void copyTo(byte[] source, int sOff, byte[] target, int tOff, int len) {
            }

            public void copyTo(char[] source, int sOff, char[] target, int tOff, int len) {
            }
        };
    }

    static MemoryCopyUtils s1() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset]};
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

    static MemoryCopyUtils s2() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s3() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset]};
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


    static MemoryCopyUtils s4() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    public static MemoryCopyUtils s5() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s6() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s7() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s8() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s9() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s10() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s11() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s12() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s13() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s14() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s15() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s16() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s17() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s18() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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

    static MemoryCopyUtils s19() {
        return new MemoryCopyUtils() {
            @Override
            public char[] copy(char[] buf, int offset, int len) {
                return new char[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
            }

            @Override
            public byte[] copy(byte[] buf, int offset, int len) {
                return new byte[]{buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset++], buf[offset]};
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
}
