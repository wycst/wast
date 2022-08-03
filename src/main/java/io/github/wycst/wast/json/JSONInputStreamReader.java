//package io.github.wycst.wast.json;
//
//import io.github.wycst.wast.json.exceptions.JSONException;
//
//import java.io.*;
//
///**
// * 字节流读取器
// *
// * @Author wangyunchao
// * @Date 2022/7/15 16:03
// */
//public class JSONInputStreamReader extends JSONByteArrayReader {
//
//    private final InputStream is;
//    private final byte[] buf;
//    private int offset;
//    private int count;
//
//    /**
//     * 通过io流(字节流)构建
//     *
//     * @param inputStream
//     */
//    public JSONInputStreamReader(InputStream inputStream) {
//        inputStream.getClass();
//        this.is = inputStream;
//        buf = new byte[bufferSize];
//        readBuf();
//    }
//
//    /**
//     * 通过file构建
//     *
//     * @param file
//     */
//    public JSONInputStreamReader(File file) {
//        this(ofFile(file));
//    }
//
//    /**
//     * 通过file构建
//     *
//     * @param file
//     */
//    public static JSONReader from(File file) {
//        return new JSONInputStreamReader(file);
//    }
//
//    private static InputStream ofFile(File file) {
//        try {
//            return new FileInputStream(file);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    protected boolean hasNext() {
//        if (offset < count) return true;
//        if (count < bufferSize) {
//            return false;
//        }
//        readBuf();
//        return count > 0;
//    }
//
//    private void readBuf() {
//        try {
//            count = is.read(buf);
//            offset = 0;
//        } catch (IOException e) {
//            throw new JSONException(e.getMessage(), e);
//        }
//    }
//
//    protected byte nextByte() {
//        if (hasNext()) {
//            return buf[offset++];
//        }
//        throw new IndexOutOfBoundsException(String.valueOf(offset));
//    }
//
//    public void close() {
//        try {
//            is.close();
//        } catch (IOException e) {
//        }
//    }
//}
