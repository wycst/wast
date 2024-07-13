package io.github.wycst.wast.json;

/**
 * 字符源（只作为一个字符读取外壳代替原来的char[]，未实现iterator）
 *
 * @Author: wangy
 * @Date: 2022/8/21 19:26
 * @Description:
 */
public interface CharSource {

    /**
     * 返回输入字符串
     *
     * @return
     */
    String input();

    /**
     * 获取字节源
     *
     * @return
     */
    byte[] byteArray();

    /**
     * 查找字符索引
     *
     * @param ch
     * @param beginIndex
     * @return
     */
    int indexOf(int ch, int beginIndex);

    /**
     * 构建子串
     *
     * @param beginIndex
     * @param endIndex
     * @return
     */
    String substring(int beginIndex, int endIndex);

//    /**
//     * 开始位置
//     *
//     * @return
//     */
//    public int fromIndex();
//
//    /**
//     * 最大索引位置(不包含)
//     *
//     * @return
//     */
//    public int toIndex();
//
//    /**
//     * 获取指定位置的字符
//     *
//     * @return
//     */
//    public char charAt(int index);
//
//    /**
//     * 构建字符串
//     *
//     * @param offset
//     * @param len
//     * @return
//     */
//    public String getString(int offset, int len);
//
//    /**
//     * 按位置写入内容到Writer
//     *
//     * @param writer
//     * @param offset
//     * @param len
//     * @throws IOException
//     */
//    public void writeTo(Writer writer, int offset, int len) throws IOException;
//
//    /**
//     * 开始字符
//     *
//     * @return
//     */
//    char begin();
//
//    /**
//     * 长度
//     *
//     * @return
//     */
//    int length();
//
//    /**
//     * 拷贝字符
//     *
//     * @param srcOff
//     * @param target
//     * @param tarOff
//     * @param len
//     */
//    void copy(int srcOff, char[] target, int tarOff, int len);
//

//
//    /**
//     * 截取字符串内容
//     *
//     * @param beginIndex
//     * @param endIndex
//     * @return
//     */
//    String substring(int beginIndex, int endIndex);
//
//
//    /**
//     * 修改索引位置的字符
//     *
//     * @param endIndex
//     * @param c
//     */
//    void setCharAt(int endIndex, char c);
}
