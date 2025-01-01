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
public final class JSONNodeContext extends JSONParseContext {

    JSONNodeContext() {
        allowLastEndComma = true;
        allowSingleQuotes = true;
        allowUnquotedFieldNames = true;
    }

    /***
     * 提取模式（提取路径的根对象）
     */
    public boolean extract;
    /**
     * 提取数据列表
     */
    public List extractValues;
    JSONNodeCollector collector;
    private JSONKeyValueMap<String> KEY_32_TABLE;
    static final JSONKeyValueMap<String> GLOBAL_KEY_8_TABLE = new JSONKeyValueMap<String>(2048);

    // Using an independent writer does not interfere with the impact on concurrent serialization
    @Override
    JSONCharArrayWriter getContextWriter() {
        if (writer == null) {
            writer = new JSONCharArrayWriter(256);
        } else {
            writer.clear();
        }
        return writer;
    }

    static String getString(char[] chars, int offset, int len) {
        if (len <= 8) {
            long h = 0;
            for (int i = 0; i < len; ++i) {
                char c = chars[offset + i];
                if (c > 0xFF) return new String(chars, offset, len);
                h = h << 8 | c;
            }
            String val = GLOBAL_KEY_8_TABLE.getValueByHash(h);
            if (val == null) {
                GLOBAL_KEY_8_TABLE.putExactHashValue(h, val = new String(chars, offset, len));
            }
            return val;
        }
        return new String(chars, offset, len);
    }

    void extractValue(JSONNode value) {
        if (collector.filter(value)) {
            extractValues.add(collector.map(value));
        }
    }

    void enableExtract(JSONNodeCollector nodeCollector) {
        collector = nodeCollector;
        extract = true;
        extractValues = new ArrayList();
    }

    public void reset() {
        super.clear();
    }

    public static void clearCacheKeys() {
        synchronized (GLOBAL_KEY_8_TABLE) {
            GLOBAL_KEY_8_TABLE.reset();
        }
    }

    @Override
    protected JSONKeyValueMap<String> getTable32() {
        if (KEY_32_TABLE == null) {
            KEY_32_TABLE = new JSONKeyValueMap<String>(128, new JSONKeyValueMap.EntryNode[128]);
        }
        return KEY_32_TABLE;
    }

    @Override
    protected JSONKeyValueMap<String> getTable8() {
        return GLOBAL_KEY_8_TABLE;
    }
}
