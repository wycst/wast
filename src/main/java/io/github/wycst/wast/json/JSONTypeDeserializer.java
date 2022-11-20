package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.CharSource;
import io.github.wycst.wast.common.beans.DateTemplate;
import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.JSONParseContext;
import io.github.wycst.wast.json.reflect.FieldDeserializer;
import io.github.wycst.wast.json.reflect.ObjectStructureWrapper;
import io.github.wycst.wast.json.util.FixedNameValueMap;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 类型反序列器
 *
 * @Author: wangy
 * @Date: 2022/6/18 5:59
 * @Description:
 */
public abstract class JSONTypeDeserializer extends JSONGeneral {

    private final static int LENGTH = ReflectConsts.ClassCategory.values().length;

    final static JSONTypeDeserializer[] TYPE_DESERIALIZERS = new JSONTypeDeserializer[LENGTH];
    final protected static CharSequenceDeserializer STRING = new CharSequenceDeserializer();
    final protected static NumberDeserializer NUMBER = new NumberDeserializer();
    final static BooleanDeserializer BOOLEAN = new BooleanDeserializer();
    final protected static ArrayDeserializer ARRAY = new ArrayDeserializer();
    final protected static CollectionDeserializer COLLECTION = new CollectionDeserializer();
    final protected static MapDeserializer MAP = new MapDeserializer();
    final protected static ObjectDeserializer OBJECT = new ObjectDeserializer();
    final protected static JSONTypeDeserializer ANY = new ANYDeserializer();
    final protected static NULLDeserializer NULL = new NULLDeserializer();
    // java.io.Serializable
    final protected static SerializableDeserializer SERIALIZABLE_DESERIALIZER = new SerializableDeserializer();

    // class and JSONTypeDeserializer mapping
    private static final Map<Class<?>, JSONTypeDeserializer> classJSONTypeDeserializerMap = new ConcurrentHashMap<Class<?>, JSONTypeDeserializer>();
    // classname mapping
    private static final Map<String, Class<?>> classNameMapping = new ConcurrentHashMap<String, Class<?>>();

