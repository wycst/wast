package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.DateFormatter;
import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.tools.Base64;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.common.utils.NumberUtils;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.options.JsonConfig;
import io.github.wycst.wast.json.reflect.FieldSerializer;
import io.github.wycst.wast.json.reflect.ObjectStructureWrapper;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author: wangy
 * @Date: 2022/6/18 5:59
 * @Description:
 */
public abstract class JSONTypeSerializer extends JSONGeneral {

    final static int LENGTH = ReflectConsts.ClassCategory.values().length;
    final static JSONTypeSerializer[] TYPE_SERIALIZERS = new JSONTypeSerializer[LENGTH];
    // class and JSONTypeSerializer mapping
    private static final Map<Class<?>, JSONTypeSerializer> classJSONTypeSerializerMap = new ConcurrentHashMap<Class<?>, JSONTypeSerializer>();
    // CharSequence
    protected static final CharSequenceSerializer CHAR_SEQUENCE = new CharSequenceSerializer();
    protected static final CharSequenceSerializer STRING = EnvUtils.JDK_9_ABOVE ? new CharSequenceSerializer.StringBytesSerializer() : new CharSequenceSerializer.StringCharsSerializer();
    protected static final SimpleSerializer.SimpleNumberSerializer NUMBER = new SimpleSerializer.SimpleNumberSerializer();
    protected static final SimpleSerializer.SimpleNumberSerializer NUMBER_LONG = new SimpleSerializer.SimpleLongSerializer();
    protected static final MapSerializer MAP = new MapSerializer();
    protected static final CollectionSerializer COLLECTION = new CollectionSerializer();

    protected static final JSONTypeSerializer DATE_AS_TIME_SERIALIZER = new DateSerializer.DateAsTimeSerializer();;

