package io.github.wycst.wast.json.options;

import io.github.wycst.wast.json.util.FixedNameValueMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Options {

    // Format output character pool
    public static final String writeFormatOutSymbol = "\n\t\t\t\t\t\t\t\t\t\t";

    // cache keys
    private static FixedNameValueMap<String> keyValueMap = new FixedNameValueMap<String>(4096);

    private static Object lock = new Object();

    // global set cache keys
    public static void addGlobalKeys(String... keys) {
        synchronized (lock) {
            Set<String> keySet = new HashSet<String>(Arrays.asList(keys));
            for (String key : keySet) {
                if (key == null || key.trim().length() == 0) continue;
                keyValueMap.putValue(key, key);
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
            value = new String(buf, offset, len);
            keyValueMap.putValue(value, value);
        }
        return value;
    }

    private static void setWriteOption(WriteOption option, JsonConfig jsonConfig) {
        if (jsonConfig != null) {
            switch (option) {
                case FormatOut:
                    jsonConfig.setFormatOut(true);
                    break;
                case FullProperty:
                    jsonConfig.setFullProperty(true);
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
            }
        }
    }

    private static void setParseContextOption(ReadOption option, JSONParseContext parseContext) {
        if (parseContext != null) {
            switch (option) {
                case ByteArrayFromHexString:
                    parseContext.setByteArrayFromHexString(true);
                    break;
                case DisableEscapeValidate:
                    parseContext.setDisableEscapeMode(true);
                    break;
                case UnknownEnumAsNull:
                    parseContext.setUnknownEnumAsNull(true);
                    break;
                case AllowSingleQuotes:
                    parseContext.setAllowSingleQuotes(true);
                    break;
                case AllowUnquotedFieldNames:
                    parseContext.setAllowUnquotedFieldNames(true);
                    break;
                case AllowComment:
                    parseContext.setAllowComment(true);
                    break;
                case UseDefaultFieldInstance:
                    parseContext.setUseDefaultFieldInstance(true);
                    break;
                case UseBigDecimalAsDefaultNumber:
                    parseContext.setUseBigDecimalAsDefault(true);
                    break;
                case UseNativeDoubleParser:
                    parseContext.setUseNativeDoubleParser(true);
                    break;
                case DisableCacheMapKey:
                    parseContext.setDisableCacheMapKey(true);
                    break;
//                case UseFields:
//                    parseContext.setUseFields(true);
//                    break;
            }
        }
    }

    public static void writeOptions(WriteOption[] options, JsonConfig jsonConfig) {
        if (options == null) return;
        for (WriteOption option : options) {
            setWriteOption(option, jsonConfig);
        }
    }

    public static void readOptions(ReadOption[] options, JSONParseContext parseContext) {
        if (options == null) return;
        for (ReadOption option : options) {
            setParseContextOption(option, parseContext);
        }
    }
}
