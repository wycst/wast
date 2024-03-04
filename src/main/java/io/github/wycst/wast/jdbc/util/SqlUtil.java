package io.github.wycst.wast.jdbc.util;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.jdbc.exception.ParameterException;
import io.github.wycst.wast.jdbc.query.sql.Sql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SqlUtil {

    // cache total
    public static Map<String, String> totalSqlMapping = new ConcurrentHashMap<String, String>();

    /**
     * 根据查询sql转分页总数sql
     *
     * @param sql
     * @return
     */
    public static String getTotalSql(String sql) {

        String totalSql = totalSqlMapping.get(sql);
        if (!totalSqlMapping.containsKey(sql)) {
            String baseSelectRegex = "([ ]*[Ss][Ee][Ll][Ee][Cc][Tt][ ]+).*([ ]+[Ff][Rr][Oo][Mm][ ]+.+)$";
            String uniconSelectRegex = "[( ]*" + baseSelectRegex + "[) ]+[Uu][Nn][Ii][Oo][Nn][( ]+" + baseSelectRegex + "[) ]*";
            // baseSelectRegex 和 uniconSelectRegex 可能同时能匹配，先判断是否有uniconSelectRegex
            if (sql.matches(uniconSelectRegex)) {
                totalSql = "SELECT count(*) FROM (" + sql + ") t";
            } else if (sql.matches(baseSelectRegex)) {
                totalSql = sql.replaceAll(baseSelectRegex, "$1count(*)$2");
            } else {
                // 如果是unicon或者其他情况
                totalSql = "SELECT count(*) FROM (" + sql + ") t";
            }
            totalSqlMapping.put(sql, totalSql);
        }
        return totalSql;
    }

    // 如果线程不安全需要不能作为全局静态变量
    // private static Pattern pattern = Pattern.compile("[#$][{](\\w+([.]\\w+)*)[}]");

    /**
     * 获取sql对象
     *
     * @param tmpSql
     * @param target 如果为空，参数集合都为空
     * @return
     */
    public static Sql getSqlObject(String tmpSql, Object target) {

        try {
            Sql sql = new Sql();
            StringBuilder builder = new StringBuilder();
            List<Object> values = new ArrayList<Object>();
            List<String> paramNames = new ArrayList<String>();

            boolean replaced = false;
            int paramIndex = 0;

            char[] chars = UnsafeHelper.getChars(tmpSql);
            int length = chars.length, beginIndex = 0;
            boolean stringMode = false;
            char ch, prev = 0;
            for (int i = 0; i < length; ++i) {
                ch = chars[i];
                if (ch == '\'') {
                    stringMode = !stringMode;
                }
                if (ch == '{') {
                    // find next }
                    boolean is$ = prev == '$';
                    if (is$ || (prev == '#' && !stringMode)) {
                        // exclude current '{' and the prev char
                        int e = i - 1;
                        builder.append(chars, beginIndex, e - beginIndex);
                        beginIndex = e;
                        int j = ++i;
                        while (j < length && ((ch = chars[j]) != '}')) {
                            ++j;
                        }
                        if (ch == '}') {
                            String key = new String(chars, i, j - i).trim();
                            Object value = SqlUtil.getParamValue(target, key, paramIndex++);
                            if (is$) {
                                if (value != null) {
                                    if (value instanceof String) {
                                        String strValue = (String) value;
                                        if (strValue.indexOf('"') > -1) {
                                            strValue = strValue.replace("\"", "\\\"");
                                        }
                                        if (strValue.indexOf('\'') > -1) {
                                            strValue = strValue.replace("'", "\\'");
                                        }
                                        value = strValue;
                                    } else if (value instanceof Date) {
                                        value = new GregorianDate(((Date) value).getTime()).format();
                                    }
                                }
                                builder.append(value);
                                replaced = true;
                            } else {
                                builder.append('?');
                                paramNames.add(key);
                                values.add(value);
                            }
                            beginIndex = j + 1;
                        }
                        i = j;
                    }
                }
                prev = ch;
            }

            if (beginIndex < length) {
                builder.append(chars, beginIndex, length - beginIndex);
            }

            sql.setOriginalSql(tmpSql);
            sql.setFormalSql(builder.toString());
            sql.setParamValues(values.toArray());
            sql.setParamNames(paramNames);
            sql.setReplaced(replaced);

            return sql;

        } catch (Exception e) {
            throw new ParameterException(" sql params parse error: " + tmpSql, e);
        }

    }

    static Object getParamValue(Object target, String key, int paramIndex) {
        if (target == null)
            return null;
        if (target instanceof byte[] || target instanceof Number || target instanceof Boolean || target instanceof CharSequence) {
            // 判断是否为二进制，基本类型或字符串
            if (paramIndex > 0) {
                throw new ParameterException("No value specified for parameter " + (paramIndex + 1));
            }
            return target;
        } else if (target.getClass().isArray()) {
            Object[] arr = (Object[]) target;
            if (paramIndex > arr.length - 1) {
                throw new ParameterException("No value specified for parameter " + (paramIndex + 1));
            }
            return arr[paramIndex];
        } else {
            return ObjectUtils.get(target, key);
        }
    }

    public static List<String> readSqlScripts(InputStream is) throws IOException {
        List<String> scriptList = new ArrayList<String>();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line).append("\r\n");
                String trimLine = line.trim();
                if(!trimLine.startsWith("--") && trimLine.endsWith(";")) {
                    scriptList.add(builder.toString());
                    builder.setLength(0);
                }
            }
            if(builder.length() > 0) {
                scriptList.add(builder.toString());
            }
            return scriptList;
        } finally {
            if(is != null) {
                is.close();
            }
            if(bufferedReader != null) {
                bufferedReader.close();
            }
        }
    }

    public static void clear() {
        totalSqlMapping.clear();
    }
}
