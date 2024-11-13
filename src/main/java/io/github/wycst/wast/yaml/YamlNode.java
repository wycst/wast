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

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.reflect.ClassStrucWrap;
import io.github.wycst.wast.common.reflect.SetterInfo;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;

/**
 * yaml 节点树模型
 * 行单位解析
 *
 * @Author: wangy
 * @Date: 2022/3/27 17:57
 * @Description:
 */
public class YamlNode extends YamlLine {

    /**
     * 根节点
     */
    private YamlNode root;

    /**
     * 父节点
     */
    private YamlNode parent;

    /**
     * 属性子节点
     */
    private Map<String, YamlNode> fieldNodes;

    /**
     * 数组子节点
     */
    private List<YamlNode> children;

    /**
     * 所有节点平铺列表（根节点下）
     */
    private List<YamlNode> yamlNodes;

    /**
     * 包含锚点的节点列表（根节点下）
     */
    private List<YamlNode> anchorYamlNodes;

    /**
     * 是否数组类型
     */
    private boolean array;

    /**
     * 引用节点
     */
    private YamlNode reference;

    /**
     * 构造函数
     */
    YamlNode() {
    }

    void buildYamlTree(List<YamlNode> yamlNodes) {
        this.yamlNodes = yamlNodes;
        this.root = this;
        // 只有根节点才有anchorYamlNodes
        this.anchorYamlNodes = new ArrayList<YamlNode>();
        // 根据缩进构建树结构关系
        YamlNode prev = null;
        for (YamlNode yamlNode : yamlNodes) {
            yamlNode.root = this;
            if (prev == null) {
                setParent(yamlNode, this);
            } else {
                int indent = yamlNode.indent;
                int prevIndent = prev.indent;
                // 查找父节点
                // 如果indent大于prevIndent，则prev为当前父节点，否则根据prev向上查找（包含prev）缩进=indent的为同胞节点，取其parent为父节点构建父子关系
                // 如果缩进小于根节点的缩进抛出异常
                if (indent > prevIndent) {
                    if (prev.leaf) {
                        throw new YamlParseException("indent value(" + indent + ") error, The indent value of the leaf node cannot be greater than the indent value(" + prevIndent + ") of the prev node, at lineNum " + (yamlNode.lineNum + 1));
                    }
                    // prev即为父节点
                    setParent(yamlNode, prev);
                } else if (indent == prevIndent) {
                    if (yamlNode.arrayToken && !prev.arrayToken) {
                        if (prev.leaf) {
                            throw new YamlParseException("indent value(" + indent + ") error, The indent value of the leaf node cannot be greater than the indent value(" + prevIndent + ") of the prev node, at lineNum " + yamlNode.lineNum);
                        }
                        // prev即为父节点
                        setParent(yamlNode, prev);
                    } else {
                        // 与上一个同胞节点
                        YamlNode parent = prev.parent;
                        //                    if (parent.leaf) {
                        //                        // 理论上代码不可达，parent一定不是叶子节点，否则判断前置节点关系时已抛出异常
                        //                        throw new YamlParseException("indent value(" + indent + ") error,The indent value of the leaf node cannot be greater than the indent value(" + prevIndent + ") of the prev node, at lineNum " + yamlNode.lineNum);
                        //                    }
                        setParent(yamlNode, parent);
                        // 相邻的同胞确定为叶子节点
                        if (prev.reference == null) prev.leaf = true;
                    }
                } else {
                    // 向上查找
                    YamlNode parent = prev.parent;
                    // 上一个节点确定为叶子节点
                    if (prev.reference == null) prev.leaf = true;
                    while (parent != null) {
                        int parentIndent = parent.indent;
                        if (indent > parentIndent) {
                            // 缩进值错误只能小于或者等于parentIndent，如果等于while结束
                            throw new RuntimeException("indent value " + indent + " error, the indent should less than or equal to " + parentIndent + ", at lineNum " + (yamlNode.lineNum + 1));
                        }
                        parent = parent.parent;
                        if (indent == parentIndent) {
                            // 命中父节点
                            break;
                        }
                    }
                    if (parent.leaf) {
                        throw new YamlParseException("indent value(" + indent + ") error, The indent value of the leaf node cannot be greater than the indent value(" + prevIndent + ") of the prev node, at lineNum " + yamlNode.lineNum);
                    }
                    setParent(yamlNode, parent);
                }
            }
            prev = yamlNode;

            // 将锚点节点放入列表
            if (yamlNode.anchorKey != null) {
                anchorYamlNodes.add(yamlNode);
            }

            // 解析引用
            if (yamlNode.referenceKey != null) {
                yamlNode.reference = getReference(anchorYamlNodes, yamlNode.referenceKey, yamlNode.lineNum);
                yamlNode.array = yamlNode.reference.array;
                yamlNode.leaf = yamlNode.reference.leaf;
            }
        }
    }