    static {
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.CharSequence.ordinal()] = CHAR_SEQUENCE;
        JSONTypeSerializer SIMPLE = new SimpleSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.NumberCategory.ordinal()] = NUMBER;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.BoolCategory.ordinal()] = SIMPLE;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.DateCategory.ordinal()] = new DateSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ClassCategory.ordinal()] = new ClassSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.EnumCategory.ordinal()] = new EnumSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.AnnotationCategory.ordinal()] = new AnnotationSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.Binary.ordinal()] = new BinarySerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ArrayCategory.ordinal()] = new ArraySerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.CollectionCategory.ordinal()] = COLLECTION;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.MapCategory.ordinal()] = MAP;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ObjectCategory.ordinal()] = new ObjectSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ANY.ordinal()] = new ANYSerializer();

        putTypeSerializer(STRING, String.class);
        putTypeSerializer(new SimpleSerializer.SimpleBigIntegerSerializer(), BigInteger.class);
        putTypeSerializer(new SimpleSerializer.SimpleDoubleSerializer(), double.class, Double.class);
        putTypeSerializer(new SimpleSerializer.SimpleFloatSerializer(), float.class, Float.class);
        putTypeSerializer(NUMBER_LONG, byte.class, Byte.class, short.class, Short.class, int.class, Integer.class, long.class, Long.class, AtomicInteger.class, AtomicLong.class);
    }

    static void putTypeSerializer(JSONTypeSerializer typeSerializer, Class... types) {
        for (Class type : types) {
            classJSONTypeSerializerMap.put(type, typeSerializer);
        }
    }

    protected static JSONTypeSerializer getTypeSerializer(ReflectConsts.ClassCategory classCategory, JsonProperty jsonProperty) {
        int ordinal = classCategory.ordinal();
        if (classCategory == ReflectConsts.ClassCategory.DateCategory) {
            if (jsonProperty != null) {
                if(jsonProperty.asTimestamp()) {
                    return DATE_AS_TIME_SERIALIZER;
                }
                String pattern = jsonProperty.pattern().trim();
                if(pattern.length() > 0) {
                    String timeZoneId = jsonProperty.timezone().trim();
                    return new DateSerializer.DatePatternSerializer(DateFormatter.of(pattern), getTimeZone(timeZoneId));
                }
            }
        }
        if (classCategory == ReflectConsts.ClassCategory.NonInstance) {
            // write classname
            return new ObjectSerializer.ObjectWithTypeSerializer();
        }
        return TYPE_SERIALIZERS[ordinal];
    }

    protected static JSONTypeSerializer getTypeSerializer(Class<?> cls) {
        int classHashCode = cls.getName().hashCode();
        switch (classHashCode) {
            case EnvUtils.STRING_HV: {
                if(cls == String.class) {
                    return STRING;
                }
                break;
            }
            case EnvUtils.INT_HV:
            case EnvUtils.INTEGER_HV:
            case EnvUtils.LONG_PRI_HV:
            case EnvUtils.LONG_HV:
                if(Number.class.isAssignableFrom(cls)) {
                    return NUMBER_LONG;
                }
                break;
            case EnvUtils.HASHMAP_HV:
            case EnvUtils.LINK_HASHMAP_HV:
                if(Map.class.isAssignableFrom(cls)) {
                    return MAP;
                }
                break;
            case EnvUtils.ARRAY_LIST_HV:
            case EnvUtils.HASH_SET_HV:
                if(Collection.class.isAssignableFrom(cls)) {
                    return COLLECTION;
                }
                break;
        }
        JSONTypeSerializer typeSerializer = classJSONTypeSerializerMap.get(cls);
        if (typeSerializer != null) {
            return typeSerializer;
        } else {
            ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(cls);
            if (classCategory == ReflectConsts.ClassCategory.ObjectCategory) {
                typeSerializer = new ObjectSerializer.ObjectWrapperSerializer(cls);
            } else {
                typeSerializer = getTypeSerializer(classCategory, null);
            }
            classJSONTypeSerializerMap.put(cls, typeSerializer);
            return typeSerializer;
        }
    }

    protected static JSONTypeSerializer getEnumSerializer(Class<?> enumClass) {
        JSONTypeSerializer enumSerializer = classJSONTypeSerializerMap.get(enumClass);
        if(enumSerializer != null) {
            return enumSerializer;
        }
        Enum[] values = (Enum[]) enumClass.getEnumConstants();
        char[][] enumNames = new char[values.length][];
        for (Enum value : values) {
            String enumName = value.name();
            char[] chars = new char[enumName.length() + 2];
            chars[0] = chars[chars.length - 1] = '"';
            enumName.getChars(0, enumName.length(), chars, 1);
            enumNames[value.ordinal()] = chars;
        }
        
        enumSerializer = new EnumSerializer.EnumInstanceSerializer(enumNames);
        classJSONTypeSerializerMap.put(enumClass, enumSerializer);
        return enumSerializer;
    }

    /**
     * <p> 序列化大部分场景可以使用分类序列化器即可比如Object类型,枚举类型，字符串类型；
     * <p> 日期时间类如果存在pattern等配置需要单独构建,不能缓存；
     * <p> 部分number类型比如int和long可以使用缓存好的序列化器提升一定性能；
     *
     * @param classCategory
     * @param type
     * @param jsonProperty
     * @return
     */
    protected static JSONTypeSerializer getFieldTypeSerializer(ReflectConsts.ClassCategory classCategory, Class<?> type, JsonProperty jsonProperty) {
        switch (classCategory) {
            case EnumCategory: {
                return getEnumSerializer(type);
            }
            case NumberCategory: {
                return getTypeSerializer(type);
            }
            case ObjectCategory: {
                ObjectStructureWrapper objectStructureWrapper = ObjectStructureWrapper.get(type);
                if (objectStructureWrapper.isTemporal()) {
                    return JSONTemporalSerializer.getTemporalSerializerInstance(objectStructureWrapper, jsonProperty);
                } else {
                    if(jsonProperty != null && jsonProperty.unfixedType()) {
                        // auto type
                        return new ObjectSerializer.ObjectWithTypeSerializer();
                    }
                    return getTypeSerializer(type);
                }
            }
        }
        // others by classCategory
        return JSONTypeSerializer.getTypeSerializer(classCategory, jsonProperty);
    }


    protected static JSONTypeSerializer getValueSerializer(Object value) {
        return getTypeSerializer(value.getClass());
    }

    /***
     * 序列化接口
     *
     * @param value
     * @param writer
     * @param jsonConfig
     * @param indent
     */
    protected abstract void serialize(Object value, Writer writer, JsonConfig jsonConfig, int indent) throws Exception;

    protected boolean checkWriteClassName(boolean writeClassName, Writer content, Class clazz, boolean formatOut, int indentLevel, JsonConfig jsonConfig) throws IOException {
        if(writeClassName) {
            writeType(content, clazz, formatOut, indentLevel, jsonConfig);
            return true;
        }
        return false;
    }

    void writeType(Writer content, Class clazz, boolean formatOut, int indentLevel, JsonConfig jsonConfig) throws IOException {
        writeFormatSymbolOut(content, indentLevel + 1, formatOut, jsonConfig);
        content.append("\"@c\":\"");
        content.append(clazz.getName());
        content.append("\"");
    }

    // 0、字符串序列化
    static class CharSequenceSerializer extends JSONTypeSerializer {

        static final void writeChars(Writer content, char[] chars) throws Exception {
            int len = chars.length;
            if(content instanceof JSONCharArrayWriter) {
                JSONCharArrayWriter writer = (JSONCharArrayWriter) content;
                char[] buf = writer.ensureCapacity(len * 6);
                int count = writer.count;
                buf[count++] = '"';
                for (int i = 0; i < len; ++i) {
                    char ch = chars[i];
                    if(ch > '\\' || (ch > 34 && ch < '\\')) {
                        buf[count++] = ch;
                        continue;
                    }
                    if (ch == '\\') {
                        buf[count++] = '\\';
                        buf[count++] = '\\';
                        continue;
                    }
                    if (needEscapes[ch]) {
                        String escapesChars = escapes[ch];
                        int escapesLen = escapesChars.length();
                        escapesChars.getChars(0, escapesLen, buf, count);
                        count += escapesLen;
                    } else {
                        buf[count++] = ch;
                    }
                }
                buf[count++] = '"';
                writer.count = count;
                return;
            }

            int beginIndex = 0;
            content.write('"');
            for (int i = 0; i < len; ++i) {
                char ch = chars[i];
                if (ch == '\\') {
                    int length = i - beginIndex;
                    if (length > 0) {
                        content.write(chars, beginIndex, length);
                    }
                    content.write('\\');
                    content.write('\\');
                    beginIndex = i + 1;
                    continue;
                }
                if (ch > 34) continue;
                if (needEscapes[ch]) {
                    int length = i - beginIndex;
                    if (length > 0) {
                        content.write(chars, beginIndex, length);
                    }
                    content.write(escapes[ch]);
                    beginIndex = i + 1;
                }
            }
            int size = len - beginIndex;
            content.write(chars, beginIndex, size);
            content.write('"');
        }

        protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            String strValue;
            boolean isCharSequence = value instanceof CharSequence;
            char[] chars;
            if (isCharSequence) {
                strValue = value.toString();
                chars = getChars(strValue);
            } else {
                if (value instanceof char[]) {
                    chars = (char[]) value;
                } else {
                    char c = (Character) value;
                    chars = new char[]{c};
                }
            }
            writeChars(content, chars);
        }

        // <= jdk8
        public static class StringCharsSerializer extends CharSequenceSerializer {
            @Override
            protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
                String stringVal = (String) value;
                writeChars(content, getChars(stringVal));
            }
        }

        // >= jdk9
        public static class StringBytesSerializer extends CharSequenceSerializer {

            @Override
            protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
                String stringVal = (String) value;
                byte[] bytes = (byte[]) UnsafeHelper.getStringValue(stringVal);
                if(bytes.length == stringVal.length()) {
                    content.write('"');
                    int beginIndex = 0, len = stringVal.length();
                    for (int i = 0; i < len; ++i) {
                        byte b = bytes[i];
                        if (b == '\\') {
                            int length = i - beginIndex;
                            if (length > 0) {
                                content.write(stringVal, beginIndex, length);
                            }
                            content.write('\\');
                            content.write('\\');
                            beginIndex = i + 1;
                            continue;
                        }
                        if (b > 34) continue;
                        if (needEscapes[b]) {
                            int length = i - beginIndex;
                            if (length > 0) {
                                content.write(stringVal, beginIndex, length);
                            }
                            content.write(escapes[b]);
                            beginIndex = i + 1;
                        }
                    }
                    int size = len - beginIndex;
                    content.write(stringVal, beginIndex, size);
                    content.write('"');
                } else {
                    writeChars(content, stringVal.toCharArray());
                }
            }
        }
    }

    // 1、number和bool序列化
    private static class SimpleSerializer extends JSONTypeSerializer {

        protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            content.append(value.toString());
        }

        static class SimpleNumberSerializer extends SimpleSerializer {

            protected void serializeNumber(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
                content.append(value.toString());
            }

            protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
                boolean writeAsString = jsonConfig.isWriteNumberAsString();
                if(writeAsString) {
                    content.write('"');
                }
                serializeNumber(value, content, jsonConfig, indent);
                if(writeAsString) {
                    content.write('"');
                }
            }
        }

        static class SimpleBigIntegerSerializer extends SimpleNumberSerializer {
            protected void serializeNumber(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
                BigInteger bigInteger = (BigInteger) value;
                int increment = ((bigInteger.bitLength() / 60) + 1) * 18;
                if(content instanceof JSONCharArrayWriter) {
                    JSONCharArrayWriter stringWriter = (JSONCharArrayWriter) content;
                    char[] buf = stringWriter.ensureCapacity(increment);
                    int off = stringWriter.count;
                    stringWriter.count += NumberUtils.writeBigInteger(bigInteger, buf, off);
                } else {
                    char[] chars = new char[increment];
                    int len = NumberUtils.writeBigInteger(bigInteger, chars, 0);
                    content.write(chars, 0, len);
                }
            }
        }

        // integer/long/byte/short
        static class SimpleLongSerializer extends SimpleNumberSerializer {

            protected void serializeNumber(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
                long numValue = ((Number) value).longValue();
                if (numValue == 0) {
                    content.write('0');
                    return;
                }
                if (numValue < 0) {
                    if(numValue == Long.MIN_VALUE) {
                        content.append("-9223372036854775808");
                        return;
                    }
                    numValue = -numValue;
                    content.write('-');
                }
                if(content instanceof JSONCharArrayWriter) {
                    JSONCharArrayWriter stringWriter = (JSONCharArrayWriter) content;
                    char[] buf = stringWriter.ensureCapacity(20);
                    int off = stringWriter.count;
                    stringWriter.count += NumberUtils.writePositiveLong(numValue, buf, off);
                } else {
                    char[] chars = CACHED_CHARS_24.get();
                    int len = NumberUtils.writePositiveLong(numValue, chars, 0);
                    content.write(chars, 0, len);
                }
            }
        }

        static class SimpleDoubleSerializer extends SimpleNumberSerializer {

            protected void serializeNumber(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
                double numValue = ((Number) value).doubleValue();
                if(jsonConfig.isWriteDecimalUseToString()) {
                     content.write(Double.toString(numValue));
                } else {
                    if(content instanceof JSONCharArrayWriter) {
                        JSONCharArrayWriter stringWriter = (JSONCharArrayWriter) content;
                        char[] buf = stringWriter.ensureCapacity(24);
                        int off = stringWriter.count;
                        stringWriter.count += NumberUtils.writeDouble(numValue, buf, off);
                    } else {
                        char[] chars = CACHED_CHARS_24.get();
                        int len = NumberUtils.writeDouble(numValue, chars, 0);
                        content.write(chars, 0, len);
                    }
                }
            }
        }

        static class SimpleFloatSerializer extends SimpleNumberSerializer {
            protected void serializeNumber(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
                float numValue = ((Number) value).floatValue();
                if(jsonConfig.isWriteDecimalUseToString()) {
                    content.write(Float.toString(numValue));
                } else {
                    if(content instanceof JSONCharArrayWriter) {
                        JSONCharArrayWriter stringWriter = (JSONCharArrayWriter) content;
                        char[] buf = stringWriter.ensureCapacity(24);
                        int off = stringWriter.count;
                        stringWriter.count += NumberUtils.writeFloat(numValue, buf, off);
                    } else {
                        char[] chars = CACHED_CHARS_24.get();
                        int len = NumberUtils.writeFloat(numValue, chars, 0);
                        content.write(chars, 0, len);
                    }
                }
            }
        }
    }

    // 3、日期
    private static class DateSerializer extends JSONTypeSerializer {

        protected TimeZone timeZone;
        
        final void writeDateAsTime(Date date, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            long time = date.getTime();
            NUMBER.serializeNumber(time, content, jsonConfig, indent);
        }

        @Override
        protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            Date date = (Date) value;
            if (jsonConfig.isWriteDateAsTime()) {
                writeDateAsTime(date, content, jsonConfig, indent);
                return;
            }
            TimeZone timeZone = jsonConfig.getTimezone();
            if(timeZone == null) {
                timeZone = this.timeZone;
            }
            int month, year, day, hourOfDay, minute, second;
            GeneralDate generalDate = new GeneralDate(date.getTime(), timeZone);
            year = generalDate.getYear();
            month = generalDate.getMonth();
            day = generalDate.getDay();
            hourOfDay = generalDate.getHourOfDay();
            minute = generalDate.getMinute();
            second = generalDate.getSecond();
            content.write('"');
            if (date instanceof Time) {
                writeHH_mm_SS(hourOfDay, minute, second, content);
            } else {
                writeYYYY_MM_DD_HH_mm_SS(year, month, day, hourOfDay, minute, second, content);
            }
            content.write('"');
        }

