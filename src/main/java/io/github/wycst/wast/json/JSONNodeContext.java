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

//    /**
//     * 是否懒加载（对路径的最后一级只进行校验扫描但不进行value解析）
//     */
//    public boolean lazy;

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
    public List extractValues = new ArrayList();

    private JSONKeyValueMap<String> KEY_32_TABLE;
    // cache keys
    static final JSONKeyValueMap<String> GLOBAL_KEY_8_TABLE = new JSONKeyValueMap<String>(2048);

    void extractValue(Object value) {
        extractValues.add(value);
    }

    public void reset() {
        super.clear();
    }

    public void clearCacheKeys() {
        GLOBAL_KEY_8_TABLE.reset();
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
