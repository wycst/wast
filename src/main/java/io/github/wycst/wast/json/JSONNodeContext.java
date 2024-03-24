package io.github.wycst.wast.json;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON node 解析上下文
 *
 * @Author: wangy
 * @Date: 2021/11/11 0:39
 * @Description:
 */
public class JSONNodeContext extends JSONParseContext {

    /**
     * 反向解析节点（解析path时生效）
     * todo 暂未实现
     */
    public boolean reverseParseNode;

    /**
     * 是否懒加载（对路径的最后一级只进行校验扫描但不进行value解析）
     */
    public boolean lazy;

    /**
     * 开启校验模式（调用validate方法时）
     */
    public boolean validate;

    /***
     * 提取模式（提取路径的根对象）
     */
    public boolean extract;

    /**
     * 提取数据列表
     */
    public List extractValues;

    public void extractValue(Object value) {
        if (extractValues == null) {
            extractValues = new ArrayList();
        }
        extractValues.add(value);
    }

    public List getExtractValues() {
        return extractValues;
    }

    public void reset() {
        super.clear();
    }
}
