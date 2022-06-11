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

import io.github.wycst.wast.common.annotations.Property;
import io.github.wycst.wast.common.reflect.*;
import io.github.wycst.wast.common.tools.Base64;
import io.github.wycst.wast.json.annotations.JsonDeserialize;
import io.github.wycst.wast.json.annotations.JsonSerialize;
import io.github.wycst.wast.json.custom.JsonDeserializer;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Time;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
 * @Author wangyunchao
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
 * @see Property
 * @see JSONNode
 * @see JSONReader
 * @see JSONWriter
 * @see GenericParameterizedType
 */
public final class JSON extends JSONGeneral {

    /**
     * 将json字符串转为对象或者数组(Convert JSON strings to objects or arrays)
     *
     * <p> 如果以{开头的json将解析为LinkHashMap实例 (If the JSON starts with {, it will resolve to a LinkHashMap instance)
     * <p> 如果以[开头的json将解析为ArrayList实例  (If the JSON starts with [, it will resolve to a ArrayList instance)
     * <p> 如果以"开头的json将解析为字符串对象 (If the JSON starts with ", it will resolve to a String )
     *
     * @param json        source
     * @param readOptions 解析配置项
     * @return object/array/string
     */
    public static Object parse(String json, ReadOption... readOptions) {
        return JSONDefaultParser.parse(json, readOptions);
    }

    /**
     * 将json字符串转化为指定class的实例
     * <p>
     * 源字符串json是{}则返回clazz该类的对象，
     * <p>
     * 如果是[]集合,则返回集合，范型是clazz
     *
     * @param json        json字符串
     * @param actualType  类型（type）
     * @param readOptions 解析配置项
     * @return object/array/collection
     */
    public static Object parse(String json, Class<?> actualType, ReadOption... readOptions) {
        if (json == null)
            return null;
        try {
            char[] buf = getChars(json);
            return parseBuffers(buf, null, actualType, null, readOptions);
        } catch (Exception ex) {
            if (ex instanceof JSONException) {
                throw (JSONException) ex;
            }
            throw new JSONException("Error: " + ex.getMessage(), ex);
        }
    }

    /**
     * 将json字符串转化为指定class的实例
     *
     * @param json        源字符串
     * @param actualType  类型
     * @param readOptions 解析配置项
     * @return T对象
     */
    public static <T> T parseObject(String json, Class<T> actualType, ReadOption... readOptions) {
        if (actualType == null || actualType == Map.class || actualType == LinkedHashMap.class) {
            return (T) JSONDefaultParser.parse(json, readOptions);
        }
        return (T) parse(json, actualType, readOptions);
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
        return (T) parseBuffers(getChars(json), genericParameterizedType, null, readOptions);
    }

