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
package io.github.wycst.wast.yaml;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * yaml文档结构：
 * <p>
 * 支持字符串，字符数组，文件，输入流，远程URL的读取解析
 * 支持对象类型转化；
 *
 * @Author: wangy
 * @Date: 2022/5/21 17:51
 * @Description:
 */
public final class YamlDocument extends YamlParser {

    /**
     * 是否存在以---分割的多文档模式
     */
    private boolean multiple;

    /**
     * 根节点（虚拟）
     */
    private YamlNode root;

    /**
     * 如果multiple为true,解析为集合类型，每个node都是一个root
     */
    private List<YamlNode> yamlNodeList;

    private YamlDocument() {
    }

    /***
     * 解析yaml格式的字符串
     *
     * @param yaml
     * @return
     */
    public static YamlDocument parse(String yaml) {
        return parse(getChars(yaml));
    }

    /***
     * 解析字符数组
     *
     * @param source
     * @return
     */
    public static YamlDocument parse(char[] source) {

        int length = source.length;
        char lastChar = source[source.length - 1];
        // 最后以换行符结束，可以避免越界检查
        // todo 性能优化点
        if (lastChar != '\n') {
            source = Arrays.copyOf(source, length + 1);
            source[length] = '\n';
        }

        YamlDocument yamlDocument = new YamlDocument();
        yamlDocument.source = source;
        yamlDocument.parseYamlRoot(0);
        return yamlDocument;
    }

    /**
     * In yaml scenarios, large files will not appear. Read the complete stream directly and then parse it
     *
     * @param is
     * @return
     * @throws IOException
     */
    public static YamlDocument read(InputStream is) throws IOException {
        InputStreamReader streamReader = new InputStreamReader(is);
        try {
            int buffSize = 8192;
            char[] source = null;
            char[] buff = new char[buffSize];
            int count = 0;
            int len;
            while ((len = streamReader.read(buff)) > 0) {
                if (source == null) {
                    source = Arrays.copyOf(buff, len);
                } else {
                    source = Arrays.copyOf(source, count + len);
                    System.arraycopy(buff, 0, source, count, len);
                }
                count += len;
            }
            return parse(source);
        } finally {
            streamReader.close();
        }
    }

    public static YamlDocument read(File yamlFile) throws IOException {
        return read(new FileInputStream(yamlFile));
    }

    public static YamlDocument read(URL url) throws IOException {
        return read(url, YamlDocument.class);
    }

    /***
     * 解析根节点
     *
     * @param offset
     */
    protected void parseYamlRoot(int offset) {
        YamlNode yamlRoot = new YamlNode();
        if (root == null) {
            root = yamlRoot;
        } else {
            this.multiple = true;
            if (yamlNodeList == null) {
                yamlNodeList = new ArrayList<YamlNode>();
                yamlNodeList.add(root);
            }
            yamlNodeList.add(yamlRoot);
        }
        List<YamlNode> yamlNodes = new ArrayList<YamlNode>();
        parseNodes(yamlNodes, offset, source.length);
        // 构建yaml树结构
        yamlRoot.buildYamlTree(yamlNodes);
    }


    /**
     * 解析字符串转化为指定类型
     *
     * @param yamlStr
     * @param actualType
     * @param <T>
     * @return
     */
    public static <T> T parse(String yamlStr, Class<T> actualType) {
        YamlDocument yamlDocument = parse(yamlStr);
        return yamlDocument.getRoot().toEntity(actualType);
    }

    /***
     * 解析字符数组转化为指定类型
     *
     * @param buf
     * @param actualType
     * @param <T>
     * @return
     */
    public static <T> T parse(char[] buf, Class<T> actualType) {
        YamlDocument yamlDocument = parse(buf);
        return yamlDocument.getRoot().toEntity(actualType);
    }


