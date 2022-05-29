package org.framework.light.json.options;

import java.util.HashMap;
import java.util.Map;

public class JsonConfig {

    /**
     * 格式化输出
     */
    private boolean writeOptionFormatOut;

    /**
     * 输出全属性
     */
    private boolean writeOptionFullProperty;

    /**
     * 以yyyy-MM-dd HH:mm:ss 格式化日期对象
     */
    private boolean writeOptionDateFormat;

    /** 是否序列化日期为时间戳 */
    private boolean writeDateAsTime;

    /**
     * 跳过循环序列化
     */
    private boolean writeOptionSkipCircularReference;

    /**
     * 日期格式默认： yyyy-MM-dd HH:mm:ss
     */
    private String dateFormatPattern;

    /**
     * 是否将byte[]数组按数组序列化
     * Serialize byte [] array to Base64
     */
    private boolean writeOptionBytesArrayToNative;

    /**
     * 是否将byte[]数组序列化为16进制字符串
     */
    private boolean writeOptionBytesArrayToHex;

    /**
     * 禁用转义符检查
     */
    private boolean writeOptionDisableEscapeValidate;

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

    /** 是否驼峰转下划线 */
    private boolean camelCaseToUnderline;

    /**
     * 指定时区
     */
    private String timezone;

    private Map<Integer, Integer> hashCodeStatus = new HashMap<Integer, Integer>();
    private static ThreadLocal<Map<Integer, Integer>> identityHashCodes = new ThreadLocal<Map<Integer, Integer>>();

    public JsonConfig() {

    }

    public JsonConfig(WriteOption[] writeOptions) {
        Options.writeOptions(writeOptions, this);
    }

    public boolean isWriteOptionFormatOut() {
        return writeOptionFormatOut;
    }

    public void setWriteOptionFormatOut(boolean writeOptionFormatOut) {
        this.writeOptionFormatOut = writeOptionFormatOut;
    }

    public boolean isWriteOptionFullProperty() {
        return writeOptionFullProperty;
    }

    public void setWriteOptionFullProperty(boolean writeOptionFullProperty) {
        this.writeOptionFullProperty = writeOptionFullProperty;
    }

    public boolean isWriteOptionDateFormat() {
        return writeOptionDateFormat;
    }

    public void setWriteOptionDateFormat(boolean writeOptionDateFormat) {
        this.writeOptionDateFormat = writeOptionDateFormat;
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

    public boolean isWriteOptionSkipCircularReference() {
        return writeOptionSkipCircularReference;
    }

    public void setWriteOptionSkipCircularReference(boolean writeOptionSkipCircularReference) {
        this.writeOptionSkipCircularReference = writeOptionSkipCircularReference;
    }

    public boolean isWriteOptionBytesArrayToNative() {
        return writeOptionBytesArrayToNative;
    }

    public void setWriteOptionBytesArrayToNative(boolean writeOptionBytesArrayToNative) {
        this.writeOptionBytesArrayToNative = writeOptionBytesArrayToNative;
    }

    public boolean isWriteOptionBytesArrayToHex() {
        return writeOptionBytesArrayToHex;
    }

    public void setWriteOptionBytesArrayToHex(boolean writeOptionBytesArrayToHex) {
        this.writeOptionBytesArrayToHex = writeOptionBytesArrayToHex;
    }

    public boolean isWriteOptionDisableEscapeValidate() {
        return writeOptionDisableEscapeValidate;
    }

    public void setWriteOptionDisableEscapeValidate(boolean writeOptionDisableEscapeValidate) {
        this.writeOptionDisableEscapeValidate = writeOptionDisableEscapeValidate;
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

    public void setStatus(int hashcode, int status) {
        if (writeOptionSkipCircularReference) {
            Map<Integer, Integer> hashCodeStatus = getOrSetIdentityHashCodes();
            hashCodeStatus.put(hashcode, status);
        }
    }

    private Map<Integer, Integer> getOrSetIdentityHashCodes() {
        Map<Integer, Integer> hashCodeStatus = identityHashCodes.get();
        if (hashCodeStatus == null) {
            identityHashCodes.set(this.hashCodeStatus);
            hashCodeStatus = this.hashCodeStatus;
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
        hashCodeStatus.clear();
        hashCodeStatus = null;
    }

}
