package org.framework.light.json.options;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * json解析上下文配置
 *
 * @Author: wangy
 * @Date: 2021/12/19 14:47
 * @Description:
 */
public class JSONParseContext {

    /**
     * 原子number对象，上下文使用，只创建一次
     * (Atomic number object, context use)
     */
    private final AtomicInteger endIndex = new AtomicInteger();

    /**
     * 字符串构建
     * （Character builder）
     */
    private final StringBuilder stringBuilder = new StringBuilder();

    /**
     * 禁用转义模式，已确定没有转义符场景下使用，能适当加速字符串解析
     * （The escape mode is disabled. It has been determined that it can be used in scenarios without escape characters, which can appropriately speed up string parsing）
     */
    private boolean disableEscapeMode;

    /**
     * 从16进制字符串中转化为字节数组
     */
    private boolean byteArrayFromHexString;

    /**
     * 是否将未知的枚举类型解析为null
     */
    private boolean unknownEnumAsNull;

    /**
     * 允许key字段使用单引号
     */
    private boolean allowSingleQuotes;

    /**
     * 允许key字段没有双引号
     */
    private boolean allowUnquotedFieldNames;

    /**
     * 允许注释
     */
    private boolean allowComment;

    /**
     * 接口或者抽象类无法实例化时启用属性默认值
     */
    private boolean useDefaultFieldInstance;

    /**
     * 使用BigDecimal作为默认number解析
     */
    private boolean useBigDecimalAsDefault;

    /**
     * 使用字段反序列化
     */
    private boolean useFields;

    /**
     * 根据path映射反序列类（Mapping inverse sequence classes according to path）
     */
    private Map<String, Class<?>> classReferenceOfPaths;

    public int getEndIndex() {
        return endIndex.get();
    }

    public void setEndIndex(int index) {
        endIndex.set(index);
    }

    public StringBuilder getStringBuilder() {
        stringBuilder.setLength(0);
        return stringBuilder;
    }

    public boolean isDisableEscapeMode() {
        return disableEscapeMode;
    }

    public void setDisableEscapeMode(boolean disableEscapeMode) {
        this.disableEscapeMode = disableEscapeMode;
    }

    public boolean isByteArrayFromHexString() {
        return byteArrayFromHexString;
    }

    public void setByteArrayFromHexString(boolean byteArrayFromHexString) {
        this.byteArrayFromHexString = byteArrayFromHexString;
    }

    public boolean isUnknownEnumAsNull() {
        return unknownEnumAsNull;
    }

    public void setUnknownEnumAsNull(boolean unknownEnumAsNull) {
        this.unknownEnumAsNull = unknownEnumAsNull;
    }

    public boolean isAllowSingleQuotes() {
        return allowSingleQuotes;
    }

    public void setAllowSingleQuotes(boolean allowSingleQuotes) {
        this.allowSingleQuotes = allowSingleQuotes;
    }

    public boolean isAllowUnquotedFieldNames() {
        return allowUnquotedFieldNames;
    }

    public void setAllowUnquotedFieldNames(boolean allowUnquotedFieldNames) {
        this.allowUnquotedFieldNames = allowUnquotedFieldNames;
    }

    public boolean isAllowComment() {
        return allowComment;
    }

    public void setAllowComment(boolean allowComment) {
        this.allowComment = allowComment;
    }

    public boolean isUseDefaultFieldInstance() {
        return useDefaultFieldInstance;
    }

    public void setUseDefaultFieldInstance(boolean useDefaultFieldInstance) {
        this.useDefaultFieldInstance = useDefaultFieldInstance;
    }

    public boolean isUseBigDecimalAsDefault() {
        return useBigDecimalAsDefault;
    }

    public void setUseBigDecimalAsDefault(boolean useBigDecimalAsDefault) {
        this.useBigDecimalAsDefault = useBigDecimalAsDefault;
    }

    public boolean isUseFields() {
        return useFields;
    }

    public void setUseFields(boolean useFields) {
        this.useFields = useFields;
    }
}
