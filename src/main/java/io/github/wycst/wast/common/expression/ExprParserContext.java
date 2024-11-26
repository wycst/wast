package io.github.wycst.wast.common.expression;

/**
 * @Author wangyunchao
 * @Date 2022/10/15 14:44
 */
final class ExprParserContext {

    // 当前执行器
    ExprEvaluator exprEvaluator;
    // 负
    boolean negate;
    // 非
    boolean logicalNot;

    // 括号结束标志
    boolean bracketMode;
    boolean bracketEndFlag;
    // ?结束
    boolean questionMode;
    boolean questionEndFlag;

    ExprParserContext() {
    }

    ExprParserContext(ExprEvaluator evaluator, boolean negate, boolean logicalNot) {
        setContext(evaluator, negate, logicalNot);
    }

    void setContext(ExprEvaluator evaluator, boolean negate, boolean logicalNot) {
        this.exprEvaluator = evaluator;
        this.negate = negate;
        this.logicalNot = logicalNot;
    }

    final static int MASK = 511;
    final static EntryNode<String>[] VAR_INDEXED = new EntryNode[512];
    static int count;
    static boolean disabledIndexed;

    static class EntryNode<T> {
        long key;
        T value;
        EntryNode<T> next;

        public EntryNode(long key, T value) {
            this.key = key;
            this.value = value;
        }
    }

    // all are visible characters
    static String getString(char[] buf, int offset, int len) {
        if (len > 8 || disabledIndexed) {
            return new String(buf, offset, len);
        }
        long key = 0;
        char ch;
        int i = offset, end = offset + len;
        for (; i < end; ++i) {
            ch = buf[i];
            if (ch > 0xff) {
                return new String(buf, offset, len);
            }
            key = key << 8 | ch;
        }
        return getKey(buf, offset, len, key);
    }

    static String getKey(char[] buf, int offset, int len, long hash) {
        String value = getIndexedKey(hash); // KEYS.get(hash);
        if (value != null) {
            return value;
        }
        synchronized (VAR_INDEXED) {
            value = getIndexedKey(hash);
            if (value != null) {
                return value;
            }
            value = new String(buf, offset, len);
            setIndexedKey(hash, value);
            return value;
        }
    }

    static void setIndexedKey(long hash, String value) {
        int index = (int) (hash & MASK);
        EntryNode<String> newNode = new EntryNode<String>(hash, value);
        EntryNode<String> entryNode = VAR_INDEXED[index];
        newNode.next = entryNode;
        VAR_INDEXED[index] = newNode;
        disabledIndexed = ++count > MASK;
    }

    static String getIndexedKey(long hash) {
        int index = (int) (hash & MASK);
        EntryNode<String> entryNode = VAR_INDEXED[index];
        while (entryNode != null) {
            if (entryNode.key == hash) {
                return entryNode.value;
            }
            entryNode = entryNode.next;
        }
        return null;
    }
}
