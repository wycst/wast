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
     * 构建子串
     *
     * @param bytes
     * @param beginIndex
     * @param endIndex
     * @return
     */
    String substring(byte[] bytes, int beginIndex, int endIndex);
}