    static {
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.CharSequence.ordinal()] = STRING;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.NumberCategory.ordinal()] = NUMBER;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.BoolCategory.ordinal()] = BOOLEAN;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.DateCategory.ordinal()] = new DateDeserializer();
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ClassCategory.ordinal()] = new ClassDeserializer();
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.EnumCategory.ordinal()] = new EnumDeserializer();
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.AnnotationCategory.ordinal()] = new AnnotationDeserializer();
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.Binary.ordinal()] = new BinaryDeserializer();
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ArrayCategory.ordinal()] = ARRAY;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.CollectionCategory.ordinal()] = COLLECTION;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.MapCategory.ordinal()] = MAP;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ObjectCategory.ordinal()] = OBJECT;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ANY.ordinal()] = ANY;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.NonInstance.ordinal()] = null;

        putTypeDeserializer(new NumberDeserializer.IntegerDeserializer(), int.class, Integer.class);
        putTypeDeserializer(new NumberDeserializer.LongDeserializer(), long.class, Long.class);
    }

    private static void putTypeDeserializer(JSONTypeDeserializer typeDeserializer, Class... types) {
        for (Class type : types) {
            classJSONTypeDeserializerMap.put(type, typeDeserializer);
        }
    }

    // Get the corresponding deserializer according to the type
    // Single example may be used
    public static JSONTypeDeserializer getTypeDeserializer(Class type) {
        if (type == null) return ANY;
        JSONTypeDeserializer typeDeserializer = classJSONTypeDeserializerMap.get(type);
        if (typeDeserializer != null) {
            return typeDeserializer;
        }
        ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(type);
        synchronized (type) {
            if (classJSONTypeDeserializerMap.containsKey(type)) {
                return classJSONTypeDeserializerMap.get(type);
            }
            switch (classCategory) {
                case ObjectCategory: {
                    typeDeserializer = createObjectDeserializer(type);
                    break;
                }
                case EnumCategory: {
                    typeDeserializer = new EnumDeserializer.EnumInstanceDeserializer(type);
                    break;
                }
            }
            if (typeDeserializer == null) {
                typeDeserializer = TYPE_DESERIALIZERS[classCategory.ordinal()];
                if (typeDeserializer == null) {
                    return null;
                }
            }
            classJSONTypeDeserializerMap.put(type, typeDeserializer);
        }

        return typeDeserializer;
    }

    private static JSONTypeDeserializer createObjectDeserializer(Class type) {
        ObjectStructureWrapper objectStructureWrapper = ObjectStructureWrapper.get(type);
        if (objectStructureWrapper.isRecord()) {
            return new JSONPojoDeserializer.JSONRecordDeserializer(type);
        } else {
            if (objectStructureWrapper.isCollision()) {
                return new JSONPojoDeserializer(type) {
                };
            } else {
                return new JSONPojoDeserializer(type) {
                    @Override
                    protected FieldDeserializer getFieldDeserializer(char[] buf, int offset, int endIndex, int hashValue) {
                        return super.getFieldDeserializer(hashValue);
                    }
                };
            }
        }
    }

    /**
     * use for FieldDeserializer
     *
     * @param genericParameterizedType
     * @param property
     * @return
     */
    protected static JSONTypeDeserializer getFieldDeserializer(GenericParameterizedType genericParameterizedType, JsonProperty property) {
        if (genericParameterizedType == null || genericParameterizedType.getActualType() == null) return ANY;
        ReflectConsts.ClassCategory classCategory = genericParameterizedType.getActualClassCategory();
        // Find matching deserializers or new deserializer instance by type
        switch (classCategory) {
            case CollectionCategory:
                // collection Deserializer instance
                return new CollectionDeserializer.CollectionInstanceDeserializer(genericParameterizedType);
            case DateCategory: {
                // Date Deserializer Instance
                // cannot use singleton because the pattern of each field may be different
                if (property != null) {
                    genericParameterizedType.setDatePattern(property.pattern().trim());
                    genericParameterizedType.setDateTimezone(property.timezone().trim());
                }
                return new DateDeserializer.DateInstanceDeserializer(genericParameterizedType);
            }
            case ObjectCategory: {
                ObjectStructureWrapper objectStructureWrapper = ObjectStructureWrapper.get(genericParameterizedType.getActualType());
                // Like the date type, the temporal type cannot use singletons also
                if (objectStructureWrapper.isTemporal()) {
                    ClassStructureWrapper.ClassWrapperType classWrapperType = objectStructureWrapper.getClassWrapperType();
                    if (property != null) {
                        genericParameterizedType.setDatePattern(property.pattern().trim());
                        genericParameterizedType.setDateTimezone(property.timezone().trim());
                    }
                    return JSONTemporalDeserializer.getTemporalDeserializerInstance(classWrapperType, genericParameterizedType);
                }
            }
        }

        // singleton from cache
        return getTypeDeserializer(genericParameterizedType.getActualType());
    }

    protected static Object doDeserialize(JSONTypeDeserializer deserializer, CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
        return deserializer.deserialize(charSource, buf, fromIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
    }

    protected static Object doDeserialize(JSONTypeDeserializer deserializer, CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
        return deserializer.deserialize(charSource, buf, fromIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
    }

    /***
     * 返回绑定类型
     *
     * @return
     */
    protected GenericParameterizedType getGenericParameterizedType() {
        return null;
    }

    /**
     * 跳过反序列
     *
     * @param buf
     * @param fromIndex
     * @param toIndex
     * @param endToken
     * @param jsonParseContext
     * @throws Exception
     */
    void skip(CharSource charSource, char[] buf, int fromIndex, int toIndex, char endToken, JSONParseContext jsonParseContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * 跳过反序列
     *
     * @param buf
     * @param fromIndex
     * @param toIndex
     * @param endToken
     * @param jsonParseContext
     * @throws Exception
     */
    void skip(CharSource charSource, byte[] buf, int fromIndex, int toIndex, byte endToken, JSONParseContext jsonParseContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * 核心反序列化
     *
     * @param buf
     * @param fromIndex
     * @param toIndex
     * @param jsonParseContext
     * @throws Exception
     */
    protected abstract Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception;

    /**
     * 拓展反序列化
     *
     * @param charSource       字符源（AsciiStringSource对象）
     * @param bytes            ascii字节数组
     * @param fromIndex
     * @param toIndex
     * @param jsonParseContext
     * @throws Exception
     */
    protected abstract Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception;

    // 0、字符串序列化
    static class CharSequenceDeserializer extends JSONTypeDeserializer {

        protected void skip(CharSource source, char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {

            int beginIndex = fromIndex + 1;
            char endCh = '"';
            if (source != null && JDK_9_ABOVE) {
                int endIndex = source.indexOf(endCh, beginIndex);
                char prev = buf[endIndex - 1];
                if (prev == '\\') {
                    do {
                        endIndex = source.indexOf(endCh, endIndex + 1);
                        prev = buf[endIndex - 1];
                    } while (prev == '\\');
                }
                jsonParseContext.setEndIndex(endIndex);
                return;
            }

            int i = fromIndex + 1;
            while (/*i < toIndex &&*/ buf[i] != '"' || buf[i - 1] == '\\') {
                ++i;
            }
            jsonParseContext.setEndIndex(i);
        }

        protected void skip(CharSource source, byte[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {

            int beginIndex = fromIndex + 1;
            char endCh = '"';
            if (source != null && JDK_9_ABOVE) {
                int endIndex = source.indexOf(endCh, beginIndex);
                byte prev = buf[endIndex - 1];
                if (prev == '\\') {
                    do {
                        endIndex = source.indexOf(endCh, endIndex + 1);
                        prev = buf[endIndex - 1];
                    } while (prev == '\\');
                }
                jsonParseContext.setEndIndex(endIndex);
                return;
            }

            int i = fromIndex + 1;
            while (/*i < toIndex &&*/ buf[i] != '"' || buf[i - 1] == '\\') {
                ++i;
            }
            jsonParseContext.setEndIndex(i);
        }

        Object deserializeString(CharSource source, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) {
            int beginIndex = fromIndex + 1;
            int paramClassType = parameterizedType.getParamClassType();
            boolean isCharArray = paramClassType == ReflectConsts.CLASS_TYPE_CHAR_ARRAY;
            JSONStringWriter writer = null;

            char ch = '\0', next = '\0';
            int i = beginIndex;
            int len;
            boolean escape = false;
            for (; /*i < toIndex*/ ; ++i) {
                while (/*i < toIndex &&*/ (ch = buf[i]) != '\\' && ch != '"') {
                    ++i;
                }

                // ch is \\ or "
                if (ch == '\\') {
                    if (i < toIndex - 1) {
                        next = buf[i + 1];
                    }
                    if (writer == null) {
                        writer = getContextWriter(jsonParseContext);
                        escape = true;
                    }
                    beginIndex = escapeNext(buf, next, i, beginIndex, writer, jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                } else {
                    jsonParseContext.setEndIndex(i);
                    len = i - beginIndex;
                    if (escape) {
                        writer.write(buf, beginIndex, len);
                        if (isCharArray) {
                            int charLen = writer.size();
                            char[] chars = new char[charLen];
                            writer.getChars(0, charLen, chars, 0);
                            return chars;
                        } else {
                            return getCharSequence(writer, paramClassType);
                        }
                    } else {
                        if (isCharArray) {
                            char[] chars = new char[len];
                            System.arraycopy(buf, beginIndex, chars, 0, len);
                            return chars;
                        } else {
                            return getCharSequence(source, buf, beginIndex, len, paramClassType);
                        }
                    }
                }
            }
        }

        Object deserializeString(CharSource source, byte[] buf, int fromIndex, int toIndex, byte endByte, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) {
            int paramClassType = parameterizedType.getParamClassType();
            String value = JSONByteArrayParser.parseJSONString(source == null ? null : source.input(), buf, fromIndex, toIndex, endByte, jsonParseContext);
            if (paramClassType == ReflectConsts.CLASS_TYPE_STRING) {
                return value;
            }
            switch (paramClassType) {
                case ReflectConsts.CLASS_TYPE_STRING_BUFFER:
                    return new StringBuffer(value);
                case ReflectConsts.CLASS_TYPE_STRING_BUILDER:
                    return new StringBuilder(value);
                case ReflectConsts.CLASS_TYPE_CHAR_ARRAY: {
                    return value.toCharArray();
                }
                default:
                    // todo
                    return null;
            }
        }

        // String/StringBuffer/StringBuilder/char[]
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];

            if(beginChar == '"') {
                return deserializeString(charSource, buf, fromIndex, toIndex, parameterizedType, jsonParseContext);
            } else if(beginChar == 'n') {
                return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
            } else {
                // 当类型为字符串时兼容number转化
                boolean isStringType = parameterizedType.getActualType() == String.class;
                if (isStringType) {
                    // 不使用skip的原因确保能转化为number，否则认定token错误
                    try {
                        NUMBER.deserializeOfString(buf, fromIndex, toIndex, endToken, ReflectConsts.CLASS_TYPE_NUMBER_DOUBLE, jsonParseContext);
                        return new String(buf, fromIndex, jsonParseContext.getEndIndex() + 1 - fromIndex);
                    } catch (Throwable throwable) {
                    }
                }
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + beginChar + "' for CharSequence, expected '\"' ");
            }
//            switch (beginChar) {
//                case '"': {
//                    return deserializeString(charSource, buf, fromIndex, toIndex, parameterizedType, jsonParseContext);
//                }
//                case 'n': {
//                    return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
//                }
//                default: {
//                    // 当类型为字符串时兼容number转化
//                    boolean isStringType = parameterizedType.getActualType() == String.class;
//                    if (isStringType) {
//                        // 不使用skip的原因确保能转化为number，否则认定token错误
//                        try {
//                            NUMBER.deserializeOfString(buf, fromIndex, toIndex, endToken, ReflectConsts.CLASS_TYPE_NUMBER_DOUBLE, jsonParseContext);
//                            return new String(buf, fromIndex, jsonParseContext.getEndIndex() + 1 - fromIndex);
//                        } catch (Throwable throwable) {
//                        }
//                    }
//                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
//                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + beginChar + "' for CharSequence, expected '\"' ");
//                }
//            }
        }

        @Override
        protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = bytes[fromIndex];
            switch (beginByte) {
                case '"': {
                    return deserializeString(charSource, bytes, fromIndex, toIndex, beginByte, parameterizedType, jsonParseContext);
                }
                case 'n': {
                    return JSONByteArrayParser.parseNull(bytes, fromIndex, toIndex, jsonParseContext);
                }
                default: {
                    // 当类型为字符串时兼容number转化
                    boolean isStringType = parameterizedType.getActualType() == String.class;
                    if (isStringType) {
                        // 不使用skip的原因确保能转化为number，否则认定token错误
                        try {
                            JSONByteArrayParser.parseDefaultNumber(bytes, fromIndex, toIndex, endToken, jsonParseContext);
                            return charSource == null ? new String(bytes, fromIndex, jsonParseContext.getEndIndex() + 1 - fromIndex) : charSource.getString(fromIndex, jsonParseContext.getEndIndex() + 1 - fromIndex);
                        } catch (Throwable throwable) {
                        }
                    }
                    String errorContextTextAt = JSONByteArrayParser.createErrorMessage(bytes, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + (char) beginByte + "' for CharSequence, expected '\"' ");
                }
            }
        }

        // String, StringBuffer or StringBuilder
        private CharSequence getCharSequence(CharSource charSource, char[] buf, int beginIndex, int len, int paramClassType) {
            if (paramClassType == ReflectConsts.CLASS_TYPE_STRING) {
                return (len < 1024 || charSource == null) ? new String(buf, beginIndex, len) : charSource.substring(beginIndex, beginIndex + len);
            }
            switch (paramClassType) {
                case ReflectConsts.CLASS_TYPE_STRING_BUFFER:
                    StringBuffer stringBuffer = new StringBuffer();
                    if (len > 0) {
                        stringBuffer.append(buf, beginIndex, len);
                    }
                    return stringBuffer;
                case ReflectConsts.CLASS_TYPE_STRING_BUILDER:
                    StringBuilder stringBuilder = new StringBuilder();
                    if (len > 0) {
                        stringBuilder.append(buf, beginIndex, len);
                    }
                    return stringBuilder;
                default:
                    // todo
                    return null;
            }
        }

        // String, StringBuffer or StringBuilder
        private CharSequence getCharSequence(JSONStringWriter writer, int paramClassType) {
            if (paramClassType == ReflectConsts.CLASS_TYPE_STRING) {
                return writer.toString();
            }
            switch (paramClassType) {
                case ReflectConsts.CLASS_TYPE_STRING_BUFFER:
                    return writer.toStringBuffer();
                case ReflectConsts.CLASS_TYPE_STRING_BUILDER:
                    return writer.toStringBuilder();
                default:
                    // todo
                    return null;
            }
        }
    }

    // 1、number
    static class NumberDeserializer extends JSONTypeDeserializer {

        protected NumberDeserializer() {
        }

        Number deserializeDefault0(char[] buf, int fromIndex, int toIndex, int offset, long val, int valueLen, boolean negative, char endToken, JSONParseContext jsonParseContext) throws Exception {
            int numberLen = valueLen;
            double value = val;
            int decimalCount = 0;
            final int radix = 10;
            int expValue = 0;
            boolean expNegative = false;
            // init integer type
            int mode = 0;
            // number suffix
            int specifySuffix = 0;

            int i = offset;
            char ch;

            // calculation
            while (/*i < toIndex*/true) {
                ch = buf[i];
                int digit = digitDecimal(ch);
                if (digit == -1) {
                    if (ch == ',' || ch == endToken) {
                        break;
                    }
                    if (ch == '.') {
                        if (mode != 0) {
                            throw new JSONException("For input string: \"" + new String(buf, fromIndex, i + 1 - fromIndex) + "\"");
                        }
                        // 小数点模式
                        mode = 1;
//                        if (++i < toIndex) {
//                            ch = buf[i];
//                        }
                        ch = buf[++i];
                        digit = digitDecimal(ch);
                    } else if (ch == 'E' || ch == 'e') {
                        if (mode == 2) {
                            throw new JSONException("For input string: \"" + new String(buf, fromIndex, i + 1 - fromIndex) + "\"");
                        }
                        // 科学计数法
                        mode = 2;
//                        if (++i < toIndex) {
//                            ch = buf[i];
//                        }
                        ch = buf[++i];
                        if (ch == '-') {
                            expNegative = true;
//                            if (++i < toIndex) {
//                                ch = buf[i];
//                            }
                            ch = buf[++i];
                        }
                        digit = digitDecimal(ch);
                    }
                }

                if (digit == -1) {
                    boolean breakLoop = false;
                    switch (ch) {
                        case 'l':
                        case 'L': {
                            if (specifySuffix == 0) {
                                specifySuffix = 1;
                                while ((ch = buf[++i]) <= ' ') ;
                                if (ch == ',' || ch == endToken) {
                                    breakLoop = true;
                                    break;
                                }
                            }
                            String contextErrorAt = createErrorContextText(buf, i);
                            throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + ch + "', context text by '" + contextErrorAt + "'");
                        }
                        case 'f':
                        case 'F': {
                            if (specifySuffix == 0) {
                                specifySuffix = 2;
                                while ((ch = buf[++i]) <= ' ') ;
                                if (ch == ',' || ch == endToken) {
                                    breakLoop = true;
                                    break;
                                }
                            }
                            String contextErrorAt = createErrorContextText(buf, i);
                            throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + ch + "', context text by '" + contextErrorAt + "'");
                        }
                        case 'd':
                        case 'D': {
                            if (specifySuffix == 0) {
                                specifySuffix = 3;
                                while ((ch = buf[++i]) <= ' ') ;
                                if (ch == ',' || ch == endToken) {
                                    breakLoop = true;
                                    break;
                                }
                            }
                            String contextErrorAt = createErrorContextText(buf, i);
                            throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + ch + "', context text by '" + contextErrorAt + "'");
                        }
                        default: {
                            if (ch <= ' ') {
                                while ((ch = buf[++i]) <= ' ') ;
                                if (ch == ',' || ch == endToken) {
                                    breakLoop = true;
                                    break;
                                }
                                String contextErrorAt = createErrorContextText(buf, i);
                                throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + ch + "', context text by '" + contextErrorAt + "'");
                            }
                        }
                    }
                    if (breakLoop) {
                        break;
                    }
                    String contextErrorAt = createErrorContextText(buf, i);
                    throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", at pos " + i + ", context text by '" + contextErrorAt + "'");
                }
                numberLen++;
                switch (mode) {
                    case 0:
                        value *= radix;
                        value += digit;
                        break;
                    case 1:
                        value *= radix;
                        value += digit;
                        decimalCount++;
                        break;
                    case 2:
                        expValue = (expValue << 3) + (expValue << 1);
                        expValue += digit;
                        break;
                }
                ++i;
            }

            // end
            jsonParseContext.setEndIndex(i - 1);

            // if check numberLen is zero ?

            // int / long / BigInteger
            if (mode == 0) {
                if (numberLen > 18) {
                    // BigInteger
                    // maybe Performance loss
                    int endIndex = i - 1;
                    while (buf[endIndex] <= ' ') {
                        endIndex--;
                    }
                    return new BigInteger(new String(buf, fromIndex, endIndex - fromIndex + 1));
                }
                value = negative ? -value : value;
                if (specifySuffix > 0) {
                    switch (specifySuffix) {
                        case 1:
                            return (long) value;
                        case 2:
                            return (float) value;
                    }
                    return value;
                }
                if (value <= Integer.MAX_VALUE && value > Integer.MIN_VALUE) {
                    return (int) value;
                }
                if (value <= Long.MAX_VALUE && value > Long.MIN_VALUE) {
                    return (long) value;
                }
                return value;
            } else {
                expValue = expNegative ? -expValue - decimalCount : expValue - decimalCount;
                if (expValue > 0) {
                    double powValue = getDecimalPowerValue(expValue); // Math.pow(radix, expValue);
                    value *= powValue;
                } else if (expValue < 0) {
                    double powValue = getDecimalPowerValue(-expValue);// Math.pow(radix, -expValue);
                    value /= powValue;
                }
                value = negative ? -value : value;
                if (specifySuffix > 0) {
                    switch (specifySuffix) {
                        case 1:
                            return (long) value;
                        case 2:
                            return (float) value;
                    }
                    return value;
                }
                return value;
            }
        }

        // Auto fit number type
        // Deserialize number until ',' and ${endToken} are encountered
        // Make sure it is not null
        Number deserializeDefault(char[] buf, int fromIndex, int toIndex, char endToken, JSONParseContext jsonParseContext) throws Exception {
            boolean negative = false;
            int i = fromIndex;
            char ch = buf[fromIndex];
            if (ch == '-') {
                // is negative
                negative = true;
                ++i;
            } else if (ch == '+') {
                ++i;
            }
            return deserializeDefault0(buf, fromIndex, toIndex, i, 0, 0, negative, endToken, jsonParseContext);
        }

        // BigDecimal or BigInteger or Double
        Object deserializeOfString(char[] buf, int fromIndex, int toIndex, char endToken, int numberType, JSONParseContext jsonParseContext) {
            int i = fromIndex;
            char ch;
            while (/*i + 1 < toIndex*/true) {
                ch = buf[i + 1];
                if (ch == ',' || ch == endToken) {
                    break;
                }
                ++i;
            }
            jsonParseContext.setEndIndex(i);

            int endIndex = i + 1;
            while ((endIndex > fromIndex) && (buf[endIndex - 1] <= ' ')) {
                endIndex--;
            }

            int len = endIndex - fromIndex;
            switch (numberType) {
                case ReflectConsts.CLASS_TYPE_NUMBER_BIGDECIMAL:
                    return new BigDecimal(buf, fromIndex, len);
                case ReflectConsts.CLASS_TYPE_NUMBER_BIG_INTEGER:
                    return new BigInteger(new String(buf, fromIndex, len));
                default:
                    return Double.parseDouble(new String(buf, fromIndex, len));
            }
        }

        // BigDecimal or BigInteger or Double
        Object deserializeOfString(CharSource charSource, byte[] bytes, int fromIndex, int toIndex, byte endToken, int numberType, JSONParseContext jsonParseContext) {
            int i = fromIndex;
            byte b;
            while (/*i + 1 < toIndex*/true) {
                b = bytes[i + 1];
                if (b == ',' || b == endToken) {
                    break;
                }
                ++i;
            }
            jsonParseContext.setEndIndex(i);

            int endIndex = i + 1;
            while ((endIndex > fromIndex) && (bytes[endIndex - 1] <= ' ')) {
                endIndex--;
            }

            int len = endIndex - fromIndex;
            String value = charSource == null ? new String(bytes, fromIndex, len) : charSource.getString(fromIndex, len);
            switch (numberType) {
                case ReflectConsts.CLASS_TYPE_NUMBER_BIGDECIMAL:
                    return new BigDecimal(value);
                case ReflectConsts.CLASS_TYPE_NUMBER_BIG_INTEGER:
                    return new BigInteger(value);
                default:
                    return Double.parseDouble(value);
            }
        }

        // If you encounter '.' Or special tags such as'e'('e') are converted to default parsing and converted to integer
        protected long deserializeInteger(char[] buf, char beginChar, int fromIndex, int toIndex, char endToken, JSONParseContext jsonParseContext) throws Exception {
            long value = 0;
            boolean negative = false;
            int digit;
            final int radix = 10;
            boolean empty = false;

            int i = fromIndex;
            char ch = beginChar;

            int v = ch - '0';
            if(v >= 0 && v <= 9) {
                value = v;
            } else {
                if(ch == '-') {
                    negative = true;
                } else if(ch != '+') {
                    String contextErrorAt = createErrorContextText(buf, i);
                    throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected '0123456789', but found '" + ch + "', context text by '" + contextErrorAt + "'");
                }
            }

//            if (ch == '-') {
//                // is negative
//                negative = true;
//            } else {
//                value = ch - '0';
//                if(value < 0 || value > 9) {
//                    if(ch != '+') {
//                        String contextErrorAt = createErrorContextText(buf, i);
//                        throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected '0123456789', but found '" + ch + "', context text by '" + contextErrorAt + "'");
//                    }
//                }
//            }
            ++i;

//            switch (ch) {
//                case '-': {
//                    negative = true;
//                    ++i;
//                    break;
//                }
//                case '+': {
//                    ++i;
//                    break;
//                }
//                case '0':
//                case '1':
//                case '2':
//                case '3':
//                case '4':
//                case '5':
//                case '6':
//                case '7':
//                case '8':
//                case '9': {
//                    value = ch - '0';
//                    ++i;
//                    break;
//                }
//                default: {
//                    String contextErrorAt = createErrorContextText(buf, i);
//                    throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected '0123456789', but found '" + ch + "', context text by '" + contextErrorAt + "'");
//                }
//            }

            while (/*i < toIndex*/true) {
                ch = buf[i];
                switch (ch) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': {
                        digit = ch - '0';
//                        empty = false;
                        value *= radix;
                        value += digit;
                        ++i;
                        continue;
                    }
                    default: {
                        if (ch == ',' || ch == endToken) {
                            return number(buf, i - 1, negative ? -value : value, empty, jsonParseContext);
                        }
                        if (ch <= ' ') {
                            while ((ch = buf[++i]) <= ' ') ;
                            if (ch == ',' || ch == endToken) {
                                return number(buf, i - 1, negative ? -value : value, empty, jsonParseContext);
                            }
                            String contextErrorAt = createErrorContextText(buf, i);
                            throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + ch + "', context text by '" + contextErrorAt + "'");
                        }
                        // forward default deserialize
                        return deserializeDefault0(buf, fromIndex, toIndex, i, value, 0, negative, endToken, jsonParseContext).longValue();
                    }
                }
            }
        }

        private long number(char[] buf, int endIndex, long value, boolean empty, JSONParseContext jsonParseContext) {
            if (empty) {
                String contextErrorAt = createErrorContextText(buf, endIndex + 1);
                throw new JSONException("For input empty number, at pos " + (endIndex + 1) + ", context text by '" + contextErrorAt + "'");
            }
            jsonParseContext.setEndIndex(endIndex);
            return value;
        }

        // Deserialize number until ',' and ${endToken} are encountered
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {

            if (parameterizedType == GenericParameterizedType.AnyType || parameterizedType == null) {
                return deserializeDefault(buf, fromIndex, toIndex, endToken, jsonParseContext);
            }

            char beginChar = buf[fromIndex];
            if (beginChar == 'n') {
                // maybe is null
                return NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
            }

            // BigDecimal and BigInteger only need to get the end position, and then build it through the construction method
            // Other unified transformation through double
            int numberType = parameterizedType.getParamClassNumberType();
            switch (numberType) {
                case ReflectConsts.CLASS_TYPE_NUMBER_BYTE:
                    return (byte) deserializeInteger(buf, beginChar, fromIndex, toIndex, endToken, jsonParseContext);
//                    return deserializeDefault(buf, fromIndex, toIndex, endToken, jsonParseContext).byteValue();
                case ReflectConsts.CLASS_TYPE_NUMBER_SHORT:
                    return (short) deserializeInteger(buf, beginChar, fromIndex, toIndex, endToken, jsonParseContext);
//                    return deserializeDefault(buf, fromIndex, toIndex, endToken, jsonParseContext).shortValue();
                case ReflectConsts.CLASS_TYPE_NUMBER_INTEGER:
                    return (int) deserializeInteger(buf, beginChar, fromIndex, toIndex, endToken, jsonParseContext);
//                    return deserializeDefault(buf, fromIndex, toIndex, endToken, jsonParseContext).intValue();
                case ReflectConsts.CLASS_TYPE_NUMBER_LONG:
                    return deserializeInteger(buf, beginChar, fromIndex, toIndex, endToken, jsonParseContext);
//                    return deserializeDefault(buf, fromIndex, toIndex, endToken, jsonParseContext).longValue();
                case ReflectConsts.CLASS_TYPE_NUMBER_FLOAT:
                    return deserializeDefault(buf, fromIndex, toIndex, endToken, jsonParseContext).floatValue();
                case ReflectConsts.CLASS_TYPE_NUMBER_DOUBLE:
                    if (jsonParseContext.isUseNativeDoubleParser()) {
                        return deserializeOfString(buf, fromIndex, toIndex, endToken, numberType, jsonParseContext);
                    }
                    return deserializeDefault(buf, fromIndex, toIndex, endToken, jsonParseContext).doubleValue();
                case ReflectConsts.CLASS_TYPE_NUMBER_ATOMIC_INTEGER: {
                    return new AtomicInteger((int)deserializeInteger(buf, beginChar, fromIndex, toIndex, endToken, jsonParseContext));
                }
                case ReflectConsts.CLASS_TYPE_NUMBER_ATOMIC_LONG: {
                    return new AtomicLong(deserializeInteger(buf, beginChar, fromIndex, toIndex, endToken, jsonParseContext));
                }
                case ReflectConsts.CLASS_TYPE_NUMBER_BIGDECIMAL:
                case ReflectConsts.CLASS_TYPE_NUMBER_BIG_INTEGER:
                    return deserializeOfString(buf, fromIndex, toIndex, endToken, numberType, jsonParseContext);
                default: {
                    // not supported
                    deserializeDefault(buf, fromIndex, toIndex, endToken, jsonParseContext);
                    // throw new UnsupportedOperationException("Unsupported number type of " + parameterizedType.getActualType());
                    return null;
                }
            }
        }

        // Deserialize number until ',' and ${endToken} are encountered
        protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {

            if (parameterizedType == GenericParameterizedType.AnyType || parameterizedType == null) {
                return JSONByteArrayParser.parseDefaultNumber(bytes, fromIndex, toIndex, endToken, jsonParseContext);
            }

            byte beginByte = bytes[fromIndex];
            if (beginByte == 'n') {
                // maybe is null
                return JSONByteArrayParser.parseNull(bytes, fromIndex, toIndex, jsonParseContext);
            }

            // BigDecimal and BigInteger only need to get the end position, and then build it through the construction method
            // Other unified transformation through double
            int numberType = parameterizedType.getParamClassNumberType();
            switch (numberType) {
                case ReflectConsts.CLASS_TYPE_NUMBER_BYTE:
                    return JSONByteArrayParser.parseDefaultNumber(bytes, fromIndex, toIndex, endToken, jsonParseContext).byteValue();
                case ReflectConsts.CLASS_TYPE_NUMBER_SHORT:
                    return JSONByteArrayParser.parseDefaultNumber(bytes, fromIndex, toIndex, endToken, jsonParseContext).shortValue();
                case ReflectConsts.CLASS_TYPE_NUMBER_INTEGER:
                    return JSONByteArrayParser.parseDefaultNumber(bytes, fromIndex, toIndex, endToken, jsonParseContext).intValue();
                case ReflectConsts.CLASS_TYPE_NUMBER_LONG:
                    return JSONByteArrayParser.parseDefaultNumber(bytes, fromIndex, toIndex, endToken, jsonParseContext).longValue();
                case ReflectConsts.CLASS_TYPE_NUMBER_FLOAT:
                    return JSONByteArrayParser.parseDefaultNumber(bytes, fromIndex, toIndex, endToken, jsonParseContext).floatValue();
                case ReflectConsts.CLASS_TYPE_NUMBER_DOUBLE:
                    if (jsonParseContext.isUseNativeDoubleParser()) {
                        return deserializeOfString(charSource, bytes, fromIndex, toIndex, endToken, numberType, jsonParseContext);
                    }
                    return JSONByteArrayParser.parseDefaultNumber(bytes, fromIndex, toIndex, endToken, jsonParseContext).doubleValue();
                case ReflectConsts.CLASS_TYPE_NUMBER_ATOMIC_INTEGER: {
                    return new AtomicInteger(JSONByteArrayParser.parseDefaultNumber(bytes, fromIndex, toIndex, endToken, jsonParseContext).intValue());
                }
                case ReflectConsts.CLASS_TYPE_NUMBER_ATOMIC_LONG: {
                    return new AtomicLong(JSONByteArrayParser.parseDefaultNumber(bytes, fromIndex, toIndex, endToken, jsonParseContext).longValue());
                }
                case ReflectConsts.CLASS_TYPE_NUMBER_BIGDECIMAL:
                case ReflectConsts.CLASS_TYPE_NUMBER_BIG_INTEGER:
                    return deserializeOfString(charSource, bytes, fromIndex, toIndex, endToken, numberType, jsonParseContext);
                default: {
                    // not supported
                    JSONByteArrayParser.parseDefaultNumber(bytes, fromIndex, toIndex, endToken, jsonParseContext);
                    // throw new UnsupportedOperationException("Unsupported number type of " + parameterizedType.getActualType());
                    return null;
                }
            }
        }

        static Object parseNumberValue(char[] buf, int fromIndex, int endIndex, int numberType) {

            char beginChar = buf[fromIndex];
            char endChar = '\0';

            // 此处在反序列化缩进美化的json时如果位于最后一个字段会存在部分性能损失
            while ((endIndex > fromIndex) && ((endChar = buf[endIndex - 1]) <= ' ')) {
                endIndex--;
            }

            int len = endIndex - fromIndex;
            if (beginChar == 'n' &&
                    len == 4 &&
                    buf[fromIndex + 1] == 'u' &&
                    buf[fromIndex + 2] == 'l' &&
                    endChar == 'l') {
                return null;
            }
            switch (numberType) {
                case ReflectConsts.CLASS_TYPE_NUMBER_INTEGER:
                    return Integer.parseInt(new String(buf, fromIndex, len));
                case ReflectConsts.CLASS_TYPE_NUMBER_FLOAT:
                    return Float.parseFloat(new String(buf, fromIndex, len));
                case ReflectConsts.CLASS_TYPE_NUMBER_LONG:
                    return Long.parseLong(new String(buf, fromIndex, len));
                case ReflectConsts.CLASS_TYPE_NUMBER_DOUBLE:
                    return Double.parseDouble(new String(buf, fromIndex, len));
                case ReflectConsts.CLASS_TYPE_NUMBER_BIGDECIMAL:
                    return new BigDecimal(buf, fromIndex, len);
                case ReflectConsts.CLASS_TYPE_NUMBER_BYTE:
                    return Byte.parseByte(new String(buf, fromIndex, len));
                case ReflectConsts.CLASS_TYPE_NUMBER_ATOMIC_INTEGER: {
                    return new AtomicInteger(Integer.parseInt(new String(buf, fromIndex, len)));
                }
                case ReflectConsts.CLASS_TYPE_NUMBER_ATOMIC_LONG: {
                    return new AtomicLong(Long.parseLong(new String(buf, fromIndex, len)));
                }
                case ReflectConsts.CLASS_TYPE_NUMBER_BIG_INTEGER: {
                    return new BigInteger(new String(buf, fromIndex, len));
                }
            }
            return null;
        }

        static class IntegerDeserializer extends NumberDeserializer {
            protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
                char beginChar = buf[fromIndex];
                if (beginChar == 'n') {
                    NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
                    if (parameterizedType.getActualType() == int.class) {
                        return 0;
                    }
                    return null;
                }
                return (int) deserializeInteger(buf, beginChar, fromIndex, toIndex, endToken, jsonParseContext);
            }
        }

        static class LongDeserializer extends NumberDeserializer {
            protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
                char beginChar = buf[fromIndex];
                if (beginChar == 'n') {
                    NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
                    if (parameterizedType.getActualType() == long.class) {
                        return 0L;
                    }
                    return null;
                }
                return deserializeInteger(buf, beginChar, fromIndex, toIndex, endToken, jsonParseContext);
            }
        }
    }

    // 2、boolean
    static class BooleanDeserializer extends JSONTypeDeserializer {

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case 't':
                    return deserializeTrue(buf, fromIndex, toIndex, parameterizedType, jsonParseContext);
                case 'f':
                    return deserializeFalse(buf, fromIndex, toIndex, parameterizedType, jsonParseContext);
            }
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + beginChar + "' for Boolean Type, expected 't' or 'f'");
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            switch (beginByte) {
                case 't':
                    return JSONByteArrayParser.parseTrue(buf, fromIndex, toIndex, jsonParseContext);
                case 'f':
                    return JSONByteArrayParser.parseFalse(buf, fromIndex, toIndex, jsonParseContext);
            }
            String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + (char) beginByte + "' for Boolean Type, expected 't' or 'f'");
        }

        Object deserializeTrue(char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) throws Exception {
            int endIndex = fromIndex + 3;
            // True only needs to read 4 characters to match true, and there is no need to read the comma or} position
//            if (endIndex < toIndex) {
//            }
            if (buf[fromIndex + 1] == 'r' && buf[fromIndex + 2] == 'u' && buf[endIndex] == 'e') {
                jsonParseContext.setEndIndex(endIndex);
                return true;
            }
            int len = Math.min(toIndex - fromIndex + 1, 4);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'true' because it starts with 't', but found text '" + new String(buf, fromIndex, len) + "'");
        }

        Object deserializeFalse(char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) throws Exception {
            int endIndex = fromIndex + 4;
            // False you only need to read 5 characters to match false, and you do not need to read the comma or} position
//            if (endIndex < toIndex) {
//            }
            if (buf[fromIndex + 1] == 'a'
                    && buf[fromIndex + 2] == 'l'
                    && buf[fromIndex + 3] == 's'
                    && buf[endIndex] == 'e') {
                jsonParseContext.setEndIndex(endIndex);
                return false;
            }
            int len = Math.min(toIndex - fromIndex + 1, 5);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'false' because it starts with 'f', but found text '" + new String(buf, fromIndex, len) + "'");
        }
    }

    // 3、日期
    static class DateDeserializer extends JSONTypeDeserializer {

        protected String pattern;
        protected int patternType;
        protected DateTemplate dateTemplate;
        protected String timezone;

        // 通过字符数组解析
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '"': {
                    STRING.skip(charSource, buf, fromIndex, toIndex, jsonParseContext);
                    int endStringIndex = jsonParseContext.getEndIndex();
                    Class<? extends Date> dateCls = (Class<? extends Date>) parameterizedType.getActualType();
                    return parseDateValueOfString(buf, fromIndex, endStringIndex + 1, pattern, patternType, dateTemplate, timezone, dateCls);
                }
                case 'n': {
                    return NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
                }
                case '{': {
                    // {\"time\": 123333443}
                    return OBJECT.deserializeObject(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
                }
                default: {
                    // long
                    long timestamp = (Long) NUMBER.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.LongType, null, endToken, jsonParseContext);
                    return parseDate(timestamp, (Class<? extends Date>) parameterizedType.getActualType());
                }
            }
        }

        // 通过字节数组解析
        protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = bytes[fromIndex];
            switch (beginByte) {
                case '"': {
                    STRING.skip(charSource, bytes, fromIndex, toIndex, jsonParseContext);
                    int endStringIndex = jsonParseContext.getEndIndex();
                    Class<? extends Date> dateCls = (Class<? extends Date>) parameterizedType.getActualType();
                    return parseDateValueOfString(bytes, fromIndex, endStringIndex + 1, pattern, patternType, dateTemplate, timezone, dateCls);
                }
                case 'n': {
                    return JSONByteArrayParser.parseNull(bytes, fromIndex, toIndex, jsonParseContext);
                }
                case '{': {
                    // {\"time\": 123333443}
                    return OBJECT.deserializeObject(charSource, bytes, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
                }
                default: {
                    // long
                    long timestamp = (Long) NUMBER.deserialize(charSource, bytes, fromIndex, toIndex, GenericParameterizedType.LongType, null, endToken, jsonParseContext);
                    return parseDate(timestamp, (Class<? extends Date>) parameterizedType.getActualType());
                }
            }
        }

        static class DateInstanceDeserializer extends DateDeserializer {

            public DateInstanceDeserializer(GenericParameterizedType genericParameterizedType) {
                genericParameterizedType.getClass();
                timezone = genericParameterizedType.getDateTimezone();
                pattern = genericParameterizedType.getDatePattern();
                patternType = getPatternType(pattern);
                if (patternType == 4) {
                    dateTemplate = new DateTemplate(pattern);
                }
            }
        }
    }

    // 4、枚举
    static class EnumDeserializer extends JSONTypeDeserializer {

        protected Enum deserializeEnumName(CharSource charSource, char[] buf, int fromIndex, int toIndex, Class enumCls, JSONParseContext jsonParseContext) throws Exception {
            String name = (String) STRING.deserializeString(charSource, buf, fromIndex, toIndex, GenericParameterizedType.StringType, jsonParseContext);
            try {
                return Enum.valueOf(enumCls, name);
            } catch (RuntimeException exception) {
                if (jsonParseContext.isUnknownEnumAsNull()) {
                    return null;
                } else {
                    throw exception;
                }
            }
        }

        protected Enum deserializeEnumName(CharSource charSource, byte[] buf, int fromIndex, int toIndex, Class enumCls, byte end, JSONParseContext jsonParseContext) throws Exception {
            String name = JSONByteArrayParser.parseJSONString(charSource == null ? null : charSource.input(), buf, fromIndex, toIndex, end, jsonParseContext);
            try {
                return Enum.valueOf(enumCls, name);
            } catch (RuntimeException exception) {
                if (jsonParseContext.isUnknownEnumAsNull()) {
                    return null;
                } else {
                    throw exception;
                }
            }
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            Class clazz = parameterizedType.getActualType();

            if(beginChar == '"') {
                return deserializeEnumName(charSource, buf, fromIndex, toIndex, clazz, jsonParseContext);
            } else if(beginChar == 'n') {
                return NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
            } else {
                // number
                Integer ordinal = (Integer) NUMBER.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.IntType, null, endToken, jsonParseContext);
                Enum[] values = (Enum[]) clazz.getEnumConstants();
                if (values != null && ordinal < values.length)
                    return values[ordinal];
                throw new JSONException("Syntax error, at pos " + fromIndex + ", ordinal " + ordinal + " cannot convert to Enum " + clazz + "");
            }

//            switch (beginChar) {
//                case 'n': {
//                    return NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
//                }
//                case '"': {
//                    return deserializeEnumName(charSource, buf, fromIndex, toIndex, clazz, jsonParseContext);
//                }
//                default: {
//                    // number
//                    Integer ordinal = (Integer) NUMBER.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.IntType, null, endToken, jsonParseContext);
//                    Enum[] values = (Enum[]) clazz.getEnumConstants();
//                    if (values != null && ordinal < values.length)
//                        return values[ordinal];
//                    throw new JSONException("Syntax error, at pos " + fromIndex + ", ordinal " + ordinal + " cannot convert to Enum " + clazz + "");
//                }
//            }
        }

        protected Object getEnumConstants(Class clazz) {
            return clazz.getEnumConstants();
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            char beginChar = (char) beginByte;
            Class clazz = parameterizedType.getActualType();
            switch (beginChar) {
                case 'n': {
                    return JSONByteArrayParser.parseNull(buf, fromIndex, toIndex, jsonParseContext);
                }
                case '"': {
                    return deserializeEnumName(charSource, buf, fromIndex, toIndex, clazz, beginByte, jsonParseContext);
                }
                default: {
                    // number
                    int ordinal = JSONByteArrayParser.parseDefaultNumber(buf, fromIndex, toIndex, endToken, jsonParseContext).intValue();
                    Enum[] values = (Enum[]) getEnumConstants(clazz);
                    if (values != null && ordinal < values.length)
                        return values[ordinal];
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", ordinal " + ordinal + " cannot convert to Enum " + clazz + "");
                }
            }
        }

        static class EnumInstanceDeserializer extends EnumDeserializer {

            private final Enum[] values;
            private final FixedNameValueMap<Enum> fixedNameValueMap;
            private final boolean collision;

            protected EnumInstanceDeserializer(Class enumType) {
                values = (Enum[]) enumType.getEnumConstants();
                fixedNameValueMap = new FixedNameValueMap(values.length);

                Set<Integer> hashValueSet = new HashSet<Integer>();
                boolean collision = false;
                for (Enum value : values) {
                    fixedNameValueMap.putValue(value.name(), value);
                    if (!hashValueSet.add(value.name().hashCode())) {
                        collision = true;
                    }
                }
                this.collision = collision;
                hashValueSet.clear();
            }

            @Override
            protected Object getEnumConstants(Class clazz) {
                return values;
            }

            @Override
            protected Enum deserializeEnumName(CharSource charSource, char[] buf, int fromIndex, int toIndex, Class enumCls, JSONParseContext jsonParseContext) throws Exception {
                int i = fromIndex;
                int hashValue = 0;
                char ch, prev = 0;
                while (/*i + 1 < toIndex &&*/ ((ch = buf[++i]) != '"' || prev == '\\')) {
                    hashValue = hashValue * 31 + ch;
                    prev = ch;
                }
//                if (ch != '"') {
//                    throw new JSONException("Syntax error, from pos " + fromIndex + ", the closing symbol '\"' is not found ");
//                }
                jsonParseContext.setEndIndex(i);

                Enum value;
                if (collision) {
                    value = fixedNameValueMap.getValue(buf, fromIndex + 1, i, hashValue);
                } else {
                    value = fixedNameValueMap.getValueByHash(hashValue);
                }
                if (value == null) {
                    if (jsonParseContext.isUnknownEnumAsNull()) {
                        return null;
                    }
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unknown Enum name '" + new String(buf, fromIndex + 1, i - fromIndex - 1) + "' of EnumType " + enumCls);
                }
                return value;
            }

            @Override
            protected Enum deserializeEnumName(CharSource charSource, byte[] buf, int fromIndex, int toIndex, Class enumCls, byte end, JSONParseContext jsonParseContext) throws Exception {
                int i = fromIndex;
                int hashValue = 0;
                byte b = '\0';
                while (/*i + 1 < toIndex &&*/ ((b = buf[++i]) != '"' || buf[i - 1] == '\\')) {
                    hashValue = hashValue * 31 + b;
                }
//                if (ch != '"') {
//                    throw new JSONException("Syntax error, from pos " + fromIndex + ", the closing symbol '\"' is not found ");
//                }
                jsonParseContext.setEndIndex(i);

                Enum value;
                if (collision) {
                    value = fixedNameValueMap.getValue(buf, fromIndex + 1, i, hashValue);
                } else {
                    value = fixedNameValueMap.getValueByHash(hashValue);
                }
                if (value == null) {
                    if (jsonParseContext.isUnknownEnumAsNull()) {
                        return null;
                    }
                    String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unknown Enum name '" + new String(buf, fromIndex + 1, i - fromIndex - 1) + "' of EnumType " + enumCls);
                }
                return value;
            }
        }
    }

    // 4、Class
    static class ClassDeserializer extends JSONTypeDeserializer {
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '"': {
                    String name = (String) STRING.deserializeString(charSource, buf, fromIndex, toIndex, GenericParameterizedType.StringType, jsonParseContext);
                    return Class.forName(name);
                }
                case 'n': {
                    return NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
                }
                default: {
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + beginChar + "' for Class Type, expected '\"' ");
                }
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            switch (beginByte) {
                case '"': {
                    String name = JSONByteArrayParser.parseJSONString(charSource == null ? null : charSource.input(), buf, fromIndex, toIndex, beginByte, jsonParseContext);
                    return Class.forName(name);
                }
                case 'n': {
                    return JSONByteArrayParser.parseNull(buf, fromIndex, toIndex, jsonParseContext);
                }
                default: {
                    String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + (char) beginByte + "' for Class Type, expected '\"' ");
                }
            }
        }
    }

    // 6、Annotation
    static class AnnotationDeserializer extends JSONTypeDeserializer {
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
            // not support
            ANY.skip(charSource, buf, fromIndex, toIndex, endToken, jsonParseContext);
            return null;
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            // not support
            ANY.skip(charSource, buf, fromIndex, toIndex, endToken, jsonParseContext);
            return null;
        }
    }

    // 7、byte[]
    static class BinaryDeserializer extends JSONTypeDeserializer {


        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {

            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case 'n': {
                    return NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
                }
                case '"': {
                    // String
                    STRING.skip(charSource, buf, fromIndex, toIndex, jsonParseContext);
                    int endStringIndex = jsonParseContext.getEndIndex();
                    byte[] bytes = parseBytesOfBuf0(fromIndex, endStringIndex - fromIndex - 1, buf, jsonParseContext);
                    return bytes;
                }
                case '[': {
                    // 转为ARRAY解析处理
                    Byte[] target = (Byte[]) ARRAY.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.arrayType(Byte.class), null, '\0', jsonParseContext);
                    byte[] bytes = new byte[target.length];
                    for (int i = 0, len = bytes.length; i < len; ++i) {
                        bytes[i] = target[i];
                    }
                    return bytes;
                }
                default: {
                    // not support
                    throw new JSONException("Syntax error, from pos " + fromIndex + ", the beginChar '" + beginChar + "' mismatch type byte[] or Byte[] ");
                }
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            char beginChar = (char) beginByte;
            switch (beginChar) {
                case 'n': {
                    return JSONByteArrayParser.parseNull(buf, fromIndex, toIndex, jsonParseContext);
                }
                case '"': {
                    // String
                    STRING.skip(charSource, buf, fromIndex, toIndex, jsonParseContext);
                    int endStringIndex = jsonParseContext.getEndIndex();
                    byte[] bytes = parseBytesOfBuf0(fromIndex, endStringIndex - fromIndex - 1, buf, jsonParseContext);
                    return bytes;
                }
                case '[': {
                    // 转为ARRAY解析处理
                    Byte[] target = (Byte[]) ARRAY.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.arrayType(Byte.class), null, endToken, jsonParseContext);
                    byte[] bytes = new byte[target.length];
                    for (int i = 0, len = bytes.length; i < len; ++i) {
                        bytes[i] = target[i];
                    }
                    return bytes;
                }
                default: {
                    // not support
                    throw new JSONException("Syntax error, from pos " + fromIndex + ", the beginChar '" + beginChar + "' mismatch type byte[] or Byte[] ");
                }
            }
        }

        private static byte[] parseBytesOfBuf0(int fromIndex, int len, char[] buf, JSONParseContext jsonParseContext) {
            if (jsonParseContext.isByteArrayFromHexString()) {
                return hexString2Bytes(buf, fromIndex + 1, len);
            } else {
                byte[] bytes = new byte[len];
                int offset = fromIndex + 1;
                for (int i = 0; i < len; ++i) {
                    bytes[i] = (byte) buf[offset + i];
                }
                return io.github.wycst.wast.common.tools.Base64.getDecoder().decode(bytes);
            }
        }

        private static byte[] parseBytesOfBuf0(int fromIndex, int len, byte[] buf, JSONParseContext jsonParseContext) {
            if (jsonParseContext.isByteArrayFromHexString()) {
                return hexString2Bytes(buf, fromIndex + 1, len);
            } else {
                int offset = fromIndex + 1;
                byte[] bytes = Arrays.copyOfRange(buf, offset, offset + len);
                return io.github.wycst.wast.common.tools.Base64.getDecoder().decode(bytes);
            }
        }
    }

    // 8、数组
    static class ArrayDeserializer extends JSONTypeDeserializer {

        Object deserializeArray(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            char ch = '\0';

            Class arrayCls = parameterizedType.getActualType();
            GenericParameterizedType valueType = parameterizedType.getValueType();

            Class<?> elementCls = null;
            if (valueType == null) {
                elementCls = arrayCls.getComponentType();
                valueType = GenericParameterizedType.actualType(elementCls);
            } else {
                elementCls = valueType.getActualType();
            }
            ReflectConsts.ClassCategory actualClassCategory = valueType.getActualClassCategory();

            // No random access is required, and it is best to use LinkedList or ConcurrentLinkedQueue to convert it into an array ??
            Collection<Object> collection = new ArrayList<Object>();

            // 允许注释
            boolean allowComment = jsonParseContext.isAllowComment();

            // 集合数组核心token是逗号（The core token of the collection array is a comma）
            // for loop
            for (int i = beginIndex; i < toIndex; ++i) {
                // clear white space characters
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // 如果提前遇到字符']'，说明是空集合
                //  （If the character ']' is encountered in advance, it indicates that it is an empty set）
                // 另一种可能性(语法错误): 非空集合，发生在空逗号后接结束字符']'直接抛出异常
                //  (Another possibility(Syntax error): whether the empty comma followed by the closing character ']' throws an exception)
                if (ch == ']') {
                    if (collection.size() > 0) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    jsonParseContext.setEndIndex(i);
                    return collectionToArray(collection, elementCls);
                }

                int ordinal = actualClassCategory.ordinal();
                Object value = JSONTypeDeserializer.TYPE_DESERIALIZERS[ordinal].deserialize(charSource, buf, i, toIndex, valueType, null, ']', jsonParseContext);
                collection.add(value);
                i = jsonParseContext.getEndIndex();

                // 清除空白字符（clear white space characters）
                while ((ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // 检查下一个字符是逗号还是结束符号。如果是，继续或者终止。如果不是，则抛出异常
                // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
                if (ch == ',') {
                    continue;
                }
                if (ch == ']') {
                    jsonParseContext.setEndIndex(i);
                    return collectionToArray(collection, elementCls);
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or ']'");
            }
            throw new JSONException("Syntax error, cannot find closing symbol ']' matching '['");
        }

        Object deserializeArray(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            byte b = '\0';

            Class arrayCls = parameterizedType.getActualType();
            GenericParameterizedType valueType = parameterizedType.getValueType();

            Class<?> elementCls = null;
            if (valueType == null) {
                elementCls = arrayCls.getComponentType();
                valueType = GenericParameterizedType.actualType(elementCls);
            } else {
                elementCls = valueType.getActualType();
            }
            ReflectConsts.ClassCategory actualClassCategory = valueType.getActualClassCategory();

            // No random access is required, and it is best to use LinkedList or ConcurrentLinkedQueue to convert it into an array ??
            Collection<Object> collection = new ArrayList<Object>();

            // 允许注释
            boolean allowComment = jsonParseContext.isAllowComment();

            // 集合数组核心token是逗号（The core token of the collection array is a comma）
            // for loop
            for (int i = beginIndex; i < toIndex; ++i) {
                // clear white space characters
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // 如果提前遇到字符']'，说明是空集合
                //  （If the character ']' is encountered in advance, it indicates that it is an empty set）
                // 另一种可能性(语法错误): 非空集合，发生在空逗号后接结束字符']'直接抛出异常
                //  (Another possibility(Syntax error): whether the empty comma followed by the closing character ']' throws an exception)
                if (b == ']') {
                    if (collection.size() > 0) {
                        String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    jsonParseContext.setEndIndex(i);
                    return collectionToArray(collection, elementCls);
                }

                int ordinal = actualClassCategory.ordinal();
                Object value = JSONTypeDeserializer.TYPE_DESERIALIZERS[ordinal].deserialize(charSource, buf, i, toIndex, valueType, null, EndArray, jsonParseContext);
                collection.add(value);
                i = jsonParseContext.getEndIndex();

                // 清除空白字符（clear white space characters）
                while ((b = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // 检查下一个字符是逗号还是结束符号。如果是，继续或者终止。如果不是，则抛出异常
                // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
                if (b == ',') {
                    continue;
                }
                if (b == ']') {
                    jsonParseContext.setEndIndex(i);
                    return collectionToArray(collection, elementCls);
                }
                String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', expected ',' or ']'");
            }
            throw new JSONException("Syntax error, cannot find closing symbol ']' matching '['");
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '[':
                    return deserializeArray(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
                case 'n':
                    return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, '\0', jsonParseContext);
                default: {
                    // not support or custom handle ?
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + beginChar + "' for Collection Type ");
                }
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            char beginChar = (char) beginByte;
            switch (beginChar) {
                case '[':
                    return deserializeArray(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
                case 'n':
                    return JSONByteArrayParser.parseNull(buf, fromIndex, toIndex, jsonParseContext);
                default: {
                    // not support or custom handle ?
                    String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + beginChar + "' for Collection Type ");
                }
            }
        }
    }

    // 9、集合
    static class CollectionDeserializer extends JSONTypeDeserializer {

        void skip(CharSource charSource, char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            char ch;
            int size = 0;
            boolean allowComment = jsonParseContext.isAllowComment();
            for (int i = beginIndex; i < toIndex; ++i) {
                // clear white space characters
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // 如果提前遇到字符']'，说明是空集合
                if (ch == ']') {
                    if (size > 0) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    jsonParseContext.setEndIndex(i);
                    return;
                }
                ++size;
                ANY.skip(charSource, buf, i, toIndex, ']', jsonParseContext);
                i = jsonParseContext.getEndIndex();

                // 清除空白字符（clear white space characters）
                while ((ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // 检查下一个字符是逗号还是结束符号。如果是，继续或者终止。如果不是，则抛出异常
                if (ch == ',') {
                    continue;
                }
                if (ch == ']') {
                    jsonParseContext.setEndIndex(i);
                    return;
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or ']'");
            }
            throw new JSONException("Syntax error, cannot find closing symbol ']' matching '['");
        }

        void skip(CharSource charSource, byte[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            byte b;
            int size = 0;
            boolean allowComment = jsonParseContext.isAllowComment();
            for (int i = beginIndex; i < toIndex; ++i) {
                // clear white space characters
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // 如果提前遇到字符']'，说明是空集合
                if (b == ']') {
                    if (size > 0) {
                        String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    jsonParseContext.setEndIndex(i);
                    return;
                }
                ++size;
                ANY.skip(charSource, buf, i, toIndex, EndArray, jsonParseContext);
                i = jsonParseContext.getEndIndex();

                // 清除空白字符（clear white space characters）
                while ((b = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // 检查下一个字符是逗号还是结束符号。如果是，继续或者终止。如果不是，则抛出异常
                if (b == ',') {
                    continue;
                }
                if (b == ']') {
                    jsonParseContext.setEndIndex(i);
                    return;
                }
                String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', expected ',' or ']'");
            }
            throw new JSONException("Syntax error, cannot find closing symbol ']' matching '['");
        }

        protected Collection createCollection(GenericParameterizedType parameterizedType) throws Exception {
            Class collectionCls = parameterizedType.getActualType();
            if (collectionCls == null || collectionCls == List.class || collectionCls == ArrayList.class) {
                return new ArrayList();
            } else {
                return createCollectionInstance(collectionCls);
            }
        }

        protected JSONTypeDeserializer getValueDeserializer(GenericParameterizedType valueGenType) {
            return JSONTypeDeserializer.getTypeDeserializer(valueGenType.getActualType());
        }

        Collection deserializeCollection(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {

            Collection<Object> collection;
            if (instance != null) {
                collection = (Collection) instance;
            } else {
                collection = createCollection(parameterizedType);
            }

            GenericParameterizedType valueGenType = parameterizedType.getValueType();
            if (valueGenType == null) {
                valueGenType = GenericParameterizedType.AnyType;
            }

            // value Deserializer
            JSONTypeDeserializer valueDeserializer = getValueDeserializer(valueGenType);

            // 允许注释
            boolean allowComment = jsonParseContext.isAllowComment();

            // begin read
            int beginIndex = fromIndex + 1;
            char ch = '\0';

            for (int i = beginIndex; /*i < toIndex*/ ; ++i) {
                // clear white space characters
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // 如果提前遇到字符']'，说明是空集合
                //  （If the character ']' is encountered in advance, it indicates that it is an empty set）
                // 另一种可能性(语法错误): 非空集合，发生在空逗号后接结束字符']'直接抛出异常
                //  (Another possibility(Syntax error): whether the empty comma followed by the closing character ']' throws an exception)
                if (ch == ']') {
                    if (collection.size() > 0) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    jsonParseContext.setEndIndex(i);
                    return collection;
                }

                Object value = valueDeserializer.deserialize(charSource, buf, i, toIndex, valueGenType, null, ']', jsonParseContext);
                collection.add(value);
                i = jsonParseContext.getEndIndex();

                // clear white space characters
                while ((ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
                if (ch == ',') {
                    continue;
                }
                if (ch == ']') {
                    jsonParseContext.setEndIndex(i);
                    return collection;
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or ']'");
            }
//            throw new JSONException("Syntax error, cannot find closing symbol ']' matching '['");
        }

        Collection deserializeCollection(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {

            Collection<Object> collection;
            if (instance != null) {
                collection = (Collection) instance;
            } else {
                collection = createCollection(parameterizedType);
            }

            GenericParameterizedType valueGenType = parameterizedType.getValueType();
            if (valueGenType == null) {
                valueGenType = GenericParameterizedType.AnyType;
            }

            // value Deserializer
            JSONTypeDeserializer valueDeserializer = getValueDeserializer(valueGenType);

            // 允许注释
            boolean allowComment = jsonParseContext.isAllowComment();

            // begin read
            int beginIndex = fromIndex + 1;
            byte b = '\0';

            for (int i = beginIndex; /*i < toIndex*/ ; ++i) {
                // clear white space characters
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                if (b == ']') {
                    if (collection.size() > 0) {
                        String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    jsonParseContext.setEndIndex(i);
                    return collection;
                }

                Object value = valueDeserializer.deserialize(charSource, buf, i, toIndex, valueGenType, null, EndArray, jsonParseContext);
                collection.add(value);
                i = jsonParseContext.getEndIndex();

                // clear white space characters
                while ((b = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
                if (b == ',') {
                    continue;
                }
                if (b == ']') {
                    jsonParseContext.setEndIndex(i);
                    return collection;
                }
                String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', expected ',' or ']'");
            }
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];

            if(beginChar == '[') {
                return deserializeCollection(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
            } else if(beginChar == 'n') {
                return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
            } else {
                // not support or custom handle ?
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', expected token character '[', but found '" + beginChar + "' for Collection Type ");
            }

//            switch (beginChar) {
//                case '[':
//                    return deserializeCollection(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
//                case 'n':
//                    return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
//                default: {
//                    // not support or custom handle ?
//                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
//                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', expected token character '[', but found '" + beginChar + "' for Collection Type ");
//                }
//            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            char beginChar = (char) beginByte;
            switch (beginChar) {
                case '[':
                    return deserializeCollection(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
                case 'n':
                    return JSONByteArrayParser.parseNull(buf, fromIndex, toIndex, jsonParseContext);
                default: {
                    // not support or custom handle ?
                    String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', expected token character '[', but found '" + beginChar + "' for Collection Type ");
                }
            }
        }

        static class CollectionInstanceDeserializer extends CollectionDeserializer {

            private final GenericParameterizedType valueType;
            private final JSONTypeDeserializer valueDeserializer;
            private final boolean useArrayList;
            private final boolean useHashSet;
            private final Class<? extends Collection> constructionClass;

            CollectionInstanceDeserializer(GenericParameterizedType genericParameterizedType) {
                this.valueType = genericParameterizedType.getValueType();
                this.valueDeserializer = valueType == null ? ANY : getTypeDeserializer(valueType.getActualType());

                Class<?> actualType = genericParameterizedType.getActualType();
                boolean useArrayList = false;
                boolean useHashSet = false;
                Class<? extends Collection> constructionClass = null;
                if (actualType == List.class || actualType == ArrayList.class || actualType.isAssignableFrom(ArrayList.class)) {
                    useArrayList = true;
                } else if (actualType == Set.class || actualType == HashSet.class || actualType.isAssignableFrom(HashSet.class)) {
                    useHashSet = true;
                } else {
                    if (actualType.isInterface() || Modifier.isAbstract(actualType.getModifiers())) {
                        throw new UnsupportedOperationException("Unsupported for collection type '" + actualType + "', Please specify an implementation class");
                    } else {
                        constructionClass = (Class<? extends Collection>) actualType;
                    }
                }
                this.useArrayList = useArrayList;
                this.useHashSet = useHashSet;
                this.constructionClass = constructionClass;
            }

            @Override
            protected Collection createCollection(GenericParameterizedType parameterizedType) throws Exception {
                if (useArrayList) {
                    return new ArrayList();
                }
                if (useHashSet) {
                    return new HashSet();
                }
                return constructionClass.newInstance();
            }

            @Override
            protected JSONTypeDeserializer getValueDeserializer(GenericParameterizedType valueGenType) {
                return valueDeserializer;
            }
        }

    }

    // 10、Map
    static class MapDeserializer extends JSONTypeDeserializer {

        void skip(CharSource charSource, char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
            boolean empty = true;
            char ch;
            for (int i = fromIndex + 1; i < toIndex; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (jsonParseContext.isAllowComment()) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (ch == '"') {
                    while (i + 1 < toIndex && ((ch = buf[++i]) != '"' || buf[i - 1] == '\\')) ;
                    empty = false;
                    if (ch != '"') {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, util pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '\"' is not found ");
                    }
                    ++i;
                } else {
                    if (ch == '}') {
                        if (!empty) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                        }
                        jsonParseContext.setEndIndex(i);
                        return;
                    }
                    if (ch == '\'') {
                        if (jsonParseContext.isAllowSingleQuotes()) {
                            while (i + 1 < toIndex && buf[++i] != '\'') ;
                            empty = false;
                            ++i;
                        } else {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (jsonParseContext.isAllowUnquotedFieldNames()) {
                            // 无引号key处理
                            // 直接锁定冒号（:）位置
                            while (i + 1 < toIndex && buf[++i] != ':') ;
                            empty = false;
                        }
                    }
                }
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (jsonParseContext.isAllowComment()) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (ch == ':') {
                    while ((ch = buf[++i]) <= ' ') ;
                    if (jsonParseContext.isAllowComment()) {
                        if (ch == '/') {
                            i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext);
                        }
                    }
                    ANY.skip(charSource, buf, i, toIndex, '}', jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                    while ((ch = buf[++i]) <= ' ') ;
                    if (jsonParseContext.isAllowComment()) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == '}') {
                        jsonParseContext.setEndIndex(i);
                        return;
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or '}'");
                } else {
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', Colon character ':' is expected.");
                }
            }
            throw new JSONException("Syntax error, the closing symbol '}' is not found ");
        }

        void skip(CharSource charSource, byte[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
            boolean empty = true;
            byte b;
            for (int i = fromIndex + 1; i < toIndex; ++i) {
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (jsonParseContext.isAllowComment()) {
                    if (b == '/') {
                        b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (b == '"') {
                    while (i + 1 < toIndex && ((b = buf[++i]) != '"' || buf[i - 1] == '\\')) ;
                    empty = false;
                    if (b != '"') {
                        String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                        throw new JSONException("Syntax error, util pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '\"' is not found ");
                    }
                    ++i;
                } else {
                    if (b == '}') {
                        if (!empty) {
                            String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                        }
                        jsonParseContext.setEndIndex(i);
                        return;
                    }
                    if (b == '\'') {
                        if (jsonParseContext.isAllowSingleQuotes()) {
                            while (i + 1 < toIndex && buf[++i] != '\'') ;
                            empty = false;
                            ++i;
                        } else {
                            String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (jsonParseContext.isAllowUnquotedFieldNames()) {
                            // 无引号key处理
                            // 直接锁定冒号（:）位置
                            while (i + 1 < toIndex && buf[++i] != ':') ;
                            empty = false;
                        }
                    }
                }
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (jsonParseContext.isAllowComment()) {
                    if (b == '/') {
                        b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (b == ':') {
                    while ((b = buf[++i]) <= ' ') ;
                    if (jsonParseContext.isAllowComment()) {
                        if (b == '/') {
                            i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext);
                        }
                    }
                    ANY.skip(charSource, buf, i, toIndex, EndObject, jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                    while ((b = buf[++i]) <= ' ') ;
                    if (jsonParseContext.isAllowComment()) {
                        if (b == '/') {
                            b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (b == ',') {
                        continue;
                    }
                    if (b == '}') {
                        jsonParseContext.setEndIndex(i);
                        return;
                    }
                    String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', expected ',' or '}'");
                } else {
                    String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', Colon character ':' is expected.");
                }
            }
            throw new JSONException("Syntax error, the closing symbol '}' is not found ");
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {

            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '{':
                    return deserializeMap(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
                case 'n':
                    return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
                default: {
                    // not support or custom handle ?
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + beginChar + "' for Map Type, expected '{' ");
                }
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            char beginChar = (char) beginByte;
            switch (beginChar) {
                case '{':
                    return deserializeMap(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
                case 'n':
                    return JSONByteArrayParser.parseNull(buf, fromIndex, toIndex, jsonParseContext);
                default: {
                    // not support or custom handle ?
                    String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + beginChar + "' for Map Type, expected '{' ");
                }
            }
        }


        protected static Object mapKeyToType(Serializable mapKey, Class<?> keyType) {
            if (mapKey == null || keyType == null || keyType == String.class || keyType == CharSequence.class) {
                return mapKey;
            }
            int paramClassType = ReflectConsts.getParamClassType(keyType);
            switch (paramClassType) {
                case ReflectConsts.CLASS_TYPE_STRING_BUFFER: {
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(mapKey.toString());
                    return buffer;
                }
                case ReflectConsts.CLASS_TYPE_STRING_BUILDER: {
                    StringBuilder builder = new StringBuilder();
                    builder.append(mapKey.toString());
                    return builder;
                }
                case ReflectConsts.CLASS_TYPE_NUMBER: {
                    String key = mapKey.toString();
                    return NUMBER.parseNumberValue(key.toCharArray(), 0, key.length(), ReflectConsts.getParamClassNumberType(keyType));
                }
            }
            throw new UnsupportedOperationException("Not Supported type '" + keyType + "' for map key ");
        }

        Object deserializeMap(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object obj, JSONParseContext jsonParseContext) throws Exception {

            GenericParameterizedType valueType = parameterizedType.getValueType();
            ReflectConsts.ClassCategory actualClassCategory = valueType == null ? ReflectConsts.ClassCategory.ANY : ReflectConsts.getClassCategory(valueType.getActualType());

            Map instance;
            if (obj != null) {
                instance = (Map) obj;
            } else {
                Class<? extends Map> mapClass = parameterizedType.getActualType();
                instance = createMapInstance(mapClass);
            }

            boolean empty = true;
            char ch;
            boolean disableCacheMapKey = jsonParseContext.isDisableCacheMapKey();
            boolean allowComment = jsonParseContext.isAllowComment();
            for (int i = fromIndex + 1; i < toIndex; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }

                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                int fieldKeyFrom = i;
                Serializable mapKey;
                Object key;

                if (ch == '"') {
                    mapKey = disableCacheMapKey ? JSONDefaultParser.parseMapKey(buf, i, toIndex, '"', jsonParseContext) : JSONDefaultParser.parseMapKeyByCache(buf, i, toIndex, '"', jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                    empty = false;
                    ++i;
                } else {
                    // empty object or exception
                    if (ch == '}') {
                        if (!empty) {
                            throw new JSONException("Syntax error, at pos " + i + ", the closing symbol '}' is not allowed here.");
                        }
                        jsonParseContext.setEndIndex(i);
                        return instance;
                    }
                    if (ch == '\'') {
                        if (jsonParseContext.isAllowSingleQuotes()) {
                            while (i + 1 < toIndex && (buf[++i] != '\'' || buf[i - 1] == '\\')) ;
                            empty = false;
                            ++i;
                            mapKey = JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, i, false);
                        } else {
                            throw new JSONException("Syntax error, at pos " + i + ", the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (jsonParseContext.isAllowUnquotedFieldNames()) {
                            while (i + 1 < toIndex && buf[++i] != ':') ;
                            empty = false;
                            mapKey = JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, i, true);
                        } else {
                            // check if null ?
                            int j = i;
                            boolean isNullKey = false;
                            mapKey = null;
                            if (ch == 'n' && buf[++i] == 'u' && buf[++i] == 'l' && buf[++i] == 'l') {
                                isNullKey = true;
                                ++i;
                            }
                            if (!isNullKey) {
                                String errorContextTextAt = createErrorContextText(buf, j);
                                throw new JSONException("Syntax error, at pos " + j + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected '\"' or use option ReadOption.AllowUnquotedFieldNames ");
                            }
                        }
                    }
                }

                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (ch == ':') {
                    Class mapKeyClass = parameterizedType == null ? null : parameterizedType.getMapKeyClass();
                    key = mapKeyToType(mapKey, mapKeyClass);  // parseKeyOfMap(fieldKeyFrom, fieldKeyTo, buf, mapKeyClass, isUnquotedFieldName, jsonParseContext);

                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }

                    int ordinal = actualClassCategory.ordinal();
                    Object value = JSONTypeDeserializer.TYPE_DESERIALIZERS[ordinal].deserialize(charSource, buf, i, toIndex, valueType, null, '}', jsonParseContext);
                    instance.put(key, value);
                    i = jsonParseContext.getEndIndex();

                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == '}') {
                        jsonParseContext.setEndIndex(i);
                        return instance;
                    }

                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or '}'");
                } else {
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', Colon character ':' is expected.");
                }
            }

            throw new JSONException("Syntax error, the closing symbol '}' is not found ");
        }

        Object deserializeMap(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object obj, JSONParseContext jsonParseContext) throws Exception {

            GenericParameterizedType valueType = parameterizedType.getValueType();
            ReflectConsts.ClassCategory actualClassCategory = valueType == null ? ReflectConsts.ClassCategory.ANY : ReflectConsts.getClassCategory(valueType.getActualType());

            Map instance;
            if (obj != null) {
                instance = (Map) obj;
            } else {
                Class<? extends Map> mapClass = parameterizedType.getActualType();
                instance = createMapInstance(mapClass);
            }

            boolean empty = true;
            byte b;
            boolean disableCacheMapKey = jsonParseContext.isDisableCacheMapKey();
            boolean allowComment = jsonParseContext.isAllowComment();
            for (int i = fromIndex + 1; i < toIndex; ++i) {
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }

                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                int fieldKeyFrom = i;
                Serializable mapKey;
                Object key;

                if (b == '"') {
                    mapKey = disableCacheMapKey ? JSONByteArrayParser.parseMapKey(buf, i, toIndex, '"', jsonParseContext) : JSONByteArrayParser.parseMapKeyByCache(buf, i, toIndex, '"', jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                    empty = false;
                    ++i;
                } else {
                    // empty object or exception
                    if (b == '}') {
                        if (!empty) {
                            throw new JSONException("Syntax error, at pos " + i + ", the closing symbol '}' is not allowed here.");
                        }
                        jsonParseContext.setEndIndex(i);
                        return instance;
                    }
                    if (b == '\'') {
                        if (jsonParseContext.isAllowSingleQuotes()) {
                            while (i + 1 < toIndex && (buf[++i] != '\'' || buf[i - 1] == '\\')) ;
                            empty = false;
                            ++i;
                            mapKey = JSONByteArrayParser.parseKeyOfMap(buf, fieldKeyFrom, i, false);
                        } else {
                            throw new JSONException("Syntax error, at pos " + i + ", the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (jsonParseContext.isAllowUnquotedFieldNames()) {
                            while (i + 1 < toIndex && buf[++i] != ':') ;
                            empty = false;
                            mapKey = JSONByteArrayParser.parseKeyOfMap(buf, fieldKeyFrom, i, true);
                        } else {
                            // check if null ?
                            int j = i;
                            boolean isNullKey = false;
                            mapKey = null;
                            if (b == 'n' && buf[++i] == 'u' && buf[++i] == 'l' && buf[++i] == 'l') {
                                isNullKey = true;
                                ++i;
                            }
                            if (!isNullKey) {
                                String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, j);
                                throw new JSONException("Syntax error, at pos " + j + ", context text by '" + errorContextTextAt + "', unexpected token character '" + (char) b + "', expected '\"' or use option ReadOption.AllowUnquotedFieldNames ");
                            }
                        }
                    }
                }

                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (b == ':') {
                    Class mapKeyClass = parameterizedType == null ? null : parameterizedType.getMapKeyClass();
                    key = mapKeyToType(mapKey, mapKeyClass);  // parseKeyOfMap(fieldKeyFrom, fieldKeyTo, buf, mapKeyClass, isUnquotedFieldName, jsonParseContext);

                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }

                    int ordinal = actualClassCategory.ordinal();
                    Object value = JSONTypeDeserializer.TYPE_DESERIALIZERS[ordinal].deserialize(charSource, buf, i, toIndex, valueType, null, EndObject, jsonParseContext);
                    instance.put(key, value);
                    i = jsonParseContext.getEndIndex();

                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (b == ',') {
                        continue;
                    }
                    if (b == '}') {
                        jsonParseContext.setEndIndex(i);
                        return instance;
                    }

                    String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', expected ',' or '}'");
                } else {
                    String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', Colon character ':' is expected.");
                }
            }

            throw new JSONException("Syntax error, the closing symbol '}' is not found ");
        }

    }

    // 11、对象
    static class ObjectDeserializer extends JSONTypeDeserializer {

        Object deserializeObject(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType genericParameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {

            Class clazz = genericParameterizedType.getActualType();
            ObjectStructureWrapper classStructureWrapper = ObjectStructureWrapper.get(clazz);

            Object entity;
            if (instance != null) {
                entity = instance;
            } else {
                entity = classStructureWrapper.newInstance();
            }

            boolean empty = true;
            char ch;

            for (int i = fromIndex + 1; /*i < toIndex*/ ; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }

                if (jsonParseContext.isAllowComment()) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                int fieldKeyFrom = i, fieldKeyTo;
                boolean isUnquotedFieldName = false;
                int hashValue = 0;
                if (ch == '"') {
                    while (/*i + 1 < toIndex &&*/ ((ch = buf[++i]) != '"' || buf[i - 1] == '\\')) {
                        hashValue = hashValue * 31 + ch;
                    }
                    empty = false;
//                    if (ch != '"') {
//                        String errorContextTextAt = createErrorContextText(buf, i);
//                        throw new JSONException("Syntax error, util pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '\"' is not found ");
//                    }
                    ++i;
                } else {
                    // 4种可能：（There are only four possibilities）
                    // 1 空对象（empty object）
                    // 2 语法错误，字符不为空，无效结束符号'}'（Syntax error, character is not empty, invalid closing symbol '}'）
                    // 3 单引号key
                    // 4 无引号key
                    if (ch == '}') {
                        if (!empty) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                        }
                        jsonParseContext.setEndIndex(i);
                        return entity;
                    }
                    if (ch == '\'') {
                        if (jsonParseContext.isAllowSingleQuotes()) {
                            while (i + 1 < toIndex && (ch = buf[++i]) != '\'') {
                                hashValue = hashValue * 31 + ch;
                            }
                            empty = false;
                            ++i;
                        } else {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (jsonParseContext.isAllowUnquotedFieldNames()) {
                            // 无引号key处理
                            // 直接锁定冒号（:）位置
                            // 需要处理后置空白（冒号前面的空白字符）
                            while (i + 1 < toIndex && buf[++i] != ':') {
                                if (ch > ' ') {
                                    hashValue = hashValue * 31 + ch;
                                }
                            }
                            empty = false;
                            isUnquotedFieldName = true;
                        }
                    }
                }

                fieldKeyTo = i;
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (jsonParseContext.isAllowComment()) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (ch == ':') {
                    FieldDeserializer fieldDeserializer = getFieldDeserializer(classStructureWrapper, buf, fieldKeyFrom, fieldKeyTo, isUnquotedFieldName, hashValue);
                    while (buf[++i] <= ' ') ;

                    Object defaultValue = null;
                    if (jsonParseContext.isAllowComment()) {
                        ch = buf[i];
                        if (ch == '/') {
                            i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext);
                        }
                    }
                    boolean isDeserialize = fieldDeserializer != null;
                    GenericParameterizedType valueType = null;
                    JSONTypeDeserializer deserializer = null;
                    if (isDeserialize) {
                        valueType = fieldDeserializer.getGenericParameterizedType();
                        deserializer = fieldDeserializer.getDeserializer();
                        boolean nonInstanceType = fieldDeserializer.isNonInstanceType();
                        if (nonInstanceType) {
                            if (jsonParseContext.isUseDefaultFieldInstance()) {
                                defaultValue = fieldDeserializer.getDefaultFieldValue(instance);
                                if (defaultValue != null) {
                                    Class implClass = defaultValue.getClass();
                                    valueType = valueType.copyAndReplaceActualType(implClass);
                                    if (deserializer == null) {
                                        deserializer = getTypeDeserializer(implClass);
                                    }
                                }
                            } else {
                                isDeserialize = false;
                            }
                        } else {
                            boolean camouflage = valueType.isCamouflage();
                            if (camouflage) {
                                valueType = getGenericValueType(genericParameterizedType, valueType);
                                if (!fieldDeserializer.isCustomDeserialize()) {
                                    deserializer = getTypeDeserializer(valueType.getActualType());
                                }
                            }
                        }
                    }

                    if (isDeserialize) {
                        Object value = deserializer.deserialize(charSource, buf, i, toIndex, valueType, defaultValue, '}', jsonParseContext);
                        fieldDeserializer.invoke(entity, value);
                        i = jsonParseContext.getEndIndex();
                    } else {
                        JSONTypeDeserializer.ANY.skip(charSource, buf, i, toIndex, '}', jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                    }

                    while ((ch = buf[++i]) <= ' ') ;
                    if (jsonParseContext.isAllowComment()) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == '}') {
                        jsonParseContext.setEndIndex(i);
                        return entity;
                    }

                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or '}'");
                } else {
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', Colon character ':' is expected.");
                }
            }
//            throw new JSONException("Syntax error, the closing symbol '}' is not found ");
        }

        Object deserializeObject(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType genericParameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {

            Class clazz = genericParameterizedType.getActualType();
            ObjectStructureWrapper classStructureWrapper = ObjectStructureWrapper.get(clazz);

            Object entity;
            if (instance != null) {
                entity = instance;
            } else {
                entity = classStructureWrapper.newInstance();
            }

            boolean empty = true;
            byte b;

            for (int i = fromIndex + 1; /*i < toIndex*/ ; ++i) {
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (jsonParseContext.isAllowComment()) {
                    if (b == '/') {
                        b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                int fieldKeyFrom = i, fieldKeyTo;
                boolean isUnquotedFieldName = false;
                int hashValue = 0;
                if (b == '"') {
                    while (/*i + 1 < toIndex &&*/ ((b = buf[++i]) != '"' || buf[i - 1] == '\\')) {
                        hashValue = hashValue * 31 + b;
                    }
                    empty = false;
                    ++i;
                } else {
                    if (b == '}') {
                        if (!empty) {
                            String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                        }
                        jsonParseContext.setEndIndex(i);
                        return entity;
                    }
                    if (b == '\'') {
                        if (jsonParseContext.isAllowSingleQuotes()) {
                            while (i + 1 < toIndex && (b = buf[++i]) != '\'') {
                                hashValue = hashValue * 31 + b;
                            }
                            empty = false;
                            ++i;
                        } else {
                            String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (jsonParseContext.isAllowUnquotedFieldNames()) {
                            // 无引号key处理
                            // 直接锁定冒号（:）位置
                            // 需要处理后置空白（冒号前面的空白字符）
                            while (i + 1 < toIndex && buf[++i] != ':') {
                                if (b > ' ') {
                                    hashValue = hashValue * 31 + b;
                                }
                            }
                            empty = false;
                            isUnquotedFieldName = true;
                        }
                    }
                }

                fieldKeyTo = i;
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (jsonParseContext.isAllowComment()) {
                    if (b == '/') {
                        b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (b == ':') {
                    FieldDeserializer fieldDeserializer = getFieldDeserializer(classStructureWrapper, buf, fieldKeyFrom, fieldKeyTo, isUnquotedFieldName, hashValue);
                    while (buf[++i] <= ' ') ;

                    Object defaultValue = null;
                    if (jsonParseContext.isAllowComment()) {
                        b = buf[i];
                        if (b == '/') {
                            i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext);
                        }
                    }
                    boolean isDeserialize = fieldDeserializer != null;
                    GenericParameterizedType valueType = null;
                    JSONTypeDeserializer deserializer = null;
                    if (isDeserialize) {
                        valueType = fieldDeserializer.getGenericParameterizedType();
                        deserializer = fieldDeserializer.getDeserializer();
                        boolean nonInstanceType = fieldDeserializer.isNonInstanceType();
                        if (nonInstanceType) {
                            if (jsonParseContext.isUseDefaultFieldInstance()) {
                                defaultValue = fieldDeserializer.getDefaultFieldValue(instance);
                                if (defaultValue != null) {
                                    Class implClass = defaultValue.getClass();
                                    valueType = valueType.copyAndReplaceActualType(implClass);
                                    if (deserializer == null) {
                                        deserializer = getTypeDeserializer(implClass);
                                    }
                                }
                            } else {
                                isDeserialize = false;
                            }
                        } else {
                            boolean camouflage = valueType.isCamouflage();
                            if (camouflage) {
                                valueType = getGenericValueType(genericParameterizedType, valueType);
                                if (!fieldDeserializer.isCustomDeserialize()) {
                                    deserializer = getTypeDeserializer(valueType.getActualType());
                                }
                            }
                        }
                    }

                    if (isDeserialize) {
                        Object value = deserializer.deserialize(charSource, buf, i, toIndex, valueType, defaultValue, EndObject, jsonParseContext);
                        fieldDeserializer.invoke(entity, value);
                        i = jsonParseContext.getEndIndex();
                    } else {
                        JSONTypeDeserializer.ANY.skip(charSource, buf, i, toIndex, EndObject, jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                    }

                    while ((b = buf[++i]) <= ' ') ;
                    if (jsonParseContext.isAllowComment()) {
                        if (b == '/') {
                            b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (b == ',') {
                        continue;
                    }
                    if (b == '}') {
                        jsonParseContext.setEndIndex(i);
                        return entity;
                    }

                    String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + (char) b + "', expected ',' or '}'");
                } else {
                    String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + (char) b + "', Colon character ':' is expected.");
                }
            }
//            throw new JSONException("Syntax error, the closing symbol '}' is not found ");
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType genericParameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {

            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '{':
                    return deserializeObject(charSource, buf, fromIndex, toIndex, genericParameterizedType, instance, jsonParseContext);
                case 'n':
                    return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
                default: {
                    // not support or custom handle ?
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + beginChar + "' for Object Type, expected '{' ");
                }
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType genericParameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            char beginChar = (char) beginByte;
            switch (beginChar) {
                case '{':
                    return deserializeObject(charSource, buf, fromIndex, toIndex, genericParameterizedType, instance, jsonParseContext);
                case 'n':
                    return JSONByteArrayParser.parseNull(buf, fromIndex, toIndex, jsonParseContext);
                default: {
                    // not support or custom handle ?
                    String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + beginChar + "' for Object Type, expected '{' ");
                }
            }
        }

        /**
         * 根据hash值和偏移位置获取属性的反序列化器
         *
         * @param pojoStructureWrapper
         * @param buf
         * @param from
         * @param to
         * @param isUnquotedFieldName  是否没有双引号
         * @param hashValue
         * @return
         */
        private FieldDeserializer getFieldDeserializer(ObjectStructureWrapper pojoStructureWrapper, char[] buf, int from, int to, boolean isUnquotedFieldName, int hashValue) {
            if (isUnquotedFieldName) {
                return getFieldDeserializer(pojoStructureWrapper, buf, from, to, hashValue);
            }
            return getFieldDeserializer(pojoStructureWrapper, buf, from + 1, to - 1, hashValue);
        }

        /**
         * 根据hash值和偏移位置获取属性的反序列化器
         *
         * @param pojoStructureWrapper
         * @param buf
         * @param from
         * @param to
         * @param isUnquotedFieldName  是否没有双引号
         * @param hashValue
         * @return
         */
        private FieldDeserializer getFieldDeserializer(ObjectStructureWrapper pojoStructureWrapper, byte[] buf, int from, int to, boolean isUnquotedFieldName, int hashValue) {
            if (isUnquotedFieldName) {
                return pojoStructureWrapper.getFieldDeserializer(buf, from, to, hashValue);
            }
            return pojoStructureWrapper.getFieldDeserializer(buf, from + 1, to - 1, hashValue);
        }

        /**
         * 根据hash值和偏移位置获取属性的反序列化器
         *
         * @param pojoStructureWrapper
         * @param buf
         * @param from
         * @param to
         * @param hashValue
         * @return
         */
        private FieldDeserializer getFieldDeserializer(ObjectStructureWrapper pojoStructureWrapper, char[] buf, int from, int to, int hashValue) {
            FieldDeserializer fieldDeserializer = pojoStructureWrapper.getFieldDeserializer(buf, from, to, hashValue);
            return fieldDeserializer;
        }


    }

    /***
     * 12、ANY
     */
    static class ANYDeserializer extends JSONTypeDeserializer {

        void skip(CharSource charSource, char[] buf, int fromIndex, int toIndex, char endToken, JSONParseContext jsonParseContext) throws Exception {

            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '{':
                    MAP.skip(charSource, buf, fromIndex, toIndex, jsonParseContext);
                    break;
                case '[':
                    COLLECTION.skip(charSource, buf, fromIndex, toIndex, jsonParseContext);
                    break;
                case '"':
                    STRING.skip(charSource, buf, fromIndex, toIndex, jsonParseContext);
                    break;
                case 'n':
                    NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
                    break;
                case 't': {
                    BOOLEAN.deserializeTrue(buf, fromIndex, toIndex, null, jsonParseContext);
                    break;
                }
                case 'f': {
                    BOOLEAN.deserializeFalse(buf, fromIndex, toIndex, null, jsonParseContext);
                    break;
                }
                default: {
                    NUMBER.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.BigDecimalType, null, endToken, jsonParseContext);
                    break;
                }
            }
        }

        void skip(CharSource charSource, byte[] buf, int fromIndex, int toIndex, byte endToken, JSONParseContext jsonParseContext) throws Exception {

            byte beginByte = buf[fromIndex];
            switch (beginByte) {
                case '{':
                    MAP.skip(charSource, buf, fromIndex, toIndex, jsonParseContext);
                    break;
                case '[':
                    COLLECTION.skip(charSource, buf, fromIndex, toIndex, jsonParseContext);
                    break;
                case '"':
                    STRING.skip(charSource, buf, fromIndex, toIndex, jsonParseContext);
                    break;
                case 'n':
                    JSONByteArrayParser.parseNull(buf, fromIndex, toIndex, jsonParseContext);
                    break;
                case 't': {
                    JSONByteArrayParser.parseTrue(buf, fromIndex, toIndex, jsonParseContext);
                    break;
                }
                case 'f': {
                    JSONByteArrayParser.parseFalse(buf, fromIndex, toIndex, jsonParseContext);
                    break;
                }
                default: {
                    JSONByteArrayParser.parseDefaultNumber(buf, fromIndex, toIndex, endToken, jsonParseContext);
                    break;
                }
            }
        }

        // ==> JSONDefaultParser.parse
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {

            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '{':
                    return JSONDefaultParser.parseJSONObject(charSource, buf, fromIndex, toIndex, new LinkedHashMap(), jsonParseContext);
//                    return MAP.deserializeMap(buf, fromIndex, toIndex, GenericParameterizedType.DefaultMap, new LinkedHashMap(), jsonParseContext);
                case '[':
                    return JSONDefaultParser.parseJSONArray(charSource, buf, fromIndex, toIndex, new ArrayList(), jsonParseContext);
//                    return COLLECTION.deserializeCollection(buf, fromIndex, toIndex, GenericParameterizedType.DefaultCollection, new ArrayList(), jsonParseContext);
                case '\'':
                case '"':
                    return JSONDefaultParser.parseJSONString(charSource, buf, fromIndex, toIndex, beginChar, jsonParseContext);
//                    return STRING.deserializeString(buf, fromIndex, toIndex, GenericParameterizedType.StringType, jsonParseContext);
                case 'n':
                    return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
                case 't': {
                    return BOOLEAN.deserializeTrue(buf, fromIndex, toIndex, null, jsonParseContext);
                }
                case 'f': {
                    return BOOLEAN.deserializeFalse(buf, fromIndex, toIndex, null, jsonParseContext);
                }
                default: {
                    return NUMBER.deserialize(charSource, buf, fromIndex, toIndex, jsonParseContext.isUseBigDecimalAsDefault() ? GenericParameterizedType.BigDecimalType : GenericParameterizedType.AnyType, null, endToken, jsonParseContext);
                }
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            char beginChar = (char) beginByte;
            switch (beginChar) {
                case '{':
                    return JSONByteArrayParser.parseJSONObject(charSource == null ? null : charSource.input(), buf, fromIndex, toIndex, new LinkedHashMap(), jsonParseContext);
                case '[':
                    return JSONByteArrayParser.parseJSONArray(charSource == null ? null : charSource.input(), buf, fromIndex, toIndex, new ArrayList(), jsonParseContext);
                case '\'':
                case '"':
                    return JSONByteArrayParser.parseJSONString(charSource == null ? null : charSource.input(), buf, fromIndex, toIndex, beginByte, jsonParseContext);
                case 'n':
                    return JSONByteArrayParser.parseNull(buf, fromIndex, toIndex, jsonParseContext);
                case 't': {
                    return JSONByteArrayParser.parseTrue(buf, fromIndex, toIndex, jsonParseContext);
                }
                case 'f': {
                    return JSONByteArrayParser.parseFalse(buf, fromIndex, toIndex, jsonParseContext);
                }
                default: {
                    return JSONByteArrayParser.parseDefaultNumber(buf, fromIndex, toIndex, endToken, jsonParseContext);
                }
            }
        }
    }

    /**
     * 解析null
     */
    static class NULLDeserializer extends JSONTypeDeserializer {

        Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
            return deserialize(charSource, buf, fromIndex, toIndex, parameterizedType, instance, '\0', jsonParseContext);
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            int endIndex = fromIndex + 3;
            if (buf[fromIndex + 1] == 'u'
                    && buf[fromIndex + 2] == 'l' && buf[endIndex] == 'l') {
                jsonParseContext.setEndIndex(endIndex);
                return null;
            }
            throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'null' because it starts with 'n', but found text '" + new String(buf, fromIndex, Math.min(toIndex - fromIndex + 1, 4)) + "'");
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            int endIndex = fromIndex + 3;
            if (buf[fromIndex + 1] == 'u'
                    && buf[fromIndex + 2] == 'l' && buf[endIndex] == 'l') {
                jsonParseContext.setEndIndex(endIndex);
                return null;
            }
            throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'null' because it starts with 'n', but found text '" + new String(buf, fromIndex, Math.min(toIndex - fromIndex + 1, 4)) + "'");
        }
    }

    private static GenericParameterizedType getGenericValueType(GenericParameterizedType parameterizedType, GenericParameterizedType valueType) {
        Class<?> actualType = parameterizedType.getGenericClass(valueType.getGenericName());
        return GenericParameterizedType.actualType(actualType);
    }

    /**
     * read first field if key is '@C' and value as the implClass
     *
     * @param buf
     * @param fromIndex
     * @param toIndex
     * @param jsonParseContext
     * @return
     * @throws Exception
     */
    protected final String parseObjectClassName(CharSource charSource, char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
        char ch;
        for (int i = fromIndex + 1; /*i < toIndex*/ ; ++i) {
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (jsonParseContext.isAllowComment()) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            boolean matched = false;
            if (ch == '"') {
                if (buf[++i] == '@' && buf[++i] == 'c' && buf[++i] == '"') {
                    matched = true;
                    ++i;
                }
            } else {
                if (ch == '}') {
                    return null;
                }
                if (ch == '\'') {
                    if (buf[++i] == '@' && buf[++i] == 'c' && buf[++i] == '\'') {
                        matched = true;
                        ++i;
                    }
                } else {
                    if (jsonParseContext.isAllowUnquotedFieldNames()) {
                        char atChar = buf[i++];
                        char cChar = buf[i++];
                        while ((ch = buf[i]) <= ' ') {
                            ++i;
                        }
                        if (atChar == '@' && cChar == 'c' && ch == ':') {
                            matched = true;
                        }
                    }
                }
            }
            if (!matched) {
                return null;
            }
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (jsonParseContext.isAllowComment()) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (ch == ':') {
                while ((ch = buf[++i]) <= ' ') ;
                if (jsonParseContext.isAllowComment()) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                Object value = ANY.deserialize(charSource, buf, i, toIndex, null, null, '}', jsonParseContext);
                if (value instanceof String) {
                    return (String) value;
                } else {
                    return null;
                }
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', Colon character ':' is expected.");
            }
        }
    }

    /**
     * read first field if key is '@C' and value as the implClass
     *
     * @param buf
     * @param fromIndex
     * @param toIndex
     * @param jsonParseContext
     * @return
     * @throws Exception
     */
    protected final String parseObjectClassName(CharSource charSource, byte[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
        byte b;
        for (int i = fromIndex + 1; /*i < toIndex*/ ; ++i) {
            while ((b = buf[i]) <= ' ') {
                ++i;
            }
            if (jsonParseContext.isAllowComment()) {
                if (b == '/') {
                    b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            boolean matched = false;
            if (b == '"') {
                if (buf[++i] == '@' && buf[++i] == 'c' && buf[++i] == '"') {
                    matched = true;
                    ++i;
                }
            } else {
                if (b == '}') {
                    return null;
                }
                if (b == '\'') {
                    if (buf[++i] == '@' && buf[++i] == 'c' && buf[++i] == '\'') {
                        matched = true;
                        ++i;
                    }
                } else {
                    if (jsonParseContext.isAllowUnquotedFieldNames()) {
                        byte atChar = buf[i++];
                        byte cChar = buf[i++];
                        while ((b = buf[i]) <= ' ') {
                            ++i;
                        }
                        if (atChar == '@' && cChar == 'c' && b == ':') {
                            matched = true;
                        }
                    }
                }
            }
            if (!matched) {
                return null;
            }
            while ((b = buf[i]) <= ' ') {
                ++i;
            }
            if (jsonParseContext.isAllowComment()) {
                if (b == '/') {
                    b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (b == ':') {
                while ((b = buf[++i]) <= ' ') ;
                if (jsonParseContext.isAllowComment()) {
                    if (b == '/') {
                        b = buf[i = JSONByteArrayParser.clearComments(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                Object value = ANY.deserialize(charSource, buf, i, toIndex, null, null, EndObject, jsonParseContext);
                if (value instanceof String) {
                    return (String) value;
                } else {
                    return null;
                }
            } else {
                String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', Colon character ':' is expected.");
            }
        }
    }

    protected final static Class<?> getClassByName(String className) throws ClassNotFoundException {
        Class<?> cls = classNameMapping.get(className);
        if (cls != null) return cls;
        classNameMapping.put(className, cls = Class.forName(className));
        return cls;
    }

    private static class SerializableDeserializer extends JSONTypeDeserializer {
        @Override
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
            Object value = ANY.deserialize(charSource, buf, fromIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
            if (value instanceof Serializable) {
                return value;
            }
            return null;
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            Object value = ANY.deserialize(charSource, buf, fromIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
            if (value instanceof Serializable) {
                return value;
            }
            return null;
        }
    }
}
