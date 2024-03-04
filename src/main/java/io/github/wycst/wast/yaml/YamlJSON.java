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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <br> 基于yaml规则的json解析（非JSON规范的解析）
 * <br> key值和value都支持单引号，双引号，或者无引号
 * <br> 对象支持无冒号例如： {name, age} 等价于 {name: null, age: null}
 * <br> {}解析为Map实例, []解析为List实例
 * <br> 可以作为常用json解析,由于没有类型处理一般比常规的JSON解析反而快；
 *
 * @Author wangyunchao
 */
public class YamlJSON extends YamlGeneral {

    /***
     * 序列化Map
     *
     * @param obj
     * @return
     */
    public final static String stringify(Object obj) {
        StringBuilder builder = new StringBuilder();
        writeObjectTo(builder, obj);
        return builder.toString();
    }

    private static void writeObjectTo(StringBuilder builder, Object obj) {
        if (obj instanceof Map) {
            writeMapTo(builder, (Map) obj);
        } else if (obj instanceof List) {
            writeListTo(builder, (List) obj);
        } else {
            builder.append(obj);
        }
    }

    private static void writeMapTo(StringBuilder builder, Map obj) {
        builder.append("{");
        int size = obj.size();
        int i = 0;
        for (Object key : obj.keySet()) {
            builder.append(key);
            builder.append(":");
            Object value = obj.get(key);
            writeObjectTo(builder, value);
            if (++i < size) {
                builder.append(",");
            }
        }
        builder.append("}");
    }

    private static void writeListTo(StringBuilder builder, List list) {
        builder.append("[");
        int size = list.size();
        int i = 0;
        for (Object value : list) {
            writeObjectTo(builder, value);
            if (++i < size) {
                builder.append(",");
            }
        }
        builder.append("]");
    }

    /***
     * 解析yaml文件中的内联json
     *
     * @param json
     * @return
     */
    public final static Object parse(String json) {
        if (json == null) return null;
        json = json.trim();
        char[] buf = getChars(json);

        int fromIndex = 0;
        int toIndex = buf.length;
        char beginChar = buf[0];

        Object result;
        AtomicInteger endIndexHolder = new AtomicInteger();
        switch (beginChar) {
            case '{':
                result = parseJSONObject(fromIndex, toIndex, buf, endIndexHolder);
                break;
            case '[':
                result = parseJSONArray(fromIndex, toIndex, buf, endIndexHolder);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported for begin character with '" + beginChar + "'");
        }

        int endIndex = endIndexHolder.get();

        if (endIndex != toIndex - 1) {
            int wordNum = Math.min(100, buf.length - endIndex - 1);
            throw new YamlParseException("Syntax error, extra characters found, '" + new String(buf, endIndex + 1, wordNum) + "', at col " + endIndex);
        }

        return result;
    }

    private static List parseJSONArray(int fromIndex, int toIndex, char[] buf, AtomicInteger endIndexHolder) {

        List<Object> list = new ArrayList<Object>();

        int beginIndex = fromIndex + 1;
        char ch = '\0';

        // 集合数组核心token是逗号（The core token of the collection array is a comma）
        // for loop
        for (int i = beginIndex; i < toIndex; i++) {

            // clear white space characters
            while (i < toIndex && (ch = buf[i]) <= ' ') {
                i++;
            }

            // for simple element parse
            int simpleFromIndex = i, simpleToIndex = -1;

            // 如果提前遇到字符']'，说明是空集合
            //  （If the character ']' is encountered in advance, it indicates that it is an empty set）
            // 另一种可能性(语法错误): 非空集合，发生在空逗号后接结束字符']'直接抛出异常
            //  (Another possibility(Syntax error): whether the empty comma followed by the closing character ']' throws an exception)
            if (ch == ']') {
                if (list.size() > 0) {
                    throw new YamlParseException("Syntax error, not allowed ',' followed by ']', pos " + i);
                }
                endIndexHolder.set(i);
                return list;
            }
            // 是否简单元素如null, true or false or numeric or string
            // (is simple elements such as null, true or false or numeric or string)
            boolean isSimpleElement = false;
            Object value = null;
            if (ch == '{') {
                value = parseJSONObject(i, toIndex, buf, endIndexHolder);
                list.add(value);
                i = endIndexHolder.get();
            } else if (ch == '[') {
                // 2 [ array
                value = parseJSONArray(i, toIndex, buf, endIndexHolder);
                list.add(value);
                i = endIndexHolder.get();
            } else if (ch == '"' || ch == '\'') {
                // 3 string
                // When there are escape characters, the escape character needs to be parsed
                value = parseJSONString(i, toIndex, buf, ch, endIndexHolder);
                list.add(value);
                i = endIndexHolder.get();
            } else {
                // 简单元素（Simple element）
                isSimpleElement = true;
                while (i + 1 < toIndex) {
                    ch = buf[i + 1];
                    if (ch == ',' || ch == ']') {
                        break;
                    }
                    i++;
                }
            }

            // 清除空白字符（clear white space characters）
            while (i + 1 < toIndex && (ch = buf[++i]) <= ' ') ;
            if (simpleToIndex == -1) {
                simpleToIndex = i;
            }

            // 检查下一个字符是逗号还是结束符号。如果是，继续或者终止。如果不是，则抛出异常
            // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
            boolean isEnd = ch == ']';
            if (ch == ',' || isEnd) {
                if (isSimpleElement) {
                    Serializable simpleValue = (Serializable) parseSimpleValue(simpleFromIndex, simpleToIndex, buf, endIndexHolder);
                    list.add(simpleValue);
                }

                if (isEnd) {
                    endIndexHolder.set(i);
                    return list;
                }
            } else {
                throw new YamlParseException("Syntax error, unexpected token character '" + ch + "', position " + i + ", Missing ',' or ']'");
            }
        }
        throw new YamlParseException("Syntax error, the closing symbol ']' is not found ");
    }

