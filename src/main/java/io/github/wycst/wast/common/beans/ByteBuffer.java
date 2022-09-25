package io.github.wycst.wast.common.beans;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * byte[]读写api
 *
 * @Author wangyunchao
 * @Date 2022/9/10 14:59
 */
public class ByteBuffer {

    protected byte[] buf;
    // 写索引；
    protected int writeIndex;
    // 读索引；
    protected int readIndex;

    public ByteBuffer() {
        buf = new byte[16];
    }

    ByteBuffer(byte[] buf) {
        this.buf = buf;
        this.writeIndex = buf.length;
    }

    /**
     * 构建ByteBuffer
     *
     * @param buf
     * @return
     */
    public static ByteBuffer of(byte[] buf) {
        return new ByteBuffer(buf);
    }

    /***
     * 将byte值写入buf数组（1字节）
     *
     * @param value
     */
    public void writeByte(byte value) {
        checkIfExpandCapacity(1);
        buf[writeIndex++] = value;
    }

    /***
     * 读取一个字节
     *
     */
    public byte readByte() {
        return buf[readIndex++];
    }

    /***
     * 读取一个字节,保持索引不动
     *
     */
    public byte readByteKeepIndex() {
        return buf[readIndex];
    }

    /***
     * 将bytes值写入buf数组
     *
     * @param values
     */
    public void writeBytes(byte... values) {
        int len = values.length;
        checkIfExpandCapacity(len);
        System.arraycopy(values, 0, buf, writeIndex, len);
        writeIndex += len;
    }

    /***
     * 读取字节到指定数组
     *
     * @param bytes
     */
    public void readBytes(byte[] bytes) {
        int len = bytes.length;
        System.arraycopy(buf, readIndex, bytes, 0, len);
        readIndex += len;
    }

    /***
     * 将boolean值写入buf数组（1字节）
     *
     * @param value
     */
    public void writeBoolean(boolean value) {
        checkIfExpandCapacity(1);
        buf[writeIndex++] = value ? (byte) 1 : (byte) 0;
    }

    /***
     * 读取下一个字节boolean
     *
     */
    public boolean readBoolean() {
        return buf[readIndex++] != 0;
    }

    /***
     * 读取下一个字节boolean
     *
     */
    public boolean readBooleanKeepIndex() {
        return buf[readIndex] != 0;
    }

    /***
     * 将char值写入buf数组（2字节默认大端模式）
     *
     * @param value
     */
    public void writeChar(char value) {
        checkIfExpandCapacity(2);
        buf[writeIndex++] = (byte) (value >> 8 & 0xff);
        buf[writeIndex++] = (byte) (value & 0xff);
    }

    /***
     * 读取2个字节返回char（2字节默认大端模式）
     *
     */
    public char readChar() {
        byte b1 = buf[readIndex++];
        byte b2 = buf[readIndex++];
        int value = (b1 & 0xFF) << 8;
        value |= b2 & 0xFF;
        return (char) value;
    }

    /***
     * 将char值写入buf数组（小端模式）
     *
     * @param value
     */
    public void writeLEChar(char value) {
        checkIfExpandCapacity(2);
        buf[writeIndex++] = (byte) (value & 0xff);
        buf[writeIndex++] = (byte) (value >> 8 & 0xff);
    }

    /***
     * 读取2个字节返回char（2字节小端模式）
     *
     */
    public char readLEChar() {
        byte b1 = buf[readIndex++];
        byte b2 = buf[readIndex++];
        int value = (b2 & 0xFF) << 8;
        value |= b1 & 0xFF;
        return (char) value;
    }


    /***
     * 将short值写入buf数组（2字节默认大端模式）
     *
     * @param value
     */
    public void writeShort(short value) {
        checkIfExpandCapacity(2);
        buf[writeIndex++] = (byte) (value >> 8 & 0xff);
        buf[writeIndex++] = (byte) (value & 0xff);
    }

    /***
     * 读取2个字节返回short（2字节默认大端模式）
     *
     */
    public short readShort() {
        return (short) readChar();
    }

    /***
     * 将short值写入buf数组（小端模式）
     *
     * @param value
     */
    public void writeLEShort(short value) {
        checkIfExpandCapacity(2);
        buf[writeIndex++] = (byte) (value & 0xff);
        buf[writeIndex++] = (byte) (value >> 8 & 0xff);
    }

    /***
     * 读取2个字节返回short（小端模式）
     *
     */
    public short readLEShort() {
        return (short) readLEChar();
    }

    /***
     * 将int值写入buf数组（默认大端模式）
     *
     * @param value
     */
    public void writeInt(int value) {
        checkIfExpandCapacity(4);
        buf[writeIndex++] = (byte) (value >> 24 & 0xff);
        buf[writeIndex++] = (byte) (value >> 16 & 0xff);
        buf[writeIndex++] = (byte) (value >> 8 & 0xff);
        buf[writeIndex++] = (byte) (value & 0xff);
    }

    /***
     * 读取4个字节返回int（4字节默认大端模式）
     *
     */
    public int readInt() {
        int value = 0;
        value |= (buf[readIndex++] & 0xFF) << 24;
        value |= (buf[readIndex++] & 0xFF) << 16;
        value |= (buf[readIndex++] & 0xFF) << 8;
        value |= buf[readIndex++] & 0xFF;
        return value;
    }

