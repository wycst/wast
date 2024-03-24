package io.github.wycst.wast.common.utils;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串相关操作封装
 * 注：非极限性能优化
 *
 * @Author: wangy
 * @Date: 2019/12/7 11:12
 * @Description:
 */
public final class StringUtils {

    /**
     * 带有下划线的数据库字段转成驼峰命名
     *
     * @param columnName
     * @return
     */
    public static String getCamelCase(String columnName) {
        return getCamelCase(columnName, false);
    }

    /**
     * 带有下划线的数据库字段转成驼峰命名
     *
     * @param columnName
     * @param upperCaseFirstChar 首字母是否大写
     * @return
     */
    public static String getCamelCase(String columnName, boolean upperCaseFirstChar) {

        if (columnName == null)
            return null;
        StringBuilder builder = new StringBuilder();
        char[] chars = UnsafeHelper.getChars(columnName);

        boolean upperCaseFlag = false;
        for (int i = 0, len = chars.length; i < len; ++i) {
            char ch = chars[i];
            if (ch == '_') {
                upperCaseFlag = true;
                continue;
            }
            char appendChar;
            boolean isLowerCase = ch >= 'a' && ch <= 'z';
            if (upperCaseFlag) {
                if (isLowerCase) {
                    appendChar = (char) (ch - 32);
                } else {
                    appendChar = ch;
                }
                upperCaseFlag = false;
            } else if (ch >= 'A' && ch <= 'Z') {
                appendChar = (char) (ch + 32);
            } else {
                appendChar = ch;
            }
            builder.append(appendChar);
        }

        if (upperCaseFirstChar) {
            char firstAppendChar = builder.charAt(0);
            if (firstAppendChar >= 'a' && firstAppendChar <= 'z') {
                builder.setCharAt(0, (char) (firstAppendChar - 32));
            }
        }

        return builder.toString();
    }

    /**
     * 驼峰转下划线
     *
     * @param camelCase
     * @return
     */
    public static String camelCaseToSymbol(String camelCase) {
        return camelCaseToSymbol(camelCase, "_");
    }

    public static String camelCaseToSymbol(String camelCase, String symbol) {
        return camelCaseToSymbol(camelCase, symbol, false);
    }

