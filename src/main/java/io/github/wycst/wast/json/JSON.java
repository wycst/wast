/*
 * Copyright [2020-2026] [wangyunchao]
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
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.ReadOption;
import io.github.wycst.wast.json.options.WriteOption;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
@SuppressWarnings({"all"})
public final class JSON extends JSONGeneral {

    static final JSONInstance INSTANCE = JSONInstance.internal();

    /**
     * json -> Map(LinkHashMap)/List(ArrayList)/String
     *
     * @param json        source
     * @param readOptions 解析配置项
     * @return Map(LinkHashMap)/List(ArrayList)/String
     */
    public static Object parse(String json, ReadOption... readOptions) {
        return INSTANCE.parse(json, readOptions);
    }

    /**
     * json -> Map(LinkHashMap)/List(ArrayList)/String
     *
     * @param json        source
     * @param handler     自定义map处理器
     * @param readOptions 解析配置项
     * @return Map(LinkHashMap)/List(ArrayList)/String
     */
    public static Object parse(String json, JSONMapHandler mapHandler, ReadOption... readOptions) {
        return INSTANCE.parse(json, mapHandler, readOptions);
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
        return INSTANCE.parseAs(json, readOptions);
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
        return INSTANCE.parseNumberAs(json, targetClass, readOptions);
    }

    /**
     * 将对象按字段固定排序（ASCII字符升序）序列化为json字符串
     *
     * @param target
     * @param options
     * @return
     */
    public static String sortJson(Object target, WriteOption... options) {
        return INSTANCE.sortJson(target, options);
    }

    /**
     * 将JSON字符串按字段固定排序（ASCII字符升序）重新排列
     *
     * @param json
     * @param options
     * @return
     */
    public static String sortJsonString(String json, WriteOption... options) {
        return INSTANCE.sortJsonString(json, options);
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
        return INSTANCE.parseMap(json, mapCls, readOptions);
    }

    /**
     * json -> map
     *
     * @param json
     * @param readOptions
     * @return map instance
     */
    public static Map parseObject(String json, ReadOption... readOptions) {
        return INSTANCE.parseMap(json, LinkedHashMap.class, readOptions);
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
        return INSTANCE.parseCollection(json, collectionCls, readOptions);
    }

    /**
     * 将json字符数组转为对象或者数组(Convert JSON strings to objects or arrays)
     *
     * @param buf         字符数组
     * @param readOptions 配置项
     * @return
     */
    public static Object parse(char[] buf, ReadOption... readOptions) {
        return INSTANCE.parse(buf, readOptions);
    }

    /***
     * 解析字节数组返回Map对象或者List集合
     *
     * @param bytes 字节数组（ascii/utf8/...）
     * @param readOptions
     * @return
     */
    public static Object parse(byte[] bytes, ReadOption... readOptions) {
        return INSTANCE.parse(bytes, readOptions);
    }

    /***
     * 解析字节数组返回Map对象或者List集合
     *
     * @param bytes       字节数组（ascii/utf8/...）
     * @param handler     自定义map处理器
     * @param readOptions 配置项
     * @return Map对象或者List集合
     */
    public Object parse(byte[] bytes, JSONMapHandler handler, ReadOption... readOptions) {
        return INSTANCE.parse(bytes, handler, readOptions);
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
        return INSTANCE.parse(bytes, ascii, readOptions);
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
        return INSTANCE.parse(json, actualType, readOptions);
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
        return INSTANCE.parse(buf, actualType, readOptions);
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
        return INSTANCE.parse(buf, fromIndex, toIndex, actualType, readOptions);
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
        return INSTANCE.parse(buf, actualType, readOptions);
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
        return INSTANCE.parseObject(json, actualType, readOptions);
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
        return INSTANCE.parseObject(buf, actualType, readOptions);
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
        return INSTANCE.parseObject(buf, actualType, readOptions);
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
        return INSTANCE.parseObject(buf, ascii, actualType, readOptions);
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
        return INSTANCE.parse(json, type, readOptions);
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
        return INSTANCE.parseNdJson(json, readOptions);
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
        return INSTANCE.parseNdJson(is, readOptions);
    }

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
        return INSTANCE.parseNdJson(json, actualType, readOptions);
    }

    /**
     * 将集合对象转为ndjson字符串
     *
     * @param collection
     * @param writeOptions
     * @return
     */
    public static String toNdJsonString(Collection collection, WriteOption... writeOptions) {
        return INSTANCE.toNdJsonString(collection, writeOptions);
    }

    /**
     * 将集合对象写入ndjson输入流（使用系统默认编码）
     *
     * @param collection
     * @param os
     * @param writeOptions
     */
    public static void writeNdJsonTo(Collection collection, OutputStream os, WriteOption... writeOptions) {
        INSTANCE.writeNdJsonTo(collection, os, EnvUtils.CHARSET_DEFAULT, writeOptions);
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
        INSTANCE.writeNdJsonTo(collection, os, charset, writeOptions);
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
        return INSTANCE.translateTo(source, actualType, readOptions);
    }

    /**
     * 使用json深度克隆一个对象
     *
     * @param source
     * @param readOptions
     * @param <T>
     * @return
     */
    public static <T> T cloneObject(Object source, ReadOption... readOptions) {
        return INSTANCE.cloneObject(source, readOptions);
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
        return INSTANCE.parse(json, genericParameterizedType, readOptions);
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
        return INSTANCE.parse(buf, genericParameterizedType, readOptions);
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
        return INSTANCE.parse(buf, genericParameterizedType, readOptions);
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
        return INSTANCE.parseArray(json, actualType, readOptions);
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
        return INSTANCE.parseArray(buf, actualType, readOptions);
    }

    /**
     * 解析集合
     *
     * @param buf         字节数组
     * @param actualType  类型
     * @param readOptions 读取配置
     * @param <T>         泛型
     * @return
     */
    public static <T> List<T> parseArray(byte[] buf, final Class<T> actualType, ReadOption... readOptions) {
        return INSTANCE.parseArray(buf, actualType, readOptions);
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
        return INSTANCE.parseToObject(json, instance, readOptions);
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
        return INSTANCE.parseToList(json, instance, actualType, readOptions);
    }

    /***
     * 读取流返回Map对象或者List集合
     *
     * @param is 流对象(注: 不要使用网络流,请使用URL或者直接使用JSONReader)
     * @param readOptions
     * @return
     */
    public static Object read(InputStream is, ReadOption... readOptions) throws IOException {
        return INSTANCE.read(is, readOptions);
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
        return INSTANCE.read(is, actualType, readOptions);
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
        return INSTANCE.read(file, actualType, readOptions);
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
        return INSTANCE.read(url, actualType, readOptions);
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
        return INSTANCE.read(url, actualType, forceStreamMode, timeout, readOptions);
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
        return INSTANCE.read(file, genericType, readOptions);
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
        return INSTANCE.read(is, genericType, readOptions);
    }

    /**
     * 将对象转化为json字符串 (Convert object to JSON string)
     *
     * @param obj 目标对象
     * @return JSON字符串
     */
    public static String toJsonString(Object obj) {
        return INSTANCE.toJsonString(obj, new JSONConfig());
    }

    /**
     * 将对象转化为json字符串
     *
     * @param obj     目标对象
     * @param options 配置选项
     * @return JSON字符串
     */
    public static String toJsonString(Object obj, WriteOption... options) {
        return INSTANCE.toJsonString(obj, options);
    }

    /**
     * 将对象转化为美化后的json字符串
     *
     * @param obj
     * @return
     */
    public static String toPrettifyJsonString(Object obj) {
        return INSTANCE.toPrettifyJsonString(obj);
    }

    /**
     * 将json字符串转为美化后的json字符串
     *
     * @param json
     * @return
     */
    public static String prettifyJsonString(String json) {
        return INSTANCE.prettifyJsonString(json);
    }

    /**
     * 将对象序列化为json字节数组
     *
     * @param obj
     * @param options
     * @return
     */
    public static byte[] toJsonBytes(Object obj, WriteOption... options) {
        return INSTANCE.toJsonBytes(obj, EnvUtils.CHARSET_UTF_8, JSONConfig.of(options));
    }

    /**
     * 将对象序列化为json字节数组
     *
     * @param obj
     * @param jsonConfig
     * @return
     */
    public static byte[] toJsonBytes(Object obj, JSONConfig jsonConfig) {
        return INSTANCE.toJsonBytes(obj, EnvUtils.CHARSET_UTF_8, jsonConfig);
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
        return INSTANCE.toJsonBytes(obj, charset, JSONConfig.of(options));
    }

    /**
     * 将对象序列化为json字节数组
     *
     * @param obj
     * @param charset    指定编码
     * @param jsonConfig
     * @return
     */
    public static byte[] toJsonBytes(Object obj, Charset charset, JSONConfig jsonConfig) {
        return INSTANCE.toJsonBytes(obj, charset, jsonConfig);
    }

    /**
     * serialize obj
     *
     * @param obj        序列化对象
     * @param jsonConfig 配置
     * @return JSON字符串
     */
    public static String toJsonString(Object obj, JSONConfig jsonConfig) {
        return INSTANCE.toJsonString(obj, jsonConfig);
    }

    /**
     * 将对象序列化为json并写入到file文件中
     *
     * @param object
     * @param file
     * @param options
     */
    public static void writeJsonTo(Object object, File file, WriteOption... options) {
        INSTANCE.writeJsonTo(object, file, options);
    }

    /**
     * 将对象序列化内容直接写入os
     *
     * @param object
     * @param os
     * @param options
     */
    public static void writeJsonTo(Object object, OutputStream os, WriteOption... options) {
        INSTANCE.writeJsonTo(object, os, EnvUtils.CHARSET_UTF_8, JSONConfig.of(options));
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
        INSTANCE.writeJsonTo(object, os, charset, JSONConfig.of(options));
    }

    /**
     * 将对象序列化内容直接写入os
     *
     * @param object
     * @param os
     * @param jsonConfig
     */
    public static void writeJsonTo(Object object, OutputStream os, JSONConfig jsonConfig) {
        INSTANCE.writeJsonTo(object, os, EnvUtils.CHARSET_UTF_8, jsonConfig);
    }

    /**
     * 将对象序列化内容直接写入os
     *
     * @param object
     * @param os
     * @param charset
     * @param jsonConfig
     */
    public static void writeJsonTo(Object object, OutputStream os, Charset charset, JSONConfig jsonConfig) {
        INSTANCE.writeJsonTo(object, os, charset, jsonConfig);
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
        INSTANCE.writeJsonTo(object, writer, options);
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
        INSTANCE.writeSuperLargeJsonTo(object, os, options);
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
        INSTANCE.writeToJSONWriter(object, JSONWriter.wrap(new OutputStreamWriter(os, charset)), JSONConfig.of(options));
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
        INSTANCE.writeToJSONWriter(object, JSONWriter.wrap(writer), JSONConfig.of(options));
    }

    /**
     * 支持自定义的对象反序列化器注册
     *
     * @param type
     * @param typeDeserializer
     */
    public synchronized static void register(Class<?> type, JSONTypeDeserializer typeDeserializer) {
        type.getClass();
        JSONStore.INSTANCE.register(typeDeserializer, type);
    }

    /**
     * 支持自定义的对象序列化器注册
     *
     * @param type
     * @param typeSerializer
     */
    public synchronized static void register(Class<?> type, JSONTypeSerializer typeSerializer) {
        INSTANCE.register(type, typeSerializer);
    }

    /**
     * 注册自定义的映射器(序列化和反序列化mapper)
     *
     * @param type
     * @param mapper
     * @param <T>
     */
    public static <T> void register(Class<T> type, final JSONTypeMapper<T> mapper) {
        INSTANCE.register(type, mapper, false);
    }

    /**
     * 注册自定义的映射器(序列化和反序列化mapper)
     *
     * @param type
     * @param mapper
     * @param applyAllSubClass 是否应用到所有子类
     * @param <T>
     */
    public static <T> void register(Class<T> type, final JSONTypeMapper<T> mapper, boolean applyAllSubClass) {
        INSTANCE.register(type, mapper, applyAllSubClass);
    }

    /**
     * 全局关闭JIT优化（注: 即使不关闭，也仅仅序列化支持启用JIT，反序列化目前依然使用反射不支持JIT）
     */
    public static void disableJIT() {
        INSTANCE.disableJIT();
    }

    /**
     * 模块注册
     *
     * @param moduleClass
     */
    public static void register(Class<? extends JSONTypeModule> moduleClass) {
        INSTANCE.register(moduleClass);
    }

    /**
     * 模块注册
     *
     * @param module
     */
    public static void register(JSONTypeModule module) {
        INSTANCE.register(module);
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
     * json -> double
     * <p>
     * 读取byte[]数组解析为double值，支持+-开头及D,d,F,f,L,l结尾及科学计数法+-e(E),并对特殊字符串做了兼容处理包括前后置的空格和0字符等比如
     * <li color=green>"00000002.3456000000d "</li>
     * <li color=green>" 00.000000000000002345678988765432345876544d"</li>
     * <li color=green>"1.0000000000000002345678988765432345876544d"</li>
     * <li color=green>"123e+12f"</li>
     * <li color=green>"-123e-12d"</li>
     * <p>
     * 如果输入不合法会抛出一个异常，例如:
     * <li color=red>"123.5g"</li>
     * <li color=red>"123.5p123"</li>
     * <p>
     * double输入解析例如: JSON.parseDouble("123e10"), 类似Double.parseDouble("123e10")
     * 和常规JSON中的double字段解析区别是没有预期的结束token(',', '}', ']')，所以需要考虑数据越界的问题
     *
     * @param json
     * @return
     */
    public static double parseDouble(String json) {
        if (EnvUtils.JDK_9_PLUS) {
            return parseFloats((byte[]) getStringValue(json), 0, json.length(), true);
        } else {
            return parseFloats((char[]) getStringValue(json), 0, json.length(), true);
        }
    }

    /**
     * bytes -> double
     *
     * @param buf
     * @return
     * @throws JSONException
     */
    public final static double parseDouble(byte[] buf) {
        return parseFloats(buf, 0, buf.length, true);
    }

    /**
     * bytes -> double
     *
     * @param buf
     * @param offset
     * @param endIndex
     * @return
     * @throws JSONException
     */
    public final static double parseDouble(byte[] buf, final int offset, final int endIndex) {
        return parseFloats(buf, offset, endIndex, true);
    }

    public static float parseFloat(String json) {
        if (EnvUtils.JDK_9_PLUS) {
            return (float) parseFloats((byte[]) getStringValue(json), 0, json.length(), false);
        } else {
            return (float) parseFloats((char[]) getStringValue(json), 0, json.length(), false);
        }
    }

    /**
     * bytes -> float
     *
     * @param buf
     * @return
     * @throws JSONException
     */
    public final static float parseFloat(byte[] buf) {
        return (float) parseFloats(buf, 0, buf.length, false);
    }

    /**
     * bytes -> float
     *
     * @param buf
     * @param offset
     * @param endIndex
     * @return
     * @throws JSONException
     */
    public final static float parseFloat(byte[] buf, final int offset, final int endIndex) {
        return (float) parseFloats(buf, offset, endIndex, false);
    }

    /**
     * 读取char[]数组解析为double值，支持+-开头及D,d,F,f,L,l结尾及科学计数法+-e(E),并对特殊字符串做了兼容处理包括前后置的空格和0字符等，比如00000002.3456000000
     * <p>
     * double输入解析例如: JSON.parseDouble("123e10"), 类似Double.parseDouble("123e10")
     * 和常规JSON中的double字段解析区别是没有预期的结束token(',', '}', ']')，所以需要考虑数据越界的问题
     * bytes -> double
     *
     * @param buf
     * @return
     */
    public final static double parseDouble(char[] buf) {
        return parseFloats(buf, 0, buf.length, true);
    }

    /**
     * 读取char[]数组解析为double值，支持+-开头及D,d,F,f,L,l结尾及科学计数法+-e(E),并对特殊字符串做了兼容处理包括前后置的空格和0字符等，比如00000002.3456000000
     * <p>
     * double输入解析例如: JSON.parseDouble("123e10"), 类似Double.parseDouble("123e10")
     * 和常规JSON中的double字段解析区别是没有预期的结束token(',', '}', ']')，所以需要考虑数据越界的问题
     * bytes -> double
     *
     * @param buf
     * @param offset
     * @param endIndex
     * @return
     */
    public final static double parseDouble(char[] buf, final int offset, final int endIndex) {
        return parseFloats(buf, offset, endIndex, true);
    }

    /**
     * chars -> float
     *
     * @param buf
     * @return
     * @throws JSONException
     */
    public final static float parseFloat(char[] buf) {
        return (float) parseFloats(buf, 0, buf.length, false);
    }

    // Faster than toJsonString
    public final static String toString(float value) {
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = JSONGeneral.CACHED_BYTES_36.get();
            int len = JSONWriter.writeFloat(value, bytes, 0);
            if (bytes[0] == 'n') {
                if (value == Float.POSITIVE_INFINITY) {
                    return "Infinity";
                } else if (value == Float.NEGATIVE_INFINITY) {
                    return "-Infinity";
                } else {
                    return "NaN";
                }
            }
            return JSONMemoryHandle.createAsciiString(bytes, 0, len);
        } else {
            char[] chars = JSONGeneral.CACHED_CHARS_36.get();
            int len = JSONWriter.writeFloat(value, chars, 0);
            if (chars[0] == 'n') {
                if (value == Float.POSITIVE_INFINITY) {
                    return "Infinity";
                } else if (value == Float.NEGATIVE_INFINITY) {
                    return "-Infinity";
                } else {
                    return "NaN";
                }
            }
            return new String(chars, 0, len);
        }
    }

    // Faster than toJsonString
    public final static String toString(double value) {
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = JSONGeneral.CACHED_BYTES_36.get();
            int len = JSONWriter.writeDouble(value, bytes, 0);
            if (bytes[0] == 'n') {
                if (value == Double.POSITIVE_INFINITY) {
                    return "Infinity";
                } else if (value == Double.NEGATIVE_INFINITY) {
                    return "-Infinity";
                } else {
                    return "NaN";
                }
            }
            return JSONMemoryHandle.createAsciiString(bytes, 0, len);
        } else {
            char[] chars = JSONGeneral.CACHED_CHARS_36.get();
            int len = JSONWriter.writeDouble(value, chars, 0);
            if (chars[0] == 'n') {
                if (value == Double.POSITIVE_INFINITY) {
                    return "Infinity";
                } else if (value == Double.NEGATIVE_INFINITY) {
                    return "-Infinity";
                } else {
                    return "NaN";
                }
            }
            return new String(chars, 0, len);
        }
    }

    // Faster than toJsonString
    public final static String toString(long value) {
        if (value == Long.MIN_VALUE) {
            return "-9223372036854775808";
        }
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = JSONGeneral.CACHED_BYTES_36.get();
            int offset = 0;
            if (value < 0) {
                bytes[offset++] = '-';
                value = -value;
            }
            offset += JSONWriter.writeLong(value, bytes, offset);
            return JSONMemoryHandle.createAsciiString(bytes, 0, offset);
        } else {
            char[] chars = JSONGeneral.CACHED_CHARS_36.get();
            int offset = 0;
            if (value < 0) {
                chars[offset++] = '-';
                value = -value;
            }
            offset += JSONWriter.writeLong(value, chars, offset);
            return new String(chars, 0, offset);
        }
    }
}
