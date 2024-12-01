package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.DateFormatter;
import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.compiler.JDKCompiler;
import io.github.wycst.wast.common.reflect.ClassStrucWrap;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
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
    private static final Set<Class> REGISTERED_TYPES = new LinkedHashSet<Class>();
    static final JSONTypeSerializer SIMPLE = new SimpleImpl();
    static final CharSequenceImpl CHAR_SEQUENCE = new CharSequenceImpl();
    static final CharSequenceImpl CHAR_SEQUENCE_STRING = EnvUtils.JDK_9_PLUS ? new StringJDK9PlusImpl() : new StringJDK8Impl();

    static final JSONTypeSerializer TO_STRING = new ToStringImpl();

    static final EnumImpl ENUM = new EnumImpl();
    static final BinaryImpl BINARY = new BinaryImpl();
    static final SimpleNumberImpl NUMBER = new SimpleNumberImpl();
    static final SimpleNumberImpl NUMBER_LONG = new LongImpl();
    static final SimpleNumberImpl NUMBER_DOUBLE = new DoubleImpl();
    static final SimpleNumberImpl NUMBER_FLOAT = new FloatImpl();
    static final MapImpl MAP = new MapImpl();
    static final ObjectImpl OBJECT = new ObjectImpl();
    static final AnyImpl ANY = new AnyImpl();
    static final ArrayImpl ARRAY_OBJECT = new ArrayImpl();
    static final ArrayImpl ARRAY_STRING = new ArrayStringImpl();
    static final ArrayImpl ARRAY_PRIMITIVE_LONG = new ArrayPrimitiveImpl(NUMBER_LONG, ReflectConsts.PrimitiveType.PrimitiveLong);
    static final ArrayImpl ARRAY_PRIMITIVE_BYTE = new ArrayPrimitiveImpl(NUMBER_LONG, ReflectConsts.PrimitiveType.PrimitiveByte);
    static final ArrayImpl ARRAY_PRIMITIVE_INTEGER = new ArrayPrimitiveImpl(NUMBER_LONG, ReflectConsts.PrimitiveType.PrimitiveInt);
    static final ArrayImpl ARRAY_PRIMITIVE_SHORT = new ArrayPrimitiveImpl(NUMBER_LONG, ReflectConsts.PrimitiveType.PrimitiveShort);
    static final ArrayImpl ARRAY_PRIMITIVE_FLOAT = new ArrayPrimitiveImpl(NUMBER_FLOAT, ReflectConsts.PrimitiveType.PrimitiveFloat);
    static final ArrayImpl ARRAY_PRIMITIVE_DOUBLE = new ArrayPrimitiveImpl(NUMBER_DOUBLE, ReflectConsts.PrimitiveType.PrimitiveDouble);
    static final CollectionImpl COLLECTION = new CollectionImpl();
    static final JSONTypeSerializer DATE_AS_TIME_SERIALIZER = new DateAsTimeImpl();
    static final Set<Class<?>> BUILT_IN_TYPE_SET;

    static {
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.CharSequence.ordinal()] = CHAR_SEQUENCE;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.NumberCategory.ordinal()] = NUMBER;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.BoolCategory.ordinal()] = SIMPLE;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.DateCategory.ordinal()] = new DateImpl();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ClassCategory.ordinal()] = new ClassImpl();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.EnumCategory.ordinal()] = ENUM;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.AnnotationCategory.ordinal()] = new AnnotationImpl();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.Binary.ordinal()] = BINARY;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ArrayCategory.ordinal()] = ARRAY_OBJECT;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.CollectionCategory.ordinal()] = COLLECTION;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.MapCategory.ordinal()] = MAP;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ObjectCategory.ordinal()] = OBJECT;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ANY.ordinal()] = ANY;

        putTypeSerializer(MAP, LinkedHashMap.class, HashMap.class, ConcurrentHashMap.class, Hashtable.class);
        putTypeSerializer(CHAR_SEQUENCE_STRING, String.class);
        putTypeSerializer(new BigIntegerImpl(), BigInteger.class);
        putTypeSerializer(NUMBER_DOUBLE, double.class, Double.class);
        putTypeSerializer(NUMBER_FLOAT, float.class, Float.class);
        putTypeSerializer(NUMBER_LONG, byte.class, Byte.class, short.class, Short.class, int.class, Integer.class, long.class, Long.class, AtomicInteger.class, AtomicLong.class);

        putTypeSerializer(ARRAY_OBJECT, Object[].class);
        putTypeSerializer(ARRAY_STRING, String[].class);
        putTypeSerializer(ARRAY_PRIMITIVE_LONG, long[].class);
        putTypeSerializer(ARRAY_PRIMITIVE_INTEGER, int[].class);
        putTypeSerializer(ARRAY_PRIMITIVE_FLOAT, float[].class);
        putTypeSerializer(ARRAY_PRIMITIVE_DOUBLE, double[].class);
        putTypeSerializer(ARRAY_PRIMITIVE_SHORT, short[].class);
        putTypeSerializer(BINARY, byte[].class);
        putTypeSerializer(new ArrayPrimitiveImpl(SIMPLE, ReflectConsts.PrimitiveType.PrimitiveBoolean), boolean[].class);
        putTypeSerializer(CHAR_SEQUENCE, char[].class);
        // extension types
        JSONTypeExtensionSer.initExtens();
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

    final static void registerTypeSerializer(JSONTypeSerializer typeSerializer, Class type) {
        classJSONTypeSerializerMap.put(type, typeSerializer);
        REGISTERED_TYPES.add(type);
    }

    final static JSONTypeSerializer checkSuperclassRegistered(Class<?> cls) {
        for (Class type : REGISTERED_TYPES) {
            if (type.isAssignableFrom(cls)) {
                return classJSONTypeSerializerMap.get(type);
            }
        }
        return null;
    }

    // 根据分类获取
    static JSONTypeSerializer getTypeSerializer(ReflectConsts.ClassCategory classCategory, JsonProperty jsonProperty) {
        int ordinal = classCategory.ordinal();
        if (classCategory == ReflectConsts.ClassCategory.DateCategory) {
            if (jsonProperty != null) {
                if (jsonProperty.asTimestamp()) {
                    return DATE_AS_TIME_SERIALIZER;
                }
                String pattern = jsonProperty.pattern().trim();
                if (pattern.length() > 0) {
                    String timeZoneId = jsonProperty.timezone().trim();
                    return new DatePatternImpl(DateFormatter.of(pattern), getTimeZone(timeZoneId));
                }
            }
        }
        if (classCategory == ReflectConsts.ClassCategory.NonInstance) {
            // write classname
            return new ObjectImpl.ObjectWithTypeImpl();
        }
        return TYPE_SERIALIZERS[ordinal];
    }

    final static JSONTypeSerializer getMapValueSerializer(Class<?> cls) {
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
    final static JSONTypeSerializer getTypeSerializer(Class<?> cls) {
        JSONTypeSerializer typeSerializer = classJSONTypeSerializerMap.get(cls);
        if (typeSerializer != null) {
            return typeSerializer.ensureInitialized();
        } else {
            synchronized (cls) {
                typeSerializer = classJSONTypeSerializerMap.get(cls);
                if (typeSerializer != null) {
                    return typeSerializer.ensureInitialized();
                }
                ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(cls);
                if (classCategory == ReflectConsts.ClassCategory.ObjectCategory) {
                    ClassStrucWrap classStrucWrap = ClassStrucWrap.get(cls);
                    if (classStrucWrap.isTemporal()) {
                        typeSerializer = JSONTemporalSerializer.getTemporalSerializerInstance(classStrucWrap, null);
                    } else if (classStrucWrap.isSubEnum()) {
                        typeSerializer = ENUM;
                    } else {
                        JSONPojoStructure pojoStructure = JSONPojoStructure.get(cls);
                        if (pojoStructure.isSupportedJIT()) {
                            try {
                                Class<?> serializerClass = JDKCompiler.compileJavaSource(JSONPojoSerializer.generateRuntimeJavaCodeSource(pojoStructure));
                                Constructor constructor = serializerClass.getDeclaredConstructor(new Class[]{JSONPojoStructure.class});
                                UnsafeHelper.setAccessible(constructor);
                                typeSerializer = (JSONPojoSerializer) constructor.newInstance(pojoStructure);
                            } catch (Throwable throwable) {
                                typeSerializer = new ObjectImpl.ObjectWrapperImpl(cls, pojoStructure);
                            }
                        } else {
                            typeSerializer = new ObjectImpl.ObjectWrapperImpl(cls, pojoStructure);
                        }
                    }
                } else {
                    if ((typeSerializer = checkSuperclassRegistered(cls)) == null) {
                        typeSerializer = getTypeSerializer(classCategory, null);
                    }
                }
                classJSONTypeSerializerMap.put(cls, typeSerializer);
                return typeSerializer.ensureInitialized();
            }
        }
    }

    JSONTypeSerializer ensureInitialized() {
        return this;
    }

    static JSONTypeSerializer getEnumSerializer(Class<?> enumClass) {
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

        enumSerializer = new EnumImpl.EnumInstanceImpl(enumNames);
        classJSONTypeSerializerMap.put(enumClass, enumSerializer);
        return enumSerializer;
    }

    static JSONTypeSerializer createCollectionSerializer(Class valueClass) {
        return new CollectionImpl.CollectionFinalTypeImpl(getTypeSerializer(valueClass));
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
    static JSONTypeSerializer getFieldTypeSerializer(ReflectConsts.ClassCategory classCategory, Class<?> type, JsonProperty jsonProperty) {
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
                ClassStrucWrap classStrucWrap = ClassStrucWrap.get(type);
                if (classStrucWrap.isTemporal()) {
                    return JSONTemporalSerializer.getTemporalSerializerInstance(classStrucWrap, jsonProperty);
                } else {
                    if (jsonProperty != null && jsonProperty.unfixedType()) {
                        // auto type
                        return new ObjectImpl.ObjectWithTypeImpl();
                    }
                    return getTypeSerializer(type);
                }
            }
        }
        // others by classCategory
        return JSONTypeSerializer.getTypeSerializer(classCategory, jsonProperty);
    }

    /***
     * 序列化接口
     *
     * @param value
     * @param writer
     * @param jsonConfig
     * @param indent
     */
    protected abstract void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception;

    /***
     * 定制序列化
     *
     * @param value
     * @param writer
     * @param jsonConfig
     * @param indentLevel
     */
    protected void serializeCustomized(Object value, JSONWriter writer, JSONConfig jsonConfig, int indentLevel, JSONCustomMapper customizedMapper) throws Exception {
        serialize(value, writer, jsonConfig, indentLevel);
    }

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
            writer.writeJSONChars((char[]) JSONUnsafe.getStringValue(stringVal));
        }
    }

    // >= jdk9
    final static class StringJDK9PlusImpl extends CharSequenceImpl {

        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            String stringVal = value.toString();
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(stringVal);
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

    final static class BigIntegerImpl extends SimpleNumberImpl {
        protected void serializeNumber(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            writer.writeBigInteger((BigInteger) value);
        }
    }

    // integer/long/byte/short
    final static class LongImpl extends SimpleNumberImpl {

        protected void serializeNumber(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            long numValue = ((Number) value).longValue();
            writer.writeLong(numValue);
        }
    }

    final static class DoubleImpl extends SimpleNumberImpl {

        protected void serializeNumber(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            double numValue = ((Number) value).doubleValue();
            writer.writeDouble(numValue);
        }
    }

    final static class FloatImpl extends SimpleNumberImpl {
        protected void serializeNumber(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            float numValue = ((Number) value).floatValue();
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
        protected final DateFormatter dateFormatter;

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
                        enumFieldNameDatas[i] = new EnumFieldNameData(null, null, UnsafeHelper.getCharLongs(enumNameTokenStr), UnsafeHelper.getByteLongs(enumNameTokenStr), chars.length);
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

    final static class AnnotationImpl extends JSONTypeSerializer {
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            writer.writeJSONToken('"');
            writer.write(value.toString());
            writer.writeJSONToken('"');
        }
    }

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
                writeFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
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
                getTypeSerializer(valueClass).serialize(value, writer, jsonConfig, indentLevel);
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

    final static class ArrayPrimitiveImpl extends ArrayImpl {
        final JSONTypeSerializer valueSerializer;
        final ReflectConsts.PrimitiveType primitiveType;

        public ArrayPrimitiveImpl(JSONTypeSerializer valueSerializer, ReflectConsts.PrimitiveType primitiveType) {
            this.valueSerializer = valueSerializer;
            this.primitiveType = primitiveType;
        }

        @Override
        protected void writeArray(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
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
                writeFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                writer.writeJSONToken(']');
            } else {
                writer.writeEmptyArray();
            }
        }
    }

    static class CollectionImpl extends JSONTypeSerializer {

        protected void writeCollection(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            boolean formatOut = jsonConfig.isFormatOut();
            Collection<?> collect = (Collection<?>) obj;
            if (collect.size() > 0) {
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
                        writer.writeNull();
                    }
                }
                if (!isEmptyFlag) {
                    writeFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
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

        @Override
        protected final void serializeCustomized(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel, JSONCustomMapper customizedMapper) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    writer.writeNull();
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            boolean formatOut = jsonConfig.isFormatOut();
            Collection<?> collect = (Collection<?>) obj;
            if (collect.size() > 0) {
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
                            firstSerializer.serializeCustomized(value, writer, jsonConfig, indentLevelPlus, customizedMapper);
                        } else {
                            if (firstElementClass == null) {
                                firstElementClass = valueClass;
                                firstSerializer = customizedMapper.getCustomizedSerializer(valueClass);
                                firstSerializer.serializeCustomized(value, writer, jsonConfig, indentLevelPlus, customizedMapper);
                            } else {
                                getTypeSerializer(valueClass).serializeCustomized(value, writer, jsonConfig, indentLevelPlus, customizedMapper);
                            }
                        }
                    } else {
                        writer.writeNull();
                    }
                }
                if (!isEmptyFlag) {
                    writeFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                }
                writer.write(']');
            } else {
                writer.writeEmptyArray();
            }
            jsonConfig.setStatus(hashcode, -1);
        }

        final static class CollectionFinalTypeImpl extends CollectionImpl {
            private final JSONTypeSerializer valueSerializer;

            CollectionFinalTypeImpl(JSONTypeSerializer valueSerializer) {
                this.valueSerializer = valueSerializer;
            }

            @Override
            protected void writeCollection(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
                boolean formatOut = jsonConfig.isFormatOut();
                Collection<Object> collect = (Collection<Object>) obj;
                if (collect.size() > 0) {
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
                        writeFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                    }
                    writer.write(']');
                } else {
                    writer.writeEmptyArray();
                }
            }
        }
    }

    static class MapImpl extends JSONTypeSerializer {

        private static void writeMap(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
            boolean formatOut = jsonConfig.isFormatOut();
            boolean formatOutColonSpace = formatOut && jsonConfig.isFormatOutColonSpace();
            Map<Object, Object> map = (Map<Object, Object>) obj;
            if (map.size() == 0) {
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
                        writer.writeJSONKeyAndColon(stringKey);
                    }
                    if (formatOutColonSpace) {
                        writer.writeJSONToken(' ');
                    }
                    if (value != null) {
                        JSONTypeSerializer valueSerializer = getMapValueSerializer(value.getClass()); // getValueSerializer(value);
                        valueSerializer.serialize(value, writer, jsonConfig, formatOut ? indentLevelPlus : -1);
                    } else {
                        writer.writeNull();
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
                    writer.writeNull();
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            writeMap(obj, writer, jsonConfig, indent);
            jsonConfig.setStatus(hashcode, -1);
        }

        @Override
        protected final void serializeCustomized(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel, JSONCustomMapper customizedMapper) throws Exception {
            int hashcode = -1;
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    writer.writeNull();
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            boolean formatOut = jsonConfig.isFormatOut();
            boolean formatOutColonSpace = formatOut && jsonConfig.isFormatOutColonSpace();
            Map<Object, Object> map = (Map<Object, Object>) obj;
            if (map.size() == 0) {
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
                        writer.writeJSONKeyAndColon(stringKey);
                    }
                    if (formatOutColonSpace) {
                        writer.writeJSONToken(' ');
                    }
                    if (value != null) {
                        JSONTypeSerializer valueSerializer = customizedMapper.getCustomizedSerializer(value.getClass());
                        valueSerializer.serializeCustomized(value, writer, jsonConfig, formatOut ? indentLevelPlus : -1, customizedMapper);
                    } else {
                        writer.writeNull();
                    }
                }
                writeFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
                writer.write('}');
            }
            jsonConfig.setStatus(hashcode, -1);
        }
    }

    static class ObjectImpl extends JSONTypeSerializer {

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
                writeFormatOutSymbols(writer, indentLevel, formatOut, jsonConfig);
            }
            writer.write('}');
            jsonConfig.setStatus(hashcode, -1);
        }

        JSONPojoStructure getPojoStructure(Class clazz) {
            JSONPojoStructure pojoStructure = JSONPojoStructure.get(clazz);
            pojoStructure.ensureInitializedFieldSerializers();
            return pojoStructure;
        }

        final static class ObjectWithTypeImpl extends ObjectImpl {
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

        final static class ObjectWrapperImpl extends ObjectImpl {
            final JSONPojoStructure pojoStructure;
            final Class objectCls;

            ObjectWrapperImpl(Class cls, JSONPojoStructure pojoStructure) {
                this.objectCls = cls;
                this.pojoStructure = pojoStructure;
            }

            JSONPojoStructure getPojoStructure(Class clazz) {
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

    final static class ToStringImpl extends JSONTypeSerializer {
        @Override
        protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
            CHAR_SEQUENCE_STRING.serialize(value.toString(), writer, jsonConfig, indent);
        }
    }
}