//        /***
//         * 日期序列化
//         *
//         * @param date
//         * @param timeZone
//         * @param content
//         * @throws IOException
//         * @see io.github.wycst.wast.common.beans.DateTemplate
//         */
//        private void writeDate(Date date, TimeZone timeZone, Writer content) throws IOException {
//
////            int month, year, day, hourOfDay, minute, second, millisecond;
////            GeneralDate generalDate = new GeneralDate(date.getTime(), timeZone);
////            year = generalDate.getYear();
////            month = generalDate.getMonth();
////            day = generalDate.getDay();
////            hourOfDay = generalDate.getHourOfDay();
////            minute = generalDate.getMinute();
////            second = generalDate.getSecond();
////
////            if (pattern == null) {
////                // use default date writer
////                if (date instanceof Time) {
////                    writeHH_mm_SS(hourOfDay, minute, second, content);
////                } else {
////                    writeYYYY_MM_DD_HH_mm_SS(year, month, day, hourOfDay, minute, second, content);
////                }
////                return;
////            }
////
////            millisecond = generalDate.getMillisecond();
////            // if config annotation
////            if(dateFormatter != null) {
////                content.append('"');
////                dateFormatter.formatTo(year, month, day, hourOfDay, minute, second, millisecond, content);
////                content.append('"');
////                return;
////            }
//
//            // maybe global pattern
////            boolean isAm = generalDate.isAm();
////            pattern = pattern.trim();
////            int len = pattern.length();
////            content.append('"');
////            // parse date pattern such as yyyy-MM-dd HH:mm:ss:s
////            char prevChar = '\0';
////            int count = 0;
////            // 增加一位虚拟字符进行遍历
////            for (int i = 0; i <= len; ++i) {
////                char ch = '\0';
////                if (i < len)
////                    ch = pattern.charAt(i);
////                if (ch == 'Y')
////                    ch = 'y';
////
////                if (prevChar == ch) {
////                    count++;
////                } else {
////
////                    // switch & case
////                    switch (prevChar) {
////                        case 'y': {
////                            // 年份
////                            int y2 = year % 100;
////                            if (count == 2) {
////                                // 输出2位数年份
////                                content.write(DigitTens[y2]);
////                                content.write(DigitOnes[y2]);
////                            } else {
////                                int y1 = year / 100;
////                                // 输出完整的年份
////                                content.write(DigitTens[y1]);
////                                content.write(DigitOnes[y1]);
////                                content.write(DigitTens[y2]);
////                                content.write(DigitOnes[y2]);
////                            }
////                            break;
////                        }
////                        case 'M': {
////                            // 月份
////                            content.write(DigitTens[month]);
////                            content.write(DigitOnes[month]);
////                            break;
////                        }
////                        case 'd': {
////                            content.write(DigitTens[day]);
////                            content.write(DigitOnes[day]);
////                            break;
////                        }
////                        case 'a': {
////                            // 上午/下午
////                            if (isAm) {
////                                content.append("am");
////                            } else {
////                                content.append("pm");
////                            }
////                            break;
////                        }
////                        case 'H': {
////                            // 0-23
////                            content.write(DigitTens[hourOfDay]);
////                            content.write(DigitOnes[hourOfDay]);
////                            break;
////                        }
////                        case 'h': {
////                            // 1-12 小时格式
////                            int h = hourOfDay % 12;
////                            if (h == 0)
////                                h = 12;
////                            content.write(DigitTens[h]);
////                            content.write(DigitOnes[h]);
////                            break;
////                        }
////                        case 'm': {
////                            // 分钟 0-59
////                            content.write(DigitTens[minute]);
////                            content.write(DigitOnes[minute]);
////                            break;
////                        }
////                        case 's': {
////                            // 秒 0-59
////                            content.write(DigitTens[second]);
////                            content.write(DigitOnes[second]);
////                            break;
////                        }
////                        case 'S': {
////                            // 毫秒
////                            content.write(String.valueOf(millisecond));
////                            break;
////                        }
////                        default: {
////                            // 其他输出
////                            if (prevChar != '\0') {
////                                // 输出count个 prevChar
////                                if(count == 1) {
////                                    if(prevChar == '"') {
////                                        content.append('\\');
////                                    }
////                                    content.append(prevChar);
////                                } else {
////                                    int n = count;
////                                    while (n-- > 0) {
////                                        if(prevChar == '"') {
////                                            content.append('\\');
////                                        }
////                                        content.append(prevChar);
////                                    }
////                                }
////                            }
////                        }
////                    }
////                    count = 1;
////                }
////                prevChar = ch;
////                // 计数+1
////            }
////            content.append('"');
//        }

        static class DateAsTimeSerializer extends DateSerializer {
            @Override
            protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
                Date date = (Date) value;
                writeDateAsTime(date, content, jsonConfig, indent);
            }
        }

        static class DatePatternSerializer extends DateSerializer {
            protected final DateFormatter dateFormatter;

            public DatePatternSerializer(DateFormatter dateFormatter, TimeZone timeZone) {
                this.dateFormatter = dateFormatter;
                this.timeZone = timeZone;
            }

            @Override
            protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
                Date date = (Date) value;
                TimeZone timeZone = jsonConfig.getTimezone();
                if(timeZone == null) {
                    timeZone = this.timeZone;
                }
                content.write('"');
                GeneralDate generalDate = new GeneralDate(date.getTime(), timeZone);
                writeGeneralDate(generalDate, dateFormatter, content);
                content.write('"');
            }
        }
    }

    private static class EnumSerializer extends JSONTypeSerializer {

        // use map value
        @Override
        protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            Enum enmuValue = (Enum) value;
            // ordinal
            if (jsonConfig.isWriteEnumAsOrdinal()) {
                content.append(String.valueOf(enmuValue.ordinal()));
            } else {
                String enumName = enmuValue.name();
                content.append('"');
                writeShortChars(content, getChars(enumName), 0, enumName.length());
                content.append('"');
            }
        }

        static class EnumInstanceSerializer extends EnumSerializer {

            final char[][] enumNames;
            EnumInstanceSerializer(char[][] enumNames) {
                this.enumNames = enumNames;
            }

            @Override
            protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
                Enum enmuValue = (Enum) value;
                int ordinal = enmuValue.ordinal();
                if (jsonConfig.isWriteEnumAsOrdinal()) {
                    content.append(String.valueOf(ordinal));
                } else {
                    char[] chars = enumNames[ordinal];
                    writeShortChars(content, chars, 0, chars.length);
                }
            }
        }
    }

    private static class ClassSerializer extends JSONTypeSerializer {
        protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            content.append('"').append(((Class) value).getName()).append('"');
        }
    }

    private static class AnnotationSerializer extends JSONTypeSerializer {
        protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            content.append('"').append(value.toString()).append('"');
        }
    }

    private static class BinarySerializer extends JSONTypeSerializer {

        protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            // use array
            if (jsonConfig.isBytesArrayToNative()) {
                ArraySerializer.writeArrayValue(value, content, jsonConfig, indent);
            } else {
                byte[] bytes = (byte[]) value;
                if (jsonConfig.isBytesArrayToHex()) {
                    content.append('"').append(printHexString(bytes, (char) 0)).append('"');
                } else {
                    // use Base64 encoding
                    content.append('"').append(Base64.getEncoder().encodeToString(bytes)).append('"');
                }
            }
        }
    }

    private static class ArraySerializer extends JSONTypeSerializer {

        static void writeArrayValue(Object obj, Writer content, JsonConfig jsonConfig, int indentLevel) throws Exception {
            boolean formatOut = jsonConfig.isFormatOut();
            int length = Array.getLength(obj);
            if (length == 0) {
                content.write(EMPTY_ARRAY);
            } else {
                // 数组类
                content.write('[');
                int lastLevel = indentLevel;
                boolean isFirstKey = true;
                for (int i = 0; i < length; ++i) {
                    if (isFirstKey) {
                        isFirstKey = false;
                    } else {
                        content.write(",");
                    }
                    writeFormatSymbolOut(content, lastLevel = indentLevel + 1, formatOut, jsonConfig);
                    Object value = getArrayValueAt(obj, i);  // Array.get(obj, i);
                    if (value == null) {
                        content.write(NULL);
                    } else {
                        JSONTypeSerializer valueSerializer = getValueSerializer(value);
                        valueSerializer.serialize(value, content, jsonConfig, formatOut ? indentLevel + 1 : -1);
                    }
                }
                if (lastLevel > indentLevel) {
                    writeFormatSymbolOut(content, indentLevel, formatOut, jsonConfig);
                }
                content.write(']');
            }
        }

        @Override
        protected void serialize(Object obj, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    content.write(NULL);
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            writeArrayValue(obj, content, jsonConfig, indent);
            jsonConfig.setStatus(hashcode, -1);
        }
    }

    private static class CollectionSerializer extends JSONTypeSerializer {

        private static void writeCollectionValue(Object obj, Writer content, JsonConfig jsonConfig, int indentLevel) throws Exception {
            boolean formatOut = jsonConfig.isFormatOut();
            Collection<?> collect = (Collection<?>) obj;
            if (collect.size() == 0) {
                content.write(EMPTY_ARRAY);
            } else {
                // 集合类
                content.write('[');
                int lastLevel = indentLevel;
                boolean isFirstElement = true;
                Class<?> firstElementClass = null;
                JSONTypeSerializer firstSerializer = null;
                for (Object value : collect) {
                    if (isFirstElement) {
                        isFirstElement = false;
                    } else {
                        content.write(',');
                    }
                    writeFormatSymbolOut(content, lastLevel = indentLevel + 1, formatOut, jsonConfig);
                    if (value == null) {
                        content.write(NULL);
                    } else {
                        // 此处防止重复查找序列化器小处理一下
                        Class<?> valueClass = value.getClass();
                        if(firstElementClass == null) {
                            firstElementClass = valueClass;
                            firstSerializer = getTypeSerializer(valueClass);
                            firstSerializer.serialize(value, content, jsonConfig, formatOut ? indentLevel + 1 : -1);
                        } else {
                            if(valueClass == firstElementClass) {
                                firstSerializer.serialize(value, content, jsonConfig, formatOut ? indentLevel + 1 : -1);
                            } else {
                                getTypeSerializer(valueClass).serialize(value, content, jsonConfig, formatOut ? indentLevel + 1 : -1);
                            }
                        }
                    }
                }
                if (lastLevel > indentLevel) {
                    writeFormatSymbolOut(content, indentLevel, formatOut, jsonConfig);
                }
                content.write(']');
            }
        }

        protected void serialize(Object obj, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    content.write(NULL);
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            writeCollectionValue(obj, content, jsonConfig, indent);
            jsonConfig.setStatus(hashcode, -1);
        }
    }

    private static class MapSerializer extends JSONTypeSerializer {

        private static void writeMapValue(Object obj, Writer content, JsonConfig jsonConfig, int indentLevel) throws Exception {
            boolean formatOut = jsonConfig.isFormatOut();
            // map call get
            Map<Object, Object> map = (Map<Object, Object>) obj;
            if (map.size() == 0) {
                content.write(EMPTY_OBJECT);
            } else {
                content.write('{');
                // 遍历map
                Set<Map.Entry<Object, Object>> entrySet = map.entrySet();
                boolean isFirstKey = true;
                for (Map.Entry<Object, Object> entry : entrySet) {
                    Object key = entry.getKey();
                    Object value = entry.getValue();

                    if (isFirstKey) {
                        isFirstKey = false;
                    } else {
                        content.write(',');
                    }
                    writeFormatSymbolOut(content, indentLevel + 1, formatOut, jsonConfig);

                    if (jsonConfig.isAllowUnquotedMapKey() && (key == null || key instanceof Number)) {
                        content.append(String.valueOf(key)).write(':');
                    } else {
                        if(content instanceof JSONCharArrayWriter) {
                            JSONCharArrayWriter writer = (JSONCharArrayWriter) content;
                            String stringKey = key == null ? "null" : key.toString();
                            int length = stringKey.length();
                            char[] buf = writer.ensureCapacity(length + 3);
                            int off = writer.count;
                            buf[off++] = '"';
                            if(!EnvUtils.JDK_9_ABOVE) {
                                char[] chars = getChars(stringKey);
                                for (int i = 0 ; i < length; ++i) {
                                    buf[off++] = chars[i];
                                }
                            } else {
                                stringKey.getChars(0, length, buf, off);
                                off += length;
                            }
                            buf[off++] = '"';
                            buf[off++] = ':';
                            writer.count = off;
                        } else {
                            // jdk9 +
                            content.append('"').append(key.toString()).append('"').append(':');
                        }
                    }
                    if (value == null) {
                        content.write(NULL);
                    } else {
                        JSONTypeSerializer valueSerializer = getValueSerializer(value);
                        valueSerializer.serialize(value, content, jsonConfig, formatOut ? indentLevel + 1 : -1);
                    }
                }
                writeFormatSymbolOut(content, indentLevel, formatOut, jsonConfig);
                content.write('}');
            }
        }

        protected void serialize(Object obj, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    content.write(NULL);
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            writeMapValue(obj, content, jsonConfig, indent);
            jsonConfig.setStatus(hashcode, -1);
        }
    }

    private static class ObjectSerializer extends JSONTypeSerializer {


        protected void serialize(Object obj, Writer content, JsonConfig jsonConfig, int indentLevel) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    content.write(NULL);
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }

            boolean writeFullProperty = jsonConfig.isFullProperty();
            boolean formatOut = jsonConfig.isFormatOut();
            boolean writeClassName = jsonConfig.isWriteClassName();
            content.write('{');

            Class clazz = obj.getClass();
            ObjectStructureWrapper classStructureWrapper = getObjectStructureWrapper(clazz);
            boolean isFirstKey = !checkWriteClassName(writeClassName, content, clazz, formatOut, indentLevel, jsonConfig);
            FieldSerializer[] fieldSerializers = classStructureWrapper.getFieldSerializers(jsonConfig.isUseFields());

            boolean skipGetterOfNoExistField = jsonConfig.isSkipGetterOfNoneField();
            boolean camelCaseToUnderline = jsonConfig.isCamelCaseToUnderline();
            for (FieldSerializer fieldSerializer : fieldSerializers) {
                GetterInfo getterInfo = fieldSerializer.getGetterInfo();
                if (!getterInfo.existField() && skipGetterOfNoExistField) {
                    continue;
                }
                Object value = getterInfo.invoke(obj);
                if (value == null && !writeFullProperty)
                    continue;

                if (isFirstKey) {
                    isFirstKey = false;
                } else {
                    content.write(',');
                }
                writeFormatSymbolOut(content, indentLevel + 1, formatOut, jsonConfig);
                if (value == null) {
                    if (camelCaseToUnderline) {
                        content.append('"').append(getterInfo.getUnderlineName()).append("\":null");
                    } else {
                        fieldSerializer.writeFieldKey(content, 0, 0);
                    }
                } else {
                    if (camelCaseToUnderline) {
                        content.append('"').append(getterInfo.getUnderlineName()).append("\":");
                    } else {
                        fieldSerializer.writeFieldKey(content, 0, 4);
                    }
                    // Custom serialization
                    JSONTypeSerializer serializer = fieldSerializer.getSerializer();
                    serializer.serialize(value, content, jsonConfig, formatOut ? indentLevel + 1 : -1);
                }
            }
            writeFormatSymbolOut(content, indentLevel, formatOut, jsonConfig);
            content.write('}');

            jsonConfig.setStatus(hashcode, -1);
        }

        ObjectStructureWrapper getObjectStructureWrapper(Class clazz) {
            return ObjectStructureWrapper.get(clazz);
        }

        static class ObjectWithTypeSerializer extends ObjectSerializer {
            protected void serialize(Object obj, Writer content, JsonConfig jsonConfig, int indentLevel) throws Exception {
                ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(obj.getClass());
                if (classCategory != ReflectConsts.ClassCategory.ObjectCategory) {
                    getTypeSerializer(classCategory, null).serialize(obj, content, jsonConfig, indentLevel);
                } else {
                    super.serialize(obj, content, jsonConfig, indentLevel);
                }
            }

            protected boolean checkWriteClassName(boolean writeClassName, Writer content, Class clazz, boolean formatOut, int indentLevel, JsonConfig jsonConfig) throws IOException {
                writeType(content, clazz, formatOut, indentLevel, jsonConfig);
                return true;
            }
        }

        static class ObjectWrapperSerializer extends ObjectSerializer {
            final ObjectStructureWrapper classStructureWrapper;
            final Class objectCls;

            ObjectWrapperSerializer(Class cls) {
                this.objectCls = cls;
                classStructureWrapper = ObjectStructureWrapper.get(cls);
            }

            ObjectStructureWrapper getObjectStructureWrapper(Class clazz) {
                return clazz == objectCls ? classStructureWrapper : ObjectStructureWrapper.get(clazz);
            }
        }
    }

    private static class ANYSerializer extends JSONTypeSerializer {
        @Override
        protected void serialize(Object value, Writer writer, JsonConfig jsonConfig, int indent) throws Exception {
            Class<?> clazz = value.getClass();
            if (clazz == Object.class) {
                writer.write(EMPTY_OBJECT);
            } else {
                ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(clazz);
                int ordinal = classCategory.ordinal();
                JSONTypeSerializer.TYPE_SERIALIZERS[ordinal].serialize(value, writer, jsonConfig, indent);
            }
        }
    }

    protected static void writeDate(int year, int month, int day, int hourOfDay, int minute, int second, int millisecond, DateFormatter dateFormatter, Writer content) throws Exception {
        if (content instanceof JSONCharArrayWriter) {
            JSONCharArrayWriter writer = (JSONCharArrayWriter) content;
            char[] buf = writer.ensureCapacity(dateFormatter.getEstimateSize());
            int off = writer.count;
            off += dateFormatter.write(year, month, day, hourOfDay, minute, second, millisecond, buf, off);
            writer.count = off;
            return;
        }
        dateFormatter.formatTo(year, month, day, hourOfDay, minute, second, millisecond, content);
    }

    protected static void writeGeneralDate(GeneralDate generalDate, DateFormatter dateFormatter, Writer content) throws Exception {
        int month, year, day, hourOfDay, minute, second, millisecond;
        year = generalDate.getYear();
        month = generalDate.getMonth();
        day = generalDate.getDay();
        hourOfDay = generalDate.getHourOfDay();
        minute = generalDate.getMinute();
        second = generalDate.getSecond();
        millisecond = generalDate.getMillisecond();
        writeDate(year, month, day, hourOfDay, minute, second, millisecond, dateFormatter, content);
    }

    protected static final void writeYYYY_MM_dd_T_HH_mm_ss_SSS(Writer writer,
                                                        int year,
                                                        int month,
                                                        int day,
                                                        int hour,
                                                        int minute,
                                                        int second,
                                                        int millisecond) throws Exception {
        int y1 = year / 100, y2 = year - y1 * 100;
        char s1 = (char) (millisecond / 100 + 48);
        int v = millisecond % 100;
        if (writer instanceof JSONCharArrayWriter) {
            JSONCharArrayWriter stringWriter = (JSONCharArrayWriter) writer;
            char[] buf = stringWriter.ensureCapacity(23);
            int off = stringWriter.count;
            buf[off++] = DigitTens[y1];
            buf[off++] = DigitOnes[y1];
            buf[off++] = DigitTens[y2];
            buf[off++] = DigitOnes[y2];
            buf[off++] = '-';
            buf[off++] = DigitTens[month];
            buf[off++] = DigitOnes[month];
            buf[off++] = '-';
            buf[off++] = DigitTens[day];
            buf[off++] = DigitOnes[day];
            buf[off++] = 'T';
            buf[off++] = DigitTens[hour];
            buf[off++] = DigitOnes[hour];
            buf[off++] = ':';
            buf[off++] = DigitTens[minute];
            buf[off++] = DigitOnes[minute];
            buf[off++] = ':';
            buf[off++] = DigitTens[second];
            buf[off++] = DigitOnes[second];
            buf[off++] = '.';
            buf[off++] = s1;
            buf[off++] = DigitTens[v];
            buf[off++] = DigitOnes[v];
            stringWriter.count = off;
            return;
        }
        writer.write(DigitTens[y1]);
        writer.write(DigitOnes[y1]);
        writer.write(DigitTens[y2]);
        writer.write(DigitOnes[y2]);
        writer.write('-');
        writer.write(DigitTens[month]);
        writer.write(DigitOnes[month]);
        writer.write('-');
        writer.write(DigitTens[day]);
        writer.write(DigitOnes[day]);
        writer.write('T');
        writer.write(DigitTens[hour]);
        writer.write(DigitOnes[hour]);
        writer.write(':');
        writer.write(DigitTens[minute]);
        writer.write(DigitOnes[minute]);
        writer.write(':');
        writer.write(DigitTens[second]);
        writer.write(DigitOnes[second]);
        writer.write('.');
        writer.write(s1);
        writer.write(DigitTens[v]);
        writer.write(DigitOnes[v]);
    }

    // "yyyy-MM-dd HH:mm:ss"
    protected static void writeYYYY_MM_DD_HH_mm_SS(int year, int month, int day, int hourOfDay, int minute, int second, Writer content) throws IOException {

        int y1 = year / 100, y2 = year - y1 * 100;
        if (content instanceof JSONCharArrayWriter) {
            JSONCharArrayWriter writer = (JSONCharArrayWriter) content;
            writer.ensureCapacity(19);
            writer.writeDirectly(DigitTens[y1]);
            writer.writeDirectly(DigitOnes[y1]);
            writer.writeDirectly(DigitTens[y2]);
            writer.writeDirectly(DigitOnes[y2]);
            writer.writeDirectly('-');
            writer.writeDirectly(DigitTens[month]);
            writer.writeDirectly(DigitOnes[month]);
            writer.writeDirectly('-');
            writer.writeDirectly(DigitTens[day]);
            writer.writeDirectly(DigitOnes[day]);
            writer.writeDirectly(' ');
            writer.writeDirectly(DigitTens[hourOfDay]);
            writer.writeDirectly(DigitOnes[hourOfDay]);
            writer.writeDirectly(':');
            writer.writeDirectly(DigitTens[minute]);
            writer.writeDirectly(DigitOnes[minute]);
            writer.writeDirectly(':');
            writer.writeDirectly(DigitTens[second]);
            writer.writeDirectly(DigitOnes[second]);
            return;
        }
        char[] chars = CACHED_CHARS_DATE_19.get();
        chars[1] = DigitTens[y1];
        chars[2] = DigitOnes[y1];
        chars[3] = DigitTens[y2];
        chars[4] = DigitOnes[y2];

        chars[6] = DigitTens[month];
        chars[7] = DigitOnes[month];

        chars[9] = DigitTens[day];
        chars[10] = DigitOnes[day];

        chars[12] = DigitTens[hourOfDay];
        chars[13] = DigitOnes[hourOfDay];

        chars[15] = DigitTens[minute];
        chars[16] = DigitOnes[minute];

        chars[18] = DigitTens[second];
        chars[19] = DigitOnes[second];
        content.write(chars);
    }

    protected static void writeYYYY_MM_DD(int year, int month, int day, Writer content) throws IOException {
        int y1 = year / 100, y2 = year - y1 * 100;
        if (content instanceof JSONCharArrayWriter) {
            JSONCharArrayWriter writer = (JSONCharArrayWriter) content;
            writer.ensureCapacity(10);
            writer.writeDirectly(DigitTens[y1]);
            writer.writeDirectly(DigitOnes[y1]);
            writer.writeDirectly(DigitTens[y2]);
            writer.writeDirectly(DigitOnes[y2]);
            writer.writeDirectly('-');
            writer.writeDirectly(DigitTens[month]);
            writer.writeDirectly(DigitOnes[month]);
            writer.writeDirectly('-');
            writer.writeDirectly(DigitTens[day]);
            writer.writeDirectly(DigitOnes[day]);
            return;
        }
        char[] chars = CACHED_CHARS_DATE_19.get();
        chars[1] = DigitTens[y1];
        chars[2] = DigitOnes[y1];
        chars[3] = DigitTens[y2];
        chars[4] = DigitOnes[y2];

        chars[6] = DigitTens[month];
        chars[7] = DigitOnes[month];

        chars[9] = DigitTens[day];
        chars[10] = DigitOnes[day];
        // yyyy-MM-dd
        content.write(chars, 0, 10);
    }

    // "HH:mm:ss"
    protected static void writeHH_mm_SS(int hourOfDay, int minute, int second, Writer content) throws IOException {
        if (content instanceof JSONCharArrayWriter) {
            JSONCharArrayWriter writer = (JSONCharArrayWriter) content;
            writer.ensureCapacity(10);
            writer.writeDirectly(DigitTens[hourOfDay]);
            writer.writeDirectly(DigitOnes[hourOfDay]);
            writer.writeDirectly(':');
            writer.writeDirectly(DigitTens[minute]);
            writer.writeDirectly(DigitOnes[minute]);
            writer.writeDirectly(':');
            writer.writeDirectly(DigitTens[second]);
            writer.writeDirectly(DigitOnes[second]);
            return;
        }
        content.write(DigitTens[hourOfDay]);
        content.write(DigitOnes[hourOfDay]);
        content.write(':');
        content.write(DigitTens[minute]);
        content.write(DigitOnes[minute]);
        content.write(':');
        content.write(DigitTens[second]);
        content.write(DigitOnes[second]);
    }

    protected static void writeShortChars(Writer content, char[] chars, int offset, int len) throws IOException {
        if(content instanceof JSONCharArrayWriter) {
            JSONCharArrayWriter writer = (JSONCharArrayWriter) content;
            char[] buf = writer.ensureCapacity(len);
            int off = writer.count;
            for (int i = 0 ; i < len; ++i) {
                buf[off++] = chars[offset++];
            }
            writer.count = off;
        } else {
            content.write(chars, offset, len);
        }
    }
}
