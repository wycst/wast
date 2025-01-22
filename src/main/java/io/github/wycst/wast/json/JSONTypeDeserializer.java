package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.DateTemplate;
import io.github.wycst.wast.common.reflect.ClassStrucWrap;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.*;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.exceptions.JSONException;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类型反序列器
 *
 * @Author: wangy
 * @Date: 2022/6/18 5:59
 * @Description:
 */
public abstract class JSONTypeDeserializer extends JSONGeneral {

    final static int LENGTH = ReflectConsts.ClassCategory.values().length;

    final static JSONTypeDeserializer[] TYPE_DESERIALIZERS = new JSONTypeDeserializer[LENGTH];
    final static CharSequenceImpl CHAR_SEQUENCE = new CharSequenceImpl();
    protected final static CharSequenceImpl CHAR_SEQUENCE_STRING;
    protected final static NumberImpl NUMBER = new NumberImpl();
    protected final static NumberImpl.Skipper NUMBER_SKIPPER = new NumberImpl.Skipper();
    protected final static NumberImpl NUMBER_LONG = new NumberImpl.LongImpl();
    protected final static NumberImpl NUMBER_INTEGER = new NumberImpl.IntegerImpl();
    protected final static NumberImpl NUMBER_SHORT = new NumberImpl.ShortImpl();
    protected final static NumberImpl NUMBER_BYTE = new NumberImpl.ByteImpl();
    protected final static NumberImpl NUMBER_FLOAT = new NumberImpl.FloatImpl();
    protected final static NumberImpl NUMBER_DOUBLE = new NumberImpl.DoubleImpl();
    protected final static NumberImpl NUMBER_BIGDECIMAL = new NumberImpl.BigDecimalImpl();
    protected final static NumberImpl NUMBER_BIGINTEGER = new NumberImpl.BigIntegerImpl();
    protected final static BinaryImpl BINARY = new BinaryImpl();
    protected final static ArrayImpl.IntArrayImpl INTEGER_ARRAY = new ArrayImpl.IntArrayImpl();
    final static BooleanImpl BOOLEAN = new BooleanImpl();
    final static DateImpl DATE = new DateImpl();
    protected final static ArrayImpl ARRAY = new ArrayImpl();
    protected final static CollectionImpl COLLECTION = new CollectionImpl();
    protected final static MapImpl MAP = new MapImpl();
    protected final static ObjectImpl OBJECT = new ObjectImpl();
    protected final static JSONTypeDeserializer ANY = new AnyImpl();
    //    protected final static NullImpl NULL = new NullImpl();
    protected final static SerializableImpl SERIALIZABLE_DESERIALIZER = new SerializableImpl();

    // class and JSONTypeDeserializer mapping
    private static final Map<Class<?>, JSONTypeDeserializer> classJSONTypeDeserializerMap = new ConcurrentHashMap<Class<?>, JSONTypeDeserializer>(256);
    private static final Set<Class> REGISTERED_TYPES = new HashSet<Class>();
    // classname mapping
    private static final Map<String, Class<?>> classNameMapping = new ConcurrentHashMap<String, Class<?>>();

    static final Set<Class<?>> BUILT_IN_TYPE_SET;

