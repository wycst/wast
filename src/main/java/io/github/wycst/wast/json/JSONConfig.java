package io.github.wycst.wast.json;

import io.github.wycst.wast.json.options.WriteOption;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public final class JSONConfig {

    private static boolean defaultFullProperty;
    private static String defaultDateFormatPattern;
    private static boolean defaultFormatIndentUseSpace;
    private static int defaultFormatIndentSpaceNum = 4;
    private static boolean defaultWriteEnumAsOrdinal;

    public static void setDefaultFullProperty(boolean defaultFullProperty) {
        JSONConfig.defaultFullProperty = defaultFullProperty;
    }

    public static void setDefaultDateFormatPattern(String defaultDateFormatPattern) {
        JSONConfig.defaultDateFormatPattern = defaultDateFormatPattern;
    }

    public static void setDefaultFormatIndentUseSpace(boolean defaultFormatIndentUseSpace) {
        JSONConfig.defaultFormatIndentUseSpace = defaultFormatIndentUseSpace;
    }

    public static void setDefaultFormatIndentSpaceNum(int defaultFormatIndentSpaceNum) {
        JSONConfig.defaultFormatIndentSpaceNum = defaultFormatIndentSpaceNum;
    }

    public static void setDefaultWriteEnumAsOrdinal(boolean defaultWriteEnumAsOrdinal) {
        JSONConfig.defaultWriteEnumAsOrdinal = defaultWriteEnumAsOrdinal;
    }

    /**
     * 格式化输出
     */
    public boolean formatOut;

    /**
     * 格式化输出
     */
    public boolean formatOutColonSpace;

    /**
     * 格式化缩进使用空格模式
     */
    public boolean formatIndentUseSpace = defaultFormatIndentUseSpace;

    /**
     * 缩进空格数量,默认4个空格
     */
    public int formatIndentSpaceNum = Math.max(defaultFormatIndentSpaceNum, 1);

    /**
     * 输出全属性
     */
    boolean fullProperty = defaultFullProperty;

    /**
     * 以yyyy-MM-dd HH:mm:ss 格式化日期对象
     */
    boolean dateFormat;

    /**
     * 是否序列化日期为时间戳
     */
    boolean writeDateAsTime;

    /**
     * 是否序列化日期为时间戳
     */
    boolean writeEnumAsOrdinal = defaultWriteEnumAsOrdinal;

    /**
     * 是否将数字类序列化位字符串
     */
    boolean writeNumberAsString;

    /**
     * 是否使用toString序列化浮点数
     */
    boolean writeDecimalUseToString;

    /**
     * 跳过循环序列化
     */
    boolean skipCircularReference;

    /**
     * 日期格式默认： yyyy-MM-dd HH:mm:ss
     */
    private String dateFormatPattern = defaultDateFormatPattern;

    /**
     * 是否将byte[]数组按数组序列化
     * Serialize byte [] array to Base64
     */
    boolean bytesArrayToNative;

    /**
     * 是否将byte[]数组序列化为16进制字符串
     */
    boolean bytesArrayToHex;

    /**
     * 忽略转义检查
     */
    boolean ignoreEscapeCheck;

    /**
     * 跳过没有属性Field的getter方法序列化
     */
    boolean skipGetterOfNoneField;

    /**
     * 自动关闭流
     */
    boolean autoCloseStream = true;

    /**
     * 允许map的key根据实际类型序列化而不是双引号包围
     */
    boolean allowUnquotedMapKey;

    /**
     * 使用字段序列化
     */
    boolean useFields;

    /**
     * 是否驼峰转下划线
     */
    boolean camelCaseToUnderline;

    /**
     * 是否序列化类型
     */
    boolean writeClassName;

    /**
     * 指定时区
     */
    TimeZone timezone;

    private Map<Integer, Integer> identityHashCodes;

    public JSONConfig() {
    }

    public JSONConfig(WriteOption[] writeOptions) {
        JSONOptions.writeOptions(writeOptions, this);
    }

    public static JSONConfig config(WriteOption... options) {
        return new JSONConfig(options);
    }

    public boolean isFormatIndentUseSpace() {
        return formatIndentUseSpace;
    }

    public void setFormatIndentUseSpace(boolean formatIndentUseSpace) {
        this.formatIndentUseSpace = formatIndentUseSpace;
    }

    public int getFormatIndentSpaceNum() {
        return formatIndentSpaceNum;
    }

    public void setFormatIndentSpaceNum(int formatIndentSpaceNum) {
        this.formatIndentSpaceNum = Math.max(formatIndentSpaceNum, 1);
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

    public TimeZone getTimezone() {
        return timezone;
    }

    public void setTimezone(TimeZone timezone) {
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

    public boolean isFormatOutColonSpace() {
        return formatOutColonSpace;
    }

    public void setFormatOutColonSpace(boolean formatOutColonSpace) {
        this.formatOutColonSpace = formatOutColonSpace;
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

    public boolean isIgnoreEscapeCheck() {
        return ignoreEscapeCheck;
    }

    public void setIgnoreEscapeCheck(boolean ignoreEscapeCheck) {
        this.ignoreEscapeCheck = ignoreEscapeCheck;
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

    public boolean isWriteNumberAsString() {
        return writeNumberAsString;
    }

    public void setWriteNumberAsString(boolean writeNumberAsString) {
        this.writeNumberAsString = writeNumberAsString;
    }

    public void setWriteEnumAsOrdinal(boolean writeEnumAsOrdinal) {
        this.writeEnumAsOrdinal = writeEnumAsOrdinal;
    }

    public boolean isWriteClassName() {
        return writeClassName;
    }

    public void setWriteClassName(boolean writeClassName) {
        this.writeClassName = writeClassName;
    }

    void setStatus(int hashcode, int status) {
        if (skipCircularReference) {
            Map<Integer, Integer> hashCodeStatus = getOrSetIdentityHashCodes();
            hashCodeStatus.put(hashcode, status);
        }
    }

    private synchronized Map<Integer, Integer> getOrSetIdentityHashCodes() {
        if (identityHashCodes == null) {
            identityHashCodes = new HashMap<Integer, Integer>();
        }
        return identityHashCodes;
    }

    int getStatus(int hashcode) {
        Map<Integer, Integer> hashCodeStatus = getOrSetIdentityHashCodes();
        if (hashCodeStatus.containsKey(hashcode)) {
            return hashCodeStatus.get(hashcode);
        }
        return -1;
    }

    public void clear() {
        if (skipCircularReference) {
            if (identityHashCodes != null) {
                identityHashCodes.clear();
            }
        }
    }

    public void setWriteDecimalUseToString(boolean writeDecimalUseToString) {
        this.writeDecimalUseToString = writeDecimalUseToString;
    }

    @Deprecated
    public boolean isWriteDecimalUseToString() {
        return writeDecimalUseToString;
    }
}
