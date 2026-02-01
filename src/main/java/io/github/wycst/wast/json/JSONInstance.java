package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.KeyValuePair;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.ReadOption;
import io.github.wycst.wast.json.options.WriteOption;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

@SuppressWarnings({"all"})
public final class JSONInstance {

    static JSONTypeModule JAVA_TIME_MODULE;
    final JSONStore store;

    public JSONInstance() {
        this(new JSONStore());
    }

    private JSONInstance(JSONStore store) {
        this.store = store;
        init();
    }

    public static JSONInstance create() {
        return new JSONInstance();
    }

    static JSONInstance internal() {
        return new JSONInstance(JSONStore.INSTANCE);
    }

    // 创建上下文
    static JSONParseContext createContext(final JSONMapHandler handler, ReadOption... readOptions) {
        final JSONParseContext context = new JSONParseContext() {
            @Override
            public Map<Serializable, Object> defaultMap() {
                return new LinkedHashMap<Serializable, Object>() {
                    @Override
                    public Object put(Serializable key, Object value) {
                        KeyValuePair<Serializable, Object> pair = handler.handle(key, value);
                        if (pair != null) {
                            return super.put(pair.getKey(), pair.getValue());
                        }
                        return null;
                    }
                };
            }
        };
        JSONOptions.readOptions(readOptions, context);
        return context;
    }

    void init() {
        if (EnvUtils.JDK_8_PLUS) {
            try {
                if (JAVA_TIME_MODULE == null) {
                    Class<JSONTypeModule> moduleClass = (Class<JSONTypeModule>) Class.forName("io.github.wycst.wast.json.JSONTypeModuleJavaTime");
                    JAVA_TIME_MODULE = moduleClass.newInstance();
                }
                store.register(JAVA_TIME_MODULE);
            } catch (Exception ignored) {
            }
        }

        // register JSONType
        register(JSONType.class, new JSONTypeMapper<JSONType>() {
            @Override
            public JSONValue<?> writeAs(JSONType value, JSONConfig jsonConfig) {
                return new JSONValue(value.getType());
            }

            @Override
            public JSONType readOf(Object value) {
                JSONType jsonType = JSONType.typeOf((String) value);
                if (jsonType == null) {
                    throw new JSONException("Unsupported JSONType: " + value);
                }
                return jsonType;
            }
        }, false);
    }

    /**
     * json -> Map(LinkHashMap)/List(ArrayList)/String
     *
     * @param json        source
     * @param readOptions 解析配置项
     * @return Map(LinkHashMap)/List(ArrayList)/String
     */
    public Object parse(String json, ReadOption... readOptions) {
        if (json == null) return null;
        return store.parser.parse(json, JSONParseContext.of(readOptions));
    }

    /**
     * json -> Map(LinkHashMap)/List(ArrayList)/String
     *
     * @param json        source
     * @param handler     自定义map处理器
     * @param readOptions 解析配置项
     * @return Map(LinkHashMap)/List(ArrayList)/String
     */
    public Object parse(String json, final JSONMapHandler handler, ReadOption... readOptions) {
        if (json == null) return null;
        return store.parser.parse(json, createContext(handler, readOptions));
    }

    /**
     * <p> 在确定返回类型情况下可以省去一个强转声明例如: </p>
     * <br/>
     * <p>double val = JSON.parseAs("1234567.89E2"); </p>
     *
     * @param json        source
     * @param readOptions 解析配置项
     * @return 六大类型: Map(LinkHashMap)/List(ArrayList)/String/Number/Boolean/null
     */
    public <T> T parseAs(String json, ReadOption... readOptions) {
        if (json == null) return null;
        return (T) store.parser.parse(json, JSONParseContext.of(readOptions));
    }

    /**
     * <p> 在确定返回number类型的情况下可以转换为常用的number实例
     * <p> 如果参数为double字符串，返回为int或者long可能出现精度丢失；
     *
     * @param json        source
     * @param targetClass targetClass
     * @param readOptions 解析配置项
     * @return Number
     */
    public <T> T parseNumberAs(String json, Class<T> targetClass, ReadOption... readOptions) {
        if (json == null) return null;
        Number value = parseAs(json, readOptions);
        return (T) ObjectUtils.toTypeNumber(value, targetClass);
    }

    /**
     * 将对象按字段固定排序（ASCII字符升序）序列化为json字符串
     *
     * @param target  target
     * @param options 配置项
     * @return json
     */
    public String sortJson(Object target, WriteOption... options) {
        if (target == null) return null;
        return sortJsonString(stringify(target, new JSONConfig()), options);
    }