    /**
     * 根据key获取引用节点
     *
     * @param anchorYamlNodes
     * @param key
     * @param lineNum
     * @return
     */
    private YamlNode getReference(List<YamlNode> anchorYamlNodes, String key, int lineNum) {
        int len = anchorYamlNodes.size();
        for (int i = len - 1; i > -1; i--) {
            YamlNode yamlNode = anchorYamlNodes.get(i);
            if (yamlNode.lineNum >= lineNum) continue;
            if (yamlNode.anchorKey.equals(key)) {
                return yamlNode;
            }
        }
        throw new YamlParseException("cannot find reference *" + key + " at lineNum " + lineNum);
    }

    private void setParent(YamlNode yamlNode, YamlNode parent) {
        yamlNode.parent = parent;
        if (yamlNode.arrayToken) {
            parent.array = true;
            if (parent.children == null) {
                parent.children = new ArrayList<YamlNode>();
            }
            parent.children.add(yamlNode);
            yamlNode.arrayIndex = parent.children.size() - 1;
        } else {
            if (parent.fieldNodes == null) {
                parent.fieldNodes = new LinkedHashMap<String, YamlNode>();
            }
            String key = yamlNode.key;
            String field = key;
            if (key.startsWith("\"") && key.endsWith("\"")) {
                field = key.substring(1, key.length() - 1);
            }
            parent.fieldNodes.put(field, yamlNode);
        }
    }

    void appendFullKey(StringBuilder builder) {
        if (parent != root) {
            parent.appendFullKey(builder);
            builder.append('.');
        }
        if (arrayToken) {
            builder.append('[').append(arrayIndex).append(']');
        } else {
            builder.append(key);
        }
    }

    public String getFullKey() {
        StringBuilder builder = new StringBuilder();
        appendFullKey(builder);
        return builder.toString();
    }

    /**
     * Convert node to properties
     * Note: if there is an object reference, it will be ignored and the properties of the root node will always be returned
     */
    public Properties toProperties() {
        List<YamlNode> yamlNodes = this.yamlNodes;
        if (root != null) {
            yamlNodes = root.yamlNodes;
        }
        StringBuilder builder = new StringBuilder();
        Properties properties = new Properties();
        for (YamlNode yamlNode : yamlNodes) {
            if (yamlNode.leaf) {
                builder.setLength(0);
                yamlNode.appendFullKey(builder);
                Object value = yamlNode.getValue();
                properties.setProperty(builder.toString(), value == null ? "" : String.valueOf(value));
            }
        }
        return properties;
    }

    /**
     * 将节点转换为map结构
     */
    public Map toMap() {
        if (this.array) {
            throw new UnsupportedOperationException("YamlNode is array does not support toMap. Please call toList");
        }
        if (fieldNodes == null) {
            return null;
        }
        Map map = new LinkedHashMap();
        for (Map.Entry<String, YamlNode> entry : fieldNodes.entrySet()) {
            String field = entry.getKey();
            YamlNode node = entry.getValue();
            putNodeValueOfMap(field, node, map);
        }
        return map;
    }

