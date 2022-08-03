//package io.github.wycst.wast.json;
//
///**
// * 字节读取器(使用utf-8解码)
// *
// * @Author wangyunchao
// * @Date 2022/6/15 14:05
// */
//public final class JSONByteArrayReader extends JSONReader {
//
//    /**
//     * 缓冲字节数组
//     */
//    private byte[] bytes;
//
//    /**
//     * 当前字节位置
//     */
//    private int index;
//
//    /***
//     * 字节数
//     */
//    private int len;
//
//    /**
//     * 下一个字符
//     */
//    private int next;
//
//    /**
//     * 是否正在读
//     */
//    private boolean reading;
//
//    // 完成标记
//    private boolean completed;
//
//    /**
//     * 通过字节数组构建
//     *
//     * @param bytes
//     */
//    public JSONByteArrayReader(byte[] bytes) {
//        super();
//        this.bytes = bytes;
//        this.index = 0;
//        this.len = bytes.length;
//    }
//
//    JSONByteArrayReader() {
//    }
//
//    /**
//     * 重写实现 readString
//     *
//     * @return
//     * @throws Exception
//     */
//    protected String readString() throws Exception {
//        this.endReading();
//        boolean escapeFlag = false;
//        while (readNext() > -1) {
//            if (escapeFlag) {
//                switch (current) {
//                    case '"':
//                        bufferWriter.append('\"');
//                        break;
//                    case 'n':
//                        bufferWriter.append('\n');
//                        break;
//                    case 'r':
//                        bufferWriter.append('\r');
//                        break;
//                    case 't':
//                        bufferWriter.append('\t');
//                        break;
//                    case 'b':
//                        bufferWriter.append('\b');
//                        break;
//                    case 'f':
//                        bufferWriter.append('\f');
//                        break;
//                    case 'u':
//                        int c1 = readNext(true);
//                        int c2 = readNext(true);
//                        int c3 = readNext(true);
//                        int c4 = readNext(true);
//                        int c = valueHex4(c1, c2, c3, c4);
//                        bufferWriter.append((char) c);
//                        break;
//                    case '\\':
//                        bufferWriter.append('\\');
//                        break;
//                    default: {
//                        bufferWriter.append((char) current);
//                        break;
//                    }
//                }
//                // read next
//                readNext(true);
//            }
//            if (current == '"') {
//                return bufferWriter.toString();
//            }
//            escapeFlag = current == '\\';
//            if (!escapeFlag) {
//                bufferWriter.append((char) current);
//            }
//        }
//        // maybe throw an exception
//        throwUnexpectedException();
//        return null;
//    }
//
//
//    /**
//     * 读取下一个字符
//     *
//     * @return
//     * @throws Exception
//     */
//    protected int readNext() throws Exception {
//        current = nextCharOfUTF8();
//        if (reading) {
//            this.appendCurrent();
//        }
//        return current;
//    }
//
//    private int nextCharOfUTF8() {
//        pos++;
//        offset++;
//        if (next > -1) {
//            int temp = next;
//            next = -1;
//            return temp;
//        }
//        if (hasNext()) {
//            try {
//                byte b = nextByte();
//                if (b > 0) {
//                    return b;
//                }
//                int s = b >> 4;
//                switch (s) {
//                    case -1: {
//                        // 1111 4个字节
//                        // 第1个字节的后4位 + 第2个字节的后6位 + 第3个字节的后6位 + 第4个字节的后6位
//                        byte b1 = nextByte();
//                        byte b2 = nextByte();
//                        byte b3 = nextByte();
//                        int a = ((b & 0x7) << 18) | ((b1 & 0x3f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f);
//                        if (Character.isSupplementaryCodePoint(a)) {
//                            next = (char) ((a & 0x3ff) + Character.MIN_LOW_SURROGATE);
//                            return (char) ((a >>> 10)
//                                    + (Character.MIN_HIGH_SURROGATE - (Character.MIN_SUPPLEMENTARY_CODE_POINT >>> 10)));
//                        } else {
//                            return a;
//                        }
//                    }
//                    case -2: {
//                        // 1110 3个字节
//                        // 第1个字节的后4位 + 第2个字节的后6位 + 第3个字节的后6位
//                        byte b1 = nextByte();
//                        byte b2 = nextByte();
//                        return ((b & 0xf) << 12) | ((b1 & 0x3f) << 6) | (b2 & 0x3f);
//                    }
//                    case -3:
//                        // 1101 和 1100都按2字节处理
//                    case -4: {
//                        // 1100 2个字节
//                        byte b1 = nextByte();
//                        return ((b & 0x1f) << 6) | (b1 & 0x3f);
//                    }
//                    default:
//                        throw new UnsupportedOperationException("utf-8 character error ");
//                }
//
//            } catch (Throwable throwable) {
//                throwable.printStackTrace();
//                throw new RuntimeException("UTF-8 reader bytes error");
//            }
//        } else {
//            return -1;
//        }
//    }
//
//    protected boolean hasNext() {
//        return index < len;
//    }
//
//    protected byte nextByte() {
//        return bytes[index++];
//    }
//
//    private void appendCurrent() {
//        bufferWriter.append((char) current);
//    }
//
//    protected void beginCurrent() {
//        this.beginReading(0);
//        appendCurrent();
//    }
//
//    protected String endReadingAsString(int n) {
//        this.reading = false;
//        bufferWriter.setLength(bufferWriter.length() + n);
//        return bufferWriter.toString();
//    }
//
//    protected void endReading() {
//        this.reading = false;
//        bufferWriter.setLength(0);
//    }
//
//    protected void endReading(int n, int newOffset) {
//        this.reading = false;
//        bufferWriter.setLength(bufferWriter.length() + n);
//    }
//
//    protected void beginReading(int n) {
//        this.reading = true;
//        bufferWriter.setLength(0);
//    }
//
//    protected boolean isCompleted() {
//        return completed;
//    }
//}