    private static Map parseJSONObject(int fromIndex, int toIndex, char[] buf, AtomicInteger endIndexHolder) {
        Map<String, Object> instance = new LinkedHashMap<String, Object>();

        int beginIndex = fromIndex + 1;
        char ch = '\0';
        String key = null;

        boolean empty = true;

        // for loop to parse
        for (int i = beginIndex; i < toIndex; i++) {
            // clear white space characters
            while (i < toIndex && (ch = buf[i]) <= ' ') {
                i++;
            }
            int fieldKeyFrom = i, fieldKeyTo, splitIndex, simpleToIndex = -1;
            // Standard JSON field name with "
            if (ch == '"') {
                while (i + 1 < toIndex && buf[++i] != '"') ;
                empty = false;
                i++;
            } else {
                if (ch == '}') {
                    if (!empty) {
                        throw new YamlParseException("Syntax error, the closing symbol '}' is not allowed at pos " + i);
                    }
                    endIndexHolder.set(i);
                    // 空对象提前结束查找
                    return instance;
                }
                if (ch == '\'') {
                    while (i + 1 < toIndex && buf[++i] != '\'') ;
                    empty = false;
                    i++;
                } else {
                    // 无引号key处理
                    // 直接锁定冒号（:）位置或者逗号位置
                    while (i + 1 < toIndex && ((ch = buf[++i]) != ':' && ch != ',' && ch != '}')) ;
                    empty = false;
                    if (ch == ',' || ch == '}') {
                        key = new String(buf, fieldKeyFrom, i - fieldKeyFrom).trim();
                        instance.put(key, null);
                        // 提前解析value
                        if (ch == '}') {
                            endIndexHolder.set(i);
                            return instance;
                        } else {
                            continue;
                        }
                    }
                }
            }

            // clear white space characters
            while (i < toIndex && (ch = buf[i]) <= ' ') {
                i++;
            }
            // 清除注释前记录属性字段的token结束位置
            fieldKeyTo = i;

            // Standard JSON rules:
            // 1 if matched, it can only be followed by a colon, and all other symbols are wrong
            // 2 after the attribute value is parsed, it can only be followed by a comma(,) or end, and other symbols are wrong
            if (ch == ':') {
                // Resolve key value pairs
                key = parseFieldKey(fieldKeyFrom, fieldKeyTo, buf);
                // 清除空白字符（clear white space characters）
                while (i + 1 < toIndex && (ch = buf[++i]) <= ' ') ;

                // 分割符位置,SimpleMode
                splitIndex = i - 1;

                // 空，布尔值，数字，或者字符串 （ null, true or false or numeric or string）
                boolean isSimpleValue = false;
                Object value = null;

                if (ch == '{') {
                    value = parseJSONObject(i, toIndex, buf, endIndexHolder);
                    i = endIndexHolder.get();
                    instance.put(key, value);
                } else if (ch == '[') {
                    // 2 [ array
                    // 解析集合或者数组 （Parse a collection or array）
                    value = parseJSONArray(i, toIndex, buf, endIndexHolder);
                    i = endIndexHolder.get();
                    instance.put(key, value);
                } else if (ch == '"' || ch == '\'') {
                    // 3 string
                    // parse string
                    value = parseJSONString(i, toIndex, buf, ch, endIndexHolder);
                    i = endIndexHolder.get();
                    instance.put(key, value);
                } else {
                    isSimpleValue = true;
                    // 4 null, true or false or numeric
                    // Find comma(,) or closing symbol(})
                    while (i + 1 < toIndex) {
                        ch = buf[i + 1];
                        if (ch == ',' || ch == '}') {
                            break;
                        }
                        i++;
                    }
                    // Check whether post comments are appended
                }

                // clear white space characters
                while (i + 1 < toIndex && (ch = buf[++i]) <= ' ') ;
                if (simpleToIndex == -1) {
                    simpleToIndex = i;
                }

                // Check whether the next character is a comma or end symbol '}'. If yes, continue or break. If not, throw an exception
                boolean isClosingSymbol = ch == '}';
                if (ch == ',' || isClosingSymbol) {
                    if (isSimpleValue) {
                        value = parseSimpleValue(splitIndex + 1, simpleToIndex, buf, endIndexHolder);
                        instance.put(key, value);
                    }
                    if (isClosingSymbol) {
                        endIndexHolder.set(i);
                        return instance;
                    }
                } else {
                    throw new YamlParseException("Syntax error, unexpected token character '" + ch + "', position " + i);
                }
            } else {
                throw new YamlParseException("Syntax error, unexpected token character '" + ch + "', position " + i);
            }
        }
        throw new YamlParseException("Syntax error, the closing symbol '}' is not found ");
    }