    private void putNodeValueOfMap(String field, YamlNode node, Map map) {
        if (node.leaf) {
            map.put(field, node.getValue());
        } else {
            String key = node.key;
            YamlNode reference = node.reference;
            if (reference != null) {
                node = reference;
            }
            if (node.array) {
                map.put(field, node.toList());
            } else {
                Map valueMap = node.toMap();
                if ("<<".equals(field) && key.length() == 2) {
                    map.putAll(valueMap);
                } else {
                    map.put(field, valueMap);
                }
            }
        }
    }

    /**
     * 将节点转换为map结构
     */
    public List toList() {
        if (!this.array) {
            throw new UnsupportedOperationException("Method toList is  does not support . Please call toMap()");
        }
        if (children == null) {
            return null;
        }
        List<Object> list = new ArrayList<Object>();
        for (YamlNode yamlNode : children) {
            if (yamlNode.leaf) {
                list.add(yamlNode.getValue());
            } else {
                if (yamlNode.array) {
                    list.add(yamlNode.toList());
                } else {
                    list.add(yamlNode.toMap());
                }
            }
        }
        return list;
    }

    /***
     * 转化为指定的实体bean
     *
     * @param actualType
     * @param <T>
     * @return
     */
    public <T> T toEntity(Class<T> actualType) {

        if (leaf) {
            throw new UnsupportedOperationException(" NodeValue is a leaf value, please call method getValue() instead !");
        }
        if (actualType == null) return (T) toMap();
        if (actualType == String.class || actualType.isArray() || actualType.isEnum() || actualType.isPrimitive()) {
            throw new UnsupportedOperationException(" ActualType " + actualType + " is not  supported!");
        }

        Object instance;
        boolean isMapInstance;
        ClassStrucWrap classStrucWrap = null;
        if (actualType == null || actualType == Map.class || actualType == LinkedHashMap.class) {
            instance = new LinkedHashMap();
            isMapInstance = true;
        } else {
            classStrucWrap = ClassStrucWrap.get(actualType);
            isMapInstance = classStrucWrap.isAssignableFromMap();
            try {
                if (!isMapInstance) {
                    instance = classStrucWrap.newInstance();
                } else {
                    instance = actualType.newInstance();
                }
            } catch (Throwable throwable) {
                throw new YamlParseException(" new instance error ", throwable);
            }
        }

        // 开始转换
        for (Map.Entry<String, YamlNode> entry : fieldNodes.entrySet()) {
            Serializable key = entry.getKey();
            YamlNode yamlNode = entry.getValue();
            if (isMapInstance) {
                Map map = (Map) instance;
                putNodeValueOfMap((String) key, yamlNode, map);
            } else {
                String fieldName = key.toString();
                SetterInfo setterInfo = classStrucWrap.getSetterInfo(fieldName);
                if (setterInfo != null) {
                    Class<?> parameterType = setterInfo.getParameterType();
                    Class<?> entityClass = parameterType;
                    Class<?> collCls = null;
                    Class<?> actualTypeArgumentType = setterInfo.getActualTypeArgument();
                    if (actualTypeArgumentType != null) {
                        // 取范型的类
                        entityClass = actualTypeArgumentType;
                        collCls = parameterType;
                    }

                    Object value;
                    if (yamlNode.array) {
                        value = yamlNode.toCollection(collCls, entityClass);
                    } else {
                        if (Date.class.isAssignableFrom(parameterType)) {
                            // 日期类型
                            value = yamlNode.getDate((Class<? extends Date>) parameterType);  // getDate(yamlNode.getValue(), (Class<? extends Date>) parameterType);
                        } else {
                            value = yamlNode.getValue(entityClass);
                        }
                    }
                    setterInfo.invoke(instance, value);
                } else {
                    // if throw an exception ?
//                    throw new YamlParseException("no setter method for field '" + fieldName + "' on " + actualType);
                }
            }
        }
        return (T) instance;
    }

    /**
     * 日期对象
     */
    public Date getDate(Class<? extends Date> dateType) {
        if (this.typeOfValue != null && this.typeOfValue instanceof Date) {
            return (Date) this.typeOfValue;
        }
        return parseDatetime(String.valueOf(value));
    }