    static {
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.CharSequence.ordinal()] = CHAR_SEQUENCE;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.NumberCategory.ordinal()] = NUMBER;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.BoolCategory.ordinal()] = BOOLEAN;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.DateCategory.ordinal()] = DATE;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ClassCategory.ordinal()] = new ClassImpl();
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.EnumCategory.ordinal()] = new EnumImpl();
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.AnnotationCategory.ordinal()] = new AnnotationImpl();
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.Binary.ordinal()] = BINARY;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ArrayCategory.ordinal()] = ARRAY;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.CollectionCategory.ordinal()] = COLLECTION;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.MapCategory.ordinal()] = MAP;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ObjectCategory.ordinal()] = OBJECT;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ANY.ordinal()] = ANY;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.NonInstance.ordinal()] = null;

        putTypeDeserializer(BOOLEAN, boolean.class, Boolean.class);
        putTypeDeserializer(NUMBER_LONG, long.class, Long.class);
        putTypeDeserializer(NUMBER_INTEGER, int.class, Integer.class);
        putTypeDeserializer(NUMBER_DOUBLE, double.class, Double.class);
        putTypeDeserializer(NUMBER_FLOAT, float.class, Float.class);
        putTypeDeserializer(NUMBER_SHORT, short.class, Short.class);
        putTypeDeserializer(NUMBER_BYTE, byte.class, Byte.class);
        putTypeDeserializer(NUMBER_BIGDECIMAL, BigDecimal.class);
        putTypeDeserializer(NUMBER_BIGINTEGER, BigInteger.class);

        putTypeDeserializer(new ArrayImpl.StringArrayImpl(), String[].class);
        putTypeDeserializer(new ArrayImpl.DoubleArrayImpl(), Double[].class);
        putTypeDeserializer(new ArrayImpl.PrimitiveDoubleArrayImpl(), double[].class);
        putTypeDeserializer(new ArrayImpl.FloatArrayImpl(), Float[].class);
        putTypeDeserializer(new ArrayImpl.PrimitiveFloatArrayImpl(), float[].class);
        putTypeDeserializer(new ArrayImpl.LongArrayImpl(), Long[].class);
        putTypeDeserializer(new ArrayImpl.PrimitiveLongArrayImpl(), long[].class);
        putTypeDeserializer(new ArrayImpl.IntArrayImpl(), Integer[].class);
        putTypeDeserializer(new ArrayImpl.PrimitiveIntArrayImpl(), int[].class);
        putTypeDeserializer(new ArrayImpl.ByteArrayImpl(), Byte[].class);
        putTypeDeserializer(BINARY, byte[].class);

        CharSequenceImpl stringDeserializer;
        if (EnvUtils.JDK_9_PLUS) {
            if (EnvUtils.JDK_16_PLUS) {
                stringDeserializer = new CharSequenceImpl.StringJDK16PlusImpl();
            } else {
                // JDK9~JDK15
                stringDeserializer = new CharSequenceImpl.StringJDK9PlusImpl();
            }
        } else {
            stringDeserializer = new CharSequenceImpl.StringJDK8Impl();
        }

        CHAR_SEQUENCE_STRING = stringDeserializer;
        putTypeDeserializer(CHAR_SEQUENCE_STRING, String.class, CharSequence.class);
        putTypeDeserializer(CHAR_SEQUENCE, StringBuilder.class, StringBuffer.class, char[].class);
        putTypeDeserializer(SERIALIZABLE_DESERIALIZER, Serializable.class);
        // other types
        putTypeDeserializer(new CharSequenceImpl.CharImpl(), char.class, Character.class);
        MapImpl HASH_TABLE = MapImpl.hashtable();
        putTypeDeserializer(HASH_TABLE, Dictionary.class, Hashtable.class);
        putTypeDeserializer(MAP, Map.class, HashMap.class, LinkedHashMap.class);
        putTypeDeserializer(COLLECTION, List.class, Set.class);
        // extension types
        JSONTypeExtensionDesr.initExtens();
        BUILT_IN_TYPE_SET = new HashSet<Class<?>>(classJSONTypeDeserializerMap.keySet());
    }

    static final boolean isBuiltInType(Class<?> type) {
        return BUILT_IN_TYPE_SET.contains(type);
    }

    static final void putTypeDeserializer(JSONTypeDeserializer typeDeserializer, Class... types) {
        for (Class type : types) {
            classJSONTypeDeserializerMap.put(type, typeDeserializer);
        }
    }

    static final void registerTypeDeserializer(JSONTypeDeserializer typeDeserializer, Class type) {
        classJSONTypeDeserializerMap.put(type, typeDeserializer);
        REGISTERED_TYPES.add(type);
    }

    static final JSONTypeDeserializer checkSuperclassRegistered(Class<?> cls) {
        for (Class type : REGISTERED_TYPES) {
            if (type.isAssignableFrom(cls)) {
                return classJSONTypeDeserializerMap.get(type);
            }
        }
        return null;
    }

    static final JSONTypeDeserializer getCachedTypeDeserializer(Class type) {
        return classJSONTypeDeserializerMap.get(type);
    }

    // Get the corresponding deserializer according to the type
    // Single example may be used
    public final static JSONTypeDeserializer getTypeDeserializer(Class type) {
        if (type == null) return ANY;
        JSONTypeDeserializer typeDeserializer = classJSONTypeDeserializerMap.get(type);
        if (typeDeserializer != null) {
            return typeDeserializer.ensureInitialized();
        }
        ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(type);
        synchronized (type) {
            if (classJSONTypeDeserializerMap.containsKey(type)) {
                return classJSONTypeDeserializerMap.get(type).ensureInitialized();
            }
            switch (classCategory) {
                case ObjectCategory: {
                    typeDeserializer = createObjectDeserializer(type);
                    break;
                }
                case EnumCategory: {
                    typeDeserializer = createEnumDeserializer(type);
                    break;
                }
                case CollectionCategory: {
                    Class<? extends Collection> collectionClass = type;
                    GenericParameterizedType parameterizedType = GenericParameterizedType.collectionOf(collectionClass);
                    typeDeserializer = new CollectionImpl.CollectionInstanceImpl(parameterizedType);
                    break;
                }
                case MapCategory: {
                    Class<? extends Map> mapClass = type;
                    GenericParameterizedType parameterizedType = GenericParameterizedType.mapOf(mapClass);
                    typeDeserializer = new MapInstanceImpl(parameterizedType);
                    break;
                }
                case NonInstance: {
                    typeDeserializer = new NonInstanceImpl(type);
                    break;
                }
            }
            if (typeDeserializer == null) {
                if ((typeDeserializer = checkSuperclassRegistered(type)) == null) {
                    typeDeserializer = TYPE_DESERIALIZERS[classCategory.ordinal()];
                }
                if (typeDeserializer == null) {
                    return null;
                }
            }
            classJSONTypeDeserializerMap.put(type, typeDeserializer);
            typeDeserializer.ensureInitialized();
        }

        return typeDeserializer;
    }

    JSONTypeDeserializer ensureInitialized() {
        return this;
    }

    final static JSONTypeDeserializer createObjectDeserializer(Class type) {
        ClassStrucWrap classStrucWrap = ClassStrucWrap.get(type);
        if (classStrucWrap.isRecord()) {
            return new JSONPojoDeserializer.RecordImpl(JSONPojoStructure.get(type));
        } else {
            if (classStrucWrap.isTemporal()) {
                ClassStrucWrap.ClassWrapperType classWrapperType = classStrucWrap.getClassWrapperType();
                return JSONTemporalDeserializer.getTemporalDeserializerInstance(classWrapperType, GenericParameterizedType.actualType(type), null);
            }
            return new JSONPojoDeserializer(JSONPojoStructure.get(type));
        }
    }

    final static JSONTypeDeserializer createEnumDeserializer(Class type) {
        Enum[] values = (Enum[]) type.getEnumConstants();
        Map<String, Enum> enumValues = new HashMap<String, Enum>();
        for (Enum value : values) {
            String name = value.name();
            enumValues.put(name, value);
        }
        JSONValueMatcher<Enum> enumMatcher = JSONValueMatcher.build(enumValues);
        return new EnumImpl.EnumInstanceImpl(values, enumMatcher);
    }

    /**
     * use for FieldDeserializer
     *
     * @param genericParameterizedType
     * @param property
     * @return
     */
    protected final static JSONTypeDeserializer getFieldDeserializer(GenericParameterizedType genericParameterizedType, JsonProperty property) {
        if (genericParameterizedType == null || genericParameterizedType.getActualType() == null) return ANY;
        ReflectConsts.ClassCategory classCategory = genericParameterizedType.getActualClassCategory();
        // Find matching deserializers or new deserializer instance by type
        switch (classCategory) {
            case CollectionCategory:
                // collection Deserializer instance
                int collectionType = getCollectionType(genericParameterizedType.getActualType());
                switch (collectionType) {
                    case COLLECTION_ARRAYLIST_TYPE:
                        return new CollectionImpl.ArrayListImpl(genericParameterizedType);
                    case COLLECTION_HASHSET_TYPE:
                        return new CollectionImpl.HashSetImpl(genericParameterizedType);
                    default:
                        return new CollectionImpl.CollectionInstanceImpl(genericParameterizedType);
                }
            case DateCategory: {
                // Date Deserializer Instance
                // cannot use singleton because the pattern of each field may be different
                return property == null ? DATE : new DateImpl.DateInstanceImpl(genericParameterizedType, property);
            }
            case ObjectCategory: {
                ClassStrucWrap classStrucWrap = ClassStrucWrap.get(genericParameterizedType.getActualType());
                // Like the date type, the temporal type cannot use singletons also
                if (classStrucWrap.isTemporal()) {
                    ClassStrucWrap.ClassWrapperType classWrapperType = classStrucWrap.getClassWrapperType();
                    return JSONTemporalDeserializer.getTemporalDeserializerInstance(classWrapperType, genericParameterizedType, property);
                }
            }
        }

        // singleton from cache
        return getTypeDeserializer(genericParameterizedType.getActualType());
    }

    protected final static Object doDeserialize(JSONTypeDeserializer deserializer, CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext parseContext) throws Exception {
        return deserializer.deserialize(charSource, buf, fromIndex, parameterizedType, defaultValue, endToken, parseContext);
    }

    protected final static Object doDeserialize(JSONTypeDeserializer deserializer, CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext parseContext) throws Exception {
        return deserializer.deserialize(charSource, buf, fromIndex, parameterizedType, defaultValue, endToken, parseContext);
    }

    /***
     * 返回绑定类型
     *
     * @return
     */
    protected <T> GenericParameterizedType getGenericParameterizedType(Class<T> actualType) {
        return GenericParameterizedType.actualType(actualType);
    }

    /**
     * 跳过反序列
     *
     * @param buf
     * @param fromIndex
     * @param endToken
     * @param parseContext
     * @throws Exception
     */
    void skip(CharSource charSource, char[] buf, int fromIndex, char endToken, JSONParseContext parseContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * 跳过反序列
     *
     * @param buf
     * @param fromIndex
     * @param endToken
     * @param parseContext
     * @throws Exception
     */
    void skip(CharSource charSource, byte[] buf, int fromIndex, byte endToken, JSONParseContext parseContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * 校验JSON是否正确(短路模式)
     *
     * @param buf
     * @param fromIndex
     * @param toIndex
     * @param endToken
     * @param parseContext
     * @throws Exception
     */
    boolean validate(CharSource charSource, char[] buf, int fromIndex, int toIndex, char endToken, JSONParseContext parseContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * 校验JSON是否正确(短路模式)
     *
     * @param buf
     * @param fromIndex
     * @param toIndex
     * @param endToken
     * @param parseContext
     * @throws Exception
     */
    boolean validate(CharSource charSource, byte[] buf, int fromIndex, int toIndex, byte endToken, JSONParseContext parseContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * 核心反序列化
     *
     * @param buf
     * @param fromIndex
     * @param parseContext
     * @throws Exception
     */
    protected abstract Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext parseContext) throws Exception;

    /**
     * 拓展反序列化
     *
     * @param charSource   字符源（AsciiStringSource对象）
     * @param bytes        ascii字节数组
     * @param fromIndex
     * @param parseContext
     * @throws Exception
     */
    protected abstract Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext parseContext) throws Exception;

    /**
     * 将字符串转化为指定实例
     *
     * @param value
     * @param actualType
     * @return
     */
    protected Object valueOf(String value, Class<?> actualType) throws Exception {
        throw new JSONException("string value \"" + value + "\" is not supported " + actualType);
    }

    // 0、字符串序列化
    static class CharSequenceImpl extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) throws Exception {
            if (actualType == char[].class) {
                return value.toCharArray();
            }
            if (actualType == StringBuilder.class) {
                return new StringBuilder(value);
            }
            if (actualType == StringBuffer.class) {
                return new StringBuffer(value);
            }
            return value;
        }

        protected void skip(CharSource source, char[] buf, int fromIndex, char endCh, JSONParseContext parseContext) throws Exception {
            if (source == null) {
                int i = fromIndex;
                while (buf[++i] != endCh) ;
                char prev = buf[i - 1];
                while (prev == '\\') {
                    boolean prevEscapeFlag = true;
                    int j = i - 1;
                    while (buf[--j] == '\\') {
                        prevEscapeFlag = !prevEscapeFlag;
                    }
                    if (prevEscapeFlag) {
                        while (buf[++i] != endCh) ;
                        prev = buf[i - 1];
                    } else {
                        break;
                    }
                }
                parseContext.endIndex = i;
            } else {
                final String input = source.input();
                int beginIndex = fromIndex + 1;
                int endIndex = input.indexOf(endCh, beginIndex);
                char prev = buf[endIndex - 1];
                while (prev == '\\') {
                    boolean prevEscapeFlag = true;
                    int j = endIndex - 1;
                    while (buf[--j] == '\\') {
                        prevEscapeFlag = !prevEscapeFlag;
                    }
                    if (prevEscapeFlag) {
                        endIndex = input.indexOf(endCh, endIndex + 1);
                        prev = buf[endIndex - 1];
                    } else {
                        break;
                    }
                }
                parseContext.endIndex = endIndex;
            }
        }

        protected void skip(CharSource source, byte[] buf, int fromIndex, byte endCh, JSONParseContext parseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            if (source != null) {
                final String input = source.input();
                int endIndex = input.indexOf(endCh, beginIndex);
                byte prev = buf[endIndex - 1];
                while (prev == '\\') {
                    boolean prevEscapeFlag = true;
                    int j = endIndex - 1;
                    while (buf[--j] == '\\') {
                        prevEscapeFlag = !prevEscapeFlag;
                    }
                    if (prevEscapeFlag) {
                        endIndex = input.indexOf(endCh, endIndex + 1);
                        prev = buf[endIndex - 1];
                    } else {
                        break;
                    }
                }
                parseContext.endIndex = endIndex;
                return;
            }

            int i = fromIndex;
            while (buf[++i] != endCh) ;
            byte prev = buf[i - 1];
            while (prev == '\\') {
                boolean prevEscapeFlag = true;
                int j = i - 1;
                while (buf[--j] == '\\') {
                    prevEscapeFlag = !prevEscapeFlag;
                }
                if (prevEscapeFlag) {
                    while (buf[++i] != endCh) ;
                    prev = buf[i - 1];
                } else {
                    break;
                }
            }
            parseContext.endIndex = i;
        }

        // StringBuilder/StringBuffer/chars
        Object deserializeString(CharSource charSource, byte[] buf, int fromIndex, int endByte, GenericParameterizedType parameterizedType, JSONParseContext parseContext) {
            String value = (String) CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, endByte, parameterizedType, parseContext);
            Class<?> actualType = parameterizedType.getActualType();
            if (actualType == char[].class) {
                return value.toCharArray();
            } else if (actualType == StringBuilder.class) {
                return new StringBuilder(value);
            } else {
                return new StringBuffer(value);
            }
        }

        // StringBuilder/StringBuffer/chars
        Object deserializeString(CharSource charSource, char[] buf, int fromIndex, char endChar, GenericParameterizedType parameterizedType, JSONParseContext parseContext) {
            String value = (String) CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, endChar, parameterizedType, parseContext);
            Class<?> actualType = parameterizedType.getActualType();
            if (actualType == char[].class) {
                return value.toCharArray();
            } else if (actualType == StringBuilder.class) {
                return new StringBuilder(value);
            } else {
                return new StringBuffer(value);
            }
        }

        // String/StringBuffer/StringBuilder/char[]
        @Override
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext parseContext) throws Exception {
            char beginChar = buf[fromIndex];
            if (beginChar == '"') {
                return deserializeString(charSource, buf, fromIndex, beginChar, parameterizedType, parseContext);
            } else if (beginChar == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            } else {
                if (parameterizedType == GenericParameterizedType.StringType) {
                    try {
                        NUMBER_SKIPPER.deserialize(charSource, buf, fromIndex, null, null, endToken, parseContext);
                        return new String(buf, fromIndex, parseContext.endIndex + 1 - fromIndex);
                    } catch (Throwable throwable) {
                    }
                }
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for CharSequence, expected '\"' ");
            }
        }

        @Override
        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext parseContext) throws Exception {
            byte b = buf[fromIndex];
            if (b == '"') {
                return deserializeString(charSource, buf, fromIndex, b, parameterizedType, parseContext);
            } else if (b == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            } else {
                // 当类型为字符串时兼容number转化
                boolean isStringType = parameterizedType.getActualType() == String.class;
                if (isStringType) {
                    try {
                        NUMBER_SKIPPER.deserialize(charSource, buf, fromIndex, null, null, endToken, parseContext);
                        return new String(buf, fromIndex, parseContext.endIndex + 1 - fromIndex);
                    } catch (Throwable throwable) {
                    }
                }
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "' for CharSequence, expected '\"' ");
            }
        }

        // JDK(6-8)
        final static class StringJDK8Impl extends CharSequenceImpl {

            String deserializeString(CharSource charSource, byte[] buf, int fromIndex, int endByte, GenericParameterizedType parameterizedType, JSONParseContext parseContext) {
                JSONCharArrayWriter writer = getContextWriter(parseContext);
                char[] chars = writer.ensureCapacity(buf.length);
                int count = writer.count;
                byte b, b0 = 0;
                int i = fromIndex;
                for (; ; ) {
                    boolean bEndFlag;
                    // Read 2 bytes per iteration
                    while ((bEndFlag = ((b = buf[++i]) != '\\' && b != endByte)) && (b0 = buf[++i]) != '\\' && b0 != endByte) {
                        if (b0 >= 0) {
                            chars[count++] = (char) b;
                            chars[count++] = (char) b0;
                        } else {
                            byte b1;
                            if (b >= 0) {
                                // b >= 0 & b0 < 0
                                chars[count++] = (char) b;
                                b = b0;
                                b1 = buf[++i];
                            } else {
                                // b < 0 & b0 < 0
                                b1 = b0;
                            }
                            // UTF-8 decode
                            int s = b >> 4;
                            // 读取字节b的前4位判断需要读取几个字节
                            if (s == -2) {
                                // 1110 3个字节
                                try {
                                    // 第1个字节的后4位 + 第2个字节的后6位 + 第3个字节的后6位
                                    byte b2 = buf[++i];
                                    int a = ((b & 0xf) << 12) | ((b1 & 0x3f) << 6) | (b2 & 0x3f);
                                    chars[count++] = (char) a;
                                } catch (Throwable throwable) {
                                    throw new UnsupportedOperationException("utf-8 character error ");
                                }
                            } else if (s == -3 || s == -4) {
                                // 1100 2 bytes
                                try {
                                    int a = ((b & 0x1f) << 6) | (b1 & 0x3f);
                                    chars[count++] = (char) a;
                                } catch (Throwable throwable) {
                                    throw new UnsupportedOperationException("utf-8 character error ");
                                }
                            } else if (s == -1) {
                                // 1111 4个字节
                                try {
                                    // 第1个字节的后4位 + 第2个字节的后6位 + 第3个字节的后6位 + 第4个字节的后6位
                                    byte b2 = buf[++i];
                                    byte b3 = buf[++i];
                                    int a = ((b & 0x7) << 18) | ((b1 & 0x3f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f);
                                    if (Character.isSupplementaryCodePoint(a)) {
                                        chars[count++] = (char) ((a >>> 10)
                                                + (Character.MIN_HIGH_SURROGATE - (Character.MIN_SUPPLEMENTARY_CODE_POINT >>> 10)));
                                        chars[count++] = (char) ((a & 0x3ff) + Character.MIN_LOW_SURROGATE);
                                    } else {
                                        chars[count++] = (char) a;
                                    }
                                } catch (Throwable throwable) {
                                    throw new UnsupportedOperationException("utf-8 character error ");
                                }
                            } else {
                                throw new UnsupportedOperationException("utf-8 character error ");
                            }
                        }
                    }
                    // b > 0 && b0 > 0
                    if (bEndFlag) {
                        // b0 == endByte or b0 == '\\'
                        chars[count++] = (char) b;
                        b = b0;
                    }
                    if (b == endByte) {
                        parseContext.endIndex = i;
                        writer.count = count;
                        return new String(chars, 0, count); // charSequenceResult(writer, parameterizedType);
                    } else {
                        byte next = '\0';
                        if (i < parseContext.toIndex - 1) {
                            next = buf[i + 1];
                        }
                        writer.count = count;
                        escape(buf, next, i, i, writer, parseContext);
                        count = writer.count;
                        i = parseContext.endIndex;
                    }
                }
            }

            @Override
            String deserializeString(CharSource source, char[] buf, int fromIndex, char endChar, GenericParameterizedType parameterizedType, JSONParseContext parseContext) {
                int beginIndex = fromIndex + 1;
                JSONCharArrayWriter writer = null;
                char ch;
                int i = beginIndex;
                int len;
                boolean unEscape = true;
                for (; ; ++i) {
                    while ((ch = buf[i]) != '\\' && ch != endChar) {
                        ++i;
                    }
                    if (ch == endChar) {
                        parseContext.endIndex = i;
                        len = i - beginIndex;
                        if (unEscape) {
                            return JSONUnsafe.createString(buf, beginIndex, len);
                        } else {
                            writer.write(buf, beginIndex, len);
                            return writer.toString();
                        }
                    } else {
                        if (writer == null) {
                            writer = getContextWriter(parseContext);
                            unEscape = false;
                        }
                        beginIndex = escapeNext(buf, buf[i + 1], i, beginIndex, writer, parseContext);
                        i = parseContext.endIndex;
                    }
                }
            }
        }

        // JDK(9-15)
        final static class StringJDK9PlusImpl extends CharSequenceImpl {

            String deserializeString(CharSource charSource, char[] buf, int fromIndex, char endChar, GenericParameterizedType parameterizedType, JSONParseContext parseContext) {
                int beginIndex = fromIndex + 1;
                String token = endChar == '"' ? "\"" : "'";
                String source = charSource.input();
                int endIndex = source.indexOf(token, beginIndex);
                if (!parseContext.checkEscapeUseString(source, beginIndex, endIndex)) {
                    parseContext.endIndex = endIndex;
                    return new String(buf, beginIndex, endIndex - beginIndex);
                }
                // must exist \\ in range {beginIndex, endIndex}
                JSONCharArrayWriter writer = getContextWriter(parseContext);
                do {
                    int escapeIndex = parseContext.getEscapeOffset();
                    beginIndex = escapeNext(buf, buf[escapeIndex + 1], escapeIndex, beginIndex, writer, parseContext);
                    if (beginIndex > endIndex) {
                        endIndex = source.indexOf(endChar, endIndex + 1);
                    }
                } while (parseContext.checkEscapeUseChar(source, beginIndex, endIndex));

                parseContext.endIndex = endIndex;
                writer.write(charSource.input(), beginIndex, endIndex - beginIndex);
                return writer.toString();
            }

            String deserializeString(CharSource charSource, byte[] buf, int fromIndex, int endByte, GenericParameterizedType parameterizedType, JSONParseContext parseContext) {
                String source = charSource.input();
                String token = endByte == '"' ? "\"" : "'";
                int beginIndex = fromIndex + 1;
                int endIndex = source.indexOf(token, beginIndex);
                if (!parseContext.checkEscapeUseString(source, beginIndex, endIndex)) {
                    parseContext.endIndex = endIndex;
                    return charSource.substring(buf, beginIndex, endIndex);
                }
                JSONCharArrayWriter writer = getContextWriter(parseContext);
                do {
                    int escapeIndex = parseContext.getEscapeOffset();
                    beginIndex = escapeAscii(source, buf, buf[escapeIndex + 1], escapeIndex, beginIndex, writer, parseContext);
                    if (beginIndex > endIndex) {
                        endIndex = source.indexOf(token, endIndex + 1);
                    }
                } while (parseContext.checkEscapeUseString(source, beginIndex, endIndex));

                parseContext.endIndex = endIndex;
                writer.write(source, beginIndex, endIndex - beginIndex);
                return writer.toString();
            }
        }

        // JDK(16+)
        final static class StringJDK16PlusImpl extends CharSequenceImpl {

            String deserializeString(CharSource charSource, char[] buf, int fromIndex, char endChar, GenericParameterizedType parameterizedType, JSONParseContext parseContext) {
                int beginIndex = fromIndex + 1;
                String source = charSource.input();
                int endIndex = source.indexOf(endChar, beginIndex);
                if (!parseContext.checkEscapeUseChar(source, beginIndex, endIndex)) {
                    parseContext.endIndex = endIndex;
                    return new String(buf, beginIndex, endIndex - beginIndex);
                }
                // must exist \\ in range {beginIndex, endIndex}
                JSONCharArrayWriter writer = getContextWriter(parseContext);
                do {
                    int escapeIndex = parseContext.getEscapeOffset();
                    beginIndex = escapeNext(buf, buf[escapeIndex + 1], escapeIndex, beginIndex, writer, parseContext);
                    if (beginIndex > endIndex) {
                        endIndex = source.indexOf(endChar, endIndex + 1);
                    }
                } while (parseContext.checkEscapeUseChar(source, beginIndex, endIndex));

                parseContext.endIndex = endIndex;
                writer.write(charSource.input(), beginIndex, endIndex - beginIndex);
                return writer.toString();
            }

            String deserializeString(CharSource charSource, byte[] buf, int fromIndex, int endByte, GenericParameterizedType parameterizedType, JSONParseContext parseContext) {
                String source = charSource.input();
                int beginIndex = fromIndex + 1;
                int endIndex = source.indexOf(endByte, beginIndex);
                if (!parseContext.checkEscapeUseChar(source, beginIndex, endIndex)) {
                    parseContext.endIndex = endIndex;
                    return charSource.substring(buf, beginIndex, endIndex);
                }
                JSONCharArrayWriter writer = getContextWriter(parseContext);
                do {
                    int escapeIndex = parseContext.getEscapeOffset();
                    beginIndex = escapeAscii(source, buf, buf[escapeIndex + 1], escapeIndex, beginIndex, writer, parseContext);
                    if (beginIndex > endIndex) {
                        endIndex = source.indexOf(endByte, endIndex + 1);
                    }
                } while (parseContext.checkEscapeUseChar(source, beginIndex, endIndex));

                parseContext.endIndex = endIndex;
                writer.write(source, beginIndex, endIndex - beginIndex);
                return writer.toString();
            }
        }

        static final class CharImpl extends CharSequenceImpl {

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return value.charAt(0);
            }

            @Override
            protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext parseContext) throws Exception {
                char firstChar = buf[fromIndex];
                switch (firstChar) {
                    case '\'':
                    case '\"': {
                        char result = 0;
                        String value = (String) CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, firstChar, null, parseContext);
                        if (value.length() > 0) {
                            result = value.charAt(0);
                        }
                        return result;
                    }
                    case 'n': {
                        return parseNull(buf, fromIndex, parseContext);
                    }
                    default: {
                        // numbder
                        short value = (Short) NUMBER_SHORT.deserialize(charSource, buf, fromIndex, parameterizedType, instance, endToken, parseContext);
                        return (char) value;
                    }
                }
            }

            @Override
            protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext parseContext) throws Exception {
                byte firstByte = buf[fromIndex];
                switch (firstByte) {
                    case '\'':
                    case '\"': {
                        char result = 0;
                        String value = (String) CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, firstByte, null, parseContext);
                        if (value.length() > 0) {
                            result = value.charAt(0);
                        }
                        return result;
                    }
                    case 'n': {
                        return parseNull(buf, fromIndex, parseContext);
                    }
                    default: {
                        // numbder
                        short value = (Short) NUMBER_SHORT.deserialize(charSource, buf, fromIndex, parameterizedType, defaultValue, endToken, parseContext);
                        return (char) value;
                    }
                }
            }
        }
    }

    // 1、number
    static class NumberImpl extends JSONTypeDeserializer {

        protected NumberImpl() {
        }

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return Double.parseDouble(value);
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext parseContext) throws Exception {
            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9': {
                    return deserializeNumber(beginChar & 0xf, false, 1, buf, fromIndex, ++fromIndex, parameterizedType, endToken, parseContext);
                }
                default: {
                    switch (beginChar) {
                        case '0':
                        case '+':
                            return deserializeNumber(0, false, 0, buf, fromIndex, ++fromIndex, parameterizedType, endToken, parseContext);
                        case '-':
                            return deserializeNumber(0, true, 0, buf, fromIndex, ++fromIndex, parameterizedType, endToken, parseContext);
                        case '"': {
                            try {
                                // 兼容字符串转化,存在嵌套\"\"问题
                                char ch = buf[++fromIndex];
                                if (ch == '"') {
                                    // discovered empty ""
                                    if (parseContext.unMatchedEmptyAsNull) {
                                        parseContext.endIndex = fromIndex;
                                        return null;
                                    }
                                    // if return null?
                                    throw new JSONException("Syntax error, at pos " + fromIndex + ", unexpected empty string '\"\"' when parsing a number, use ReadOption.UnMatchedEmptyAsNull to support");
                                }
                                Number result = (Number) deserialize(charSource, buf, fromIndex, parameterizedType, instance, '"', parseContext);
                                int endIndex = parseContext.endIndex;
                                int endChar = parseContext.endToken;
                                if (endChar == 0) {
                                    if (buf[++endIndex] != '"') {
                                        throw new JSONException("Syntax error, for input: \"" + new String(buf, fromIndex, endIndex - fromIndex) + "\", unable to convert to number");
                                    }
                                    parseContext.endIndex = endIndex;
                                } else {
                                    parseContext.endToken = 0;
                                    if (endChar != '"') {
                                        throw new JSONException("Syntax error, for input: \"" + new String(buf, fromIndex, endIndex - fromIndex) + "\", unable to convert to number");
                                    }
                                }
                                // 兼容类型处理
                                return ObjectUtils.toTypeNumber(result, parameterizedType.getActualType());
                            } catch (Exception exception) {
                                // 是否支持空字符串？
                                throw exception;
                            }
                        }
                        case 'n': {
                            parseNull(buf, fromIndex, parseContext);
                            if (parameterizedType.getActualType().isPrimitive()) {
                                return ZERO;
                            }
                            return null;
                        }
                    }
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", unexpected character '" + beginChar + "' when try parsing a number");
                }
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext parseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            switch (beginByte) {
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                    return deserializeNumber(beginByte & 0xF, false, 1, buf, fromIndex, ++fromIndex, parameterizedType, endToken, parseContext);
                case '0':
                case '+':
                    return deserializeNumber(0, false, 0, buf, fromIndex, ++fromIndex, parameterizedType, endToken, parseContext);
                case '-':
                    return deserializeNumber(0, true, 0, buf, fromIndex, ++fromIndex, parameterizedType, endToken, parseContext);
                case '"': {
                    try {
                        // 兼容字符串转化,存在嵌套\"\"问题
                        byte b = buf[++fromIndex];
                        if (b == DOUBLE_QUOTATION) {
                            throw new JSONException("Syntax error, at pos " + fromIndex + ", unexpected character '\"' when parsing a number");
                        }
                        Number result = (Number) deserialize(charSource, buf, fromIndex, parameterizedType, instance, DOUBLE_QUOTATION, parseContext);
                        int endIndex = parseContext.endIndex;
                        int endChar = parseContext.endToken;
                        if (endChar == 0) {
                            if (buf[++endIndex] != '"') {
                                throw new JSONException("Syntax error, for input: \"" + new String(buf, fromIndex, endIndex - fromIndex) + "\", unable to convert to number");
                            }
                            parseContext.endIndex = endIndex;
                        } else {
                            parseContext.endToken = 0;
                            if (endChar != '"') {
                                throw new JSONException("Syntax error, for input: \"" + new String(buf, fromIndex, endIndex - fromIndex) + "\", unable to convert to number");
                            }
                        }
                        return result;
                    } catch (Exception exception) {
                        // 是否支持空字符串？
                        throw exception;
                    }
                }
                case 'n': {
                    parseNull(buf, fromIndex, parseContext);
                    if (parameterizedType.getActualType().isPrimitive()) {
                        return ZERO;
                    }
                    return null;
                }
            }
            throw new JSONException("Syntax error, at pos " + fromIndex + ", unexpected character '" + (char) beginByte + "' when try parsing a number");
        }

        final static Number parseNumber(char[] buf, int fromIndex, int offset, long val, int cnt, boolean negative, char endToken, int returnType, JSONParseContext parseContext) throws Exception {
            long value = val;
            int decimalCount = 0;
            int expValue = 0;
            boolean expNegative = false;
            // init integer type
            int mode = 0;
            // number suffix
            int specifySuffix = 0;

            int i = offset;
            char ch;
            do {
                while (NumberUtils.isDigit((ch = buf[i]))) {
                    value = (value << 3) + (value << 1) + (ch & 0xf);
                    ++cnt;
                    ++i;
                }
                if (ch == '.') {
                    // 小数点模式
                    mode = 1;
                    // direct scan numbers
                    while (NumberUtils.isDigit((ch = buf[++i]))) {
                        value = (value << 3) + (value << 1) + (ch & 0xf);
                        ++decimalCount;
                        ++cnt;
                    }
                }
                if (ch <= ' ') {
                    while ((ch = buf[++i]) <= ' ') ;
                }
                if (ch == ',' || ch == endToken) {
                    break;
                }
                if (ch == 'E' || ch == 'e') {
                    // 科学计数法(浮点模式)
                    mode = 2;
                    ch = buf[++i];
                    if ((expNegative = ch == '-') || ch == '+') {
                        ch = buf[++i];
                    }
                    if (NumberUtils.isDigit(ch)) {
                        expValue = (ch & 0xf);
                        while (NumberUtils.isDigit(ch = buf[++i])) {
                            expValue = (expValue << 3) + (expValue << 1) + (ch & 0xf);
                        }
                    }
                    if (ch <= ' ') {
                        while ((ch = buf[++i]) <= ' ') ;
                    }
                    if (ch == ',' || ch == endToken) {
                        break;
                    }
                }
                switch (ch) {
                    case 'l':
                    case 'L': {
                        specifySuffix = 1;
                        while ((ch = buf[++i]) <= ' ') ;
                        if (ch == ',' || ch == endToken) {
                            break;
                        }
                        String contextErrorAt = createErrorContextText(buf, i);
                        throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + ch + "', context text by '" + contextErrorAt + "'");
                    }
                    case 'f':
                    case 'F': {
                        specifySuffix = 2;
                        while ((ch = buf[++i]) <= ' ') ;
                        if (ch == ',' || ch == endToken) {
                            break;
                        }
                        String contextErrorAt = createErrorContextText(buf, i);
                        throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + ch + "', context text by '" + contextErrorAt + "'");
                    }
                    case 'd':
                    case 'D': {
                        specifySuffix = 3;
                        while ((ch = buf[++i]) <= ' ') ;
                        if (ch == ',' || ch == endToken) {
                            break;
                        }
                        String contextErrorAt = createErrorContextText(buf, i);
                        throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + ch + "', context text by '" + contextErrorAt + "'");
                    }
                    default: {
                        String contextErrorAt = createErrorContextText(buf, i);
                        throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + ch + "', context text by '" + contextErrorAt + "'");
                    }
                }
                break;
            } while (false);

            // end
            int endIndex = i - 1;
            parseContext.endIndex = endIndex;
            if (returnType == TYPE_DOUBLE) {
                if (cnt > 19) {
                    // Compatible with double in abnormal length
                    // Get the top 18 significant digits
                    value = 0;
                    cnt = 0;
                    int j = fromIndex, decimalPointIndex = endIndex;
                    decimalCount = 0;
                    for (; j < i; ++j) {
                        if (NumberUtils.isDigit(ch = buf[j])) {
                            if (cnt++ < 18) {
                                value = value * 10 + (ch & 0xf);
                            }
                            if (j > decimalPointIndex) {
                                ++decimalCount;
                            }
                        } else {
                            if (ch == '.') {
                                decimalPointIndex = j;
                            } else if (ch == 'e' || ch == 'E') {
                                break;
                            }
                        }
                        if (cnt >= 18 && decimalCount > 0) break;
                    }
                    decimalCount -= cnt - 18;
                }
                double dv = NumberUtils.scientificToIEEEDouble(value, expNegative ? expValue + decimalCount : decimalCount - expValue);
                return negative ? -dv : dv;
            } else {
                switch (returnType) {
                    case TYPE_BIGDECIMAL: {
                        if (cnt > 19) {
                            // BigDecimal by String input
                            while (buf[endIndex] <= ' ') {
                                --endIndex;
                            }
                            return new BigDecimal(buf, fromIndex, endIndex - fromIndex + 1);
                        }
                        value = negative ? -value : value;
                        return new BigDecimal(BigInteger.valueOf(value), expNegative ? expValue + decimalCount : decimalCount - expValue);
                    }
                    case TYPE_BIGINTEGER: {
                        if (cnt > 19) {
                            // BigInteger by String input
                            while (buf[endIndex] <= ' ') {
                                --endIndex;
                            }
                            return new BigInteger(new String(buf, fromIndex, endIndex - fromIndex + 1));
                        }
                        value = negative ? -value : value;
                        return BigInteger.valueOf(value);
                    }
                    case TYPE_FLOAT: {
                        float fv = NumberUtils.scientificToIEEEFloat(value, expNegative ? expValue + decimalCount : decimalCount - expValue);
                        return negative ? -fv : fv;
                    }
                }
            }

            // int / long / BigInteger
            if (mode == 0) {
                if (cnt > 19) {
                    // BigInteger
                    while (buf[endIndex] <= ' ') {
                        --endIndex;
                    }
                    return new BigInteger(new String(buf, fromIndex, endIndex - fromIndex + 1));
                }
                value = negative ? -value : value;
                if (specifySuffix > 0) {
                    switch (specifySuffix) {
                        case 1:
                            return value;
                        case 2:
                            return (float) value;
                    }
                    return value;
                }
                if (value <= Integer.MAX_VALUE && value > Integer.MIN_VALUE) {
                    return (int) value;
                }
                return value;
            } else {
                if (cnt > 19) {
                    // 随便指定一个数字长度25
                    if (value < 0 || cnt > 25) {
                        while (buf[endIndex] <= ' ') {
                            --endIndex;
                        }
                        return new BigDecimal(buf, fromIndex, endIndex - fromIndex + 1);
                    }
                }
                expValue = expNegative ? -expValue - decimalCount : expValue - decimalCount;
                double doubleVal = NumberUtils.scientificToIEEEDouble(value, -expValue);
                doubleVal = negative ? -doubleVal : doubleVal;
                if (specifySuffix > 0) {
                    switch (specifySuffix) {
                        case 1:
                            return (long) doubleVal;
                        case 2:
                            return (float) doubleVal;
                    }
                    return doubleVal;
                }
                return doubleVal;
            }
        }

        final static Number parseNumber(byte[] buf, int fromIndex, int offset, long val, int cnt, boolean negative, byte endToken, int returnType, JSONParseContext parseContext) throws Exception {
            long value = val;
            int decimalCount = 0;
            int expValue = 0;
            boolean expNegative = false;
            // init integer type
            int mode = 0;
            // number suffix
            int specifySuffix = 0;
            int i = offset;
            byte b;
            do {
                int v;
                while ((v = JSONUnsafe.UNSAFE_ENDIAN.digits2Bytes(buf, i)) != -1) {
                    value = value * 100 + v;
                    cnt += 2;
                    i += 2;
                }
                if (NumberUtils.isDigit(b = buf[i])) {
                    value = (value << 3) + (value << 1) + (b & 0xF);
                    b = buf[++i];
                    ++cnt;
                }
                if (b == '.') {
                    // 小数点模式
                    mode = 1;
                    ++i;
                    // direct scan numbers
                    while ((v = JSONUnsafe.UNSAFE_ENDIAN.digits2Bytes(buf, i)) != -1) {
                        value = value * 100 + v;
                        cnt += 2;
                        decimalCount += 2;
                        i += 2;
                    }
                    if (NumberUtils.isDigit(b = buf[i])) {
                        value = (value << 3) + (value << 1) + (b & 0xF);
                        b = buf[++i];
                        ++cnt;
                        ++decimalCount;
                    }
                }
                if (b <= ' ') {
                    while ((b = buf[++i]) <= ' ') ;
                }
                if (b == ',' || b == endToken) {
                    break;
                }
                if (b == 'E' || b == 'e') {
                    // 科学计数法(浮点模式)
                    mode = 2;
                    b = buf[++i];
                    if ((expNegative = b == '-') || b == '+') {
                        b = buf[++i];
                    }
                    if (NumberUtils.isDigit(b)) {
                        expValue = (b & 0xF);
                        while (NumberUtils.isDigit(b = buf[++i])) {
                            expValue = (expValue << 3) + (expValue << 1) + (b & 0xF);
                        }
                    }
                    if (b <= ' ') {
                        while ((b = buf[++i]) <= ' ') ;
                    }
                    if (b == ',' || b == endToken) {
                        break;
                    }
                }
                switch (b) {
                    case 'l':
                    case 'L': {
                        specifySuffix = 1;
                        while ((b = buf[++i]) <= ' ') ;
                        if (b == ',' || b == endToken) {
                            break;
                        }
                        String contextErrorAt = createErrorContextText(buf, i);
                        throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + (char) endToken + "', but found '" + (char) b + "', context text by '" + contextErrorAt + "'");
                    }
                    case 'f':
                    case 'F': {
                        specifySuffix = 2;
                        while ((b = buf[++i]) <= ' ') ;
                        if (b == ',' || b == endToken) {
                            break;
                        }
                        String contextErrorAt = createErrorContextText(buf, i);
                        throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + (char) endToken + "', but found '" + (char) b + "', context text by '" + contextErrorAt + "'");
                    }
                    case 'd':
                    case 'D': {
                        specifySuffix = 3;
                        while ((b = buf[++i]) <= ' ') ;
                        if (b == ',' || b == endToken) {
                            break;
                        }
                        String contextErrorAt = createErrorContextText(buf, i);
                        throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + (char) endToken + "', but found '" + (char) b + "', context text by '" + contextErrorAt + "'");
                    }
                    default: {
                        String contextErrorAt = createErrorContextText(buf, i);
                        throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + (char) endToken + "', but found '" + (char) b + "', context text by '" + contextErrorAt + "'");
                    }
                }
                break;
            } while (false);

            // end
            int endIndex = i - 1;
            parseContext.endIndex = endIndex;
            if (returnType == TYPE_DOUBLE) {
                if (cnt > 19) {
                    // Compatible with double in abnormal length
                    // Get the top 18 significant digits
                    value = 0;
                    cnt = 0;
                    int j = fromIndex, decimalPointIndex = endIndex;
                    decimalCount = 0;
                    for (; j < i; ++j) {
                        if (NumberUtils.isDigit(b = buf[j])) {
                            if (cnt++ < 18) {
                                value = value * 10 + (b & 0xF);
                            }
                            if (j > decimalPointIndex) {
                                ++decimalCount;
                            }
                        } else {
                            if (b == '.') {
                                decimalPointIndex = j;
                            } else if (b == 'e' || b == 'E') {
                                break;
                            }
                        }
                        if (cnt >= 18 && decimalCount > 0) break;
                    }
                    decimalCount -= cnt - 18;
                }
                double dv = NumberUtils.scientificToIEEEDouble(value, expNegative ? expValue + decimalCount : decimalCount - expValue);
                return negative ? -dv : dv;
            } else {
                switch (returnType) {
                    case TYPE_BIGDECIMAL: {
                        if (cnt > 19) {
                            // BigDecimal by String input
                            while (buf[endIndex] <= ' ') {
                                --endIndex;
                            }
                            return new BigDecimal(new String(buf, fromIndex, endIndex - fromIndex + 1));
                        }
                        value = negative ? -value : value;
                        return new BigDecimal(BigInteger.valueOf(value), expNegative ? expValue + decimalCount : decimalCount - expValue);
                    }
                    case TYPE_BIGINTEGER: {
                        if (cnt > 19) {
                            // BigInteger by String input
                            while (buf[endIndex] <= ' ') {
                                --endIndex;
                            }
                            return new BigInteger(new String(buf, fromIndex, endIndex - fromIndex + 1));
                        }
                        value = negative ? -value : value;
                        return BigInteger.valueOf(value);
                    }
                    case TYPE_FLOAT: {
                        float fv = NumberUtils.scientificToIEEEFloat(value, expNegative ? expValue + decimalCount : decimalCount - expValue);
                        return negative ? -fv : fv;
                    }
                }
            }

            // int / long / BigInteger
            if (mode == 0) {
                if (cnt > 19) {
                    // BigInteger
                    while (buf[endIndex] <= ' ') {
                        --endIndex;
                    }
                    return new BigInteger(new String(buf, fromIndex, endIndex - fromIndex + 1));
                }
                value = negative ? -value : value;
                if (specifySuffix > 0) {
                    switch (specifySuffix) {
                        case 1:
                            return value;
                        case 2:
                            return (float) value;
                    }
                    return value;
                }
                if (value <= Integer.MAX_VALUE && value > Integer.MIN_VALUE) {
                    return (int) value;
                }
                return value;
            } else {
                if (cnt > 19) {
                    // 随便指定一个数字长度25
                    if (value < 0 || cnt > 25) {
                        while (buf[endIndex] <= ' ') {
                            --endIndex;
                        }
                        return new BigDecimal(new String(buf, fromIndex, endIndex - fromIndex + 1));
                    }
                }
                expValue = expNegative ? -expValue - decimalCount : expValue - decimalCount;
                double doubleVal = NumberUtils.scientificToIEEEDouble(value, -expValue);
                doubleVal = negative ? -doubleVal : doubleVal;
                if (specifySuffix > 0) {
                    switch (specifySuffix) {
                        case 1:
                            return (long) doubleVal;
                        case 2:
                            return (float) doubleVal;
                    }
                    return doubleVal;
                }
                return doubleVal;
            }
        }

        protected final static long deserializeInteger(long value, boolean negative, int cnt, char[] buf, int fromIndex, char endToken, JSONParseContext parseContext) throws Exception {
            int i = fromIndex;
            char ch, ch1 = 0;
            boolean isDigit;
            while ((isDigit = NumberUtils.isDigit((ch = buf[i]))) && NumberUtils.isDigit(ch1 = buf[++i])) {
                value = value * 100 + twoDigitsValue(ch, ch1);
                ++i;
            }
            if (isDigit) {
                value = (value << 3) + (value << 1) + (ch & 0xf);
                ch = ch1;
            }
            if (ch == COMMA || ch == endToken) {
                parseContext.endIndex = i;
                parseContext.endToken = ch;
                return negative ? -value : value;
            }
            if (ch <= WHITE_SPACE) {
                while ((ch = buf[++i]) <= WHITE_SPACE) ;
                if (ch == COMMA || ch == endToken) {
                    parseContext.endIndex = i;
                    parseContext.endToken = ch;
                    return negative ? -value : value;
                }
                String contextErrorAt = createErrorContextText(buf, i);
                throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + ch + "', context text by '" + contextErrorAt + "'");
            }
            // forward default deserialize
            return parseNumber(buf, fromIndex, i, value, cnt + i - fromIndex, negative, endToken, 0, parseContext).longValue();
        }

        protected final static long deserializeInteger(long value, boolean negative, int cnt, byte[] buf, int fromIndex, byte endToken, JSONParseContext parseContext) throws Exception {
            int i = fromIndex, val;
            byte b;
            while ((val = JSONUnsafe.UNSAFE_ENDIAN.digits2Bytes(buf, i)) != -1) {
                value = value * 100 + val;
                i += 2;
            }
            if (NumberUtils.isDigit(b = buf[i])) {
                value = (value << 3) + (value << 1) + (b & 0xF);
                b = buf[++i];
            }
            if (b == COMMA || b == endToken) {
                parseContext.endIndex = i;
                parseContext.endToken = b;
                return negative ? -value : value;
            }
            if (b <= WHITE_SPACE) {
                while ((b = buf[++i]) <= WHITE_SPACE) ;
                if (b == COMMA || b == endToken) {
                    parseContext.endIndex = i;
                    parseContext.endToken = b;
                    return negative ? -value : value;
                }
                String contextErrorAt = createErrorContextText(buf, i);
                throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + (char) b + "', context text by '" + contextErrorAt + "'");
            }
            // forward default deserialize
            return parseNumber(buf, fromIndex, i, value, cnt + i - fromIndex, negative, endToken, 0, parseContext).longValue();
        }

        protected Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, char endToken, JSONParseContext parseContext) throws Exception {
            if (parameterizedType == GenericParameterizedType.AnyType || parameterizedType == null) {
                return parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, 0, parseContext);
            }
            if (parameterizedType == GenericParameterizedType.BigDecimalType) {
                return parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, TYPE_BIGDECIMAL, parseContext);
            }
            Number value = parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, 0, parseContext);
            return ObjectUtils.toTypeNumber(value, parameterizedType.getActualType());
        }

        protected Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext parseContext) throws Exception {
            if (parameterizedType == GenericParameterizedType.AnyType || parameterizedType == null) {
                return parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, 0, parseContext);
            }
            if (parameterizedType == GenericParameterizedType.BigDecimalType) {
                return parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, TYPE_BIGDECIMAL, parseContext);
            }
            // not supported
            Number value = parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, 0, parseContext);
            return ObjectUtils.toTypeNumber(value, parameterizedType.getActualType());
        }

        final static class LongImpl extends NumberImpl {
            protected Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, char endToken, JSONParseContext parseContext) throws Exception {
                return deserializeInteger(initValue, negative, cnt, buf, offset, endToken, parseContext);
            }

            protected Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext parseContext) throws Exception {
                return deserializeInteger(initValue, negative, cnt, buf, offset, endToken, parseContext);
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return Long.parseLong(value);
            }
        }

        final static class IntegerImpl extends NumberImpl {
            protected Integer deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, char endToken, JSONParseContext parseContext) throws Exception {
                return (int) deserializeInteger(initValue, negative, cnt, buf, offset, endToken, parseContext);
            }

            protected Integer deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext parseContext) throws Exception {
                return (int) deserializeInteger(initValue, negative, cnt, buf, offset, endToken, parseContext);
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return Integer.parseInt(value);
            }
        }

        final static class ShortImpl extends NumberImpl {
            protected Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, char endToken, JSONParseContext parseContext) throws Exception {
                return (short) deserializeInteger(initValue, negative, cnt, buf, offset, endToken, parseContext);
            }

            protected Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext parseContext) throws Exception {
                return (short) deserializeInteger(initValue, negative, cnt, buf, offset, endToken, parseContext);
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return Short.parseShort(value);
            }
        }

        final static class ByteImpl extends NumberImpl {
            protected Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, char endToken, JSONParseContext parseContext) throws Exception {
                return (byte) deserializeInteger(initValue, negative, cnt, buf, offset, endToken, parseContext);
            }

            protected Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext parseContext) throws Exception {
                return (byte) deserializeInteger(initValue, negative, cnt, buf, offset, endToken, parseContext);
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return Byte.parseByte(value);
            }
        }

        final static class DoubleImpl extends NumberImpl {
            protected Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, char endToken, JSONParseContext parseContext) throws Exception {
                return parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, TYPE_DOUBLE, parseContext);
            }

            protected Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext parseContext) throws Exception {
                return parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, TYPE_DOUBLE, parseContext);
            }
        }

        final static class FloatImpl extends NumberImpl {
            protected Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, char endToken, JSONParseContext parseContext) throws Exception {
                return parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, TYPE_FLOAT, parseContext);
            }

            protected Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext parseContext) throws Exception {
                return parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, TYPE_FLOAT, parseContext);
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return Float.parseFloat(value);
            }
        }

        final static class BigDecimalImpl extends NumberImpl {
            protected Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, char endToken, JSONParseContext parseContext) throws Exception {
                return parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, TYPE_BIGDECIMAL, parseContext);
            }

            protected Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext parseContext) throws Exception {
                return parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, TYPE_BIGDECIMAL, parseContext);
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return new BigDecimal(value);
            }
        }

        final static class BigIntegerImpl extends NumberImpl {
            protected Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, char endToken, JSONParseContext parseContext) throws Exception {
                return parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, TYPE_BIGINTEGER, parseContext);
            }

            protected Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext parseContext) throws Exception {
                return parseNumber(buf, fromIndex, offset, initValue, cnt, negative, endToken, TYPE_BIGINTEGER, parseContext);
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return new BigInteger(value);
            }
        }

        final static class Skipper extends NumberImpl {

            protected Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, char endToken, JSONParseContext parseContext) throws Exception {
                int i = offset;
                char ch;
                do {
                    while (NumberUtils.isDigit((ch = buf[i]))) {
                        ++i;
                    }
                    if (ch == '.') {
                        while (NumberUtils.isDigit((ch = buf[++i]))) ;
                    }
                    if (ch <= ' ') {
                        while ((ch = buf[++i]) <= ' ') ;
                    }
                    if (ch == ',' || ch == endToken) {
                        break;
                    }
                    if (ch == 'E' || ch == 'e') {
                        ch = buf[++i];
                        if (ch == '-' || ch == '+') {
                            ch = buf[++i];
                        }
                        if (NumberUtils.isDigit(ch)) {
                            while (NumberUtils.isDigit(ch = buf[++i])) ;
                        }
                        if (ch <= ' ') {
                            while ((ch = buf[++i]) <= ' ') ;
                        }
                        if (ch == ',' || ch == endToken) {
                            break;
                        }
                    }
                    switch (ch) {
                        case 'l':
                        case 'L':
                        case 'f':
                        case 'F':
                        case 'd':
                        case 'D': {
                            while ((ch = buf[++i]) <= ' ') ;
                            if (ch == ',' || ch == endToken) {
                                break;
                            }
                            if (parseContext.validate) {
                                parseContext.validateFail = true;
                                return null;
                            }
                            String contextErrorAt = createErrorContextText(buf, i);
                            throw new JSONException("skip input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + ch + "', context text by '" + contextErrorAt + "'");
                        }
                        default: {
                            if (parseContext.validate) {
                                parseContext.validateFail = true;
                                return null;
                            }
                            String contextErrorAt = createErrorContextText(buf, i);
                            throw new JSONException("skip input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + ch + "', context text by '" + contextErrorAt + "'");
                        }
                    }
                    break;
                } while (false);
                parseContext.endIndex = i - 1;
                return null;
            }

            protected Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext parseContext) throws Exception {
                int i = offset;
                byte b;
                do {
                    while (NumberUtils.isDigit((b = buf[i]))) {
                        ++i;
                    }
                    if (b == '.') {
                        while (NumberUtils.isDigit((b = buf[++i]))) ;
                    }
                    if (b <= ' ') {
                        while ((b = buf[++i]) <= ' ') ;
                    }
                    if (b == ',' || b == endToken) {
                        break;
                    }
                    if (b == 'E' || b == 'e') {
                        b = buf[++i];
                        if (b == '-' || b == '+') {
                            b = buf[++i];
                        }
                        if (NumberUtils.isDigit(b)) {
                            while (NumberUtils.isDigit(b = buf[++i])) ;
                        }
                        if (b <= ' ') {
                            while ((b = buf[++i]) <= ' ') ;
                        }
                        if (b == ',' || b == endToken) {
                            break;
                        }
                    }
                    switch (b) {
                        case 'l':
                        case 'L':
                        case 'f':
                        case 'F':
                        case 'd':
                        case 'D': {
                            while ((b = buf[++i]) <= ' ') ;
                            if (b == ',' || b == endToken) {
                                break;
                            }
                            if (parseContext.validate) {
                                parseContext.validateFail = true;
                                return null;
                            }
                            String contextErrorAt = createErrorContextText(buf, i);
                            throw new JSONException("skip input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + b + "', context text by '" + contextErrorAt + "'");
                        }
                        default: {
                            if (parseContext.validate) {
                                parseContext.validateFail = true;
                                return null;
                            }
                            String contextErrorAt = createErrorContextText(buf, i);
                            throw new JSONException("skip input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + b + "', context text by '" + contextErrorAt + "'");
                        }
                    }
                    break;
                } while (false);
                parseContext.endIndex = i - 1;
                return null;
            }
        }
    }

    // 2、boolean
    final static class BooleanImpl extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return value.equals("true");
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext parseContext) throws Exception {
            char beginChar = buf[fromIndex];
            if (beginChar == 't') {
                return parseTrue(buf, fromIndex, parseContext);
            } else if (beginChar == 'f') {
                return parseFalse(buf, fromIndex, parseContext);
            } else {
                if (beginChar == 'n' && parameterizedType.getActualType() == Boolean.class) {
                    return parseNull(buf, fromIndex, parseContext);
                }
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for boolean Type, expected 't' or 'f'");
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext parseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            if (beginByte == 't') {
                return parseTrue(buf, fromIndex, parseContext);
            } else if (beginByte == 'f') {
                return parseFalse(buf, fromIndex, parseContext);
            } else {
                if (beginByte == 'n' && parameterizedType.getActualType() == Boolean.class) {
                    return parseNull(buf, fromIndex, parseContext);
                }
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) beginByte + "' for Boolean Type, expected 't' or 'f'");
            }
        }
    }

    // 3、日期
    static class DateImpl extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return matchDate(getChars(value), 0, value.length(), null, (Class<? extends Date>) actualType);
        }

        /**
         * Default模式将自动匹配日期(char[])
         *
         * @param buf
         * @param from
         * @param to
         * @param dateCls
         * @return
         */
        protected Object deserializeDate(char[] buf, int from, int to, Class<? extends Date> dateCls) {
            return matchDate(buf, from + 1, to - 1, null, dateCls);
        }

        /**
         * Default模式将自动匹配日期(byte[])
         *
         * @param buf
         * @param from
         * @param to
         * @param dateCls
         * @return
         */
        protected Object deserializeDate(byte[] buf, int from, int to, Class<? extends Date> dateCls) {
            return matchDate(buf, from + 1, to - 1, null, dateCls);
        }

        // 通过字符数组解析
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext parseContext) throws Exception {
            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '\'':
                case '"': {
                    CHAR_SEQUENCE.skip(charSource, buf, fromIndex, beginChar, parseContext);
                    int endStringIndex = parseContext.endIndex;
                    Class<? extends Date> dateCls = (Class<? extends Date>) parameterizedType.getActualType();
                    return deserializeDate(buf, fromIndex, endStringIndex + 1, dateCls);
                }
                case 'n': {
                    return parseNull(buf, fromIndex, parseContext);
                }
                case '{': {
                    return OBJECT.deserializeObject(charSource, buf, fromIndex, parameterizedType, instance, parseContext);
                }
                default: {
                    // long
                    long timestamp = (Long) NUMBER_LONG.deserialize(charSource, buf, fromIndex, GenericParameterizedType.LongType, null, endToken, parseContext);
                    return parseDate(timestamp, (Class<? extends Date>) parameterizedType.getActualType());
                }
            }
        }

        // 通过字节数组解析
        protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext parseContext) throws Exception {
            byte beginByte = bytes[fromIndex];
            switch (beginByte) {
                case '\'':
                case '"': {
                    CHAR_SEQUENCE.skip(charSource, bytes, fromIndex, beginByte, parseContext);
                    int endStringIndex = parseContext.endIndex;
                    Class<? extends Date> dateCls = (Class<? extends Date>) parameterizedType.getActualType();
                    return deserializeDate(bytes, fromIndex, endStringIndex + 1, dateCls);
                }
                case 'n': {
                    return parseNull(bytes, fromIndex, parseContext);
                }
                case '{': {
                    return OBJECT.deserializeObject(charSource, bytes, fromIndex, parameterizedType, instance, parseContext);
                }
                default: {
                    // long
                    long timestamp = (Long) NUMBER_LONG.deserialize(charSource, bytes, fromIndex, GenericParameterizedType.LongType, null, endToken, parseContext);
                    return parseDate(timestamp, (Class<? extends Date>) parameterizedType.getActualType());
                }
            }
        }

        final static class DateInstanceImpl extends DateImpl {

            String pattern;
            int patternType;
            DateTemplate dateTemplate;
            String timezone;

            public DateInstanceImpl(GenericParameterizedType genericParameterizedType, JsonProperty property) {
                genericParameterizedType.getClass();
                String timezoneAt = property.timezone().trim();
                if (timezoneAt.length() > 0) {
                    timezone = timezoneAt;
                }
                String patternAt = property.pattern().trim();
                if (patternAt.length() > 0) {
                    pattern = patternAt;
                    patternType = getPatternType(pattern);
                }

                if (patternType == 4) {
                    dateTemplate = new DateTemplate(pattern);
                }
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return parseDateValueOfString(getChars(value), -1, value.length() + 1, pattern, patternType, dateTemplate, timezone, (Class<? extends Date>) actualType);
            }

            protected Object deserializeDate(char[] buf, int from, int to, Class<? extends Date> dateCls) {
                return parseDateValueOfString(buf, from, to, pattern, patternType, dateTemplate, timezone, dateCls);
            }

            protected Object deserializeDate(byte[] buf, int from, int to, Class<? extends Date> dateCls) {
                return parseDateValueOfString(buf, from, to, pattern, patternType, dateTemplate, timezone, dateCls);
            }
        }
    }

    // 4、Enum
    static class EnumImpl extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            Class enumCls = actualType;
            return Enum.valueOf(enumCls, value);
        }

        protected Enum deserializeEnumName(CharSource charSource, char[] buf, int fromIndex, Class enumCls, JSONParseContext parseContext) throws Exception {
            String name = (String) CHAR_SEQUENCE.deserializeString(charSource, buf, fromIndex, '"', GenericParameterizedType.StringType, parseContext);
            try {
                return Enum.valueOf(enumCls, name);
            } catch (RuntimeException exception) {
                if (parseContext.unknownEnumAsNull) {
                    return null;
                } else {
                    throw exception;
                }
            }
        }

        protected Enum deserializeEnumName(CharSource charSource, byte[] buf, int fromIndex, Class enumCls, JSONParseContext parseContext) throws Exception {
            String name = (String) CHAR_SEQUENCE.deserializeString(charSource, buf, fromIndex, '"', GenericParameterizedType.StringType, parseContext);
            try {
                return Enum.valueOf(enumCls, name);
            } catch (RuntimeException exception) {
                if (parseContext.unknownEnumAsNull) {
                    return null;
                } else {
                    throw exception;
                }
            }
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext parseContext) throws Exception {
            char beginChar = buf[fromIndex];
            Class clazz = parameterizedType.getActualType();

            if (beginChar == '"') {
                return deserializeEnumName(charSource, buf, fromIndex, clazz, parseContext);
            } else if (beginChar == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            } else {
                // number
                Integer ordinal = (Integer) NUMBER_INTEGER.deserialize(charSource, buf, fromIndex, GenericParameterizedType.IntType, null, endToken, parseContext);
                Enum[] values = (Enum[]) getEnumConstants(clazz);
                if (values != null && ordinal < values.length)
                    return values[ordinal];
                throw new JSONException("Syntax error, at pos " + fromIndex + ", ordinal " + ordinal + " cannot convert to Enum " + clazz + "");
            }
        }

        protected Object getEnumConstants(Class clazz) {
            return clazz.getEnumConstants();
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext parseContext) throws Exception {
            byte b = buf[fromIndex];
            Class clazz = parameterizedType.getActualType();

            if (b == '"') {
                return deserializeEnumName(charSource, buf, fromIndex, clazz, parseContext);
            } else if (b == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            } else {
                // number
                Integer ordinal = (Integer) NUMBER_INTEGER.deserialize(charSource, buf, fromIndex, GenericParameterizedType.IntType, null, endToken, parseContext);
                Enum[] values = (Enum[]) clazz.getEnumConstants();
                if (values != null && ordinal < values.length)
                    return values[ordinal];
                throw new JSONException("Syntax error, at pos " + fromIndex + ", ordinal " + ordinal + " cannot convert to Enum " + clazz + "");
            }
        }

        final static class EnumInstanceImpl extends EnumImpl {

            private final Enum[] values;
            protected final JSONValueMatcher<Enum> enumValueMatcher;

            public EnumInstanceImpl(Enum[] values, JSONValueMatcher<Enum> enumValueMatcher) {
                this.values = values;
                this.enumValueMatcher = enumValueMatcher;
            }

            @Override
            protected Object getEnumConstants(Class clazz) {
                return values;
            }

            @Override
            protected Enum deserializeEnumName(CharSource charSource, char[] buf, int fromIndex, Class enumCls, JSONParseContext parseContext) throws Exception {
                int begin = fromIndex + 1, i = begin;
                Enum value = enumValueMatcher.matchValue(charSource, buf, i, '"', parseContext);
                i = parseContext.endIndex;
                if (value == null) {
                    if (buf[i - 1] == '\\') {
                        // skip
                        char ch, prev = 0;
                        while (((ch = buf[++i]) != '"' || prev == '\\')) {
                            prev = ch;
                        }
                    }
                    parseContext.endIndex = i;
                    if (parseContext.unknownEnumAsNull) {
                        return null;
                    }
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unknown Enum name '" + new String(buf, fromIndex + 1, i - fromIndex - 1) + "' of EnumType " + enumCls);
                }
                parseContext.endIndex = i;
                return value;
            }

            @Override
            protected Enum deserializeEnumName(CharSource charSource, byte[] buf, int fromIndex, Class enumCls, JSONParseContext parseContext) throws Exception {
                int begin = fromIndex + 1, i = begin;
                Enum value = enumValueMatcher.matchValue(charSource, buf, i, '"', parseContext);
                i = parseContext.endIndex;
                if (value == null) {
                    if (buf[i - 1] == ESCAPE) {
                        // skip
                        byte b, prev = 0;
                        while (((b = buf[++i]) != DOUBLE_QUOTATION || prev == ESCAPE)) {
                            prev = b;
                        }
                    }
                    parseContext.endIndex = i;
                    if (parseContext.unknownEnumAsNull) {
                        return null;
                    }
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unknown Enum name '" + new String(buf, fromIndex + 1, i - fromIndex - 1) + "' of EnumType " + enumCls);
                }
                parseContext.endIndex = i;
                return value;
            }
        }
    }

    // 4、Class
    final static class ClassImpl extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) throws ClassNotFoundException {
            return Class.forName(value);
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext parseContext) throws Exception {
            char beginChar = buf[fromIndex];
            if (beginChar == '"') {
                String name = (String) CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, beginChar, GenericParameterizedType.StringType, parseContext);
                return Class.forName(name);
            }
            if (beginChar == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            }
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for Class Type, expected '\"' ");
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext parseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            if (beginByte == '"') {
                String name = (String) CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, beginByte, GenericParameterizedType.StringType, parseContext);
                return Class.forName(name);
            }
            if (beginByte == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            }
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) beginByte + "' for Class Type, expected '\"' ");
        }
    }

    // 6、Annotation
    final static class AnnotationImpl extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return null;
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext parseContext) throws Exception {
            // not support
            ANY.skip(charSource, buf, fromIndex, endToken, parseContext);
            return null;
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext parseContext) throws Exception {
            // not support
            ANY.skip(charSource, buf, fromIndex, endToken, parseContext);
            return null;
        }
    }

    // 7、byte[]
    final static class BinaryImpl extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return value.getBytes();
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext parseContext) throws Exception {

            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case 'n': {
                    return parseNull(buf, fromIndex, parseContext);
                }
                case '"': {
                    // String
                    CHAR_SEQUENCE.skip(charSource, buf, fromIndex, beginChar, parseContext);
                    int endStringIndex = parseContext.endIndex;
                    byte[] bytes = parseBytesOfBuf0(fromIndex, endStringIndex - fromIndex - 1, buf, parseContext);
                    return bytes;
                }
                case '[': {
                    return ARRAY.deserialize(charSource, buf, fromIndex, GenericParameterizedType.arrayType(byte.class), null, '\0', parseContext);
                }
                default: {
                    // not support
                    throw new JSONException("Syntax error, from pos " + fromIndex + ", the beginChar '" + beginChar + "' mismatch type byte[] or Byte[] ");
                }
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext parseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            char beginChar = (char) beginByte;
            switch (beginChar) {
                case 'n': {
                    return parseNull(buf, fromIndex, parseContext);
                }
                case '\'':
                case '"': {
                    // String
                    CHAR_SEQUENCE.skip(charSource, buf, fromIndex, beginByte, parseContext);
                    int endStringIndex = parseContext.endIndex;
                    byte[] bytes = parseBytesOfBuf0(fromIndex, endStringIndex - fromIndex - 1, buf, parseContext);
                    return bytes;
                }
                case '[': {
                    return ARRAY.deserialize(charSource, buf, fromIndex, GenericParameterizedType.arrayType(byte.class), null, endToken, parseContext);
                }
                default: {
                    // not support
                    throw new JSONException("Syntax error, from pos " + fromIndex + ", the beginChar '" + beginChar + "' mismatch type byte[] or Byte[] ");
                }
            }
        }

        private static byte[] parseBytesOfBuf0(int fromIndex, int len, char[] buf, JSONParseContext parseContext) {
            if (parseContext.byteArrayFromHexString) {
                return hexString2Bytes(buf, fromIndex + 1, len);
            } else {
                return Base64Utils.decode(buf, fromIndex + 1, len);
            }
        }

        private static byte[] parseBytesOfBuf0(int fromIndex, int len, byte[] buf, JSONParseContext parseContext) {
            if (parseContext.byteArrayFromHexString) {
                return hexString2Bytes(buf, fromIndex + 1, len);
            } else {
                return Base64Utils.decode(buf, fromIndex + 1, len);
            }
        }
    }

    // 8、数组
    static class ArrayImpl extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return null;
        }

        Object deserializeArray(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext parseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            char ch;

            Class arrayCls = parameterizedType.getActualType();
            GenericParameterizedType valueType = parameterizedType.getValueType();
            JSONTypeDeserializer valueDeserializer;
            Class<?> elementCls;
            if (valueType == null) {
                elementCls = arrayCls.getComponentType();
                valueType = GenericParameterizedType.actualType(elementCls);
                valueDeserializer = getTypeDeserializer(elementCls);
            } else {
                elementCls = valueType.getActualType();
                valueDeserializer = getTypeDeserializer(elementCls);
            }

            ArrayList<Object> collection = new ArrayList<Object>(5);
            boolean allowComment = parseContext.allowComment;
            for (int i = beginIndex; ; ++i) {
                // clear white space characters
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ']') {
                    if (collection.size() > 0 && !parseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    parseContext.endIndex = i;
                    return CollectionUtils.toArray(collection, elementCls);
                }

                Object value = valueDeserializer.deserialize(charSource, buf, i, valueType, null, ']', parseContext);
                collection.add(value);
                i = parseContext.endIndex;
                int endChar = parseContext.endToken;
                if (endChar == 0) {
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                } else {
                    ch = (char) endChar;
                    parseContext.endToken = 0;
                }
                if (ch == ',') {
                    continue;
                }
                if (ch == ']') {
                    parseContext.endIndex = i;
                    return CollectionUtils.toArray(collection, elementCls);
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or ']'");
            }
        }

        Object deserializeArray(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext parseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            byte b;
            Class arrayCls = parameterizedType.getActualType();
            GenericParameterizedType valueType = parameterizedType.getValueType();

            Class<?> elementCls;
            JSONTypeDeserializer valueDeserializer;
            if (valueType == null) {
                elementCls = arrayCls.getComponentType();
                valueType = GenericParameterizedType.actualType(elementCls);
                valueDeserializer = getTypeDeserializer(elementCls);
            } else {
                elementCls = valueType.getActualType();
                valueDeserializer = getTypeDeserializer(elementCls);
            }
            ArrayList<Object> collection = new ArrayList<Object>(5);
            boolean allowComment = parseContext.allowComment;
            for (int i = beginIndex; ; ++i) {
                // clear white space characters
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (b == ']') {
                    if (collection.size() > 0 && !parseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    parseContext.endIndex = i;
                    return CollectionUtils.toArray(collection, elementCls);
                }

                Object value = valueDeserializer.deserialize(charSource, buf, i, valueType, null, END_ARRAY, parseContext);
                collection.add(value);
                i = parseContext.endIndex;
                int endChar = parseContext.endToken;
                if (endChar == 0) {
                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                } else {
                    b = (byte) endChar;
                    parseContext.endToken = 0;
                }
                if (b == ',') {
                    continue;
                }
                if (b == ']') {
                    parseContext.endIndex = i;
                    return CollectionUtils.toArray(collection, elementCls);
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or ']'");
            }
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext parseContext) throws Exception {
            char beginChar = buf[fromIndex];
            if (beginChar == '[') {
                return deserializeArray(charSource, buf, fromIndex, parameterizedType, instance, parseContext);
            } else if (beginChar == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            } else {
                if (parseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                    parseContext.endIndex = fromIndex + 1;
                    return null;
                }
                // not support or custom handle ?
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for collection type ");
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext parseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            if (beginByte == '[') {
                return deserializeArray(charSource, buf, fromIndex, parameterizedType, instance, parseContext);
            } else if (beginByte == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            } else {
                char beginChar = (char) beginByte;
                if (parseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                    parseContext.endIndex = fromIndex + 1;
                    return null;
                }
                // not support or custom handle ?
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for Collection Type ");
            }
        }

        static abstract class ArrayInstanceImpl extends ArrayImpl {

            public Object empty() {
                throw new UnsupportedOperationException();
            }

            public Object initArray(JSONParseContext parseContext) {
                throw new UnsupportedOperationException();
            }

            public int size(Object arr) {
                throw new UnsupportedOperationException();
            }

            public void setElementAt(Object arr, Object element, int index) {
                throw new UnsupportedOperationException();
            }

            public Object copyOf(Object value, int len) {
                throw new UnsupportedOperationException();
            }

            public Object subOf(Object value, int len) {
                return copyOf(value, len);
            }

            public JSONTypeDeserializer getValueDeserializer() {
                throw new UnsupportedOperationException();
            }

            @Override
            Object deserializeArray(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext parseContext) throws Exception {
                int beginIndex = fromIndex + 1;
                char ch;
                Object arr = initArray(parseContext);
                int size = size(arr);
                int len = 0;
                boolean allowComment = parseContext.allowComment;
                for (int i = beginIndex; ; ++i) {
                    // clear white space characters
                    while ((ch = buf[i]) <= ' ') {
                        ++i;
                    }
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    if (ch == ']') {
                        if (len > 0 && !parseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                        }
                        parseContext.endIndex = i;
                        return len == 0 ? empty() : subOf(arr, len);
                    }

                    Object value = getValueDeserializer().deserialize(charSource, buf, i, null, null, ']', parseContext);
                    if (len >= size) {
                        arr = copyOf(arr, size = (size << 1));
                    }
                    setElementAt(arr, value, len++);
                    i = parseContext.endIndex;
                    int endChar = parseContext.endToken;
                    if (endChar == 0) {
                        while ((ch = buf[++i]) <= ' ') ;
                        if (allowComment) {
                            if (ch == '/') {
                                ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                            }
                        }
                    } else {
                        ch = (char) endChar;
                        parseContext.endToken = 0;
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == ']') {
                        parseContext.endIndex = i;
                        return subOf(arr, len);
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or ']'");
                }
            }

            @Override
            Object deserializeArray(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext parseContext) throws Exception {
                int beginIndex = fromIndex + 1;
                byte b;
                Object arr = initArray(parseContext);
                int size = size(arr);
                int len = 0;
                boolean allowComment = parseContext.allowComment;
                for (int i = beginIndex; ; ++i) {
                    // clear white space characters
                    while ((b = buf[i]) <= ' ') {
                        ++i;
                    }
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    if (b == ']') {
                        if (len > 0 && !parseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                        }
                        parseContext.endIndex = i;
                        return len == 0 ? empty() : subOf(arr, len);
                    }

                    Object value = getValueDeserializer().deserialize(charSource, buf, i, null, null, END_ARRAY, parseContext);
                    if (len >= size) {
                        arr = copyOf(arr, size = (size << 1));
                    }
                    setElementAt(arr, value, len++);

                    i = parseContext.endIndex;
                    int endChar = parseContext.endToken;
                    if (endChar == 0) {
                        while ((b = buf[++i]) <= WHITE_SPACE) ;
                        if (allowComment) {
                            if (b == '/') {
                                b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                            }
                        }
                    } else {
                        b = (byte) endChar;
                        parseContext.endToken = 0;
                    }
                    if (b == ',') {
                        continue;
                    }
                    if (b == END_ARRAY) {
                        parseContext.endIndex = i;
                        return subOf(arr, len);
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or ']'");
                }
            }
        }

        final static class StringArrayImpl extends ArrayInstanceImpl {

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return new String[]{value};
            }

            @Override
            Object deserializeArray(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext parseContext) throws Exception {
                int beginIndex = fromIndex + 1;
                char ch;
                String[] arr = parseContext.getContextStrings();
                int size = arr.length;
                int len = 0;
                boolean allowComment = parseContext.allowComment;
                for (int i = beginIndex; ; ++i) {
                    while ((ch = buf[i]) <= ' ') {
                        ++i;
                    }
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    if (ch == ']') {
                        if (len > 0 && !parseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                        }
                        parseContext.endIndex = i;
                        return len == 0 ? EMPTY_STRINGS : JSONUnsafe.copyStrings(arr, 0, len);
                    }

                    String value = (String) CHAR_SEQUENCE_STRING.deserialize(charSource, buf, i, null, null, ']', parseContext);
                    if (len >= size) {
                        arr = Arrays.copyOf(arr, size = (size << 1));
                    }
                    arr[len++] = value;
                    i = parseContext.endIndex;
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == ']') {
                        parseContext.endIndex = i;
                        return JSONUnsafe.copyStrings(arr, 0, len);
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or ']'");
                }
            }

            @Override
            Object deserializeArray(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext parseContext) throws Exception {
                int beginIndex = fromIndex + 1;
                byte b;
                String[] arr = parseContext.getContextStrings();
                int size = arr.length;
                int len = 0;
                boolean allowComment = parseContext.allowComment;
                for (int i = beginIndex; ; ++i) {
                    // clear white space characters
                    while ((b = buf[i]) <= ' ') {
                        ++i;
                    }
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    if (b == ']') {
                        if (len > 0 && !parseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                        }
                        parseContext.endIndex = i;
                        return len == 0 ? EMPTY_STRINGS : JSONUnsafe.copyStrings(arr, 0, len);
                    }

                    String value = (String) CHAR_SEQUENCE_STRING.deserialize(charSource, buf, i, null, null, END_ARRAY, parseContext);
                    if (len >= size) {
                        arr = Arrays.copyOf(arr, size = (size << 1));
                    }
                    arr[len++] = value;

                    i = parseContext.endIndex;
                    while ((b = buf[++i]) <= WHITE_SPACE) ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    if (b == ',') {
                        continue;
                    }
                    if (b == END_ARRAY) {
                        parseContext.endIndex = i;
                        return JSONUnsafe.copyStrings(arr, 0, len);
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or ']'");
                }
            }
        }

        final static class DoubleArrayImpl extends ArrayInstanceImpl {
            @Override
            public Object empty() {
                return new Double[0];
            }

            @Override
            public Object initArray(JSONParseContext parseContext) {
                return new Double[10];
            }

            @Override
            public int size(Object arr) {
                return ((Double[]) arr).length;
            }

            @Override
            public void setElementAt(Object arr, Object element, int index) {
                Double[] value = (Double[]) arr;
                value[index] = (Double) element;
            }

            @Override
            public Object copyOf(Object arr, int len) {
                Double[] value = (Double[]) arr;
                return Arrays.copyOf(value, len);
            }

            @Override
            public JSONTypeDeserializer getValueDeserializer() {
                return NUMBER_DOUBLE;
            }
        }

        final static class PrimitiveDoubleArrayImpl extends ArrayInstanceImpl {
            @Override
            public Object empty() {
                return EMPTY_DOUBLES;
            }

            @Override
            public Object initArray(JSONParseContext parseContext) {
                return DOUBLE_ARRAY_TL.get();
            }

            @Override
            public int size(Object arr) {
                return ((double[]) arr).length;
            }

            @Override
            public void setElementAt(Object arr, Object element, int index) {
                double[] value = (double[]) arr;
                value[index] = (Double) element;
            }

            @Override
            public Object copyOf(Object arr, int len) {
                double[] value = (double[]) arr;
                return Arrays.copyOf(value, len);
            }

            @Override
            public Object subOf(Object arr, int len) {
                double[] value = (double[]) arr;
                return JSONUnsafe.copyDoubles(value, 0, len);
            }

            @Override
            public JSONTypeDeserializer getValueDeserializer() {
                return NUMBER_DOUBLE;
            }
        }

        final static class LongArrayImpl extends ArrayInstanceImpl {
            @Override
            public Object empty() {
                return new Long[0];
            }

            @Override
            public Object initArray(JSONParseContext parseContext) {
                return new Long[10];
            }

            @Override
            public int size(Object arr) {
                return ((Long[]) arr).length;
            }

            @Override
            public void setElementAt(Object arr, Object element, int index) {
                Long[] value = (Long[]) arr;
                value[index] = (Long) element;
            }

            @Override
            public Object copyOf(Object arr, int len) {
                Long[] value = (Long[]) arr;
                return Arrays.copyOf(value, len);
            }

            @Override
            public JSONTypeDeserializer getValueDeserializer() {
                return NUMBER_LONG;
            }
        }

        final static class PrimitiveLongArrayImpl extends ArrayInstanceImpl {
            @Override
            public Object empty() {
                return EMPTY_LONGS;
            }

            @Override
            public Object initArray(JSONParseContext parseContext) {
                return LONG_ARRAY_TL.get();
            }

            @Override
            public int size(Object arr) {
                return ((long[]) arr).length;
            }

            @Override
            public void setElementAt(Object arr, Object element, int index) {
                long[] value = (long[]) arr;
                value[index] = (Long) element;
            }

            @Override
            public Object copyOf(Object arr, int len) {
                long[] value = (long[]) arr;
                return Arrays.copyOf(value, len);
            }

            @Override
            public Object subOf(Object arr, int len) {
                long[] value = (long[]) arr;
                return JSONUnsafe.copyLongs(value, 0, len);
            }

            @Override
            public JSONTypeDeserializer getValueDeserializer() {
                return NUMBER_LONG;
            }
        }

        final static class FloatArrayImpl extends ArrayInstanceImpl {
            @Override
            public Object empty() {
                return new Float[0];
            }

            @Override
            public Object initArray(JSONParseContext parseContext) {
                return new Float[10];
            }

            @Override
            public int size(Object arr) {
                return ((Float[]) arr).length;
            }

            @Override
            public void setElementAt(Object arr, Object element, int index) {
                Float[] value = (Float[]) arr;
                value[index] = (Float) element;
            }

            @Override
            public Object copyOf(Object arr, int len) {
                Float[] value = (Float[]) arr;
                return Arrays.copyOf(value, len);
            }

            @Override
            public JSONTypeDeserializer getValueDeserializer() {
                return NUMBER_FLOAT;
            }
        }

        final static class PrimitiveFloatArrayImpl extends ArrayInstanceImpl {
            @Override
            public Object empty() {
                return new float[0];
            }

            @Override
            public Object initArray(JSONParseContext parseContext) {
                return new float[10];
            }

            @Override
            public int size(Object arr) {
                return ((float[]) arr).length;
            }

            @Override
            public void setElementAt(Object arr, Object element, int index) {
                float[] value = (float[]) arr;
                value[index] = (Float) element;
            }

            @Override
            public Object copyOf(Object arr, int len) {
                float[] value = (float[]) arr;
                return Arrays.copyOf(value, len);
            }

            @Override
            public JSONTypeDeserializer getValueDeserializer() {
                return NUMBER_FLOAT;
            }
        }

        final static class IntArrayImpl extends ArrayInstanceImpl {
            @Override
            public Object empty() {
                return new Integer[0];
            }

            @Override
            public Object initArray(JSONParseContext parseContext) {
                return new Integer[10];
            }

            @Override
            public int size(Object arr) {
                return ((Integer[]) arr).length;
            }

            @Override
            public void setElementAt(Object arr, Object element, int index) {
                Integer[] value = (Integer[]) arr;
                value[index] = (Integer) element;
            }

            @Override
            public Object copyOf(Object arr, int len) {
                Integer[] value = (Integer[]) arr;
                return Arrays.copyOf(value, len);
            }

            protected Integer[] deserialize(char[] chars, int offset, JSONParseContext parseContext) {
                try {
                    return (Integer[]) deserializeArray(null, chars, offset, null, null, parseContext);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public JSONTypeDeserializer getValueDeserializer() {
                return NUMBER_INTEGER;
            }
        }

        final static class PrimitiveIntArrayImpl extends ArrayInstanceImpl {
            @Override
            public Object empty() {
                return EMPTY_INTS;
            }

            @Override
            public Object initArray(JSONParseContext parseContext) {
                return INT_ARRAY_TL.get();
            }

            @Override
            public int size(Object arr) {
                return ((int[]) arr).length;
            }

            @Override
            public void setElementAt(Object arr, Object element, int index) {
                int[] value = (int[]) arr;
                value[index] = (Integer) element;
            }

            @Override
            public Object copyOf(Object arr, int len) {
                int[] value = (int[]) arr;
                return Arrays.copyOf(value, len);
            }

            @Override
            public JSONTypeDeserializer getValueDeserializer() {
                return NUMBER_INTEGER;
            }
        }

        final static class ByteArrayImpl extends ArrayInstanceImpl {
            @Override
            public Object empty() {
                return new Byte[0];
            }

            @Override
            public Object initArray(JSONParseContext parseContext) {
                return new Byte[10];
            }

            @Override
            public int size(Object arr) {
                return ((Byte[]) arr).length;
            }

            @Override
            public void setElementAt(Object arr, Object element, int index) {
                Byte[] value = (Byte[]) arr;
                value[index] = (Byte) element;
            }

            @Override
            public Object copyOf(Object arr, int len) {
                Byte[] value = (Byte[]) arr;
                return Arrays.copyOf(value, len);
            }

            @Override
            public JSONTypeDeserializer getValueDeserializer() {
                return NUMBER_BYTE;
            }
        }
    }

    // 9、集合
    static class CollectionImpl extends JSONTypeDeserializer {

        @Override
        boolean validate(CharSource charSource, char[] buf, int fromIndex, int toIndex, char endToken, JSONParseContext parseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            char ch;
            int size = 0;
            boolean allowComment = parseContext.allowComment;
            for (int i = beginIndex; i < toIndex; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ']') {
                    if (size > 0 && !parseContext.allowLastEndComma) {
                        return false;
                    }
                    parseContext.endIndex = i;
                    return true;
                }
                ++size;
                boolean validate = ANY.validate(charSource, buf, i, toIndex, ']', parseContext);
                if (!validate) {
                    return false;
                }
                i = parseContext.endIndex;
                while ((ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ',') {
                    continue;
                }
                if (ch == ']') {
                    parseContext.endIndex = i;
                    parseContext.elementSize = size;
                    return true;
                }
                return false;
            }
            return false;
        }

        @Override
        boolean validate(CharSource charSource, byte[] buf, int fromIndex, int toIndex, byte endToken, JSONParseContext parseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            byte ch;
            int size = 0;
            boolean allowComment = parseContext.allowComment;
            for (int i = beginIndex; i < toIndex; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ']') {
                    if (size > 0 && !parseContext.allowLastEndComma) {
                        return false;
                    }
                    parseContext.endIndex = i;
                    return true;
                }
                ++size;
                boolean validate = ANY.validate(charSource, buf, i, toIndex, (byte) ']', parseContext);
                if (!validate) {
                    return false;
                }
                i = parseContext.endIndex;
                while ((ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ',') {
                    continue;
                }
                if (ch == ']') {
                    parseContext.endIndex = i;
                    parseContext.elementSize = size;
                    return true;
                }
                return false;
            }
            return false;
        }

        void skip(CharSource charSource, char[] buf, int fromIndex, JSONParseContext parseContext) throws Exception {
            int beginIndex = fromIndex + 1, toIndex = parseContext.toIndex;
            char ch;
            int size = 0;
            boolean allowComment = parseContext.allowComment;
            for (int i = beginIndex; i < toIndex; ++i) {
                // clear white space characters
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ']') {
                    if (size > 0 && !parseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    parseContext.endIndex = i;
                    return;
                }
                ++size;
                ANY.skip(charSource, buf, i, ']', parseContext);
                i = parseContext.endIndex;
                while ((ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ',') {
                    continue;
                }
                if (ch == ']') {
                    parseContext.endIndex = i;
                    parseContext.elementSize = size;
                    return;
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or ']'");
            }
            throw new JSONException("Syntax error, cannot find closing symbol ']' matching '['");
        }

        void skip(CharSource charSource, byte[] buf, int fromIndex, JSONParseContext parseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            byte b;
            int size = 0, toIndex = parseContext.toIndex;
            boolean allowComment = parseContext.allowComment;
            for (int i = beginIndex; i < toIndex; ++i) {
                // clear white space characters
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (b == ']') {
                    if (size > 0 && !parseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    parseContext.endIndex = i;
                    return;
                }
                ++size;
                ANY.skip(charSource, buf, i, END_ARRAY, parseContext);
                i = parseContext.endIndex;
                while ((b = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (b == ',') {
                    continue;
                }
                if (b == ']') {
                    parseContext.endIndex = i;
                    parseContext.elementSize = size;
                    return;
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or ']'");
            }
            throw new JSONException("Syntax error, cannot find closing symbol ']' matching '['");
        }

        protected Collection createCollection(GenericParameterizedType parameterizedType) throws Exception {
            Class collectionCls = parameterizedType.getActualType();
            if (collectionCls == null || collectionCls == List.class || collectionCls == ArrayList.class) {
                return new ArrayList(5);
            } else {
                return createCollectionInstance(collectionCls);
            }
        }

        protected JSONTypeDeserializer getValueDeserializer(GenericParameterizedType valueGenType) {
            return JSONTypeDeserializer.getTypeDeserializer(valueGenType.getActualType());
        }

        Collection deserializeCollection(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext parseContext) throws Exception {
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
            JSONTypeDeserializer valueDeserializer = getValueDeserializer(valueGenType);
            final boolean allowComment = parseContext.allowComment;
            int beginIndex = fromIndex + 1;
            char ch = '\0';
            for (int i = beginIndex; /*i < toIndex*/ ; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ']') {
                    if (collection.size() > 0 && !parseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    parseContext.endIndex = i;
                    return collection;
                }

                Object value = valueDeserializer.deserialize(charSource, buf, i, valueGenType, null, ']', parseContext);
                collection.add(value);
                i = parseContext.endIndex;
                int endChar = parseContext.endToken;
                if (endChar == 0) {
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                } else {
                    ch = (char) endChar;
                    parseContext.endToken = 0;
                }
                if (ch == ',') {
                    continue;
                }
                if (ch == ']') {
                    parseContext.endIndex = i;
                    return collection;
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or ']'");
            }
        }

        Collection deserializeCollection(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext parseContext) throws Exception {

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
            JSONTypeDeserializer valueDeserializer = getValueDeserializer(valueGenType);
            final boolean allowComment = parseContext.allowComment;
            int beginIndex = fromIndex + 1;
            byte b = '\0';
            for (int i = beginIndex; /*i < toIndex*/ ; ++i) {
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (b == ']') {
                    if (collection.size() > 0 && !parseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    parseContext.endIndex = i;
                    return collection;
                }
                Object value = valueDeserializer.deserialize(charSource, buf, i, valueGenType, null, END_ARRAY, parseContext);
                collection.add(value);
                i = parseContext.endIndex;
                int endChar = parseContext.endToken;
                if (endChar == 0) {
                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                } else {
                    b = (byte) endChar;
                    parseContext.endToken = 0;
                }
                if (b == ',') {
                    continue;
                }
                if (b == ']') {
                    parseContext.endIndex = i;
                    return collection;
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or ']'");
            }
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext parseContext) throws Exception {
            char beginChar = buf[fromIndex];
            if (beginChar == '[') {
                return deserializeCollection(charSource, buf, fromIndex, parameterizedType, instance, parseContext);
            } else if (beginChar == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            } else {
                if (parseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                    parseContext.endIndex = fromIndex + 1;
                    return null;
                }
                // not support or custom handle ?
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', expected token character '[', but found '" + beginChar + "' for Collection Type ");
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext parseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            if (beginByte == '[') {
                return deserializeCollection(charSource, buf, fromIndex, parameterizedType, instance, parseContext);
            }
            if (beginByte == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            }
            if (parseContext.unMatchedEmptyAsNull && beginByte == DOUBLE_QUOTATION && buf[fromIndex + 1] == DOUBLE_QUOTATION) {
                parseContext.endIndex = fromIndex + 1;
                return null;
            }
            // not support or custom handle ?
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', expected token character '[', but found '" + (char) beginByte + "' for Collection Type ");
        }

        static class CollectionInstanceImpl extends CollectionImpl {

            protected final GenericParameterizedType parameterizedType;
            protected final GenericParameterizedType valueType;
            protected final JSONTypeDeserializer valueDeserializer;
            private final Class<? extends Collection> constructionClass;

            CollectionInstanceImpl(GenericParameterizedType genericParameterizedType) {
                this.parameterizedType = genericParameterizedType;
                this.valueType = genericParameterizedType.getValueType();
                this.valueDeserializer = valueType == null ? ANY : getTypeDeserializer(valueType.getActualType());
                this.constructionClass = (Class<? extends Collection>) genericParameterizedType.getActualType();
            }

            @Override
            protected Collection createCollection(GenericParameterizedType parameterizedType) throws Exception {
                try {
                    Class<?> targetClass = constructionClass;
                    if (targetClass.isInterface() || Modifier.isAbstract(targetClass.getModifiers())) {
                        JSONImplInstCreator implInstCreator = getJSONImplInstCreator(targetClass);
                        if (implInstCreator != null) {
                            return (Collection) implInstCreator.create(parameterizedType);
                        } else {
                            throw new JSONException("create instance error for " + targetClass);
                        }
                    } else {
                        return (Collection) UnsafeHelper.newInstance(targetClass);
                    }
                } catch (Throwable throwable) {
                    throw new JSONException("create instance error for " + parameterizedType.getActualType());
                }
            }

            @Override
            protected JSONTypeDeserializer getValueDeserializer(GenericParameterizedType valueGenType) {
                return valueDeserializer;
            }
        }

        // arraylist实现
        final static class ArrayListImpl extends CollectionInstanceImpl {
            ArrayListImpl(GenericParameterizedType genericParameterizedType) {
                super(genericParameterizedType);
            }

            Collection deserializeCollection(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext parseContext) throws Exception {
                final boolean allowComment = parseContext.allowComment;
                char ch;
                final ArrayList collection = new ArrayList(5);
                int i = fromIndex;
                for (; ; ) {
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    if (ch == ']') {
                        if (collection.size() > 0 && !parseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                        }
                        parseContext.endIndex = i;
                        return collection;
                    }

                    Object value = valueDeserializer.deserialize(charSource, buf, i, valueType, null, ']', parseContext);
                    collection.add(value);
                    i = parseContext.endIndex;
                    int endChar = parseContext.endToken;
                    if (endChar == 0) {
                        while ((ch = buf[++i]) <= ' ') ;
                        // clear white space characters
                        if (allowComment) {
                            if (ch == '/') {
                                ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                            }
                        }
                    } else {
                        ch = (char) endChar;
                        parseContext.endToken = 0;
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == ']') {
                        parseContext.endIndex = i;
                        return collection;
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or ']'");
                }
            }

            Collection deserializeCollection(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext parseContext) throws Exception {
                final boolean allowComment = parseContext.allowComment;
                byte b;
                final ArrayList collection = new ArrayList(5);
                int i = fromIndex;
                for (; ; ) {
                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    if (b == ']') {
                        if (collection.size() > 0 && !parseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                        }
                        parseContext.endIndex = i;
                        return collection;
                    }

                    Object value = valueDeserializer.deserialize(charSource, buf, i, valueType, null, END_ARRAY, parseContext);
                    collection.add(value);
                    i = parseContext.endIndex;
                    int endChar = parseContext.endToken;
                    if (endChar == 0) {
                        while ((b = buf[++i]) <= WHITE_SPACE) ;
                        if (allowComment) {
                            if (b == '/') {
                                b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                            }
                        }
                    } else {
                        b = (byte) endChar;
                        parseContext.endToken = 0;
                    }
                    if (b == COMMA) {
                        continue;
                    }
                    if (b == END_ARRAY) {
                        parseContext.endIndex = i;
                        return collection;
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or ']'");
                }
            }

            @Override
            protected Collection createCollection(GenericParameterizedType parameterizedType) throws Exception {
                return new ArrayList(5);
            }
        }

        // hashset实现
        final static class HashSetImpl extends CollectionInstanceImpl {
            HashSetImpl(GenericParameterizedType genericParameterizedType) {
                super(genericParameterizedType);
            }

            @Override
            protected Collection createCollection(GenericParameterizedType parameterizedType) throws Exception {
                return new HashSet(5);
            }
        }
    }

    // 10、Map
    static class MapImpl extends JSONTypeDeserializer {

        public static MapImpl hashtable() {
            return new MapImpl() {
                @Override
                Map createMap(GenericParameterizedType parameterizedType) {
                    return new Hashtable();
                }
            };
        }

        @Override
        boolean validate(CharSource charSource, char[] buf, int fromIndex, int toIndex, char endToken, JSONParseContext parseContext) throws Exception {
            boolean empty = true;
            char ch;
            final boolean allowComment = parseContext.allowComment;
            for (int i = fromIndex + 1; i < toIndex; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == '"') {
                    CHAR_SEQUENCE.skip(charSource, buf, i, ch, parseContext);
                    i = parseContext.endIndex + 1;
                    empty = false;
                } else {
                    if (ch == '}') {
                        if (!empty && !parseContext.allowLastEndComma) {
                            return false;
                        }
                        parseContext.endIndex = i;
                        return true;
                    }
                    if (ch == '\'') {
                        if (parseContext.allowSingleQuotes) {
                            CHAR_SEQUENCE.skip(charSource, buf, i, ch, parseContext);
                            i = parseContext.endIndex + 1;
                            empty = false;
                        } else {
                            return false;
                        }
                    } else {
                        if (parseContext.allowUnquotedFieldNames) {
                            while (i + 1 < toIndex && buf[++i] != ':') ;
                            empty = false;
                        } else {
                            return false;
                        }
                    }
                }
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ':') {
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext);
                        }
                    }
                    boolean result = ANY.validate(charSource, buf, i, toIndex, '}', parseContext);
                    if (!result) {
                        return false;
                    }
                    i = parseContext.endIndex;
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == '}') {
                        parseContext.endIndex = i;
                        return true;
                    }
                    return false;
                } else {
                    return false;
                }
            }
            return false;
        }

        @Override
        boolean validate(CharSource charSource, byte[] buf, int fromIndex, int toIndex, byte endToken, JSONParseContext parseContext) throws Exception {
            boolean empty = true;
            byte ch;
            final boolean allowComment = parseContext.allowComment;
            for (int i = fromIndex + 1; i < toIndex; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == '"') {
                    CHAR_SEQUENCE.skip(charSource, buf, i, ch, parseContext);
                    i = parseContext.endIndex + 1;
                    empty = false;
                } else {
                    if (ch == '}') {
                        if (!empty && !parseContext.allowLastEndComma) {
                            return false;
                        }
                        parseContext.endIndex = i;
                        return true;
                    }
                    if (ch == '\'') {
                        if (parseContext.allowSingleQuotes) {
                            CHAR_SEQUENCE.skip(charSource, buf, i, ch, parseContext);
                            i = parseContext.endIndex + 1;
                            empty = false;
                        } else {
                            return false;
                        }
                    } else {
                        if (parseContext.allowUnquotedFieldNames) {
                            while (i + 1 < toIndex && buf[++i] != ':') ;
                            empty = false;
                        } else {
                            return false;
                        }
                    }
                }
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ':') {
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext);
                        }
                    }
                    boolean result = ANY.validate(charSource, buf, i, toIndex, (byte) '}', parseContext);
                    if (!result) {
                        return false;
                    }
                    i = parseContext.endIndex;
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == '}') {
                        parseContext.endIndex = i;
                        return true;
                    }
                    return false;
                } else {
                    return false;
                }
            }
            return false;
        }

        void skip(CharSource charSource, char[] buf, int fromIndex, JSONParseContext parseContext) throws Exception {
            boolean empty = true;
            char ch;
            final int toIndex = parseContext.toIndex;
            final boolean allowComment = parseContext.allowComment;
            for (int i = fromIndex + 1; i < toIndex; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == '"') {
                    CHAR_SEQUENCE.skip(charSource, buf, i, '"', parseContext);
                    i = parseContext.endIndex + 1;
                    empty = false;
                } else {
                    if (ch == '}') {
                        if (!empty && !parseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                        }
                        parseContext.endIndex = i;
                        return;
                    }
                    if (ch == '\'') {
                        if (parseContext.allowSingleQuotes) {
                            CHAR_SEQUENCE.skip(charSource, buf, i, '\'', parseContext);
                            i = parseContext.endIndex + 1;
                            empty = false;
                        } else {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (parseContext.allowUnquotedFieldNames) {
                            while (i + 1 < toIndex && buf[++i] != ':') ;
                            empty = false;
                        }
                    }
                }
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ':') {
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext);
                        }
                    }
                    ANY.skip(charSource, buf, i, '}', parseContext);
                    i = parseContext.endIndex;
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == '}') {
                        parseContext.endIndex = i;
                        return;
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or '}'");
                } else {
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', token ':' is expected.");
                }
            }
            throw new JSONException("Syntax error, the closing symbol '}' is not found ");
        }

        void skip(CharSource charSource, byte[] buf, int fromIndex, JSONParseContext parseContext) throws Exception {
            boolean empty = true;
            byte b;
            int toIndex = parseContext.toIndex;
            final boolean allowComment = parseContext.allowComment;
            for (int i = fromIndex + 1; i < toIndex; ++i) {
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (b == '"') {
                    CHAR_SEQUENCE.skip(charSource, buf, i, b, parseContext);
                    i = parseContext.endIndex + 1;
                    empty = false;
                } else {
                    if (b == '}') {
                        if (!empty && !parseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                        }
                        parseContext.endIndex = i;
                        return;
                    }
                    if (b == '\'') {
                        if (parseContext.allowSingleQuotes) {
                            CHAR_SEQUENCE.skip(charSource, buf, i, b, parseContext);
                            i = parseContext.endIndex + 1;
                            empty = false;
                        } else {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (parseContext.allowUnquotedFieldNames) {
                            // :
                            while (i + 1 < toIndex && buf[++i] != ':') ;
                            empty = false;
                        }
                    }
                }
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (b == ':') {
                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext);
                        }
                    }
                    ANY.skip(charSource, buf, i, END_OBJECT, parseContext);
                    i = parseContext.endIndex;
                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    if (b == ',') {
                        continue;
                    }
                    if (b == '}') {
                        parseContext.endIndex = i;
                        return;
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or '}'");
                } else {
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', token ':' is expected.");
                }
            }
            throw new JSONException("Syntax error, the closing symbol '}' is not found ");
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext parseContext) throws Exception {
            char beginChar = buf[fromIndex];
            if (beginChar == '{') {
                return deserializeMap(charSource, buf, fromIndex, parameterizedType, instance, parseContext);
            }
            if (beginChar == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            }
            if (parseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                parseContext.endIndex = fromIndex + 1;
                return null;
            }
            // not support or custom handle ?
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for Map Type, expected '{' ");
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext parseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            char beginChar = (char) beginByte;
            if (beginChar == '{') {
                return deserializeMap(charSource, buf, fromIndex, parameterizedType, instance, parseContext);
            }
            if (beginChar == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            }
            if (parseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                parseContext.endIndex = fromIndex + 1;
                return null;
            }
            // not support or custom handle ?
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for Map Type, expected '{' ");
        }


        protected static Object mapKeyToType(Serializable mapKey, Class<?> keyType) {
            if (mapKey == null || keyType == null || keyType == String.class || keyType == CharSequence.class) {
                return mapKey;
            }
            Object key = ObjectUtils.toType(mapKey, keyType);
            if (key == null) {
                throw new UnsupportedOperationException("not supported type '" + keyType + "' as map key ");
            }
            return key;
        }

        Map createMap(GenericParameterizedType parameterizedType) {
            return createMapInstance(parameterizedType);
        }

        GenericParameterizedType getValueType(GenericParameterizedType parameterizedType) {
            return parameterizedType.getValueType();
        }

        JSONTypeDeserializer getValueDeserializer(GenericParameterizedType parameterizedType) {
            GenericParameterizedType valueType = parameterizedType.getValueType();
            return valueType == null ? ANY : getTypeDeserializer(valueType.getActualType());
        }

        Object deserializeMap(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object obj, JSONParseContext parseContext) throws Exception {
            Map instance;
            if (obj != null) {
                instance = (Map) obj;
            } else {
                instance = createMap(parameterizedType);
            }
            int toIndex = parseContext.toIndex;
            boolean empty = true;
            char ch;
            boolean disableCacheMapKey = parseContext.disableCacheMapKey;
            boolean allowComment = parseContext.allowComment;
            for (int i = fromIndex + 1; ; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }

                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }

                int fieldKeyFrom = i;
                Serializable mapKey;
                Object key;

                if (ch == '"') {
                    mapKey = disableCacheMapKey ? (String) CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, i, '"', GenericParameterizedType.StringType, parseContext) : JSONDefaultParser.parseMapKeyByCache(buf, i, '"', parseContext);
                    i = parseContext.endIndex;
                    empty = false;
                    ++i;
                } else {
                    if (ch == '}') {
                        if (!empty && !parseContext.allowLastEndComma) {
                            throw new JSONException("Syntax error, at pos " + i + ", the closing symbol '}' is not allowed here.");
                        }
                        parseContext.endIndex = i;
                        return instance;
                    }
                    if (ch == '\'') {
                        if (parseContext.allowSingleQuotes) {
                            while (i + 1 < toIndex && (buf[++i] != '\'' || buf[i - 1] == '\\')) ;
                            empty = false;
                            ++i;
                            mapKey = parseKeyOfMap(buf, fieldKeyFrom, i, false);
                        } else {
                            throw new JSONException("Syntax error, at pos " + i + ", the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (parseContext.allowUnquotedFieldNames) {
                            while (i + 1 < toIndex && buf[++i] != ':') ;
                            empty = false;
                            mapKey = parseKeyOfMap(buf, fieldKeyFrom, i, true);
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
                                throw new JSONException("Syntax error, at pos " + j + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected '\"' or use option ReadOption.AllowUnquotedFieldNames ");
                            }
                        }
                    }
                }

                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ':') {
                    Class mapKeyClass = parameterizedType == null ? null : parameterizedType.getMapKeyClass();
                    key = mapKeyToType(mapKey, mapKeyClass);  // parseKeyOfMap(fieldKeyFrom, fieldKeyTo, buf, mapKeyClass, isUnquotedFieldName, parseContext);

                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    JSONTypeDeserializer valueDeserializer = getValueDeserializer(parameterizedType);
                    Object value = valueDeserializer.deserialize(charSource, buf, i, getValueType(parameterizedType), null, '}', parseContext);
                    instance.put(key, value);
                    i = parseContext.endIndex;
                    int endChar = parseContext.endToken;
                    if (endChar == 0) {
                        while ((ch = buf[++i]) <= ' ') ;
                        if (allowComment) {
                            if (ch == '/') {
                                ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                            }
                        }
                    } else {
                        ch = (char) endChar;
                        parseContext.endToken = 0;
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == '}') {
                        parseContext.endIndex = i;
                        return instance;
                    }

                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or '}'");
                } else {
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', token ':' is expected.");
                }
            }
        }

        Object deserializeMap(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object obj, JSONParseContext parseContext) throws Exception {

            Map instance;
            if (obj != null) {
                instance = (Map) obj;
            } else {
                instance = createMapInstance(parameterizedType);
            }
            int toIndex = parseContext.toIndex;
            boolean empty = true;
            byte b;
            boolean disableCacheMapKey = parseContext.disableCacheMapKey;
            boolean allowComment = parseContext.allowComment;
            for (int i = fromIndex + 1; ; ++i) {
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }

                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }

                int fieldKeyFrom = i;
                Serializable mapKey;
                Object key;

                if (b == '"') {
                    mapKey = disableCacheMapKey ? parseMapKey(buf, i, '"', parseContext) : parseMapKeyByCache(buf, i, '"', parseContext);
                    i = parseContext.endIndex;
                    empty = false;
                    ++i;
                } else {
                    if (b == '}') {
                        if (!empty && !parseContext.allowLastEndComma) {
                            throw new JSONException("Syntax error, at pos " + i + ", the closing symbol '}' is not allowed here.");
                        }
                        parseContext.endIndex = i;
                        return instance;
                    }
                    if (b == '\'') {
                        if (parseContext.allowSingleQuotes) {
                            while (i + 1 < toIndex && (buf[++i] != '\'' || buf[i - 1] == '\\')) ;
                            empty = false;
                            ++i;
                            mapKey = parseKeyOfMap(buf, fieldKeyFrom, i, false);
                        } else {
                            throw new JSONException("Syntax error, at pos " + i + ", the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (parseContext.allowUnquotedFieldNames) {
                            while (i + 1 < toIndex && buf[++i] != ':') ;
                            empty = false;
                            mapKey = parseKeyOfMap(buf, fieldKeyFrom, i, true);
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
                                String errorContextTextAt = createErrorContextText(buf, j);
                                throw new JSONException("Syntax error, at pos " + j + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected '\"' or use option ReadOption.AllowUnquotedFieldNames ");
                            }
                        }
                    }
                }

                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (b == ':') {
                    Class mapKeyClass = parameterizedType == null ? null : parameterizedType.getMapKeyClass();
                    key = mapKeyToType(mapKey, mapKeyClass);  // parseKeyOfMap(fieldKeyFrom, fieldKeyTo, buf, mapKeyClass, isUnquotedFieldName, parseContext);

                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }

                    Object value = getValueDeserializer(parameterizedType).deserialize(charSource, buf, i, getValueType(parameterizedType), null, END_OBJECT, parseContext);
                    instance.put(key, value);
                    i = parseContext.endIndex;
                    int endChar = parseContext.endToken;
                    if (endChar == 0) {
                        while ((b = buf[++i]) <= WHITE_SPACE) ;
                        if (allowComment) {
                            if (b == '/') {
                                b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                            }
                        }
                    } else {
                        b = (byte) endChar;
                        parseContext.endToken = 0;
                    }

                    if (b == ',') {
                        continue;
                    }
                    if (b == '}') {
                        parseContext.endIndex = i;
                        return instance;
                    }

                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or '}'");
                } else {
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', token ':' is expected.");
                }
            }
        }

    }

    final static class MapInstanceImpl extends MapImpl {

        final GenericParameterizedType genericParameterizedType;
        final GenericParameterizedType valueParameterizedType;
        JSONTypeDeserializer valueDeserializer;

        GenericParameterizedType getValueType(GenericParameterizedType parameterizedType) {
            return valueParameterizedType;
        }

        @Override
        protected <T> GenericParameterizedType getGenericParameterizedType(Class<T> actualType) {
            return genericParameterizedType;
        }

        protected JSONTypeDeserializer getValueDeserializer(GenericParameterizedType valueGenType) {
            if (valueDeserializer == null) {
                valueDeserializer = valueParameterizedType == null ? ANY : getTypeDeserializer(valueParameterizedType.getActualType());
            }
            return valueDeserializer;
        }

        public MapInstanceImpl(GenericParameterizedType genericParameterizedType) {
            this.genericParameterizedType = genericParameterizedType;
            this.valueParameterizedType = genericParameterizedType.getValueType();
        }
    }

    // 11、对象
    final static class ObjectImpl extends JSONTypeDeserializer {

        Object deserializeObject(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType genericParameterizedType, Object instance, JSONParseContext parseContext) throws Exception {
            Class clazz = genericParameterizedType.getActualType();
            JSONPojoDeserializer pojoDeserializer = (JSONPojoDeserializer) createObjectDeserializer(clazz);
            return pojoDeserializer.deserializePojo(charSource, buf, fromIndex, genericParameterizedType, instance, parseContext);
        }

        Object deserializeObject(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType genericParameterizedType, Object instance, JSONParseContext parseContext) throws Exception {
            Class clazz = genericParameterizedType.getActualType();
            JSONPojoDeserializer pojoDeserializer = (JSONPojoDeserializer) createObjectDeserializer(clazz);
            return pojoDeserializer.deserializePojo(charSource, buf, fromIndex, genericParameterizedType, instance, parseContext);
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType genericParameterizedType, Object instance, char endToken, JSONParseContext parseContext) throws Exception {
            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '{':
                    return deserializeObject(charSource, buf, fromIndex, genericParameterizedType, instance, parseContext);
                case 'n':
                    return parseNull(buf, fromIndex, parseContext);
                default: {
                    if (parseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                        parseContext.endIndex = fromIndex + 1;
                        return null;
                    }
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' , expected '{' ");
                }
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType genericParameterizedType, Object instance, byte endToken, JSONParseContext parseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            char beginChar = (char) beginByte;
            switch (beginChar) {
                case '{':
                    return deserializeObject(charSource, buf, fromIndex, genericParameterizedType, instance, parseContext);
                case 'n':
                    return parseNull(buf, fromIndex, parseContext);
                default: {
                    if (parseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                        parseContext.endIndex = fromIndex + 1;
                        return null;
                    }
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' , expected '{' ");
                }
            }
        }
    }

    /***
     * 12、ANY
     */
    final static class AnyImpl extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return value;
        }

        @Override
        boolean validate(CharSource charSource, char[] buf, int fromIndex, int toIndex, char endToken, JSONParseContext parseContext) throws Exception {
            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '{':
                    return MAP.validate(charSource, buf, fromIndex, toIndex, endToken, parseContext);
                case '[':
                    return COLLECTION.validate(charSource, buf, fromIndex, toIndex, endToken, parseContext);
                case '\'':
                case '"':
                    CHAR_SEQUENCE.skip(charSource, buf, fromIndex, beginChar, parseContext);
                    break;
                case 'n':
                    parseNull(buf, fromIndex, parseContext);
                    break;
                case 't': {
                    parseTrue(buf, fromIndex, parseContext);
                    break;
                }
                case 'f': {
                    parseFalse(buf, fromIndex, parseContext);
                    break;
                }
                default: {
                    NUMBER_SKIPPER.deserialize(charSource, buf, fromIndex, null, null, endToken, parseContext);
                    break;
                }
            }
            return !parseContext.validateFail;
        }

        @Override
        boolean validate(CharSource charSource, byte[] buf, int fromIndex, int toIndex, byte endToken, JSONParseContext parseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            switch (beginByte) {
                case '{':
                    return MAP.validate(charSource, buf, fromIndex, toIndex, endToken, parseContext);
                case '[':
                    return COLLECTION.validate(charSource, buf, fromIndex, toIndex, endToken, parseContext);
                case '\'':
                case '"':
                    CHAR_SEQUENCE.skip(charSource, buf, fromIndex, beginByte, parseContext);
                    break;
                case 'n':
                    parseNull(buf, fromIndex, parseContext);
                    break;
                case 't': {
                    parseTrue(buf, fromIndex, parseContext);
                    break;
                }
                case 'f': {
                    parseFalse(buf, fromIndex, parseContext);
                    break;
                }
                default: {
                    NUMBER_SKIPPER.deserialize(charSource, buf, fromIndex, null, null, endToken, parseContext);
                    break;
                }
            }
            return !parseContext.validateFail;
        }

        void skip(CharSource charSource, char[] buf, int fromIndex, char endToken, JSONParseContext parseContext) throws Exception {

            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '{':
                    MAP.skip(charSource, buf, fromIndex, parseContext);
                    break;
                case '[':
                    COLLECTION.skip(charSource, buf, fromIndex, parseContext);
                    break;
                case '\'':
                case '"':
                    CHAR_SEQUENCE.skip(charSource, buf, fromIndex, beginChar, parseContext);
                    break;
                case 'n':
                    parseNull(buf, fromIndex, parseContext);
                    break;
                case 't': {
                    parseTrue(buf, fromIndex, parseContext);
                    break;
                }
                case 'f': {
                    parseFalse(buf, fromIndex, parseContext);
                    break;
                }
                default: {
                    NUMBER_SKIPPER.deserialize(charSource, buf, fromIndex, null, null, endToken, parseContext);
                    break;
                }
            }
        }

        void skip(CharSource charSource, byte[] buf, int fromIndex, byte endToken, JSONParseContext parseContext) throws Exception {

            byte beginByte = buf[fromIndex];
            switch (beginByte) {
                case '{':
                    MAP.skip(charSource, buf, fromIndex, parseContext);
                    break;
                case '[':
                    COLLECTION.skip(charSource, buf, fromIndex, parseContext);
                    break;
                case '\'':
                case '"':
                    CHAR_SEQUENCE.skip(charSource, buf, fromIndex, beginByte, parseContext);
                    break;
                case 'n':
                    parseNull(buf, fromIndex, parseContext);
                    break;
                case 't': {
                    parseTrue(buf, fromIndex, parseContext);
                    break;
                }
                case 'f': {
                    parseFalse(buf, fromIndex, parseContext);
                    break;
                }
                default: {
                    NUMBER_SKIPPER.deserialize(charSource, buf, fromIndex, null, null, endToken, parseContext);
                    break;
                }
            }
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext parseContext) throws Exception {

            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '{':
                    return JSONDefaultParser.parseJSONObject(charSource, buf, fromIndex, new LinkedHashMap(), parseContext);
                case '[':
                    return JSONDefaultParser.parseJSONArray(charSource, buf, fromIndex, new ArrayList(), parseContext);
                case '\'':
                case '"':
                    return CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, beginChar, GenericParameterizedType.StringType, parseContext);
                case 'n':
                    return parseNull(buf, fromIndex, parseContext);
                case 't': {
                    return parseTrue(buf, fromIndex, parseContext);
                }
                case 'f': {
                    return parseFalse(buf, fromIndex, parseContext);
                }
                default: {
                    return NUMBER.deserialize(charSource, buf, fromIndex, parseContext.useBigDecimalAsDefault ? GenericParameterizedType.BigDecimalType : GenericParameterizedType.AnyType, null, endToken, parseContext);
                }
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext parseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            char beginChar = (char) beginByte;
            switch (beginChar) {
                case '{':
                    return JSONDefaultParser.parseJSONObject(charSource, buf, fromIndex, new LinkedHashMap(), parseContext);
                case '[':
                    return JSONDefaultParser.parseJSONArray(charSource, buf, fromIndex, new ArrayList(), parseContext);
                case '\'':
                case '"':
                    return CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, beginByte, GenericParameterizedType.StringType, parseContext);
                case 'n':
                    return parseNull(buf, fromIndex, parseContext);
                case 't': {
                    return parseTrue(buf, fromIndex, parseContext);
                }
                case 'f': {
                    return parseFalse(buf, fromIndex, parseContext);
                }
                default: {
                    return NUMBER.deserialize(null, buf, fromIndex, GenericParameterizedType.AnyType, null, endToken, parseContext);
                }
            }
        }
    }

    /**
     * read first field if key is '@C' and value as the implClass
     *
     * @param buf
     * @param fromIndex
     * @param parseContext
     * @return
     * @throws Exception
     */
    protected static final String parseObjectClassName(CharSource charSource, char[] buf, int fromIndex, JSONParseContext parseContext) throws Exception {
        char ch;
        for (int i = fromIndex + 1; /*i < toIndex*/ ; ++i) {
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (parseContext.allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
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
                    if (parseContext.allowUnquotedFieldNames) {
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
            if (parseContext.allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                }
            }
            if (ch == ':') {
                while ((ch = buf[++i]) <= ' ') ;
                if (parseContext.allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                Object value = ANY.deserialize(charSource, buf, i, null, null, '}', parseContext);
                if (value instanceof String) {
                    return ((String) value).trim();
                } else {
                    return null;
                }
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', token ':' is expected.");
            }
        }
    }

    /**
     * read first field if key is '@C' and value as the implClass
     *
     * @param buf
     * @param fromIndex
     * @param parseContext
     * @return
     * @throws Exception
     */
    protected static final String parseObjectClassName(CharSource charSource, byte[] buf, int fromIndex, JSONParseContext parseContext) throws Exception {
        byte b;
        for (int i = fromIndex + 1; /*i < toIndex*/ ; ++i) {
            while ((b = buf[i]) <= ' ') {
                ++i;
            }
            if (parseContext.allowComment) {
                if (b == '/') {
                    b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
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
                    if (parseContext.allowUnquotedFieldNames) {
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
            if (parseContext.allowComment) {
                if (b == '/') {
                    b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                }
            }
            if (b == ':') {
                while ((b = buf[++i]) <= ' ') ;
                if (parseContext.allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                Object value = ANY.deserialize(charSource, buf, i, null, null, END_OBJECT, parseContext);
                if (value instanceof String) {
                    return ((String) value).trim();
                } else {
                    return null;
                }
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', token ':' is expected.");
            }
        }
    }

    protected final static Class<?> getClassByName(String className) throws ClassNotFoundException {
        Class<?> cls = classNameMapping.get(className);
        if (cls != null) return cls;
        classNameMapping.put(className, cls = Class.forName(className));
        return cls;
    }

    // 属性声明为java.io.Serializable类型的反序列化处理使用ANY代理
    final static class SerializableImpl extends JSONTypeDeserializer {
        @Override
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext parseContext) throws Exception {
            Object value = ANY.deserialize(charSource, buf, fromIndex, parameterizedType, defaultValue, endToken, parseContext);
            if (value instanceof Serializable) {
                return value;
            }
            return null;
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext parseContext) throws Exception {
            Object value = ANY.deserialize(charSource, buf, fromIndex, parameterizedType, defaultValue, endToken, parseContext);
            if (value instanceof Serializable) {
                return value;
            }
            return null;
        }
    }

    // NonInstance类型处理写了类名的json字符串反序列化
    final static class NonInstanceImpl extends JSONTypeDeserializer {

        final Class<?> baseClass;

        NonInstanceImpl(Class<?> baseClass) {
            this.baseClass = baseClass;
        }

        boolean isAvailableImpl(Class<?> cls) {
            return baseClass.isAssignableFrom(cls) && ReflectConsts.getClassCategory(cls) == ReflectConsts.ClassCategory.ObjectCategory;
        }

        JSONTypeDeserializer getJSONTypeDeserializer(String className) {
            if (className != null) {
                try {
                    Class<?> cls = getClassByName(className);
                    // check if isAssignableFrom cls
                    if (isAvailableImpl(cls)) {
                        return getTypeDeserializer(cls);
                    }
                } catch (Throwable throwable) {
                    throw throwable instanceof RuntimeException ? (RuntimeException) throwable : new JSONException(throwable.getMessage(), throwable);
                }
            }
            return null;
        }

        @Override
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext parseContext) throws Exception {
            String className = parseObjectClassName(charSource, buf, fromIndex, parseContext);
            JSONPojoDeserializer deserializer = (JSONPojoDeserializer) getJSONTypeDeserializer(className);
            if (deserializer == null) {
                ANY.skip(charSource, buf, fromIndex, endToken, parseContext);
                return null;
            }
            int i = parseContext.endIndex;
            char ch;
            while ((ch = buf[++i]) <= ' ') ;
            if (ch == ',') {
                return deserializer.deserializePojo(charSource, buf, i, parameterizedType, defaultValue, parseContext);
            } else if (ch == '}') {
                parseContext.endIndex = i;
                return deserializer.pojo(deserializer.createPojo());
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or '}'");
            }
//            return deserializer.deserializePojo(charSource, buf, parseContext.endIndex, toIndex, parameterizedType, defaultValue, endToken, parseContext);
        }

        @Override
        protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext parseContext) throws Exception {
            String className = parseObjectClassName(charSource, bytes, fromIndex, parseContext);
            JSONPojoDeserializer deserializer = (JSONPojoDeserializer) getJSONTypeDeserializer(className);
            if (deserializer == null) {
                ANY.skip(charSource, bytes, fromIndex, endToken, parseContext);
                return null;
            }

            int i = parseContext.endIndex;
            byte b;
            while ((b = bytes[++i]) <= ' ') ;
            if (b == ',') {
                return deserializer.deserializePojo(charSource, bytes, i, parameterizedType, defaultValue, parseContext);
            } else if (b == '}') {
                parseContext.endIndex = i;
                return deserializer.pojo(deserializer.createPojo());
            } else {
                String errorContextTextAt = createErrorContextText(bytes, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or '}'");
            }
            // return deserializer.deserializePojo(charSource, bytes, fromIndex, toIndex, parameterizedType, defaultValue, endToken, parseContext);
        }
    }

    /**
     * 反序列化null
     *
     * @return
     */
    protected final static Object parseNull(byte[] bytes, int fromIndex, JSONParseContext parseContext) {
        int endIndex = fromIndex + 3;
        if (JSONUnsafe.getInt(bytes, fromIndex) == NULL_INT/*bytes[fromIndex + 1] == 'u' && bytes[fromIndex + 2] == 'l' && bytes[endIndex] == 'l'*/) {
            parseContext.endIndex = endIndex;
            return null;
        }
        if (parseContext.validate) {
            parseContext.validateFail = true;
            return null;
        }
        throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'null' because it starts with 'n', but found text '" + new String(bytes, fromIndex, Math.min(parseContext.toIndex - fromIndex + 1, 4)) + "'");
    }

    /**
     * 反序列化null
     *
     * @return
     */
    protected final static Object parseNull(char[] buf, int fromIndex, JSONParseContext parseContext) {
        int endIndex = fromIndex + 3;
        if (JSONUnsafe.getLong(buf, fromIndex) == NULL_LONG) {
            parseContext.endIndex = endIndex;
            return null;
        }
        if (parseContext.validate) {
            parseContext.validateFail = true;
            return null;
        }
        throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'null' because it starts with 'n', but found text '" + new String(buf, fromIndex, Math.min(parseContext.toIndex - fromIndex + 1, 4)) + "'");
    }

    protected static final boolean parseTrue(char[] buf, int fromIndex, JSONParseContext parseContext) throws Exception {
        int endIndex = fromIndex + 3;
        if (JSONUnsafe.getLong(buf, fromIndex) == TRUE_LONG) {
            parseContext.endIndex = endIndex;
            return true;
        }
        if (parseContext.validate) {
            parseContext.validateFail = true;
            return false;
        }
        int len = Math.min(parseContext.toIndex - fromIndex + 1, 4);
        throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'true' because it starts with 't', but found text '" + new String(buf, fromIndex, len) + "'");
    }

    protected static final boolean parseFalse(char[] buf, int fromIndex, JSONParseContext parseContext) throws Exception {
        int endIndex = fromIndex + 4;
        if (JSONUnsafe.getLong(buf, fromIndex + 1) == ALSE_LONG) {
            parseContext.endIndex = endIndex;
            return false;
        }
        if (parseContext.validate) {
            parseContext.validateFail = true;
            return false;
        }
        int len = Math.min(parseContext.toIndex - fromIndex + 1, 5);
        throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'false' because it starts with 'f', but found text '" + new String(buf, fromIndex, len) + "'");
    }

    protected final static boolean parseTrue(byte[] bytes, int fromIndex, JSONParseContext parseContext) throws Exception {
        int endIndex = fromIndex + 3;
        if (JSONUnsafe.getInt(bytes, fromIndex) == TRUE_INT) {
            parseContext.endIndex = endIndex;
            return true;
        }
        if (parseContext.validate) {
            parseContext.validateFail = true;
            return false;
        }
        int len = Math.min(parseContext.toIndex - fromIndex + 1, 4);
        throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'true' because it starts with 't', but found text '" + new String(bytes, fromIndex, len) + "'");
    }

    protected final static boolean parseFalse(byte[] bytes, int fromIndex, JSONParseContext parseContext) throws Exception {
        int endIndex = fromIndex + 4;
        if (JSONUnsafe.getInt(bytes, fromIndex + 1) == ALSE_INT) {
            parseContext.endIndex = endIndex;
            return false;
        }
        if (parseContext.validate) {
            parseContext.validateFail = true;
            return false;
        }
        int len = Math.min(parseContext.toIndex - fromIndex + 1, 5);
        throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'false' because it starts with 'f', but found text '" + new String(bytes, fromIndex, len) + "'");
    }

    static String parseMapKey(byte[] bytes, int from, char endCh, JSONParseContext parseContext) {
        int beginIndex = from + 1;
        byte b, next = '\0';
        int i = from;
        int len;

        JSONCharArrayWriter writer = null;
        boolean escape = false;
        for (; ; ) {
            while ((b = bytes[++i]) != '\\' && b != endCh) ;
            // b is \\ or "
            if (b == '\\') {
                if (i < parseContext.toIndex - 1) {
                    next = bytes[i + 1];
                }
                if (writer == null) {
                    writer = getContextWriter(parseContext);
                }
                escape = true;
                beginIndex = escape(bytes, next, i, beginIndex, writer, parseContext);
                i = parseContext.endIndex;
            } else {
                parseContext.endIndex = i;
                len = i - beginIndex;
                if (escape) {
                    writer.writeBytes(bytes, beginIndex, len);
                    return writer.toString();
                } else {
                    return len == 0 ? "" : new String(bytes, beginIndex, len);
                }
            }
        }
    }

    static String parseMapKeyByCache(byte[] bytes, int from, char endCh, JSONParseContext parseContext) {
        byte b, next = 0;
        int i = from, toIndex = parseContext.toIndex;
        int beginIndex = from + 1;
        int len;
        JSONCharArrayWriter writer = null;
        if (!parseContext.escape) {
            long hashValue = ESCAPE;
            byte b1;
            if ((b = bytes[++i]) != endCh && (b1 = bytes[++i]) != endCh) {
                hashValue = (hashValue << 16) | (b << 8) | b1;
                if ((b = bytes[++i]) != endCh && (b1 = bytes[++i]) != endCh) {
                    hashValue = (hashValue << 16) | (b << 8) | b1;
                    while ((b = bytes[++i]) != endCh && (b1 = bytes[++i]) != endCh) {
                        hashValue = (hashValue << 16) | (b << 8) | b1;
                    }
                }
            }
            if (b != endCh) {
                hashValue = hashValue << 8 | b;
            }
            parseContext.endIndex = i;
            len = i - beginIndex;
            if (len <= 8) {
                return parseContext.getCacheEightBytesKey(bytes, beginIndex, len, hashValue);
            }
            return parseContext.getCacheKey(bytes, beginIndex, len, hashValue);
        } else {
            boolean escape = false;
            for (; ; ) {
                // Setting to ESCAPE can solve interference
                long hashValue = ESCAPE;
                while ((b = bytes[++i]) != '\\' && b != endCh) {
                    hashValue = hashValue << 8 | b;
                }
                // ch is \\ or "
                if (b == '\\') {
                    if (i < toIndex - 1) {
                        next = bytes[i + 1];
                    }
                    if (writer == null) {
                        writer = getContextWriter(parseContext);
                    }
                    escape = true;
                    beginIndex = escape(bytes, next, i, beginIndex, writer, parseContext);
                    i = parseContext.endIndex;
                } else {
                    parseContext.endIndex = i;
                    len = i - beginIndex;
                    if (escape) {
                        writer.writeBytes(bytes, beginIndex, len);
                        return writer.toString();
                    } else {
                        if (len <= 8) {
                            return parseContext.getCacheEightBytesKey(bytes, beginIndex, len, hashValue);
                        }
                        return parseContext.getCacheKey(bytes, beginIndex, len, hashValue);
                    }
                }
            }
        }
    }
}
