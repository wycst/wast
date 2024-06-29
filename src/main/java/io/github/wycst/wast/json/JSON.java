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

import io.github.wycst.wast.common.beans.AsciiStringSource;
import io.github.wycst.wast.common.beans.CharSource;
import io.github.wycst.wast.common.beans.ISO_8859_1CharSource;
import io.github.wycst.wast.common.beans.UTF16ByteArraySource;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.EnvUtils;
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
@SuppressWarnings("rawtypes")
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
                return JSONDefaultParser.parse(AsciiStringSource.of(bytes), bytes, null, readOptions);
            } else {
                return JSONDefaultParser.parse(ISO_8859_1CharSource.of(bytes), bytes, null, readOptions);
            }
        }
        return JSONDefaultParser.parse(bytes, readOptions);
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
            byte[] bytes = (byte[]) UnsafeHelper.getStringValue(json);
            if (bytes.length == json.length()) {
                AsciiStringSource charSource = AsciiStringSource.of(json, bytes);
                return parse(charSource, bytes, actualType, null, readOptions);
            } else {
                // utf16
                char[] chars = json.toCharArray();
                return parse(UTF16ByteArraySource.of(json), chars, actualType, null, readOptions);
            }
        }
        return parse(null, getChars(json), actualType, null, readOptions);
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
        // if use JSONDefaultParser.parse
        JSONTypeDeserializer typeDeserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) UnsafeHelper.getStringValue(json);
            if (bytes.length == json.length()) {
                // use ascii bytes
                AsciiStringSource charSource = new AsciiStringSource(json, bytes);
                return parseObject(typeDeserializer, charSource, bytes, actualType, readOptions);
            } else {
                // utf16
                char[] chars = json.toCharArray();
                return parseObject(typeDeserializer, UTF16ByteArraySource.of(json), chars, actualType, readOptions);
            }
        }

        return parseObject(typeDeserializer, null, getChars(json), actualType, readOptions);
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
        return parseObject(typeDeserializer, null, buf, actualType, readOptions);
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
                return parseObject(typeDeserializer, AsciiStringSource.of(buf), buf, actualType, readOptions);
            } else {
                return parseObject(typeDeserializer, ISO_8859_1CharSource.of(buf), buf, actualType, readOptions);
            }
        }
        return parseObject(typeDeserializer, null, buf, actualType, readOptions);
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