    /**
     * 将JSON字符串按字段固定排序（ASCII字符升序）重新排列
     *
     * @param json    json
     * @param options 配置项
     * @return json
     */
    public String sortJsonString(String json, WriteOption... options) {
        if (json == null) return null;
        JSONParseContext context = new JSONParseContext() {
            @Override
            public Map<Serializable, Object> defaultMap() {
                return new TreeMap(new Comparator<Object>() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        if (o1 instanceof Number && o2 instanceof Number) {
                            Double d1 = ((Number) o1).doubleValue();
                            Double d2 = ((Number) o2).doubleValue();
                            return d1.compareTo(d2);
                        }
                        return String.valueOf(o1).compareTo(String.valueOf(o2));
                    }
                });
            }
        };

        return stringify(store.parser.parse(json, context), JSONConfig.of(options));
    }

    /**
     * json -> map
     *
     * @param json        json
     * @param mapCls      map class
     * @param readOptions 配置项
     * @return map instance
     */
    public Map parseMap(String json, Class<? extends Map> mapCls, ReadOption... readOptions) {
        return store.parser.parseMap(json, mapCls, readOptions);
    }

    /**
     * json -> map
     *
     * @param json        json
     * @param readOptions 配置项
     * @return map instance
     */
    public Map parseObject(String json, ReadOption... readOptions) {
        return store.parser.parseMap(json, LinkedHashMap.class, readOptions);
    }

    /**
     * 将json字符串转化为指定collection
     *
     * @param json          json字符串
     * @param collectionCls collection class
     * @param readOptions   配置项
     * @return collection instance
     */
    public Collection<?> parseCollection(String json, Class<? extends Collection> collectionCls, ReadOption... readOptions) {
        return store.parser.parseCollection(json, collectionCls, readOptions);
    }

    /**
     * 将json字符数组转为对象或者数组(Convert JSON strings to objects or arrays)
     *
     * @param buf         字符数组
     * @param readOptions 配置项
     * @return 对象或者数组
     */
    public Object parse(char[] buf, ReadOption... readOptions) {
        return store.parser.parse(buf, readOptions);
    }

    /***
     * 解析字节数组返回Map对象或者List集合
     *
     * @param bytes 字节数组（ascii/utf8/...）
     * @param readOptions 配置项
     * @return Map对象或者List集合
     */
    public Object parse(byte[] bytes, ReadOption... readOptions) {
        if (bytes == null) return null;
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.JDK_AGENT_INSTANCE.hasNegatives(bytes, 0, bytes.length)) {
                return store.parser.parseInternal(AsciiStringSource.of(JSONMemoryHandle.createAsciiString(bytes)), bytes, 0, bytes.length, null, readOptions);
            } else {
                return store.parser.parseInternal(UTF8CharSource.of(JSONMemoryHandle.createAsciiString(bytes)), bytes, 0, bytes.length, null, readOptions);
            }
        }
        return store.parser.parseInternal(null, bytes, 0, bytes.length, null, readOptions);
    }

    /***
     * 解析字节数组返回Map对象或者List集合
     *
     * @param bytes       字节数组（ascii/utf8/...）
     * @param handler     自定义map处理器
     * @param readOptions 配置项
     * @return Map对象或者List集合
     */
    public Object parse(byte[] bytes, final JSONMapHandler handler, ReadOption... readOptions) {
        if (bytes == null) return null;
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.JDK_AGENT_INSTANCE.hasNegatives(bytes, 0, bytes.length)) {
                return store.parser.parseInternal(AsciiStringSource.of(JSONMemoryHandle.createAsciiString(bytes)), bytes, 0, bytes.length, null, createContext(handler, readOptions));
            } else {
                return store.parser.parseInternal(UTF8CharSource.of(JSONMemoryHandle.createAsciiString(bytes)), bytes, 0, bytes.length, null, createContext(handler, readOptions));
            }
        }
        return store.parser.parseInternal(null, bytes, 0, bytes.length, null, createContext(handler, readOptions));
    }


    /***
     * 解析字节数组返回Map对象或者List集合
     *
     * @param bytes 字节数组（ascii/utf8）
     * @param ascii 指定参数为单字节数组（JDK9+）以减少一次扫描检测
     * @param readOptions 配置项
     * @return Map对象或者List集合
     * @see #parse(byte[], ReadOption...)
     */
    public Object parse(byte[] bytes, boolean ascii, ReadOption... readOptions) {
        if (bytes == null) return null;
        if (EnvUtils.JDK_9_PLUS) {
            if (ascii) {
                return store.parser.parseInternal(AsciiStringSource.of(JSONMemoryHandle.createAsciiString(bytes)), bytes, 0, bytes.length, null, readOptions);
            } else {
                return store.parser.parseInternal(UTF8CharSource.of(JSONMemoryHandle.createAsciiString(bytes)), bytes, 0, bytes.length, null, readOptions);
            }
        }
        return store.parser.parseInternal(null, bytes, 0, bytes.length, null, readOptions);
    }

    /**
     * 将json字符串转化为指定actualType的实例
     * <p>
     * 源字符串json是{}则返回actualType该类的对象，
     * <p>
     * 如果是[]集合,则返回元素类型为actualType的集合(collection)或者数组（actualType）
     *
     * @param json        json字符串
     * @param actualType  类型（type）
     * @param readOptions 解析配置项
     * @return object/array/collection
     */
    public Object parse(String json, Class<?> actualType, ReadOption... readOptions) {
        if (json == null) return null;
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(json);
            if (bytes.length == json.length()) {
                AsciiStringSource charSource = AsciiStringSource.of(json);
                return parseInternal(charSource, bytes, actualType, readOptions);
            } else {
                // utf16
                char[] chars = json.toCharArray();
                return parseInternal(UTF16ByteArraySource.of(json), chars, 0, chars.length, actualType, readOptions);
            }
        }
        char[] chars = (char[]) JSONMemoryHandle.getStringValue(json);
        return parseInternal(null, chars, 0, chars.length, actualType, readOptions);
    }

    /**
     * 解析buf字节数组，返回对象或者集合类型
     *
     * @param buf         字符数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @return 集合或者对象
     */
    public Object parse(byte[] buf, Class<?> actualType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.JDK_AGENT_INSTANCE.hasNegatives(buf, 0, buf.length)) {
                return parseInternal(AsciiStringSource.of(JSONMemoryHandle.createAsciiString(buf)), buf, actualType, readOptions);
            } else {
                return parseInternal(UTF8CharSource.of(JSONMemoryHandle.createAsciiString(buf)), buf, actualType, readOptions);
            }
        }
        return parseInternal(null, buf, actualType, readOptions);
    }

    /**
     * 解析buf字符数组，返回对象或者集合类型
     *
     * @param buf         字符数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @return 集合或者对象
     */
    public Object parse(char[] buf, Class<?> actualType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            String json = new String(buf);
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(json);
            if (bytes.length == buf.length) {
                AsciiStringSource charSource = AsciiStringSource.of(json);
                return parseInternal(charSource, bytes, actualType, readOptions);
            } else {
                // utf16
                return parseInternal(UTF16ByteArraySource.of(json), buf, 0, buf.length, actualType, readOptions);
            }
        }
        return parseInternal(null, buf, 0, buf.length, actualType, readOptions);
    }

    /**
     * 解析buf字符数组，返回对象或者集合类型
     *
     * @param buf         字符数组
     * @param fromIndex   开始位置
     * @param toIndex     结束位置
     * @param actualType  类型
     * @param readOptions 读取配置
     * @return 集合或者对象
     */
    public Object parse(char[] buf, int fromIndex, int toIndex, Class<?> actualType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            int len = toIndex - fromIndex;
            String json = new String(buf, fromIndex, len);
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(json);
            if (bytes.length == len) {
                AsciiStringSource charSource = AsciiStringSource.of(json);
                return parseInternal(charSource, bytes, actualType, readOptions);
            } else {
                // utf16
                return parseInternal(UTF16ByteArraySource.of(json), buf, fromIndex, toIndex, actualType, readOptions);
            }
        }
        return parseInternal(null, buf, fromIndex, toIndex, actualType, readOptions);
    }

    // 基于偏移位置和长度解析
    private Object parseInternal(final CharSource charSource, char[] buf, int fromIndex, int toIndex, final Class<?> actualType, ReadOption... readOptions) {
        return deserialize(buf, fromIndex, toIndex, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                char beginChar = buf[fromIndex];
                if (beginChar == '[') {
                    return store.COLLECTION_DESER.deserializeCollection(charSource, buf, fromIndex, GenericParameterizedType.collectionType(ArrayList.class, actualType), new ArrayList(), jsonParseContext);
                }
                JSONTypeDeserializer typeDeserializer = store.getTypeDeserializer(actualType);
                GenericParameterizedType<?> type = typeDeserializer.getGenericParameterizedType(actualType);
                return typeDeserializer.deserialize(charSource, buf, fromIndex, type, null, '\0', jsonParseContext);
            }
        }, readOptions);
    }

    /**
     * 解析buf字节数组，返回对象或者集合类型
     *
     * @param charSource  源
     * @param buf         字节数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @return 对象
     */
    private Object parseInternal(final CharSource charSource, byte[] buf, final Class<?> actualType, ReadOption... readOptions) {
        return deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                byte beginByte = buf[fromIndex];
                if (beginByte == '[') {
                    return store.COLLECTION_DESER.deserializeCollection(charSource, buf, fromIndex, GenericParameterizedType.collectionType(ArrayList.class, actualType), new ArrayList(), jsonParseContext);
                }
                JSONTypeDeserializer typeDeserializer = store.getTypeDeserializer(actualType);
                GenericParameterizedType<?> type = typeDeserializer.getGenericParameterizedType(actualType);
                return typeDeserializer.deserialize(charSource, buf, fromIndex, type, null, (byte) '\0', jsonParseContext);
            }
        }, readOptions);
    }

    private Object deserialize(char[] buf, int fromIndex, int toIndex, Deserializer deserializer, ReadOption... readOptions) {
        JSONParseContext parseContext = JSONParseContext.of(readOptions);
        try {
            char beginChar = '\0';
            while ((beginChar = buf[fromIndex]) <= ' ') {
                ++fromIndex;
            }
            while (buf[toIndex - 1] <= ' ') {
                --toIndex;
            }
            parseContext.toIndex = toIndex;
            boolean allowComment = parseContext.allowComment;
            if (allowComment && beginChar == '/') {
                /** 去除声明在头部的注释*/
                fromIndex = JSONGeneral.clearCommentAndWhiteSpaces(buf, fromIndex + 1, parseContext);
            }
            Object result = deserializer.deserialize(buf, fromIndex, parseContext);
            int endIndex = parseContext.endIndex;
            if (allowComment) {
                /** 去除声明在尾部的注释*/
                if (endIndex < toIndex - 1) {
                    char commentStart = '\0';
                    while (endIndex + 1 < toIndex && (commentStart = buf[++endIndex]) <= ' ') ;
                    if (commentStart == '/') {
                        endIndex = JSONGeneral.clearCommentAndWhiteSpaces(buf, endIndex + 1, parseContext);
                    }
                }
            }
            if (endIndex != toIndex - 1) {
                int wordNum = Math.min(50, buf.length - endIndex);
                throw new JSONException("Syntax error, at pos " + endIndex + " extra characters found, '" + new String(buf, endIndex, wordNum) + " ...'");
            }

            return result;
        } catch (Exception ex) {
            JSONGeneral.handleCatchException(ex, buf, toIndex);
            throw new JSONException("Error: " + ex.getMessage(), ex);
        } finally {
            parseContext.clear();
        }
    }

    /***
     * 提取重复代码
     *
     * @param buf 字节数组
     * @param deserializer 处理器
     * @param readOptions 读取配置
     * @return 结果
     */
    private Object deserialize(byte[] buf, int fromIndex, int toIndex, Deserializer deserializer, ReadOption... readOptions) {
        JSONParseContext parseContext = JSONParseContext.of(readOptions);
        try {
            byte beginByte;
            while ((beginByte = buf[fromIndex]) <= ' ') {
                ++fromIndex;
            }
            while (buf[toIndex - 1] <= ' ') {
                --toIndex;
            }
            parseContext.toIndex = toIndex;
            boolean allowComment = parseContext.allowComment;
            if (allowComment && beginByte == '/') {
                fromIndex = JSONGeneral.clearCommentAndWhiteSpaces(buf, fromIndex + 1, parseContext);
            }
            Object result = deserializer.deserialize(buf, fromIndex, parseContext);
            int endIndex = parseContext.endIndex;
            if (allowComment) {
                /** 去除声明在尾部的注释*/
                if (endIndex < toIndex - 1) {
                    byte commentStart = '\0';
                    while (endIndex + 1 < toIndex && (commentStart = buf[++endIndex]) <= ' ') ;
                    if (commentStart == '/') {
                        endIndex = JSONGeneral.clearCommentAndWhiteSpaces(buf, endIndex + 1, parseContext);
                    }
                }
            }

            if (endIndex != toIndex - 1) {
                int wordNum = Math.min(50, buf.length - endIndex);
                throw new JSONException("Syntax error, at pos " + endIndex + " extra characters found, '" + new String(buf, endIndex, wordNum) + " ...'");
            }

            return result;
        } catch (Exception ex) {
            JSONGeneral.handleCatchException(ex, buf, toIndex);
            throw new JSONException("Error: " + ex.getMessage(), ex);
        } finally {
            parseContext.clear();
        }
    }

    /**
     * <p> 将json字符串（{}）转化为指定class的实例
     *
     * @param json        源字符串
     * @param actualType  类型
     * @param readOptions 解析配置项
     * @return T对象
     */
    public <T> T parseObject(String json, Class<T> actualType, ReadOption... readOptions) {
        if (json == null) return null;
        JSONTypeDeserializer deserializer = store.getTypeDeserializer(actualType);
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(json);
            if (bytes.length == json.length()) {
                // use ascii bytes
                AsciiStringSource charSource = new AsciiStringSource(json);
                return parseObjectInternal(deserializer, charSource, bytes, actualType, readOptions);
            } else {
                // utf16
                char[] chars = json.toCharArray();
                return parseObjectInternal(deserializer, UTF16ByteArraySource.of(json), chars, actualType, readOptions);
            }
        }
        return parseObjectInternal(deserializer, null, (char[]) JSONMemoryHandle.getStringValue(json), actualType, readOptions);
    }

    <T> T parseObjectInternal(final JSONTypeDeserializer deserializer, final CharSource charSource, char[] buf, final Class<T> actualType, ReadOption... readOptions) {
        return (T) deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                GenericParameterizedType<?> genericParameterizedType = deserializer.getGenericParameterizedType(actualType);
                return deserializer.deserialize(charSource, buf, fromIndex, genericParameterizedType, null, '\0', jsonParseContext);
            }
        }, readOptions);
    }

    <T> T parseObjectInternal(final JSONTypeDeserializer deserializer, final CharSource charSource, byte[] buf, final Class<T> actualType, ReadOption... readOptions) {
        return (T) deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                GenericParameterizedType<?> genericParameterizedType = deserializer.getGenericParameterizedType(actualType);
                return deserializer.deserialize(charSource, buf, fromIndex, genericParameterizedType, null, JSONGeneral.ZERO, jsonParseContext);
            }
        }, readOptions);
    }

    /**
     * <p> 将字节数组转化为指定class的实例
     *
     * @param buf         字节数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @param <T>         泛型
     * @return 类型对象
     */
    public <T> T parseObject(byte[] buf, final Class<T> actualType, ReadOption... readOptions) {
        JSONTypeDeserializer typeDeserializer = store.getTypeDeserializer(actualType);
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.JDK_AGENT_INSTANCE.hasNegatives(buf, 0, buf.length)) {
                return parseObjectInternal(typeDeserializer, AsciiStringSource.of(JSONMemoryHandle.createAsciiString(buf)), buf, actualType, readOptions);
            } else {
                return parseObjectInternal(typeDeserializer, UTF8CharSource.of(JSONMemoryHandle.createAsciiString(buf)), buf, actualType, readOptions);
            }
        }
        return parseObjectInternal(typeDeserializer, null, buf, actualType, readOptions);
    }

    /**
     * <p> 将字节数组转化为指定class的实例
     *
     * @param buf         字节数组
     * @param ascii       是否ascii(或者iso-8859-1)编码（JDK9+）或者utf-8编码<br>
     *                    注: 只有确定buf都是单字节编码数组指定true，确定地utf-8指定false，,如果不确定请直接使用parseObject(buf, actualType)进行检测
     * @param actualType  类型
     * @param readOptions 读取配置
     * @param <T>         泛型
     * @return 类型对象
     * @see #parseObject(byte[], Class, ReadOption...)
     */
    public <T> T parseObject(byte[] buf, boolean ascii, final Class<T> actualType, ReadOption... readOptions) {
        JSONTypeDeserializer typeDeserializer = store.getTypeDeserializer(actualType);
        if (EnvUtils.JDK_9_PLUS) {
            if (ascii) {
                return parseObjectInternal(typeDeserializer, AsciiStringSource.of(JSONMemoryHandle.createAsciiString(buf)), buf, actualType, readOptions);
            } else {
                return parseObjectInternal(typeDeserializer, UTF8CharSource.of(JSONMemoryHandle.createAsciiString(buf)), buf, actualType, readOptions);
            }
        }
        return parseObjectInternal(typeDeserializer, null, buf, actualType, readOptions);
    }

    /**
     * <p> 将字符数组转化为指定class的实例
     *
     * @param buf         字符数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @param <T>         泛型
     * @return 类型对象
     */
    public <T> T parseObject(char[] buf, final Class<T> actualType, ReadOption... readOptions) {
        JSONTypeDeserializer typeDeserializer = store.getTypeDeserializer(actualType);
        if (EnvUtils.JDK_9_PLUS) {
            String json = new String(buf);
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(json);
            if (bytes.length == buf.length) {
                // use ascii bytes
                AsciiStringSource charSource = new AsciiStringSource(json);
                return parseObjectInternal(typeDeserializer, charSource, bytes, actualType, readOptions);
            } else {
                // utf16
                return parseObjectInternal(typeDeserializer, UTF16ByteArraySource.of(json), buf, actualType, readOptions);
            }
        }
        return parseObjectInternal(typeDeserializer, null, buf, actualType, readOptions);
    }

    /**
     * <p> 对Type类型的支持
     *
     * @param json        json字符串
     * @param type        类型
     * @param readOptions 读取配置
     * @param <T>         泛型
     * @return 类型对象
     */
    public <T> T parse(String json, Type type, ReadOption... readOptions) {
        if (type instanceof Class) {
            return (T) parse(json, (Class<?>) type, readOptions);
        }
        GenericParameterizedType<?> genericParameterizedType = GenericParameterizedType.of(type);
        if (genericParameterizedType == null) {
            throw new JSONException("not supported type " + type);
        }
        return (T) parse(json, genericParameterizedType, readOptions);
    }

    /**
     * 将json字符串转化为指定class的实例
     *
     * @param json                     源字符串
     * @param genericParameterizedType 泛型结构
     * @param readOptions              解析配置项
     * @return T对象
     */
    public <T> T parse(String json, GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        if (json == null) return null;
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(json);
            if (bytes.length == json.length()) {
                return parseInternal(AsciiStringSource.of(json), bytes, genericParameterizedType, readOptions);
            } else {
                // utf16
                char[] chars = json.toCharArray();
                return parseInternal(UTF16ByteArraySource.of(json), chars, genericParameterizedType, readOptions);
            }
        } else {
            return parseInternal(null, (char[]) JSONMemoryHandle.getStringValue(json), genericParameterizedType, readOptions);
        }
    }

    /**
     * 根据输入genericType选择对应的序列化器进行序列化
     *
     * @param charSource               源
     * @param buf                      json字符数组
     * @param genericParameterizedType 泛型结构
     * @param readOptions              读取配置
     * @param <T>                      泛型类型
     * @return 对象或者数组
     */
    private <T> T parseInternal(final CharSource charSource, char[] buf, final GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        return (T) deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                JSONTypeDeserializer deserializer = store.getTypeDeserializer(genericParameterizedType.getActualType());
                return deserializer.deserialize(charSource, buf, fromIndex, genericParameterizedType, null, '\0', jsonParseContext);
            }
        }, readOptions);
    }

    /**
     * 根据输入genericType选择对应的序列化器进行序列化
     *
     * @param charSource               源
     * @param buf                      json字节数组
     * @param genericParameterizedType 泛型结构
     * @param readOptions              读取配置
     * @param <T>                      泛型类型
     * @return 对象或者数组
     */
    private <T> T parseInternal(final CharSource charSource, byte[] buf, final GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        return (T) deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                JSONTypeDeserializer deserializer = store.getTypeDeserializer(genericParameterizedType.getActualType());
                return deserializer.deserialize(charSource, buf, fromIndex, genericParameterizedType, null, JSONGeneral.ZERO, jsonParseContext);
            }
        }, readOptions);
    }

    /**
     * 根据输入genericType选择对应的序列化器进行序列化
     *
     * @param buf                      json字节数组
     * @param genericParameterizedType 泛型结构
     * @param readOptions              读取配置
     * @param <T>                      泛型类型
     * @return 对象或者数组
     */
    public <T> T parse(byte[] buf, final GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.JDK_AGENT_INSTANCE.hasNegatives(buf, 0, buf.length)) {
                return parseInternal(AsciiStringSource.of(JSONMemoryHandle.createAsciiString(buf)), buf, genericParameterizedType, readOptions);
            } else {
                return parseInternal(UTF8CharSource.of(JSONMemoryHandle.createAsciiString(buf)), buf, genericParameterizedType, readOptions);
            }
        }
        return parseInternal(null, buf, genericParameterizedType, readOptions);
    }

    /**
     * 根据输入genericType选择对应的序列化器进行序列化
     *
     * @param buf                      json字符数组
     * @param genericParameterizedType 泛型结构
     * @param readOptions              读取配置
     * @param <T>                      泛型类型
     * @return 对象或者数组
     */
    public <T> T parse(char[] buf, final GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            String json = new String(buf);
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(json);
            if (bytes.length == buf.length) {
                return parseInternal(AsciiStringSource.of(json), bytes, genericParameterizedType, readOptions);
            } else {
                // utf16
                return parseInternal(UTF16ByteArraySource.of(json), buf, genericParameterizedType, readOptions);
            }
        }
        return parseInternal(null, buf, genericParameterizedType, readOptions);
    }

    /**
     * 解析集合
     *
     * @param json        源字符串
     * @param actualType  类型
     * @param readOptions 解析配置项
     * @return 集合
     */
    public <T> List<T> parseArray(String json, Class<T> actualType, ReadOption... readOptions) {
        if (json == null) return null;
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(json);
            if (bytes.length == json.length()) {
                return parseArrayInternal(AsciiStringSource.of(json), bytes, actualType, readOptions);
            } else {
                return parseArrayInternal(UTF16ByteArraySource.of(json), json.toCharArray(), actualType, readOptions);
            }
        }
        return parseArrayInternal(null, (char[]) JSONMemoryHandle.getStringValue(json), actualType, readOptions);
    }

    /**
     * 解析集合
     *
     * @param buf         字符数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @param <T>         泛型
     * @return 集合
     */
    public <T> List<T> parseArray(char[] buf, final Class<T> actualType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            String json = new String(buf);
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(json);
            if (bytes.length == buf.length) {
                return parseArrayInternal(AsciiStringSource.of(json), bytes, actualType, readOptions);
            } else {
                return parseArrayInternal(UTF16ByteArraySource.of(json), buf, actualType, readOptions);
            }
        }
        return parseArrayInternal(null, buf, actualType, readOptions);
    }

    /**
     * 解析集合
     *
     * @param buf         字符数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @param <T>         泛型
     * @return 集合
     */
    private <T> List<T> parseArrayInternal(final CharSource charSource, char[] buf, final Class<T> actualType, ReadOption... readOptions) {
        return (List<T>) deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                return store.COLLECTION_DESER.deserialize(charSource, buf, fromIndex, GenericParameterizedType.collectionType(ArrayList.class, actualType), null, '\0', jsonParseContext);
            }
        }, readOptions);
    }

    /**
     * 解析集合
     *
     * @param buf         字符数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @param <T>         泛型
     * @return 集合
     */
    private <T> List<T> parseArrayInternal(final CharSource charSource, byte[] buf, final Class<T> actualType, ReadOption... readOptions) {
        return (List<T>) deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                return store.COLLECTION_DESER.deserialize(charSource, buf, fromIndex, GenericParameterizedType.collectionType(ArrayList.class, actualType), null, JSONGeneral.ZERO, jsonParseContext);
            }
        }, readOptions);
    }

    /**
     * 解析集合
     *
     * @param buf         字节数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @param <T>         泛型
     * @return 集合
     */
    public <T> List<T> parseArray(byte[] buf, final Class<T> actualType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.JDK_AGENT_INSTANCE.hasNegatives(buf, 0, buf.length)) {
                return parseArrayInternal(AsciiStringSource.of(JSONMemoryHandle.createAsciiString(buf)), buf, actualType, readOptions);
            } else {
                return parseArrayInternal(UTF8CharSource.of(JSONMemoryHandle.createAsciiString(buf)), buf, actualType, readOptions);
            }
        }
        return parseArrayInternal(null, buf, actualType, readOptions);
    }

    /**
     * 解析ndjson（jsonl）字符串返回列表
     * <p>
     * 注: 分隔符不限于换行回车符，即使没有分隔符或美化格式化后的多个json也支持解析
     *
     * @param json        json字符串
     * @param readOptions 解析配置项
     * @return 列表
     */
    public List<?> parseNdJson(String json, ReadOption... readOptions) {
        return JSONL.parseNdJson(json, store.ANY_DESER, readOptions);
    }

    /**
     * 解析ndjson（jsonl）流返回列表
     * <p>
     * 注: 分隔符不限于换行回车符，即使没有分隔符或美化格式化后的多个json也支持解析
     *
     * @param is          输入流
     * @param readOptions 解析配置项
     * @return 列表
     */
    public List<?> parseNdJson(InputStream is, ReadOption... readOptions) {
        return JSONL.parseNdJson(StringUtils.fromStream(is), store.ANY_DESER, readOptions);
    }

    /**
     * 支持同类型的ndjson（jsonl）解析
     *
     * @param json        json字符串
     * @param actualType  实例对象类型
     * @param readOptions 解析配置项
     * @param <T>         泛型
     * @return 列表
     */
    public <T> List<T> parseNdJson(String json, Class<T> actualType, ReadOption... readOptions) {
        return JSONL.parseNdJson(json, store.getTypeDeserializer(actualType), readOptions);
    }

    /**
     * 将json解析到指定实例对象中
     *
     * @param json        json字符串
     * @param instance    在外部构造的实例对象中（避免创建对象的反射开销）
     * @param readOptions 解析配置项
     * @return instance 对象
     */
    public Object parseToObject(String json, final Object instance, ReadOption... readOptions) {
        if (instance == null || json == null) {
            return null;
        }
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(json);
            if (json.length() == bytes.length) {
                // ascii
                return parseToObjectInternal(AsciiStringSource.of(json), bytes, instance, readOptions);
            } else {
                // utf16
                return parseToObjectInternal(UTF16ByteArraySource.of(json), json.toCharArray(), instance, readOptions);
            }
        }
        return parseToObjectInternal(null, (char[]) JSONMemoryHandle.getStringValue(json), instance, readOptions);
    }

    private Object parseToObjectInternal(final CharSource charSource, char[] buf, final Object instance, ReadOption... readOptions) {
        return deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                if (instance instanceof Map) {
                    return store.MAP_DESER.deserialize(charSource, buf, fromIndex, GenericParameterizedType.DefaultMap, instance, '\0', jsonParseContext);
                }
                Class<?> actualType = instance.getClass();
                JSONTypeDeserializer deserializer = store.getTypeDeserializer(actualType);
                GenericParameterizedType<?> type = deserializer.getGenericParameterizedType(actualType);
                return deserializer.deserialize(charSource, buf, fromIndex, type, instance, '\0', jsonParseContext);
            }
        }, readOptions);
    }

    private Object parseToObjectInternal(final CharSource charSource, byte[] buf, final Object instance, ReadOption... readOptions) {
        return deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                if (instance instanceof Map) {
                    return store.MAP_DESER.deserialize(charSource, buf, fromIndex, GenericParameterizedType.DefaultMap, instance, JSONGeneral.ZERO, jsonParseContext);
                }
                Class<?> actualType = instance.getClass();
                JSONTypeDeserializer deserializer = store.getTypeDeserializer(actualType);
                GenericParameterizedType<?> type = deserializer.getGenericParameterizedType(actualType);
                return deserializer.deserialize(charSource, buf, fromIndex, type, instance, JSONGeneral.ZERO, jsonParseContext);
            }
        }, readOptions);
    }

    /**
     * 解析目标json到指定泛型类型的集合对象
     *
     * @param json        json字符串
     * @param instance    集合类型
     * @param actualType  泛型类型
     * @param readOptions 解析配置项
     * @param <E>         泛型
     * @return 集合
     */
    public <E> Object parseToList(String json, final Collection<?> instance, final Class<E> actualType, ReadOption... readOptions) {
        if (instance == null || json == null) {
            return null;
        }
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(json);
            if (json.length() == bytes.length) {
                // ascii
                return parseToListInternal(AsciiStringSource.of(json), bytes, instance, actualType, readOptions);
            } else {
                // utf16
                return parseToListInternal(UTF16ByteArraySource.of(json), json.toCharArray(), instance, actualType, readOptions);
            }
        }
        return parseToListInternal(null, (char[]) JSONMemoryHandle.getStringValue(json), instance, actualType, readOptions);
    }

    private <E> Object parseToListInternal(final CharSource charSource, char[] buf, final Collection<?> instance, final Class<E> actualType, ReadOption... readOptions) {
        return deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                return store.COLLECTION_DESER.deserializeCollection(charSource, buf, fromIndex, GenericParameterizedType.collectionType(instance.getClass(), actualType), instance, jsonParseContext);
            }
        }, readOptions);
    }

    private <E> Object parseToListInternal(final CharSource charSource, byte[] buf, final Collection<?> instance, final Class<E> actualType, ReadOption... readOptions) {
        return deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                return store.COLLECTION_DESER.deserializeCollection(charSource, buf, fromIndex, GenericParameterizedType.collectionType(instance.getClass(), actualType), instance, jsonParseContext);
            }
        }, readOptions);
    }

    /***
     * 读取流返回Map对象或者List集合
     *
     * @param is 流对象(注: 不要使用网络流,请使用URL或者直接使用JSONReader)
     * @param readOptions 解析配置项
     * @return 集合或者Map对象
     */
    public Object read(InputStream is, ReadOption... readOptions) throws IOException {
        if (is == null) return null;
        return read(is, is.available(), readOptions);
    }

    /***
     * 读取流返回Map对象或者List集合
     *
     * @param is 流对象(注: 不要使用网络流,请使用URL或者直接使用JSONReader)
     * @param readOptions 解析配置项
     * @return 集合或者Map对象
     */
    private Object read(InputStream is, long size, ReadOption... readOptions) throws IOException {
        if (size <= 0) size = is.available();
        if (size <= JSONGeneral.DIRECT_READ_BUFFER_SIZE) {
            char[] buf = JSONGeneral.readOnceInputStream(is, (int) size);
            return parse(buf, readOptions);
        } else {
            JSONReader jsonReader = new JSONReader(is);
            jsonReader.setOptions(readOptions);
            return jsonReader.read();
        }
    }

    /**
     * 将输入流转化为指定class的实例
     *
     * @param is          输入流
     * @param actualType  实体类型
     * @param readOptions 解析配置项
     * @return T对象
     */
    public <T> T read(InputStream is, Class<T> actualType, ReadOption... readOptions) throws IOException {
        return read(is, Integer.MAX_VALUE, actualType, readOptions);
    }

    /**
     * 将输入流转化为指定class的实例
     *
     * @param is          输入流
     * @param size        总长度
     * @param actualType  实体类型
     * @param readOptions 解析配置项
     * @return T对象
     */
    <T> T read(InputStream is, long size, Class<T> actualType, ReadOption... readOptions) throws IOException {
        if (size <= 0) size = is.available();
        if (size <= JSONGeneral.DIRECT_READ_BUFFER_SIZE) {
            if (size == 0) return null;
            char[] buf = JSONGeneral.readOnceInputStream(is, (int) size);
            return (T) parse(buf, actualType, readOptions);
        } else {
            JSONReader jsonReader = new JSONReader(is);
            jsonReader.setOptions(readOptions);
            return jsonReader.readAsResult(GenericParameterizedType.actualType(actualType));
        }
    }

    /**
     * 读取json文件转化为指定class的实例
     *
     * @param file        文件
     * @param actualType  实体类型
     * @param readOptions 解析配置项
     * @return T对象
     */
    public <T> T read(File file, Class<T> actualType, ReadOption... readOptions) throws IOException {
        return read(new FileInputStream(file), Integer.MAX_VALUE, actualType, readOptions);
    }

    /**
     * 读取远程资源json转化为指定class的实例
     *
     * @param url         URL资源
     * @param actualType  实体类型
     * @param readOptions 解析配置项
     * @return T对象
     */
    public <T> T read(URL url, Class<T> actualType, ReadOption... readOptions) throws IOException {
        return read(url, actualType, false, -1, readOptions);
    }

    /**
     * 读取远程资源json转化为指定class的实例
     *
     * @param url             URL资源
     * @param actualType      实体类型
     * @param forceStreamMode 强制使用流模式(如果为false将读取contentLength作为字节长度)
     * @param timeout         超时时间
     * @param readOptions     解析配置项
     * @return T对象
     */
    public <T> T read(URL url, Class<T> actualType, boolean forceStreamMode, int timeout, ReadOption... readOptions) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (timeout > 0) {
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
        }
        conn.connect();
        return read(conn.getInputStream(), forceStreamMode ? Integer.MAX_VALUE : conn.getContentLength(), actualType, readOptions);
    }

    /**
     * 读取json文件转化为指定class的实例
     *
     * @param file        文件
     * @param genericType 泛型模型
     * @param readOptions 解析配置项
     * @return T对象
     */
    public <T> T read(File file, GenericParameterizedType<T> genericType, ReadOption... readOptions) throws IOException {
        JSONReader jsonReader = JSONReader.from(file);
        jsonReader.setOptions(readOptions);
        return jsonReader.readAsResult(genericType);
    }

    /**
     * 读取json文件转化为指定class的实例
     *
     * @param is          io流
     * @param genericType 泛型模型
     * @param readOptions 解析配置项
     * @return T对象
     */
    public <T> T read(InputStream is, GenericParameterizedType<T> genericType, ReadOption... readOptions) throws IOException {
        JSONReader jsonReader = JSONReader.from(is);
        jsonReader.setOptions(readOptions);
        return jsonReader.readAsResult(genericType);
    }

    /**
     * 将集合对象转为ndjson字符串
     *
     * @param collection   集合对象
     * @param writeOptions 写入配置项
     * @return ndjson字符串
     */
    public String toNdJsonString(Collection<?> collection, WriteOption... writeOptions) {
        JSONConfig jsonConfig = JSONConfig.of(writeOptions);
        JSONWriter content = JSONWriter.forStringWriter(jsonConfig);
        try {
            JSONL.writeNdJsonTo(store, content, collection, jsonConfig);
            return content.toString();
        } catch (Exception e) {
            throw (e instanceof JSONException) ? (JSONException) e : new JSONException(e);
        } finally {
            content.reset();
            jsonConfig.clear();
        }
    }

    /**
     * 将集合对象写入ndjson输入流（使用系统默认编码）
     *
     * @param collection   集合对象
     * @param os           输出流
     * @param writeOptions 写入配置项
     */
    public void writeNdJsonTo(Collection<?> collection, OutputStream os, WriteOption... writeOptions) {
        writeNdJsonTo(collection, os, EnvUtils.CHARSET_DEFAULT, writeOptions);
    }

    /**
     * 将集合对象写入ndjson输入流（使用系统默认编码）
     *
     * @param collection   集合对象
     * @param os           输出流
     * @param charset      编码
     * @param writeOptions 写入配置项
     */
    public void writeNdJsonTo(Collection<?> collection, OutputStream os, Charset charset, WriteOption... writeOptions) {
        JSONConfig jsonConfig = JSONConfig.of(writeOptions);
        JSONWriter streamWriter = JSONWriter.forStreamWriter(charset, jsonConfig);
        try {
            JSONL.writeNdJsonTo(store, streamWriter, collection, jsonConfig);
            streamWriter.toOutputStream(os);
        } catch (Exception e) {
            throw (e instanceof JSONException) ? (JSONException) e : new JSONException(e);
        } finally {
            streamWriter.reset();
            jsonConfig.clear();
        }
    }

    /**
     * 将source序列化为字符串然后解析为指定类型
     *
     * @param source      源对象
     * @param actualType  实体类型
     * @param readOptions 解析配置项
     * @param <T>         泛型类型
     * @return T
     */
    public <T> T translateTo(Object source, Class<T> actualType, ReadOption... readOptions) {
        return parseObject(toJsonString(source), actualType, readOptions);
    }

    /**
     * 使用json深度克隆一个对象
     *
     * @param source      源对象
     * @param readOptions 配置项
     * @param <T>         泛型类型
     * @return T
     */
    public <T> T cloneObject(Object source, ReadOption... readOptions) {
        return (T) parseObject(toJsonString(source), source.getClass(), readOptions);
    }

    /**
     * 将对象转化为json字符串 (Convert object to JSON string)
     *
     * @param obj 目标对象
     * @return JSON字符串
     */
    public String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        return stringify(obj, new JSONConfig());
    }

    /**
     * 将对象转化为json字符串
     *
     * @param obj     目标对象
     * @param options 配置选项
     * @return JSON字符串
     */
    public String toJsonString(Object obj, WriteOption... options) {
        if (obj == null) {
            return null;
        }
        return stringify(obj, JSONConfig.of(options));
    }

    /**
     * serialize obj
     *
     * @param obj        序列化对象
     * @param jsonConfig 配置
     * @return JSON字符串
     */
    public String toJsonString(Object obj, JSONConfig jsonConfig) {
        if (obj == null) {
            return null;
        }
        return stringify(obj, jsonConfig);
    }

    /**
     * 将对象转化为美化后的json字符串
     *
     * @param obj 源对象
     */
    public String toPrettifyJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        return stringify(obj, JSONConfig.of(WriteOption.FormatOutColonSpace));
    }

    /**
     * 将json字符串转为美化后的json字符串
     *
     * @param json json字符串
     */
    public String prettifyJsonString(String json) {
        if (json == null) {
            return null;
        }
        return stringify(parse(json), JSONConfig.of(WriteOption.FormatOutColonSpace));
    }

    String stringify(Object obj, JSONConfig jsonConfig) {
        JSONWriter content = JSONWriter.forStringWriter(jsonConfig);
        try {
            store.getTypeSerializer(obj.getClass()).serialize(obj, content, jsonConfig, 0);
            return content.toString();
        } catch (Exception e) {
            throw (e instanceof JSONException) ? (JSONException) e : new JSONException(e);
        } finally {
            content.reset();
            jsonConfig.clear();
        }
    }

    void writeToJSONWriter(Object object, JSONWriter writer, JSONConfig jsonConfig) {
        if (object != null) {
            try {
                store.getTypeSerializer(object.getClass()).serialize(object, writer, jsonConfig, 0);
                writer.flush();
            } catch (Exception e) {
                throw new JSONException(e);
            } finally {
                jsonConfig.clear();
                if (jsonConfig.autoCloseStream) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 将对象序列化为json字节数组
     *
     * @param obj     目标对象
     * @param options 配置
     */
    public byte[] toJsonBytes(Object obj, WriteOption... options) {
        return toJsonBytes(obj, EnvUtils.CHARSET_UTF_8, JSONConfig.of(options));
    }

    /**
     * 将对象序列化为json字节数组
     *
     * @param obj        目标对象
     * @param jsonConfig 配置
     */
    public byte[] toJsonBytes(Object obj, JSONConfig jsonConfig) {
        return toJsonBytes(obj, EnvUtils.CHARSET_UTF_8, jsonConfig);
    }

    /**
     * 将对象序列化为json字节数组
     *
     * @param obj     目标对象
     * @param charset 指定编码
     * @param options 配置
     */
    public byte[] toJsonBytes(Object obj, Charset charset, WriteOption... options) {
        return toJsonBytes(obj, charset, JSONConfig.of(options));
    }

    /**
     * 将对象序列化为json字节数组
     *
     * @param obj        目标对象
     * @param charset    指定编码
     * @param jsonConfig 配置
     */
    public byte[] toJsonBytes(Object obj, Charset charset, JSONConfig jsonConfig) {
        if (obj == null) {
            return null;
        }
        JSONWriter stringWriter = JSONWriter.forBytesWriter(charset, jsonConfig);
        try {
            writeToJSONWriter(obj, stringWriter, jsonConfig);
            return stringWriter.toBytes();
        } finally {
            stringWriter.reset();
        }
    }


    /**
     * 将对象序列化为json并写入到file文件中
     *
     * @param object  目标对象
     * @param file    目标文件
     * @param options 配置
     */
    public void writeJsonTo(Object object, File file, WriteOption... options) {
        try {
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            writeJsonTo(object, new FileOutputStream(file), EnvUtils.CHARSET_UTF_8, JSONConfig.of(options));
        } catch (FileNotFoundException e) {
            throw new JSONException(e.getMessage(), e);
        }
    }

    /**
     * 将对象序列化内容直接写入os
     *
     * @param object  目标对象
     * @param os      输出流
     * @param options 配置
     */
    public void writeJsonTo(Object object, OutputStream os, WriteOption... options) {
        writeJsonTo(object, os, EnvUtils.CHARSET_UTF_8, JSONConfig.of(options));
    }

    /**
     * 将对象序列化内容直接写入os
     *
     * @param object  目标对象
     * @param os      输出流
     * @param charset 指定编码
     * @param options 配置
     */
    public void writeJsonTo(Object object, OutputStream os, Charset charset, WriteOption... options) {
        writeJsonTo(object, os, charset, JSONConfig.of(options));
    }

    /**
     * 将对象序列化内容直接写入os
     *
     * @param object     目标对象
     * @param os         输出流
     * @param jsonConfig 配置
     */
    public void writeJsonTo(Object object, OutputStream os, JSONConfig jsonConfig) {
        writeJsonTo(object, os, EnvUtils.CHARSET_UTF_8, jsonConfig);
    }

    /**
     * 将对象序列化内容直接写入os
     *
     * @param object     目标对象
     * @param os         输出流
     * @param charset    指定编码
     * @param jsonConfig 配置
     */
    public void writeJsonTo(Object object, OutputStream os, Charset charset, JSONConfig jsonConfig) {
        JSONWriter streamWriter = JSONWriter.forStreamWriter(charset, jsonConfig);
        try {
            writeToJSONWriter(object, streamWriter, jsonConfig);
            streamWriter.toOutputStream(os);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            streamWriter.reset();
        }
    }

    /**
     * <p>
     * 将对象序列化内容使用指定的writer；<br/>
     * </p>
     *
     * <pre>
     *  OutputStream os = ...
     *  OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
     *  JSON.writeJsonTo(object, osw);
     * </pre>
     *
     * @param object  目标对象
     * @param writer  目标writer
     * @param options 序列化选项
     */
    public void writeJsonTo(Object object, Writer writer, WriteOption... options) {
        JSONConfig jsonConfig = JSONConfig.of(options);
        JSONWriter jsonWriter = JSONWriter.forStringWriter(jsonConfig);
        try {
            writeToJSONWriter(object, jsonWriter, jsonConfig);
            jsonWriter.writeTo(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            jsonWriter.reset();
        }
    }

    /**
     * 将超大对象（比如1G以上）序列化内容直接写入os <br/>
     * 此方法内部实现中没有缓冲，序列化时每次写入都会实时写入writer，不会占用过大内存；<br/>
     *
     * @param object  目标对象
     * @param os      目标os
     * @param options 序列化选项
     */
    public void writeSuperLargeJsonTo(Object object, OutputStream os, WriteOption... options) {
        writeToJSONWriter(object, JSONWriter.wrap(new OutputStreamWriter(os, EnvUtils.CHARSET_DEFAULT)), JSONConfig.of(options));
    }

    /**
     * 将超大对象（比如1G以上）序列化内容直接写入os <br/>
     * 此方法内部实现中没有缓冲，序列化时每次写入都会实时写入writer，不会占用过大内存；<br/>
     *
     * @param object  目标对象
     * @param os      目标os
     * @param charset 指定编码
     * @param options 序列化选项
     */
    public void writeSuperLargeJsonTo(Object object, OutputStream os, Charset charset, WriteOption... options) {
        writeToJSONWriter(object, JSONWriter.wrap(new OutputStreamWriter(os, charset)), JSONConfig.of(options));
    }

    /**
     * 将超大对象（比如1G以上）序列化内容直接写入os <br/>
     * 此方法内部实现中没有缓冲，序列化时每次写入都会实时写入writer,不会占用过大内存；<br/>
     *
     * @param object  目标对象
     * @param writer  目标writer
     * @param options 序列化选项
     */
    public void writeSuperLargeJsonTo(Object object, Writer writer, WriteOption... options) {
        writeToJSONWriter(object, JSONWriter.wrap(writer), JSONConfig.of(options));
    }

    /**
     * 支持自定义的对象反序列化器注册
     *
     * @param type             目标类型
     * @param typeDeserializer 反序列化器
     */
    public void register(Class<?> type, JSONTypeDeserializer typeDeserializer) {
        type.getClass();
        store.register(typeDeserializer, type);
    }

    /**
     * 支持自定义的对象序列化器注册
     *
     * @param type           目标类型
     * @param typeSerializer 序列化器
     */
    public void register(Class<?> type, JSONTypeSerializer typeSerializer) {
        type.getClass();
        store.register(typeSerializer, type);
    }

    /**
     * 支持自定义的对象模块注册
     *
     * @param type   目标类型
     * @param mapper 映射器
     */
    public <T> void register(Class<T> type, JSONTypeMapper<T> mapper) {
        register(type, mapper, false);
    }

    /**
     * 支持自定义的对象模块注册
     *
     * @param type             目标类型
     * @param mapper           映射器
     * @param applyAllSubClass 是否应用到所有子类
     */
    public <T> void register(Class<T> type, final JSONTypeMapper<T> mapper, boolean applyAllSubClass) {
        store.register(type, mapper, applyAllSubClass);
    }

    /**
     * 模块注册
     *
     * @param module 模块
     */
    public void register(JSONTypeModule module) {
        store.register(module);
    }

    /**
     * 模块注册
     */
    public void register(Class<? extends JSONTypeModule> moduleClass) {
        store.register(moduleClass);
    }

    /**
     * 全局关闭JIT优化（注: 即使不关闭，也仅仅序列化支持启用JIT，反序列化目前依然使用反射不支持JIT）
     */
    public void disableJIT() {
        store.disableJIT();
    }

    /**
     * 设置POJO字段别名(针对无法设置注解的实体类中给实体)
     *
     * @param pojoClass 实体类
     * @param fieldName 实体字段名
     * @param aliasName 别名
     */
    public JSONInstance mixinPojoField(Class<?> pojoClass, String fieldName, String aliasName) {
        store.updatePojoFieldAliasName(pojoClass, fieldName, aliasName);
        return this;
    }

    /**
     * 设置POJO字段别名(针对无法设置注解的实体类中给实体)
     *
     * @param pojoClass 实体类
     * @param namePairs 支持多个字段名-别名（数组长度必须偶数）
     */
    public JSONInstance mixinPojoFieldPairs(Class<?> pojoClass, String... namePairs) {
        Map<String, String> aliasNames = new HashMap<String, String>();
        if ((namePairs.length & 1) == 1) {
            throw new IllegalArgumentException("namePair.length must be even");
        }
        for (int i = 0; i < namePairs.length; i += 2) {
            aliasNames.put(namePairs[i], namePairs[i + 1]);
        }
        return mixinPojoFieldAliasNames(pojoClass, aliasNames);
    }

    /**
     * 设置POJO字段别名
     *
     * @param pojoClass  实体类
     * @param aliasNames 别名集合
     */
    public JSONInstance mixinPojoFieldAliasNames(Class<?> pojoClass, Map<String, String> aliasNames) {
        store.updatePojoFieldAliasNames(pojoClass, aliasNames);
        return this;
    }

    /**
     * 设置POJO字段别名
     *
     * @param pojoClass  实体类
     * @param properties 属性定义
     */
    public void mixinPojoProperties(Class<?> pojoClass, Map<String, JSONPropertyDefinition> properties) {
        store.updatePojoProperties(pojoClass, properties);
    }

    /**
     * 使用字符串解析方式： swar(JDK16+)
     */
    public JSONInstance useSWAR() {
        if (EnvUtils.JDK_16_PLUS) {
            store.setStringDeserializer(JSONTypeDeserializer.STRING_SWAR);
        }
        return this;
    }

    /**
     * 使用字符串向量处理方式： vector(JDK17+)
     */
    public JSONInstance useStringVector() {
        if (!EnvUtils.SUPPORTED_VECTOR) {
            throw new UnsupportedOperationException("Not supported vector operation");
        }
        store.setStringDeserializer(JSONTypeDeserializer.STRING_VECTOR);
        return this;
    }

    /**
     * 禁用字符串模式，按需场景使用（只关注非字符串字段的解析）；
     * <p>如果设置为true；</p>
     * <p>1. 序列化: 所有字符串输出为null（实体类跳过）；</p>
     * <p>2. 反序列化： 跳过字符串解析；</p>
     *
     * @param noneStringMode 是否禁用字符串模式
     */
    public JSONInstance setNoneStringMode(boolean noneStringMode) {
        store.setNoneStringMode(noneStringMode);
        return this;
    }

    /**
     * 重置store
     */
    public void reset() {
        store.reset();
        init();
    }

    /**
     * 清空所有注册的对象
     */
    public void clearAll() {
        store.clearAll();
    }

    /***
     * 本地反序列化器提取重复代码
     */
    abstract static class Deserializer {
        Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
            throw new UnsupportedOperationException();
        }

        Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
            throw new UnsupportedOperationException();
        }
    }
}
