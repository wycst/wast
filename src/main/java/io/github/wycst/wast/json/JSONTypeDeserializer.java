package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.CharSource;
import io.github.wycst.wast.common.beans.DateTemplate;
import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
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

    private final static int LENGTH = ReflectConsts.ClassCategory.values().length;

    final static JSONTypeDeserializer[] TYPE_DESERIALIZERS = new JSONTypeDeserializer[LENGTH];
    private final static CharSequenceDeserializer CHAR_SEQUENCE = new CharSequenceDeserializer();
    final protected static CharSequenceDeserializer.StringDeserializer CHAR_SEQUENCE_STRING;
    final protected static NumberDeserializer NUMBER = new NumberDeserializer();
    final protected static NumberDeserializer NUMBER_LONG = new NumberDeserializer.LongDeserializer();
    final protected static NumberDeserializer NUMBER_INTEGER = new NumberDeserializer.IntegerDeserializer();
    final protected static NumberDeserializer NUMBER_SHORT = new NumberDeserializer.ShortDeserializer();
    final protected static NumberDeserializer NUMBER_BYTE = new NumberDeserializer.ByteDeserializer();
    final protected static NumberDeserializer NUMBER_FLOAT = new NumberDeserializer.FloatDeserializer();
    final protected static NumberDeserializer NUMBER_DOUBLE = new NumberDeserializer.DoubleDeserializer();
    final protected static NumberDeserializer NUMBER_BIGDECIMAL = new NumberDeserializer.BigDecimalDeserializer();
    final protected static NumberDeserializer NUMBER_BIGINTEGER = new NumberDeserializer.BigIntegerDeserializer();
    final protected static BinaryDeserializer BINARY = new BinaryDeserializer();
    final static BooleanDeserializer BOOLEAN = new BooleanDeserializer();
    final static DateDeserializer DATE = new DateDeserializer();
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

    static final Set<Class<?>> BUILT_IN_TYPE_SET;

    static {
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.CharSequence.ordinal()] = CHAR_SEQUENCE;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.NumberCategory.ordinal()] = NUMBER;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.BoolCategory.ordinal()] = BOOLEAN;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.DateCategory.ordinal()] = DATE;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ClassCategory.ordinal()] = new ClassDeserializer();
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.EnumCategory.ordinal()] = new EnumDeserializer();
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.AnnotationCategory.ordinal()] = new AnnotationDeserializer();
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.Binary.ordinal()] = BINARY;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ArrayCategory.ordinal()] = ARRAY;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.CollectionCategory.ordinal()] = COLLECTION;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.MapCategory.ordinal()] = MAP;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ObjectCategory.ordinal()] = OBJECT;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ANY.ordinal()] = ANY;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.NonInstance.ordinal()] = null;

        putTypeDeserializer(NUMBER_LONG, long.class, Long.class);
        putTypeDeserializer(NUMBER_INTEGER, int.class, Integer.class);
        putTypeDeserializer(NUMBER_DOUBLE, double.class, Double.class);
        putTypeDeserializer(NUMBER_FLOAT, float.class, Float.class);
        putTypeDeserializer(NUMBER_SHORT, short.class, Short.class);
        putTypeDeserializer(NUMBER_BYTE, byte.class, Byte.class);
        putTypeDeserializer(NUMBER_BIGDECIMAL, BigDecimal.class);
        putTypeDeserializer(NUMBER_BIGINTEGER, BigInteger.class);

        putTypeDeserializer(new ArrayDeserializer.StringArrayDeserializer(), String[].class);
        putTypeDeserializer(new ArrayDeserializer.DoubleArrayDeserializer(), Double[].class);
        putTypeDeserializer(new ArrayDeserializer.PrimitiveDoubleArrayDeserializer(), double[].class);
        putTypeDeserializer(new ArrayDeserializer.FloatArrayDeserializer(), Float[].class);
        putTypeDeserializer(new ArrayDeserializer.PrimitiveFloatArrayDeserializer(), float[].class);
        putTypeDeserializer(new ArrayDeserializer.LongArrayDeserializer(), Long[].class);
        putTypeDeserializer(new ArrayDeserializer.PrimitiveLongArrayDeserializer(), long[].class);
        putTypeDeserializer(new ArrayDeserializer.IntArrayDeserializer(), Integer[].class);
        putTypeDeserializer(new ArrayDeserializer.PrimitiveIntArrayDeserializer(), int[].class);
        putTypeDeserializer(new ArrayDeserializer.ByteArrayDeserializer(), Byte[].class);
        putTypeDeserializer(BINARY, byte[].class);

        CharSequenceDeserializer.StringDeserializer defaultStringDeserializer = new CharSequenceDeserializer.StringDeserializer();
        if (EnvUtils.JDK_9_PLUS) {
            if (EnvUtils.JDK_16_PLUS) {
                defaultStringDeserializer = new CharSequenceDeserializer.StringJDK16PlusDeserializer();
            } else {
                // JDK9~JDK15
                defaultStringDeserializer = new CharSequenceDeserializer.StringJDK9PlusDeserializer();
            }
        }

        CHAR_SEQUENCE_STRING = defaultStringDeserializer;
        putTypeDeserializer(CHAR_SEQUENCE_STRING, String.class);
        putTypeDeserializer(SERIALIZABLE_DESERIALIZER, Serializable.class);
        // other types
        putTypeDeserializer(new CharSequenceDeserializer.CharDeserializer(), char.class, Character.class);
        putTypeDeserializer(new UUIDDeserializer(), UUID.class);
        putTypeDeserializer(MapDeserializer.hashtable(), Dictionary.class);

        BUILT_IN_TYPE_SET = new HashSet<Class<?>>(classJSONTypeDeserializerMap.keySet());
    }

    static boolean isBuiltInType(Class<?> type) {
        return BUILT_IN_TYPE_SET.contains(type);
    }

    static void putTypeDeserializer(JSONTypeDeserializer typeDeserializer, Class... types) {
        for (Class type : types) {
            classJSONTypeDeserializerMap.put(type, typeDeserializer);
        }
    }

    static JSONTypeDeserializer getCachedTypeDeserializer(Class type) {
        return classJSONTypeDeserializerMap.get(type);
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
                    typeDeserializer = createEnumDeserializer(type);
                    break;
                }
                case MapCategory: {
                    // key/value
                    Class[] kvTypes = ReflectUtils.getMapDefinedKVTypes(type);
                    if (kvTypes == null) {
                        typeDeserializer = MAP;
                    } else {
                        typeDeserializer = new CustomMapDeserializer(kvTypes[0], kvTypes[1]);
                    }
                    break;
                }
                case NonInstance: {
                    typeDeserializer = new NonInstanceDeserializer(type);
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
        JSONPojoStructure objectStructureWrapper = JSONPojoStructure.get(type);
        if (objectStructureWrapper.isRecord()) {
            return new JSONPojoDeserializer.JSONRecordDeserializer(objectStructureWrapper);
        } else {
            if (objectStructureWrapper.isTemporal()) {
                ClassStructureWrapper.ClassWrapperType classWrapperType = objectStructureWrapper.getClassWrapperType();
                return JSONTemporalDeserializer.getTemporalDeserializerInstance(classWrapperType, GenericParameterizedType.actualType(type), null);
            }
            return new JSONPojoDeserializer(objectStructureWrapper);
        }
    }

    private static JSONTypeDeserializer createEnumDeserializer(Class type) {
        Enum[] values = (Enum[]) type.getEnumConstants();
        Map<String, Enum> enumValues = new HashMap<String, Enum>();
        for (Enum value : values) {
            String name = value.name();
            enumValues.put(name, value);
        }
        JSONValueMatcher<Enum> enumMatcher = JSONValueMatcher.build(enumValues);
        return new EnumDeserializer.EnumInstanceDeserializer(values, enumMatcher);
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
                int collectionType = getCollectionType(genericParameterizedType.getActualType());
                switch (collectionType) {
                    case COLLECTION_ARRAYLIST_TYPE:
                        return new CollectionDeserializer.ArrayListDeserializer(genericParameterizedType);
                    case COLLECTION_HASHSET_TYPE:
                        return new CollectionDeserializer.HashSetDeserializer(genericParameterizedType);
                    default:
                        return new CollectionDeserializer.CollectionInstanceDeserializer(genericParameterizedType);
                }
            case DateCategory: {
                // Date Deserializer Instance
                // cannot use singleton because the pattern of each field may be different
                return property == null ? DATE : new DateDeserializer.DateInstanceDeserializer(genericParameterizedType, property);
            }
            case ObjectCategory: {
                ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(genericParameterizedType.getActualType());
                // Like the date type, the temporal type cannot use singletons also
                if (classStructureWrapper.isTemporal()) {
                    ClassStructureWrapper.ClassWrapperType classWrapperType = classStructureWrapper.getClassWrapperType();
                    return JSONTemporalDeserializer.getTemporalDeserializerInstance(classWrapperType, genericParameterizedType, property);
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
    static class CharSequenceDeserializer extends JSONTypeDeserializer {

        protected void skip(CharSource source, char[] buf, int fromIndex, char endCh, JSONParseContext jsonParseContext) throws Exception {
            if (source != null) {
                int beginIndex = fromIndex + 1;
                int endIndex = source.indexOf(endCh, beginIndex);
                char prev = buf[endIndex - 1];
                while (prev == '\\') {
                    boolean prevEscapeFlag = true;
                    int j = endIndex - 1;
                    while (buf[--j] == '\\') {
                        prevEscapeFlag = !prevEscapeFlag;
                    }
                    if (prevEscapeFlag) {
                        endIndex = source.indexOf(endCh, endIndex + 1);
                        prev = buf[endIndex - 1];
                    } else {
                        break;
                    }
                }
                jsonParseContext.endIndex = endIndex;
                return;
            }

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
            jsonParseContext.endIndex = i;
        }

        protected void skip(CharSource source, byte[] buf, int fromIndex, int endCh, JSONParseContext jsonParseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            if (source != null) {
                String input = source.input();
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
                jsonParseContext.endIndex = endIndex;
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
            jsonParseContext.endIndex = i;
        }

        protected Object unEscapeAsciiResult(String source, byte[] buf, int beginIndex, int len, GenericParameterizedType parameterizedType) {
            Class<?> actualType = parameterizedType.getActualType();
            if (actualType == char[].class) {
                char[] chars = new char[len];
                for (int i = 0; i < len; ++i) {
                    chars[i] = (char) buf[beginIndex++];
                }
                return chars;
            }
            if (actualType == StringBuilder.class) {
                return new StringBuilder().append(source, beginIndex, beginIndex + len);
            }
            return new StringBuilder().append(source, beginIndex, beginIndex + len);
        }

        protected Object escapeResult(JSONCharArrayWriter writer, GenericParameterizedType parameterizedType) {
            Class<?> actualType = parameterizedType.getActualType();
            if (actualType == char[].class) {
                return writer.toChars();
            }
            if (actualType == StringBuilder.class) {
                return writer.toStringBuilder();
            }
            return writer.toStringBuffer();
        }

        Object deserializeString(CharSource charSource, byte[] buf, int fromIndex, int toIndex, int endByte, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) {
            if (charSource != null) {
                // ASCII mode
                return deserializeAsciiCharSource(charSource, buf, fromIndex, endByte, parameterizedType, jsonParseContext);
            } else {
                // UTF-8 & JDK8
                JSONCharArrayWriter writer = getContextWriter(jsonParseContext);
                char[] chars = writer.ensureCapacity(buf.length);
                int count = writer.count;
                byte b, b0 = 0;
                int i = fromIndex;
                for (; ; ) {
                    boolean bEndFlag;
                    // Read 2 bytes per iteration
                    while ((bEndFlag = ((b = buf[++i]) != '\\' && b != endByte)) && (b0 = buf[++i]) != '\\' && b0 != endByte) {
                        // b >= 0 & b0 > 0
                        // b >= 0 & b0 < 0
                        // b < 0 & b0 < 0
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
                        jsonParseContext.endIndex = i;
                        writer.count = count;
                        return charSequenceResult(writer, parameterizedType);
                    } else {
                        byte next = '\0';
                        if (i < toIndex - 1) {
                            next = buf[i + 1];
                        }
                        writer.count = count;
                        escape(buf, next, i, i, writer, jsonParseContext);
                        count = writer.count;
                        i = jsonParseContext.endIndex;
                    }
                }
            }
        }

        Object deserializeAsciiCharSource(CharSource charSource, byte[] buf, int fromIndex, int endByte, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) {
            return CHAR_SEQUENCE_STRING.deserializeAsciiCharSourceAs(charSource, buf, fromIndex, endByte, parameterizedType, jsonParseContext);
        }

        // common
        Object deserializeString(CharSource source, char[] buf, int fromIndex, int toIndex, char endChar, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) {
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
                    jsonParseContext.endIndex = i;
                    len = i - beginIndex;
                    return charSequenceResult(buf, beginIndex, len, unEscape, writer, parameterizedType);
                } else {
                    if (writer == null) {
                        writer = getContextWriter(jsonParseContext);
                        unEscape = false;
                    }
                    beginIndex = escapeNext(buf, buf[i + 1], i, beginIndex, writer, jsonParseContext);
                    i = jsonParseContext.endIndex;
                }
            }
        }

        Object charSequenceResult(char[] buf, int beginIndex, int len, boolean unEscape, JSONCharArrayWriter writer, GenericParameterizedType parameterizedType) {
            Class<?> actualType = parameterizedType.getActualType();
            boolean isCharArray = actualType == char[].class;
            if (unEscape) {
                if (isCharArray) {
                    char[] chars = new char[len];
                    System.arraycopy(buf, beginIndex, chars, 0, len);
                    return chars;
                } else {
                    if (actualType == StringBuilder.class) {
                        return new StringBuilder(len).append(buf, beginIndex, len);
                    } else {
                        return new StringBuffer(len).append(buf, beginIndex, len);
                    }
                }
            } else {
                writer.write(buf, beginIndex, len);
                if (isCharArray) {
                    return writer.toChars();
                } else {
                    if (actualType == StringBuilder.class) {
                        return writer.toStringBuilder();
                    } else {
                        return writer.toStringBuilder();
                    }
                }
            }
        }

        Object charSequenceResult(JSONCharArrayWriter writer, GenericParameterizedType parameterizedType) {
            Class<?> actualType = parameterizedType.getActualType();
            boolean isCharArray = actualType == char[].class;
            if (isCharArray) {
                return writer.toChars();
            } else {
                if (actualType == StringBuilder.class) {
                    return writer.toStringBuilder();
                } else {
                    return writer.toStringBuffer();
                }
            }
        }

        // String/StringBuffer/StringBuilder/char[]
        @Override
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            if (beginChar == '"') {
                return deserializeString(charSource, buf, fromIndex, toIndex, beginChar, parameterizedType, jsonParseContext);
            } else if (beginChar == 'n') {
                return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
            } else {
                // 当类型为字符串时兼容number转化
                boolean isStringType = parameterizedType.getActualType() == String.class;
                if (isStringType) {
                    // 不使用skip的原因确保能转化为number，否则认定token错误
                    try {
//                        NUMBER.deserializeOfString(buf, fromIndex, toIndex, endToken, ReflectConsts.CLASS_TYPE_NUMBER_DOUBLE, jsonParseContext);
                        NUMBER.deserialize(charSource, buf, fromIndex, toIndex, null, null, endToken, jsonParseContext);
                        return new String(buf, fromIndex, jsonParseContext.endIndex + 1 - fromIndex);
                    } catch (Throwable throwable) {
                    }
                }
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for CharSequence, expected '\"' ");
            }
        }

        @Override
        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte b = buf[fromIndex];
            if (b == '"') {
                return deserializeString(charSource, buf, fromIndex, toIndex, b, parameterizedType, jsonParseContext);
            } else if (b == 'n') {
                return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
            } else {
                // 当类型为字符串时兼容number转化
                boolean isStringType = parameterizedType.getActualType() == String.class;
                if (isStringType) {
                    // 不使用skip的原因确保能转化为number，否则认定token错误
                    try {
                        NUMBER.deserialize(charSource, buf, fromIndex, toIndex, null, null, endToken, jsonParseContext);
                        // NUMBER.deserializeOfString(buf, fromIndex, toIndex, endToken, ReflectConsts.CLASS_TYPE_NUMBER_DOUBLE, jsonParseContext);
                        return new String(buf, fromIndex, jsonParseContext.endIndex + 1 - fromIndex);
                    } catch (Throwable throwable) {
                    }
                }
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "' for CharSequence, expected '\"' ");
            }
        }

        // java.lang.String
        static class StringDeserializer extends CharSequenceDeserializer {

            final Object deserializeString(CharSource charSource, char[] buf, int fromIndex, int toIndex, char endChar, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) {
                if (charSource == null) {
                    return super.deserializeString(charSource, buf, fromIndex, toIndex, endChar, parameterizedType, jsonParseContext);
                }
                int beginIndex = fromIndex + 1;
                String source = charSource.input();
                int endIndex = source.indexOf(endChar, beginIndex);
                if (!jsonParseContext.checkEscapeUseChar(source, beginIndex, endIndex)) {
                    jsonParseContext.endIndex = endIndex;
                    return new String(buf, beginIndex, endIndex - beginIndex);
                }
                // must exist \\ in range {beginIndex, endIndex}
                JSONCharArrayWriter writer = getContextWriter(jsonParseContext);
                do {
                    int escapeIndex = jsonParseContext.getEscapeOffset();
                    beginIndex = escapeNext(buf, buf[escapeIndex + 1], escapeIndex, beginIndex, writer, jsonParseContext);
                    if (beginIndex > endIndex) {
                        endIndex = source.indexOf(endChar, endIndex + 1);
                    }
                } while (jsonParseContext.checkEscapeUseChar(source, beginIndex, endIndex));

                jsonParseContext.endIndex = endIndex;
                writer.write(charSource.input(), beginIndex, endIndex - beginIndex);
                return writer.toString();
            }

            @Override
            final Object charSequenceResult(char[] buf, int beginIndex, int len, boolean unEscape, JSONCharArrayWriter writer, GenericParameterizedType parameterizedType) {
                if (unEscape) {
//                     return new String(buf, beginIndex, len);
                    return UnsafeHelper.getString(MemoryOptimizerUtils.copyOfRange(buf, beginIndex, len));
                } else {
                    writer.write(buf, beginIndex, len);
                    return writer.toString();
                }
            }

            @Override
            final String charSequenceResult(JSONCharArrayWriter writer, GenericParameterizedType parameterizedType) {
                return writer.toString();
            }

            public Object deserializeAsciiCharSourceAs(CharSource charSource, byte[] buf, int fromIndex, int endByte, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) {
                throw new UnsupportedOperationException();
            }
        }

        static class StringJDK9PlusDeserializer extends StringDeserializer {

            public Object deserializeAsciiCharSourceAs(CharSource charSource, byte[] buf, int fromIndex, int endByte, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) {
                String source = charSource.input();
                String token = endByte == '"' ? "\"" : "'";
                int beginIndex = fromIndex + 1;
                int endIndex = source.indexOf(token, beginIndex);
                if (!jsonParseContext.checkEscapeUseString(source, beginIndex, endIndex)) {
                    jsonParseContext.endIndex = endIndex;
                    int len = endIndex - beginIndex;
                    return unEscapeAsciiResult(source, buf, beginIndex, len, parameterizedType);
                }
                JSONCharArrayWriter writer = getContextWriter(jsonParseContext);
                do {
                    int escapeIndex = jsonParseContext.getEscapeOffset();
                    beginIndex = escapeAscii(source, buf, buf[escapeIndex + 1], escapeIndex, beginIndex, writer, jsonParseContext);
                    if (beginIndex > endIndex) {
                        endIndex = source.indexOf(token, endIndex + 1);
                    }
                } while (jsonParseContext.checkEscapeUseString(source, beginIndex, endIndex));

                jsonParseContext.endIndex = endIndex;
                writer.write(source, beginIndex, endIndex - beginIndex);
                return escapeResult(writer, parameterizedType);
            }

            Object deserializeAsciiCharSource(CharSource charSource, byte[] buf, int fromIndex, int endByte, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) {
                String source = charSource.input();
                String token = endByte == '"' ? "\"" : "'";
                int beginIndex = fromIndex + 1;
                int endIndex = source.indexOf(token, beginIndex);
                if (!jsonParseContext.checkEscapeUseString(source, beginIndex, endIndex)) {
                    jsonParseContext.endIndex = endIndex;
                    return charSource.substring(beginIndex, endIndex);
                }
                JSONCharArrayWriter writer = getContextWriter(jsonParseContext);
                do {
                    int escapeIndex = jsonParseContext.getEscapeOffset();
                    beginIndex = escapeAscii(source, buf, buf[escapeIndex + 1], escapeIndex, beginIndex, writer, jsonParseContext);
                    if (beginIndex > endIndex) {
                        endIndex = source.indexOf(token, endIndex + 1);
                    }
                } while (jsonParseContext.checkEscapeUseString(source, beginIndex, endIndex));

                jsonParseContext.endIndex = endIndex;
                writer.write(source, beginIndex, endIndex - beginIndex);
                return writer.toString();
            }
        }

        static class StringJDK16PlusDeserializer extends StringDeserializer {

            public Object deserializeAsciiCharSourceAs(CharSource charSource, byte[] buf, int fromIndex, int endByte, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) {
                String source = charSource.input();
                int beginIndex = fromIndex + 1;
                int endIndex = source.indexOf(endByte, beginIndex);
                if (!jsonParseContext.checkEscapeUseChar(source, beginIndex, endIndex)) {
                    jsonParseContext.endIndex = endIndex;
                    int len = endIndex - beginIndex;
                    return unEscapeAsciiResult(source, buf, beginIndex, len, parameterizedType);
                }
                // must exist \\ in range {beginIndex, endIndex}
                JSONCharArrayWriter writer = getContextWriter(jsonParseContext);
                do {
                    int escapeIndex = jsonParseContext.getEscapeOffset();
                    beginIndex = escapeAscii(source, buf, buf[escapeIndex + 1], escapeIndex, beginIndex, writer, jsonParseContext);
                    if (beginIndex > endIndex) {
                        endIndex = source.indexOf(endByte, endIndex + 1);
                    }
                } while (jsonParseContext.checkEscapeUseChar(source, beginIndex, endIndex));

                jsonParseContext.endIndex = endIndex;
                writer.write(source, beginIndex, endIndex - beginIndex);
                return escapeResult(writer, parameterizedType);
            }

            String deserializeAsciiCharSource(CharSource charSource, byte[] buf, int fromIndex, int endByte, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) {
                String source = charSource.input();
                int beginIndex = fromIndex + 1;
                int endIndex = source.indexOf(endByte, beginIndex);
                if (!jsonParseContext.checkEscapeUseChar(source, beginIndex, endIndex)) {
                    jsonParseContext.endIndex = endIndex;
                    return charSource.substring(beginIndex, endIndex);
                }
                JSONCharArrayWriter writer = getContextWriter(jsonParseContext);
                do {
                    int escapeIndex = jsonParseContext.getEscapeOffset();
                    beginIndex = escapeAscii(source, buf, buf[escapeIndex + 1], escapeIndex, beginIndex, writer, jsonParseContext);
                    if (beginIndex > endIndex) {
                        endIndex = source.indexOf(endByte, endIndex + 1);
                    }
                } while (jsonParseContext.checkEscapeUseChar(source, beginIndex, endIndex));

                jsonParseContext.endIndex = endIndex;
                writer.write(source, beginIndex, endIndex - beginIndex);
                return writer.toString();
            }
        }

        public static class CharDeserializer extends CharSequenceDeserializer {

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return value.charAt(0);
            }

            @Override
            protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
                char firstChar = buf[fromIndex];
                switch (firstChar) {
                    case '\'':
                    case '\"': {
                        char result = 0;
                        String value = (String) CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, toIndex, firstChar, null, jsonParseContext);
                        if (value.length() > 0) {
                            result = value.charAt(0);
                        }
                        return result;
                    }
                    case 'n': {
                        return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
                    }
                    default: {
                        // numbder
                        short value = (Short) NUMBER_SHORT.deserialize(charSource, buf, fromIndex, toIndex, parameterizedType, instance, endToken, jsonParseContext);
                        return (char) value;
                    }
                }
            }

            @Override
            protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
                byte firstByte = buf[fromIndex];
                switch (firstByte) {
                    case '\'':
                    case '\"': {
                        char result = 0;
                        String value = (String) CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, toIndex, firstByte, null, jsonParseContext);
                        if (value.length() > 0) {
                            result = value.charAt(0);
                        }
                        return result;
                    }
                    case 'n': {
                        return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
                    }
                    default: {
                        // numbder
                        short value = (Short) NUMBER_SHORT.deserialize(charSource, buf, fromIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
                        return (char) value;
                    }
                }
            }
        }
    }

    // 1、number
    static class NumberDeserializer extends JSONTypeDeserializer {

        protected NumberDeserializer() {
        }

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return Double.parseDouble(value);
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
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
                    return deserializeNumber(beginChar - '0', false, 1, buf, fromIndex, ++fromIndex, toIndex, parameterizedType, endToken, jsonParseContext);
                }
                default: {
                    switch (beginChar) {
                        case '0':
                        case '+':
                            return deserializeNumber(0, false, 0, buf, fromIndex, ++fromIndex, toIndex, parameterizedType, endToken, jsonParseContext);
                        case '-':
                            return deserializeNumber(0, true, 0, buf, fromIndex, ++fromIndex, toIndex, parameterizedType, endToken, jsonParseContext);
                        case '"': {
                            try {
                                // 兼容字符串转化,存在嵌套\"\"问题
                                char ch = buf[++fromIndex];
                                if (ch == '"') {
                                    // discovered empty ""
                                    if (jsonParseContext.unMatchedEmptyAsNull) {
                                        jsonParseContext.endIndex = fromIndex;
                                        return null;
                                    }
                                    // if return null?
                                    throw new JSONException("Syntax error, at pos " + fromIndex + ", unexpected empty string '\"\"' when parsing a number, use ReadOption.UnMatchedEmptyAsNull to support");
                                }
                                Number result = (Number) deserialize(charSource, buf, fromIndex, toIndex, parameterizedType, instance, '"', jsonParseContext);
                                int endIndex = jsonParseContext.endIndex;
                                if (buf[++endIndex] != '"') {
                                    throw new JSONException("Syntax error, for input: \"" + new String(buf, fromIndex, endIndex - fromIndex) + "\", unable to convert to number");
                                }
                                jsonParseContext.endIndex = endIndex;
                                // 兼容类型处理
                                return ObjectUtils.toTypeNumber(result, parameterizedType.getActualType());
                            } catch (Exception exception) {
                                // 是否支持空字符串？
                                throw exception;
                            }
                        }
                        case 'n': {
                            NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
                            if (parameterizedType.getActualType().isPrimitive()) {
                                return ZERO;
                            }
                            return null;
                        }
                    }
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", unexpected character '" + beginChar + "' when parsing a number");
                }
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
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
                    return deserializeNumber(beginByte - 48, false, 1, buf, fromIndex, ++fromIndex, toIndex, parameterizedType, endToken, jsonParseContext);
                case '0':
                case '+':
                    return deserializeNumber(0, false, 0, buf, fromIndex, ++fromIndex, toIndex, parameterizedType, endToken, jsonParseContext);
                case '-':
                    return deserializeNumber(0, true, 0, buf, fromIndex, ++fromIndex, toIndex, parameterizedType, endToken, jsonParseContext);
                case '"': {
                    try {
                        // 兼容字符串转化,存在嵌套\"\"问题
                        byte b = buf[++fromIndex];
                        if (b == DOUBLE_QUOTATION) {
                            throw new JSONException("Syntax error, at pos " + fromIndex + ", unexpected character '\"' when parsing a number");
                        }
                        Number result = (Number) deserialize(charSource, buf, fromIndex, toIndex, parameterizedType, instance, DOUBLE_QUOTATION, jsonParseContext);
                        int endIndex = jsonParseContext.endIndex;
                        if (buf[++endIndex] != '"') {
                            throw new JSONException("For input string: \"" + new String(buf, fromIndex, endIndex - fromIndex) + "\"");
                        }
                        jsonParseContext.endIndex = endIndex;
                        return result;
                    } catch (Exception exception) {
                        // 是否支持空字符串？
                        throw exception;
                    }
                }
                case 'n': {
                    NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
                    if (parameterizedType.getActualType().isPrimitive()) {
                        return ZERO;
                    }
                    return null;
                }
            }
            throw new JSONException("Syntax error, at pos " + fromIndex + ", unexpected character '" + beginByte + "' when parsing a number");
        }

        Number deserializeDefault0(char[] buf, int fromIndex, int toIndex, int offset, long val, int cnt, boolean negative, char endToken, int returnType, JSONParseContext jsonParseContext) throws Exception {
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
                while (isDigit((ch = buf[i]))) {
                    value = (value << 3) + (value << 1) + ch - 48;
                    ++cnt;
                    ++i;
                }
                if (ch == '.') {
                    // 小数点模式
                    mode = 1;
                    // direct scan numbers
                    while (isDigit((ch = buf[++i]))) {
                        value = (value << 3) + (value << 1) + ch - 48;
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
                    if (isDigit(ch)) {
                        expValue = ch - 48;
                        while (isDigit(ch = buf[++i])) {
                            expValue = (expValue << 3) + (expValue << 1) + ch - 48;
                        }
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
            jsonParseContext.endIndex = endIndex;
            if (returnType == TYPE_DOUBLE) {
                if (cnt > 19) {
                    // Compatible with double in abnormal length
                    // Get the top 18 significant digits
                    value = 0;
                    cnt = 0;
                    int j = fromIndex, decimalPointIndex = endIndex;
                    decimalCount = 0;
                    for (; ; ++j) {
                        if (isDigit(ch = buf[j])) {
                            value = value * 10 + ch - 48;
                            ++cnt;
                            if (j > decimalPointIndex) {
                                ++decimalCount;
                            }
                        } else {
                            if (ch == '.') {
                                decimalPointIndex = j;
                            }
                        }
                        if (cnt == 18) break;
                    }
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
                    while (buf[endIndex] <= ' ') {
                        --endIndex;
                    }
                    return new BigDecimal(buf, fromIndex, endIndex - fromIndex + 1);
                }
                expValue = expNegative ? -expValue - decimalCount : expValue - decimalCount;
                double doubleVal = NumberUtils.scientificToIEEEDouble(value, -expValue);
//                double doubleVal = value;
//                if (expValue > 0) {
//                    double powValue = getDecimalPowerValue(expValue); // Math.pow(radix, expValue);
//                    doubleVal *= powValue;
//                } else if (expValue < 0) {
//                    double powValue = getDecimalPowerValue(-expValue);// Math.pow(radix, -expValue);
//                    doubleVal /= powValue;
//                }
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

        Number deserializeDefault0(byte[] buf, int fromIndex, int toIndex, int offset, long val, int cnt, boolean negative, byte endToken, int returnType, JSONParseContext jsonParseContext) throws Exception {
            long value = val;
            int decimalCount = 0;
            int expValue = 0;
            boolean expNegative = false;
            // init integer type
            int mode = 0;
            // number suffix
            int specifySuffix = 0;

            int i = offset;
            byte b, b1 = 0;
            do {
                boolean isDigit;
                while ((isDigit = isDigit((b = buf[i]))) && isDigit(b1 = buf[++i])) {
                    value = value * 100 + b * 10 + b1 - 528;
                    cnt = cnt + 2;
                    ++i;
                }
                if (isDigit) {
                    value = (value << 3) + (value << 1) + b - 48;
                    b = b1;
                    ++cnt;
                }
                if (b == '.') {
                    // 小数点模式
                    mode = 1;
                    // direct scan numbers
//                    while (isDigit((b = buf[++i]))) {
//                        value = (value << 3) + (value << 1) + b - 48;
//                        ++decimalCount;
//                        ++cnt;
//                    }
                    while ((isDigit = isDigit((b = buf[++i]))) && isDigit(b1 = buf[++i])) {
                        value = value * 100 + b * 10 + b1 - 528;
                        cnt = cnt + 2;
                        decimalCount = decimalCount + 2;
                    }
                    if (isDigit) {
                        value = (value << 3) + (value << 1) + b - 48;
                        b = b1;
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
                    if (isDigit(b)) {
                        expValue = b - 48;
                        while (isDigit(b = buf[++i])) {
                            expValue = (expValue << 3) + (expValue << 1) + b - 48;
                        }
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
            jsonParseContext.endIndex = endIndex;
            if (returnType == TYPE_DOUBLE) {
                if (cnt > 19) {
                    // Compatible with double in abnormal length
                    // Get the top 18 significant digits
                    value = 0;
                    cnt = 0;
                    int j = fromIndex, decimalPointIndex = endIndex;
                    decimalCount = 0;
                    for (; ; ++j) {
                        if (isDigit(b = buf[j])) {
                            value = value * 10 + b - 48;
                            ++cnt;
                            if (j > decimalPointIndex) {
                                ++decimalCount;
                            }
                        } else {
                            if (b == '.') {
                                decimalPointIndex = j;
                            }
                        }
                        if (cnt == 18) break;
                    }
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
                    while (buf[endIndex] <= ' ') {
                        --endIndex;
                    }
                    return new BigDecimal(new String(buf, fromIndex, endIndex - fromIndex + 1));
                }
                expValue = expNegative ? -expValue - decimalCount : expValue - decimalCount;
                double doubleVal = NumberUtils.scientificToIEEEDouble(value, -expValue);
//                double doubleVal = value;
//                expValue = expNegative ? -expValue - decimalCount : expValue - decimalCount;
//                if (expValue > 0) {
//                    double powValue = getDecimalPowerValue(expValue); // Math.pow(radix, expValue);
//                    doubleVal *= powValue;
//                } else if (expValue < 0) {
//                    double powValue = getDecimalPowerValue(-expValue);// Math.pow(radix, -expValue);
//                    doubleVal /= powValue;
//                }
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

        Number deserializeDouble(char[] buf, int fromIndex, char endToken, JSONParseContext jsonParseContext) {
            int i = fromIndex;
            char ch;
            while (true) {
                ch = buf[i + 1];
                if (ch == ',' || ch == endToken) {
                    break;
                }
                ++i;
            }
            jsonParseContext.endIndex = i;
            int endIndex = i + 1;
            while ((endIndex > fromIndex) && (buf[endIndex - 1] <= ' ')) {
                endIndex--;
            }
            int len = endIndex - fromIndex;
            return Double.parseDouble(new String(buf, fromIndex, len));
        }

//        Number deserializeDouble(byte[] bytes, int fromIndex, byte endToken, JSONParseContext jsonParseContext) {
//            int i = fromIndex;
//            byte b;
//            while (true) {
//                b = bytes[i + 1];
//                if (b == ',' || b == endToken) {
//                    break;
//                }
//                ++i;
//            }
//            jsonParseContext.endIndex = i;
//            int endIndex = i + 1;
//            while ((endIndex > fromIndex) && (bytes[endIndex - 1] <= ' ')) {
//                endIndex--;
//            }
//            int len = endIndex - fromIndex;
//            String value = new String(bytes, fromIndex, len);
//            return Double.parseDouble(value);
//        }

        protected long deserializeInteger(long value, boolean negative, int cnt, char[] buf, int fromIndex, int toIndex, char endToken, JSONParseContext jsonParseContext) throws Exception {
            int i = fromIndex;
            char ch, ch1 = 0;
            boolean isDigit;
            while ((isDigit = isDigit((ch = buf[i]))) && isDigit(ch1 = buf[++i])) {
                value = value * 100 + ch * 10 + ch1 - 528;
                ++i;
            }
            if (isDigit) {
                value = (value << 3) + (value << 1) + ch - 48;
                ch = ch1;
            }
            if (ch == COMMA || ch == endToken) {
                jsonParseContext.endIndex = i;
                jsonParseContext.endChar = ch;
                return negative ? -value : value;
            }
            if (ch <= WHITE_SPACE) {
                while ((ch = buf[++i]) <= WHITE_SPACE) ;
                if (ch == COMMA || ch == endToken) {
                    jsonParseContext.endIndex = i;
                    jsonParseContext.endChar = ch;
                    return negative ? -value : value;
                }
                String contextErrorAt = createErrorContextText(buf, i);
                throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + ch + "', context text by '" + contextErrorAt + "'");
            }
            // forward default deserialize
            return deserializeDefault0(buf, fromIndex, toIndex, i, value, cnt + i - fromIndex, negative, endToken, 0, jsonParseContext).longValue();
        }

        protected long deserializeInteger(long value, boolean negative, int cnt, byte[] buf, int fromIndex, int toIndex, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            int i = fromIndex;
            byte b, b1 = 0;
            boolean isDigit;
            while ((isDigit = isDigit((b = buf[i]))) && isDigit(b1 = buf[++i])) {
                value = value * 100 + b * 10 + b1 - 528;
                ++i;
            }
            if (isDigit) {
                value = (value << 3) + (value << 1) + b - 48;
                b = b1;
            }
            if (b == COMMA || b == endToken) {
                jsonParseContext.endIndex = i;
                jsonParseContext.endChar = b;
                return negative ? -value : value;
            }
            if (b <= WHITE_SPACE) {
                while ((b = buf[++i]) <= WHITE_SPACE) ;
                if (b == COMMA || b == endToken) {
                    jsonParseContext.endIndex = i;
                    jsonParseContext.endChar = b;
                    return negative ? -value : value;
                }
                String contextErrorAt = createErrorContextText(buf, i);
                throw new JSONException("For input string: \"" + new String(buf, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + (char) b + "', context text by '" + contextErrorAt + "'");
            }
            // forward default deserialize
            return deserializeDefault0(buf, fromIndex, toIndex, i, value, cnt + i - fromIndex, negative, endToken, 0, jsonParseContext).longValue();
        }

        protected Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, char endToken, JSONParseContext jsonParseContext) throws Exception {
            if (parameterizedType == GenericParameterizedType.AnyType || parameterizedType == null) {
                return deserializeDefault0(buf, fromIndex, toIndex, offset, initValue, cnt, negative, endToken, 0, jsonParseContext);
            }
            if (parameterizedType == GenericParameterizedType.BigDecimalType) {
                return NUMBER_BIGDECIMAL.deserializeNumber(initValue, negative, cnt, buf, fromIndex, offset, toIndex, parameterizedType, endToken, jsonParseContext);
            }
            Number value = deserializeDefault0(buf, fromIndex, toIndex, offset, initValue, cnt, negative, endToken, 0, jsonParseContext);
            return ObjectUtils.toTypeNumber(value, parameterizedType.getActualType());
        }

        protected Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            if (parameterizedType == GenericParameterizedType.AnyType || parameterizedType == null) {
                return deserializeDefault0(buf, fromIndex, toIndex, offset, initValue, cnt, negative, endToken, 0, jsonParseContext);
            }
            if (parameterizedType == GenericParameterizedType.BigDecimalType) {
                return NUMBER_BIGDECIMAL.deserializeNumber(initValue, negative, cnt, buf, fromIndex, offset, toIndex, parameterizedType, endToken, jsonParseContext);
            }
            // not supported
            Number value = deserializeDefault0(buf, fromIndex, toIndex, offset, initValue, cnt, negative, endToken, 0, jsonParseContext);
            return ObjectUtils.toTypeNumber(value, parameterizedType.getActualType());
        }

        static class LongDeserializer extends NumberDeserializer {
            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, char endToken, JSONParseContext jsonParseContext) throws Exception {
                return deserializeInteger(initValue, negative, cnt, buf, offset, toIndex, endToken, jsonParseContext);
            }

            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext jsonParseContext) throws Exception {
                return deserializeInteger(initValue, negative, cnt, buf, offset, toIndex, endToken, jsonParseContext);
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return Long.parseLong(value);
            }
        }

        static class IntegerDeserializer extends NumberDeserializer {
            protected final Integer deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, char endToken, JSONParseContext jsonParseContext) throws Exception {
                return (int) deserializeInteger(initValue, negative, cnt, buf, offset, toIndex, endToken, jsonParseContext);
            }

            protected final Integer deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext jsonParseContext) throws Exception {
                return (int) deserializeInteger(initValue, negative, cnt, buf, offset, toIndex, endToken, jsonParseContext);
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return Integer.parseInt(value);
            }
        }

        static class ShortDeserializer extends NumberDeserializer {
            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, char endToken, JSONParseContext jsonParseContext) throws Exception {
                return (short) deserializeInteger(initValue, negative, cnt, buf, offset, toIndex, endToken, jsonParseContext);
            }

            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext jsonParseContext) throws Exception {
                return (short) deserializeInteger(initValue, negative, cnt, buf, offset, toIndex, endToken, jsonParseContext);
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return Short.parseShort(value);
            }
        }

        static class ByteDeserializer extends NumberDeserializer {
            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, char endToken, JSONParseContext jsonParseContext) throws Exception {
                return (byte) deserializeInteger(initValue, negative, cnt, buf, offset, toIndex, endToken, jsonParseContext);
            }

            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext jsonParseContext) throws Exception {
                return (byte) deserializeInteger(initValue, negative, cnt, buf, offset, toIndex, endToken, jsonParseContext);
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return Byte.parseByte(value);
            }
        }

        static class DoubleDeserializer extends NumberDeserializer {
            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, char endToken, JSONParseContext jsonParseContext) throws Exception {
//                if (jsonParseContext.useJDKDoubleParser) {
//                    return deserializeDouble(buf, fromIndex, endToken, jsonParseContext);
//                }
                return deserializeDefault0(buf, fromIndex, toIndex, offset, initValue, cnt, negative, endToken, TYPE_DOUBLE, jsonParseContext).doubleValue();
            }

            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext jsonParseContext) throws Exception {
//                if (jsonParseContext.useJDKDoubleParser) {
//                    return deserializeDouble(buf, fromIndex, endToken, jsonParseContext);
//                }
                return deserializeDefault0(buf, fromIndex, toIndex, offset, initValue, cnt, negative, endToken, TYPE_DOUBLE, jsonParseContext).doubleValue();
            }
        }

        static class FloatDeserializer extends NumberDeserializer {
            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, char endToken, JSONParseContext jsonParseContext) throws Exception {
                return deserializeDefault0(buf, fromIndex, toIndex, offset, initValue, cnt, negative, endToken, TYPE_FLOAT, jsonParseContext).floatValue();
            }

            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext jsonParseContext) throws Exception {
                return deserializeDefault0(buf, fromIndex, toIndex, offset, initValue, cnt, negative, endToken, TYPE_FLOAT, jsonParseContext).floatValue();
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return Float.parseFloat(value);
            }
        }

        static class BigDecimalDeserializer extends NumberDeserializer {
            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, char endToken, JSONParseContext jsonParseContext) throws Exception {
                return deserializeDefault0(buf, fromIndex, toIndex, offset, initValue, cnt, negative, endToken, TYPE_BIGDECIMAL, jsonParseContext);
            }

            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext jsonParseContext) throws Exception {
                return deserializeDefault0(buf, fromIndex, toIndex, offset, initValue, cnt, negative, endToken, TYPE_BIGDECIMAL, jsonParseContext);
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return new BigDecimal(value);
            }
        }

        static class BigIntegerDeserializer extends NumberDeserializer {
            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, char[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, char endToken, JSONParseContext jsonParseContext) throws Exception {
                return deserializeDefault0(buf, fromIndex, toIndex, offset, initValue, cnt, negative, endToken, TYPE_BIGINTEGER, jsonParseContext);
            }

            protected final Number deserializeNumber(long initValue, boolean negative, int cnt, byte[] buf, int fromIndex, int offset, int toIndex, GenericParameterizedType parameterizedType, byte endToken, JSONParseContext jsonParseContext) throws Exception {
                return deserializeDefault0(buf, fromIndex, toIndex, offset, initValue, cnt, negative, endToken, TYPE_BIGINTEGER, jsonParseContext);
            }

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return new BigInteger(value);
            }
        }
    }

    // 2、boolean
    static class BooleanDeserializer extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return value.equals("true");
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            if (beginChar == 't') {
                return deserializeTrue(buf, fromIndex, toIndex, parameterizedType, jsonParseContext);
            } else if (beginChar == 'f') {
                return deserializeFalse(buf, fromIndex, toIndex, parameterizedType, jsonParseContext);
            } else {
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for Boolean Type, expected 't' or 'f'");
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            if (beginByte == 't') {
                return parseTrue(buf, fromIndex, toIndex, jsonParseContext);
            } else if (beginByte == 'f') {
                return parseFalse(buf, fromIndex, toIndex, jsonParseContext);
            } else {
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) beginByte + "' for Boolean Type, expected 't' or 'f'");
            }
        }

        Object deserializeTrue(char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) throws Exception {
            int endIndex = fromIndex + 3;
            if (UnsafeHelper.getLong(buf, fromIndex) == TRUE_LONG/*buf[fromIndex + 1] == 'r' && buf[fromIndex + 2] == 'u' && buf[endIndex] == 'e'*/) {
                jsonParseContext.endIndex = endIndex;
                return true;
            }
            int len = Math.min(toIndex - fromIndex + 1, 4);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'true' because it starts with 't', but found text '" + new String(buf, fromIndex, len) + "'");
        }

        Object deserializeFalse(char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) throws Exception {
            int endIndex = fromIndex + 4;
            if (UnsafeHelper.getLong(buf, fromIndex + 1) == ALSE_LONG/*buf[fromIndex + 1] == 'a'
                    && buf[fromIndex + 2] == 'l'
                    && buf[fromIndex + 3] == 's'
                    && buf[endIndex] == 'e'*/) {
                jsonParseContext.endIndex = endIndex;
                return false;
            }
            int len = Math.min(toIndex - fromIndex + 1, 5);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'false' because it starts with 'f', but found text '" + new String(buf, fromIndex, len) + "'");
        }
    }

    // 3、日期
    static class DateDeserializer extends JSONTypeDeserializer {

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
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '"': {
                    CHAR_SEQUENCE.skip(charSource, buf, fromIndex, beginChar, jsonParseContext);
                    int endStringIndex = jsonParseContext.endIndex;
                    Class<? extends Date> dateCls = (Class<? extends Date>) parameterizedType.getActualType();
                    return deserializeDate(buf, fromIndex, endStringIndex + 1, dateCls);
                }
                case 'n': {
                    return NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
                }
                case '{': {
                    return OBJECT.deserializeObject(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
                }
                default: {
                    // long
                    long timestamp = (Long) NUMBER_LONG.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.LongType, null, endToken, jsonParseContext);
                    return parseDate(timestamp, (Class<? extends Date>) parameterizedType.getActualType());
                }
            }
        }

        // 通过字节数组解析
        protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = bytes[fromIndex];
            switch (beginByte) {
                case '\'':
                case '"': {
                    CHAR_SEQUENCE.skip(charSource, bytes, fromIndex, beginByte, jsonParseContext);
                    int endStringIndex = jsonParseContext.endIndex;
                    Class<? extends Date> dateCls = (Class<? extends Date>) parameterizedType.getActualType();
                    return deserializeDate(bytes, fromIndex, endStringIndex + 1, dateCls);
                }
                case 'n': {
                    return parseNull(bytes, fromIndex, toIndex, jsonParseContext);
                }
                case '{': {
                    return OBJECT.deserializeObject(charSource, bytes, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
                }
                default: {
                    // long
                    long timestamp = (Long) NUMBER_LONG.deserialize(charSource, bytes, fromIndex, toIndex, GenericParameterizedType.LongType, null, endToken, jsonParseContext);
                    return parseDate(timestamp, (Class<? extends Date>) parameterizedType.getActualType());
                }
            }
        }

        static class DateInstanceDeserializer extends DateDeserializer {

            String pattern;
            int patternType;
            DateTemplate dateTemplate;
            String timezone;

            public DateInstanceDeserializer(GenericParameterizedType genericParameterizedType, JsonProperty property) {
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

    // 4、枚举
    static class EnumDeserializer extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            Class enumCls = actualType;
            return Enum.valueOf(enumCls, value);
        }

        protected Enum deserializeEnumName(CharSource charSource, char[] buf, int fromIndex, int toIndex, Class enumCls, JSONParseContext jsonParseContext) throws Exception {
            String name = (String) CHAR_SEQUENCE.deserializeString(charSource, buf, fromIndex, toIndex, '"', GenericParameterizedType.StringType, jsonParseContext);
            try {
                return Enum.valueOf(enumCls, name);
            } catch (RuntimeException exception) {
                if (jsonParseContext.unknownEnumAsNull) {
                    return null;
                } else {
                    throw exception;
                }
            }
        }

        protected Enum deserializeEnumName(CharSource charSource, byte[] buf, int fromIndex, int toIndex, Class enumCls, JSONParseContext jsonParseContext) throws Exception {
            String name = (String) CHAR_SEQUENCE.deserializeString(charSource, buf, fromIndex, toIndex, '"', GenericParameterizedType.StringType, jsonParseContext);
            try {
                return Enum.valueOf(enumCls, name);
            } catch (RuntimeException exception) {
                if (jsonParseContext.unknownEnumAsNull) {
                    return null;
                } else {
                    throw exception;
                }
            }
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            Class clazz = parameterizedType.getActualType();

            if (beginChar == '"') {
                return deserializeEnumName(charSource, buf, fromIndex, toIndex, clazz, jsonParseContext);
            } else if (beginChar == 'n') {
                return NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
            } else {
                // number
                Integer ordinal = (Integer) NUMBER_INTEGER.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.IntType, null, endToken, jsonParseContext);
                Enum[] values = (Enum[]) getEnumConstants(clazz);
                if (values != null && ordinal < values.length)
                    return values[ordinal];
                throw new JSONException("Syntax error, at pos " + fromIndex + ", ordinal " + ordinal + " cannot convert to Enum " + clazz + "");
            }
        }

        protected Object getEnumConstants(Class clazz) {
            return clazz.getEnumConstants();
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte b = buf[fromIndex];
            Class clazz = parameterizedType.getActualType();

            if (b == '"') {
                return deserializeEnumName(charSource, buf, fromIndex, toIndex, clazz, jsonParseContext);
            } else if (b == 'n') {
                return NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
            } else {
                // number
                Integer ordinal = (Integer) NUMBER_INTEGER.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.IntType, null, endToken, jsonParseContext);
                Enum[] values = (Enum[]) clazz.getEnumConstants();
                if (values != null && ordinal < values.length)
                    return values[ordinal];
                throw new JSONException("Syntax error, at pos " + fromIndex + ", ordinal " + ordinal + " cannot convert to Enum " + clazz + "");
            }
        }

        static class EnumInstanceDeserializer extends EnumDeserializer {

            private final Enum[] values;
            protected final JSONValueMatcher<Enum> enumValueMatcher;

            public EnumInstanceDeserializer(Enum[] values, JSONValueMatcher<Enum> enumValueMatcher) {
                this.values = values;
                this.enumValueMatcher = enumValueMatcher;
            }

            @Override
            protected Object getEnumConstants(Class clazz) {
                return values;
            }

//            protected Enum getEnumValue(char[] buf, int begin, int end, long hashValue) {
//                if (enumValueMapUseChars.isCollision()) {
//                    return enumValueMapUseChars.getValue(buf, begin, end, hashValue);
//                } else {
//                    return enumValueMapUseChars.getValueByHash(hashValue);
//                }
//            }
//
//            protected Enum getEnumValue(byte[] buf, int begin, int end, long hashValue) {
//                if (enumValueMapUseBytes.isCollision()) {
//                    return enumValueMapUseBytes.getValue(buf, begin, end, hashValue);
//                } else {
//                    return enumValueMapUseBytes.getValueByHash(hashValue);
//                }
//            }
//
//            protected long hashEnumName(char[] buf, int i, JSONParseContext jsonParseContext) {
//                long hashValue = 0;
//                char ch;
//                char ch1;
//                if ((ch = buf[i]) != DOUBLE_QUOTATION && (ch1 = buf[++i]) != DOUBLE_QUOTATION) {
//                    hashValue = enumValueMapUseBytes.hash(hashValue, ch, ch1);
//                    if ((ch = buf[++i]) != DOUBLE_QUOTATION && (ch1 = buf[++i]) != DOUBLE_QUOTATION) {
//                        hashValue = enumValueMapUseBytes.hash(hashValue, ch, ch1);
//                        while ((ch = buf[++i]) != DOUBLE_QUOTATION && (ch1 = buf[++i]) != DOUBLE_QUOTATION) {
//                            hashValue = enumValueMapUseBytes.hash(hashValue, ch, ch1);
//                        }
//                    }
//                }
//                if (ch != DOUBLE_QUOTATION) {
//                    hashValue = enumValueMapUseBytes.hash(hashValue, ch);
//                }
//                jsonParseContext.endIndex = i;
//                return hashValue;
//            }

            @Override
            protected Enum deserializeEnumName(CharSource charSource, char[] buf, int fromIndex, int toIndex, Class enumCls, JSONParseContext jsonParseContext) throws Exception {
                int begin = fromIndex + 1, i = begin;
                Enum value = enumValueMatcher.matchValue(charSource, buf, i, '"', jsonParseContext);
                i = jsonParseContext.endIndex;
                if (value == null) {
                    if (buf[i - 1] == '\\') {
                        // skip
                        char ch, prev = 0;
                        while (((ch = buf[++i]) != '"' || prev == '\\')) {
                            prev = ch;
                        }
                    }
                    jsonParseContext.endIndex = i;
                    if (jsonParseContext.unknownEnumAsNull) {
                        return null;
                    }
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unknown Enum name '" + new String(buf, fromIndex + 1, i - fromIndex - 1) + "' of EnumType " + enumCls);
                }
                jsonParseContext.endIndex = i;
                return value;
            }

//            protected long hashEnumName(byte[] buf, int i, JSONParseContext jsonParseContext) {
//                long hashValue = 0;
//                byte b;
//                byte b1;
//                if ((b = buf[i]) != DOUBLE_QUOTATION && (b1 = buf[++i]) != DOUBLE_QUOTATION) {
//                    hashValue = enumValueMapUseBytes.hash(hashValue, b, b1);
//                    if ((b = buf[++i]) != DOUBLE_QUOTATION && (b1 = buf[++i]) != DOUBLE_QUOTATION) {
//                        hashValue = enumValueMapUseBytes.hash(hashValue, b, b1);
//                        while ((b = buf[++i]) != DOUBLE_QUOTATION && (b1 = buf[++i]) != DOUBLE_QUOTATION) {
//                            hashValue = enumValueMapUseBytes.hash(hashValue, b, b1);
//                        }
//                    }
//                }
//                if (b != DOUBLE_QUOTATION) {
//                    hashValue = enumValueMapUseBytes.hash(hashValue, b);
//                }
//                jsonParseContext.endIndex = i;
//                return hashValue;
//            }

            @Override
            protected Enum deserializeEnumName(CharSource charSource, byte[] buf, int fromIndex, int toIndex, Class enumCls, JSONParseContext jsonParseContext) throws Exception {
                int begin = fromIndex + 1, i = begin;
                Enum value = enumValueMatcher.matchValue(charSource, buf, i, '"', jsonParseContext);
                i = jsonParseContext.endIndex;
                if (value == null) {
                    if (buf[i - 1] == ESCAPE) {
                        // skip
                        byte b, prev = 0;
                        while (((b = buf[++i]) != DOUBLE_QUOTATION || prev == ESCAPE)) {
                            prev = b;
                        }
                    }
                    jsonParseContext.endIndex = i;
                    if (jsonParseContext.unknownEnumAsNull) {
                        return null;
                    }
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unknown Enum name '" + new String(buf, fromIndex + 1, i - fromIndex - 1) + "' of EnumType " + enumCls);
                }
                jsonParseContext.endIndex = i;
                return value;
            }
        }
    }

    // 4、Class
    static class ClassDeserializer extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) throws ClassNotFoundException {
            return Class.forName(value);
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            if (beginChar == '"') {
                String name = (String) CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, toIndex, beginChar, GenericParameterizedType.StringType, jsonParseContext);
                return Class.forName(name);
            }
            if (beginChar == 'n') {
                return NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
            }
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for Class Type, expected '\"' ");
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            if (beginByte == '"') {
                String name = (String) CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, toIndex, beginByte, GenericParameterizedType.StringType, jsonParseContext);
                return Class.forName(name);
            }
            if (beginByte == 'n') {
                return parseNull(buf, fromIndex, toIndex, jsonParseContext);
            }
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) beginByte + "' for Class Type, expected '\"' ");
        }
    }

    // 6、Annotation
    static class AnnotationDeserializer extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return null;
        }

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

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return value.getBytes();
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {

            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case 'n': {
                    return NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
                }
                case '"': {
                    // String
                    CHAR_SEQUENCE.skip(charSource, buf, fromIndex, beginChar, jsonParseContext);
                    int endStringIndex = jsonParseContext.endIndex;
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
                    return parseNull(buf, fromIndex, toIndex, jsonParseContext);
                }
                case '\'':
                case '"': {
                    // String
                    CHAR_SEQUENCE.skip(charSource, buf, fromIndex, beginChar, jsonParseContext);
                    int endStringIndex = jsonParseContext.endIndex;
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
            if (jsonParseContext.byteArrayFromHexString) {
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
            if (jsonParseContext.byteArrayFromHexString) {
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

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return value.toCharArray();
        }

        Object deserializeArray(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            char ch = '\0';

            Class arrayCls = parameterizedType.getActualType();
            GenericParameterizedType valueType = parameterizedType.getValueType();
            JSONTypeDeserializer valueDeserializer = null;
            Class<?> elementCls = null;
            if (valueType == null) {
                elementCls = arrayCls.getComponentType();
                valueType = GenericParameterizedType.actualType(elementCls);
                valueDeserializer = getTypeDeserializer(elementCls);
            } else {
                elementCls = valueType.getActualType();
                valueDeserializer = getTypeDeserializer(elementCls);
            }

            ArrayList<Object> collection = new ArrayList<Object>(5);

            // 允许注释
            boolean allowComment = jsonParseContext.allowComment;

            // 集合数组核心token是逗号（The core token of the collection array is a comma）
            // for loop
            for (int i = beginIndex; ; ++i) {
                // clear white space characters
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (ch == ']') {
                    if (collection.size() > 0 && !jsonParseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    jsonParseContext.endIndex = i;
                    return collectionToArray(collection, elementCls);
                }

                Object value = valueDeserializer.deserialize(charSource, buf, i, toIndex, valueType, null, ']', jsonParseContext);
                collection.add(value);
                i = jsonParseContext.endIndex;
                int endChar = jsonParseContext.endChar;
                if (endChar == 0) {
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                } else {
                    ch = (char) endChar;
                    jsonParseContext.endChar = 0;
                }
                if (ch == ',') {
                    continue;
                }
                if (ch == ']') {
                    jsonParseContext.endIndex = i;
                    return collectionToArray(collection, elementCls);
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or ']'");
            }
        }

        Object deserializeArray(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            byte b = '\0';

            Class arrayCls = parameterizedType.getActualType();
            GenericParameterizedType valueType = parameterizedType.getValueType();

            Class<?> elementCls = null;
            JSONTypeDeserializer valueDeserializer = null;
            if (valueType == null) {
                elementCls = arrayCls.getComponentType();
                valueType = GenericParameterizedType.actualType(elementCls);
                valueDeserializer = getTypeDeserializer(elementCls);
            } else {
                elementCls = valueType.getActualType();
                valueDeserializer = getTypeDeserializer(elementCls);
            }

            ArrayList<Object> collection = new ArrayList<Object>(5);

            // 允许注释
            boolean allowComment = jsonParseContext.allowComment;

            // 集合数组核心token是逗号（The core token of the collection array is a comma）
            // for loop
            for (int i = beginIndex; ; ++i) {
                // clear white space characters
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (b == ']') {
                    if (collection.size() > 0 && !jsonParseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    jsonParseContext.endIndex = i;
                    return collectionToArray(collection, elementCls);
                }

                Object value = valueDeserializer.deserialize(charSource, buf, i, toIndex, valueType, null, END_ARRAY, jsonParseContext);
                collection.add(value);
                i = jsonParseContext.endIndex;
                int endChar = jsonParseContext.endChar;
                if (endChar == 0) {
                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                } else {
                    b = (byte) endChar;
                    jsonParseContext.endChar = 0;
                }
                // 检查下一个字符是逗号还是结束符号。如果是，继续或者终止。如果不是，则抛出异常
                // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
                if (b == ',') {
                    continue;
                }
                if (b == ']') {
                    jsonParseContext.endIndex = i;
                    return collectionToArray(collection, elementCls);
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or ']'");
            }
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            if (beginChar == '[') {
                return deserializeArray(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
            } else if (beginChar == 'n') {
                return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, '\0', jsonParseContext);
            } else {
                if (jsonParseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                    jsonParseContext.endIndex = fromIndex + 1;
                    return null;
                }
                // not support or custom handle ?
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for collection type ");
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            if (beginByte == '[') {
                return deserializeArray(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
            } else if (beginByte == 'n') {
                return parseNull(buf, fromIndex, toIndex, jsonParseContext);
            } else {
                char beginChar = (char) beginByte;
                if (jsonParseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                    jsonParseContext.endIndex = fromIndex + 1;
                    return null;
                }
                // not support or custom handle ?
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for Collection Type ");
            }
        }

        static abstract class ArrayInstanceDeserializer extends ArrayDeserializer {

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
            Object deserializeArray(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
                int beginIndex = fromIndex + 1;
                char ch;
                Object arr = initArray(jsonParseContext);
                int size = size(arr);
                int len = 0;
                boolean allowComment = jsonParseContext.allowComment;
                for (int i = beginIndex; ; ++i) {
                    // clear white space characters
                    while ((ch = buf[i]) <= ' ') {
                        ++i;
                    }
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (ch == ']') {
                        if (len > 0 && !jsonParseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                        }
                        jsonParseContext.endIndex = i;
                        return len == 0 ? empty() : subOf(arr, len);
                    }

                    Object value = getValueDeserializer().deserialize(charSource, buf, i, toIndex, null, null, ']', jsonParseContext);
                    if (len >= size) {
                        arr = copyOf(arr, size = (size << 1));
                    }
                    setElementAt(arr, value, len++);
                    i = jsonParseContext.endIndex;
                    int endChar = jsonParseContext.endChar;
                    if (endChar == 0) {
                        while ((ch = buf[++i]) <= ' ') ;
                        if (allowComment) {
                            if (ch == '/') {
                                ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                            }
                        }
                    } else {
                        ch = (char) endChar;
                        jsonParseContext.endChar = 0;
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == ']') {
                        jsonParseContext.endIndex = i;
                        return subOf(arr, len);
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or ']'");
                }
            }

            @Override
            Object deserializeArray(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
                int beginIndex = fromIndex + 1;
                byte b;
                Object arr = initArray(jsonParseContext);
                int size = size(arr);
                int len = 0;
                boolean allowComment = jsonParseContext.allowComment;
                for (int i = beginIndex; ; ++i) {
                    // clear white space characters
                    while ((b = buf[i]) <= ' ') {
                        ++i;
                    }
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (b == ']') {
                        if (len > 0 && !jsonParseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                        }
                        jsonParseContext.endIndex = i;
                        return len == 0 ? empty() : subOf(arr, len);
                    }

                    Object value = getValueDeserializer().deserialize(charSource, buf, i, toIndex, null, null, END_ARRAY, jsonParseContext);
                    if (len >= size) {
                        arr = copyOf(arr, size = (size << 1));
                    }
                    setElementAt(arr, value, len++);

                    i = jsonParseContext.endIndex;
                    int endChar = jsonParseContext.endChar;
                    if (endChar == 0) {
                        while ((b = buf[++i]) <= WHITE_SPACE) ;
                        if (allowComment) {
                            if (b == '/') {
                                b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                            }
                        }
                    } else {
                        b = (byte) endChar;
                        jsonParseContext.endChar = 0;
                    }
                    if (b == ',') {
                        continue;
                    }
                    if (b == END_ARRAY) {
                        jsonParseContext.endIndex = i;
                        return subOf(arr, len);
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or ']'");
                }
            }
        }

        final static class StringArrayDeserializer extends ArrayInstanceDeserializer {

            @Override
            protected Object valueOf(String value, Class<?> actualType) {
                return new String[]{value};
            }

            @Override
            Object deserializeArray(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
                int beginIndex = fromIndex + 1;
                char ch;
                String[] arr = jsonParseContext.getContextStrings();
                int size = arr.length;
                int len = 0;
                boolean allowComment = jsonParseContext.allowComment;
                for (int i = beginIndex; ; ++i) {
                    while ((ch = buf[i]) <= ' ') {
                        ++i;
                    }
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (ch == ']') {
                        if (len > 0 && !jsonParseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                        }
                        jsonParseContext.endIndex = i;
                        return len == 0 ? EMPTY_STRINGS : MemoryOptimizerUtils.copyOfRange(arr, 0, len);
                    }

                    String value = (String) CHAR_SEQUENCE_STRING.deserialize(charSource, buf, i, toIndex, null, null, ']', jsonParseContext);
                    if (len >= size) {
                        arr = Arrays.copyOf(arr, size = (size << 1));
                    }
                    arr[len++] = value;
                    i = jsonParseContext.endIndex;
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == ']') {
                        jsonParseContext.endIndex = i;
                        return MemoryOptimizerUtils.copyOfRange(arr, 0, len);
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or ']'");
                }
            }

            @Override
            Object deserializeArray(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
                int beginIndex = fromIndex + 1;
                byte b;
                String[] arr = jsonParseContext.getContextStrings();
                int size = arr.length;
                int len = 0;
                boolean allowComment = jsonParseContext.allowComment;
                for (int i = beginIndex; ; ++i) {
                    // clear white space characters
                    while ((b = buf[i]) <= ' ') {
                        ++i;
                    }
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (b == ']') {
                        if (len > 0 && !jsonParseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                        }
                        jsonParseContext.endIndex = i;
                        return len == 0 ? EMPTY_STRINGS : MemoryOptimizerUtils.copyOfRange(arr, 0, len);
                    }

                    String value = (String) CHAR_SEQUENCE_STRING.deserialize(charSource, buf, i, toIndex, null, null, END_ARRAY, jsonParseContext);
                    if (len >= size) {
                        arr = Arrays.copyOf(arr, size = (size << 1));
                    }
                    arr[len++] = value;

                    i = jsonParseContext.endIndex;
                    while ((b = buf[++i]) <= WHITE_SPACE) ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (b == ',') {
                        continue;
                    }
                    if (b == END_ARRAY) {
                        jsonParseContext.endIndex = i;
                        return MemoryOptimizerUtils.copyOfRange(arr, 0, len);
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or ']'");
                }
            }
        }

        static class DoubleArrayDeserializer extends ArrayInstanceDeserializer {
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

        static class PrimitiveDoubleArrayDeserializer extends ArrayInstanceDeserializer {
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
                return MemoryOptimizerUtils.copyOfRange(value, 0, len);
            }

            @Override
            public JSONTypeDeserializer getValueDeserializer() {
                return NUMBER_DOUBLE;
            }
        }

        static class LongArrayDeserializer extends ArrayInstanceDeserializer {
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

        static class PrimitiveLongArrayDeserializer extends ArrayInstanceDeserializer {
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
                return MemoryOptimizerUtils.copyOfRange(value, 0, len);
            }

            @Override
            public JSONTypeDeserializer getValueDeserializer() {
                return NUMBER_LONG;
            }
        }

        static class FloatArrayDeserializer extends ArrayInstanceDeserializer {
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

        static class PrimitiveFloatArrayDeserializer extends ArrayInstanceDeserializer {
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

        static class IntArrayDeserializer extends ArrayInstanceDeserializer {
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

            @Override
            public JSONTypeDeserializer getValueDeserializer() {
                return NUMBER_INTEGER;
            }
        }

        static class PrimitiveIntArrayDeserializer extends ArrayInstanceDeserializer {
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

        static class ByteArrayDeserializer extends ArrayInstanceDeserializer {
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
    static class CollectionDeserializer extends JSONTypeDeserializer {

        void skip(CharSource charSource, char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            char ch;
            int size = 0;
            boolean allowComment = jsonParseContext.allowComment;
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
                    if (size > 0 && !jsonParseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    jsonParseContext.endIndex = i;
                    return;
                }
                ++size;
                ANY.skip(charSource, buf, i, toIndex, ']', jsonParseContext);
                i = jsonParseContext.endIndex;

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
                    jsonParseContext.endIndex = i;
                    return;
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or ']'");
            }
            throw new JSONException("Syntax error, cannot find closing symbol ']' matching '['");
        }

        void skip(CharSource charSource, byte[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
            int beginIndex = fromIndex + 1;
            byte b;
            int size = 0;
            boolean allowComment = jsonParseContext.allowComment;
            for (int i = beginIndex; i < toIndex; ++i) {
                // clear white space characters
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // 如果提前遇到字符']'，说明是空集合
                if (b == ']') {
                    if (size > 0 && !jsonParseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    jsonParseContext.endIndex = i;
                    return;
                }
                ++size;
                ANY.skip(charSource, buf, i, toIndex, END_ARRAY, jsonParseContext);
                i = jsonParseContext.endIndex;

                // 清除空白字符（clear white space characters）
                while ((b = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // 检查下一个字符是逗号还是结束符号。如果是，继续或者终止。如果不是，则抛出异常
                if (b == ',') {
                    continue;
                }
                if (b == ']') {
                    jsonParseContext.endIndex = i;
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
                return new ArrayList(4);
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
            boolean allowComment = jsonParseContext.allowComment;
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
                if (ch == ']') {
                    if (collection.size() > 0 && !jsonParseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    jsonParseContext.endIndex = i;
                    return collection;
                }

                Object value = valueDeserializer.deserialize(charSource, buf, i, toIndex, valueGenType, null, ']', jsonParseContext);
                collection.add(value);
                i = jsonParseContext.endIndex;
                int endChar = jsonParseContext.endChar;
                if (endChar == 0) {
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                } else {
                    ch = (char) endChar;
                    jsonParseContext.endChar = 0;
                }
                if (ch == ',') {
                    continue;
                }
                if (ch == ']') {
                    jsonParseContext.endIndex = i;
                    return collection;
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or ']'");
            }
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
            boolean allowComment = jsonParseContext.allowComment;

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
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                if (b == ']') {
                    if (collection.size() > 0 && !jsonParseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                    }
                    jsonParseContext.endIndex = i;
                    return collection;
                }

                Object value = valueDeserializer.deserialize(charSource, buf, i, toIndex, valueGenType, null, END_ARRAY, jsonParseContext);
                collection.add(value);
                i = jsonParseContext.endIndex;
                int endChar = jsonParseContext.endChar;
                if (endChar == 0) {
                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                } else {
                    b = (byte) endChar;
                    jsonParseContext.endChar = 0;
                }
                // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
                if (b == ',') {
                    continue;
                }
                if (b == ']') {
                    jsonParseContext.endIndex = i;
                    return collection;
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or ']'");
            }
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            if (beginChar == '[') {
                return deserializeCollection(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
            } else if (beginChar == 'n') {
                return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
            } else {
                if (jsonParseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                    jsonParseContext.endIndex = fromIndex + 1;
                    return null;
                }
                // not support or custom handle ?
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', expected token character '[', but found '" + beginChar + "' for Collection Type ");
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            if (beginByte == '[') {
                return deserializeCollection(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
            }
            if (beginByte == 'n') {
                return parseNull(buf, fromIndex, toIndex, jsonParseContext);
            }
            if (jsonParseContext.unMatchedEmptyAsNull && beginByte == DOUBLE_QUOTATION && buf[fromIndex + 1] == DOUBLE_QUOTATION) {
                jsonParseContext.endIndex = fromIndex + 1;
                return null;
            }
            // not support or custom handle ?
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', expected token character '[', but found '" + (char) beginByte + "' for Collection Type ");
        }

        static class CollectionInstanceDeserializer extends CollectionDeserializer {

            protected final GenericParameterizedType parameterizedType;
            protected final GenericParameterizedType valueType;
            protected final JSONTypeDeserializer valueDeserializer;
            private final Class<? extends Collection> constructionClass;

            CollectionInstanceDeserializer(GenericParameterizedType genericParameterizedType) {
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
        static class ArrayListDeserializer extends CollectionInstanceDeserializer {
            ArrayListDeserializer(GenericParameterizedType genericParameterizedType) {
                super(genericParameterizedType);
            }

            Collection deserializeCollection(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
                // 允许注释
                final boolean allowComment = jsonParseContext.allowComment;
                char ch;
                final ArrayList collection = new ArrayList(5);
                int i = fromIndex;
                for (; ; ) {
                    // clear white space characters
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (ch == ']') {
                        if (collection.size() > 0 && !jsonParseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                        }
                        jsonParseContext.endIndex = i;
                        return collection;
                    }

                    Object value = valueDeserializer.deserialize(charSource, buf, i, toIndex, valueType, null, ']', jsonParseContext);
                    collection.add(value);
                    i = jsonParseContext.endIndex;
                    int endChar = jsonParseContext.endChar;
                    if (endChar == 0) {
                        while ((ch = buf[++i]) <= ' ') ;
                        // clear white space characters
                        if (allowComment) {
                            if (ch == '/') {
                                ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                            }
                        }
                    } else {
                        ch = (char) endChar;
                        jsonParseContext.endChar = 0;
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == ']') {
                        jsonParseContext.endIndex = i;
                        return collection;
                    }
                    String errorContextTextAt = createErrorContextText(buf, i);
                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or ']'");
                }
            }

            Collection deserializeCollection(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
                final boolean allowComment = jsonParseContext.allowComment;
                byte b;
                final ArrayList collection = new ArrayList(5);
                int i = fromIndex;
                for (; ; ) {
                    // clear white space characters
                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (b == ']') {
                        if (collection.size() > 0 && !jsonParseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol ']' is not allowed here.");
                        }
                        jsonParseContext.endIndex = i;
                        return collection;
                    }

                    Object value = valueDeserializer.deserialize(charSource, buf, i, toIndex, valueType, null, END_ARRAY, jsonParseContext);
                    collection.add(value);
                    i = jsonParseContext.endIndex;
                    int endChar = jsonParseContext.endChar;
                    if (endChar == 0) {
                        // clear white space characters
                        while ((b = buf[++i]) <= WHITE_SPACE) ;
                        if (allowComment) {
                            if (b == '/') {
                                b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                            }
                        }
                    } else {
                        b = (byte) endChar;
                        jsonParseContext.endChar = 0;
                    }
                    if (b == COMMA) {
                        continue;
                    }
                    if (b == END_ARRAY) {
                        jsonParseContext.endIndex = i;
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
        static class HashSetDeserializer extends CollectionInstanceDeserializer {
            HashSetDeserializer(GenericParameterizedType genericParameterizedType) {
                super(genericParameterizedType);
            }

            @Override
            protected final Collection createCollection(GenericParameterizedType parameterizedType) throws Exception {
                return new HashSet(5);
            }
        }
    }

    // 10、Map
    static class MapDeserializer extends JSONTypeDeserializer {

        public static JSONTypeDeserializer hashtable() {
            return new MapDeserializer() {
                @Override
                Map createMap(GenericParameterizedType parameterizedType) {
                    return new Hashtable();
                }
            };
        }

        void skip(CharSource charSource, char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
            boolean empty = true;
            char ch;
            for (int i = fromIndex + 1; i < toIndex; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (jsonParseContext.allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (ch == '"') {
//                    while (i + 1 < toIndex && ((ch = buf[++i]) != '"' || buf[i - 1] == '\\')) ;
                    CHAR_SEQUENCE.skip(charSource, buf, i, '"', jsonParseContext);
                    i = jsonParseContext.endIndex;
                    ch = buf[i];

                    empty = false;
                    if (ch != '"') {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, util pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '\"' is not found ");
                    }
                    ++i;
                } else {
                    if (ch == '}') {
                        if (!empty && !jsonParseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                        }
                        jsonParseContext.endIndex = i;
                        return;
                    }
                    if (ch == '\'') {
                        if (jsonParseContext.allowSingleQuotes) {
//                            while (i + 1 < toIndex && buf[++i] != '\'') ;
                            CHAR_SEQUENCE.skip(charSource, buf, i, '\'', jsonParseContext);
                            i = jsonParseContext.endIndex + 1;
                            empty = false;
                        } else {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (jsonParseContext.allowUnquotedFieldNames) {
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
                if (jsonParseContext.allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (ch == ':') {
                    while ((ch = buf[++i]) <= ' ') ;
                    if (jsonParseContext.allowComment) {
                        if (ch == '/') {
                            i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext);
                        }
                    }
                    ANY.skip(charSource, buf, i, toIndex, '}', jsonParseContext);
                    i = jsonParseContext.endIndex;
                    while ((ch = buf[++i]) <= ' ') ;
                    if (jsonParseContext.allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == '}') {
                        jsonParseContext.endIndex = i;
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

        void skip(CharSource charSource, byte[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
            boolean empty = true;
            byte b;
            for (int i = fromIndex + 1; i < toIndex; ++i) {
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (jsonParseContext.allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (b == '"') {
//                    while (i + 1 < toIndex && ((b = buf[++i]) != '"' || buf[i - 1] == '\\')) ;
                    CHAR_SEQUENCE.skip(charSource, buf, i, '"', jsonParseContext);
                    i = jsonParseContext.endIndex;
                    b = buf[i];

                    empty = false;
                    if (b != '"') {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, util pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '\"' is not found ");
                    }
                    ++i;
                } else {
                    if (b == '}') {
                        if (!empty && !jsonParseContext.allowLastEndComma) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                        }
                        jsonParseContext.endIndex = i;
                        return;
                    }
                    if (b == '\'') {
                        if (jsonParseContext.allowSingleQuotes) {
//                            while (i + 1 < toIndex && buf[++i] != '\'') ;
//                            ++i;
                            CHAR_SEQUENCE.skip(charSource, buf, i, '\'', jsonParseContext);
                            i = jsonParseContext.endIndex + 1;
                            empty = false;
                        } else {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (jsonParseContext.allowUnquotedFieldNames) {
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
                if (jsonParseContext.allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (b == ':') {
                    while ((b = buf[++i]) <= ' ') ;
                    if (jsonParseContext.allowComment) {
                        if (b == '/') {
                            i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext);
                        }
                    }
                    ANY.skip(charSource, buf, i, toIndex, END_OBJECT, jsonParseContext);
                    i = jsonParseContext.endIndex;
                    while ((b = buf[++i]) <= ' ') ;
                    if (jsonParseContext.allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                    if (b == ',') {
                        continue;
                    }
                    if (b == '}') {
                        jsonParseContext.endIndex = i;
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

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            if (beginChar == '{') {
                return deserializeMap(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
            }
            if (beginChar == 'n') {
                return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
            }
            if (jsonParseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                jsonParseContext.endIndex = fromIndex + 1;
                return null;
            }
            // not support or custom handle ?
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for Map Type, expected '{' ");
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            char beginChar = (char) beginByte;
            if (beginChar == '{') {
                return deserializeMap(charSource, buf, fromIndex, toIndex, parameterizedType, instance, jsonParseContext);
            }
            if (beginChar == 'n') {
                return parseNull(buf, fromIndex, toIndex, jsonParseContext);
            }
            if (jsonParseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                jsonParseContext.endIndex = fromIndex + 1;
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
            return valueType == null ? ANY : JSONTypeDeserializer.getTypeDeserializer(valueType.getActualType());
        }

        Object deserializeMap(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object obj, JSONParseContext jsonParseContext) throws Exception {
            Map instance;
            if (obj != null) {
                instance = (Map) obj;
            } else {
                instance = createMap(parameterizedType);
            }

            boolean empty = true;
            char ch;
            boolean disableCacheMapKey = jsonParseContext.disableCacheMapKey;
            boolean allowComment = jsonParseContext.allowComment;
            for (int i = fromIndex + 1; ; ++i) {
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
                    mapKey = disableCacheMapKey ? (String) CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, i, toIndex, '"', GenericParameterizedType.StringType, jsonParseContext) : JSONDefaultParser.parseMapKeyByCache(buf, i, toIndex, '"', jsonParseContext);
                    i = jsonParseContext.endIndex;
                    empty = false;
                    ++i;
                } else {
                    // empty object or exception
                    if (ch == '}') {
                        if (!empty && !jsonParseContext.allowLastEndComma) {
                            throw new JSONException("Syntax error, at pos " + i + ", the closing symbol '}' is not allowed here.");
                        }
                        jsonParseContext.endIndex = i;
                        return instance;
                    }
                    if (ch == '\'') {
                        if (jsonParseContext.allowSingleQuotes) {
                            while (i + 1 < toIndex && (buf[++i] != '\'' || buf[i - 1] == '\\')) ;
                            empty = false;
                            ++i;
                            mapKey = parseKeyOfMap(buf, fieldKeyFrom, i, false);
                        } else {
                            throw new JSONException("Syntax error, at pos " + i + ", the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (jsonParseContext.allowUnquotedFieldNames) {
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
                    JSONTypeDeserializer valueDeserializer = getValueDeserializer(parameterizedType);
                    Object value = valueDeserializer.deserialize(charSource, buf, i, toIndex, getValueType(parameterizedType), null, '}', jsonParseContext);
                    instance.put(key, value);
                    i = jsonParseContext.endIndex;
                    int endChar = jsonParseContext.endChar;
                    if (endChar == 0) {
                        while ((ch = buf[++i]) <= ' ') ;
                        if (allowComment) {
                            if (ch == '/') {
                                ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                            }
                        }
                    } else {
                        ch = (char) endChar;
                        jsonParseContext.endChar = 0;
                    }
                    if (ch == ',') {
                        continue;
                    }
                    if (ch == '}') {
                        jsonParseContext.endIndex = i;
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

        Object deserializeMap(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object obj, JSONParseContext jsonParseContext) throws Exception {

            Map instance;
            if (obj != null) {
                instance = (Map) obj;
            } else {
                instance = createMapInstance(parameterizedType);
            }

            boolean empty = true;
            byte b;
            boolean disableCacheMapKey = jsonParseContext.disableCacheMapKey;
            boolean allowComment = jsonParseContext.allowComment;
            for (int i = fromIndex + 1; ; ++i) {
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }

                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                int fieldKeyFrom = i;
                Serializable mapKey;
                Object key;

                if (b == '"') {
                    mapKey = disableCacheMapKey ? parseMapKey(buf, i, toIndex, '"', jsonParseContext) : parseMapKeyByCache(buf, i, toIndex, '"', jsonParseContext);
                    i = jsonParseContext.endIndex;
                    empty = false;
                    ++i;
                } else {
                    // empty object or exception
                    if (b == '}') {
                        if (!empty && !jsonParseContext.allowLastEndComma) {
                            throw new JSONException("Syntax error, at pos " + i + ", the closing symbol '}' is not allowed here.");
                        }
                        jsonParseContext.endIndex = i;
                        return instance;
                    }
                    if (b == '\'') {
                        if (jsonParseContext.allowSingleQuotes) {
                            while (i + 1 < toIndex && (buf[++i] != '\'' || buf[i - 1] == '\\')) ;
                            empty = false;
                            ++i;
                            mapKey = parseKeyOfMap(buf, fieldKeyFrom, i, false);
                        } else {
                            throw new JSONException("Syntax error, at pos " + i + ", the single quote symbol ' is not allowed here.");
                        }
                    } else {
                        if (jsonParseContext.allowUnquotedFieldNames) {
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
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (b == ':') {
                    Class mapKeyClass = parameterizedType == null ? null : parameterizedType.getMapKeyClass();
                    key = mapKeyToType(mapKey, mapKeyClass);  // parseKeyOfMap(fieldKeyFrom, fieldKeyTo, buf, mapKeyClass, isUnquotedFieldName, jsonParseContext);

                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }

                    Object value = getValueDeserializer(parameterizedType).deserialize(charSource, buf, i, toIndex, getValueType(parameterizedType), null, END_OBJECT, jsonParseContext);
                    instance.put(key, value);
                    i = jsonParseContext.endIndex;
                    int endChar = jsonParseContext.endChar;
                    if (endChar == 0) {
                        // clear white space characters
                        while ((b = buf[++i]) <= WHITE_SPACE) ;
                        if (allowComment) {
                            if (b == '/') {
                                b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                            }
                        }
                    } else {
                        b = (byte) endChar;
                        jsonParseContext.endChar = 0;
                    }

                    if (b == ',') {
                        continue;
                    }
                    if (b == '}') {
                        jsonParseContext.endIndex = i;
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

    static class CustomMapDeserializer extends MapDeserializer {

        final GenericParameterizedType valueParameterizedType;
        JSONTypeDeserializer valueDeserializer;

        GenericParameterizedType getValueType(GenericParameterizedType parameterizedType) {
            return valueParameterizedType;
        }

        protected JSONTypeDeserializer getValueDeserializer(GenericParameterizedType valueGenType) {
            if (valueDeserializer == null) {
                valueDeserializer = JSONTypeDeserializer.getTypeDeserializer(valueParameterizedType.getActualType());
            }
            return valueDeserializer;
        }

        public CustomMapDeserializer(Class<?> keyClass, Class<?> valueClass) {
            this.valueParameterizedType = GenericParameterizedType.actualType(valueClass);
        }
    }

    // 11、对象
    static class ObjectDeserializer extends JSONTypeDeserializer {

        Object deserializeObject(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType genericParameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
            Class clazz = genericParameterizedType.getActualType();
            JSONPojoDeserializer pojoDeserializer = (JSONPojoDeserializer) createObjectDeserializer(clazz);
            return pojoDeserializer.deserializePojo(charSource, buf, fromIndex, toIndex, genericParameterizedType, instance, '}', jsonParseContext);
        }

        Object deserializeObject(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType genericParameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
            Class clazz = genericParameterizedType.getActualType();
            JSONPojoDeserializer pojoDeserializer = (JSONPojoDeserializer) createObjectDeserializer(clazz);
            return pojoDeserializer.deserializePojo(charSource, buf, fromIndex, toIndex, genericParameterizedType, instance, END_OBJECT, jsonParseContext);
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType genericParameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '{':
                    return deserializeObject(charSource, buf, fromIndex, toIndex, genericParameterizedType, instance, jsonParseContext);
                case 'n':
                    return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
                default: {
                    if (jsonParseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                        jsonParseContext.endIndex = fromIndex + 1;
                        return null;
                    }
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for Object Type, expected '{' ");
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
                    return parseNull(buf, fromIndex, toIndex, jsonParseContext);
                default: {
                    if (jsonParseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                        jsonParseContext.endIndex = fromIndex + 1;
                        return null;
                    }
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for Object Type, expected '{' ");
                }
            }
        }
    }

    /***
     * 12、ANY
     */
    static class ANYDeserializer extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return value;
        }

        void skip(CharSource charSource, char[] buf, int fromIndex, int toIndex, char endToken, JSONParseContext jsonParseContext) throws Exception {

            char beginChar = buf[fromIndex];
            switch (beginChar) {
                case '{':
                    MAP.skip(charSource, buf, fromIndex, toIndex, jsonParseContext);
                    break;
                case '[':
                    COLLECTION.skip(charSource, buf, fromIndex, toIndex, jsonParseContext);
                    break;
                case '\'':
                case '"':
                    CHAR_SEQUENCE.skip(charSource, buf, fromIndex, beginChar, jsonParseContext);
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
                    NUMBER.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.AnyType, null, endToken, jsonParseContext);
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
                case '\'':
                case '"':
                    CHAR_SEQUENCE.skip(charSource, buf, fromIndex, beginByte, jsonParseContext);
                    break;
                case 'n':
                    parseNull(buf, fromIndex, toIndex, jsonParseContext);
                    break;
                case 't': {
                    parseTrue(buf, fromIndex, toIndex, jsonParseContext);
                    break;
                }
                case 'f': {
                    parseFalse(buf, fromIndex, toIndex, jsonParseContext);
                    break;
                }
                default: {
                    JSONTypeDeserializer.NUMBER.deserialize(null, buf, fromIndex, toIndex, GenericParameterizedType.AnyType, null, endToken, jsonParseContext);
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
                case '[':
                    return JSONDefaultParser.parseJSONArray(charSource, buf, fromIndex, toIndex, new ArrayList(), jsonParseContext);
                case '\'':
                case '"':
                    return CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, toIndex, beginChar, GenericParameterizedType.StringType, jsonParseContext);
//                    return JSONDefaultParser.parseJSONString(charSource, buf, fromIndex, toIndex, beginChar, jsonParseContext);
                case 'n':
                    return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
                case 't': {
                    return BOOLEAN.deserializeTrue(buf, fromIndex, toIndex, null, jsonParseContext);
                }
                case 'f': {
                    return BOOLEAN.deserializeFalse(buf, fromIndex, toIndex, null, jsonParseContext);
                }
                default: {
                    return NUMBER.deserialize(charSource, buf, fromIndex, toIndex, jsonParseContext.useBigDecimalAsDefault ? GenericParameterizedType.BigDecimalType : GenericParameterizedType.AnyType, null, endToken, jsonParseContext);
                }
            }
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            byte beginByte = buf[fromIndex];
            char beginChar = (char) beginByte;
            switch (beginChar) {
                case '{':
                    return JSONDefaultParser.parseJSONObject(charSource, buf, fromIndex, toIndex, new LinkedHashMap(), jsonParseContext);
                case '[':
                    return JSONDefaultParser.parseJSONArray(charSource, buf, fromIndex, toIndex, new ArrayList(), jsonParseContext);
                case '\'':
                case '"':
                    return CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, toIndex, beginByte, GenericParameterizedType.StringType, jsonParseContext);
                case 'n':
                    return parseNull(buf, fromIndex, toIndex, jsonParseContext);
                case 't': {
                    return parseTrue(buf, fromIndex, toIndex, jsonParseContext);
                }
                case 'f': {
                    return parseFalse(buf, fromIndex, toIndex, jsonParseContext);
                }
                default: {
                    return JSONTypeDeserializer.NUMBER.deserialize(null, buf, fromIndex, toIndex, GenericParameterizedType.AnyType, null, endToken, jsonParseContext);
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

        Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, JSONParseContext jsonParseContext) throws Exception {
            return deserialize(charSource, buf, fromIndex, toIndex, parameterizedType, instance, ZERO, jsonParseContext);
        }

        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, char endToken, JSONParseContext jsonParseContext) throws Exception {
            int endIndex = fromIndex + 3;
            if (buf[fromIndex + 1] == 'u'
                    && buf[fromIndex + 2] == 'l' && buf[endIndex] == 'l') {
                jsonParseContext.endIndex = endIndex;
                return null;
            }
            throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'null' because it starts with 'n', but found text '" + new String(buf, fromIndex, Math.min(toIndex - fromIndex + 1, 4)) + "'");
        }

        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object instance, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            int endIndex = fromIndex + 3;
            if (buf[fromIndex + 1] == 'u'
                    && buf[fromIndex + 2] == 'l' && buf[endIndex] == 'l') {
                jsonParseContext.endIndex = endIndex;
                return null;
            }
            throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'null' because it starts with 'n', but found text '" + new String(buf, fromIndex, Math.min(toIndex - fromIndex + 1, 4)) + "'");
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
    protected static final String parseObjectClassName(CharSource charSource, char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
        char ch;
        for (int i = fromIndex + 1; /*i < toIndex*/ ; ++i) {
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (jsonParseContext.allowComment) {
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
                    if (jsonParseContext.allowUnquotedFieldNames) {
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
            if (jsonParseContext.allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (ch == ':') {
                while ((ch = buf[++i]) <= ' ') ;
                if (jsonParseContext.allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                Object value = ANY.deserialize(charSource, buf, i, toIndex, null, null, '}', jsonParseContext);
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
     * @param toIndex
     * @param jsonParseContext
     * @return
     * @throws Exception
     */
    protected static final String parseObjectClassName(CharSource charSource, byte[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
        byte b;
        for (int i = fromIndex + 1; /*i < toIndex*/ ; ++i) {
            while ((b = buf[i]) <= ' ') {
                ++i;
            }
            if (jsonParseContext.allowComment) {
                if (b == '/') {
                    b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
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
                    if (jsonParseContext.allowUnquotedFieldNames) {
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
            if (jsonParseContext.allowComment) {
                if (b == '/') {
                    b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (b == ':') {
                while ((b = buf[++i]) <= ' ') ;
                if (jsonParseContext.allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                Object value = ANY.deserialize(charSource, buf, i, toIndex, null, null, END_OBJECT, jsonParseContext);
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
    static class SerializableDeserializer extends JSONTypeDeserializer {
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

    // NonInstance类型处理写了类名的json字符串反序列化
    static class NonInstanceDeserializer extends JSONTypeDeserializer {

        final Class<?> baseClass;

        NonInstanceDeserializer(Class<?> baseClass) {
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
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
            String className = parseObjectClassName(charSource, buf, fromIndex, toIndex, jsonParseContext);
            JSONPojoDeserializer deserializer = (JSONPojoDeserializer) getJSONTypeDeserializer(className);
            if (deserializer == null) {
                ANY.skip(charSource, buf, fromIndex, toIndex, endToken, jsonParseContext);
                return null;
            }
            int i = jsonParseContext.endIndex;
            char ch;
            while ((ch = buf[++i]) <= ' ') ;
            if (ch == ',') {
                return deserializer.deserializePojo(charSource, buf, i, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
            } else if (ch == '}') {
                jsonParseContext.endIndex = i;
                return deserializer.pojo(deserializer.createPojo());
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or '}'");
            }
//            return deserializer.deserializePojo(charSource, buf, jsonParseContext.endIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
        }

        @Override
        protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            String className = parseObjectClassName(charSource, bytes, fromIndex, toIndex, jsonParseContext);
            JSONPojoDeserializer deserializer = (JSONPojoDeserializer) getJSONTypeDeserializer(className);
            if (deserializer == null) {
                ANY.skip(charSource, bytes, fromIndex, toIndex, endToken, jsonParseContext);
                return null;
            }

            int i = jsonParseContext.endIndex;
            byte b;
            while ((b = bytes[++i]) <= ' ') ;
            if (b == ',') {
                return deserializer.deserializePojo(charSource, bytes, i, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
            } else if (b == '}') {
                jsonParseContext.endIndex = i;
                return deserializer.pojo(deserializer.createPojo());
            } else {
                String errorContextTextAt = createErrorContextText(bytes, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or '}'");
            }
            // return deserializer.deserializePojo(charSource, bytes, fromIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
        }
    }

    /**
     * 反序列化null
     *
     * @return
     */
    protected final static Object parseNull(byte[] bytes, int fromIndex, int toIndex, JSONParseContext jsonParseContext) {
        int endIndex = fromIndex + 3;
        if (bytes[fromIndex + 1] == 'u' && bytes[fromIndex + 2] == 'l' && bytes[endIndex] == 'l') {
            jsonParseContext.endIndex = endIndex;
            return null;
        }
        throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'null' because it starts with 'n', but found text '" + new String(bytes, fromIndex, Math.min(toIndex - fromIndex + 1, 4)) + "'");
    }

    /**
     * 反序列化true
     *
     * @param bytes
     * @param fromIndex
     * @param toIndex
     * @param jsonParseContext
     * @return
     * @throws Exception
     */
    protected final static boolean parseTrue(byte[] bytes, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
        int endIndex = fromIndex + 3;
        if (UnsafeHelper.getInt(bytes, fromIndex) == TRUE_INT
            /*bytes[fromIndex + 1] == 'r' && bytes[fromIndex + 2] == 'u' && bytes[endIndex] == 'e'*/) {
            jsonParseContext.endIndex = endIndex;
            return true;
        }
        int len = Math.min(toIndex - fromIndex + 1, 4);
        throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'true' because it starts with 't', but found text '" + new String(bytes, fromIndex, len) + "'");
    }

    /**
     * 反序列化false
     *
     * @param bytes
     * @param fromIndex
     * @param toIndex
     * @param jsonParseContext
     * @return
     * @throws Exception
     */
    protected final static boolean parseFalse(byte[] bytes, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
        int endIndex = fromIndex + 4;
        if (UnsafeHelper.getInt(bytes, fromIndex + 1) == ALSE_INT/*bytes[fromIndex + 1] == 'a'
                && bytes[fromIndex + 2] == 'l'
                && bytes[fromIndex + 3] == 's'
                && bytes[endIndex] == 'e'*/) {
            jsonParseContext.endIndex = endIndex;
            return false;
        }
        int len = Math.min(toIndex - fromIndex + 1, 5);
        throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'false' because it starts with 'f', but found text '" + new String(bytes, fromIndex, len) + "'");
    }

    static String parseMapKey(byte[] bytes, int from, int toIndex, char endCh, JSONParseContext jsonParseContext) {
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
                if (i < toIndex - 1) {
                    next = bytes[i + 1];
                }
                if (writer == null) {
                    writer = getContextWriter(jsonParseContext);
                }
                escape = true;
                beginIndex = escape(bytes, next, i, beginIndex, writer, jsonParseContext);
                i = jsonParseContext.endIndex;
            } else {
                jsonParseContext.endIndex = i;
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

    static String parseMapKeyByCache(byte[] bytes, int from, int toIndex, char endCh, JSONParseContext jsonParseContext) {
        byte b, next = 0;
        int i = from;
        int beginIndex = from + 1;
        int len;
        JSONCharArrayWriter writer = null;
        if (!jsonParseContext.escape) {
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
            jsonParseContext.endIndex = i;
            len = i - beginIndex;
            if (len <= 8) {
                return getCacheEightBytesKey(bytes, beginIndex, len, hashValue);
            }
            return getCacheKey(bytes, beginIndex, len, hashValue);
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
                        writer = getContextWriter(jsonParseContext);
                    }
                    escape = true;
                    beginIndex = escape(bytes, next, i, beginIndex, writer, jsonParseContext);
                    i = jsonParseContext.endIndex;
                } else {
                    jsonParseContext.endIndex = i;
                    len = i - beginIndex;
                    if (escape) {
                        writer.writeBytes(bytes, beginIndex, len);
                        return writer.toString();
                    } else {
                        if (len <= 8) {
                            return getCacheEightBytesKey(bytes, beginIndex, len, hashValue);
                        }
                        return getCacheKey(bytes, beginIndex, len, hashValue);
                    }
                }
            }
        }
    }


    private static class UUIDDeserializer extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return UUID.fromString(value);
        }

        @Override
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
            final char beginChar = buf[fromIndex], next;
            int offset = fromIndex + 1;
            if (beginChar == DOUBLE_QUOTATION || beginChar == '\'') {
                if ((next = buf[offset]) == beginChar) {
                    jsonParseContext.endIndex = offset;
                    return null;
                }
                try {
                    long h1 = hex(next), h2 = hex(buf[++offset]), h3 = hex(buf[++offset]), h4 = hex(buf[++offset]), h5 = hex(buf[++offset]), h6 = hex(buf[++offset]), h7 = hex(buf[++offset]), h8 = hex(buf[++offset]), h9 = buf[++offset],
                            h10 = hex(buf[++offset]), h11 = hex(buf[++offset]), h12 = hex(buf[++offset]), h13 = hex(buf[++offset]), h14 = buf[++offset], h15 = hex(buf[++offset]), h16 = hex(buf[++offset]), h17 = hex(buf[++offset]), h18 = hex(buf[++offset]),
                            h19 = buf[++offset], h20 = hex(buf[++offset]), h21 = hex(buf[++offset]), h22 = hex(buf[++offset]), h23 = hex(buf[++offset]), h24 = buf[++offset], h25 = hex(buf[++offset]), h26 = hex(buf[++offset]), h27 = hex(buf[++offset]),
                            h28 = hex(buf[++offset]), h29 = hex(buf[++offset]), h30 = hex(buf[++offset]), h31 = hex(buf[++offset]), h32 = hex(buf[++offset]), h33 = hex(buf[++offset]), h34 = hex(buf[++offset]), h35 = hex(buf[++offset]), h36 = hex(buf[++offset]);
                    long mostSigBits = h1 << 60 | h2 << 56 | h3 << 52 | h4 << 48 | h5 << 44 | h6 << 40 | h7 << 36 | h8 << 32 | h10 << 28 | h11 << 24 | h12 << 20 | h13 << 16 | h15 << 12 | h16 << 8 | h17 << 4 | h18;
                    long leastSigBits = h20 << 60 | h21 << 56 | h22 << 52 | h23 << 48 | h25 << 44 | h26 << 40 | h27 << 36 | h28 << 32 | h29 << 28 | h30 << 24 | h31 << 20 | h32 << 16 | h33 << 12 | h34 << 8 | h35 << 4 | h36;
                    if (h9 == '-' && h14 == '-' && h19 == '-' && h24 == '-' && buf[++offset] == beginChar) {
                        jsonParseContext.endIndex = offset;
                        return new UUID(mostSigBits, leastSigBits);
                    }
                } catch (Throwable throwable) {
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', deserialize UUID fail");
                }
            } else if (beginChar == 'n') {
                return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
            }

            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', deserialize UUID fail");
        }

        @Override
        protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            final byte beginByte = bytes[fromIndex], next;
            int offset = fromIndex + 1;
            if (beginByte == DOUBLE_QUOTATION || beginByte == '\'') {
                if ((next = bytes[offset]) == beginByte) {
                    jsonParseContext.endIndex = offset;
                    return null;
                }
                try {
                    long h1 = hex(next), h2 = hex(bytes[++offset]), h3 = hex(bytes[++offset]), h4 = hex(bytes[++offset]), h5 = hex(bytes[++offset]), h6 = hex(bytes[++offset]), h7 = hex(bytes[++offset]), h8 = hex(bytes[++offset]), h9 = bytes[++offset],
                            h10 = hex(bytes[++offset]), h11 = hex(bytes[++offset]), h12 = hex(bytes[++offset]), h13 = hex(bytes[++offset]), h14 = bytes[++offset], h15 = hex(bytes[++offset]), h16 = hex(bytes[++offset]), h17 = hex(bytes[++offset]), h18 = hex(bytes[++offset]),
                            h19 = bytes[++offset], h20 = hex(bytes[++offset]), h21 = hex(bytes[++offset]), h22 = hex(bytes[++offset]), h23 = hex(bytes[++offset]), h24 = bytes[++offset], h25 = hex(bytes[++offset]), h26 = hex(bytes[++offset]), h27 = hex(bytes[++offset]),
                            h28 = hex(bytes[++offset]), h29 = hex(bytes[++offset]), h30 = hex(bytes[++offset]), h31 = hex(bytes[++offset]), h32 = hex(bytes[++offset]), h33 = hex(bytes[++offset]), h34 = hex(bytes[++offset]), h35 = hex(bytes[++offset]), h36 = hex(bytes[++offset]);
                    long mostSigBits = h1 << 60 | h2 << 56 | h3 << 52 | h4 << 48 | h5 << 44 | h6 << 40 | h7 << 36 | h8 << 32 | h10 << 28 | h11 << 24 | h12 << 20 | h13 << 16 | h15 << 12 | h16 << 8 | h17 << 4 | h18;
                    long leastSigBits = h20 << 60 | h21 << 56 | h22 << 52 | h23 << 48 | h25 << 44 | h26 << 40 | h27 << 36 | h28 << 32 | h29 << 28 | h30 << 24 | h31 << 20 | h32 << 16 | h33 << 12 | h34 << 8 | h35 << 4 | h36;
                    if (h9 == '-' && h14 == '-' && h19 == '-' && h24 == '-' && bytes[++offset] == beginByte) {
                        jsonParseContext.endIndex = offset;
                        return new UUID(mostSigBits, leastSigBits);
                    }
                } catch (Throwable throwable) {
                    String errorContextTextAt = createErrorContextText(bytes, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', deserialize UUID fail");
                }
            } else if (beginByte == 'n') {
                return NULL.deserialize(charSource, bytes, fromIndex, toIndex, null, null, jsonParseContext);
            }

            String errorContextTextAt = createErrorContextText(bytes, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', deserialize UUID fail");
        }
    }
}
