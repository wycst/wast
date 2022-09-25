/*
 * Copyright [2020-2022] [wangyunchao]
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
import io.github.wycst.wast.common.beans.UTF16ByteArraySource;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
 * @see JSON#toJsonString(Object, JsonConfig)
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
 * @see JSONStringWriter
 * @see GenericParameterizedType
 */
@SuppressWarnings("rawtypes")
public final class JSON extends JSONGeneral {

    public static String FLAG = "0.0.7";

    /**
     * 将json字符串转为对象或者数组(Convert JSON strings to objects or arrays)
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
     * 将json字符串转化为指定map
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
        return JSONDefaultParser.parseBytes(bytes, readOptions);
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
        if (JDK_9_ABOVE) {
            int code = UnsafeHelper.getStringCoder(json);
            if (code == 0) {
                if(json.length() < DIRECT_READ_BUFFER_SIZE) {
                    // Small text uses chars
                    return parse(null, getChars(json), actualType, null, readOptions);
                }
                CharSource charSource = AsciiStringSource.of(json);
                return parse(charSource, charSource.byteArray(), actualType, null, readOptions);
            } else {
                // utf16
                char[] chars = getChars(json);
                return parse(UTF16ByteArraySource.of(json, chars), chars, actualType, null, readOptions);
            }
        }
        return parse(null, getChars(json), actualType, null, readOptions);
    }

    /**
     * 将json字符串（{}）转化为指定class的实例
     * <p> 去除不可见字符后以{开始
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
        if (typeDeserializer == JSONTypeDeserializer.MAP) {
            Object map = JSONDefaultParser.parseMap(json, (Class<? extends Map>) actualType, readOptions);
            return (T) map;
        }

        if (JDK_9_ABOVE) {
            int code = UnsafeHelper.getStringCoder(json);
            if (code == 0) {
                // use ascii bytes
                if(json.length() < DIRECT_READ_BUFFER_SIZE) {
                    // Small text uses chars
                    return parseObject(typeDeserializer, null, getChars(json), actualType, readOptions);
                }
                CharSource charSource = AsciiStringSource.of(json);
                return parseObject(typeDeserializer, charSource, charSource.byteArray(), actualType, readOptions);
            } else {
                // utf16
                char[] chars = getChars(json);
                return parseObject(typeDeserializer, UTF16ByteArraySource.of(json, chars), chars, actualType, readOptions);
            }
        }

        return parseObject(typeDeserializer, null, getChars(json), actualType, readOptions);
    }

    /**
     * 将字符数组转化为指定class的实例
     * <p> 去除不可见字符后以字符{开始
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
     * 将字节数组转化为指定class的实例
     * <p> 去除不可见字符后以字符{开始
     *
     * @param buf         字节数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @param <T>         泛型
     * @return 类型对象
     */
    public static <T> T parseObject(byte[] buf, final Class<T> actualType, ReadOption... readOptions) {
        JSONTypeDeserializer typeDeserializer = JSONTypeDeserializer.getTypeDeserializer(actualType);
        return parseObject(typeDeserializer, null, buf, actualType, readOptions);
    }

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
     */
    public static <T> T parseObject(String json, GenericParameterizedType<T> genericParameterizedType, ReadOption... readOptions) {
        if (json == null) return null;
        return parseObject(getChars(json), genericParameterizedType, readOptions);
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
        if (JDK_9_ABOVE) {
            int code = UnsafeHelper.getStringCoder(json);
            if (code == 1) {
                // utf16
                char[] chars = getChars(json);
                return parse(UTF16ByteArraySource.of(json, chars), chars, genericParameterizedType, readOptions);
            }
        }
        return parse(getChars(json), genericParameterizedType, readOptions);
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
     * 解析集合
     *
     * @param json        源字符串
     * @param actualType  类型
     * @param readOptions 解析配置项
     * @return 集合
     */
    public static <T> List<T> parseArray(String json, Class<T> actualType, ReadOption... readOptions) {
        if (json == null) return null;
        if (JDK_9_ABOVE) {
            int code = UnsafeHelper.getStringCoder(json);
            if (code == 1) {
                // utf16
                char[] chars = getChars(json);
                return parseArray(UTF16ByteArraySource.of(json, chars), chars, actualType, readOptions);
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
        return parse(null, buf, actualType, null, readOptions);
//        if (JDK_9_ABOVE) {
//            return parse(null, buf, actualType, null, readOptions);
//        }
//        return parse(new String(buf), actualType, readOptions);
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
        if (JDK_9_ABOVE) {
            int code = UnsafeHelper.getStringCoder(json);
            if (code == 1) {
                // utf16
                char[] chars = getChars(json);
                return parseToObject(UTF16ByteArraySource.of(json, chars), chars, instance, readOptions);
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
        if (JDK_9_ABOVE) {
            int code = UnsafeHelper.getStringCoder(json);
            if (code == 1) {
                // utf16
                char[] chars = getChars(json);
                return parseToList(UTF16ByteArraySource.of(json, chars), chars, instance, actualType, readOptions);
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
            fromIndex++;
        }
        while ((toIndex > fromIndex) && buf[toIndex - 1] <= ' ') {
            toIndex--;
        }

        JSONParseContext jsonParseContext = new JSONParseContext();
        Options.readOptions(readOptions, jsonParseContext);
        try {
            boolean allowComment = jsonParseContext.isAllowComment();
            if (allowComment && beginChar == '/') {
                /** 去除声明在头部的注释*/
                fromIndex = clearCommentAndWhiteSpaces(buf, fromIndex + 1, toIndex, jsonParseContext);
            }
            Object result = deserializer.deserialize(buf, fromIndex, toIndex, jsonParseContext);
            int endIndex = jsonParseContext.getEndIndex();
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
            if (ex instanceof JSONException) {
                throw (JSONException) ex;
            }
            // There is only one possibility to control out of bounds exceptions when indexing toindex
            if (ex instanceof IndexOutOfBoundsException) {
                String errorContextTextAt = createErrorContextText(buf, toIndex);
                throw new JSONException("Syntax error, context text by '" + errorContextTextAt + "', JSON format error, and the end token may be missing, such as '\"' or ', ' or '}' or ']'.");
            }
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
        Options.readOptions(readOptions, jsonParseContext);
        try {
            boolean allowComment = jsonParseContext.isAllowComment();
            if (allowComment && beginByte == '/') {
                fromIndex = JSONByteArrayParser.clearComments(buf, fromIndex + 1, toIndex, jsonParseContext);
            }
            Object result = deserializer.deserialize(buf, fromIndex, toIndex, jsonParseContext);
            int endIndex = jsonParseContext.getEndIndex();
            if (allowComment) {
                /** 去除声明在尾部的注释*/
                if (endIndex < toIndex - 1) {
                    byte commentStart = '\0';
                    while (endIndex + 1 < toIndex && (commentStart = buf[++endIndex]) <= ' ') ;
                    if (commentStart == '/') {
                        endIndex = JSONByteArrayParser.clearComments(buf, endIndex + 1, toIndex, jsonParseContext);
                    }
                }
            }

            if (endIndex != toIndex - 1) {
                int wordNum = Math.min(50, buf.length - endIndex);
                throw new JSONException("Syntax error, at pos " + endIndex + " extra characters found, '" + new String(buf, endIndex, wordNum) + " ...'");
            }

            return result;
        } catch (Exception ex) {
            if (ex instanceof JSONException) {
                throw (JSONException) ex;
            }
            // There is only one possibility to control out of bounds exceptions when indexing toindex
            if (ex instanceof IndexOutOfBoundsException) {
                String errorContextTextAt = JSONByteArrayParser.createErrorMessage(buf, toIndex);
                throw new JSONException("Syntax error, context text by '" + errorContextTextAt + "', JSON format error, and the end token may be missing, such as '\"' or ', ' or '}' or ']'.");
            }
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
        return JSONDefaultParser.parseBytes(bytes, readOptions);
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
        return (T) parse(null, bytes, actualType, null, readOptions);
        // return (T) parse(getChars(new String(bytes)), actualType, readOptions);
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
        return toJsonString(obj, WriteOption.Default);
    }

    /**
     * 将对象转化为json字符串
     *
     * @param obj 目标对象
     * @return JSON字符串
     */
    public static String toJsonString(Object obj, WriteOption... options) {
        if (obj == null) {
            return null;
        }
        JsonConfig jsonConfig = new JsonConfig();
        Options.writeOptions(options, jsonConfig);
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
        if (obj == null) {
            return null;
        }
        String json = toJsonString(obj, options);
        return json.getBytes();
    }

    /**
     * serialize obj
     *
     * @param obj        序列化对象
     * @param jsonConfig 配置
     * @return JSON字符串
     */
    public static String toJsonString(Object obj, JsonConfig jsonConfig) {
        if (obj == null) {
            return null;
        }
        return stringify(obj, jsonConfig, 0);
    }

    /**
     * serialize obj
     *
     * @param obj        序列化对象
     * @param jsonConfig 配置
     * @return JSON字符串
     */
    public static String stringify(Object obj, JsonConfig jsonConfig) {
        if (obj == null) {
            return null;
        }
        return stringify(obj, jsonConfig, 0);
    }

    private static String stringify(Object obj, JsonConfig jsonConfig, int indentLevel) {
        JSONStringWriter content = new JSONStringWriter();
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
     */
    public static void writeJsonTo(Object object, File file, WriteOption... options) {
        try {
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            writeJsonTo(object, new FileOutputStream(file), options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将对象序列化内容直接写入os
     *
     * @param object
     * @param os
     */
    public static void writeJsonTo(Object object, OutputStream os, WriteOption... options) {
        writeJsonTo(object, new OutputStreamWriter(os), options);
    }

    /**
     * 将对象序列化内容使用指定的writer写入
     *
     * @param object
     * @param writer
     */
    public static void writeJsonTo(Object object, Writer writer, WriteOption... options) {
        if (object == null) return;
        BufferedWriter bufferedWriter;
        if (writer instanceof BufferedWriter) {
            bufferedWriter = (BufferedWriter) writer;
        } else {
            bufferedWriter = new BufferedWriter(writer);
        }

        JsonConfig jsonConfig = new JsonConfig();
        Options.writeOptions(options, jsonConfig);
        try {
            stringify(object, bufferedWriter, jsonConfig, 0);
            bufferedWriter.flush();
        } catch (Exception e) {
            throw new JSONException(e);
        } finally {
            jsonConfig.clear();
            if (jsonConfig.isAutoCloseStream()) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // obj is not null
    static void stringify(Object obj, Writer content, JsonConfig jsonConfig, int indentLevel) throws Exception {
        JSONTypeSerializer.getValueSerializer(obj).serialize(obj, content, jsonConfig, indentLevel);
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

}