    /***
     * 读取4个字节返回int（4字节默认大端模式）
     *
     */
    public int readIntKeepIndex() {
        int value = readInt();
        readIndex -= 4;
        return value;
    }

    /***
     * 将int值写入buf数组(小端模式)
     *
     * @param value
     */
    public void writeLEInt(int value) {
        checkIfExpandCapacity(4);
        buf[writeIndex++] = (byte) (value & 0xff);
        buf[writeIndex++] = (byte) (value >> 8 & 0xff);
        buf[writeIndex++] = (byte) (value >> 16 & 0xff);
        buf[writeIndex++] = (byte) (value >> 24 & 0xff);
    }

    /***
     * 读取4个字节返回int（4字节小端模式）
     *
     */
    public int readLEInt() {
        int value = 0;
        value |= buf[readIndex++] & 0xFF;
        value |= (buf[readIndex++] & 0xFF) << 8;
        value |= (buf[readIndex++] & 0xFF) << 16;
        value |= (buf[readIndex++] & 0xFF) << 24;
        return value;
    }

    /***
     * 读取4个字节返回int（4字节小端模式）
     *
     */
    public int readLEIntKeepIndex() {
        int value = readLEInt();
        readIndex -= 4;
        return value;
    }

    /***
     * 将float值写入buf数组（默认大端模式）
     *
     * @param value
     */
    public void writeFloat(float value) {
        int fv = Float.floatToIntBits(value);
        writeInt(fv);
    }

    /***
     * 将float值写入buf数组(小端模式)
     *
     * @param value
     */
    public void writeLEFloat(float value) {
        int fv = Float.floatToIntBits(value);
        writeLEInt(fv);
    }

    /***
     * 将long值写入buf数组（默认大端模式）
     *
     * @param value
     */
    public void writeLong(long value) {
        checkIfExpandCapacity(8);
        buf[writeIndex++] = (byte) (value >> 56 & 0xff);
        buf[writeIndex++] = (byte) (value >> 48 & 0xff);
        buf[writeIndex++] = (byte) (value >> 40 & 0xff);
        buf[writeIndex++] = (byte) (value >> 32 & 0xff);
        buf[writeIndex++] = (byte) (value >> 24 & 0xff);
        buf[writeIndex++] = (byte) (value >> 16 & 0xff);
        buf[writeIndex++] = (byte) (value >> 8 & 0xff);
        buf[writeIndex++] = (byte) (value & 0xff);
    }

    /***
     * 将long值写入buf数组(小端模式)
     *
     * @param value
     */
    public void writeLELong(long value) {
        checkIfExpandCapacity(8);
        buf[writeIndex++] = (byte) (value & 0xff);
        buf[writeIndex++] = (byte) (value >> 8 & 0xff);
        buf[writeIndex++] = (byte) (value >> 16 & 0xff);
        buf[writeIndex++] = (byte) (value >> 24 & 0xff);
        buf[writeIndex++] = (byte) (value >> 32 & 0xff);
        buf[writeIndex++] = (byte) (value >> 40 & 0xff);
        buf[writeIndex++] = (byte) (value >> 48 & 0xff);
        buf[writeIndex++] = (byte) (value >> 56 & 0xff);
    }

    /***
     * 将double值写入buf数组（默认大端模式）
     *
     * @param value
     */
    public void writeDouble(double value) {
        long dv = Double.doubleToLongBits(value);
        writeLong(dv);
    }

    /***
     * 将double值写入buf数组(小端模式)
     *
     * @param value
     */
    public void writeLEDouble(double value) {
        long dv = Double.doubleToLongBits(value);
        writeLELong(dv);
    }

    /**
     * 字符串写入数组返回字符串的字节长度
     *
     * @param value 字符串内容
     */
    public void writeString(String value) {
        byte[] bytes = value.getBytes(Charset.forName("UTF-8"));
        int len = bytes.length;
        checkIfExpandCapacity(len);
        System.arraycopy(bytes, 0, buf, writeIndex, len);
        writeIndex += len;
    }

    /**
     * 读取字符串（utf-8编码）
     *
     * @param length 字符串字节长度
     */
    public String readString(int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(buf, readIndex, bytes, 0, length);
        return new String(bytes, Charset.forName("UTF-8"));
    }

    // 检查
    private void checkIfExpandCapacity(int len) {
        int newWriteIndex = writeIndex + len;
        if (newWriteIndex > buf.length) {
            expandCapacity(Math.max(buf.length << 1, buf.length + newWriteIndex));
        }
    }

    // 扩容
    void expandCapacity(int capacity) {
        buf = Arrays.copyOf(buf, capacity);
    }

    /**
     * 构建bytes
     *
     * @return
     */
    public byte[] toBytes() {
        return Arrays.copyOf(buf, writeIndex);
    }

    public void setWriteIndex(int writeIndex) {
        this.writeIndex = writeIndex;
    }

    public void setReadIndex(int readIndex) {
        this.readIndex = readIndex;
    }
}
