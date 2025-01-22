/*
 * Copyright [2020-2024] [wangyunchao]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.github.wycst.wast.json;

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

/**
 * JSON serialization and deserialization tools
 * <p>
 * For example,
 *
 * <pre>
 *
 *  1, serialized object string
 *
 *  Map map = new HashMap();
 *  map.put("msg", "hello, light json !");
 *
 *  String result = JSON.toJsonString(map);
 *
 *  2, serialize objects to files or streams
 *
 *  Map map = new HashMap();
 *  map.put("msg", "hello, light json !");
 *  JSON.writeJsonTo(map, new File("/tmp/test.json"));
 *
 *  JSON.writeJsonTo(map, new FileOutputStream(new File("/tmp/text.json")));
 *
 *  Deserialize JSON Object：
 *
 *  Map map2 = (Map)JSON.parse(result);
 *  // or
 *  map2 = JSON.parseObject(result, Map.class);
 *
 *  Deserialize Collection
 *
 *  String json = "[\"test\", \"hello\", \"json\"]";
 *  List<String> msgs = (List<String>) JSON.parse(json);
 *  // or
 *  msgs = JSON.parseArray(json, String.class);
 *
 *
 * </pre>
 *
 * @author wangyunchao
 * @see JSON#toJsonString(Object)
 * @see JSON#toJsonString(Object, WriteOption...)
 * @see JSON#toJsonString(Object, JSONConfig)
 * @see JSON#writeJsonTo(Object, File, WriteOption...)
 * @see JSON#writeJsonTo(Object, OutputStream, WriteOption...)
 * @see JSON#writeJsonTo(Object, Writer, WriteOption...)
 * @see JSON#parse(String, ReadOption...)
 * @see JSON#parse(String, Class, ReadOption...)
 * @see JSON#parseObject(String, Class, ReadOption...)
 * @see JSON#parseArray(String, Class, ReadOption...)
 * @see JSON#parse(char[], Class, ReadOption...)
 * <p>
 * for more ...
 * @see JSONNode
 * @see JSONReader
 * @see JSONCharArrayWriter
 * @see GenericParameterizedType
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class JSON extends JSONGeneral {

    /**
     * json -> Map(LinkHashMap)/List(ArrayList)/String
     *
     * @param json        source
     * @param readOptions 解析配置项
     * @return Map(LinkHashMap)/List(ArrayList)/String
     */
    public static Object parse(String json, ReadOption... readOptions) {
        if (json == null) return null;
        return JSONDefaultParser.parse(json, readOptions);
    }

    /**
     * <p> 在确定返回类型情况下可以省去一个强转声明例如: </p>
     * <br/>
     * <p>double val = JSON.parseAs("1234567.89E2"); </p>
     *
     * @param json        source
     * @param readOptions 解析配置项
     * @return 六大类型: Map(LinkHashMap)/List(ArrayList)/String/Number/Boolean/null
     * @throws ClassCastException
     */
    public static <T> T parseAs(String json, ReadOption... readOptions) {
        if (json == null) return null;
        return (T) JSONDefaultParser.parse(json, readOptions);
    }

    /**
     * <p> 在确定返回number类型的情况下可以转换为常用的number实例
     * <p> 如果参数为double字符串，返回为int或者long可能出现精度丢失；
     *
     * @param json        source
     * @param targetClass targetClass
     * @param readOptions 解析配置项
     * @return Number
     * @throws ClassCastException
     */
    public static <T> T parseNumberAs(String json, Class<T> targetClass, ReadOption... readOptions) {
        if (json == null) return null;
        Number value = parseAs(json, readOptions);
        return (T) ObjectUtils.toTypeNumber(value, targetClass);
    }

    /**
     * json -> map
     *
     * @param json
     * @param mapCls
     * @param readOptions
     * @return map instance
     */
    public static Map parseMap(String json, Class<? extends Map> mapCls, ReadOption... readOptions) {
        return JSONDefaultParser.parseMap(json, mapCls, readOptions);
    }

    /**
     * json -> map
     *
     * @param json
     * @param readOptions
     * @return map instance
     */
    public static Map parseObject(String json, ReadOption... readOptions) {
        return JSONDefaultParser.parseMap(json, LinkedHashMap.class, readOptions);
    }

    /**
     * === > JSONNode.parse
     *
     * @param json
     * @param readOptions
     * @return
     */
    public static JSONNode parseNode(String json, ReadOption... readOptions) {
        if (json == null) return null;
        return JSONNode.parse(json, readOptions);
    }

    /**
     * 将json字符串转化为指定collection
     *
     * @param json
     * @param collectionCls
     * @param readOptions
     * @return collection instance
     */
    public static Collection parseCollection(String json, Class<? extends Collection> collectionCls, ReadOption... readOptions) {
        return JSONDefaultParser.parseCollection(json, collectionCls, readOptions);
    }

    /**
     * 将json字符数组转为对象或者数组(Convert JSON strings to objects or arrays)
     *
     * @param buf         字符数组
     * @param readOptions 配置项
     * @return
     */
    public static Object parse(char[] buf, ReadOption... readOptions) {
        return JSONDefaultParser.parse(buf, readOptions);
    }

    /***
     * 解析字节数组返回Map对象或者List集合
     *
     * @param bytes 字节数组（ascii/utf8/...）
     * @param readOptions
     * @return
     */
    public static Object parse(byte[] bytes, ReadOption... readOptions) {
        if (bytes == null) return null;
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.hasNegatives(bytes, 0, bytes.length)) {
                return JSONDefaultParser.parseInternal(AsciiStringSource.of(JSONUnsafe.createAsciiString(bytes)), bytes, 0, bytes.length, null, readOptions);
            } else {
                return JSONDefaultParser.parseInternal(UTF8CharSource.of(JSONUnsafe.createAsciiString(bytes)), bytes, 0, bytes.length, null, readOptions);
            }
        }
        return JSONDefaultParser.parseInternal(null, bytes, 0, bytes.length, null, readOptions);
    }

    /***
     * 解析字节数组返回Map对象或者List集合
     *
     * @param bytes 字节数组（ascii/utf8）
     * @param ascii 指定参数为单字节数组（JDK9+）以减少一次扫描检测
     * @param readOptions
     * @return
     * @see #parse(byte[], ReadOption...)
     */
    public static Object parse(byte[] bytes, boolean ascii, ReadOption... readOptions) {
        if (bytes == null) return null;
        if (EnvUtils.JDK_9_PLUS) {
            if (ascii) {
                return JSONDefaultParser.parseInternal(AsciiStringSource.of(JSONUnsafe.createAsciiString(bytes)), bytes, 0, bytes.length, null, readOptions);
            } else {
                return JSONDefaultParser.parseInternal(UTF8CharSource.of(JSONUnsafe.createAsciiString(bytes)), bytes, 0, bytes.length, null, readOptions);
            }
        }
        return JSONDefaultParser.parseInternal(null, bytes, 0, bytes.length, null, readOptions);
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
    public static Object parse(String json, Class<?> actualType, ReadOption... readOptions) {
        if (json == null)
            return null;
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == json.length()) {
                AsciiStringSource charSource = AsciiStringSource.of(json);
                return parseInternal(charSource, bytes, actualType, null, readOptions);
            } else {
                // utf16
                char[] chars = json.toCharArray();
                return parseInternal(UTF16ByteArraySource.of(json), chars, 0, chars.length, actualType, null, readOptions);
            }
        }
        char[] chars = (char[]) JSONUnsafe.getStringValue(json);
        return parseInternal(null, chars, 0, chars.length, actualType, null, readOptions);
    }

    /**
     * <p> 将json字符串（{}）转化为指定class的实例
     *
     * @param json       源字符串
     * @param actualType 类型
     * @return T对象
     * @see #parseObject(String, Class, ReadOption...)
     */
    public static <T> T parseObject(String json, Class<T> actualType) {
        if (json == null) return null;
        // if use JSONDefaultParser.parse
        JSONTypeDeserializer deserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == json.length()) {
                // use ascii bytes
                AsciiStringSource charSource = new AsciiStringSource(json);
                return parseObjectInternalWithoutOptions(deserializer, charSource, bytes, actualType);
            } else {
                // utf16
                char[] chars = json.toCharArray();
                return parseObjectInternalWithoutOptions(deserializer, UTF16ByteArraySource.of(json), chars, actualType);
            }
        }
        return parseObjectInternalWithoutOptions(deserializer, null, (char[]) JSONUnsafe.getStringValue(json), actualType);
    }

    /**
     * <p> 将json字符串（{}）转化为指定class的实例
     *
     * @param json        源字符串
     * @param actualType  类型
     * @param readOptions 解析配置项
     * @return T对象
     */
    public static <T> T parseObject(String json, Class<T> actualType, ReadOption... readOptions) {
        if (json == null) return null;
        JSONTypeDeserializer deserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
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
        return parseObjectInternal(deserializer, null, (char[]) JSONUnsafe.getStringValue(json), actualType, readOptions);
    }

    /**
     * <p> 将json字符串（{}）转化为指定class的实例
     *
     * @param json         源字符串
     * @param actualType   类型
     * @param customMapper
     * @param readOptions  解析配置项
     * @return T对象
     */
    public static <T> T parseObject(String json, Class<T> actualType, JSONCustomMapper customMapper, ReadOption... readOptions) {
        if (json == null) return null;
        try {
            return customMapper.parseCustomObject(json, actualType, readOptions);
        } catch (Throwable throwable) {
            throw new JSONException("custom error for " + actualType, throwable);
        }
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
    public static <T> T parseObject(char[] buf, final Class<T> actualType, ReadOption... readOptions) {
        JSONTypeDeserializer typeDeserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
        if (EnvUtils.JDK_9_PLUS) {
            String json = new String(buf);
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
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
     * <p> 将字节数组转化为指定class的实例
     *
     * @param buf        字节数组
     * @param actualType 类型
     * @param <T>        泛型
     * @return 类型对象
     * @see #parseObject(byte[], Class, ReadOption...)
     */
    public static <T> T parseObject(byte[] buf, final Class<T> actualType) {
        JSONTypeDeserializer deserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.hasNegatives(buf, 0, buf.length)) {
                return parseObjectInternalWithoutOptions(deserializer, AsciiStringSource.of(JSONUnsafe.createAsciiString(buf)), buf, actualType);
            } else {
                return parseObjectInternalWithoutOptions(deserializer, UTF8CharSource.of(JSONUnsafe.createAsciiString(buf)), buf, actualType);
            }
        }
        return parseObjectInternalWithoutOptions(deserializer, null, buf, actualType);
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
    public static <T> T parseObject(byte[] buf, final Class<T> actualType, ReadOption... readOptions) {
        JSONTypeDeserializer typeDeserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.hasNegatives(buf, 0, buf.length)) {
                return parseObjectInternal(typeDeserializer, AsciiStringSource.of(JSONUnsafe.createAsciiString(buf)), buf, actualType, readOptions);
            } else {
                return parseObjectInternal(typeDeserializer, UTF8CharSource.of(JSONUnsafe.createAsciiString(buf)), buf, actualType, readOptions);
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
    public static <T> T parseObject(byte[] buf, boolean ascii, final Class<T> actualType, ReadOption... readOptions) {
        JSONTypeDeserializer typeDeserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
        if (EnvUtils.JDK_9_PLUS) {
            if (ascii) {
                return parseObjectInternal(typeDeserializer, AsciiStringSource.of(JSONUnsafe.createAsciiString(buf)), buf, actualType, readOptions);
            } else {
                return parseObjectInternal(typeDeserializer, UTF8CharSource.of(JSONUnsafe.createAsciiString(buf)), buf, actualType, readOptions);
            }
        }
        return parseObjectInternal(typeDeserializer, null, buf, actualType, readOptions);
    }

    /**
     * <p> 对Type类型的支持
     *
     * @param json
     * @param type
     * @param readOptions
     * @param <T>
     * @return
     */
    public static <T> T parse(String json, Type type, ReadOption... readOptions) {
        if (type instanceof Class) {
            return (T) parse(json, (Class<?>) type, readOptions);
        }
        GenericParameterizedType genericParameterizedType = GenericParameterizedType.of(type);
        if (genericParameterizedType == null) {
            throw new JSONException("not supported type " + type);
        }
        return (T) parse(json, genericParameterizedType, readOptions);
    }

    /**
     * 解析ndjson（jsonl）字符串返回列表
     * <p>
     * 注: 分隔符不限于换行回车符，即使没有分隔符或美化格式化后的多个json也支持解析
     *
     * @param json
     * @param readOptions
     * @return
     */
    public static List parseNdJson(String json, ReadOption... readOptions) {
        return JSONL.parseNdJson(json, JSONTypeDeserializer.ANY, readOptions);
    }

    /**
     * 解析ndjson（jsonl）流返回列表
     * <p>
     * 注: 分隔符不限于换行回车符，即使没有分隔符或美化格式化后的多个json也支持解析
     *
     * @param is
     * @param readOptions
     * @return
     */
    public static List parseNdJson(InputStream is, ReadOption... readOptions) {
//        JSONReader reader = (JSONReader) JSONReader.from(is).multiple(true).options(readOptions);
//        List list = new ArrayList();
//        Object result;
//        while ((result = reader.read()) != null) {
//            list.add(result);
//        }
//        return list;
        // Using parsing APIs seems to be faster
        return JSONL.parseNdJson(StringUtils.fromStream(is), JSONTypeDeserializer.ANY, readOptions);
    }

//    /**
//     * 解析ndjson（jsonl）返回指定索引位置的对象(按需)
//     *
//     * @param json
//     * @param index 支持负数代表倒数
//     * @param readOptions
//     * @return
//     */
//    public static Object parseNdJsonAt(String json, int index, ReadOption... readOptions) {
//        return JSONDefaultParser.parseNdJsonAt(json, index, readOptions);
//    }

    /**
     * 支持同类型的ndjson（jsonl）解析
     *
     * @param json
     * @param actualType
     * @param readOptions
     * @param <T>
     * @return
     */
    public static <T> List<T> parseNdJson(String json, Class<T> actualType, ReadOption... readOptions) {
        return JSONL.parseNdJson(json, JSONTypeDeserializer.getTypeDeserializer(actualType), readOptions);
    }


//    public static <T> T parseNdJson(String json, Class<T> actualType, int index, ReadOption... readOptions) {
//        return null;
//    }

    /**
     * 将集合对象转为ndjson字符串
     *
     * @param collection
     * @param writeOptions
     * @return
     */
    public static String toNdJsonString(Collection collection, WriteOption... writeOptions) {
        JSONConfig jsonConfig = JSONConfig.config(writeOptions);
        JSONWriter content = JSONWriter.forStringWriter(jsonConfig);
        try {
            JSONL.writeNdJsonTo(content, collection, jsonConfig);
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
     * @param collection
     * @param os
     * @param writeOptions
     */
    public static void writeNdJsonTo(Collection collection, OutputStream os, WriteOption... writeOptions) {
        writeNdJsonTo(collection, os, EnvUtils.CHARSET_DEFAULT, writeOptions);
    }

    /**
     * 将集合对象写入ndjson输入流（使用系统默认编码）
     *
     * @param collection
     * @param os
     * @param charset
     * @param writeOptions
     */
    public static void writeNdJsonTo(Collection collection, OutputStream os, Charset charset, WriteOption... writeOptions) {
        JSONConfig jsonConfig = JSONConfig.config(writeOptions);
        JSONWriter streamWriter = JSONWriter.forStreamWriter(charset, jsonConfig);
        try {
            JSONL.writeNdJsonTo(streamWriter, collection, jsonConfig);
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
     * @param source
     * @param actualType
     * @param readOptions
     * @param <T>
     * @return
     */
    public static <T> T translateTo(Object source, Class<T> actualType, ReadOption... readOptions) {
        return parseObject(toJsonString(source), actualType, readOptions);
    }

    /**
     * 使用json深度克隆一个对象
     *
     * @param source
     * @param <T>
     * @return
     */
    public static <T> T cloneObject(Object source, ReadOption... readOptions) {
        return (T) parseObject(toJsonString(source), source.getClass(), readOptions);
    }

    private static <T> T parseObjectInternalWithoutOptions(final JSONTypeDeserializer deserializer, final CharSource charSource, char[] buf, final Class<T> actualType) {
        int fromIndex = 0;
        int toIndex = buf.length;
        while ((buf[fromIndex]) <= ' ') {
            ++fromIndex;
        }
        JSONParseContext parseContext = new JSONParseContext();
        parseContext.toIndex = buf.length;
        try {
            GenericParameterizedType<T> genericParameterizedType = deserializer.getGenericParameterizedType(actualType);
            return (T) deserializer.deserialize(charSource, buf, fromIndex, genericParameterizedType, null, '\0', parseContext);
        } catch (Exception ex) {
            handleCatchException(ex, buf, toIndex);
            throw new JSONException("Error: " + ex.getMessage(), ex);
        } finally {
            parseContext.clear();
        }
    }

    private static <T> T parseObjectInternal(final JSONTypeDeserializer deserializer, final CharSource charSource, char[] buf, final Class<T> actualType, ReadOption... readOptions) {
        return (T) deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                GenericParameterizedType<T> genericParameterizedType = deserializer.getGenericParameterizedType(actualType);
                return deserializer.deserialize(charSource, buf, fromIndex, genericParameterizedType, null, '\0', jsonParseContext);
            }
        }, readOptions);
    }

    private static <T> T parseObjectInternalWithoutOptions(final JSONTypeDeserializer deserializer, final CharSource charSource, byte[] buf, final Class<T> actualType) {
        int fromIndex = 0;
        int toIndex = buf.length;
        while ((buf[fromIndex]) <= ' ') {
            ++fromIndex;
        }
        JSONParseContext parseContext = new JSONParseContext();
        parseContext.toIndex = buf.length;
        try {
            GenericParameterizedType<T> genericParameterizedType = deserializer.getGenericParameterizedType(actualType);
            return (T) deserializer.deserialize(charSource, buf, fromIndex, genericParameterizedType, null, ZERO, parseContext);
        } catch (Exception ex) {
            handleCatchException(ex, buf, toIndex);
            throw new JSONException("Error: " + ex.getMessage(), ex);
        } finally {
            parseContext.clear();
        }
    }

    private static <T> T parseObjectInternal(final JSONTypeDeserializer deserializer, final CharSource charSource, byte[] buf, final Class<T> actualType, ReadOption... readOptions) {
        return (T) deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                GenericParameterizedType<T> genericParameterizedType = deserializer.getGenericParameterizedType(actualType);
                return deserializer.deserialize(charSource, buf, fromIndex, genericParameterizedType, null, ZERO, jsonParseContext);
            }
        }, readOptions);
    }

    /**
     * 将json字符串转化为指定class的实例
     *
     * @param json                     源字符串
     * @param genericParameterizedType 泛型结构
     * @param readOptions              解析配置项
     * @return T对象
     */
    public static <T> T parse(String json, GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        if (json == null) return null;
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == json.length()) {
                return parseInternal(AsciiStringSource.of(json), bytes, genericParameterizedType, readOptions);
            } else {
                // utf16
                char[] chars = json.toCharArray();
                return parseInternal(UTF16ByteArraySource.of(json), chars, genericParameterizedType, readOptions);
            }
        } else {
            return parseInternal(null, (char[]) JSONUnsafe.getStringValue(json), genericParameterizedType, readOptions);
        }
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
    public static <T> T parse(char[] buf, final GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            String json = new String(buf);
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
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
     * 根据输入genericType选择对应的序列化器进行序列化
     *
     * @param buf                      json字节数组
     * @param genericParameterizedType 泛型结构
     * @param readOptions              读取配置
     * @param <T>                      泛型类型
     * @return 对象或者数组
     */
    public static <T> T parse(byte[] buf, final GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.hasNegatives(buf, 0, buf.length)) {
                return parseInternal(AsciiStringSource.of(JSONUnsafe.createAsciiString(buf)), buf, genericParameterizedType, readOptions);
            } else {
                return parseInternal(UTF8CharSource.of(JSONUnsafe.createAsciiString(buf)), buf, genericParameterizedType, readOptions);
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
    public static <T> List<T> parseArray(String json, Class<T> actualType, ReadOption... readOptions) {
        if (json == null) return null;
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == json.length()) {
                return parseArrayInternal(AsciiStringSource.of(json), bytes, actualType, readOptions);
            } else {
                return parseArrayInternal(UTF16ByteArraySource.of(json), json.toCharArray(), actualType, readOptions);
            }
        }
        return parseArrayInternal(null, (char[]) JSONUnsafe.getStringValue(json), actualType, readOptions);
    }

    /**
     * 解析集合
     *
     * @param buf         字符数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @param <T>         泛型
     * @return
     */
    public static <T> List<T> parseArray(char[] buf, final Class<T> actualType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            String json = new String(buf);
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == buf.length) {
                return parseArrayInternal(AsciiStringSource.of(json), bytes, actualType, readOptions);
            } else {
                return parseArrayInternal(UTF16ByteArraySource.of(json), buf, actualType, readOptions);
            }
        }
        return parseArrayInternal(null, buf, actualType, readOptions);
    }

    /**
     * 解析buf字符数组，返回对象或者集合类型
     *
     * @param buf         字符数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @return
     */
    public static Object parse(char[] buf, Class<?> actualType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            String json = new String(buf);
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == buf.length) {
                AsciiStringSource charSource = AsciiStringSource.of(json);
                return parseInternal(charSource, bytes, actualType, null, readOptions);
            } else {
                // utf16
                return parseInternal(UTF16ByteArraySource.of(json), buf, 0, buf.length, actualType, null, readOptions);
            }
        }
        return parseInternal(null, buf, 0, buf.length, actualType, null, readOptions);
    }

    /**
     * 解析buf字符数组，返回对象或者集合类型
     *
     * @param buf         字符数组
     * @param fromIndex   开始位置
     * @param toIndex     结束位置
     * @param actualType  类型
     * @param readOptions 读取配置
     * @return
     */
    public static Object parse(char[] buf, int fromIndex, int toIndex, Class<?> actualType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            int len = toIndex - fromIndex;
            String json = new String(buf, fromIndex, len);
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == len) {
                AsciiStringSource charSource = AsciiStringSource.of(json);
                return parseInternal(charSource, bytes, actualType, null, readOptions);
            } else {
                // utf16
                return parseInternal(UTF16ByteArraySource.of(json), buf, fromIndex, toIndex, actualType, null, readOptions);
            }
        }
        return parseInternal(null, buf, fromIndex, toIndex, actualType, null, readOptions);
    }

    /**
     * 解析buf字节数组，返回对象或者集合类型
     *
     * @param buf         字符数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @return
     */
    public static Object parse(byte[] buf, Class<?> actualType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.hasNegatives(buf, 0, buf.length)) {
                return parseInternal(AsciiStringSource.of(JSONUnsafe.createAsciiString(buf)), buf, actualType, null, readOptions);
            } else {
                return parseInternal(UTF8CharSource.of(JSONUnsafe.createAsciiString(buf)), buf, actualType, null, readOptions);
            }
        }
        return parseInternal(null, buf, actualType, null, readOptions);
    }

    /**
     * 将json解析到指定实例对象中
     *
     * @param json        json字符串
     * @param instance    在外部构造的实例对象中（避免创建对象的反射开销）
     * @param readOptions 解析配置项
     * @return instance 对象
     */
    public static Object parseToObject(String json, final Object instance, ReadOption... readOptions) {
        if (instance == null || json == null) {
            return null;
        }
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (json.length() == bytes.length) {
                // ascii
                return parseToObjectInternal(AsciiStringSource.of(json), bytes, instance, readOptions);
            } else {
                // utf16
                return parseToObjectInternal(UTF16ByteArraySource.of(json), json.toCharArray(), instance, readOptions);
            }
        }
        return parseToObjectInternal(null, (char[]) JSONUnsafe.getStringValue(json), instance, readOptions);
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
    public static <E> Object parseToList(String json, final Collection instance, final Class<E> actualType, ReadOption... readOptions) {
        if (instance == null || json == null) {
            return null;
        }
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (json.length() == bytes.length) {
                // ascii
                return parseToListInternal(AsciiStringSource.of(json), bytes, instance, actualType, readOptions);
            } else {
                // utf16
                return parseToListInternal(UTF16ByteArraySource.of(json), json.toCharArray(), instance, actualType, readOptions);
            }
        }
        return parseToListInternal(null, (char[]) JSONUnsafe.getStringValue(json), instance, actualType, readOptions);
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
    private static <T> T parseInternal(final CharSource charSource, char[] buf, final GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        return (T) deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                JSONTypeDeserializer deserializer = JSONTypeDeserializer.getTypeDeserializer(genericParameterizedType.getActualType());
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
    private static <T> T parseInternal(final CharSource charSource, byte[] buf, final GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        return (T) deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                JSONTypeDeserializer deserializer = JSONTypeDeserializer.getTypeDeserializer(genericParameterizedType.getActualType());
                return deserializer.deserialize(charSource, buf, fromIndex, genericParameterizedType, null, ZERO, jsonParseContext);
            }
        }, readOptions);
    }

    // 基于偏移位置和长度解析
    private static Object parseInternal(final CharSource charSource, char[] buf, int fromIndex, int toIndex, final Class<?> actualType, final Object defaultValue, ReadOption... readOptions) {
        return deserialize(buf, fromIndex, toIndex, new Deserializer() {

            Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                char beginChar = buf[fromIndex];
                switch (beginChar) {
                    case '[':
                        return JSONTypeDeserializer.COLLECTION.deserializeCollection(charSource, buf, fromIndex, GenericParameterizedType.collectionType(ArrayList.class, actualType), defaultValue == null ? new ArrayList() : defaultValue, jsonParseContext);
                    default: {
                        JSONTypeDeserializer typeDeserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
                        GenericParameterizedType type = typeDeserializer.getGenericParameterizedType(actualType);
                        return typeDeserializer.deserialize(charSource, buf, fromIndex, type, defaultValue, '\0', jsonParseContext);
                    }
                }
            }
        }, readOptions);
    }

    /**
     * 解析buf字节数组，返回对象或者集合类型
     *
     * @param charSource   源
     * @param buf          字节数组
     * @param actualType   类型
     * @param defaultValue 默认值
     * @param readOptions  读取配置
     * @return 对象
     */
    private static Object parseInternal(final CharSource charSource, byte[] buf, final Class<?> actualType, final Object defaultValue, ReadOption... readOptions) {
        return deserialize(buf, 0, buf.length, new Deserializer() {

            Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                byte beginByte = buf[fromIndex];
                switch (beginByte) {
                    case '[':
                        return JSONTypeDeserializer.COLLECTION.deserializeCollection(charSource, buf, fromIndex, GenericParameterizedType.collectionType(ArrayList.class, actualType), defaultValue == null ? new ArrayList() : defaultValue, jsonParseContext);
                    default: {
                        JSONTypeDeserializer typeDeserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
                        GenericParameterizedType type = typeDeserializer.getGenericParameterizedType(actualType);
                        return typeDeserializer.deserialize(charSource, buf, fromIndex, type, defaultValue, (byte) '\0', jsonParseContext);
                    }
                }
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
     * @return
     */
    private static <T> List<T> parseArrayInternal(final CharSource charSource, char[] buf, final Class<T> actualType, ReadOption... readOptions) {
        return (List<T>) deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                return JSONTypeDeserializer.COLLECTION.deserialize(charSource, buf, fromIndex, GenericParameterizedType.collectionType(ArrayList.class, actualType), null, '\0', jsonParseContext);
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
     * @return
     */
    private static <T> List<T> parseArrayInternal(final CharSource charSource, byte[] buf, final Class<T> actualType, ReadOption... readOptions) {
        return (List<T>) deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                return JSONTypeDeserializer.COLLECTION.deserialize(charSource, buf, fromIndex, GenericParameterizedType.collectionType(ArrayList.class, actualType), null, ZERO, jsonParseContext);
            }
        }, readOptions);
    }

    private static Object parseToObjectInternal(final CharSource charSource, char[] buf, final Object instance, ReadOption... readOptions) {
        return deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                if (instance instanceof Map) {
                    return JSONTypeDeserializer.MAP.deserialize(charSource, buf, fromIndex, GenericParameterizedType.DefaultMap, instance, '\0', jsonParseContext);
                }
                Class<?> actualType = instance.getClass();
                JSONTypeDeserializer deserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
                GenericParameterizedType type = deserializer.getGenericParameterizedType(actualType);
                return deserializer.deserialize(charSource, buf, fromIndex, type, instance, '\0', jsonParseContext);
            }
        }, readOptions);
    }

    private static Object parseToObjectInternal(final CharSource charSource, byte[] buf, final Object instance, ReadOption... readOptions) {
        return deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                if (instance instanceof Map) {
                    return JSONTypeDeserializer.MAP.deserialize(charSource, buf, fromIndex, GenericParameterizedType.DefaultMap, instance, ZERO, jsonParseContext);
                }
                Class<?> actualType = instance.getClass();
                JSONTypeDeserializer deserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
                GenericParameterizedType type = deserializer.getGenericParameterizedType(actualType);
                return deserializer.deserialize(charSource, buf, fromIndex, type, instance, ZERO, jsonParseContext);
            }
        }, readOptions);
    }

    private static <E> Object parseToListInternal(final CharSource charSource, char[] buf, final Collection instance, final Class<E> actualType, ReadOption... readOptions) {
        return deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                return JSONTypeDeserializer.COLLECTION.deserializeCollection(charSource, buf, fromIndex, GenericParameterizedType.collectionType(instance.getClass(), actualType), instance, jsonParseContext);
            }
        }, readOptions);
    }

    private static <E> Object parseToListInternal(final CharSource charSource, byte[] buf, final Collection instance, final Class<E> actualType, ReadOption... readOptions) {
        return deserialize(buf, 0, buf.length, new Deserializer() {
            Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
                return JSONTypeDeserializer.COLLECTION.deserializeCollection(charSource, buf, fromIndex, GenericParameterizedType.collectionType(instance.getClass(), actualType), instance, jsonParseContext);
            }
        }, readOptions);
    }

    /***
     * 本地反序列化器提取代码
     */
    abstract static class Deserializer {
        Object deserialize(char[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
            throw new UnsupportedOperationException();
        }

        Object deserialize(byte[] buf, int fromIndex, JSONParseContext jsonParseContext) throws Exception {
            throw new UnsupportedOperationException();
        }
    }

    private static Object deserialize(char[] buf, int fromIndex, int toIndex, Deserializer deserializer, ReadOption... readOptions) {
        JSONParseContext parseContext = new JSONParseContext();
        try {
            // Trim remove white space characters
            char beginChar = '\0';
            while (/*(fromIndex < toIndex) && */(beginChar = buf[fromIndex]) <= ' ') {
                ++fromIndex;
            }
            while (/*(toIndex > fromIndex) && */buf[toIndex - 1] <= ' ') {
                --toIndex;
            }
            JSONOptions.readOptions(readOptions, parseContext);
            parseContext.toIndex = toIndex;
            boolean allowComment = parseContext.allowComment;
            if (allowComment && beginChar == '/') {
                /** 去除声明在头部的注释*/
                fromIndex = clearCommentAndWhiteSpaces(buf, fromIndex + 1, parseContext);
            }
            Object result = deserializer.deserialize(buf, fromIndex, parseContext);
            int endIndex = parseContext.endIndex;
            if (allowComment) {
                /** 去除声明在尾部的注释*/
                if (endIndex < toIndex - 1) {
                    char commentStart = '\0';
                    while (endIndex + 1 < toIndex && (commentStart = buf[++endIndex]) <= ' ') ;
                    if (commentStart == '/') {
                        endIndex = clearCommentAndWhiteSpaces(buf, endIndex + 1, parseContext);
                    }
                }
            }

            if (endIndex != toIndex - 1) {
                int wordNum = Math.min(50, buf.length - endIndex);
                throw new JSONException("Syntax error, at pos " + endIndex + " extra characters found, '" + new String(buf, endIndex, wordNum) + " ...'");
            }

            return result;
        } catch (Exception ex) {
            handleCatchException(ex, buf, toIndex);
            throw new JSONException("Error: " + ex.getMessage(), ex);
        } finally {
            parseContext.clear();
        }
    }

    /***
     * 提取重复代码
     *
     * @param buf
     * @param deserializer
     * @param readOptions
     * @return
     */
    private static Object deserialize(byte[] buf, int fromIndex, int toIndex, Deserializer deserializer, ReadOption... readOptions) {
        JSONParseContext parseContext = new JSONParseContext();
        try {
            // Trim remove white space characters
            byte beginByte;
            while (/*(fromIndex < toIndex) && */(beginByte = buf[fromIndex]) <= ' ') {
                ++fromIndex;
            }
            while (/*(toIndex > fromIndex) && */buf[toIndex - 1] <= ' ') {
                --toIndex;
            }
            JSONOptions.readOptions(readOptions, parseContext);
            parseContext.toIndex = toIndex;
            boolean allowComment = parseContext.allowComment;
            if (allowComment && beginByte == '/') {
                fromIndex = clearCommentAndWhiteSpaces(buf, fromIndex + 1, parseContext);
            }
            Object result = deserializer.deserialize(buf, fromIndex, parseContext);
            int endIndex = parseContext.endIndex;
            if (allowComment) {
                /** 去除声明在尾部的注释*/
                if (endIndex < toIndex - 1) {
                    byte commentStart = '\0';
                    while (endIndex + 1 < toIndex && (commentStart = buf[++endIndex]) <= ' ') ;
                    if (commentStart == '/') {
                        endIndex = clearCommentAndWhiteSpaces(buf, endIndex + 1, parseContext);
                    }
                }
            }

            if (endIndex != toIndex - 1) {
                int wordNum = Math.min(50, buf.length - endIndex);
                throw new JSONException("Syntax error, at pos " + endIndex + " extra characters found, '" + new String(buf, endIndex, wordNum) + " ...'");
            }

            return result;
        } catch (Exception ex) {
            handleCatchException(ex, buf, toIndex);
            throw new JSONException("Error: " + ex.getMessage(), ex);
        } finally {
            parseContext.clear();
        }
    }

    /***
     * 读取字节数组返回Map对象或者List集合
     *
     * @param bytes utf编码
     * @param readOptions
     * @return
     */
    public static Object read(byte[] bytes, ReadOption... readOptions) {
        return parse(bytes, readOptions);
    }

    /***
     * 读取流返回Map对象或者List集合
     *
     * @param is 流对象(注: 不要使用网络流,请使用URL或者直接使用JSONReader)
     * @param readOptions
     * @return
     */
    public static Object read(InputStream is, ReadOption... readOptions) throws IOException {
        if (is == null) return null;
        return read(is, is.available(), readOptions);
    }

    /***
     * 读取流返回Map对象或者List集合
     *
     * @param is
     * @param readOptions
     * @return
     */
    private static Object read(InputStream is, long size, ReadOption... readOptions) throws IOException {
        if (size <= 0) size = is.available();
        if (size <= DIRECT_READ_BUFFER_SIZE) {
            char[] buf = readOnceInputStream(is, (int) size);
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
    public static <T> T read(InputStream is, Class<T> actualType, ReadOption... readOptions) throws IOException {
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
    private static <T> T read(InputStream is, long size, Class<T> actualType, ReadOption... readOptions) throws IOException {
        if (size <= 0) size = is.available();
        if (size <= DIRECT_READ_BUFFER_SIZE) {
            if (size == 0) return null;
            char[] buf = readOnceInputStream(is, (int) size);
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
    public static <T> T read(File file, Class<T> actualType, ReadOption... readOptions) throws IOException {
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
    public static <T> T read(URL url, Class<T> actualType, ReadOption... readOptions) throws IOException {
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
    public static <T> T read(URL url, Class<T> actualType, boolean forceStreamMode, int timeout, ReadOption... readOptions) throws IOException {
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
    public static <T> T read(File file, GenericParameterizedType<T> genericType, ReadOption... readOptions) throws IOException {
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
    public static <T> T read(InputStream is, GenericParameterizedType<T> genericType, ReadOption... readOptions) throws IOException {
        JSONReader jsonReader = JSONReader.from(is);
        jsonReader.setOptions(readOptions);
        return jsonReader.readAsResult(genericType);
    }

    /**
     * 将对象转化为json字符串 (Convert object to JSON string)
     *
     * @param obj 目标对象
     * @return JSON字符串
     */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        return stringify(obj, new JSONConfig(), 0);
    }

    /**
     * 将对象转化为json字符串
     *
     * @param obj     目标对象
     * @param options 配置选项
     * @return JSON字符串
     */
    public static String toJsonString(Object obj, WriteOption... options) {
        if (obj == null) {
            return null;
        }
        JSONConfig jsonConfig = new JSONConfig();
        JSONOptions.writeOptions(options, jsonConfig);
        return stringify(obj, jsonConfig, 0);
    }

    /**
     * 将对象转化为美化后的json字符串
     *
     * @param obj
     * @return
     */
    public static String toPrettifyJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        return stringify(obj, JSONConfig.config(WriteOption.FormatOutColonSpace), 0);
    }

    /**
     * 将json字符串转为美化后的json字符串
     *
     * @param json
     * @return
     */
    public static String prettifyJsonString(String json) {
        if (json == null) {
            return null;
        }
        return stringify(parse(json), JSONConfig.config(WriteOption.FormatOutColonSpace), 0);
    }

    /**
     * 将对象序列化为json字节数组
     *
     * @param obj
     * @param options
     * @return
     */
    public static byte[] toJsonBytes(Object obj, WriteOption... options) {
        return toJsonBytes(obj, Charset.defaultCharset(), options);
    }

    /**
     * 将对象序列化为json字节数组
     *
     * @param obj
     * @param charset 指定编码
     * @param options
     * @return
     */
    public static byte[] toJsonBytes(Object obj, Charset charset, WriteOption... options) {
        if (obj == null) {
            return null;
        }
        JSONConfig jsonConfig = JSONConfig.config(options);
        JSONWriter stringWriter = JSONWriter.forBytesWriter(charset, jsonConfig);
        try {
            writeToJSONWriter(obj, stringWriter, jsonConfig);
            return stringWriter.toBytes();
        } finally {
            stringWriter.reset();
        }
    }

    /**
     * serialize obj
     *
     * @param obj        序列化对象
     * @param jsonConfig 配置
     * @return JSON字符串
     */
    public static String toJsonString(Object obj, JSONConfig jsonConfig) {
        if (obj == null) {
            return null;
        }
        return stringify(obj, jsonConfig, 0);
    }

    private static String stringify(Object obj, JSONConfig jsonConfig, int indentLevel) {
        JSONWriter content = JSONWriter.forStringWriter(jsonConfig);
        try {
            JSONTypeSerializer.getTypeSerializer(obj.getClass()).serialize(obj, content, jsonConfig, indentLevel);
            return content.toString();
        } catch (Exception e) {
            throw (e instanceof JSONException) ? (JSONException) e : new JSONException(e);
        } finally {
            content.reset();
            jsonConfig.clear();
        }
    }

    /**
     * 将对象序列化为json并写入到file文件中
     *
     * @param object
     * @param file
     * @param options
     */
    public static void writeJsonTo(Object object, File file, WriteOption... options) {
        try {
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            writeJsonTo(object, new FileOutputStream(file), EnvUtils.CHARSET_DEFAULT, options);
        } catch (FileNotFoundException e) {
            throw new JSONException("file not found", e);
        }
    }

    /**
     * 将对象序列化内容直接写入os
     *
     * @param object
     * @param os
     * @param options
     */
    public static void writeJsonTo(Object object, OutputStream os, WriteOption... options) {
        writeJsonTo(object, os, EnvUtils.CHARSET_DEFAULT, options);
    }

    /**
     * 将对象序列化内容直接写入os
     *
     * @param object
     * @param os
     * @param charset
     * @param options
     */
    public static void writeJsonTo(Object object, OutputStream os, Charset charset, WriteOption... options) {
        JSONConfig jsonConfig = JSONConfig.config(options);
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
     * @param object
     * @param writer
     * @param options
     */
    public static void writeJsonTo(Object object, Writer writer, WriteOption... options) {
        JSONConfig jsonConfig = JSONConfig.config(options);
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
     * @param object
     * @param os
     * @param options
     */
    public static void writeSuperLargeJsonTo(Object object, OutputStream os, WriteOption... options) {
        writeToJSONWriter(object, JSONWriter.wrap(new OutputStreamWriter(os, EnvUtils.CHARSET_DEFAULT)), JSONConfig.config(options));
    }

    /**
     * 将超大对象（比如1G以上）序列化内容直接写入os <br/>
     * 此方法内部实现中没有缓冲，序列化时每次写入都会实时写入writer，不会占用过大内存；<br/>
     *
     * @param object
     * @param os
     * @param charset
     * @param options
     */
    public static void writeSuperLargeJsonTo(Object object, OutputStream os, Charset charset, WriteOption... options) {
        writeToJSONWriter(object, JSONWriter.wrap(new OutputStreamWriter(os, charset)), JSONConfig.config(options));
    }

    /**
     * 将超大对象（比如1G以上）序列化内容直接写入os <br/>
     * 此方法内部实现中没有缓冲，序列化时每次写入都会实时写入writer,不会占用过大内存；<br/>
     *
     * @param object
     * @param writer
     * @param options
     */
    public static void writeSuperLargeJsonTo(Object object, Writer writer, WriteOption... options) {
        writeToJSONWriter(object, JSONWriter.wrap(writer), JSONConfig.config(options));
    }

    /**
     * 将对象转化为json字符串
     *
     * @param obj     目标对象
     * @param options 配置选项
     * @return JSON字符串
     */
    public static String toJsonString(Object obj, JSONCustomMapper customizedMapper, WriteOption... options) {
        if (obj == null) {
            return null;
        }
        JSONConfig jsonConfig = JSONConfig.config(options);
        if (customizedMapper == null) {
            return stringify(obj, jsonConfig, 0);
        } else {
            return stringify(obj, jsonConfig, 0, customizedMapper);
        }
    }

    /**
     * 将对象转化为美化后的json字符串
     *
     * @param obj
     * @return
     */
    public static String toPrettifyJsonString(Object obj, JSONCustomMapper customizedMapper) {
        if (obj == null) {
            return null;
        }
        return stringify(obj, JSONConfig.config(WriteOption.FormatOutColonSpace), 0, customizedMapper);
    }

    /**
     * serialize obj
     *
     * @param obj          序列化对象
     * @param jsonConfig   配置
     * @param customMapper
     * @return JSON字符串
     */
    public static String toJsonString(Object obj, JSONConfig jsonConfig, JSONCustomMapper customMapper) {
        if (obj == null) {
            return null;
        }
        return stringify(obj, jsonConfig, 0, customMapper);
    }

    /**
     * 将对象序列化内容直接写入os
     *
     * @param object
     * @param os
     * @param options
     */
    public static void writeJsonTo(Object object, OutputStream os, JSONCustomMapper customMapper, WriteOption... options) {
        writeJsonTo(object, os, EnvUtils.CHARSET_DEFAULT, customMapper, options);
    }

    /**
     * 将对象序列化内容直接写入os
     *
     * @param object
     * @param os
     * @param charset
     * @param customMapper
     * @param options
     */
    public static void writeJsonTo(Object object, OutputStream os, Charset charset, JSONCustomMapper customMapper, WriteOption... options) {
        JSONConfig jsonConfig = JSONConfig.config(options);
        JSONWriter streamWriter = JSONWriter.forStreamWriter(charset, jsonConfig);
        try {
            writeToJSONWriter(object, streamWriter, jsonConfig, customMapper);
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
     * @param object
     * @param writer
     * @param customMapper
     * @param options
     */
    public static void writeJsonTo(Object object, Writer writer, JSONCustomMapper customMapper, WriteOption... options) {
        JSONConfig jsonConfig = JSONConfig.config(options);
        JSONWriter jsonWriter = JSONWriter.forStringWriter(jsonConfig);
        try {
            writeToJSONWriter(object, jsonWriter, jsonConfig, customMapper);
            jsonWriter.writeTo(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            jsonWriter.reset();
        }
    }

    static void writeToJSONWriter(Object object, JSONWriter writer, JSONConfig jsonConfig) {
        if (object != null) {
            try {
                JSONTypeSerializer.getTypeSerializer(object.getClass()).serialize(object, writer, jsonConfig, 0);
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

    static void writeToJSONWriter(Object object, JSONWriter writer, JSONConfig jsonConfig, JSONCustomMapper customizedMapper) {
        if (object != null) {
            try {
                customizedMapper.serializeCustomized(object, writer, jsonConfig, 0);
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

    private static String stringify(Object obj, JSONConfig jsonConfig, int indentLevel, JSONCustomMapper customizedMapper) {
        JSONWriter content = JSONWriter.forStringWriter(jsonConfig);
        try {
            customizedMapper.serializeCustomized(obj, content, jsonConfig, indentLevel);
            return content.toString();
        } catch (Exception e) {
            throw (e instanceof JSONException) ? (JSONException) e : new JSONException(e);
        } finally {
            content.reset();
            jsonConfig.clear();
        }
    }

    public static boolean validate(String json, ReadOption... readOptions) {
        if (json == null) return false;
        return JSONValidator.validate(json, readOptions);
    }

    public static boolean validate(char[] buf, ReadOption... readOptions) {
        return JSONValidator.validate(buf, readOptions);
    }

    public static boolean validate(byte[] buf, ReadOption... readOptions) {
        return JSONValidator.validate(buf, readOptions);
    }

    /**
     * 支持自定义的对象反序列化器注册
     *
     * @param type
     * @param typeDeserializer
     */
    public synchronized static void registerTypeDeserializer(Class<?> type, JSONTypeDeserializer typeDeserializer) {
        type.getClass();
        if (JSONTypeDeserializer.isBuiltInType(type)) {
            throw new UnsupportedOperationException("Existing built-in implementation, does not support overwrite for " + type);
        }
        JSONTypeDeserializer.putTypeDeserializer(typeDeserializer, type);
    }

    /**
     * 支持自定义的对象序列化器注册
     *
     * @param type
     * @param typeSerializer
     */
    public synchronized static void registerTypeSerializer(Class<?> type, JSONTypeSerializer typeSerializer) {
        type.getClass();
        if (JSONTypeSerializer.isBuiltInType(type)) {
            throw new UnsupportedOperationException("Existing built-in implementation, does not support overwrite for " + type);
        }
        JSONTypeSerializer.putTypeSerializer(typeSerializer, type);
    }

    public static <T> void registerTypeMapper(Class<T> type, JSONTypeMapper<T> mapper) {
        registerTypeMapper(type, mapper, false);
    }

    public static <T> void registerTypeMapper(Class<T> type, final JSONTypeMapper<T> mapper, boolean ignoreIfExist) {
        type.getClass();
        synchronized (type) {
            if (JSONTypeDeserializer.isBuiltInType(type)) {
                if (!ignoreIfExist) {
                    throw new UnsupportedOperationException("Existing built-in implementation, does not support overwrite for " + type);
                }
            } else {
                // deserializer
                JSONTypeDeserializer.registerTypeDeserializer(buildCustomizedDeserializer(mapper), type);
            }
            if (JSONTypeSerializer.isBuiltInType(type)) {
                if (!ignoreIfExist) {
                    throw new UnsupportedOperationException("Existing built-in implementation, does not support overwrite for class " + type);
                }
            } else {
                // serializer
                JSONTypeSerializer.registerTypeSerializer(buildCustomizedSerializer(mapper), type);
            }
        }
    }

    static <T> JSONTypeSerializer buildCustomizedSerializer(final JSONTypeMapper<T> mapper) {
        return new JSONTypeSerializer() {
            @Override
            protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                JSONValue<?> result = mapper.writeAs((T) value, jsonConfig);
                Object baseValue;
                if (result == null || (baseValue = result.value) == null) {
                    writer.writeNull();
                } else {
                    JSONTypeSerializer typeSerializer = getTypeSerializer(baseValue.getClass());
                    typeSerializer.serialize(baseValue, writer, jsonConfig, indent);
                }
            }

            @Override
            protected void serializeCustomized(Object value, JSONWriter writer, JSONConfig jsonConfig, int indentLevel, JSONCustomMapper customizedMapper) throws Exception {
                JSONValue<?> result = mapper.writeAs((T) value, jsonConfig);
                Object baseValue;
                if (result == null || (baseValue = result.value) == null) {
                    writer.writeNull();
                } else {
                    if (baseValue instanceof Map) {
                        MAP.serializeCustomized(baseValue, writer, jsonConfig, indentLevel, customizedMapper);
                    } else if (baseValue instanceof List) {
                        COLLECTION.serializeCustomized(baseValue, writer, jsonConfig, indentLevel, customizedMapper);
                    } else {
                        JSONTypeSerializer typeSerializer = getTypeSerializer(baseValue.getClass());
                        typeSerializer.serialize(baseValue, writer, jsonConfig, indentLevel);
                    }
                }
            }
        };
    }

    static <E> JSONTypeDeserializer buildCustomizedDeserializer(final JSONTypeMapper<E> typeMapper) {
        return new JSONTypeDeserializer() {
            @Override
            protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
                Object value = ANY.deserialize(charSource, buf, fromIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
                return typeMapper.readOf(value);
            }

            @Override
            protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
                Object value = ANY.deserialize(charSource, bytes, fromIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
                return typeMapper.readOf(value);
            }
        };
    }

    static {
        if (EnvUtils.JDK_8_PLUS) {
            try {
                Class.forName("io.github.wycst.wast.json.JSONTemporalExtension");
            } catch (ClassNotFoundException e) {
            }
        }
    }
}
