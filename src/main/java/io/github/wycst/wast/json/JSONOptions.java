package io.github.wycst.wast.json;

import io.github.wycst.wast.json.options.ReadOption;
import io.github.wycst.wast.json.options.WriteOption;

final class JSONOptions {

    private static void setWriteOption(WriteOption option, JSONConfig jsonConfig) {
        if (jsonConfig != null) {
            switch (option) {
                case FormatOut:
                    jsonConfig.setFormatOut(true);
                    break;
                case FormatOutColonSpace:
                    jsonConfig.setFormatOut(true);
                    jsonConfig.setFormatOutColonSpace(true);
                case FormatIndentUseTab:
                    jsonConfig.setFormatOut(true);
                    jsonConfig.setFormatIndentUseSpace(false);
                    break;
                case FormatIndentUseSpace:
                    jsonConfig.setFormatOut(true);
                    jsonConfig.setFormatIndentUseSpace(true);
                    break;
                case FormatIndentUseSpace8:
                    jsonConfig.setFormatOut(true);
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
                case UseBigDecimalAsDefault:
                    parseContext.useBigDecimalAsDefault = true;
                    break;
                case UseJDKDoubleParser:
                    parseContext.useJDKDoubleParser = true;
                    break;
                case UnMatchedEmptyAsNull:
                    parseContext.unMatchedEmptyAsNull = true;
                    break;
                case DisableCacheMapKey:
                    parseContext.disableCacheMapKey = true;
                    break;
                case IgnoreEscapeCheck:
                    parseContext.setIgnoreEscapeCheck();
                    break;
                case StrictMode:
                    parseContext.strictMode = true;
                    break;
            }
        }
    }

    static void writeOptions(WriteOption[] options, JSONConfig jsonConfig) {
        if (options == null || options.length == 0) return;
        for (WriteOption option : options) {
            setWriteOption(option, jsonConfig);
        }
    }

    static void readOptions(ReadOption[] options, JSONParseContext parseContext) {
        if (options == null || options.length == 0) return;
        for (ReadOption option : options) {
            setParseContextOption(option, parseContext);
        }
    }
}