    /**
     * 驼峰转下划线
     *
     * @param camelCase
     * @param symbol
     * @param firstSymbol 第一个字符如果是大小是否添加symbol
     * @return
     */
    public static String camelCaseToSymbol(String camelCase, String symbol, boolean firstSymbol) {
        if (camelCase == null) {
            return null;
        }
        // return camelCase.replaceAll("([A-Z])", symbol + "$1").toLowerCase();
        char[] chars = UnsafeHelper.getChars(camelCase);
        StringBuilder builder = new StringBuilder();
        for (int i = 0, len = chars.length; i < len; ++i) {
            char ch = chars[i];
            if (ch >= 'A' && ch <= 'Z') {
                if (firstSymbol || i != 0) {
                    builder.append(symbol);
                }
                builder.append((char) (ch + 32));
            } else {
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    public static boolean isNotEmpty(String str) {
        return str != null && str.trim().length() > 0;
    }

    /**
     * 组正则表达式替换占位符x{aa}->eg: ${name}
     * <p> 表达式支持a.b.c
     *
     * @param source
     * @param groupRegex
     * @param context
     * @return
     */
    public static String regexGroupExprReplace(String source, String groupRegex, String prefix, Object context) {
        List<String> groups = RegexUtils.getMatcherGroups(source, groupRegex, false);
        Set<String> hashGroups = new HashSet<String>(groups);
        String result = source;
        if (hashGroups != null && hashGroups.size() > 0) {
            for (String group : hashGroups) {
                Object value = ObjectUtils.get(context, group.trim());
                if (value != null) {
                    result = result.replace(prefix + "{" + group + "}", value.toString());
                }
            }
        }
        return result;
    }

    /***
     * <p> 固定占位符使用参数替换</p>
     * <p> 例如 message: "{}, hello", placeholder: "{}", parameters: ["xx"] 替换后： xx, hello</p>
     * <p> 参考log类的使用： log.info("xxx {}, sdsds {}", p1, p2);</p>
     * <p> 不支持正则表达式</p>
     *
     * @param message
     * @param placeholder
     * @param parameters
     * @return
     */
    public static String replacePlaceholder(String message, String placeholder, Object... parameters) {

        int parameterCount;
        if (StringUtils.isEmpty(placeholder) || parameters == null || (parameterCount = parameters.length) == 0) {
            return message;
        }
        int placeholderIndex = -1;
        if ((placeholderIndex = message.indexOf(placeholder)) == -1) {
            return message;
        }
        StringBuilder buffer = new StringBuilder();
        int fromIndex = 0;
        int placeholderLen = placeholder.length();
        int i = 0;
        while (placeholderIndex > -1) {
            buffer.append(message, fromIndex, placeholderIndex);
            if (i < parameterCount) {
                buffer.append(parameters[i++]);
            } else {
                buffer.append(placeholder);
            }
            fromIndex = placeholderIndex + placeholderLen;
            placeholderIndex = message.indexOf(placeholder, fromIndex);
        }
        if (fromIndex < message.length()) {
            buffer.append(message, fromIndex, message.length());
        }

        return buffer.toString();
    }

    /***
     * 替换${var}占位符
     *
     * @param message
     * @param context
     * @return
     */
    public static String replaceGroupRegex(String message, Object context) {
        return replaceGroupRegex(message, "[$][{](.*?)[}]", context);
    }

    /***
     * 替换${var}占位符
     * <p>模板引擎专用
     *
     * @param message
     * @param context
     * @param emptyIfNull 如果key值为空使用空字符串替换而不是null
     * @return
     */
    public static String replaceGroupRegex(String message, Object context, boolean emptyIfNull) {
        return replaceGroupRegex(message, "[$][{]([ ]*[0-9a-zA-Z_.$]+[ ]*)[}]", context, emptyIfNull);
    }

    /***
     * <p> 正则占位符使用动态参数替换
     * <p> groupRegex 注意特殊字符需要转义或者使用[],例如 ${(.*?)} -> [$][{](.*?)[}]或者\\$\\{(.*?)\\}
     *     ()必须存在代表group
     * <p> message: "${name}, hello", groupRegex: ${(.*?)}, context: name -> xx,  替换后： xx, hello
     *
     * @param message
     * @param groupRegex
     * @param context
     * @return
     */
    public static String replaceGroupRegex(String message, String groupRegex, Object context) {
        return replaceGroupRegex(message, groupRegex, context, false);
    }

    /***
     * <p> 正则占位符使用动态参数替换
     * <p> groupRegex 注意特殊字符需要转义或者使用[],例如 ${(.*?)} -> [$][{](.*?)[}]或者\\$\\{(.*?)\\}
     *     ()必须存在代表group
     * <p> message: "${name}, hello", groupRegex: ${(.*?)}, context: name -> xx,  替换后： xx, hello
     *
     * @param message
     * @param groupRegex
     * @param context
     * @param emptyIfNull 如果key值为空使用空字符串替换而不是null
     * @return
     */
    public static String replaceGroupRegex(String message, String groupRegex, Object context, boolean emptyIfNull) {

        if (StringUtils.isEmpty(groupRegex) || context == null) {
            return message;
        }
        // 必须包含()
        if (groupRegex.indexOf(")") <= groupRegex.indexOf("(") || groupRegex.indexOf("(") == -1) {
            return message;
        }
        StringBuilder buffer = new StringBuilder();
        Pattern pattern = RegexUtils.getPattern(groupRegex); // Pattern.compile(groupRegex);
        Matcher matcher = pattern.matcher(message);
        int beginIndex = 0;
        while (matcher.find()) {
            // 判断group开始是否为转义标识符\\,如果是就原group跳过
            int newBeginIndex = matcher.start(0);
            if (message.charAt(newBeginIndex - 1) == '\\') {
                buffer.append(message, beginIndex, newBeginIndex - 1);
                buffer.append(matcher.group(0));
            } else {
                String key = matcher.group(1).trim();
                buffer.append(message, beginIndex, newBeginIndex);
                Object value = ObjectUtils.get(context, key);
                if (value == null && emptyIfNull) {
                } else {
                    buffer.append(value);
                }
            }
            beginIndex = matcher.end(0);
        }
        buffer.append(message, beginIndex, message.length());
        return buffer.toString();
    }

    public static String getThrowableContent(Throwable t) {
        if (t == null)
            return null;
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            t.printStackTrace(pw);
            return sw.toString();
        } finally {
            pw.close();
        }
    }

    public static String escapeHtml(String value) {
        if (value == null) {
            return value;
        }
        value = value.replace("&", "&amp;");
        value = value.replaceAll("[<](.*?)[>]", "&lt;$1&gt;").replaceAll("[(](.*?)[)]", "&#40;$1&#41;");
        value = value.replaceAll("=([ ]*)\"", "=$1&quot;");
        value = value.replace("'", "&#39;").replace(" ", "&nbsp;");
        return value;
    }

    public static String htmlUnescape(String value) {
        if (value == null) {
            return value;
        }
        value = value.replace("&#39;", "'").replace("&nbsp;", " ");
        ;
        value = value.replaceAll("=([ ]*)&quot;", "=$1\"");
        value = value.replaceAll("&lt;(.*?)&gt;", "<$1>").replaceAll("&#40;(.*?)&#41;", "($1)");
        value = value.replace("&amp;", "&");
        return value;
    }

    public static boolean contains(String[] arr, String element) {
        if (arr == null || element == null)
            return false;
        for (String str : arr) {
            if (element.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 格式化路径
     *
     * @param paths
     * @return
     */
    public static String formatMappingPath(String... paths) {
        StringBuilder pathBuffer = new StringBuilder("/");
        for (String path : paths) {
            if (path == null || path.length() == 0)
                continue;
            pathBuffer.append(path);
            if (!path.endsWith("/")) {
                pathBuffer.append("/");
            }
        }
        String path = pathBuffer.toString().trim();
        if (path.indexOf("\\") > -1) {
            path = path.replace("\\", "/");
        }
        path = path.replaceAll("(/)+", "$1");
        return path;
    }

    /***
     * 读取资源字符串
     *
     * @param resource
     * @return
     */
    public static String fromResource(String resource) {
        if (resource == null)
            return null;
        if (!resource.startsWith("/")) {
            resource = "/" + resource;
        }
        InputStream is = StringUtils.class.getResourceAsStream(resource);
        return fromStream(is);
    }

    /***
     * 读取流字符串
     *
     * @param is
     * @return
     */
    public static String fromStream(InputStream is) {
        return fromStream(is, Charset.defaultCharset());
    }

    /***
     * 读取流字符串
     *
     * @param is
     * @param charsetName 指定编码
     * @return
     */
    public static String fromStream(InputStream is, String charsetName) {
        return fromStream(is, Charset.forName(charsetName));
    }

    /***
     * 读取流字符串
     *
     * @param is
     * @param charset 指定编码
     * @return
     */
    public static String fromStream(InputStream is, Charset charset) {
        try {
            byte[] bytes = IOUtils.readBytes(is);
            return new String(bytes, charset);
        } catch (IOException e) {
            return null;
        }
    }
}