    private static String parseJSONString(int from, int toIndex, char[] buf, char endCh, AtomicInteger endIndexHolder) {
        int beginIndex = from + 1;
        char ch = '\0', next = '\0';
        int i = beginIndex;
        int len;

        StringBuilder stringBuilder = new StringBuilder();
        for (; i < toIndex; i++) {
            while (i < toIndex && (ch = buf[i]) != '\\' && ch != endCh) {
                i++;
            }
            // ch is \\ or "
            if (ch == '\\') {
                if (i < toIndex - 1) {
                    next = buf[i + 1];
                }
                switch (next) {
                    case '"':
                        len = i - beginIndex;
                        stringBuilder.append(buf, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, '"');
                        beginIndex = ++i + 1;
                        break;
                    case 'n':
                        len = i - beginIndex;
                        stringBuilder.append(buf, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, '\n');
                        beginIndex = ++i + 1;
                        break;
                    case 'r':
                        len = i - beginIndex;
                        stringBuilder.append(buf, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, '\r');
                        beginIndex = ++i + 1;
                        break;
                    case 't':
                        len = i - beginIndex;
                        stringBuilder.append(buf, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, '\t');
                        beginIndex = ++i + 1;
                        break;
                    case 'b':
                        len = i - beginIndex;
                        stringBuilder.append(buf, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, '\b');
                        beginIndex = ++i + 1;
                        break;
                    case 'f':
                        len = i - beginIndex;
                        stringBuilder.append(buf, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, '\f');
                        beginIndex = ++i + 1;
                        break;
                    case 'u':
                        len = i - beginIndex;
                        stringBuilder.append(buf, beginIndex, len + 1);
                        int c = Integer.parseInt(new String(buf, i + 2, 4), 16);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, (char) c);
                        i += 4;
                        beginIndex = ++i + 1;
                        break;
                    default:
                        len = i - beginIndex;
                        stringBuilder.append(buf, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, next);
                        beginIndex = ++i + 1;
                        break;
                }
            } else {
                endIndexHolder.set(i);
                len = i - beginIndex;
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(buf, beginIndex, len);
                    return stringBuilder.toString();
                } else {
                    return len == 0 ? "" : new String(buf, beginIndex, len);
                }
            }
        }
        throw new YamlParseException("Syntax error, the closing symbol '" + endCh + "' is not found ");
    }

    // 字符串，数字， boolean， null(非json标准)
    private static Object parseSimpleValue(int fromIndex, int toIndex, char[] buf, AtomicInteger endIndexHolder) {

        char beginChar = '\0';
        char endChar = '\0';

        while ((fromIndex < toIndex) && ((beginChar = buf[fromIndex]) <= ' ')) {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && ((endChar = buf[toIndex - 1]) <= ' ')) {
            toIndex--;
        }

        int len = toIndex - fromIndex;
        if (len == 4
                && beginChar == 't'
                && buf[fromIndex + 1] == 'r'
                && buf[fromIndex + 2] == 'u'
                && endChar == 'e') {
            return true;
        } else if (len == 5
                && beginChar == 'f'
                && buf[fromIndex + 1] == 'a'
                && buf[fromIndex + 2] == 'l'
                && buf[fromIndex + 3] == 's'
                && endChar == 'e') {
            return false;
        } else {
            if (len == 4
                    && beginChar == 'n'
                    && buf[fromIndex + 1] == 'u'
                    && buf[fromIndex + 2] == 'l'
                    && endChar == 'l') {
                return null;
            }

            // 无引号字符串或者number
            boolean existUnDigit = false;
            boolean existDot = false;
            int digitNum = 0;
            for (int i = 0; i < len; i++) {
                char ch = buf[i + fromIndex];
                if (ch == '.') {
                    existDot = true;
                } else if (i == 0 && ch == '-') {
                } else if (!Character.isDigit(ch)) {
                    existUnDigit = true;
                } else {
                    digitNum++;
                }
            }
            String val = new String(buf, fromIndex, len);
            if (existUnDigit) {
                return val;
            } else {
                if (existDot) {
                    return Double.parseDouble(val);
                } else {
                    long num = Long.parseLong(val);
                    return num >= Integer.MIN_VALUE && num <= Integer.MAX_VALUE ? (int) num : num;
                }
            }
        }
    }

    /**
     * @param from
     * @param to
     * @param buf
     * @return
     */
    private static String parseFieldKey(int from, int to, char[] buf) {
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
            return new String(buf, from + 1, len);
        }
        return new String(buf, from, to - from);
    }


}
