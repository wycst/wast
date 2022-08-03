package io.github.wycst.wast.json.options;

import java.util.HashMap;
import java.util.Map;

public class JsonConfig {

    /**
     * 格式化输出
     */
    private boolean formatOut;

    /**
     * 输出全属性
     */
    private boolean fullProperty;

    /**
     * 以yyyy-MM-dd HH:mm:ss 格式化日期对象
     */
    private boolean dateFormat;

    /**
     * 是否序列化日期为时间戳
     */
    private boolean writeDateAsTime;

    /**
     * 是否序列化日期为时间戳
     */
    private boolean writeEnumAsOrdinal;

    /**
     * 跳过循环序列化
     */
    private boolean skipCircularReference;

    /**
     * 日期格式默认： yyyy-MM-dd HH:mm:ss
     */
    private String dateFormatPattern;

    /**
     * 是否将byte[]数组按数组序列化
     * Serialize byte [] array to Base64
     */
    private boolean bytesArrayToNative;

    /**
     * 是否将byte[]数组序列化为16进制字符串
     */
    private boolean bytesArrayToHex;

    /**
     * 禁用转义符检查
     */
    private boolean disableEscapeValidate;

    /**
     * 跳过没有属性Field的getter方法序列化
     */
    private boolean skipGetterOfNoneField;

    /**
     * 自动关闭流
     */
    private boolean autoCloseStream = true;

    /**
     * 允许map的key根据实际类型序列化而不是双引号包围
     */
    private boolean allowUnquotedMapKey;

    /**
     * 使用字段序列化
     */
    private boolean useFields;

    /**
     * 是否驼峰转下划线
     */
    private boolean camelCaseToUnderline;

    /**
     * 指定时区
     */
    private String timezone;

    private static ThreadLocal<Map<Integer, Integer>> identityHashCodes = new ThreadLocal<Map<Integer, Integer>>();

    private char[] contextChars;

    public JsonConfig() {
    }

    public JsonConfig(WriteOption[] writeOptions) {
        Options.writeOptions(writeOptions, this);
    }

    public boolean isWriteDateAsTime() {
        return writeDateAsTime;
    }

    public void setWriteDateAsTime(boolean writeDateAsTime) {
        this.writeDateAsTime = writeDateAsTime;
    }

    public String getDateFormatPattern() {
        return dateFormatPattern;
    }

    public void setDateFormatPattern(String dateFormatPattern) {
        this.dateFormatPattern = dateFormatPattern;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isSkipCircularReference() {
        return skipCircularReference;
    }

    public void setSkipCircularReference(boolean skipCircularReference) {
        this.skipCircularReference = skipCircularReference;
    }

    public boolean isBytesArrayToNative() {
        return bytesArrayToNative;
    }

    public void setBytesArrayToNative(boolean bytesArrayToNative) {
        this.bytesArrayToNative = bytesArrayToNative;
    }

    public boolean isFormatOut() {
        return formatOut;
    }

    public void setFormatOut(boolean formatOut) {
        this.formatOut = formatOut;
    }

    public boolean isFullProperty() {
        return fullProperty;
    }

    public void setFullProperty(boolean fullProperty) {
        this.fullProperty = fullProperty;
    }

    public boolean isDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(boolean dateFormat) {
        this.dateFormat = dateFormat;
    }

    public boolean isBytesArrayToHex() {
        return bytesArrayToHex;
    }

    public void setBytesArrayToHex(boolean bytesArrayToHex) {
        this.bytesArrayToHex = bytesArrayToHex;
    }

    public boolean isDisableEscapeValidate() {
        return disableEscapeValidate;
    }

    public void setDisableEscapeValidate(boolean disableEscapeValidate) {
        this.disableEscapeValidate = disableEscapeValidate;
    }

    public boolean isSkipGetterOfNoneField() {
        return skipGetterOfNoneField;
    }

    public void setSkipGetterOfNoneField(boolean skipGetterOfNoneField) {
        this.skipGetterOfNoneField = skipGetterOfNoneField;
    }

    public boolean isAutoCloseStream() {
        return autoCloseStream;
    }

    public void setAutoCloseStream(boolean autoCloseStream) {
        this.autoCloseStream = autoCloseStream;
    }

    public boolean isAllowUnquotedMapKey() {
        return allowUnquotedMapKey;
    }

    public void setAllowUnquotedMapKey(boolean allowUnquotedMapKey) {
        this.allowUnquotedMapKey = allowUnquotedMapKey;
    }

    public boolean isUseFields() {
        return useFields;
    }

    public void setUseFields(boolean useFields) {
        this.useFields = useFields;
    }

    public boolean isCamelCaseToUnderline() {
        return camelCaseToUnderline;
    }

    public void setCamelCaseToUnderline(boolean camelCaseToUnderline) {
        this.camelCaseToUnderline = camelCaseToUnderline;
    }

    public boolean isWriteEnumAsOrdinal() {
        return writeEnumAsOrdinal;
    }

    public void setWriteEnumAsOrdinal(boolean writeEnumAsOrdinal) {
        this.writeEnumAsOrdinal = writeEnumAsOrdinal;
    }

    public void setStatus(int hashcode, int status) {
        if (skipCircularReference) {
            Map<Integer, Integer> hashCodeStatus = getOrSetIdentityHashCodes();
            hashCodeStatus.put(hashcode, status);
        }
    }

    private Map<Integer, Integer> getOrSetIdentityHashCodes() {
        Map<Integer, Integer> hashCodeStatus = identityHashCodes.get();
        if (hashCodeStatus == null) {
            hashCodeStatus = new HashMap<Integer, Integer>();
            identityHashCodes.set(hashCodeStatus);
        }
        return hashCodeStatus;
    }

    public int getStatus(int hashcode) {
        Map<Integer, Integer> hashCodeStatus = getOrSetIdentityHashCodes();
        if (hashCodeStatus.containsKey(hashcode)) {
            return hashCodeStatus.get(hashcode);
        }
        return -1;
    }

    public void clear() {
        identityHashCodes.remove();
    }

    public char[] getContextChars() {
        if (contextChars == null) {
            contextChars = new char[20];
        }
        return contextChars;
    }
}
