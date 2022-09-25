package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.tools.Base64;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.options.JsonConfig;
import io.github.wycst.wast.json.reflect.FieldSerializer;
import io.github.wycst.wast.json.reflect.ObjectStructureWrapper;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    protected static final CharSequenceSerializer STRING = new CharSequenceSerializer();

    static {
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.CharSequence.ordinal()] = STRING;
        JSONTypeSerializer SIMPLE = new SimpleSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.NumberCategory.ordinal()] = SIMPLE;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.BoolCategory.ordinal()] = SIMPLE;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.DateCategory.ordinal()] = new DateSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ClassCategory.ordinal()] = new ClassSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.EnumCategory.ordinal()] = new EnumSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.AnnotationCategory.ordinal()] = new AnnotationSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.Binary.ordinal()] = new BinarySerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ArrayCategory.ordinal()] = new ArraySerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.CollectionCategory.ordinal()] = new CollectionSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.MapCategory.ordinal()] = new MapSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ObjectCategory.ordinal()] = new ObjectSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ANY.ordinal()] = new ANYSerializer();

        JSONTypeSerializer integerSerializer = new SimpleSerializer.SimpleIntegerSerializer();
        putTypeSerializer(integerSerializer, byte.class, Byte.class, short.class, Short.class, int.class, Integer.class, long.class, Long.class);
    }

    private static void putTypeSerializer(JSONTypeSerializer typeSerializer, Class... types) {
        for (Class type : types) {
            classJSONTypeSerializerMap.put(type, typeSerializer);
        }
    }

    protected static JSONTypeSerializer getTypeSerializer(ReflectConsts.ClassCategory classCategory, JsonProperty jsonProperty) {
        int ordinal = classCategory.ordinal();
        if (classCategory == ReflectConsts.ClassCategory.DateCategory) {
            if (jsonProperty != null)
                return new DateSerializer.DateInstanceSerializer(jsonProperty);
        }
        if (classCategory == ReflectConsts.ClassCategory.NonInstance) {
            // write classname
            return new ObjectSerializer.ObjectWithTypeSerializer();
        }
        return TYPE_SERIALIZERS[ordinal];
    }

    protected static JSONTypeSerializer getTypeSerializer(Class<?> cls) {
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
        if (classCategory == ReflectConsts.ClassCategory.NumberCategory) {
            // number type use cache
            return JSONTypeSerializer.getTypeSerializer(type);
        } else if (classCategory == ReflectConsts.ClassCategory.ObjectCategory) {
            ObjectStructureWrapper objectStructureWrapper = ObjectStructureWrapper.get(type);
            if (objectStructureWrapper.isTemporal()) {
                return JSONTemporalSerializer.getTemporalSerializerInstance(objectStructureWrapper, jsonProperty);
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

    // 0、字符串序列化
    static class CharSequenceSerializer extends JSONTypeSerializer {

        protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            String strValue;
            boolean isCharSequence = value instanceof CharSequence;
            char[] chars;
            int len;
            if (isCharSequence) {
                strValue = value.toString();
                len = strValue.length();
                chars = getChars(strValue);
            } else {
                if (value instanceof char[]) {
                    chars = (char[]) value;
                    len = chars.length;
                } else {
                    char c = (Character) value;
                    chars = new char[]{c};
                    len = 1;
                }
            }

            content.append('"');
            int beginIndex = 0;

            if (!jsonConfig.isDisableEscapeValidate()) {
                // It takes too much time to determine whether there is an escape character
                for (int i = 0; i < len; ++i) {
                    char ch = chars[i];
                    if (ch == '\\') {
                        int length = i - beginIndex;
                        if (length > 0) {
                            content.write(chars, beginIndex, length);
                        }
                        content.append('\\').append('\\');
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
            }
            content.write(chars, beginIndex, len - beginIndex);
            content.append('"');
        }
    }

    // 1、number和bool序列化
    private static class SimpleSerializer extends JSONTypeSerializer {

        protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            content.append(value.toString());
        }

        // integer/long/byte/short
        static class SimpleIntegerSerializer extends SimpleSerializer {

            static int stringSize(long x) {
                long p = 10;
                for (int i = 1; i < 19; ++i) {
                    if (x < p)
                        return i;
                    p = (p << 3) + (p << 1);
                }
                return 19;
            }

            protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
                long numValue = ((Number) value).longValue();
                if (numValue == 0) {
                    content.append('0');
                    return;
                }

                int size, pos;
                if (numValue < 0) {
                    numValue = -numValue;
                    content.append('-');
                }
                size = stringSize(numValue);
                pos = size;
                if(content instanceof JSONStringWriter) {
                    JSONStringWriter stringWriter = (JSONStringWriter) content;
                    int count = stringWriter.addLength(size);
                    if ((pos & 1) == 1) {
                        long v = numValue / 10;
                        int digit = (int) (numValue - ((v << 3) + (v << 1)));
                        stringWriter.setCharAt(--count,  DigitOnes[digit]);
                        numValue = v;
                    }
                    while (numValue > 0) {
                        long v = numValue / 100;
                        int digits = (int) (numValue - ((v << 6) + (v << 5) + (v << 2)));
                        stringWriter.setCharAt(--count, DigitOnes[digits]);
                        stringWriter.setCharAt(--count, DigitTens[digits]);
                        numValue = v;
                    }
                    return;
                }
                // 20位
                char[] contextChars = CachedChars_20.get();
                if ((pos & 1) == 1) {
                    long v = numValue / 10;
                    int digit = (int) (numValue - ((v << 3) + (v << 1)));
                    contextChars[--pos] = DigitOnes[digit];
                    numValue = v;
                }

                while (numValue > 0) {
                    long v = numValue / 100;
                    int digits = (int) (numValue - ((v << 6) + (v << 5) + (v << 2)));
                    contextChars[--pos] = DigitOnes[digits];
                    contextChars[--pos] = DigitTens[digits];
                    numValue = v;
                }
                content.write(contextChars, pos, size);
            }
        }

    }

    // 3、日期
    private static class DateSerializer extends JSONTypeSerializer {

        protected boolean writeDateAsTime;
        protected String pattern;
        protected String timezone;

        @Override
        protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            Date date = (Date) value;
            if (writeDateAsTime || jsonConfig.isWriteDateAsTime()) {
                content.append(String.valueOf(date.getTime()));
                return;
            }
            String pattern = this.pattern;
            String timezone = this.timezone;
            if (pattern == null) {
                pattern = jsonConfig.getDateFormatPattern();
            }
            if (timezone == null) {
                timezone = jsonConfig.getTimezone();
            }
            writeDate(date, pattern, timezone, content);
        }

        /***
         * 日期序列化
         *
         * @param date
         * @param pattern
         * @param timezone
         * @param content
         * @throws IOException
         */
        private static void writeDate(Date date, String pattern, String timezone, Writer content) throws IOException {

            int month, year, day, hourOfDay, minute, second, millisecond;
            GeneralDate generalDate = new GeneralDate(date.getTime(), timezone);
            year = generalDate.getYear();
            month = generalDate.getMonth();
            day = generalDate.getDay();
            hourOfDay = generalDate.getHourOfDay();
            minute = generalDate.getMinute();
            second = generalDate.getSecond();

            if (pattern == null) {
                if (date instanceof Time) {
                    writeDefaultFormatTime(hourOfDay, minute, second, content);
                } else {
                    writeDefaultFormatDate(year, month, day, hourOfDay, minute, second, content);
                }
                return;
            }

            millisecond = generalDate.getMillisecond();
            boolean isAm = generalDate.isAm();

            pattern = pattern.trim();
            int len = pattern.length();
            content.append('"');
            // 解析pattern
            // yy-MM-dd HH:mm:ss:s
            char prevChar = '\0';
            int count = 0;
            // 增加一位虚拟字符进行遍历
            for (int i = 0; i <= len; ++i) {
                char ch = '\0';
                if (i < len)
                    ch = pattern.charAt(i);
                if (ch == 'Y')
                    ch = 'y';

                if (prevChar == ch) {
                    count++;
                } else {

                    // switch & case
                    switch (prevChar) {
                        case 'y': {
                            // 年份
                            int y2 = year % 100;
                            if (count == 2) {
                                // 输出2位数年份
                                content.write(DigitTens[y2]);
                                content.write(DigitOnes[y2]);
                            } else {
                                int y1 = year / 100;
                                // 输出完整的年份
                                content.write(DigitTens[y1]);
                                content.write(DigitOnes[y1]);
                                content.write(DigitTens[y2]);
                                content.write(DigitOnes[y2]);
                            }
                            break;
                        }
                        case 'M': {
                            // 月份
                            content.write(DigitTens[month]);
                            content.write(DigitOnes[month]);
                            break;
                        }
                        case 'd': {
                            content.write(DigitTens[day]);
                            content.write(DigitOnes[day]);
                            break;
                        }
                        case 'a': {
                            // 上午/下午
                            if (isAm) {
                                content.append("am");
                            } else {
                                content.append("pm");
                            }
                            break;
                        }
                        case 'H': {
                            // 0-23
                            content.write(DigitTens[hourOfDay]);
                            content.write(DigitOnes[hourOfDay]);
                            break;
                        }
                        case 'h': {
                            // 1-12 小时格式
                            int h = hourOfDay % 12;
                            if (h == 0)
                                h = 12;
                            content.write(DigitTens[h]);
                            content.write(DigitOnes[h]);
                            break;
                        }
                        case 'm': {
                            // 分钟 0-59
                            content.write(DigitTens[minute]);
                            content.write(DigitOnes[minute]);
                            break;
                        }
                        case 's': {
                            // 秒 0-59
                            content.write(DigitTens[second]);
                            content.write(DigitOnes[second]);
                            break;
                        }
                        case 'S': {
                            // 毫秒
                            content.write(String.valueOf(millisecond));
                            break;
                        }
                        default: {
                            // 其他输出
                            if (prevChar != '\0') {
                                // 输出count个 prevChar
                                if(count == 1) {
                                    if(prevChar == '"') {
                                        content.append('\\');
                                    }
                                    content.append(prevChar);
                                } else {
                                    int n = count;
                                    while (n-- > 0) {
                                        if(prevChar == '"') {
                                            content.append('\\');
                                        }
                                        content.append(prevChar);
                                    }
                                }
                            }
                        }
                    }
                    count = 1;
                }
                prevChar = ch;
                // 计数+1
            }
            content.append('"');
        }

        // "yyyy-MM-dd HH:mm:ss"
        private static void writeDefaultFormatDate(int year, int month, int day, int hourOfDay, int minute, int second, Writer content) throws IOException {

            int y1 = year / 100, y2 = year - y1 * 100;
            if (content instanceof JSONStringWriter) {
                JSONStringWriter writer = (JSONStringWriter) content;
                writer.ensureCapacity(21);
                writer.writeDirectly('"');
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
                writer.writeDirectly('"');
                return;
            }

            char[] chars = CachedCharsDate_19.get();
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

        // "HH:mm:ss"
        private static void writeDefaultFormatTime(int hourOfDay, int minute, int second, Writer content) throws IOException {

            if (content instanceof JSONStringWriter) {
                JSONStringWriter writer = (JSONStringWriter) content;
                writer.ensureCapacity(10);
                writer.writeDirectly('"');
                writer.writeDirectly(DigitTens[hourOfDay]);
                writer.writeDirectly(DigitOnes[hourOfDay]);
                writer.writeDirectly(':');
                writer.writeDirectly(DigitTens[minute]);
                writer.writeDirectly(DigitOnes[minute]);
                writer.writeDirectly(':');
                writer.writeDirectly(DigitTens[second]);
                writer.writeDirectly(DigitOnes[second]);
                writer.writeDirectly('"');
                return;
            }

            content.write('"');
            content.write(DigitTens[hourOfDay]);
            content.write(DigitOnes[hourOfDay]);
            content.write(':');
            content.write(DigitTens[minute]);
            content.write(DigitOnes[minute]);
            content.write(':');
            content.write(DigitTens[second]);
            content.write(DigitOnes[second]);
            content.write('"');
        }

        static class DateInstanceSerializer extends DateSerializer {
            public DateInstanceSerializer(JsonProperty jsonProperty) {
                writeDateAsTime = jsonProperty.asTimestamp();
                String pattern = jsonProperty.pattern().trim();
                String timezone = jsonProperty.timezone().trim();
                if (pattern.length() > 0) {
                    this.pattern = pattern;
                }
                if (timezone.length() > 0) {
                    this.timezone = timezone;
                }
            }
        }
    }

    private static class EnumSerializer extends JSONTypeSerializer {

        @Override
        protected void serialize(Object value, Writer content, JsonConfig jsonConfig, int indent) throws Exception {
            Enum enmuValue = (Enum) value;
            // ordinal
            if (jsonConfig.isWriteEnumAsOrdinal()) {
                content.append(String.valueOf(enmuValue.ordinal()));
            } else {
                // name
                content.append('"').append(enmuValue.name()).append('"');
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
                        content.append(",");
                    }
                    writeFormatSymbolOut(content, lastLevel = indentLevel + 1, formatOut);
                    Object value = getArrayValueAt(obj, i);  // Array.get(obj, i);
                    if (value == null) {
                        content.write(NULL);
                    } else {
                        JSONTypeSerializer valueSerializer = getValueSerializer(value);
                        valueSerializer.serialize(value, content, jsonConfig, formatOut ? indentLevel + 1 : -1);
                    }
                }
                if (lastLevel > indentLevel) {
                    writeFormatSymbolOut(content, indentLevel, formatOut);
                }
                content.append(']');
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
                for (Object value : collect) {
                    if (isFirstElement) {
                        isFirstElement = false;
                    } else {
                        content.write(',');
                    }
                    writeFormatSymbolOut(content, lastLevel = indentLevel + 1, formatOut);
                    if (value == null) {
                        content.write(NULL);
                    } else {
                        JSONTypeSerializer valueSerializer = getValueSerializer(value);
                        valueSerializer.serialize(value, content, jsonConfig, formatOut ? indentLevel + 1 : -1);
                    }
                }
                if (lastLevel > indentLevel) {
                    writeFormatSymbolOut(content, indentLevel, formatOut);
                }
                content.append(']');
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
                Iterator<Map.Entry<Object, Object>> iterator = map.entrySet().iterator();
                boolean isFirstKey = true;
                while (iterator.hasNext()) {
                    Map.Entry<Object, Object> entry = iterator.next();
                    Object key = entry.getKey();
                    Object value = entry.getValue();

                    if (isFirstKey) {
                        isFirstKey = false;
                    } else {
                        content.append(',');
                    }
                    writeFormatSymbolOut(content, indentLevel + 1, formatOut);

                    if (jsonConfig.isAllowUnquotedMapKey() && (key == null || key instanceof Number)) {
                        content.append(String.valueOf(key)).append(':');
                    } else {
                        content.append('"').append(key.toString()).append('"').append(':');
                    }
                    if (value == null) {
                        content.write(NULL);
                    } else {
                        JSONTypeSerializer valueSerializer = getValueSerializer(value);
                        valueSerializer.serialize(value, content, jsonConfig, formatOut ? indentLevel + 1 : -1);
                    }
                }
                writeFormatSymbolOut(content, indentLevel, formatOut);
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

            content.append('{');

            Class clazz = obj.getClass();
            ObjectStructureWrapper classStructureWrapper = getObjectStructureWrapper(clazz);
            boolean isFirstKey = isWriteType(content, clazz, formatOut, indentLevel);
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

                char[] quotBuffers = fieldSerializer.getFixedFieldName();
                if (isFirstKey) {
                    isFirstKey = false;
                } else {
                    content.append(",");
                }
                writeFormatSymbolOut(content, indentLevel + 1, formatOut);
                if (value == null) {
                    if (camelCaseToUnderline) {
                        content.append('"').append(getterInfo.getUnderlineName()).append("\":null");
                    } else {
                        content.write(quotBuffers, 1, quotBuffers.length - 2);
                    }
                } else {
                    if (camelCaseToUnderline) {
                        content.append('"').append(getterInfo.getUnderlineName()).append("\":");
                    } else {
                        content.write(quotBuffers, 1, quotBuffers.length - 6);
                    }
                    // Custom serialization
                    JSONTypeSerializer serializer = fieldSerializer.getSerializer();
                    serializer.serialize(value, content, jsonConfig, formatOut ? indentLevel + 1 : -1);
                }
            }
            writeFormatSymbolOut(content, indentLevel, formatOut);
            content.append('}');

            jsonConfig.setStatus(hashcode, -1);
        }

        ObjectStructureWrapper getObjectStructureWrapper(Class clazz) {
            return ObjectStructureWrapper.get(clazz);
        }

        boolean isWriteType(Writer content, Class clazz, boolean formatOut, int indentLevel) throws IOException {
            return true;
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

            boolean isWriteType(Writer content, Class clazz, boolean formatOut, int indentLevel) throws IOException {
                writeFormatSymbolOut(content, indentLevel + 1, formatOut);
                content.append("\"@c\":\"");
                content.append(clazz.getName());
                content.append("\"");
                return false;
            }
        }

        static class ObjectWrapperSerializer extends ObjectSerializer {
            final ObjectStructureWrapper classStructureWrapper;

            ObjectWrapperSerializer(Class cls) {
                classStructureWrapper = ObjectStructureWrapper.get(cls);
            }

            ObjectStructureWrapper getObjectStructureWrapper(Class clazz) {
                return classStructureWrapper;
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
}