//    public static <T> T parse(char[] buf, Type type, ReadOption... readOptions) {
//        return null;
//    }
//
//    public static <T> T parse(byte[] buf, Type type, ReadOption... readOptions) {
//        return null;
//    }

    private static <T> T parseObject(final JSONTypeDeserializer deserializer, final CharSource charSource, char[] buf, final Class<T> actualType, ReadOption[] readOptions) {
        return (T) deserialize(buf, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
                GenericParameterizedType<T> genericParameterizedType = deserializer.getGenericParameterizedType();
                if (genericParameterizedType == null) {
                    genericParameterizedType = GenericParameterizedType.actualType(actualType);
                }
                return deserializer.deserialize(charSource, buf, fromIndex, toIndex, genericParameterizedType, null, '\0', jsonParseContext);
            }
        }, readOptions);
    }

    private static <T> T parseObject(final JSONTypeDeserializer deserializer, final CharSource charSource, byte[] buf, final Class<T> actualType, ReadOption[] readOptions) {
        return (T) deserialize(buf, new Deserializer() {
            Object deserialize(byte[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
                GenericParameterizedType<T> genericParameterizedType = deserializer.getGenericParameterizedType();
                if (genericParameterizedType == null) {
                    genericParameterizedType = GenericParameterizedType.actualType(actualType);
                }
                return deserializer.deserialize(charSource, buf, fromIndex, toIndex, genericParameterizedType, null, (byte) 0, jsonParseContext);
            }
        }, readOptions);
    }


    /**
     * 将JSON字符串转化为指定Type的实例
     * <p> 去除不可见字符后以字符{开始
     *
     * @param json                     JSON字符串
     * @param genericParameterizedType 泛型结构
     * @param readOptions              读取配置
     * @param <T>                      泛型类型
     * @return 对象
     * @see JSON#parse(String, GenericParameterizedType, ReadOption...)
     */
    public static <T> T parseObject(String json, GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        return parse(json, genericParameterizedType, readOptions);
    }

    /**
     * 将字符数组转化为指定class的实例
     * <p> 去除不可见字符后以字符{开始
     *
     * @param buf                      json字符数组
     * @param genericParameterizedType 泛型结构
     * @param readOptions              读取配置
     * @param <T>                      泛型类型
     * @return 对象
     */
    public static <T> T parseObject(char[] buf, final GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        return parse(buf, genericParameterizedType, readOptions);
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
            byte[] bytes = (byte[]) UnsafeHelper.getStringValue(json);
            if (bytes.length == json.length()) {
                return parse(AsciiStringSource.of(json, bytes), bytes, genericParameterizedType, readOptions);
            } else {
                // utf16
                char[] chars = json.toCharArray();
                return parse(UTF16ByteArraySource.of(json), chars, genericParameterizedType, readOptions);
            }
        } else {
            return parseObject(getChars(json), genericParameterizedType, readOptions);
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
        return parse(null, buf, genericParameterizedType, readOptions);
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
                return parse(AsciiStringSource.of(buf), buf, genericParameterizedType, readOptions);
            } else {
                return parse(ISO_8859_1CharSource.of(buf), buf, genericParameterizedType, readOptions);
            }
        }
        return parse(null, buf, genericParameterizedType, readOptions);
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
    private static <T> T parse(final CharSource charSource, char[] buf, final GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        return (T) deserialize(buf, new Deserializer() {

            Object deserialize(char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
                ReflectConsts.ClassCategory classCategory = genericParameterizedType.getActualClassCategory();
                return JSONTypeDeserializer.TYPE_DESERIALIZERS[classCategory.ordinal()].deserialize(charSource, buf, fromIndex, toIndex, genericParameterizedType, null, '\0', jsonParseContext);
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
    private static <T> T parse(final CharSource charSource, byte[] buf, final GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        return (T) deserialize(buf, new Deserializer() {
            Object deserialize(byte[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
                ReflectConsts.ClassCategory classCategory = genericParameterizedType.getActualClassCategory();
                return JSONTypeDeserializer.TYPE_DESERIALIZERS[classCategory.ordinal()].deserialize(charSource, buf, fromIndex, toIndex, genericParameterizedType, null, (byte) 0, jsonParseContext);
            }
        }, readOptions);
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
            int code = UnsafeHelper.getStringCoder(json);
            if (code == 1) {
                // utf16
                char[] chars = getChars(json);
                return parseArray(UTF16ByteArraySource.of(json), chars, actualType, readOptions);
            }
        }
        return parseArray(getChars(json), actualType, readOptions);
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
        return parseArray(null, buf, actualType, readOptions);
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
    private static <T> List<T> parseArray(final CharSource charSource, char[] buf, final Class<T> actualType, ReadOption... readOptions) {
        return (List<T>) deserialize(buf, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
                return JSONTypeDeserializer.COLLECTION.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.collectionType(ArrayList.class, actualType), null, '\0', jsonParseContext);
            }
        }, readOptions);
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
        return parse(null, buf, 0, buf.length, actualType, null, readOptions);
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
        return parse(null, buf, fromIndex, toIndex, actualType, null, readOptions);
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
                return parse(AsciiStringSource.of(buf), buf, actualType, null, readOptions);
            } else {
                return parse(ISO_8859_1CharSource.of(buf), buf, actualType, null, readOptions);
            }
        }
        return parse(null, buf, actualType, null, readOptions);
    }

    /**
     * 解析buf字符数组，返回对象或者集合类型
     *
     * @param charSource   源
     * @param buf          字符数组
     * @param actualType   类型
     * @param defaultValue 默认值
     * @param readOptions  读取配置
     * @return 对象
     */
    private static Object parse(final CharSource charSource, char[] buf, final Class<?> actualType, final Object defaultValue, ReadOption... readOptions) {
        return parse(charSource, buf, 0, buf.length, actualType, defaultValue, readOptions);
    }

    // 基于偏移位置和长度解析
    private static Object parse(final CharSource charSource, char[] buf, int fromIndex, int toIndex, final Class<?> actualType, final Object defaultValue, ReadOption... readOptions) {
        return deserialize(buf, fromIndex, toIndex, new Deserializer() {

            Object deserialize(char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
                char beginChar = buf[fromIndex];
                switch (beginChar) {
                    case '[':
                        return JSONTypeDeserializer.COLLECTION.deserializeCollection(charSource, buf, fromIndex, toIndex, GenericParameterizedType.collectionType(ArrayList.class, actualType), defaultValue == null ? new ArrayList() : defaultValue, jsonParseContext);
                    default: {
                        JSONTypeDeserializer typeDeserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
                        GenericParameterizedType type = typeDeserializer.getGenericParameterizedType();
                        if (type == null) {
                            type = GenericParameterizedType.actualType(actualType);
                        }
                        return typeDeserializer.deserialize(charSource, buf, fromIndex, toIndex, type, defaultValue, '\0', jsonParseContext);
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
    private static Object parse(final CharSource charSource, byte[] buf, final Class<?> actualType, final Object defaultValue, ReadOption... readOptions) {
        return deserialize(buf, new Deserializer() {

            Object deserialize(byte[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
                byte beginByte = buf[fromIndex];
                switch (beginByte) {
                    case '[':
                        return JSONTypeDeserializer.COLLECTION.deserializeCollection(charSource, buf, fromIndex, toIndex, GenericParameterizedType.collectionType(ArrayList.class, actualType), defaultValue == null ? new ArrayList() : defaultValue, jsonParseContext);
                    default: {
                        JSONTypeDeserializer typeDeserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
                        GenericParameterizedType type = typeDeserializer.getGenericParameterizedType();
                        if (type == null) {
                            type = GenericParameterizedType.actualType(actualType);
                        }
                        return typeDeserializer.deserialize(charSource, buf, fromIndex, toIndex, type, defaultValue, (byte) '\0', jsonParseContext);
                    }
                }
            }
        }, readOptions);
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
            int code = UnsafeHelper.getStringCoder(json);
            if (code == 1) {
                // utf16
                char[] chars = getChars(json);
                return parseToObject(UTF16ByteArraySource.of(json), chars, instance, readOptions);
            }
        }
        return parseToObject(null, getChars(json), instance, readOptions);
    }

    private static Object parseToObject(final CharSource charSource, char[] buf, final Object instance, ReadOption... readOptions) {
        return deserialize(buf, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
                if (instance instanceof Map) {
                    return JSONTypeDeserializer.MAP.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.DefaultMap, instance, '\0', jsonParseContext);
                }
                return JSONTypeDeserializer.OBJECT.deserializeObject(charSource, buf, fromIndex, toIndex, GenericParameterizedType.actualType(instance.getClass()), instance, jsonParseContext);
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
    public static <E> Object parseToList(String json, final Collection instance, final Class<E> actualType, ReadOption... readOptions) {
        if (instance == null || json == null) {
            return null;
        }
        if (EnvUtils.JDK_9_PLUS) {
            int code = UnsafeHelper.getStringCoder(json);
            if (code == 1) {
                // utf16
                char[] chars = getChars(json);
                return parseToList(UTF16ByteArraySource.of(json), chars, instance, actualType, readOptions);
            }
        }
        return parseToList(null, getChars(json), instance, actualType, readOptions);
    }

    private static <E> Object parseToList(final CharSource charSource, char[] buf, final Collection instance, final Class<E> actualType, ReadOption... readOptions) {
        return deserialize(buf, new Deserializer() {
            Object deserialize(char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
                return JSONTypeDeserializer.COLLECTION.deserializeCollection(charSource, buf, fromIndex, toIndex, GenericParameterizedType.collectionType(instance.getClass(), actualType), instance, jsonParseContext);
            }
        }, readOptions);
    }

    /***
     * 本地反序列化器提取代码
     */
    abstract static class Deserializer {
        Object deserialize(char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
            throw new UnsupportedOperationException();
        }

        Object deserialize(byte[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
            throw new UnsupportedOperationException();
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
    private static Object deserialize(char[] buf, Deserializer deserializer, ReadOption... readOptions) {
        return deserialize(buf, 0, buf.length, deserializer, readOptions);
    }

    private static Object deserialize(char[] buf, int fromIndex, int toIndex, Deserializer deserializer, ReadOption... readOptions) {
        // Trim remove white space characters
        char beginChar = '\0';
        while ((fromIndex < toIndex) && (beginChar = buf[fromIndex]) <= ' ') {
            ++fromIndex;
        }
        while ((toIndex > fromIndex) && buf[toIndex - 1] <= ' ') {
            --toIndex;
        }

        JSONParseContext jsonParseContext = new JSONParseContext();
        JSONOptions.readOptions(readOptions, jsonParseContext);
        try {
            boolean allowComment = jsonParseContext.allowComment;
            if (allowComment && beginChar == '/') {
                /** 去除声明在头部的注释*/
                fromIndex = clearCommentAndWhiteSpaces(buf, fromIndex + 1, toIndex, jsonParseContext);
            }
            Object result = deserializer.deserialize(buf, fromIndex, toIndex, jsonParseContext);
            int endIndex = jsonParseContext.endIndex;
            if (allowComment) {
                /** 去除声明在尾部的注释*/
                if (endIndex < toIndex - 1) {
                    char commentStart = '\0';
                    while (endIndex + 1 < toIndex && (commentStart = buf[++endIndex]) <= ' ') ;
                    if (commentStart == '/') {
                        endIndex = clearCommentAndWhiteSpaces(buf, endIndex + 1, toIndex, jsonParseContext);
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
            jsonParseContext.clear();
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
    private static Object deserialize(byte[] buf, Deserializer deserializer, ReadOption... readOptions) {
        int fromIndex = 0;
        int toIndex = buf.length;
        // Trim remove white space characters
        byte beginByte = '\0';
        while ((fromIndex < toIndex) && (beginByte = buf[fromIndex]) <= ' ') {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && buf[toIndex - 1] <= ' ') {
            toIndex--;
        }

        JSONParseContext jsonParseContext = new JSONParseContext();
        JSONOptions.readOptions(readOptions, jsonParseContext);
        try {
            boolean allowComment = jsonParseContext.allowComment;
            if (allowComment && beginByte == '/') {
                fromIndex = clearCommentAndWhiteSpaces(buf, fromIndex + 1, toIndex, jsonParseContext);
            }
            Object result = deserializer.deserialize(buf, fromIndex, toIndex, jsonParseContext);
            int endIndex = jsonParseContext.endIndex;
            if (allowComment) {
                /** 去除声明在尾部的注释*/
                if (endIndex < toIndex - 1) {
                    byte commentStart = '\0';
                    while (endIndex + 1 < toIndex && (commentStart = buf[++endIndex]) <= ' ') ;
                    if (commentStart == '/') {
                        endIndex = clearCommentAndWhiteSpaces(buf, endIndex + 1, toIndex, jsonParseContext);
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
            jsonParseContext.clear();
        }
    }

    /**
     * 解析日期
     *
     * @param dateStr
     * @param dateCls
     * @return
     */
    public final static Date parseDate(String dateStr, Class<? extends Date> dateCls) {
        char[] buf = getChars(dateStr);
        return matchDate(buf, 0, dateStr.length(), null, dateCls);
    }

    /***
     * 读取字节数组返回Map对象或者List集合
     *
     * @param bytes utf编码
     * @param readOptions
     * @return
     */
    public static Object read(byte[] bytes, ReadOption... readOptions) {
        if (bytes == null) return null;
        return JSONDefaultParser.parse(bytes, readOptions);
    }

    /***
     * 读取流返回Map对象或者List集合
     *
     * @param is
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
            char[] buf = readInputStream(is, (int) size);
            return parse(buf, readOptions);
        } else {
            JSONReader jsonReader = new JSONReader(is);
            jsonReader.setOptions(readOptions);
            return jsonReader.read();
        }
    }

    /**
     * 将字节数组转化为指定class的实例
     *
     * @param bytes       字节流
     * @param actualType  实体类型
     * @param readOptions 解析配置项
     * @return T对象
     */
    public static <T> T read(byte[] bytes, Class<T> actualType, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.hasNegatives(bytes, 0, bytes.length)) {
                return (T) parse(AsciiStringSource.of(bytes), bytes, actualType, null, readOptions);
            } else {
                return (T) parse(ISO_8859_1CharSource.of(bytes), bytes, actualType, null, readOptions);
            }
        }
        return (T) parse(null, bytes, actualType, null, readOptions);
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
            char[] buf = readInputStream(is, (int) size);
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
            stringify(obj, content, jsonConfig, indentLevel);
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
            writeJsonTo(object, new FileOutputStream(file), options);
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

//    /**
//     * 将对象序列化内容直接写入os
//     *
//     * @param object
//     * @param os
//     * @param options
//     */
//    public static void writeToStream(Object object, OutputStream os, WriteOption... options) {
//        JSONConfig jsonConfig = JSONConfig.config(options);
//        JSONWriter streamWriter = JSONWriter.forBytesWriter(EnvUtils.CHARSET_DEFAULT, jsonConfig);
//        try {
//            writeToJSONWriter(object, streamWriter, jsonConfig);
//            streamWriter.toOutputStream(os);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } finally {
//            streamWriter.reset();
//        }
//    }

    /**
     * <p>
     * 将对象序列化内容使用指定的writer写入比如BufferedWriter或者OutputStreamWriter；<br/>
     * 此方法内部实现中没有缓冲buf对象，序列化每个字段都会实时写入writer；<br/>
     *
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
        writeToJSONWriter(object, JSONWriter.wrap(writer), JSONConfig.config(options));
    }

    /**
     * 将对象序列化内容使用指定的writer写入
     *
     * @param object
     * @param writer
     * @param jsonConfig
     */
    static void writeToJSONWriter(Object object, JSONWriter writer, JSONConfig jsonConfig) {
        if (object != null) {
            try {
                stringify(object, writer, jsonConfig, 0);
                writer.flush();
            } catch (Exception e) {
                throw new JSONException(e);
            } finally {
                jsonConfig.clear();
                if (jsonConfig.isAutoCloseStream()) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // obj is not null
    static void stringify(Object obj, JSONWriter content, JSONConfig jsonConfig, int indentLevel) throws Exception {
        JSONTypeSerializer serializer = JSONTypeSerializer.getTypeSerializer(obj.getClass());
        serializer.serialize(obj, content, jsonConfig, indentLevel);
    }

    public static boolean validate(String json, ReadOption... readOptions) {
        return validate(json, false, readOptions);
    }

    public static boolean validate(String json, boolean printIfException, ReadOption... readOptions) {
        if (json == null) return false;
        return validate(getChars(json), printIfException, readOptions);
    }

    public static boolean validate(char[] buf, ReadOption... readOptions) {
        return validate(buf, false, readOptions);
    }

    public static boolean validate(char[] buf, boolean printIfException, ReadOption... readOptions) {
        JSONValidator jsonValidator = new JSONValidator(buf);
        boolean result = jsonValidator.validate(readOptions);
        if (printIfException) {
        }
        return result;
    }

    /**
     * <p> 校验json字符串是否正确，
     *
     * @param json
     * @param readOptions
     * @return <p> 如果正确，返回null; 否则返回错误信息
     */
    public static String validateMessage(String json, ReadOption... readOptions) {
        if (json == null) return "Exception: java.lang.NullPointerException";
        return validateMessage(getChars(json), readOptions);
    }

    /**
     * <p> 校验json字符串是否正确，
     *
     * @param buf
     * @param readOptions
     * @return <p> 如果正确，返回null; 否则返回错误信息
     */
    public static String validateMessage(char[] buf, ReadOption... readOptions) {
        JSONValidator jsonValidator = new JSONValidator(buf);
        jsonValidator.validate(true, readOptions);
        return jsonValidator.getValidateMessage();
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
                JSONTypeDeserializer.putTypeDeserializer(new JSONTypeDeserializer() {
                    @Override
                    protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
                        Object value = JSONTypeDeserializer.ANY.deserialize(charSource, buf, fromIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
                        return mapper.read(value);
                    }

                    @Override
                    protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
                        Object value = JSONTypeDeserializer.ANY.deserialize(charSource, bytes, fromIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
                        return mapper.read(value);
                    }
                }, type);
            }
            if (JSONTypeSerializer.isBuiltInType(type)) {
                if (!ignoreIfExist) {
                    throw new UnsupportedOperationException("Existing built-in implementation, does not support overwrite for class " + type);
                }
            } else {
                // serializer
                JSONTypeSerializer.putTypeSerializer(new JSONTypeSerializer() {
                    @Override
                    protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                        mapper.write(writer, (T) value, jsonConfig, indent);
                    }
                }, type);
            }
        }
    }

}
