package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.DateFormatter;
import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.common.utils.StringUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author: wangy
 * @Date: 2022/6/18 5:59
 * @Description:
 */
@SuppressWarnings({"all"})
public abstract class JSONTypeSerializer extends JSONGeneral {

    final static Map<Class<?>, JSONTypeSerializer> GLOBAL_SERIALIZERS = new HashMap<Class<?>, JSONTypeSerializer>(32);
    static final JSONTypeSerializer SIMPLE = new SimpleImpl();
    static final JSONTypeSerializer DATE = new DateImpl();
    static final JSONTypeSerializer CLASS = new ClassImpl();
    static final CharSequenceImpl CHAR_SEQUENCE = new CharSequenceImpl();
    static final CharSequenceImpl CHAR_SEQUENCE_STRING = EnvUtils.JDK_9_PLUS ? new StringJDK9PlusImpl() : new StringJDK8Impl();
    static final JSONTypeSerializer TO_STRING = new ToStringImpl();
    static final EnumImpl ENUM = new EnumImpl();
    static final JSONTypeSerializer ANNOTATION = TO_STRING;
    static final BinaryImpl BINARY = new BinaryImpl();
    static final SimpleNumberImpl NUMBER = new SimpleNumberImpl();
    static final BigIntegerImpl NUMBER_BIG_INTEGER = new BigIntegerImpl();
    static final SimpleNumberImpl NUMBER_LONG = new LongImpl();
    static final SimpleImpl NUMBER_INTEGER = new IntegerImpl();
    static final SimpleImpl NUMBER_DOUBLE = new DoubleImpl();
    static final SimpleImpl NUMBER_FLOAT = new FloatImpl();
    static final ArrayImpl ARRAY_STRING = new ArrayStringImpl();
    static final ArrayImpl ARRAY_PRIMITIVE_DOUBLE = new ArrayPrimitiveImpl(NUMBER_DOUBLE, ReflectConsts.PrimitiveType.PrimitiveDouble);
    static final ArrayImpl ARRAY_PRIMITIVE_BYTE = new ArrayPrimitiveImpl(NUMBER_INTEGER, ReflectConsts.PrimitiveType.PrimitiveByte);
    static final ArrayImpl ARRAY_PRIMITIVE_SHORT = new ArrayPrimitiveImpl(NUMBER_INTEGER, ReflectConsts.PrimitiveType.PrimitiveShort);
    static final ArrayImpl ARRAY_PRIMITIVE_FLOAT = new ArrayPrimitiveImpl(NUMBER_FLOAT, ReflectConsts.PrimitiveType.PrimitiveFloat);
    static final ArrayImpl ARRAY_PRIMITIVE_BOOLEAN = new ArrayPrimitiveImpl(SIMPLE, ReflectConsts.PrimitiveType.PrimitiveBoolean);
    static final ArrayImpl ARRAY_PRIMITIVE_INTEGER = new ArrayPrimitiveIntImpl();
    static final ArrayImpl ARRAY_PRIMITIVE_LONG = new ArrayPrimitiveLongImpl();
    static final JSONTypeSerializer DATE_AS_TIME_SERIALIZER = new DateAsTimeImpl();
    static final JSONTypeSerializer TO_NULL = new JSONTypeSerializer() {
        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            writer.writeNull();
        }
    };

    static {
        putTypeSerializer(GLOBAL_SERIALIZERS, CHAR_SEQUENCE_STRING, String.class);
        putTypeSerializer(GLOBAL_SERIALIZERS, NUMBER_BIG_INTEGER, BigInteger.class);
        putTypeSerializer(GLOBAL_SERIALIZERS, NUMBER_DOUBLE, double.class, Double.class);
        putTypeSerializer(GLOBAL_SERIALIZERS, NUMBER_FLOAT, float.class, Float.class);
        putTypeSerializer(GLOBAL_SERIALIZERS, NUMBER_LONG, long.class, Long.class, AtomicLong.class);
        putTypeSerializer(GLOBAL_SERIALIZERS, NUMBER_INTEGER, byte.class, Byte.class, short.class, Short.class, int.class, Integer.class, AtomicInteger.class);
        putTypeSerializer(GLOBAL_SERIALIZERS, ARRAY_STRING, String[].class);
        putTypeSerializer(GLOBAL_SERIALIZERS, ARRAY_PRIMITIVE_LONG, long[].class);
        putTypeSerializer(GLOBAL_SERIALIZERS, ARRAY_PRIMITIVE_INTEGER, int[].class);
        putTypeSerializer(GLOBAL_SERIALIZERS, ARRAY_PRIMITIVE_FLOAT, float[].class);
        putTypeSerializer(GLOBAL_SERIALIZERS, ARRAY_PRIMITIVE_DOUBLE, double[].class);
        putTypeSerializer(GLOBAL_SERIALIZERS, ARRAY_PRIMITIVE_SHORT, short[].class);
        putTypeSerializer(GLOBAL_SERIALIZERS, BINARY, byte[].class);
        putTypeSerializer(GLOBAL_SERIALIZERS, ARRAY_PRIMITIVE_BOOLEAN, boolean[].class);
        putTypeSerializer(GLOBAL_SERIALIZERS, CHAR_SEQUENCE, char[].class, StringBuffer.class, StringBuilder.class, char.class, Character.class);
        JSONTypeExtensionSer.initExtens();
    }

    static void putTypeSerializer(Map<Class<?>, JSONTypeSerializer> serializerMap, JSONTypeSerializer typeSerializer, Class<?>... types) {
        for (Class<?> type : types) {
            serializerMap.put(type, typeSerializer);
        }
    }

    JSONTypeSerializer ensureInitialized() {
        return this;
    }

    /***
     * 序列化接口
     */
    protected abstract void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception;

    protected boolean checkWriteClassName(boolean writeClassName, JSONWriter writer, Class clazz, boolean formatOut, int indentLevel, JSONConfig jsonConfig) throws IOException {
        if (writeClassName) {
            writeType(writer, clazz, formatOut, indentLevel, jsonConfig);
            return true;
        }
        return false;
    }

    void writeType(JSONWriter writer, Class clazz, boolean formatOut, int indentLevel, JSONConfig jsonConfig) throws IOException {
        writeFormatOutSymbols(writer, indentLevel + 1, formatOut, jsonConfig);
        if (formatOut && jsonConfig.formatOutColonSpace) {
            writer.write("\"@c\": \"");
        } else {
            writer.write("\"@c\":\"");
        }
        writer.write(clazz.getName());
        writer.write("\"");
    }

    // 0、字符串序列化
    static class CharSequenceImpl extends JSONTypeSerializer {
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            if (value instanceof CharSequence) {
                writer.writeJSONString(value.toString());
            } else {
                if (value instanceof char[]) {
                    writer.writeJSONChars((char[]) value);
                } else {
                    char c = (Character) value;
                    writer.writeJSONChar(c);
                }
            }
        }
    }

    // <= jdk8
    final static class StringJDK8Impl extends CharSequenceImpl {
        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            String stringVal = value.toString();
            writer.writeJSONChars((char[]) JSONMemoryHandle.getStringValue(stringVal));
        }
    }

    // >= jdk9
    final static class StringJDK9PlusImpl extends CharSequenceImpl {

        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            String stringVal = value.toString();
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(stringVal);
            writer.writeJSONStringBytes(stringVal, bytes);
        }
    }

    // 1、number和bool序列化
    static class SimpleImpl extends JSONTypeSerializer {
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            writer.write(value.toString());
        }
    }

    static class SimpleNumberImpl extends SimpleImpl {

        protected void serializeNumber(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            writer.writeLatinString(value.toString());
        }

        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            boolean writeAsString = jsonConfig.isWriteNumberAsString();
            if (!writeAsString) {
                serializeNumber(value, writer, jsonConfig, indent);
            } else {
                writer.writeJSONToken('"');
                serializeNumber(value, writer, jsonConfig, indent);
                writer.writeJSONToken('"');
            }
        }
    }

    final static class BigIntegerImpl extends SimpleNumberImpl {
        protected void serializeNumber(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            writer.writeBigInteger((BigInteger) value);
        }
    }

    // long
    final static class LongImpl extends SimpleNumberImpl {

        protected void serializeNumber(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            long numValue = ((Number) value).longValue();
            writer.writeLong(numValue);
        }
    }


    // integer/byte/short
    final static class IntegerImpl extends SimpleImpl {

        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            int numValue = ((Number) value).intValue();
            writer.writeInt(numValue);
        }
    }

    final static class DoubleImpl extends SimpleImpl {

        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            double numValue = (Double) value;
            writer.writeDouble(numValue);
        }
    }

    final static class FloatImpl extends SimpleImpl {
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            float numValue = (Float) value;
            writer.writeFloat(numValue);
        }
    }

    // 3、日期
    static class DateImpl extends JSONTypeSerializer {
        protected TimeZone timeZone;

        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            Date date = (Date) value;
            if (jsonConfig.isWriteDateAsTime()) {
                writer.writeLong(date.getTime());
                return;
            }
            TimeZone timeZone = jsonConfig.getTimezone();
            if (timeZone == null) {
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
            writer.writeJSONToken('"');
            if (date instanceof Time) {
                writer.writeTime(hourOfDay, minute, second);
            } else {
                writer.writeDate(year, month, day, hourOfDay, minute, second);
            }
            writer.writeJSONToken('"');
        }
    }

    final static class DateAsTimeImpl extends DateImpl {
        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            writer.writeLong(((Date) value).getTime());
        }
    }

    final static class DatePatternImpl extends DateImpl {
        private final DateFormatter dateFormatter;

        public DatePatternImpl(DateFormatter dateFormatter, TimeZone timeZone) {
            this.dateFormatter = dateFormatter;
            this.timeZone = timeZone;
        }

        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            Date date = (Date) value;
            TimeZone timeZone = jsonConfig.getTimezone();
            if (timeZone == null) {
                timeZone = this.timeZone;
            }
            writer.writeJSONToken('"');
            GeneralDate generalDate = new GeneralDate(date.getTime(), timeZone);
            writeGeneralDate(generalDate, dateFormatter, writer);
            writer.writeJSONToken('"');
        }
    }

    static class EnumImpl extends JSONTypeSerializer {
        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            Enum enmuValue = (Enum) value;
            // ordinal
            if (jsonConfig.isWriteEnumAsOrdinal()) {
                writer.writeLong(enmuValue.ordinal());
            } else {
                writer.writeJSONToken('"');
                writer.write(enmuValue.name());
                writer.writeJSONToken('"');
            }
        }

        static class EnumFieldNameData {
            final String enumNameTokenStr; // \"${name}\"
            final char[] enumNameTokenChars; // \"${name}\"
            final long[] enumNameTokenCharLongs;
            final long[] enumNameTokenByteLongs;
            final int tokenLength;
            final boolean useLong;

            public EnumFieldNameData(String enumNameTokenStr, char[] enumNameTokenChars, long[] enumNameTokenCharLongs, long[] enumNameTokenByteLongs, int tokenLength) {
                this.enumNameTokenStr = enumNameTokenStr;
                this.enumNameTokenChars = enumNameTokenChars;
                this.enumNameTokenCharLongs = enumNameTokenCharLongs;
                this.enumNameTokenByteLongs = enumNameTokenByteLongs;
                this.tokenLength = tokenLength;
                this.useLong = enumNameTokenCharLongs != null;
            }
        }

        static class EnumInstanceImpl extends EnumImpl {
            final EnumFieldNameData[] enumFieldNameDatas;

            EnumInstanceImpl(char[][] enumNameTokenChars) {
                EnumFieldNameData[] enumFieldNameDatas = new EnumFieldNameData[enumNameTokenChars.length];
                for (int i = 0; i < enumNameTokenChars.length; ++i) {
                    char[] chars = enumNameTokenChars[i];
                    String enumNameTokenStr = new String(chars);
                    boolean useUnsafe = enumNameTokenStr.getBytes().length == chars.length;
                    if (useUnsafe) {
                        enumFieldNameDatas[i] = new EnumFieldNameData(null, null, JSONMemoryHandle.getCharLongs(enumNameTokenStr), JSONMemoryHandle.getByteLongs(enumNameTokenStr), chars.length);
                    } else {
                        if (EnvUtils.JDK_9_PLUS) {
                            enumFieldNameDatas[i] = new EnumFieldNameData(enumNameTokenStr, null, null, null, chars.length);
                        } else {
                            enumFieldNameDatas[i] = new EnumFieldNameData(null, chars, null, null, chars.length);
                        }
                    }
                }
                this.enumFieldNameDatas = enumFieldNameDatas;
            }

            @Override
            protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                Enum enmuValue = (Enum) value;
                int ordinal = enmuValue.ordinal();
                if (!jsonConfig.isWriteEnumAsOrdinal()) {
                    EnumFieldNameData fieldNameData = enumFieldNameDatas[ordinal];
                    if (fieldNameData.useLong) {
                        writer.writeMemory(fieldNameData.enumNameTokenCharLongs, fieldNameData.enumNameTokenByteLongs, fieldNameData.tokenLength);
                    } else {
                        if (EnvUtils.JDK_9_PLUS) {
                            String enumNameToken = fieldNameData.enumNameTokenStr;
                            // writeShortString(writer, enumNameToken, 0, enumNameToken.length());
                            writer.write(enumNameToken, 0, enumNameToken.length());
                        } else {
                            char[] chars = fieldNameData.enumNameTokenChars;
                            writer.writeShortChars(chars, 0, chars.length);
                        }
                    }
                } else {
                    writer.writeLong(ordinal);
                }
            }
        }
    }

    final static class ClassImpl extends JSONTypeSerializer {
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            writer.writeJSONToken('"');
            writer.write(((Class) value).getName());
            writer.writeJSONToken('"');
        }
    }

