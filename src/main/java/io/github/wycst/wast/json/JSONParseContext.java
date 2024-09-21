package io.github.wycst.wast.json;

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
    public int endIndex;
    public int endToken;

    /**
     * JSON写入器
     * （Character builder）
     */
    public JSONCharArrayWriter writer;

    /**
     * 从16进制字符串中转化为字节数组
     */
    public boolean byteArrayFromHexString;

    /**
     * 是否将未知的枚举类型解析为null
     */
    public boolean unknownEnumAsNull;

    /**
     * 允许key字段使用单引号
     */
    public boolean allowSingleQuotes;

    /**
     * 允许key字段没有双引号
     */
    public boolean allowUnquotedFieldNames;

    /**
     * 允许注释
     */
    public boolean allowComment;

    /**
     * 是否允许对象属性或者集合元素中最后一个元素后面是逗号（非JSON标准格式）
     */
    public boolean allowLastEndComma;

    /**
     * 接口或者抽象类无法实例化时启用属性默认值
     */
    public boolean useDefaultFieldInstance;

    /**
     * 使用BigDecimal作为默认number解析
     */
    public boolean useBigDecimalAsDefault;

    /**
     * 使用Double.parse来处理声明为double类型的属性的解析操作
     */
    @Deprecated
    public boolean useJDKDoubleParser;

    /**
     * 禁用cache key
     */
    public boolean disableCacheMapKey;
    public boolean unMatchedEmptyAsNull;
    public boolean strictMode;

    void setIgnoreEscapeCheck() {
        escape = false;
    }

    boolean escape = true;
    int escapeOffset = -1;
    private String[] strings;

    public void setContextWriter(JSONCharArrayWriter writer) {
        this.writer = writer;
    }

    JSONCharArrayWriter getContextWriter() {
        if (writer != null) {
            writer.clear();
        }
        return writer;
    }

    String[] getContextStrings() {
        if(strings == null) {
            strings = new String[32];
        }
        return strings;
    }

    void clear() {
        if (writer != null) {
            writer.reset();
            writer = null;
        }
        strings = null;
    }

    final boolean checkEscapeUseChar(String input, int fromIndex, int endIndex) {
        if(!escape || endIndex < escapeOffset) return false;
        if(fromIndex > escapeOffset) {
            escapeOffset = input.indexOf('\\', fromIndex);
            escape = escapeOffset > -1;
            if(!escape) return false;
        }
        return endIndex > escapeOffset;
    }

    final boolean checkEscapeUseString(String input, int fromIndex, int endIndex) {
        if(!escape || endIndex < escapeOffset) return false;
        if(fromIndex > escapeOffset) {
            escapeOffset = input.indexOf("\\", fromIndex);
            escape = escapeOffset > -1;
            if(!escape) return false;
        }
        return endIndex > escapeOffset;
    }

    final int getEscapeOffset() {
        return escapeOffset;
    }

    protected JSONKeyValueMap<String> getTable32() {
        return JSONGeneral.KEY_32_TABLE;
    }

    protected JSONKeyValueMap<String> getTable8() {
        return JSONGeneral.KEY_8_TABLE;
    }

    protected final String getCacheEightCharsKey(char[] buf, int offset, int len, long hashValue) {
        return JSONGeneral.getCacheEightCharsKey(buf, offset, len, hashValue, getTable8());
    }

    protected final String getCacheKey(char[] buf, int offset, int len, long hashValue) {
        return JSONGeneral.getCacheKey(buf, offset, len, hashValue, getTable32());
    }

    protected final String getCacheEightBytesKey(byte[] bytes, int offset, int len, long hashValue) {
        return JSONGeneral.getCacheEightBytesKey(bytes, offset, len, hashValue, getTable8());
    }

    protected final String getCacheKey(byte[] bytes, int offset, int len, long hashValue) {
        return JSONGeneral.getCacheKey(bytes, offset, len, hashValue, getTable32());
    }
}
