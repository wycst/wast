package org.framework.light.json.options;

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

    // 是否已预扫描
    public boolean prepared;

    // 根对象类型
    public int type;

    // 引号数量（prepared）
    public int quotTokenCount;
    // 双引号token记录（prepared）
    public int[] quotTokenIndexs;
    // 双引号游标位置（prepared）
    public int quotTokenOffset;

    // 是否存在需要转义的字符（prepared）
    public boolean escape;

    // 需要转义的字符位置与quotTokenIndexs一一对应，如果不存在为-1，如果存在记录实际的escapeCharIndex
    public int[] escapeCharIndexs;
    // 转义字符数量
    public int escapeCharCount;

    // 反向解析节点（解析path时生效）
    // 暂时未有时间实现
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

    public void addQuotToken(int i) {
        if (quotTokenIndexs == null) {
            quotTokenIndexs = new int[256];
        } else if (quotTokenCount == quotTokenIndexs.length) {
            int[] oldTokenIndexs = quotTokenIndexs;
            quotTokenIndexs = new int[oldTokenIndexs.length << 1];
            System.arraycopy(oldTokenIndexs, 0, quotTokenIndexs, 0, oldTokenIndexs.length);
        }
        quotTokenIndexs[this.quotTokenCount++] = i;
    }

    public void addEscapeIndex(int index) {
        if (escapeCharIndexs == null) {
            escapeCharIndexs = new int[128];
        } else {
            if (escapeCharCount == escapeCharIndexs.length) {
                // 扩容
                int[] oldEscapeCharIndexs = escapeCharIndexs;
                escapeCharIndexs = new int[oldEscapeCharIndexs.length << 1];
                System.arraycopy(oldEscapeCharIndexs, 0, escapeCharIndexs, 0, oldEscapeCharIndexs.length);
            }
        }
        escapeCharIndexs[escapeCharCount++] = index;
    }

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
        this.escape = false;
        this.quotTokenCount = 0;
        this.escapeCharCount = 0;
        this.quotTokenOffset = 0;
    }
}