//    final static class AnnotationImpl extends JSONTypeSerializer {
//        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
//            writer.writeJSONToken('"');
//            writer.write(value.toString());
//            writer.writeJSONToken('"');
//        }
//    }

    final static class BinaryImpl extends JSONTypeSerializer {

        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            // use byte array
            if (jsonConfig.isBytesArrayToNative()) {
                ARRAY_PRIMITIVE_BYTE.serialize(value, writer, jsonConfig, indent);
            } else {
                byte[] bytes = (byte[]) value;
                if (jsonConfig.isBytesArrayToHex()) {
                    // use hex encoding
                    writer.writeAsHexString(bytes);
                } else {
                    // use Base64 encoding
                    writer.writeAsBase64String(bytes);
                }
            }
        }
    }

    static class ArrayImpl extends JSONTypeSerializer {
        final JSONStore store;

        ArrayImpl() {
            this(null);
        }

        ArrayImpl(JSONStore store) {
            this.store = store;
        }

        protected JSONTypeSerializer getComponentTypeSerializer(Class<?> componentType) {
            return store.getTypeSerializer(componentType);
        }

        protected void writeArray(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            boolean formatOut = jsonConfig.isFormatOut();
            Object[] objects = (Object[]) obj;
            int length = objects.length;
            if (length > 0) {
                writer.writeJSONToken('[');
                int indentLevelPlus = indentLevel + 1;
                Class<?> componentType = obj.getClass().getComponentType();
                JSONTypeSerializer valueSerializer = getComponentTypeSerializer(componentType);

                Object value = objects[0];
                writeFormatOutSymbols(writer, indentLevelPlus, formatOut, jsonConfig);
                if (value != null) {
                    serializeComponent(valueSerializer, value, componentType, writer, jsonConfig, indentLevelPlus);
                } else {
                    writer.writeNull();
                }
                for (int i = 1; i < length; ++i) {
                    writer.writeJSONToken(',');
                    writeFormatOutSymbols(writer, indentLevelPlus, formatOut, jsonConfig);
                    value = objects[i];
                    if (value != null) {
                        serializeComponent(valueSerializer, value, componentType, writer, jsonConfig, indentLevelPlus);
                    } else {
                        writer.writeNull();
                    }
                }
                writeEndFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                writer.write(']');
            } else {
                writer.writeEmptyArray();
            }
        }

        protected void serializeComponent(JSONTypeSerializer valueSerializer, Object value, Class<?> componentType, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            Class<?> valueClass = value.getClass();
            if (componentType == valueClass) {
                valueSerializer.serialize(value, writer, jsonConfig, indentLevel);
            } else {
                store.getTypeSerializer(valueClass).serialize(value, writer, jsonConfig, indentLevel);
            }
        }

        @Override
        protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    writer.writeNull();
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            writeArray(obj, writer, jsonConfig, indent);
            jsonConfig.setStatus(hashcode, -1);
        }
    }

    final static class ArrayStringImpl extends ArrayImpl {
        @Override
        protected JSONTypeSerializer getComponentTypeSerializer(Class<?> componentType) {
            return null;
        }

        @Override
        protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            writeArray(obj, writer, jsonConfig, indentLevel);
        }

        @Override
        protected void serializeComponent(JSONTypeSerializer valueSerializer, Object value, Class<?> componentType, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            CHAR_SEQUENCE_STRING.serialize(value, writer, jsonConfig, indentLevel);
        }
    }

    final static class ArrayPrimitiveLongImpl extends ArrayImpl {
        @Override
        protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            boolean formatOut = jsonConfig.isFormatOut();
            long[] longs = (long[]) obj;
            int length = longs.length;
            if (length > 0) {
                // 数组类
                writer.writeJSONToken('[');
                int indentLevelPlus = indentLevel + 1;
                writeFormatOutSymbols(writer, indentLevelPlus, formatOut, jsonConfig);
                writer.writeLong(longs[0], jsonConfig);
                for (int i = 1; i < length; ++i) {
                    writer.writeJSONToken(',');
                    writeFormatOutSymbols(writer, indentLevelPlus, formatOut, jsonConfig);
                    writer.writeLong(longs[i], jsonConfig);
                }
                writeEndFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                writer.writeJSONToken(']');
            } else {
                writer.writeEmptyArray();
            }
        }
    }

    final static class ArrayPrimitiveIntImpl extends ArrayImpl {
        @Override
        protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            boolean formatOut = jsonConfig.isFormatOut();
            int[] ints = (int[]) obj;
            int length = ints.length;
            if (length > 0) {
                // 数组类
                writer.writeJSONToken('[');
                int indentLevelPlus = indentLevel + 1;
                writeFormatOutSymbols(writer, indentLevelPlus, formatOut, jsonConfig);
                writer.writeInt(ints[0]);
                for (int i = 1; i < length; ++i) {
                    writer.writeJSONToken(',');
                    writeFormatOutSymbols(writer, indentLevelPlus, formatOut, jsonConfig);
                    writer.writeInt(ints[i]);
                }
                writeEndFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                writer.writeJSONToken(']');
            } else {
                writer.writeEmptyArray();
            }
        }
    }

    final static class ArrayPrimitiveImpl extends ArrayImpl {
        final JSONTypeSerializer valueSerializer;
        final ReflectConsts.PrimitiveType primitiveType;

        public ArrayPrimitiveImpl(JSONTypeSerializer valueSerializer, ReflectConsts.PrimitiveType primitiveType) {
            this.valueSerializer = valueSerializer;
            this.primitiveType = primitiveType;
        }

        @Override
        protected void writeArray(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            boolean formatOut = jsonConfig.isFormatOut();
            int length = primitiveType.arrayLength(obj);
            if (length > 0) {
                // 数组类
                writer.writeJSONToken('[');
                int indentLevelPlus = indentLevel + 1;
                writeFormatOutSymbols(writer, indentLevelPlus, formatOut, jsonConfig);
                valueSerializer.serialize(primitiveType.elementAt(obj, 0), writer, jsonConfig, indentLevelPlus);
                for (int i = 1; i < length; ++i) {
                    writer.writeJSONToken(',');
                    writeFormatOutSymbols(writer, indentLevelPlus, formatOut, jsonConfig);
                    valueSerializer.serialize(primitiveType.elementAt(obj, i), writer, jsonConfig, indentLevelPlus);
                }
                writeEndFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                writer.writeJSONToken(']');
            } else {
                writer.writeEmptyArray();
            }
        }
    }

    static class CollectionImpl extends JSONTypeSerializer {

        final JSONStore store;

        public CollectionImpl(JSONStore store) {
            this.store = store;
        }

        protected void writeCollection(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            boolean formatOut = jsonConfig.isFormatOut();
            Collection<?> collect = (Collection<?>) obj;
            if (!collect.isEmpty()) {
                writer.writeJSONToken('[');
                int indentLevelPlus = indentLevel + 1;
                boolean isEmptyFlag = true;
                Class<?> firstElementClass = null;
                JSONTypeSerializer firstSerializer = null;
                for (Object value : collect) {
                    if (isEmptyFlag) {
                        isEmptyFlag = false;
                    } else {
                        writer.writeJSONToken(',');
                    }
                    writeFormatOutSymbols(writer, indentLevelPlus, formatOut, jsonConfig);
                    if (value != null) {
                        // 此处防止重复查找序列化器小处理一下
                        Class<?> valueClass = value.getClass();
                        if (valueClass == firstElementClass) {
                            firstSerializer.serialize(value, writer, jsonConfig, indentLevelPlus);
                        } else {
                            if (firstElementClass == null) {
                                firstElementClass = valueClass;
                                firstSerializer = store.getTypeSerializer(valueClass);
                                firstSerializer.serialize(value, writer, jsonConfig, indentLevelPlus);
                            } else {
                                store.getTypeSerializer(valueClass).serialize(value, writer, jsonConfig, indentLevelPlus);
                            }
                        }
                    } else {
                        writer.writeNull();
                    }
                }
                if (!isEmptyFlag) {
                    writeEndFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                }
                writer.write(']');
            } else {
                writer.writeEmptyArray();
            }
        }

        protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    writer.writeNull();
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            writeCollection(obj, writer, jsonConfig, indent);
            jsonConfig.setStatus(hashcode, -1);
        }

        final static class CollectionFinalTypeImpl extends CollectionImpl {
            private final JSONTypeSerializer valueSerializer;

            CollectionFinalTypeImpl(JSONTypeSerializer valueSerializer) {
                super(null);
                this.valueSerializer = valueSerializer;
            }

            @Override
            protected void writeCollection(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
                boolean formatOut = jsonConfig.isFormatOut();
                Collection<Object> collect = (Collection<Object>) obj;
                if (!collect.isEmpty()) {
                    writer.writeJSONToken('[');
                    int indentLevelPlus = indentLevel + 1;
                    boolean isEmptyFlag = true;
                    for (Object value : collect) {
                        if (isEmptyFlag) {
                            isEmptyFlag = false;
                        } else {
                            writer.writeJSONToken(',');
                        }
                        writeFormatOutSymbols(writer, indentLevelPlus, formatOut, jsonConfig);
                        if (value != null) {
                            valueSerializer.serialize(value, writer, jsonConfig, indentLevelPlus);
                        } else {
                            writer.writeNull();
                        }
                    }
                    if (!isEmptyFlag) {
                        writeEndFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                    }
                    writer.write(']');
                } else {
                    writer.writeEmptyArray();
                }
            }
        }
    }

    static class MapImpl extends JSONTypeSerializer {
        final JSONStore store;

        MapImpl(JSONStore store) {
            this.store = store;
        }

        private void writeMap(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            boolean formatOut = jsonConfig.isFormatOut();
            boolean formatOutColonSpace = formatOut && jsonConfig.isFormatOutColonSpace();
            Map<Object, Object> map = (Map<Object, Object>) obj;
            if (map.isEmpty()) {
                writer.write(EMPTY_OBJECT);
            } else {
                writer.writeJSONToken('{');
                Set<Map.Entry<Object, Object>> entrySet = map.entrySet();
                boolean isFirstKey = true;
                int indentLevelPlus = indentLevel + 1;
                for (Map.Entry<Object, Object> entry : entrySet) {
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if (isFirstKey) {
                        isFirstKey = false;
                    } else {
                        writer.writeJSONToken(',');
                    }
                    writeFormatOutSymbols(writer, indentLevelPlus, formatOut, jsonConfig);
                    if (jsonConfig.isAllowUnquotedMapKey() && (key == null || key instanceof Number)) {
                        writer.append(String.valueOf(key)).write(':');
                    } else {
                        String stringKey = key == null ? "null" : key.toString();
                        if (jsonConfig.isCamelCaseToUnderline()) {
                            stringKey = StringUtils.camelCaseToSymbol(stringKey);
                        }
                        writer.writeJSONKeyAndColon(stringKey);
                    }
                    if (formatOutColonSpace) {
                        writer.writeJSONToken(' ');
                    }
                    if (value != null) {
                        JSONTypeSerializer valueSerializer = store.getMapValueSerializer(value.getClass()); // getValueSerializer(value);
                        valueSerializer.serialize(value, writer, jsonConfig, formatOut ? indentLevelPlus : -1);
                    } else {
                        writer.writeNull();
                    }
                }
                writeEndFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                writer.write('}');
            }
        }

        protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    writer.writeNull();
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            writeMap(obj, writer, jsonConfig, indent);
            jsonConfig.setStatus(hashcode, -1);
        }
    }

    static class ObjectImpl extends JSONTypeSerializer {
        final JSONStore store;

        ObjectImpl(JSONStore store) {
            this.store = store;
        }

        protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    writer.writeNull();
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }

            boolean writeFullProperty = jsonConfig.isFullProperty();
            boolean formatOut = jsonConfig.isFormatOut();
            boolean formatOutColonSpace = formatOut && jsonConfig.isFormatOutColonSpace();
            boolean writeClassName = jsonConfig.isWriteClassName();
            writer.writeJSONToken('{');

            Class clazz = obj.getClass();
            JSONPojoStructure pojoStructure = getPojoStructure(clazz);

            boolean isEmptyFlag = !checkWriteClassName(writeClassName, writer, clazz, formatOut, indentLevel, jsonConfig);
            JSONPojoFieldSerializer[] fieldSerializers = pojoStructure.getFieldSerializers(jsonConfig.isUseFields());

            boolean skipGetterOfNoExistField = jsonConfig.isSkipGetterOfNoneField();
            boolean unCamelCaseToUnderline = !jsonConfig.isCamelCaseToUnderline();
            int indentPlus = indentLevel + 1;
            for (JSONPojoFieldSerializer fieldSerializer : fieldSerializers) {
                GetterInfo getterInfo = fieldSerializer.getterInfo;
                if (!getterInfo.existField() && skipGetterOfNoExistField) {
                    continue;
                }
                Object value = JSON_SECURE_TRUSTED_ACCESS.get(getterInfo, obj); // getterInfo.invoke(obj);
                if (value == null && !writeFullProperty)
                    continue;
                if (isEmptyFlag) {
                    isEmptyFlag = false;
                } else {
                    writer.writeJSONToken(',');
                }
                writeFormatOutSymbols(writer, indentPlus, formatOut, jsonConfig);
                if (value != null) {
                    if (unCamelCaseToUnderline) {
                        fieldSerializer.writeFieldNameAndColonTo(writer);
                    } else {
                        writer.writeJSONToken('"');
                        writer.write(getterInfo.getUnderlineName());
                        writer.write("\":");
                    }
                    if (formatOutColonSpace) {
                        writer.writeJSONToken(' ');
                    }
                    fieldSerializer.serializer.serialize(value, writer, jsonConfig, indentPlus);
                } else {
                    if (unCamelCaseToUnderline) {
                        if (formatOutColonSpace) {
                            fieldSerializer.writeFieldNameAndColonTo(writer);
                            writer.writeJSONToken(' ');
                            writer.writeNull();
                        } else {
                            fieldSerializer.writeJSONFieldNameWithNull(writer);
                        }
                    } else {
                        writer.writeJSONToken('"');
                        writer.write(getterInfo.getUnderlineName());
                        writer.writeJSONToken('"');
                        if (formatOutColonSpace) {
                            writer.write(": null");
                        } else {
                            writer.write(":null");
                        }
                    }
                }
            }
            if (!isEmptyFlag) {
                writeEndFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
            }
            writer.write('}');
            jsonConfig.setStatus(hashcode, -1);
        }

        JSONPojoStructure getPojoStructure(Class<?> clazz) {
            JSONPojoStructure pojoStructure = store.getPojoStruc(clazz);
            pojoStructure.ensureInitializedFieldSerializers();
            return pojoStructure;
        }

        final static class ObjectWithTypeImpl extends ObjectImpl {

            ObjectWithTypeImpl(JSONStore store) {
                super(store);
            }

            protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
                ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(obj.getClass());
                if (classCategory != ReflectConsts.ClassCategory.ObjectCategory) {
                    store.TYPE_SERIALIZERS[classCategory.ordinal()].serialize(obj, writer, jsonConfig, indentLevel);
                } else {
                    super.serialize(obj, writer, jsonConfig, indentLevel);
                }
            }

            protected boolean checkWriteClassName(boolean writeClassName, JSONWriter writer, Class clazz, boolean formatOut, int indentLevel, JSONConfig jsonConfig) throws IOException {
                writeType(writer, clazz, formatOut, indentLevel, jsonConfig);
                return true;
            }
        }

        final static class ObjectWrapperImpl extends ObjectImpl {
            final JSONPojoStructure pojoStructure;
            final Class<?> objectCls;

            ObjectWrapperImpl(Class<?> cls, JSONPojoStructure pojoStructure) {
                super(pojoStructure.store);
                this.objectCls = cls;
                this.pojoStructure = pojoStructure;
            }

            JSONPojoStructure getPojoStructure(Class<?> clazz) {
                return clazz == objectCls ? pojoStructure : super.getPojoStructure(clazz);
            }

            @Override
            JSONTypeSerializer ensureInitialized() {
                pojoStructure.ensureInitializedFieldSerializers();
                return this;
            }
        }
    }

    final static class AnyImpl extends JSONTypeSerializer {
        final JSONStore store;

        AnyImpl(JSONStore store) {
            this.store = store;
        }

        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            Class<?> clazz = value.getClass();
            if (clazz == Object.class) {
                writer.write(EMPTY_OBJECT);
            } else {
                JSONTypeSerializer typeSerializer = store.getTypeSerializer(clazz);
                if (typeSerializer != this) {
                    typeSerializer.serialize(value, writer, jsonConfig, indent);
                } else {
                    writer.writeNull();
                }
            }
        }
    }

    protected final static void writeDate(int year, int month, int day, int hourOfDay, int minute, int second, int millisecond, DateFormatter dateFormatter, JSONWriter writer) throws Exception {
        if (writer instanceof JSONCharArrayWriter) {
            JSONCharArrayWriter charArrayWriter = (JSONCharArrayWriter) writer;
            char[] buf = charArrayWriter.ensureCapacity(dateFormatter.getEstimateSize() + JSONWriter.SECURITY_UNCHECK_SPACE);
            int off = charArrayWriter.count;
            off += dateFormatter.write(year, month, day, hourOfDay, minute, second, millisecond, buf, off);
            charArrayWriter.count = off;
            return;
        }
        dateFormatter.formatTo(year, month, day, hourOfDay, minute, second, millisecond, writer);
    }

    protected final static void writeGeneralDate(GeneralDate generalDate, DateFormatter dateFormatter, JSONWriter writer) throws Exception {
        int month, year, day, hourOfDay, minute, second, millisecond;
        year = generalDate.getYear();
        month = generalDate.getMonth();
        day = generalDate.getDay();
        hourOfDay = generalDate.getHourOfDay();
        minute = generalDate.getMinute();
        second = generalDate.getSecond();
        millisecond = generalDate.getMillisecond();
        writeDate(year, month, day, hourOfDay, minute, second, millisecond, dateFormatter, writer);
    }

    static class ToStringImpl extends JSONTypeSerializer {
        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            // CHAR_SEQUENCE_STRING.serialize(value.toString(), writer, jsonConfig, indent);
            writer.writeJSONString(value.toString());
        }
    }

    static abstract class ToIntegerImpl<E> extends JSONTypeSerializer {
        public abstract int intValue(E target) throws Exception;

        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            writer.writeInt(intValue((E) value));
        }
    }
}