    /***
     * 读取字节数组返回Map对象或者List集合
     *
     * @param bytes
     * @param readOptions
     * @return
     */
    public static Object read(byte[] bytes, ReadOption... readOptions) {
        if (bytes == null) return null;
        return parse(new String(bytes), readOptions);
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
            return JSONDefaultParser.parse(buf, readOptions);
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
//        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//        try {
//            return read(bais, bytes.length, actualType, readOptions);
//        } catch (IOException e) {
//        }
//        return null;
        return (T) parse(getChars(new String(bytes)), actualType, readOptions);
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
            if(size == 0) return null;
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
     * 返回集合
     *
     * @param json        源字符串
     * @param actualType  类型
     * @param readOptions 解析配置项
     * @return 集合
     */
    public static <T> List<T> parseArray(String json, Class<T> actualType, ReadOption... readOptions) {
        return (List<T>) parse(json, actualType, readOptions);
    }

    /**
     * 解析buf字符数组，返回对象或者集合类型
     *
     * @param buf         字符数组
     * @param actualType  类型
     * @param readOptions 解析配置项
     * @return 对象或集合
     */
    public static Object parse(char[] buf, Class<?> actualType, ReadOption... readOptions) {
        if (buf == null || buf.length == 0)
            return null;
        return parseBuffers(buf, null, actualType, null, readOptions);
    }

    /**
     * 将json解析到指定实例对象中
     *
     * @param json        json字符串
     * @param instance    在外部构造的实例对象中（避免创建对象的反射开销）
     * @param readOptions 解析配置项
     * @return instance 返回
     */
    public static Object parseToObject(String json, Object instance, ReadOption... readOptions) {
        if (instance == null) {
            return null;
        }
        parseBuffers(getChars(json), null, instance.getClass(), instance, readOptions);
        return instance;
    }

    /**
     * 解析目标json到指定泛型类型的集合对象
     *
     * @param json        json字符串
     * @param instance    集合类型
     * @param actualType  泛型类型
     * @param readOptions 解析配置项
     * @param <E>
     * @return
     */
    public static <E> Object parseToList(String json, Collection instance, Class<E> actualType, ReadOption... readOptions) {
        if (instance == null) {
            return null;
        }
        parseBuffers(getChars(json), instance.getClass(), actualType, instance, readOptions);
        return instance;
    }

    private static Object parseBuffers(char[] buf, final GenericParameterizedType genericParameterizedType, final Object instance, ReadOption... readOptions) {
        return executeParseBuffers(buf, new LocalBufferParser() {

            public Object parse(char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
                Object result;
                char beginChar = buf[fromIndex];
                switch (beginChar) {
                    case '{':
                        result = parseObjectOfBuffers(fromIndex, toIndex, buf, genericParameterizedType, instance, true, jsonParseContext);
                        break;
                    case '[':
                        result = parseArrayOfBuffers(fromIndex, toIndex, buf, genericParameterizedType, instance, true, jsonParseContext);
                        break;
                    case '"':
                        result = parseStringOfBuffers(fromIndex, toIndex, buf, genericParameterizedType, jsonParseContext);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported for begin character with '" + beginChar + "'");
                }
                return result;
            }
        }, readOptions);
    }

    private static Object executeParseBuffers(char[] buf, LocalBufferParser parser, ReadOption... readOptions) {
        int fromIndex = 0;
        int toIndex = buf.length;
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

            Object result = parser.parse(buf, fromIndex, toIndex, jsonParseContext);
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
                int wordNum = Math.min(50, buf.length - endIndex - 1);
                throw new JSONException("Syntax error, at pos " + endIndex + " extra characters found, '" + new String(buf, endIndex + 1, wordNum) + " ...'");
            }
            return result;
        } catch (Exception ex) {
            if (ex instanceof JSONException) {
                throw (JSONException) ex;
            }
            throw new JSONException("Error: " + ex.getMessage(), ex);
        } finally {
            jsonParseContext.clear();
        }
    }

    private static Object parseBuffers(char[] buf, final Class<? extends Collection> collectionCls, final Class<?> clazz, final Object instance, ReadOption... readOptions) {
        return executeParseBuffers(buf, new LocalBufferParser() {
            @Override
            public Object parse(char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
                Object result;
                char beginChar = buf[fromIndex];
                switch (beginChar) {
                    case '{':
                        result = parseObjectOfBuffers(fromIndex, toIndex, buf, GenericParameterizedType.actualType(clazz), instance, true, jsonParseContext);
                        break;
                    case '[':
                        result = parseArrayOfBuffers(fromIndex, toIndex, buf, GenericParameterizedType.collectionType(collectionCls, clazz), instance, true, jsonParseContext);
                        break;
                    case '"':
                        Class<?> actualType = clazz == null ? String.class : clazz;
                        result = parseStringOfBuffers(fromIndex, toIndex, buf, GenericParameterizedType.actualType(actualType), jsonParseContext);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported for begin character with '" + beginChar + "'");
                }
                return result;
            }
        }, readOptions);
    }

    /**
     * 解析对象，以{开始，直到遇到}结束 （Resolve the object, starting with '{', until '}' is encountered）
     *
     * @param fromIndex                开始位置（Start index of '{'）
     * @param toIndex                  最大索引位置（Maximum end index）
     * @param buf                      缓冲数组（char[]）
     * @param genericParameterizedType 类型结构
     * @param instance                 实例对象（Entity Class）
     * @param deserialize              是否反序列化
     * @param jsonParseContext         上下文配置（context config）
     * @param <T>                      泛型类型（actualType）
     * @return 对象（object）
     * @throws Exception 异常(Exception)
     */
    private static <T> T parseObjectOfBuffers(int fromIndex, int toIndex, char[] buf, GenericParameterizedType genericParameterizedType, Object instance, boolean deserialize, JSONParseContext jsonParseContext) throws Exception {

        int beginIndex = fromIndex + 1;
        char ch = '\0';
        Object key = null;
        boolean assignableFromMap = true;
        SetterInfo setterInfo = null;
        ClassStructureWrapper classStructureWrapper = null;
        Class clazz = genericParameterizedType == null ? null : genericParameterizedType.getActualType();
        if (instance != null) {
            if (!(instance instanceof Map)) {
                classStructureWrapper = ClassStructureWrapper.get(clazz);
                assignableFromMap = false;
            }
        } else {
            if (clazz == null || clazz == Object.class
                    || clazz == Map.class || clazz == LinkedHashMap.class) {
                instance = new LinkedHashMap<String, Object>();
            } else if (clazz == HashMap.class) {
                instance = new HashMap<String, Object>();
            } else {
                classStructureWrapper = ClassStructureWrapper.get(clazz);
                assignableFromMap = classStructureWrapper == null ? false : classStructureWrapper.isAssignableFromMap();
                if (deserialize) {
                    if (!assignableFromMap) {
                        instance = classStructureWrapper.newInstance();
                    } else {
                        instance = clazz.newInstance();
                    }
                }
            }
        }

        boolean empty = true;
        boolean allowComment = jsonParseContext.isAllowComment();

        // for loop to parse
        for (int i = beginIndex; i < toIndex; i++) {
            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                i++;
            }
            if (jsonParseContext.isAllowComment()) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }

            int fieldKeyFrom = i, fieldKeyTo, splitIndex, simpleToIndex = -1;
            boolean isUnquotedFieldName = false;
            JsonDeserialize jsonDeserialize = null;
            boolean skipValue = !deserialize;

            // Standard JSON field name with "
            if (ch == '"') {
                while (i + 1 < toIndex && (buf[++i] != '"' || buf[i - 1] == '\\')) ;
                empty = false;
                i++;
            } else {
                // 4种可能：（There are only four possibilities）
                // 1 空对象（empty object）
                // 2 语法错误，字符不为空，无效结束符号'}'（Syntax error, character is not empty, invalid closing symbol '}'）
                // 3 单引号key
                // 4 无引号key
                if (ch == '}') {
                    if (!empty) {
                        throw new JSONException("Syntax error, at pos " + i + ", the closing symbol '}' is not allowed here.");
                    }
                    jsonParseContext.setEndIndex(i);
                    return (T) instance;
                }
                if (ch == '\'') {
                    if (jsonParseContext.isAllowSingleQuotes()) {
                        while (i + 1 < toIndex && buf[++i] != '\'') ;
                        empty = false;
                        i++;
                    } else {
                        throw new JSONException("Syntax error, at pos " + i + ", the single quote symbol ' is not allowed here.");
                    }
                } else {
                    if (jsonParseContext.isAllowUnquotedFieldNames()) {
                        // 无引号key处理
                        // 直接锁定冒号（:）位置
                        while (i + 1 < toIndex && buf[++i] != ':') ;
                        empty = false;
                        isUnquotedFieldName = true;
                    }
                }
            }

            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                i++;
            }
            // 清除注释前记录属性字段的token结束位置
            fieldKeyTo = i;
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }

            // Standard JSON rules:
            // 1 if matched, it can only be followed by a colon, and all other symbols are wrong
            // 2 after the attribute value is parsed, it can only be followed by a comma(,) or end, and other symbols are wrong
            if (ch == ':') {
                // Resolve key value pairs
                if (assignableFromMap) {
                    // 如果是map类型直接解析出key (If it is a map type, directly resolve the key)
                    Class mapKeyClass = genericParameterizedType == null ? null : genericParameterizedType.getMapKeyClass();
                    key = parseMapKey(fieldKeyFrom, fieldKeyTo, buf, mapKeyClass, jsonParseContext);
                } else {
                    // 如果是实体对象，通过匹配key查找setter信息 （If it is an entity object, find the setter information by matching the key）
                    if (deserialize) {
                        setterInfo = getSetterMethodInfo(classStructureWrapper, buf, fieldKeyFrom, fieldKeyTo, isUnquotedFieldName);

                        // Custom deserialization
                        if (setterInfo != null) {
                            // Whether to consider setterMethodInfo.isDeserialize() ?
                            jsonDeserialize = (JsonDeserialize) setterInfo.getDeserializeAnnotation(JsonDeserialize.class);
                        } else {
                            skipValue = true;
                        }
                    }
                }

                // 清除空白字符（clear white space characters）
                while ((ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                // 分割符位置,SimpleMode
                splitIndex = i - 1;

                // 空，布尔值，数字，或者字符串 （ null, true or false or numeric or string）
                boolean isSimpleValue = false;
                // 是否反序列
                boolean isDeserialize = deserialize;

                // custom Deserializer
                if (jsonDeserialize != null) {
                    JsonDeserializer jsonDeserializer = getJsonDeserializer(jsonDeserialize);
                    // Parse value part
                    // Get the end position according to the match (comma or})
                    Object deserializableValue = parseDeserializableValue(splitIndex + 1, toIndex, buf, jsonDeserialize.useSource(), jsonParseContext);
                    Object value = jsonDeserializer.deserialize(deserializableValue, null, jsonParseContext);
                    doDeserializeInvokeValue(false, instance, key, value, setterInfo);
                    i = jsonParseContext.getEndIndex();
                } else {
                    if (deserialize && setterInfo != null) {
                        isDeserialize = setterInfo.isDeserialize();
                    }

                    switch (ch) {
                        case '{': {
                            GenericParameterizedType valueType = null;
                            Object defaultValue = null;
                            if (!assignableFromMap && setterInfo != null) {
                                valueType = setterInfo.getGenericParameterizedType();
                                boolean nonInstanceType = setterInfo.isNonInstanceType();
                                if (nonInstanceType) {
                                    if (jsonParseContext.isUseDefaultFieldInstance()) {
                                        defaultValue = setterInfo.getDefaultFieldValue(instance);
                                        if (defaultValue != null) {
                                            valueType = valueType.copyAndReplaceActualType(defaultValue.getClass());
                                        }
                                    } else {
                                        isDeserialize = false;
                                    }
                                } else {
                                    if (valueType.isCamouflage() && genericParameterizedType != null) {
                                        // 根据入参指定获取伪泛型的实际类型
                                        Class<?> actualType = genericParameterizedType.getGenericClass(valueType.getGenericName());
                                        valueType = GenericParameterizedType.actualType(actualType);
                                    }
                                }
                            } else {
                                valueType = genericParameterizedType == null ? null : genericParameterizedType.getValueType();
                            }
                            Object value = parseObjectOfBuffers(i, toIndex, buf, valueType, defaultValue, isDeserialize, jsonParseContext);
                            if (isDeserialize) {
                                doDeserializeInvokeValue(assignableFromMap, instance, key, value, setterInfo);
                            }
                            i = jsonParseContext.getEndIndex();
                            break;
                        }
                        case '[': {
                            // 2 [ array
                            GenericParameterizedType valueType = getGenericValueType(assignableFromMap, setterInfo, genericParameterizedType);
                            // 解析集合或者数组 （Parse a collection or array）
                            Object value = parseArrayOfBuffers(i, toIndex, buf, valueType, null, isDeserialize, jsonParseContext);
                            if (isDeserialize) {
                                doDeserializeInvokeValue(assignableFromMap, instance, key, value, setterInfo);
                            }
                            i = jsonParseContext.getEndIndex();
                            break;
                        }
                        case '"': {
                            // 3 string
                            GenericParameterizedType valueType = getGenericValueType(assignableFromMap, setterInfo, genericParameterizedType);
                            // parse string
                            Object value = parseStringOfBuffers(i, toIndex, buf, skipValue, valueType, jsonParseContext);
                            if (isDeserialize) {
                                doDeserializeInvokeValue(assignableFromMap, instance, key, value, setterInfo);
                            }
                            i = jsonParseContext.getEndIndex();
                            break;
                        }
                        default: {
                            isSimpleValue = true;
                            // 4 null, true or false or numeric
                            // Find comma(,) or closing symbol(})
                            while (i + 1 < toIndex) {
                                ch = buf[i + 1];
                                if (allowComment) {
                                    // '/' in simple mode must be a comment, otherwise an exception will be thrown
                                    if (ch == '/') {
                                        simpleToIndex = i + 1;
                                        int j = clearCommentAndWhiteSpaces(buf, ++i + 1, toIndex, jsonParseContext);
                                        ch = buf[j];
                                        // Make sure the I position precedes ',' or '}'
                                        i = j - 1;
                                    }
                                }
                                if (ch == ',' || ch == '}') {
                                    break;
                                }
                                i++;
                            }
                        }
                    }
                }

                // clear white space characters
                while ((ch = buf[++i]) <= ' ') ;
                if (simpleToIndex == -1) {
                    simpleToIndex = i;
                }

                if (allowComment) {
                    // clearComment and append whiteSpaces
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }

                // Check whether the next character is a comma or end symbol '}'. If yes, continue or break. If not, throw an exception
                boolean isClosingSymbol = ch == '}';
                if (ch == ',' || isClosingSymbol) {
                    if (isSimpleValue && isDeserialize) {
                        GenericParameterizedType valueType = getGenericValueType(assignableFromMap, setterInfo, genericParameterizedType);
                        parseSimpleValue(assignableFromMap, instance, setterInfo, valueType, key, splitIndex, simpleToIndex, buf, jsonParseContext);
                    }
                    if (isClosingSymbol) {
                        jsonParseContext.setEndIndex(i);
                        return (T) instance;
                    }
                } else {
                    throw new JSONException("Syntax error, at pos " + i + ", unexpected token character '" + ch + "', expected ',' or '}'");
                }
            } else {
                throw new JSONException("Syntax error, at pos " + i + ", unexpected token character '" + ch + "', Colon character ':' is expected.");
            }
        }
        throw new JSONException("Syntax error, the closing symbol '}' is not found ");
    }

    private static GenericParameterizedType getGenericValueType(boolean assignableFromMap, SetterInfo setterInfo, GenericParameterizedType hostParameterizedType) {
        GenericParameterizedType valueType;
        if (!assignableFromMap && setterInfo != null) {
            valueType = setterInfo.getGenericParameterizedType();
            if (valueType.isCamouflage() && hostParameterizedType != null) {
                Class<?> actualType = hostParameterizedType.getGenericClass(valueType.getGenericName());
                valueType = GenericParameterizedType.actualType(actualType);
            }
        } else {
            valueType = hostParameterizedType == null ? null : hostParameterizedType.getValueType();
        }
        return valueType;
    }

    /**
     * 解析自定义反序列内容
     *
     * @param beginIndex       开始位置
     * @param toIndex          最大结束位置
     * @param buf              json内容
     * @param useSource        是否使用源字符串（暂时不支持）
     * @param jsonParseContext 解析配置
     * @return Number，String， Map(LinkHashMap), List(ArrayList)
     */
    private static Object parseDeserializableValue(int beginIndex, int toIndex, char[] buf, boolean useSource, JSONParseContext jsonParseContext) throws Exception {
        char startCh = buf[beginIndex], ch;
        switch (startCh) {
            case '{':
                return parseObjectOfBuffers(beginIndex, toIndex, buf, GenericParameterizedType.actualType(Map.class), null, true, jsonParseContext);
            case '[':
                return parseArrayOfBuffers(beginIndex, toIndex, buf, GenericParameterizedType.collectionType(List.class, (Class) null), null, true, jsonParseContext);
            case '"':
                return parseStringOfBuffers(beginIndex, toIndex, buf, GenericParameterizedType.actualType(String.class), jsonParseContext);
            default:
                // 数字，null, true/false
                // 是否考虑清除注释？
                int i = beginIndex;
                int endIndex = -1;
                boolean allowComment = jsonParseContext.isAllowComment();
                while (i + 1 < toIndex) {
                    ch = buf[i + 1];
                    if (allowComment) {
                        if (ch == '/') {
                            endIndex = i + 1;
                            int j = clearCommentAndWhiteSpaces(buf, ++i + 1, toIndex, jsonParseContext);
                            ch = buf[j];
                            i = j - 1;
                        }
                    }
                    if (ch == ',' || ch == '}') {
                        break;
                    }
                    i++;
                }
                jsonParseContext.setEndIndex(i);
                if (endIndex == -1) {
                    endIndex = i + 1;
                }
                // clear end WhiteSpaces
                while (endIndex > beginIndex && buf[endIndex - 1] <= ' ') {
                    endIndex--;
                }
                int len = endIndex - beginIndex;
                if (len == 4) {
                    if (startCh == 'n'
                            && buf[beginIndex + 1] == 'u'
                            && buf[beginIndex + 2] == 'l'
                            && buf[beginIndex + 3] == 'l') {
                        return null;
                    }
                    if (startCh == 't'
                            && buf[beginIndex + 1] == 'r'
                            && buf[beginIndex + 2] == 'u'
                            && buf[beginIndex + 3] == 'e') {
                        return true;
                    }
                }
                if (len == 5 &&
                        startCh == 'f'
                        && buf[beginIndex + 1] == 'a'
                        && buf[beginIndex + 2] == 'l'
                        && buf[beginIndex + 3] == 's'
                        && buf[beginIndex + 4] == 'e') {
                    return false;
                }
                return parseNumber(buf, beginIndex, endIndex, jsonParseContext.isUseBigDecimalAsDefault());
        }
    }

    private static void doDeserializeInvokeValue(boolean assignableFromMap, Object instance, Object key, Object value, SetterInfo setterInfo) {
        if (assignableFromMap) {
            Map<Object, Object> mapInstance = (Map<Object, Object>) instance;
            mapInstance.put(key, value);
        } else {
            if (setterInfo != null)
                setterInfo.invoke(instance, value);
        }
    }

    private static Object parseArrayOfBuffers(int fromIndex, int toIndex, char[] buf, GenericParameterizedType parameterizedType, Object instance, boolean deserialize, JSONParseContext jsonParseContext) throws Exception {

        int beginIndex = fromIndex + 1;
        char ch = '\0';

        Collection<Object> collection;
        boolean isArrayCls = false;

        Class<?> collectionCls = null;
        GenericParameterizedType valueType = null;
        Class actualType = null;

        if (parameterizedType != null) {
            collectionCls = parameterizedType.getActualType();
            valueType = parameterizedType.getValueType();
            actualType = valueType == null ? null : valueType.getActualType();
        }

        if (instance != null) {
            if (instance instanceof Collection) {
                collection = (Collection) instance;
            } else {
                isArrayCls = collectionCls.isArray();
                if (isArrayCls) {
                    collection = new ArrayList<Object>();
                    actualType = collectionCls.getComponentType();
                    if (valueType == null) {
                        valueType = GenericParameterizedType.actualType(actualType);
                    }
                } else {
                    throw new UnsupportedOperationException("Unsupported for collection type '" + collectionCls + "'");
                }
            }

        } else {
            if (collectionCls == null || collectionCls == ArrayList.class) {
                collection = new ArrayList<Object>();
            } else {
                isArrayCls = collectionCls.isArray();
                if (isArrayCls) {
                    // arr用list先封装数据再转化为数组
                    collection = new ArrayList<Object>();
                    actualType = collectionCls.getComponentType();
                    if (valueType == null) {
                        valueType = GenericParameterizedType.actualType(actualType);
                    }
                } else {
                    collection = createCollectionInstance(collectionCls);
                }
            }
        }

        // 允许注释
        boolean allowComment = jsonParseContext.isAllowComment();

        // 集合数组核心token是逗号（The core token of the collection array is a comma）
        // for loop
        for (int i = beginIndex; i < toIndex; i++) {
            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                i++;
            }
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }

            // for simple element parse
            int simpleFromIndex = i, simpleToIndex = -1;

            // 如果提前遇到字符']'，说明是空集合
            //  （If the character ']' is encountered in advance, it indicates that it is an empty set）
            // 另一种可能性(语法错误): 非空集合，发生在空逗号后接结束字符']'直接抛出异常
            //  (Another possibility(Syntax error): whether the empty comma followed by the closing character ']' throws an exception)
            if (ch == ']') {
                if (collection.size() > 0) {
                    throw new JSONException("Syntax error, at pos " + i + ", the closing symbol ']' is not allowed here.");
                }
                jsonParseContext.setEndIndex(i);
                if (isArrayCls) {
                    return Array.newInstance(valueType == null ? Object.class : valueType.getActualType(), 0);
                }
                return collection;
            }
            // 是否简单元素如null, true or false or numeric or string
            // (is simple elements such as null, true or false or numeric or string)
            boolean isSimpleElement = false;
            boolean skipValue = !deserialize;
            switch (ch) {
                case '{': {
                    Object value = parseObjectOfBuffers(i, toIndex, buf, valueType, null, deserialize, jsonParseContext);
                    if (deserialize) {
                        collection.add(value);
                    }
                    i = jsonParseContext.getEndIndex();
                    break;
                }
                case '[': {
                    // 2 [ array
                    Object value = parseArrayOfBuffers(i, toIndex, buf, valueType, null, deserialize, jsonParseContext);
                    collection.add(value);
                    i = jsonParseContext.getEndIndex();
                    break;
                }
                case '"': {
                    // 3 string
                    // When there are escape characters, the escape character needs to be parsed
                    Object value = null;
                    if (actualType == byte[].class) {
                        value = parseBytesOfBuffers(i, toIndex, buf, jsonParseContext);
                    } else {
                        value = parseStringOfBuffers(i, toIndex, buf, skipValue, valueType, jsonParseContext);
                        if (actualType == char[].class && value instanceof String) {
                            value = getChars((String) value);
                        }
                    }
                    collection.add(value);
                    i = jsonParseContext.getEndIndex();
                    break;
                }
                default: {
                    // 简单元素（Simple element）
                    isSimpleElement = true;
                    // 查找逗号或者结束字符']' (Find comma or closing character ']')
                    // 注： 查找后i的位置在逗号或者']'之前(i pos before index of ',' or ']')
                    while (i + 1 < toIndex) {
                        ch = buf[i + 1];
                        if (allowComment) {
                            // '/' in simple mode must be a comment, otherwise an exception will be thrown
                            if (ch == '/') {
                                simpleToIndex = i + 1;
                                int j = clearCommentAndWhiteSpaces(buf, ++i + 1, toIndex, jsonParseContext);
                                ch = buf[j];
                                // Make sure the I position precedes', 'or'} '
                                i = j - 1;
                            }
                        }
                        if (ch == ',' || ch == ']') {
                            break;
                        }
                        i++;
                    }
                }
            }

            // 清除空白字符（clear white space characters）
            while ((ch = buf[++i]) <= ' ') ;
            if (simpleToIndex == -1) {
                simpleToIndex = i;
            }

            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }

            // 检查下一个字符是逗号还是结束符号。如果是，继续或者终止。如果不是，则抛出异常
            // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
            boolean isEnd = ch == ']';
            if (ch == ',' || isEnd) {
                if (isSimpleElement) {
                    Object value;
                    int paramClassType = ReflectConsts.getParamClassType(actualType);

                    switch (paramClassType) {
                        case ReflectConsts.CLASS_TYPE_DATE:
                            value = parseDateValue(simpleFromIndex, simpleToIndex, buf, null, null, (Class<? extends Date>) actualType);
                            break;
                        case ReflectConsts.CLASS_TYPE_NUMBER:
                            value = parseNumberValue(simpleFromIndex, simpleToIndex, buf, ReflectConsts.getParamClassNumberType(actualType));
                            break;
                        case ReflectConsts.CLASS_TYPE_STRING:
                            value = parseStringValue(simpleFromIndex, simpleToIndex, buf);
                            break;
                        case ReflectConsts.CLASS_TYPE_CHAR_ARRAY:
                            value = parseStringValue(simpleFromIndex, simpleToIndex, buf).toCharArray();
                            break;
                        case ReflectConsts.CLASS_TYPE_BYTE_ARRAY:
                            value = parseBytesOfBuffers0(buf, simpleFromIndex, simpleToIndex, jsonParseContext);
                            break;
                        default:
                            value = parseOtherTypeValue(simpleFromIndex, simpleToIndex, buf, actualType, jsonParseContext);
                            break;
                    }
                    collection.add(value);
                }
                if (isEnd) {
                    jsonParseContext.setEndIndex(i);
                    // 如果是数组类型转化为数组（If it is an array type, convert it to an array）
                    if (isArrayCls) {
                        return collectionToArray(collection, actualType == null ? Object.class : actualType);
                    }
                    // 返回集合类型（Return collection type）
                    return collection;
                }
            } else {
                throw new JSONException("Syntax error, at pos " + i + ", unexpected token character '" + ch + "', expected ',' or ']'");
            }
        }
        throw new JSONException("Syntax error, cannot find closing symbol ']' matching '['");
    }

    private static Object parseStringOfBuffers(int fromIndex, int toIndex, char[] buf, boolean skipValue, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) throws Exception {

        if (skipValue) {
            char prev = '\0', ch = '\0';
            int i = fromIndex + 1;
            while (i < toIndex && (ch = buf[i]) != '"' || prev == '\\') {
                i++;
                prev = ch;
            }
            jsonParseContext.setEndIndex(i);
            return null;
        }

        return parseStringOfBuffers(fromIndex, toIndex, buf, parameterizedType, jsonParseContext);
    }

    private static Object parseStringOfBuffers(int fromIndex, int toIndex, char[] buf, GenericParameterizedType parameterizedType, JSONParseContext jsonParseContext) throws Exception {

        int paramClassType, beginIndex = fromIndex + 1;
        boolean isCharArray = false;
        Class<?> actualType = null;
        boolean isStringType = parameterizedType == null || (actualType = parameterizedType.getActualType()) == String.class || actualType == Object.class;
        if (isStringType
                || (paramClassType = parameterizedType.getParamClassType()) == ReflectConsts.CLASS_TYPE_STRING
                || (isCharArray = paramClassType == ReflectConsts.CLASS_TYPE_CHAR_ARRAY)) {
            // String可能存在转义字符，单独处理（String may have escape characters, which should be handled separately）
            char ch = '\0', next = '\0';
            int i = beginIndex;
            int len;

//            StringBuilder stringBuilder = jsonParseContext.getStringBuilder();
            JSONWriter writer = null;
            boolean escape = false;

            for (; i < toIndex; i++) {
//                if(jsonParseContext.isDisableEscapeMode()) {
//                    i = indexOf(buf, '"', beginIndex, toIndex);
//                    if (i == -1) {
//                        throw new JSONException("Syntax error, the closing symbol '\"' is not found ");
//                    }
//                    ch = '"';
//                } else {
//                    while (i < toIndex && (ch = buf[i]) != '\\' && ch != '"') {
//                        i++;
//                    }
//                }
                while (i < toIndex && (ch = buf[i]) != '\\' && ch != '"') {
                    i++;
                }

                // ch is \\ or "
                if (ch == '\\') {
                    if (i < toIndex - 1) {
                        next = buf[i + 1];
                    }
                    if(writer == null) {
                        writer = getContextWriter(jsonParseContext);
                    }
                    escape = true;

                    switch (next) {
                        case '"':
                            // add len
//                            len = i - beginIndex;
//                            tmp = chars;
//                            chars = new char[tmp.length + len + 1];
//                            System.arraycopy(tmp, 0, chars, 0, tmp.length);
//                            System.arraycopy(buf, beginIndex, chars, tmp.length, len);
//                            chars[tmp.length + len] = '"';
//                            beginIndex = ++i + 1;
//                            len = i - beginIndex;
                            if (i > beginIndex) {
                                writer.write(buf, beginIndex, i - beginIndex + 1);
                                writer.setCharAt(writer.size() - 1, '"');
//                                stringBuilder.setCharAt(stringBuilder.length() - 1, '"');
                            } else {
                                writer.append('"');
                            }
                            beginIndex = ++i + 1;
                            break;
                        case 'n':
                            len = i - beginIndex;
                            writer.write(buf, beginIndex, len + 1);
                            writer.setCharAt(writer.size() - 1, '\n');
                            beginIndex = ++i + 1;
                            break;
                        case 'r':
                            len = i - beginIndex;
                            writer.write(buf, beginIndex, len + 1);
                            writer.setCharAt(writer.size() - 1, '\r');
                            beginIndex = ++i + 1;
                            break;
                        case 't':
                            len = i - beginIndex;
                            writer.write(buf, beginIndex, len + 1);
                            writer.setCharAt(writer.size() - 1, '\t');
                            beginIndex = ++i + 1;
                            break;
                        case 'b':
                            len = i - beginIndex;
                            writer.write(buf, beginIndex, len + 1);
                            writer.setCharAt(writer.size() - 1, '\b');
                            beginIndex = ++i + 1;
                            break;
                        case 'f':
                            len = i - beginIndex;
                            writer.write(buf, beginIndex, len + 1);
                            writer.setCharAt(writer.size() - 1, '\f');
                            beginIndex = ++i + 1;
                            break;
                        case 'u':
                            len = i - beginIndex;
                            writer.write(buf, beginIndex, len + 1);
                            int c = parseInt(buf, i + 2, i + 6, 16);
                            writer.setCharAt(writer.size() - 1, (char) c);
                            i += 4;
                            beginIndex = ++i + 1;
                            break;
                        case '\\':
//                            len = i - beginIndex;
                            if (i > beginIndex) {
                                writer.write(buf, beginIndex, i - beginIndex + 1);
                                writer.setCharAt(writer.size() - 1, '\\');
                            } else {
                                writer.append('\\');
                            }
                            beginIndex = ++i + 1;
                            break;
                    }
                } else {
                    jsonParseContext.setEndIndex(i);
                    len = i - beginIndex;
                    if (escape) {
                        writer.write(buf, beginIndex, len);
                        if (isCharArray) {
                            int charLen = writer.size();
                            char[] chars = new char[charLen];
                            writer.getChars(0, charLen, chars, 0);
                            return chars;
                        }
                        return writer.toString();
                    } else {
                        if (isCharArray) {
                            char[] chars = new char[len];
                            System.arraycopy(buf, beginIndex, chars, 0, len);
//                            return Arrays.copyOfRange(buf, beginIndex, beginIndex + len);
                            return chars;
                        }
                        return len == 0 ? "" : new String(buf, beginIndex, len);
                    }
                }
            }
            throw new JSONException("Syntax error, from pos " + beginIndex + ", the closing symbol '\"' is not found ");
        }

        // Other types ignore escape
        int endStringIndex = indexOf(buf, '"', beginIndex, toIndex);
        if (endStringIndex == -1) {
            throw new JSONException("Syntax error, from pos " + beginIndex + ", the closing symbol '\"' is not found ");
        }

        // End position
        jsonParseContext.setEndIndex(endStringIndex);
        switch (paramClassType) {
            case ReflectConsts.CLASS_TYPE_DATE:
                // date type
                Class<? extends Date> dateCls = (Class<? extends Date>) actualType;
                return parseDateValue(fromIndex, endStringIndex + 1, buf, parameterizedType.getDatePattern(), parameterizedType.getDateTimezone(), dateCls);
            case ReflectConsts.CLASS_TYPE_NUMBER:
                return parseNumberValue(fromIndex, endStringIndex + 1, buf, parameterizedType.getParamClassNumberType());
            case ReflectConsts.CLASS_TYPE_BYTE_ARRAY:
                return parseBytesOfBuffers0(fromIndex, endStringIndex - fromIndex - 1, buf, jsonParseContext);
            default: {
                return parseOtherTypeValue(fromIndex, endStringIndex + 1, buf, actualType, jsonParseContext);
            }
        }
    }

    private static byte[] parseBytesOfBuffers(int fromIndex, int toIndex, char[] buf, JSONParseContext jsonParseContext) {
        int endStringIndex = indexOf(buf, '"', fromIndex + 1, toIndex);
        if (endStringIndex == -1) {
            throw new JSONException("Syntax error, from pos " + (fromIndex + 1) + ", the closing symbol '\"' is not found ");
        }
        jsonParseContext.setEndIndex(endStringIndex);
        int len = endStringIndex - fromIndex - 1;
        return parseBytesOfBuffers0(fromIndex, len, buf, jsonParseContext);
    }

    private static byte[] parseBytesOfBuffers0(char[] buf, int fromIndex, int splitIndex, JSONParseContext jsonParseContext) {
        while ((fromIndex < splitIndex) && buf[fromIndex] <= ' ') {
            fromIndex++;
        }
        while ((splitIndex > fromIndex) && buf[splitIndex - 1] <= ' ') {
            splitIndex--;
        }
        return parseBytesOfBuffers0(fromIndex, splitIndex - fromIndex - 2, buf, jsonParseContext);
    }

    private static byte[] parseBytesOfBuffers0(int fromIndex, int len, char[] buf, JSONParseContext jsonParseContext) {
        if (jsonParseContext.isByteArrayFromHexString()) {
            return hexString2Bytes(buf, fromIndex + 1, len);
        } else {
            byte[] bytes = new byte[len];
            int offset = fromIndex + 1;
            for (int i = 0; i < len; i++) {
                bytes[i] = (byte) buf[offset + i];
            }
            return io.github.wycst.wast.common.tools.Base64.getDecoder().decode(bytes);
        }
    }

    /**
     * @param assignableFromMap 是否map类型
     * @param instance          实例对象
     * @param setterInfo        setter方法
     * @param key               map的key
     * @param fromIndex         一般为冒号位置
     * @param splitIndex        逗号或者}位置
     * @param buf
     * @param jsonParseContext
     * @throws Exception
     */
    private static void parseSimpleValue(boolean assignableFromMap, Object instance, SetterInfo setterInfo, GenericParameterizedType valueType, Object key, int fromIndex, int splitIndex, char[] buf, JSONParseContext jsonParseContext) throws Exception {
        if (assignableFromMap) {
            Object propertyValue = parseOtherTypeValue(fromIndex + 1, splitIndex, buf, null, jsonParseContext);
            Map<Object, Object> instMap = (Map<Object, Object>) instance;
            instMap.put(key, propertyValue);
        } else {
            if (setterInfo == null) {
                /** 如果没有命中setterInfo,不能直接return，否则会缺少对简单value的校验 */
                parseOtherTypeValue(fromIndex + 1, splitIndex, buf, null, jsonParseContext);
                return;
            }
            Class<?> parameterType = valueType.getActualType();
            int paramClassType = valueType.getParamClassType();

            Object value;
            switch (paramClassType) {
                case ReflectConsts.CLASS_TYPE_DATE:
                    // 日期类型
                    Class<? extends Date> dateCls = (Class<? extends Date>) parameterType;
                    value = parseDateValue(fromIndex + 1, splitIndex, buf, setterInfo.getPattern(), setterInfo.getTimezone(), dateCls);
                    setterInfo.invoke(instance, value);
                    break;
                case ReflectConsts.CLASS_TYPE_NUMBER:
                    value = parseNumberValue(fromIndex + 1, splitIndex, buf, valueType.getParamClassNumberType());
                    setterInfo.invoke(instance, value);
                    break;
                case ReflectConsts.CLASS_TYPE_STRING:
                    value = parseStringValue(fromIndex + 1, splitIndex, buf);
                    setterInfo.invoke(instance, value);
                    break;
                case ReflectConsts.CLASS_TYPE_CHAR_ARRAY:
                    value = parseStringValue(fromIndex + 1, splitIndex, buf).toCharArray();
                    setterInfo.invoke(instance, value);
                    break;
                case ReflectConsts.CLASS_TYPE_BYTE_ARRAY:
                    value = parseBytesOfBuffers0(buf, fromIndex + 1, splitIndex, jsonParseContext);
                    setterInfo.invoke(instance, value);
                    break;
                default: {
                    value = parseOtherTypeValue(fromIndex + 1, splitIndex, buf, parameterType, jsonParseContext);
                    setterInfo.invoke(instance, value);
                    break;
                }
            }
        }
    }

    private static String parseStringValue(int fromIndex, int toIndex, char[] buf) {
        // 初始位置
        int pos = fromIndex;
        char beginChar = '\0';
        char endChar = '\0';

        while ((fromIndex < toIndex) && ((beginChar = buf[fromIndex]) <= ' ')) {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && ((endChar = buf[toIndex - 1]) <= ' ')) {
            toIndex--;
        }

        int len = toIndex - fromIndex;
        if (beginChar == '"' && endChar == '"') {
            return new String(buf, fromIndex + 1, len - 2);
        }

        if (len == 4
                && beginChar == 'n'
                && buf[fromIndex + 1] == 'u'
                && buf[fromIndex + 2] == 'l'
                && endChar == 'l') {
            return null;
        }
        throw new JSONException("Syntax error, at pos " + pos + ", source: '" + new String(buf, fromIndex, len) + " ...' missing front and back double quotes(\").");
    }

    private static Object parseOtherTypeValue(int fromIndex, int toIndex, char[] buf, Class<?> clazz, JSONParseContext jsonParseContext) throws Exception {
        // 初始位置
        int pos = fromIndex;

        char beginChar = '\0';
        char endChar = '\0';

        while ((fromIndex < toIndex) && ((beginChar = buf[fromIndex]) <= ' ')) {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && ((endChar = buf[toIndex - 1]) <= ' ')) {
            toIndex--;
        }

        int len = toIndex - fromIndex;
        if (clazz != null) {
            if (len == 4
                    && beginChar == 'n'
                    && buf[fromIndex + 1] == 'u'
                    && buf[fromIndex + 2] == 'l'
                    && endChar == 'l') {
                return null;
            }
            if (clazz == StringBuffer.class) {
                if (beginChar == '"' && endChar == '"') {
                    StringBuffer buffer = new StringBuffer(len);
                    buffer.append(buf, fromIndex + 1, len - 2);
                    return buffer;
                } else {
                    throw new JSONException("Syntax error, at pos " + pos + ", source: '" + new String(buf, fromIndex, len) + " ...' cannot convert to StringBuffer.");
                }
            } else if (clazz == StringBuilder.class) {
                if (beginChar == '"' && endChar == '"') {
                    StringBuilder stringBuilder = new StringBuilder(len);
                    stringBuilder.append(buf, fromIndex + 1, len - 2);
                    return stringBuilder;
                } else {
                    throw new JSONException("Syntax error, at pos " + pos + ", source: '" + new String(buf, fromIndex, len) + " ...' cannot convert to StringBuilder.");
                }
            } else if (Enum.class.isAssignableFrom(clazz)) {
                if (beginChar == '"' && endChar == '"') {
                    Class cls = clazz;
                    try {
                        return Enum.valueOf(cls, new String(buf, fromIndex + 1, len - 2));
                    } catch (RuntimeException exception) {
                        if (jsonParseContext.isUnknownEnumAsNull()) {
                            return null;
                        } else {
                            throw exception;
                        }
                    }
                } else {
                    try {
                        int ordinal = parseInt(buf, fromIndex, toIndex);
                        Enum[] values = (Enum[]) clazz.getEnumConstants();
                        if (values != null && ordinal < values.length)
                            return values[ordinal];
                    } catch (Throwable throwable) {
                    }
                    throw new JSONException("Syntax error, at pos " + pos + ",source: '" + new String(buf, fromIndex, len) + " ...' cannot convert to Enum " + clazz + "");
                }
            } else if (clazz == Class.class) {
                if (beginChar == '"' && endChar == '"') {
                    return Class.forName(new String(buf, fromIndex + 1, len - 2));
                } else {
                    throw new JSONException("Syntax error, at pos " + pos + ",source: '" + new String(buf, fromIndex, len) + " ...' cannot convert to Class " + clazz + "");
                }
            } else {
                // Type not supported
                if (beginChar != '"') {
                    throw new JSONException("Syntax error, at pos " + pos + ",source: '" + new String(buf, fromIndex, len) + " ...' cannot convert to Class " + clazz + "");
                }
                return null;
            }
        } else {

            switch (beginChar) {
//                case '"':
//                    // 读取项设置禁用转义会出现，endChar一定是\”不用判断
//                    return new String(buf, fromIndex + 1, len - 2);
                case 't':
                    if (len == 4 && buf[fromIndex + 1] == 'r'
                            && buf[fromIndex + 2] == 'u'
                            && endChar == 'e') {
                        return true;
                    }
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'true' because it starts with 't', but found text '" + new String(buf, fromIndex, len) + "'");
                case 'f':
                    if (len == 5 && buf[fromIndex + 1] == 'a'
                            && buf[fromIndex + 2] == 'l'
                            && buf[fromIndex + 3] == 's'
                            && endChar == 'e') {
                        return false;
                    }
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'false' because it starts with 'f', but found text '" + new String(buf, fromIndex, len) + "'");
                case 'n':
                    if (len == 4 && buf[fromIndex + 1] == 'u'
                            && buf[fromIndex + 2] == 'l'
                            && endChar == 'l') {
                        return null;
                    }
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'null' because it starts with 'n', but found text '" + new String(buf, fromIndex, len) + "'");
                default:
                    return parseNumber(buf, fromIndex, toIndex, jsonParseContext.isUseBigDecimalAsDefault());
            }

//
//            if (beginChar == '"' && endChar == '"') {
//                return new String(buf, fromIndex + 1, len - 2);
//            } else if (len == 4
//                    && beginChar == 't'
//                    && buf[fromIndex + 1] == 'r'
//                    && buf[fromIndex + 2] == 'u'
//                    && endChar == 'e') {
//                return true;
//            } else if (len == 5
//                    && beginChar == 'f'
//                    && buf[fromIndex + 1] == 'a'
//                    && buf[fromIndex + 2] == 'l'
//                    && buf[fromIndex + 3] == 's'
//                    && endChar == 'e') {
//                return false;
//            } else {
//                if (len == 4
//                        && beginChar == 'n'
//                        && buf[fromIndex + 1] == 'u'
//                        && buf[fromIndex + 2] == 'l'
//                        && endChar == 'l') {
//                    return null;
//                }
//                // 除了字符串，true, false, null以外都以number处理
//                return parseNumber(buf, fromIndex, toIndex, jsonParseContext.isUseBigDecimalAsDefault());
//            }
        }
    }

    private static Object parseNumberValue(int fromIndex, int toIndex, char[] buf, int numberType) {

        // 去除前后的空白字符 参考String.trim()
        // 去除非打印字符如换行\n,制表符\t等
        char beginChar = '\0';
        char endChar = '\0';

        while ((fromIndex < toIndex) && ((beginChar = buf[fromIndex]) <= ' ')) {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && ((endChar = buf[toIndex - 1]) <= ' ')) {
            toIndex--;
        }

        int len = toIndex - fromIndex;
        if (len == 4 &&
                buf[0] == 'n' &&
                buf[1] == 'u' &&
                buf[2] == 'l' &&
                buf[3] == 'l') {
            return null;
        }
        switch (numberType) {
            case ReflectConsts.CLASS_TYPE_NUMBER_INTEGER:
                return parseInt(buf, fromIndex, toIndex);
            case ReflectConsts.CLASS_TYPE_NUMBER_FLOAT:
                return parseNumber(buf, fromIndex, toIndex, false).floatValue();
            case ReflectConsts.CLASS_TYPE_NUMBER_LONG:
                return parseLong(buf, fromIndex, toIndex);
            case ReflectConsts.CLASS_TYPE_NUMBER_DOUBLE:
                return parseNumber(buf, fromIndex, toIndex, false).doubleValue();
            case ReflectConsts.CLASS_TYPE_NUMBER_BOOLEAN: {
                if (len == 4
                        && beginChar == 't'
                        && buf[fromIndex + 1] == 'r'
                        && buf[fromIndex + 2] == 'u'
                        && buf[fromIndex + 3] == 'e') {
                    return true;
                } else if (len == 5
                        && beginChar == 'f'
                        && buf[fromIndex + 1] == 'a'
                        && buf[fromIndex + 2] == 'l'
                        && buf[fromIndex + 3] == 's'
                        && buf[fromIndex + 4] == 'e') {
                    return false;
                } else if (len == 1 && beginChar == '0') {
                    return false;
                } else if (len == 1 && beginChar == '1') {
                    return true;
                } else {
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", '" + new String(buf, fromIndex, len) + "' cannot convert to boolean ");
                }
            }
            case ReflectConsts.CLASS_TYPE_NUMBER_BIGDECIMAL:
                return new BigDecimal(buf, fromIndex, len);
            case ReflectConsts.CLASS_TYPE_NUMBER_BYTE:
                return (byte) parseInt(buf, fromIndex, toIndex);
            case ReflectConsts.CLASS_TYPE_NUMBER_CHARACTER: {
                if (beginChar == '\'' && endChar == '\'') {
                    if (len == 2)
                        return '\0';
                    if (len == 3) {
                        return buf[fromIndex + 1];
                    }
                    // '\u0011'
//                    if(len == 8) {
//                        // 使用场景小概率暂时不处理
//                    }
                }
                throw new JSONException("Syntax error,  pos " + fromIndex + ", '" + new String(buf, fromIndex, len) + "' cannot convert to char ");
            }
            case ReflectConsts.CLASS_TYPE_NUMBER_ATOMIC_INTEGER: {
                return new AtomicInteger(parseInt(buf, fromIndex, toIndex));
            }
            case ReflectConsts.CLASS_TYPE_NUMBER_ATOMIC_LONG: {
                return new AtomicLong(parseLong(buf, fromIndex, toIndex));
            }
            case ReflectConsts.CLASS_TYPE_NUMBER_ATOMIC_BOOLEAN: {
                boolean flag;
                if (len == 4
                        && beginChar == 't'
                        && buf[fromIndex + 1] == 'r'
                        && buf[fromIndex + 2] == 'u'
                        && buf[fromIndex + 3] == 'e') {
                    flag = true;
                } else if (len == 5
                        && beginChar == 'f'
                        && buf[fromIndex + 1] == 'a'
                        && buf[fromIndex + 2] == 'l'
                        && buf[fromIndex + 3] == 's'
                        && buf[fromIndex + 4] == 'e') {
                    flag = false;
                } else if (len == 1 && beginChar == '0') {
                    flag = false;
                } else if (len == 1 && beginChar == '1') {
                    flag = true;
                } else {
                    throw new JSONException("Syntax error,  pos " + fromIndex + ", '" + new String(buf, fromIndex, len) + "' cannot convert to AtomicBoolean ");
                }
                return new AtomicBoolean(flag);
            }
            case ReflectConsts.CLASS_TYPE_NUMBER_BIG_INTEGER: {
                return new BigInteger(new String(buf, fromIndex, len));
            }
        }
        return null;
    }

    /**
     * 双引号中间内容，去除前后空格 eg : , "name" : "tom" , == > "name" :
     *
     * @param from
     * @param to
     * @param buf
     * @param keyType 支持简单类型key
     * @return
     */
    private static Object parseMapKey(int from, int to, char[] buf, Class<?> keyType, JSONParseContext jsonParseContext) {
        char start = '"';
        while ((from < to) && ((start = buf[from]) <= ' ')) {
            from++;
        }
        char end = '"';
        while ((to > from) && ((end = buf[to - 1]) <= ' ')) {
            to--;
        }
        if (start == '"' && end == '"' || (start == '\'' && end == '\'')) {
            int len = to - from - 2;
            if (keyType == null || keyType == String.class) {
                return new String(buf, from + 1, len);
            }
            if (keyType == StringBuffer.class) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(buf, from + 1, len);
                return buffer;
            } else if (keyType == StringBuilder.class) {
                StringBuilder builder = new StringBuilder();
                builder.append(buf, from + 1, len);
                return builder;
            } else if (Number.class.isAssignableFrom(keyType)) {
                return parseNumberValue(from + 1, to - 1, buf, ReflectConsts.getParamClassNumberType(keyType));
            } else {
                throw new UnsupportedOperationException("Not Supported type '" + keyType + "' for map key ");
            }
        } else {
            if (keyType == null) {
                return parseNumber(buf, from, to, jsonParseContext.isUseBigDecimalAsDefault());
            } else {
                if (ReflectConsts.isNumberType(keyType)) {
                    return parseNumberValue(from, to, buf, ReflectConsts.getParamClassNumberType(keyType));
                }
            }
        }
        throw new JSONException("Syntax error,  pos " + from + ", '" + new String(buf, from, to - from) + "' is Invalid key ");
    }

    private static SetterInfo getSetterMethodInfo(ClassStructureWrapper classStructureWrapper, char[] buf, int from, int to, boolean isUnquotedFieldName) {
        char start = '"';
        while ((from < to) && ((start = buf[from]) <= ' ')) {
            from++;
        }
        char end = '"';
        while ((to > from) && ((end = buf[to - 1]) <= ' ')) {
            to--;
        }
        if (start == '"' && end == '"' || (start == '\'' && end == '\'')) {
            return classStructureWrapper.getSetterInfo(buf, from + 1, to - 1);
        }
        if (isUnquotedFieldName) {
            return classStructureWrapper.getSetterInfo(buf, from, to);
        }
        throw new JSONException("Syntax error,  pos " + from + ", '" + new String(buf, from, to - from) + "' is an Invalid key ");
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeJsonTo(obj, baos, options);
        return baos.toByteArray();
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
        JSONWriter content = new JSONWriter();
        try {
            stringify(obj, content, jsonConfig, indentLevel);
            return content.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            content.reset();
            jsonConfig.clear();
        }
        return null;
    }

    /**
     * 将对象序列化为json并写入到file文件中
     *
     * @param object
     * @param file
     */
    public static void writeJsonTo(Object object, File file, WriteOption... options) {
        try {
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

    static void stringify(Object obj, Writer content, JsonConfig jsonConfig, int indentLevel) throws Exception {

        int hashcode = System.identityHashCode(obj);
        if (jsonConfig.isWriteOptionSkipCircularReference()) {
            // 如果发现循环引用跳过,还是特殊标记？
            if (jsonConfig.getStatus(hashcode) == 0) {
                // 循环引用
                content.append('n').append('u').append('l').append('l');
                return;
            }
            jsonConfig.setStatus(hashcode, 0);
        }

        Class<?> clazz = obj.getClass();

        boolean writeFullProperty = jsonConfig.isWriteOptionFullProperty();
        boolean formatOut = jsonConfig.isWriteOptionFormatOut();

        if (clazz.isArray()) {
            int length = Array.getLength(obj);
            if (length == 0) {
                content.append('[').append(']');
            } else {
                // 数组类
                content.write('[');
                int lastLevel = indentLevel;
                boolean isFirstKey = true;
                for (int i = 0; i < length; i++) {
                    if (isFirstKey) {
                        isFirstKey = false;
                    } else {
                        content.append(",");
                    }
                    writeFormatSymbolOut(content, lastLevel = indentLevel + 1, formatOut);
                    Object value = Array.get(obj, i);
                    if (value == null) {
                        content.append('n').append('u').append('l').append('l');
                    } else {
                        writeValue(value, content, null, jsonConfig, formatOut ? indentLevel + 1 : -1);
                    }
                }
                if (lastLevel > indentLevel) {
                    writeFormatSymbolOut(content, indentLevel, formatOut);
                }
                content.append(']');
            }
        } else if (obj instanceof Collection) {

            Collection<?> collect = (Collection<?>) obj;
            if (collect.size() == 0) {
                content.append('[').append(']');
            } else {
                // 集合类
                content.write('[');
                int lastLevel = indentLevel;
                boolean isFirstKey = true;
                for (Object value : collect) {
                    if (isFirstKey) {
                        isFirstKey = false;
                    } else {
                        content.write(',');
                    }
                    writeFormatSymbolOut(content, lastLevel = indentLevel + 1, formatOut);
                    if (value == null) {
                        content.append('n').append('u').append('l').append('l');
                    } else {
                        writeValue(value, content, null, jsonConfig, formatOut ? indentLevel + 1 : -1);
                    }
                }
                // 如果最后一个元素是基本类型则 lastLevel > level
                // 否则 lastLevel = level，由最后一个元素{}决定是否缩进
                if (lastLevel > indentLevel) {
                    writeFormatSymbolOut(content, indentLevel, formatOut);
                }
                content.append(']');
            }

        } else if (obj instanceof Map) {
            // map类 调用get获取
            Map<Object, Object> map = (Map<Object, Object>) obj;
            if (map.size() == 0) {
                content.append('{').append('}');
            } else {
                content.write('{');
                // 遍历map
                Iterator<Entry<Object, Object>> iterator = map.entrySet().iterator();
                boolean isFirstKey = true;
                while (iterator.hasNext()) {
                    Entry<Object, Object> entry = iterator.next();
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

                    if (value != null) {
                        writeValue(value, content, null, jsonConfig, formatOut ? indentLevel + 1 : -1);
                    } else {
                        content.append('n').append('u').append('l').append('l');
                    }
                }
                writeFormatSymbolOut(content, indentLevel, formatOut);
                content.write('}');
            }
        } else if (obj instanceof CharSequence) {
            content.append('"').append(obj.toString()).append('"');
        } else if (obj instanceof Number) {
            content.append(obj.toString());
        } else if (obj instanceof Character) {
            content.append('"').append(String.valueOf(obj)).append('"');
        } else if (obj instanceof Date) {
            writeDate((Date) obj, jsonConfig.getDateFormatPattern(), jsonConfig.getTimezone(), content);
        } else if (obj instanceof Class) {
            content.append('"').append(((Class) obj).getName()).append('"');
        } else if (obj instanceof Enum) {
            content.append('"').append(((Enum) obj).name()).append('"');
        } else if (obj instanceof Annotation) {
            content.append('"').append(obj.toString()).append('"');
        } else {
            // 其他类型调用 get方法取
            content.append('{');
            ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(clazz);
            boolean isFirstKey = true;
            List<GetterInfo> getterInfos = classStructureWrapper.getGetterInfos(jsonConfig.isUseFields());

            boolean skipGetterOfNoExistField = jsonConfig.isSkipGetterOfNoneField();
            boolean camelCaseToUnderline = jsonConfig.isCamelCaseToUnderline();
            for (GetterInfo getterInfo : getterInfos) {

                if (!getterInfo.isSerialize())
                    continue;

                if (!getterInfo.existField() && skipGetterOfNoExistField) {
                    continue;
                }

                Object value = getterInfo.invoke(obj);
                if (value == null && !writeFullProperty)
                    continue;

                char[] quotBuffers = getterInfo.getFixedQuotBuffers();
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
                    JsonSerialize jsonSerialize = (JsonSerialize) getterInfo.getSerializeAnnotation(JsonSerialize.class);
                    if (jsonSerialize != null) {
                        getJsonSerializer(jsonSerialize).serialize(content, value, indentLevel + 1, jsonConfig);
                    } else {
                        writeValue(value, content, getterInfo, jsonConfig, formatOut ? indentLevel + 1 : -1);
                    }
                }
            }
            writeFormatSymbolOut(content, indentLevel, formatOut);

            content.append('}');
        }

        jsonConfig.setStatus(hashcode, -1);
    }

    private static void writeValue(Object value, Writer content, GetterInfo getterInfo, JsonConfig jsonConfig, int indentLevel) throws Exception {
        if (value instanceof CharSequence
                || value instanceof char[]
                || value instanceof Number
                || value instanceof Boolean) {
            appendValue(value, content, jsonConfig);
        } else if (value instanceof byte[]) {
            // Byte arrays may also be serialized as strings and processed separately
            byte[] bytes = (byte[]) value;
            if (jsonConfig.isWriteOptionBytesArrayToNative()) {
                // default array serialize mode
                stringify(value, content, jsonConfig, indentLevel);
            } else {
                if (jsonConfig.isWriteOptionBytesArrayToHex()) {
                    content.append('"').append(printHexString(bytes, (char) 0)).append('"');
                } else {
                    // use Base64 encoding
                    content.append('"').append(Base64.getEncoder().encodeToString(bytes)).append('"');
                }
            }
        } else if (value instanceof Date) {
            Date date = (Date) value;
            boolean writeDateAsTime = false;
            if (getterInfo != null && getterInfo.isWriteDateAsTime()) {
                writeDateAsTime = true;
            } else if (jsonConfig.isWriteDateAsTime()) {
                writeDateAsTime = true;
            }
            if (writeDateAsTime) {
                content.append(String.valueOf(date.getTime()));
                return;
            }
            String pattern = null;
            String timezone = null;
            if (getterInfo != null) {
                pattern = getterInfo.getPattern();
                timezone = getterInfo.getTimezone();
            }
            if (pattern == null) {
                pattern = jsonConfig.getDateFormatPattern();
            }
            if (timezone == null) {
                timezone = jsonConfig.getTimezone();
            }
            writeDate(date, pattern, timezone, content);
        } else {
            stringify(value, content, jsonConfig, indentLevel);
        }
    }

    private static void writeDate(Date date, String pattern, String timezone, Writer content) throws IOException {

        content.append('"');
        if (pattern == null) {
            pattern = date instanceof Time ? "HH:mm:ss" : "yyyy-MM-dd HH:mm:ss";
        }

        pattern = pattern.trim();
        int len = pattern.length();

        int month, year, day, hourOfDay, minute, second, millisecond;
        io.github.wycst.wast.common.beans.Date commonDate = new io.github.wycst.wast.common.beans.Date(date.getTime());
        if (timezone != null) {
            commonDate.setTimeZone(timezone);
        }
        year = commonDate.getYear();
        month = commonDate.getMonth();
        day = commonDate.getDay();
        hourOfDay = commonDate.getHourOfDay();
        minute = commonDate.getMinute();
        second = commonDate.getSecond();
        millisecond = commonDate.getMillisecond();
        boolean isAm = commonDate.isAm();

        // 解析pattern
        // yy-MM-dd HH:mm:ss:s
        char prevChar = '\0';
        int count = 0;
        // 增加一位虚拟字符进行遍历
        for (int i = 0; i <= len; i++) {
            char ch = '\0';
            if (i < len)
                ch = pattern.charAt(i);
            if (ch == 'Y')
                ch = 'y';

            if (prevChar == ch) {
                count++;
            } else {
                // 判断prevChar开始输出
                if (prevChar == 'y') {
                    // 年份
                    if (count == 2) {
                        // 输出2位数年份
                        int j = year % 100;
                        if (j < 10) {
                            content.write(FORMAT_DIGITS[j]);
                        } else {
                            content.write(String.valueOf(j));
                        }
                    } else {
                        // 输出完整的年份
                        content.write(String.valueOf(year));
                    }
                } else if (prevChar == 'M') {
                    // 年份
                    if (count == 1 || month >= 10) {
                        // 输出实际month
                        content.write(String.valueOf(month));
                    } else {
                        // 输出完整的month
                        content.write(FORMAT_DIGITS[month]);
                    }
                } else if (prevChar == 'd') {
                    if (count == 1 || day >= 10) {
                        // 输出实际day
                        content.write(String.valueOf(day));
                    } else {
                        // 输出完整的day
                        content.write(FORMAT_DIGITS[day]);
                    }
                } else if (prevChar == 'a') {
                    // 上午/下午
                    if (isAm) {
                        content.append("am");
                    } else {
                        content.append("pm");
                    }
                } else if (prevChar == 'H') {
                    // 0-23
                    if (count == 1 || hourOfDay >= 10) {
                        // 输出实际hourOfDay
                        content.write(String.valueOf(hourOfDay));
                    } else {
                        // 输出完整的hourOfDay
                        content.write(FORMAT_DIGITS[hourOfDay]);
                    }
                } else if (prevChar == 'h') {
                    // 1-12 小时格式
                    int h = hourOfDay % 12;
                    if (h == 0)
                        h = 12;
                    if (count == 1 || h >= 10) {
                        // 输出实际h
                        content.write(String.valueOf(h));
                    } else {
                        // 输出完整的h
                        content.write(FORMAT_DIGITS[h]);
                    }
                    // 小时 1-12
                } else if (prevChar == 'm') {
                    // 分钟 0-59
                    if (count == 1 || minute >= 10) {
                        // 输出实际分钟
                        content.write(String.valueOf(minute));
                    } else {
                        // 输出2位分钟数
                        content.write(FORMAT_DIGITS[minute]);
                    }
                } else if (prevChar == 's') {
                    // 秒 0-59
                    if (count == 1 || second >= 10) {
                        // 输出实际秒
                        content.write(String.valueOf(second));
                    } else {
                        // 输出2位秒
                        content.write(FORMAT_DIGITS[second]);
                    }
                } else if (prevChar == 'S') {
                    // 毫秒
                    content.write(String.valueOf(millisecond));
                } else {
                    // 其他输出
                    if (prevChar != '\0') {
                        // 输出count个 prevChar
                        int n = count;
                        while (n-- > 0)
                            content.append(prevChar);
                    }
                }
                count = 1;
            }
            prevChar = ch;
            // 计数+1
        }
        content.append('"');
    }

    private static void writeFormatSymbolOut(Writer content, int level, boolean formatOut) throws IOException {
        // 0位是换行符
        // level = \t的个数
        if (formatOut && level > -1) {
            String symbol = Options.writeFormatOutSymbol;
            int symbolLen = 11;
            if (symbolLen - 1 > level) {
                content.write(symbol, 0, level + 1);
            } else {
                // 全部的symbol
                content.append(symbol);
                // 补齐差的\t个数
                int appendTabLen = level - symbolLen + 1;
                while (appendTabLen-- > 0) {
                    content.append('\t');
                }
            }
        }
    }

    /***
     * The core method of serialization optimization performance bottleneck
     *
     * CharSequence, char[], Number,  Boolean
     *
     * @param value
     * @param content
     */
    private static void appendValue(Object value, Writer content, JsonConfig jsonConfig) throws IOException {
        if (value == null) {
            content.append('n').append('u').append('l').append('l');
            return;
        }
        String strValue;
        boolean isCharSequence;
        if ((isCharSequence = value instanceof CharSequence) || value instanceof char[]) {
            char[] chars;
            int len;
            if (isCharSequence) {
                strValue = value.toString();
                len = strValue.length();
                chars = getChars(strValue);
            } else {
                chars = (char[]) value;
                len = chars.length;
            }

            content.append('"');
            int beginIndex = 0;

            if (!jsonConfig.isWriteOptionDisableEscapeValidate()) {
                // It takes too much time to determine whether there is an escape character
                for (int i = 0; i < len; i++) {
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
        } else if (value instanceof Number || value instanceof Boolean) {
            content.append(value.toString());
        } else {
            // Code unreachable
        }
    }

    public static boolean validate(String json, ReadOption... readOptions) {
        return validate(json, false, readOptions);
    }

    public static boolean validate(String json, boolean printIfException, ReadOption... readOptions) {
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

    abstract static class LocalBufferParser {
        abstract Object parse(char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception;
    }

}
