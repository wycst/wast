package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.KeyValuePair;

import java.io.Serializable;
import java.util.Map;

/**
 * JSON映射处理器抽象类，用于在JSON解析过程中处理键值对的转换和过滤
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>提供键值对处理的统一接口</li>
 *   <li>支持键的重映射功能</li>
 *   <li>支持null值过滤功能</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>JSON解析时动态修改键名</li>
 *   <li>过滤不需要的键值对（如null值）</li>
 *   <li>自定义键值对处理逻辑</li>
 * </ul>
 *
 * <p>示例用法：</p>
 * <pre>
 * // 创建键映射处理器
 * Map&lt;String, String&gt; mapping = new HashMap&lt;&gt;();
 * mapping.put("oldKey", "newKey");
 * JSONMapHandler handler = JSONMapHandler.create(mapping);
 *
 * // 创建带null值过滤的处理器
 * JSONMapHandler handlerWithFilter = JSONMapHandler.create(mapping, true);
 * </pre>
 *
 * @author wycst
 */
public abstract class JSONMapHandler {

    /**
     * 处理JSON映射中的键值对
     *
     * @param key   键，实现了Serializable接口
     * @param value 值，任意对象类型
     * @return KeyValuePair对象，包含处理后的键和值；当返回null时表示跳过此键值对
     */
    public abstract KeyValuePair<Serializable, Object> handle(Serializable key, Object value);

    /**
     * 创建键映射处理器
     *
     * @param mapping         键映射关系
     * @param ignoreNullValue 是否忽略null值
     * @return 键映射处理器
     */
    public static JSONMapHandler of(final Map<? extends Serializable, ? extends Serializable> mapping, final boolean ignoreNullValue) {
        return new JSONMapHandler() {
            @Override
            public KeyValuePair<Serializable, Object> handle(Serializable key, Object value) {
                // 当ignoreNullValue为true且value为null时，跳过该键值对
                if (ignoreNullValue && value == null) {
                    return null;
                }
                Serializable newKey = mapping.get(key);
                return KeyValuePair.of(newKey == null ? key : newKey, value);
            }
        };
    }

    /**
     * 创建键映射处理器
     *
     * @param mapping 键映射关系
     * @return 键映射处理器
     */
    public static JSONMapHandler of(final Map<? extends Serializable, ? extends Serializable> mapping) {
        return of(mapping, false);
    }
}
