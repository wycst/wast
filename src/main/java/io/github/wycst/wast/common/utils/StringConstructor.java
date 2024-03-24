//package io.github.wycst.wast.common.utils;
//
//import io.github.wycst.wast.common.reflect.UnsafeHelper;
//
//class StringConstructor {
//
//    public String create(char[] buf, int offset, int len) {
//        return new String(buf, offset, len);
//    }
//
//    public String create(char[] buf) {
//        return UnsafeHelper.getString(buf);
//    }
//
//    public String createAscii(byte[] buf, int offset, int len) {
//        return UnsafeHelper.getAsciiString(MemoryCopyUtils.copyOfRange(buf, offset, len));
//    }
//
//    public String createUTF16(byte[] buf, int offset, int len) {
//        return UnsafeHelper.getUTF16String(MemoryCopyUtils.copyOfRange(buf, offset, len));
//    }
//
//    public String createAscii(byte[] bytes) {
//        return UnsafeHelper.getAsciiString(bytes);
//    }
//
//    public String createUTF16(byte[] bytes) {
//        return UnsafeHelper.getUTF16String(bytes);
//    }
//}