    public Object toCollection(Class<?> collectionCls, Class<?> entityClass) {

        if (children == null) return null;
        int size = children.size();
        Class target = entityClass;
        Collection collection = null;
        boolean isArrayCls = false;
        Object arrayObj = null;

        if (collectionCls == null || collectionCls == ArrayList.class) {
            collection = new ArrayList<Object>();
        } else {
            isArrayCls = collectionCls.isArray();
            if (isArrayCls) {
                arrayObj = Array.newInstance(entityClass, children.size());
            } else {
                if (collectionCls.isInterface()) {
                    if (Set.class.isAssignableFrom(collectionCls)) {
                        collection = new LinkedHashSet<Object>();
                    } else if (List.class.isAssignableFrom(collectionCls)) {
                        collection = new ArrayList<Object>();
                    }
                } else {
                    if (collectionCls == HashSet.class) {
                        collection = new HashSet<Object>();
                    } else if (collectionCls == Vector.class) {
                        collection = new Vector<Object>();
                    } else {
                    }
                }
            }
        }

        for (int i = 0; i < size; i++) {
            YamlNode yamlNode = children.get(i);
            Object result = null;
            if (yamlNode.array) {
                result = yamlNode.toList();
            } else {
                result = yamlNode.getValue(target);
            }
            if (isArrayCls) {
                Array.set(arrayObj, i, result);
            } else {
                collection.add(result);
            }
        }

        return isArrayCls ? arrayObj : collection;
    }

    /**
     * 返回配置的字符串值
     *
     * @return
     */
    public String getRawValue() {
        return value;
    }

    /***
     * 获取计算值
     *
     * @return
     */
    public Object getValue() {
        if (this.reference != null) {
            return this.reference.getValue();
        }

        if (this.typeOfValue != null) {
            return this.typeOfValue;
        }

        if (valueType > 0) {
            switch (valueType) {
                // str
                case 1:
                    // binary 按字符串处理
                case 5: {
                    this.typeOfValue = String.valueOf(value);
                    break;
                }
                // float
                case 2: {
                    this.typeOfValue = Float.parseFloat(value);
                    break;
                }
                // int
                case 3: {
                    this.typeOfValue = Integer.parseInt(value);
                    break;
                }
                // bool
                case 4: {
                    if (value == null) {
                        this.typeOfValue = false;
                        break;
                    }
                    boolean bool;
                    char ch = value.charAt(0);
                    int len = value.length();
                    if (len == 4 && (ch == 'T' || ch == 't') && value.endsWith("rue")) {
                        bool = true;
                    } else if (len == 5 && (ch == 'F' || ch == 'f') && value.endsWith("alse")) {
                        bool = false;
                    } else if (value.equalsIgnoreCase("ON")) {
                        bool = true;
                    } else if (value.equalsIgnoreCase("OFF")) {
                        bool = false;
                    } else if (value.equals("1")) {
                        bool = true;
                    } else if (value.equals("0")) {
                        bool = false;
                    } else {
                        throw new YamlParseException("value '" + value + "' cannot transform to bool type");
                    }
                    this.typeOfValue = bool;
                    break;
                }
                // timestamp
                case 6: {
                    // 转化为日期对象的时间戳
                    this.typeOfValue = parseDatetime(value);
                    break;
                }
            }

            return this.typeOfValue;
        }

        return value;
    }

