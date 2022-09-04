package io.github.wycst.wast.json.options;

import io.github.wycst.wast.json.JSONStringWriter;

/**
 * json解析上下文配置
 *
 * @Author: wangy
 * @Date: 2021/12/19 14:47
 * @Description:
 */
public class JSONParseContext {

    /***
     * 解析上下文结束位置
     */
    private int endIndex;

    /**
     * JSON写入器
     * （Character builder）
     */
    JSONStringWriter writer;

    /**
     * 禁用转义模式，已确定没有转义符场景下使用，能适当加速字符串解析
     * （The escape mode is disabled. It has been determined that it can be used in scenarios without escape characters, which can appropriately speed up string parsing）
     */
    @Deprecated
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
     * 使用Double.parse来处理声明为double类型的属性的解析操作
     */
    private boolean useNativeDoubleParser;

    /**
     * 使用字段反序列化
     */
    private boolean useFields;

    /**
     * 禁用cache key
     */
    private boolean disableCacheMapKey;

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int index) {
        endIndex = index;
    }

    public void setContextWriter(JSONStringWriter writer) {
        this.writer = writer;
    }

    public JSONStringWriter getContextWriter() {
        if (writer != null) {
            writer.reset();
        }
        return writer;
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

    public boolean isUseNativeDoubleParser() {
        return useNativeDoubleParser;
    }

    public void setUseNativeDoubleParser(boolean useNativeDoubleParser) {
        this.useNativeDoubleParser = useNativeDoubleParser;
    }

    public boolean isDisableCacheMapKey() {
        return disableCacheMapKey;
    }

    public void setDisableCacheMapKey(boolean disableCacheMapKey) {
        this.disableCacheMapKey = disableCacheMapKey;
    }

    public final String getCacheKey(char[] buf, int offset, int len, int hashCode) {
        return Options.getCacheKey(buf, offset, len, hashCode);
    }

    public final String getCacheKey(byte[] bytes, int offset, int len, int hashCode) {
        return Options.getCacheKey(bytes, offset, len, hashCode);
    }

    public void clear() {
        if (writer != null) {
            writer.reset();
        }
    }
}
