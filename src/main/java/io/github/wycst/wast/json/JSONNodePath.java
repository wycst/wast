package io.github.wycst.wast.json;

import io.github.wycst.wast.common.expression.ExprParser;
import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.json.exceptions.JSONException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JSON xpath
 *
 * <li>"//" :  从当前节点开始查找所有满足条件的节点，并遍历所有子孙节点；</li>
 * <li>"/"  :  仅从当前节点开始查找所有满足条件的节点（不包括子孙节点）；</li>
 *
 * @Date 2024/10/9 19:28
 * @Created by wangyc
 */
public final class JSONNodePath {

    final static JSONNodePath ALL = JSONNodePath.collectors(JSONNodePathCollector.any().recursive(true));
    private int depth;
    JSONNodePathCollector head;
    JSONNodePathCollector tail;
    boolean supportedExtract;
    // 默认贪婪模式（只在path递归查找下生效）
    boolean greedy = true;

    private JSONNodePath() {
    }

    JSONNodePath(JSONNodePathCollector head, JSONNodePathCollector tail, int depth) {
        this.head = head;
        this.tail = tail;
        this.depth = depth;
    }

    // 是否为单节点模式
    JSONNodePath supportedExtract(boolean supportedExtract) {
        this.supportedExtract = supportedExtract;
        return this;
    }

    public JSONNodePath self() {
        return this;
    }

    /**
     * 设置贪婪模式
     *
     * @param greedy
     * @return
     */
    public JSONNodePath greedy(boolean greedy) {
        this.greedy = greedy;
        return this;
    }

    public List<JSONNode> collect(JSONNode parentNode) {
        List<JSONNode> results = new ArrayList<JSONNode>();
        head.collect(parentNode, results, JSONNodeCollector.DEFAULT, newCollectCtx());
        return results;
    }

    public <T> List<T> collect(JSONNode parentNode, JSONNodeCollector<T> collector) {
        List<T> results = new ArrayList<T>();
        head.collect(parentNode, results, collector, newCollectCtx());
        return results;
    }

    public void collect(JSONNode parentNode, Collection<JSONNode> results) {
        head.collect(parentNode, results, JSONNodeCollector.DEFAULT, newCollectCtx());
    }

    public <T> void collect(JSONNode parentNode, Collection<T> results, JSONNodeCollector<T> collector) {
        collector.getClass();
        head.collect(parentNode, results, collector, newCollectCtx());
    }

    JSONNodePathCtx newCollectCtx() {
        return new JSONNodePathCtx(greedy);
    }

    public static JSONNodePath create() {
        return new JSONNodePath();
    }