    private Date parseDatetime(String value) {
        // 暂定几种支持格式
        // 2018-01-01t16:59:43.103-05:00
        // 2018-01-01 16:59:43.010
        // 2018-01-01 16:59:43.110+8
        char[] buffers = value.toCharArray();
        int len = value.length(), to = len;
        int from = 0;
        String timezoneIdAt = null;
        TimeZone timeZone = null;
        if (len > 23) {
            // yyyy-MM-ddTHH:mm:ss.SSS+XX:YY
            int j = len - 1, ch;
            while (j > from) {
                if ((ch = value.charAt(--j)) == '.') break;
                if (ch == '+' || ch == '-' || ch == 'Z') {
                    timezoneIdAt = new String(buffers, j, len - j);
                    to = j;
                    len = to - from;
                    break;
                }
            }
            if (timezoneIdAt != null) {
                if (timezoneIdAt.startsWith("GMT")) {
                    timeZone = TimeZone.getTimeZone(timezoneIdAt);
                } else {
                    timeZone = TimeZone.getTimeZone("GMT" + timezoneIdAt);
                }
            }
        }

        long time = autoMatchDate(buffers, from, len, timeZone);
        if (time > -1) {
            return new Timestamp(time);
        }

        return null;
    }

    private long autoMatchDate(char[] buf, int from, int len, TimeZone timeZone) {
        if (len == 19 || len == 21 || len == 22 || len == 23) {
            try {
                int year = parseInt(buf, from + 0, 4, 10);
                int month = parseInt(buf, from + 5, 2, 10);
                int day = parseInt(buf, from + 8, 2, 10);
                int hour = parseInt(buf, from + 11, 2, 10);
                int minute = parseInt(buf, from + 14, 2, 10);
                int second = parseInt(buf, from + 17, 2, 10);
                int millsecond = 0;
                if (len > 20) {
                    millsecond = parseInt(buf, from + 20, len - 20, 10);
                }
                GregorianDate date = new GregorianDate(year, month, day, hour, minute, second, millsecond, timeZone);
                return date.getTime();
            } catch (Throwable throwable) {
            }
        }
        return -1;
    }

    /**
     * 转化指定类型
     */
    public <E> Object getValue(Class<E> typeClass) {
        Object value = getValue();
        if (value == null) {
            return null;
        }
        String strValue = String.valueOf(value);
        if (typeClass == null || typeClass == String.class) {
            return strValue;
        } else if (typeClass == int.class || typeClass == Integer.class) {
            return (E) Integer.valueOf(strValue);
        } else if (typeClass == long.class || typeClass == Long.class) {
            return (E) Long.valueOf(strValue);
        } else if (typeClass == double.class || typeClass == Double.class) {
            return (E) Double.valueOf(strValue);
        } else if (typeClass == float.class || typeClass == Float.class) {
            return (E) Float.valueOf(strValue);
        } else if (typeClass == byte.class || typeClass == Byte.class) {
            return (E) Byte.valueOf(strValue);
        } else if (typeClass == BigDecimal.class) {
            return (E) new BigDecimal(strValue);
        } else if (typeClass == BigInteger.class) {
            return (E) new BigInteger(strValue);
        } else if (Number.class.isAssignableFrom(typeClass)) {
            if (strValue.indexOf('.') > -1) {
                return Double.valueOf(strValue);
            } else {
                return Long.valueOf(strValue);
            }
        } else if (typeClass == Boolean.class || typeClass == boolean.class) {
            if (strValue.equalsIgnoreCase("true")) {
                return true;
            } else if (strValue.equalsIgnoreCase("false")) {
                return false;
            } else {
                throw new IllegalArgumentException(String.format("%s Cannot convert to Boolean value ", strValue));
            }
        } else if (typeClass == Date.class) {
            return parseDatetime(strValue);
        } else if (Enum.class.isAssignableFrom(typeClass)) {
            Class enumCls = typeClass;
            return Enum.valueOf(enumCls, strValue);
        } else {

        }
        return null;
    }

    public YamlNode root() {
        return root;
    }

    public YamlNode parent() {
        return parent;
    }

    public boolean isArray() {
        return array;
    }

