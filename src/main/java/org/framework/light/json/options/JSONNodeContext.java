package org.framework.light.json.options;

/**
 * JSON node 解析上下文
 *
 * @Author: wangy
 * @Date: 2021/11/11 0:39
 * @Description:
 */
public class JSONNodeContext extends JSONParseContext {

    // 是否已预扫描
    public boolean prepared = false;

    // 类型
    public int type;

    // 引号数量
    public int quotTokenCount;
    // 双引号token记录
    public int[] quotTokenIndexs;
    // 双引号游标位置
    public int quotTokenOffset;

    // 是否存在需要转义的字符
    public boolean escape;

    // 需要转义的字符位置与quotTokenIndexs一一对应，如果不存在为-1，如果存在记录实际的escapeCharIndex
    public int[] escapeCharIndexs;
    public int escapeCharCount;

    // 反向解析节点（解析path时生效）
    // 暂时未有时间实现
    public boolean reverseParseNode;

    /** 是否懒加载 */
    public boolean lazy;

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

    public void reset() {
        this.escape = false;
        this.quotTokenCount = 0;
        this.escapeCharCount = 0;
        this.quotTokenOffset = 0;
    }
}
