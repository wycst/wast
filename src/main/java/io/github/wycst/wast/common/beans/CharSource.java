package io.github.wycst.wast.common.beans;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;

/**
 * 字符源（只作为一个字符读取外壳代替原来的char[]，未实现iterator）
 *
 * @Author: wangy
 * @Date: 2022/8/21 19:26
 * @Description:
 */
public interface CharSource extends CharSequence {

    /**
     * 获取字符源
     *
     * @return
     */
    char[] getSource();

    /**
     * 开始位置
     *
     * @return
     */
    public int fromIndex();

    /**
     * 最大索引位置(不包含)
     *
     * @return
     */
    public int toIndex();

    /**
     * 获取指定位置的字符
     *
     * @return
     */
    public char charAt(int index);

    /**
     * 构建字符串
     *
     * @param offset
     * @param len
     * @return
     */
    public String getString(int offset, int len);

    /**
     * 按位置写入内容到Writer
     *
     * @param writer
     * @param offset
     * @param len
     * @throws IOException
     */
    public void writeTo(Writer writer, int offset, int len) throws IOException;

    /**
     * 按位置append内容到StringBuffer
     *
     * @param stringBuffer
     * @param offset
     * @param len
     */
    void appendTo(StringBuffer stringBuffer, int offset, int len);

    /**
     * 按位置append内容到StringBuffer
     *
     * @param stringBuilder
     * @param offset
     * @param len
     */
    void appendTo(StringBuilder stringBuilder, int offset, int len);

    /**
     * 开始字符
     *
     * @return
     */
    char begin();

    /**
     * 长度
     *
     * @return
     */
    int length();

    /**
     * 拷贝字符
     *
     * @param srcOff
     * @param target
     * @param tarOff
     * @param len
     */
    void copy(int srcOff, char[] target, int tarOff, int len);

    /**
     * 构建bigDecimal对象
     *
     * @param fromIndex
     * @param len
     * @return
     */
    BigDecimal ofBigDecimal(int fromIndex, int len);
}
