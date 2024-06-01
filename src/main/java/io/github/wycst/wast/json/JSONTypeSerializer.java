package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.DateFormatter;
import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.compiler.JDKCompiler;
import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.tools.Base64;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.json.annotations.JsonProperty;

import java.io.IOException;
import java.lang.reflect.Constructor;
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
    protected static final CharSequenceSerializer CHAR_SEQUENCE_STRING = EnvUtils.JDK_9_PLUS ? new CharSequenceSerializer.StringBytesSerializer() : new CharSequenceSerializer.StringCharsSerializer();
    protected static final EnumSerializer ENUM = new EnumSerializer();
    protected static final SimpleSerializer.SimpleNumberSerializer NUMBER = new SimpleSerializer.SimpleNumberSerializer();
    protected static final SimpleSerializer.SimpleNumberSerializer NUMBER_LONG = new SimpleSerializer.SimpleLongSerializer();
    protected static final SimpleSerializer.SimpleNumberSerializer NUMBER_DOUBLE = new SimpleSerializer.SimpleDoubleSerializer();
    protected static final SimpleSerializer.SimpleNumberSerializer NUMBER_FLOAT = new SimpleSerializer.SimpleFloatSerializer();
    protected static final MapSerializer MAP = new MapSerializer();
    protected static final ArraySerializer ARRAY_OBJECT = new ArraySerializer();
    protected static final ArraySerializer ARRAY_STRING = new ArraySerializer.ArrayStringSerializer();
    protected static final ArraySerializer ARRAY_PRIMITIVE_LONG = new ArraySerializer.ArrayPrimitiveSerializer(NUMBER_LONG, ReflectConsts.PrimitiveType.PrimitiveLong);
    protected static final ArraySerializer ARRAY_PRIMITIVE_BYTE = new ArraySerializer.ArrayPrimitiveSerializer(NUMBER_LONG, ReflectConsts.PrimitiveType.PrimitiveByte);
    protected static final ArraySerializer ARRAY_PRIMITIVE_INTEGER = new ArraySerializer.ArrayPrimitiveSerializer(NUMBER_LONG, ReflectConsts.PrimitiveType.PrimitiveInt);
    protected static final ArraySerializer ARRAY_PRIMITIVE_SHORT = new ArraySerializer.ArrayPrimitiveSerializer(NUMBER_LONG, ReflectConsts.PrimitiveType.PrimitiveShort);
    protected static final ArraySerializer ARRAY_PRIMITIVE_FLOAT = new ArraySerializer.ArrayPrimitiveSerializer(NUMBER_FLOAT, ReflectConsts.PrimitiveType.PrimitiveFloat);
    protected static final ArraySerializer ARRAY_PRIMITIVE_DOUBLE = new ArraySerializer.ArrayPrimitiveSerializer(NUMBER_DOUBLE, ReflectConsts.PrimitiveType.PrimitiveDouble);
    protected static final CollectionSerializer COLLECTION = new CollectionSerializer();
    protected static final JSONTypeSerializer DATE_AS_TIME_SERIALIZER = new DateSerializer.DateAsTimeSerializer();

    //    protected static final JSONTypeSerializer TO_STRING = new JSONTypeSerializer() {
