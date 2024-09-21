package io.github.wycst.wast.json;

/**
 * 不同JDK环境下字符串结构抽象处理
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
}
