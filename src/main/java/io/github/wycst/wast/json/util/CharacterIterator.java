package io.github.wycst.wast.json.util;

/**
 * 字符迭代器
 *
 * @Author: wangy
 * @Date: 2022/8/21 19:26
 * @Description:
 */
public interface CharacterIterator {

    /**
     * 返回下一个字符
     *
     * @return
     */
    public char next();

    /**
     * 当前位置字符
     *
     * @return
     */
    public char current();

    /**
     * 是否存在下一个字符
     *
     * @return
     */
    public boolean hasNext();

    /**
     * 当前位置
     *
     * @return
     */
    public int offset();

    /**
     * 获取指定位置的字符
     *
     * @return
     */
    public char charAt(int index);

    /**
     * 最大索引位置(不包含)
     *
     * @return
     */
    public int endIndex();

    /**
     * 构建字符串
     *
     * @param offset
     * @param len
     * @return
     */
    public String getString(int offset, int len);

}