    /**
     * 解析xpath字符串（通过字符串构建path模型功能有限）
     * <ul>
     * <li> 以斜杠为分界，如果路径中存在斜杠内容请使用单引号字符串，单引号需要紧跟斜杠，例如 /name/'/'/P;
     * <li> 支持双斜杠代表递归查找;
     * <li> 以单引号开头代表字符串明确匹配对象节点的属性;
     * <li> 以[开头代表明确数组下标集合（离散下标），例如/users/[1,3,7,9];
     * <li> 其他开头智能判断(支持n, n+,n-, n~m等);
     * <li> 路径片段结束支持紧跟中括号代表属性过滤操作，例如/persion[name = 'li'];
     * <li> 过滤表达式中的字符串内容以单引号标记；
     * <li> 不支持path内容中存在单引号，请使用编程方式构建；
     * <li> 不支持设置禁用贪婪模式，请使用编程方式构建(禁用后避免很多无效搜索遍历能提升很大性能)；
     * </ul>
     *
     * @param xpath
     * @return
     */
    public static JSONNodePath parse(String xpath) {
        int len;
        if (xpath == null || (len = (xpath = xpath.trim()).length()) == 0) {
            return null;
        }
        int offset = 0;
        char ch;
        JSONNodePath result = JSONNodePath.create();
        char[] chars = UnsafeHelper.getChars(xpath);
        try {
            int dn = 0;
            char beginChar = chars[0];
            if (beginChar != '/') {
                ++dn;
            }
            for (; offset < len; ++offset, dn = 1) {
                while ((ch = chars[offset]) == '/') {
                    ++dn;
                    if (++offset == len) {
                        // finish
                        return result;
                    }
                }
                boolean recursive = dn > 1;
                int begin = offset;
                JSONNodePathCollector pathCollector;
                do {
                    if (ch == '\'') {
                        while (chars[++offset] != '\'') ;
                        String path = new String(chars, begin + 1, offset - begin - 1);
                        pathCollector = JSONNodePathCollector.exact(path).recursive(recursive);
                        result.next(pathCollector);
                        if (++offset == len) {
                            return result;
                        }
                        // ch is / or [
                        ch = chars[offset];
                        break;
                    }
                    if (ch == '[') {
                        try {
                            JSONParseContext parseContext = new JSONNodeContext();
                            parseContext.toIndex = len;
                            Integer[] values = JSONTypeDeserializer.INTEGER_ARRAY.deserialize(chars, offset, parseContext);
                            if (values.length == 1) {
                                pathCollector = JSONNodePathCollector.exact(values[0]);
                            } else {
                                if (values.length == 0) {
                                    String errorMsg = JSONGeneral.createErrorContextText(chars, offset);
                                    throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorMsg + "', empty array");
                                }
                                pathCollector = JSONNodePathCollector.indexs(values);
                            }
                            result.next(pathCollector.recursive(recursive));
                            offset = parseContext.endIndex + 1;
                            if (offset == len) {
                                return result;
                            }
                            ch = chars[offset];
                            if (ch == '[' || ch == '/') {
                                break;
                            }
                        } catch (Throwable throwable) {
                        }
                        String errorMsg = JSONGeneral.createErrorContextText(chars, offset);
                        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorMsg + "', error index array");
                    }

                    // any or
                    begin = offset++;
                    beginChar = ch;
                    char endChar;
                    boolean isNegative = ch == '-';
                    if (isNegative || JSONGeneral.isDigit(ch)) {
                        int val = 0;
                        if (!isNegative) {
                            val = ch - 48;
                        }
                        while (offset < len && JSONGeneral.isDigit(ch = chars[offset])) {
                            ++offset;
                            val = val * 10 + ch - 48;
                        }
                        int digitCnt = offset - begin;
                        // max index: 999999
                        if (digitCnt < 7) {
                            boolean up, down = false, range = false;
                            if ((up = ch == '+') || (down = ch == '-') || (range = ch == '~')) {
                                ++offset;
                            }
                            if (range) {
                                final int from = isNegative ? -val : val;
                                int to = 0;
                                isNegative = chars[offset] == '-'; // Don't worry about whether the array is out of bounds or not
                                if (isNegative) {
                                    ++offset;
                                }
                                while (offset < len && JSONGeneral.isDigit(ch = chars[offset])) {
                                    ++offset;
                                    to = to * 10 + ch - 48;
                                }
                                pathCollector = JSONNodePathCollector.range(from, isNegative ? -to : to);
                                result.next(pathCollector.recursive(recursive));
                                if (offset == len) {
                                    return result;
                                }
                                if (ch == '[' || ch == '/') {
                                    break;
                                }
                                String errorMsg = JSONGeneral.createErrorContextText(chars, offset);
                                throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorMsg + "', error range ");
                            }
                            boolean isEnd = offset == len;
                            if (isEnd || (ch = chars[offset]) == '[' || ch == '/') {
                                if (up) {
                                    pathCollector = JSONNodePathCollector.ge(isNegative ? -val : val);
                                } else if (down) {
                                    pathCollector = JSONNodePathCollector.le(isNegative ? -val : val);
                                } else {
                                    pathCollector = JSONNodePathCollector.exact(isNegative ? -val : val);
                                }
                                result.next(pathCollector.recursive(recursive));
                                if (isEnd) {
                                    return result;
                                }
                                break;
                            }
                        }
                        // -> string collector
                    }
                    while (offset < len && (ch = chars[offset]) != '[' && ch != '/') {
                        ++offset;
                    }
                    int pathLen = offset - begin;
                    endChar = chars[offset - 1];
                    if (beginChar == '*') {
                        if (pathLen == 1) {
                            pathCollector = JSONNodePathCollector.any();
                        } else {
                            if (endChar == '*') {
                                pathCollector = JSONNodePathCollector.contains(new String(chars, begin + 1, pathLen - 2));
                            } else {
                                pathCollector = JSONNodePathCollector.suffix(new String(chars, begin + 1, pathLen - 1));
                            }
                        }
                    } else if (beginChar == '^') {
                        // 正则
                        pathCollector = JSONNodePathCollector.regular(new String(chars, begin + 1, pathLen - 1));
                    } else {
                        if (endChar == '*') {
                            pathCollector = JSONNodePathCollector.prefix(new String(chars, begin, pathLen - 1));
                        } else {
                            pathCollector = JSONNodePathCollector.exact(JSONNodeContext.getString(chars, begin, pathLen));
                        }
                    }
                    result.next(pathCollector.recursive(recursive));
                    if (offset == len) {
                        return result;
                    }

                } while (false);
                if (ch == '[') {
                    // Expression parsing util the end token ']'
                    try {
                        ExprParser parser = Expression.find(xpath, ++offset);
                        offset = parser.findIndex();
                        pathCollector.condition(parser);
                        result.supportedExtract(false);
                        if (offset == len || (ch = xpath.charAt(offset++)) != ']') {
                            String errorMsg = JSONGeneral.createErrorContextText(chars, offset);
                            throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorMsg + "', unexpected '" + ch + "', expected end ']'  error expression, ");
                        }
                        if (offset == len) {
                            return result;
                        }
                        ch = chars[offset];
                    } catch (RuntimeException e) {
                        if (e instanceof JSONException) {
                            throw e;
                        }
                        String errorMsg = JSONGeneral.createErrorContextText(chars, offset);
                        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorMsg + "', error expression", e);
                    }
                }
                if (ch != '/') {
                    throw new JSONException("Syntax error, at pos " + offset + ", unexpected '" + ch + "', expected '/'.");
                }
            }
        } catch (Throwable e) {
            throw e instanceof JSONException ? (JSONException) e : new JSONException("not supported path '" + xpath + "'");
        }
        return result;
    }

    public static JSONNodePath collectors(JSONNodePathCollector... collectors) {
        JSONNodePathCollector prev = null, head = null, tail = null;
        boolean supportedExtract = true;
        for (JSONNodePathCollector pathCollector : collectors) {
            if (!pathCollector.isSupportedExtract()) {
                supportedExtract = false;
            }
            if (prev == null) {
                head = tail = prev = pathCollector.clone();
            } else {
                pathCollector.chainable(prev);
                tail = prev = prev.next;
            }
        }
        return new JSONNodePath(head, tail, collectors.length).supportedExtract(supportedExtract);
    }

    /**
     * 构建精确路径链(单节点)
     *
     * @param paths
     * @return
     */
    public static JSONNodePath paths(Serializable... paths) {
        JSONNodePathCollector prev = null, head = null, tail = null;
        for (Serializable path : paths) {
            JSONNodePathCollector pathFragment = JSONNodePathCollector.exact(path);
            if (prev == null) {
                head = tail = prev = pathFragment;
            } else {
                prev.next = pathFragment;
                tail = prev = pathFragment;
            }
        }
        return new JSONNodePath(head, tail, paths.length).supportedExtract(true);
    }

    public JSONNodePath nextPaths(Serializable... paths) {
        JSONNodePath nextNodePath = paths(paths);
        if (head == null) {
            head = nextNodePath.head;
            tail = nextNodePath.tail;
        } else {
            tail.next = nextNodePath.head;
            tail = nextNodePath.tail;
        }
        depth += paths.length;
        return this;
    }

    public JSONNodePath any() {
        return any(false);
    }

    public JSONNodePath any(boolean recursive) {
        return next(JSONNodePathCollector.any().recursive(recursive));
    }

    public JSONNodePath exact(Serializable path) {
        return exact(path, false);
    }

    /**
     * 精确匹配: //${path} or /${path}
     *
     * @param path
     * @param recursive true //${path}; false /${path}
     * @return
     */
    public JSONNodePath exact(Serializable path, boolean recursive) {
        return next(JSONNodePathCollector.exact(path).recursive(recursive));
    }

    public JSONNodePath prefix(String path) {
        return prefix(path, false);
    }

    /**
     * 匹配前缀: //${path}* or /${path}*
     *
     * @param path
     * @param recursive true -> //${path}*; false -> /${path}*
     * @return
     */
    public JSONNodePath prefix(String path, boolean recursive) {
        return next(JSONNodePathCollector.prefix(path).recursive(recursive));
    }

    public JSONNodePath suffix(String path) {
        return suffix(path, false);
    }

    /**
     * 匹配后缀: //*${path} or /*${path}
     *
     * @param path
     * @param recursive true -> //*${path}; false -> /*${path}
     * @return
     */
    public JSONNodePath suffix(String path, boolean recursive) {
        return next(JSONNodePathCollector.suffix(path).recursive(recursive));
    }

    public JSONNodePath contains(String path) {
        return contains(path, false);
    }

    /**
     * 模糊匹配: //*${path}* or /*${path}*
     *
     * @param path
     * @param recursive true -> //*${path}*; false -> /*${path}*
     * @return
     */
    public JSONNodePath contains(String path, boolean recursive) {
        return next(JSONNodePathCollector.contains(path).recursive(recursive));
    }

    public JSONNodePath regular(String path) {
        return regular(path, false);
    }

    /**
     * 正则表达式匹配路径
     *
     * @param str
     * @param recursive
     * @return
     */
    public JSONNodePath regular(String str, boolean recursive) {
        return next(JSONNodePathCollector.regular(str).recursive(recursive));
    }

    public JSONNodePath ge(int index) {
        return ge(index, false);
    }

    /**
     * 当目标节点为数组元素时支持收集大于或等于某个下标的集合
     *
     * @param index     下标支持负数（倒数）
     * @param recursive
     * @return
     */
    public JSONNodePath ge(int index, boolean recursive) {
        return next(JSONNodePathCollector.ge(index).recursive(recursive));
    }

    public JSONNodePath le(int index) {
        return le(index, false);
    }

    /**
     * 当目标节点为数组元素时支持收集小于或等于某个下标的集合
     *
     * @param index     下标支持负数（倒数）
     * @param recursive
     * @return
     */
    public JSONNodePath le(int index, boolean recursive) {
        return next(JSONNodePathCollector.le(index).recursive(recursive));
    }

    public JSONNodePath range(int from, int to) {
        return range(from, to, false);
    }

    /**
     * 当目标节点为数组元素时支持范围收集(from和to都是包含)
     *
     * @param from      下标支持负数（倒数）
     * @param to        下标支持负数（倒数）
     * @param recursive
     * @return
     */
    public JSONNodePath range(int from, int to, boolean recursive) {
        return next(JSONNodePathCollector.range(from, to).recursive(recursive));
    }

    public JSONNodePath indexs(Integer... indexs) {
        return indexs(false, indexs);
    }

    /**
     * 当目标节点为数组元素时支持收集离散的下标列表
     *
     * @param recursive
     * @param indexs
     * @return
     */
    public JSONNodePath indexs(boolean recursive, Integer... indexs) {
        return next(JSONNodePathCollector.indexs(indexs).recursive(recursive));
    }

    public JSONNodePath next(JSONNodePathCollector next) {
        final boolean supportedExtract = next.isSupportedExtract();
        if (head == null) {
            head = tail = next.self();
            this.supportedExtract = supportedExtract;
        } else {
            tail = tail.next = next.self();
            if (!supportedExtract) {
                this.supportedExtract = false;
            }
        }
        ++depth;
        return this;
    }

    public JSONNodePath resetPaths() {
        head = tail = null;
        supportedExtract = false;
        depth = 0;
        return this;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        JSONNodePathCollector value = head;
        while (value != null) {
            builder.append(value);
            value = value.next;
        }
        return builder.toString();
    }
}
