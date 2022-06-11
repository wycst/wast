package io.github.wycst.wast.common.template;

import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.common.utils.StringUtils;

import java.util.Map;

public abstract class TemplateClass {
    private final StringBuffer buffer;

    public TemplateClass() {
        buffer = new StringBuffer();
    }

    /***
     * 输出普通文本
     * @param text
     */
    final protected TemplateClass print(Object text) {
        buffer.append(text);
        return this;
    }

    /***
     * 输出换行符
     */
    final protected TemplateClass println() {
        buffer.append("\r\n");
        return this;
    }

    /***
     * 输出普通文本并换行
     * @param text
     */
    final protected TemplateClass println(Object text) {
        buffer.append(text).append("\r\n");
        return this;
    }

    /**
     * 占位符替换
     *
     * @param text
     * @param context
     */
    final protected TemplateClass println(String text, Map<String, Object> context) {
        return println(StringUtils.replaceGroupRegex(text, context, true));
    }

    final protected Iterable<Object> getContextIterable(Map<String, Object> context, String key) {
        return ObjectUtils.getIterable(context, key);
    }

    /**
     * 获取上下文内容
     *
     * @param target
     * @param key
     * @return
     */
    final protected Object getContextValue(Object target, String key) {
        return ObjectUtils.get(target, key);
    }

    /**
     * 获取上下文内容（提供缺省取值）
     *
     * @param target
     * @param key
     * @param defaultValue 默认值
     * @return
     */
    final protected Object getContextValue(Object target, String key, Object defaultValue) {
        Object value = ObjectUtils.get(target, key);
        if(value == null) {
            return defaultValue;
        }
        return value;
    }


    protected synchronized String render(Map<String, Object> context) {
        buffer.setLength(0);
        renderTemplate(context);
        return getTemplate();
    }

    protected abstract void renderTemplate(Map<String, Object> context);

    private String getTemplate() {
        return buffer.toString();
    }
}