//        @Override
//        protected void serialize(Object value, JSONWriter content, JsonConfig jsonConfig, int indent) throws Exception {
//            content.write('"');
//            content.write(value.toString());
//            content.write('"');
//        }
//    };
    static final Set<Class<?>> BUILT_IN_TYPE_SET;

    static {
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.CharSequence.ordinal()] = CHAR_SEQUENCE;
        JSONTypeSerializer SIMPLE = new SimpleSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.NumberCategory.ordinal()] = NUMBER;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.BoolCategory.ordinal()] = SIMPLE;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.DateCategory.ordinal()] = new DateSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ClassCategory.ordinal()] = new ClassSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.EnumCategory.ordinal()] = ENUM;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.AnnotationCategory.ordinal()] = new AnnotationSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.Binary.ordinal()] = new BinarySerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ArrayCategory.ordinal()] = ARRAY_OBJECT;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.CollectionCategory.ordinal()] = COLLECTION;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.MapCategory.ordinal()] = MAP;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ObjectCategory.ordinal()] = new ObjectSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ANY.ordinal()] = new ANYSerializer();

        putTypeSerializer(CHAR_SEQUENCE_STRING, String.class);
        putTypeSerializer(new SimpleSerializer.SimpleBigIntegerSerializer(), BigInteger.class);
        putTypeSerializer(new SimpleSerializer.SimpleDoubleSerializer(), double.class, Double.class);
        putTypeSerializer(new SimpleSerializer.SimpleFloatSerializer(), float.class, Float.class);
        putTypeSerializer(NUMBER_LONG, byte.class, Byte.class, short.class, Short.class, int.class, Integer.class, long.class, Long.class, AtomicInteger.class, AtomicLong.class);

        putTypeSerializer(ARRAY_OBJECT, Object[].class);
        putTypeSerializer(ARRAY_STRING, String[].class);
        putTypeSerializer(ARRAY_PRIMITIVE_LONG, long[].class);
        putTypeSerializer(ARRAY_PRIMITIVE_INTEGER, int[].class);
        putTypeSerializer(ARRAY_PRIMITIVE_FLOAT, float[].class);
        putTypeSerializer(ARRAY_PRIMITIVE_DOUBLE, double[].class);
        putTypeSerializer(ARRAY_PRIMITIVE_SHORT, short[].class);
        putTypeSerializer(ARRAY_PRIMITIVE_BYTE, byte[].class);
        putTypeSerializer(new ArraySerializer.ArrayPrimitiveSerializer(SIMPLE, ReflectConsts.PrimitiveType.PrimitiveBoolean), boolean[].class);
        putTypeSerializer(CHAR_SEQUENCE, char[].class);

        // 其他类型支持
        putTypeSerializer(new JSONTypeSerializer() {
            @Override
            protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                writer.writeUUID((UUID) value);
            }
        }, UUID.class);

        BUILT_IN_TYPE_SET = new HashSet<Class<?>>(classJSONTypeSerializerMap.keySet());
    }

    static boolean isBuiltInType(Class<?> type) {
        return BUILT_IN_TYPE_SET.contains(type);
    }

    static void putTypeSerializer(JSONTypeSerializer typeSerializer, Class... types) {
        for (Class type : types) {
            classJSONTypeSerializerMap.put(type, typeSerializer);
        }
    }

    // 根据分类获取
    protected static JSONTypeSerializer getTypeSerializer(ReflectConsts.ClassCategory classCategory, JsonProperty jsonProperty) {
        int ordinal = classCategory.ordinal();
        if (classCategory == ReflectConsts.ClassCategory.DateCategory) {
            if (jsonProperty != null) {
                if (jsonProperty.asTimestamp()) {
                    return DATE_AS_TIME_SERIALIZER;
                }
                String pattern = jsonProperty.pattern().trim();
                if (pattern.length() > 0) {
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

    protected final static JSONTypeSerializer getMapValueSerializer(Class<?> cls) {
        int classHashCode = cls.getName().hashCode();
        switch (classHashCode) {
            case EnvUtils.STRING_HV: {
                if (cls == String.class) {
                    return CHAR_SEQUENCE_STRING;
                }
                break;
            }
            case EnvUtils.INT_HV:
            case EnvUtils.INTEGER_HV:
            case EnvUtils.LONG_PRI_HV:
            case EnvUtils.LONG_HV:
                if (Number.class.isAssignableFrom(cls)) {
                    return NUMBER_LONG;
                }
                break;
            case EnvUtils.HASHMAP_HV:
            case EnvUtils.LINK_HASHMAP_HV:
                if (Map.class.isAssignableFrom(cls)) {
                    return MAP;
                }
                break;
            case EnvUtils.ARRAY_LIST_HV:
            case EnvUtils.HASH_SET_HV:
                if (Collection.class.isAssignableFrom(cls)) {
                    return COLLECTION;
                }
                break;
        }
        return getTypeSerializer(cls);
    }

    // Quick search based on usage frequency, significant effect when serializing map objects
    protected static JSONTypeSerializer getTypeSerializer(Class<?> cls) {
        JSONTypeSerializer typeSerializer = classJSONTypeSerializerMap.get(cls);
        if (typeSerializer != null) {
            return typeSerializer;
        } else {
            synchronized (cls) {
                typeSerializer = classJSONTypeSerializerMap.get(cls);
                if (typeSerializer != null) {
                    return typeSerializer;
                }
                ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(cls);
                if (classCategory == ReflectConsts.ClassCategory.ObjectCategory) {
                    ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(cls);
                    if (classStructureWrapper.isTemporal()) {
                        typeSerializer = JSONTemporalSerializer.getTemporalSerializerInstance(classStructureWrapper, null);
                    } else if (classStructureWrapper.isSubEnum()) {
                        typeSerializer = ENUM;
                    } else {
                        JSONPojoStructure jsonPojoStructure = JSONPojoStructure.get(cls);
                        if (jsonPojoStructure.isSupportedJIT()) {
                            try {
                                Class<?> serializerClass = JDKCompiler.compileJavaSource(JSONPojoSerializer.generateRuntimeJavaCodeSource(jsonPojoStructure));
                                Constructor constructor = serializerClass.getDeclaredConstructor(new Class[]{JSONPojoStructure.class});
                                UnsafeHelper.setAccessible(constructor);
                                typeSerializer = (JSONPojoSerializer) constructor.newInstance(jsonPojoStructure);
                            } catch (Throwable throwable) {
                                typeSerializer = new ObjectSerializer.ObjectWrapperSerializer(cls);
                            }
                        } else {
                            typeSerializer = new ObjectSerializer.ObjectWrapperSerializer(cls);
                        }
                    }
                } else {
                    typeSerializer = getTypeSerializer(classCategory, null);
                }
                classJSONTypeSerializerMap.put(cls, typeSerializer);
                return typeSerializer;
            }
        }
    }

    protected static JSONTypeSerializer getEnumSerializer(Class<?> enumClass) {
        JSONTypeSerializer enumSerializer = classJSONTypeSerializerMap.get(enumClass);
        if (enumSerializer != null) {
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
            case ArrayCategory: {
                if (Object[].class.isAssignableFrom(type)) {
                    return type == String[].class ? ARRAY_STRING : ARRAY_OBJECT;
                } else {
                    return getTypeSerializer(type);
                }
            }
            case ObjectCategory: {
                ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(type);
                if (classStructureWrapper.isTemporal()) {
                    return JSONTemporalSerializer.getTemporalSerializerInstance(classStructureWrapper, jsonProperty);
                } else {
                    if (jsonProperty != null && jsonProperty.unfixedType()) {
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

//    protected static JSONTypeSerializer getValueSerializer(Object value) {
//        return getTypeSerializer(value.getClass());
//    }

    /***
     * 序列化接口
     *
     * @param value
     * @param writer
     * @param jsonConfig
     * @param indent
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
        writer.write("\"@c\":\"");
        writer.write(clazz.getName());
        writer.write("\"");
    }

    // 0、字符串序列化
    static class CharSequenceSerializer extends JSONTypeSerializer {

        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
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
            writer.writeJSONChars(chars);
        }

        // <= jdk8
        public static class StringCharsSerializer extends CharSequenceSerializer {
            @Override
            protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                String stringVal = (String) value;
                writer.writeJSONChars(getChars(stringVal));
            }
        }

        // >= jdk9
        public static class StringBytesSerializer extends CharSequenceSerializer {

            @Override
            protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                String stringVal = (String) value;
                byte[] bytes = (byte[]) UnsafeHelper.getStringValue(stringVal);
                writer.writeJSONStringBytes(stringVal, bytes);
            }
        }
    }

    // 1、number和bool序列化
    private static class SimpleSerializer extends JSONTypeSerializer {

        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            writer.write(value.toString());
        }

        static class SimpleNumberSerializer extends SimpleSerializer {

            protected void serializeNumber(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                writer.write(value.toString());
            }

            protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                boolean writeAsString = jsonConfig.isWriteNumberAsString();
                if (!writeAsString) {
                    serializeNumber(value, writer, jsonConfig, indent);
                } else {
                    writer.write('"');
                    serializeNumber(value, writer, jsonConfig, indent);
                    writer.write('"');
                }
            }
        }

        static class SimpleBigIntegerSerializer extends SimpleNumberSerializer {
            protected void serializeNumber(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                writer.writeBigInteger((BigInteger) value);
            }
        }

        // integer/long/byte/short
        static class SimpleLongSerializer extends SimpleNumberSerializer {

            protected void serializeNumber(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                long numValue = ((Number) value).longValue();
                writer.writeLong(numValue);
            }
        }

        static class SimpleDoubleSerializer extends SimpleNumberSerializer {

            protected void serializeNumber(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                double numValue = ((Number) value).doubleValue();
                if (jsonConfig.isWriteDecimalUseToString()) {
                    writer.write(Double.toString(numValue));
                } else {
                    writer.writeDouble(numValue);
                }
            }
        }

        static class SimpleFloatSerializer extends SimpleNumberSerializer {
            protected void serializeNumber(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                float numValue = ((Number) value).floatValue();
                if (jsonConfig.isWriteDecimalUseToString()) {
                    writer.write(Float.toString(numValue));
                } else {
                    writer.writeFloat(numValue);
                }
            }
        }
    }

    // 3、日期
    private static class DateSerializer extends JSONTypeSerializer {

        protected TimeZone timeZone;

        final void writeDateAsTime(Date date, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            long time = date.getTime();
            NUMBER_LONG.serializeNumber(time, writer, jsonConfig, indent);
        }

        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            Date date = (Date) value;
            if (jsonConfig.isWriteDateAsTime()) {
                writeDateAsTime(date, writer, jsonConfig, indent);
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
            writer.write('"');
            if (date instanceof Time) {
                writer.writeTime(hourOfDay, minute, second);
            } else {
                writer.writeDate(year, month, day, hourOfDay, minute, second);
            }
            writer.write('"');
        }

        static class DateAsTimeSerializer extends DateSerializer {
            @Override
            protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                Date date = (Date) value;
                writeDateAsTime(date, writer, jsonConfig, indent);
            }
        }

        static class DatePatternSerializer extends DateSerializer {
            protected final DateFormatter dateFormatter;

            public DatePatternSerializer(DateFormatter dateFormatter, TimeZone timeZone) {
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
                writer.write('"');
                GeneralDate generalDate = new GeneralDate(date.getTime(), timeZone);
                writeGeneralDate(generalDate, dateFormatter, writer);
                writer.write('"');
            }
        }
    }

    private static class EnumSerializer extends JSONTypeSerializer {

        // use map value
        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            Enum enmuValue = (Enum) value;
            // ordinal
            if (jsonConfig.isWriteEnumAsOrdinal()) {
                NUMBER_LONG.serializeNumber(enmuValue.ordinal(), writer, jsonConfig, indent);
            } else {
                String enumName = enmuValue.name();
                writer.append('"');
                if (EnvUtils.JDK_9_PLUS) {
                    writer.write(enumName);
                } else {
                    writer.writeShortChars(getChars(enumName), 0, enumName.length());
                }
                writer.append('"');
            }
        }

        static class EnumInstanceSerializer extends EnumSerializer {

            final char[][] enumNameTokenChars;
            String[] enumNameTokens;

            EnumInstanceSerializer(char[][] enumNameTokenChars) {
                this.enumNameTokenChars = enumNameTokenChars;
                if (EnvUtils.JDK_9_PLUS) {
                    this.enumNameTokens = new String[enumNameTokenChars.length];
                    for (int i = 0; i < enumNameTokenChars.length; ++i) {
                        enumNameTokens[i] = new String(enumNameTokenChars[i]);
                    }
                }
            }

            @Override
            protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                Enum enmuValue = (Enum) value;
                int ordinal = enmuValue.ordinal();
                if (!jsonConfig.isWriteEnumAsOrdinal()) {
                    if (EnvUtils.JDK_9_PLUS) {
                        String enumNameToken = enumNameTokens[ordinal];
                        // writeShortString(writer, enumNameToken, 0, enumNameToken.length());
                        writer.write(enumNameToken, 0, enumNameToken.length());
                    } else {
                        char[] chars = enumNameTokenChars[ordinal];
                        writer.writeShortChars(chars, 0, chars.length);
                    }
                } else {
                    NUMBER_LONG.serializeNumber(ordinal, writer, jsonConfig, indent);
                }
            }
        }
    }

    private static class ClassSerializer extends JSONTypeSerializer {
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            writer.append('"').append(((Class) value).getName()).append('"');
        }
    }

    private static class AnnotationSerializer extends JSONTypeSerializer {
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            writer.append('"').append(value.toString()).append('"');
        }
    }

    private static class BinarySerializer extends JSONTypeSerializer {

        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            // use byte array
            if (jsonConfig.isBytesArrayToNative()) {
                ARRAY_PRIMITIVE_BYTE.writeArray(value, writer, jsonConfig, indent);
            } else {
                byte[] bytes = (byte[]) value;
                if (jsonConfig.isBytesArrayToHex()) {
                    writer.append('"').append(printHexString(bytes, (char) 0)).append('"');
                } else {
                    // use Base64 encoding
                    writer.append('"').append(Base64.getEncoder().encodeToString(bytes)).append('"');
                }
            }
        }
    }

    private static class ArraySerializer extends JSONTypeSerializer {

        protected JSONTypeSerializer getComponentTypeSerializer(Class<?> componentType) {
            return JSONTypeSerializer.getTypeSerializer(componentType);
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
                    writer.write(NULL);
                }
                for (int i = 1; i < length; ++i) {
                    writer.writeJSONToken(',');
                    writeFormatOutSymbols(writer, indentLevelPlus, formatOut, jsonConfig);
                    value = objects[i];
                    if (value != null) {
                        serializeComponent(valueSerializer, value, componentType, writer, jsonConfig, indentLevelPlus);
                    } else {
                        writer.write(NULL);
                    }
                }
                writeFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                writer.write(']');
            } else {
//                writer.write(EMPTY_ARRAY);
                writer.writeEmptyArray();
            }
        }

        protected void serializeComponent(JSONTypeSerializer valueSerializer, Object value, Class<?> componentType, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            Class<?> valueClass = value.getClass();
            if (componentType == valueClass) {
                valueSerializer.serialize(value, writer, jsonConfig, indentLevel);
            } else {
                getTypeSerializer(valueClass).serialize(value, writer, jsonConfig, indentLevel);
            }
        }

        @Override
        protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    writer.write(NULL);
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            writeArray(obj, writer, jsonConfig, indent);
            jsonConfig.setStatus(hashcode, -1);
        }

        static final class ArrayStringSerializer extends ArraySerializer {
            @Override
            protected JSONTypeSerializer getComponentTypeSerializer(Class<?> componentType) {
                return null;
            }

            @Override
            protected void serializeComponent(JSONTypeSerializer valueSerializer, Object value, Class<?> componentType, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
                CHAR_SEQUENCE_STRING.serialize(value, writer, jsonConfig, indentLevel);
            }
        }

        static class ArrayPrimitiveSerializer extends ArraySerializer {
            final JSONTypeSerializer valueSerializer;
            final ReflectConsts.PrimitiveType primitiveType;

            public ArrayPrimitiveSerializer(JSONTypeSerializer valueSerializer, ReflectConsts.PrimitiveType primitiveType) {
                this.valueSerializer = valueSerializer;
                this.primitiveType = primitiveType;
            }

            @Override
            protected void writeArray(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
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
                    writeFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                    writer.writeJSONToken(']');
                } else {
//                    writer.write(EMPTY_ARRAY);
                    writer.writeEmptyArray();
                }
            }
        }
    }

    private static class CollectionSerializer extends JSONTypeSerializer {

        protected void writeCollectionValue(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            boolean formatOut = jsonConfig.isFormatOut();
            Collection<?> collect = (Collection<?>) obj;
            if (collect.size() > 0) {
                // 集合类
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
                                firstSerializer = getTypeSerializer(valueClass);
                                firstSerializer.serialize(value, writer, jsonConfig, indentLevelPlus);
                            } else {
                                getTypeSerializer(valueClass).serialize(value, writer, jsonConfig, indentLevelPlus);
                            }
                        }
                    } else {
                        writer.write(NULL);
                    }
                }
                if (!isEmptyFlag) {
                    writeFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                }
                writer.write(']');
            } else {
//                writer.write(EMPTY_ARRAY);
                writer.writeEmptyArray();
            }
        }

        protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    writer.write(NULL);
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            writeCollectionValue(obj, writer, jsonConfig, indent);
            jsonConfig.setStatus(hashcode, -1);
        }
    }

    private static class MapSerializer extends JSONTypeSerializer {

        private static void writeMap(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            boolean formatOut = jsonConfig.isFormatOut();
            boolean formatOutColonSpace = formatOut && jsonConfig.isFormatOutColonSpace();
            // map call get
            Map<Object, Object> map = (Map<Object, Object>) obj;
            if (map.size() == 0) {
                writer.write(EMPTY_OBJECT);
            } else {
                writer.writeJSONToken('{');
                // 遍历map
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
                        writer.writeJSONKeyAndColon(stringKey);
                    }
                    if (formatOutColonSpace) {
                        writer.writeJSONToken(' ');
                    }
                    if (value != null) {
                        JSONTypeSerializer valueSerializer = getMapValueSerializer(value.getClass()); // getValueSerializer(value);
                        valueSerializer.serialize(value, writer, jsonConfig, formatOut ? indentLevelPlus : -1);
                    } else {
                        writer.write(NULL);
                    }
                }
                writeFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                writer.write('}');
            }
        }

        protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    writer.write(NULL);
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            writeMap(obj, writer, jsonConfig, indent);
            jsonConfig.setStatus(hashcode, -1);
        }
    }

    private static class ObjectSerializer extends JSONTypeSerializer {

        protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    writer.write(NULL);
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
            JSONPojoStructure classStructureWrapper = getObjectStructureWrapper(clazz);
            classStructureWrapper.ensureInitialized();

            boolean isEmptyFlag = !checkWriteClassName(writeClassName, writer, clazz, formatOut, indentLevel, jsonConfig);
            JSONPojoFieldSerializer[] fieldSerializers = classStructureWrapper.getFieldSerializers(jsonConfig.isUseFields());

            boolean skipGetterOfNoExistField = jsonConfig.isSkipGetterOfNoneField();
            boolean unCamelCaseToUnderline = !jsonConfig.isCamelCaseToUnderline();
            int indentPlus = indentLevel + 1;
            for (JSONPojoFieldSerializer fieldSerializer : fieldSerializers) {
                GetterInfo getterInfo = fieldSerializer.getGetterInfo();
                if (!getterInfo.existField() && skipGetterOfNoExistField) {
                    continue;
                }
                Object value = getterInfo.invoke(obj);
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
                        fieldSerializer.writeJSONFieldName(writer);
                    } else {
                        writer.append('"').append(getterInfo.getUnderlineName()).append("\":");
                    }
                    if (formatOutColonSpace) {
                        writer.writeJSONToken(' ');
                    }
                    // Custom serialization
                    JSONTypeSerializer serializer = fieldSerializer.getSerializer();
                    serializer.serialize(value, writer, jsonConfig, indentPlus);
                } else {
                    if (unCamelCaseToUnderline) {
                        if (formatOutColonSpace) {
                            fieldSerializer.writeJSONFieldName(writer);
                            writer.writeJSONToken(' ');
                            writer.write(NULL);
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
                            writer.write("null");
                        }
                    }
                }
            }
            if (!isEmptyFlag) {
                writeFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
            }
            writer.write('}');
            jsonConfig.setStatus(hashcode, -1);
        }

        JSONPojoStructure getObjectStructureWrapper(Class clazz) {
            return JSONPojoStructure.get(clazz);
        }

        final static class ObjectWithTypeSerializer extends ObjectSerializer {
            protected void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
                ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(obj.getClass());
                if (classCategory != ReflectConsts.ClassCategory.ObjectCategory) {
                    getTypeSerializer(classCategory, null).serialize(obj, writer, jsonConfig, indentLevel);
                } else {
                    super.serialize(obj, writer, jsonConfig, indentLevel);
                }
            }

            protected boolean checkWriteClassName(boolean writeClassName, JSONWriter writer, Class clazz, boolean formatOut, int indentLevel, JSONConfig jsonConfig) throws IOException {
                writeType(writer, clazz, formatOut, indentLevel, jsonConfig);
                return true;
            }
        }

        final static class ObjectWrapperSerializer extends ObjectSerializer {
            final JSONPojoStructure classStructureWrapper;
            final Class objectCls;

            ObjectWrapperSerializer(Class cls) {
                this.objectCls = cls;
                classStructureWrapper = JSONPojoStructure.get(cls);
            }

            JSONPojoStructure getObjectStructureWrapper(Class clazz) {
                return clazz == objectCls ? classStructureWrapper : JSONPojoStructure.get(clazz);
            }
        }
    }

    private static class ANYSerializer extends JSONTypeSerializer {
        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
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

    protected static void writeDate(int year, int month, int day, int hourOfDay, int minute, int second, int millisecond, DateFormatter dateFormatter, JSONWriter writer) throws Exception {
        if (writer instanceof JSONCharArrayWriter) {
            JSONCharArrayWriter charArrayWriter = (JSONCharArrayWriter) writer;
            char[] buf = charArrayWriter.ensureCapacity(dateFormatter.getEstimateSize());
            int off = charArrayWriter.count;
            off += dateFormatter.write(year, month, day, hourOfDay, minute, second, millisecond, buf, off);
            charArrayWriter.count = off;
            return;
        }
        dateFormatter.formatTo(year, month, day, hourOfDay, minute, second, millisecond, writer);
    }

    protected static void writeGeneralDate(GeneralDate generalDate, DateFormatter dateFormatter, JSONWriter writer) throws Exception {
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
}
