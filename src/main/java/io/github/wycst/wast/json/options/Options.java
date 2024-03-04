package io.github.wycst.wast.json.options;

import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.json.util.FixedNameValueMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Options {

    // cache keys
    private static FixedNameValueMap<String> keyValueMap = new FixedNameValueMap<String>(4096);

    private static Object lock = new Object();

    // global set cache keys
    public static void addGlobalKeys(String... keys) {
        synchronized (lock) {
            Set<String> keySet = new HashSet<String>(Arrays.asList(keys));
            for (String key : keySet) {
                if (key == null || key.trim().length() == 0) continue;
                keyValueMap.putValue(key, key.hashCode(), key);
            }
        }
    }

    // global set cache keys
    public static void setGlobalKeys(String... keys) {
        clearGlobalKeys();
        addGlobalKeys(keys);
    }

    // clear cache keys
    public static void clearGlobalKeys() {
        synchronized (lock) {
            keyValueMap.reset();
        }
    }

    static String getCacheKey(char[] buf, int offset, int len, int hashCode) {
        //  len > 0
        String value = keyValueMap.getValue(buf, offset, offset + len, hashCode);
        if (value == null) {
            value = StringUtils.create(buf, offset, len);
            keyValueMap.putValue(value, value.hashCode() ,value);
        }
        return value;
    }

    static String getCacheKey(byte[] bytes, int offset, int len, int hashCode) {
        //  len > 0
        String value = keyValueMap.getValue(bytes, offset, offset + len, hashCode);
        if (value == null) {
            value = new String(bytes, offset, len);
            keyValueMap.putValue(value, value.hashCode(), value);
        }
        return value;
    }

    private static void setWriteOption(WriteOption option, JsonConfig jsonConfig) {
        if (jsonConfig != null) {
            switch (option) {
                case FormatOut:
                    jsonConfig.setFormatOut(true);
                    break;
                case FormatIndentUseTab:
                    jsonConfig.setFormatIndentUseSpace(false);
                    break;
                case FormatIndentUseSpace:
                    jsonConfig.setFormatIndentUseSpace(true);
                    break;
                case FormatIndentUseSpace8:
                    jsonConfig.setFormatIndentUseSpace(true);
                    jsonConfig.setFormatIndentSpaceNum(8);
                    break;
                case FullProperty:
                    jsonConfig.setFullProperty(true);
                    break;
                case IgnoreNullProperty:
                    jsonConfig.setFullProperty(false);
                    break;
                case DateFormat:
                    jsonConfig.setDateFormat(true);
                    jsonConfig.setDateFormatPattern("yyyy-MM-dd HH:mm:ss");
                    break;
                case WriteDateAsTime:
                    jsonConfig.setWriteDateAsTime(true);
                    break;
                case WriteEnumAsOrdinal:
                    jsonConfig.setWriteEnumAsOrdinal(true);
                    break;
                case WriteEnumAsName:
                    jsonConfig.setWriteEnumAsOrdinal(false);
                    break;
                case WriteNumberAsString:
                    jsonConfig.setWriteNumberAsString(true);
                    break;
                case WriteDecimalUseToString:
                    jsonConfig.setWriteDecimalUseToString(true);
                    break;
                case SkipCircularReference:
                    jsonConfig.setSkipCircularReference(true);
                    break;
                case BytesArrayToNative:
                    jsonConfig.setBytesArrayToNative(true);
                    break;
                case DisableEscapeValidate:
                    jsonConfig.setDisableEscapeValidate(true);
                    break;
                case BytesArrayToHex:
                    jsonConfig.setBytesArrayToHex(true);
                    break;
                case SkipGetterOfNoneField:
                    jsonConfig.setSkipGetterOfNoneField(true);
                    break;
                case KeepOpenStream:
                    jsonConfig.setAutoCloseStream(false);
                    break;
                case AllowUnquotedMapKey:
                    jsonConfig.setAllowUnquotedMapKey(true);
                    break;
                case UseFields:
                    jsonConfig.setUseFields(true);
                    break;
                case CamelCaseToUnderline:
                    jsonConfig.setCamelCaseToUnderline(true);
                    break;
                case WriteClassName:
                    jsonConfig.setWriteClassName(true);
                    break;
            }
        }
    }

    private static void setParseContextOption(ReadOption option, JSONParseContext parseContext) {
        if (parseContext != null) {
            switch (option) {
                case ByteArrayFromHexString:
                    parseContext.byteArrayFromHexString = true;
                    break;
                case UnknownEnumAsNull:
                    parseContext.unknownEnumAsNull = true;
                    break;
                case AllowSingleQuotes:
                    parseContext.allowSingleQuotes = true;
                    break;
                case AllowUnquotedFieldNames:
                    parseContext.allowUnquotedFieldNames = true;
                    break;
                case AllowComment:
                    parseContext.allowComment = true;
                    break;
                case AllowLastEndComma:
                    parseContext.allowLastEndComma = true;
                    break;
                case UseDefaultFieldInstance:
                    parseContext.useDefaultFieldInstance = true;
                    break;
                case UseBigDecimalAsDefaultNumber:
                    parseContext.useBigDecimalAsDefault = true;
                    break;
                case UseNativeDoubleParser:
                    parseContext.useNativeDoubleParser = true;
                    break;
                case UnMatchedEmptyAsNull:
                    parseContext.unMatchedEmptyAsNull = true;
                    break;
                case DisableCacheMapKey:
                    parseContext.disableCacheMapKey = true;
                    break;
//                case UseFields:
//                    parseContext.setUseFields(true);
//                    break;
            }
        }
    }

    public final static void writeOptions(WriteOption[] options, JsonConfig jsonConfig) {
        if (options == null || options.length == 0) return;
        for (WriteOption option : options) {
            setWriteOption(option, jsonConfig);
        }
    }

    public final static void readOptions(ReadOption[] options, JSONParseContext parseContext) {
        if (options == null || options.length == 0) return;
        for (ReadOption option : options) {
            setParseContextOption(option, parseContext);
        }
    }
}