    /***
     * 将节点信息写入writer
     * （保持缩进不变，原来文件中的注释会丢失）
     * @param writer
     */
    public void writeTo(Writer writer) throws IOException {

        writeIndent(writer);
        if (this.key != null) {
            writer.write(key);
            writer.write(": ");
            if (!leaf) {
                writeNewLine(writer);
            }
        } else {
            if (!leaf) {
                if (arrayIndex > -1) {
                    writer.write("- ");
                    writeNewLine(writer);
                }
            } else {
                //writeIndent(writer);
            }
        }

        if (array) {
            List<YamlNode> children = this.children;
            if (children == null) {
                children = reference.children;
            }
            int size = children.size();
            int i = 0;
            for (YamlNode yamlNode : children) {
                yamlNode.writeTo(writer);
                if (++i < size) {
                    writeNewLine(writer);
                }
            }
        } else {
            if (leaf) {
                writeLeafValue(writer);
            } else {
                Map<String, YamlNode> fieldNodes = this.fieldNodes;
                if (fieldNodes == null) {
                    fieldNodes = reference.fieldNodes;
                }
                int size = fieldNodes.size();
                int i = 0;
                for (Map.Entry<String, YamlNode> entry : fieldNodes.entrySet()) {
                    YamlNode node = entry.getValue();
                    node.writeTo(writer);
                    if (++i < size) {
                        writeNewLine(writer);
                    }
                }
            }
        }
    }

    private void writeLeafValue(Writer writer) throws IOException {
        writeArrayToken(writer);
        writeTypeToken(writer);

        Object val = getValue();
        String value;
        if (val instanceof Map || val instanceof List) {
            value = YamlJSON.stringify(val);
        } else {
            value = val.toString();
            // 普通文本字符串添加单引号
            if (valueType == 1 && !this.textBlock) {
                value = "'" + value + "'";
            }
        }

        if (this.textBlock) {
            writeBlockToken(writer);
            writeNewLine(writer);

            String[] lines = value.split("\n");
            int len = lines.length;
            int i = 0;
            for (String line : lines) {
                writeIndent(writer, indent + 1);
                writer.write(line);
                if (++i < len) {
                    writeNewLine(writer);
                }
            }
        } else {
            writer.write(value);
        }
    }

    /**
     * 获取指定路径的对象并类型转化
     *
     * @param childPath
     * @param clazz
     * @param <E>
     * @return
     */
    public <E> E getPathValue(String childPath, Class<E> clazz) {
        YamlNode yamlNode = get(childPath);
        if (yamlNode == null)
            return null;
        if (yamlNode.leaf) {
            return (E) yamlNode.getValue(clazz);
        }
        return yamlNode.toEntity(clazz);
    }

    /**
     * 获取指定路径的节点对象
     * <p>如果path为空将返回当前YamlNode节点对象 </p>
     * <p>如果/开头表示从根节点开始查找 </p>
     * <p>数组元素使用[n]</p>
     *
     * @param childPath 查找路径，以/作为级别分割线
     * @return
     */
    public YamlNode get(String childPath) {
        if (childPath == null || (childPath = childPath.trim()).length() == 0)
            return this;
        if (childPath.startsWith("/")) {
            return root.get(childPath.substring(1));
        }
        if (leaf) {
            return null;
        }
        int splitIndex = childPath.indexOf('/');
        if (splitIndex == -1) {
            return getChildNode(childPath);
        } else {
            String path = childPath.substring(0, splitIndex).trim();
            YamlNode childNode = getChildNode(path);
            if (childNode != null) {
                String nextPath = childPath.substring(splitIndex + 1).trim();
                return childNode.get(nextPath);
            }
        }
        return null;
    }

    private YamlNode getChildNode(String path) {
        YamlNode value = null;
        if (array) {
            if (path.startsWith("[") && path.endsWith("]")) {
                int index = Integer.parseInt(path.substring(1, path.length() - 1));
                return children.get(index);
            } else {
                throw new IllegalArgumentException("current JSONNode is an isArray type, path should like []");
            }
        } else {
            // map获取属性
            if (fieldNodes != null) {
                value = fieldNodes.get(path);
            }
        }
        return value;
    }

    /**
     * 给指定路径节点设值
     * <p> 只支持叶子节点
     *
     * @param path
     * @param value
     */
    public void setPathValue(String path, Serializable value) {
        YamlNode node = get(path);
        if (node != null && node.leaf) {
            node.typeOfValue = value;
        }
    }

}