    /**
     * 读取字节数组转化为指定class的实例
     *
     * @param bytes      字节流
     * @param actualType 实体类型
     * @return T对象
     */
    public static <T> T read(byte[] bytes, Class<T> actualType) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try {
            return read(bais, actualType);
        } catch (IOException e) {
        }
        return null;
    }

    /**
     * 读取yaml文件转化为指定class的实例
     *
     * @param file       文件
     * @param actualType 实体类型
     * @return T对象
     */
    public static <T> T read(File file, Class<T> actualType) throws IOException {
        return read(new FileInputStream(file), actualType);
    }

    /**
     * 读取远程资源yaml转化为指定class的实例
     *
     * @param url        URL资源
     * @param actualType 实体类型
     * @return T对象
     */
    public static <T> T read(URL url, Class<T> actualType) throws IOException {
        return read(url, actualType, -1);
    }

    /**
     * 读取远程资源yaml转化为指定class的实例
     *
     * @param url        URL资源
     * @param actualType 实体类型
     * @param timeout    超时时间
     * @return T对象
     */
    public static <T> T read(URL url, Class<T> actualType, int timeout) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (timeout > 0) {
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
        }
        conn.connect();
        return read(conn.getInputStream(), actualType);
    }

    /**
     * 将输入流转化为指定class的实例
     *
     * @param is         输入流
     * @param actualType 实体类型
     * @return T对象
     */
    private static <T> T read(InputStream is, Class<T> actualType) throws IOException {
        return read(is).toEntity(actualType);
    }

    /**
     * 读取属性列表
     *
     * @param is
     * @return
     * @throws IOException
     */
    public static Properties loadProperties(InputStream is) throws IOException {
        return read(is).toProperties();
    }

    public boolean isMultiple() {
        return multiple;
    }

    public YamlNode getRoot() {
        return root;
    }

    public List<YamlNode> getYamlNodeList() {
        return yamlNodeList;
    }

    /***
     * 将文档(根节点)转化为指定的实体bean
     *
     * @param actualType
     * @param <T>
     * @return
     */
    public <T> T toEntity(Class<T> actualType) {
        if (actualType == YamlDocument.class) {
            return (T) this;
        }
        if (actualType == YamlNode.class) {
            return (T) getRoot();
        }
        return getRoot().toEntity(actualType);
    }

    /***
     * 将文档(根节点)转化为map
     *
     * @return
     */
    public Map toMap() {
        return getRoot().toMap();
    }

    /***
     * 将文档(根节点)转化为properties
     *
     * @return
     */
    public Properties toProperties() {
        return getRoot().toProperties();
    }

    /**
     * 写入文件
     *
     * @param file
     * @throws IOException
     */
    public void writeTo(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        writeTo(new FileWriter(file));
    }

    /**
     * 写入指定的输出流
     *
     * @param os
     * @throws IOException
     */
    public void writeTo(OutputStream os) throws IOException {
        writeTo(new OutputStreamWriter(os));
    }

    /**
     * 写入指定的writer
     *
     * <p>简单模式下的yaml回写转化(如果包含&引用和*关联等语法时缩进会混乱，暂时不支持)</p>
     *
     * @param writer
     * @throws IOException
     */
    public void writeTo(Writer writer) throws IOException {
        try {
            if (this.multiple) {
                for (YamlNode rootNode : yamlNodeList) {
                    rootNode.writeTo(writer);
                    writer.write("---");
                }
            } else {
                this.root.writeTo(writer);
            }
            writer.flush();
        } finally {
            writer.close();
        }
    }

    /***
     * 将文档转为yaml字符串
     *
     * @return
     */
    public String toYamlString() {
        StringWriter writer = new StringWriter();
        try {
            writeTo(writer);
        } catch (IOException e) {
        }
        return writer.toString();
    }

//    /***
//     * 对象转化为yaml字符串
//     * todo
//     * @param obj
//     * @return
//     */
//    public static String toYamlString(Object obj) {
//        return null;
//    }
}
