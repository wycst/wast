//package io.github.wycst.wast.common.beans;
//
//import java.io.IOException;
//import java.io.Writer;
//import java.math.BigDecimal;
//
///**
// * <p> 针对jdk9+字符串内置bytes数组coder=UTF16(1)时的字符转换读取
// * <p> 2字节一个字符,字符总长度为数组一半
// * <p> jdk9以下不要使用
// *
// * @Author: wangy
// * @Date: 2022/8/27 20:07
// * @Description:
// */
//class UTF16ByteArraySource extends AbstractCharSource implements CharSource {
//
//    private final byte[] bytes;
//    private final char[] source;
//
//    UTF16ByteArraySource(char[] source, byte[] bytes, int from, int to) {
//        super(from, to);
//        this.bytes = bytes;
//        this.source = source;
//    }
//
//    /**
//     * 构建对象
//     *
//     * @param utf16
//     * @param bytes
//     * @return
//     */
//    public static UTF16ByteArraySource ofTrim(String utf16, byte[] bytes) {
//        // if use cache by threadlocal for allocate char[] memory?
//        char[] source = utf16.toCharArray();
//        int fromIndex = 0;
//        int toIndex = bytes.length >> 1;
//        while ((fromIndex < toIndex) && source[fromIndex] <= ' ') {
//            fromIndex++;
//        }
//        while ((toIndex > fromIndex) && source[toIndex - 1] <= ' ') {
//            toIndex--;
//        }
//        return new UTF16ByteArraySource(source, bytes, fromIndex, toIndex);
//    }
//
//    @Override
//    public char[] getSource() {
//        return source;
//    }
//
//    @Override
//    public char charAt(int index) {
//        return source[index];
//    }
//
//    /**
//     * 构建字符串，需要计算新字符串的coder值
//     *
//     * @param offset 字符索引位置，计算时需要移动一位
//     * @param len    长度
//     * @return
//     */
//    @Override
//    public String getString(int offset, int len) {
//        // 如果范围内的coder是UTF16，则使用Arrays.copy性能最高，否则使用迭代器设置每个位置的字节
//        // UTF16 use copy force UTF16 even if LATIN1 (skip compress)
////        int last = offset + len;
////        byte[] utf16Bytes = Arrays.copyOfRange(bytes, offset << 1, last << 1);
////        return UnsafeHelper.getUTF16String(utf16Bytes);
//        return new String(source, offset, len);
//    }
//
//    @Override
//    public void writeTo(Writer writer, int offset, int len) throws IOException {
//        writer.write(source, offset, len);
//    }
//
//    @Override
//    public void appendTo(StringBuffer stringBuffer, int offset, int len) {
//        stringBuffer.append(source, offset, len);
//    }
//
//    @Override
//    public void appendTo(StringBuilder stringBuilder, int offset, int len) {
//        stringBuilder.append(source, offset, len);
//    }
//
//    @Override
//    public void copy(int srcOff, char[] target, int tarOff, int len) {
//        System.arraycopy(source, srcOff, target, tarOff, len);
//    }
//
//    @Override
//    public BigDecimal ofBigDecimal(int fromIndex, int len) {
//        return new BigDecimal(source, fromIndex, len);
//    }
//    //    /**
////     * 获取UTF16编码直接数组在指定字符位置下的字符
////     *
////     * @param source
////     * @param index
////     * @return
////     */
////    private static char charAt(byte[] source, int index) {
////        index <<= 1;
////        return (char) ((source[index++] & 255) << HI_BYTE_SHIFT | (source[index] & 255) << LO_BYTE_SHIFT);
////    }
//
////    static final boolean LE;
////    static final int HI_BYTE_SHIFT;
////    static final int LO_BYTE_SHIFT;
////
////    static {
////        // JDK9+ 可以直接使用地址判断
////        LE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
////        if (LE) {
////            HI_BYTE_SHIFT = 0;
////            LO_BYTE_SHIFT = 8;
////        } else {
////            HI_BYTE_SHIFT = 8;
////            LO_BYTE_SHIFT = 0;
////        }
////    }
//}
