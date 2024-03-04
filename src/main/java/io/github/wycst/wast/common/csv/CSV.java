package io.github.wycst.wast.common.csv;

import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.ObjectUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * CSV处理
 *
 * @author wangyunchao
 */
public final class CSV {

    /**
     * 读取文件返回CSV表格对象
     *
     * @param file
     * @return
     */
    public static CSVTable read(File file) {
        try {
            return read(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 指定编码
     *
     * @param file
     * @param charsetName
     * @return
     */
    public static CSVTable read(File file, String charsetName) {
        try {
            return read(new FileInputStream(file), charsetName);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 读取流返回CSV表格对象
     *
     * @param is
     * @return
     */
    public static CSVTable read(InputStream is) {
        return read(is, Charset.defaultCharset());
    }

    /**
     * 读取流返回CSV表格对象
     *
     * @param is
     * @param charsetName
     * @return
     */
    public static CSVTable read(InputStream is, String charsetName) {
        return read(is, Charset.forName(charsetName));
    }

    /**
     * 读取流返回CSV表格对象
     *
     * @param is
     * @param charset
     * @return
     */
    public static CSVTable read(InputStream is, Charset charset) {
        CSVTable csvTable = new CSVTable();
        List<CSVRow> csvRows = new ArrayList<CSVRow>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, charset));
        try {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if((line = line.trim()).length() > 0) {
                    // 读取行
                    csvRows.add(readRow(csvTable, line));
                }
            }
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        csvTable.setRows(csvRows);
        return csvTable;
    }

    /**
     * 读取line并返回行记录
     *
     * @param csvTable
     * @param line
     * @return
     */
    private static CSVRow readRow(CSVTable csvTable, String line) {
        List<String> rowValues = new ArrayList<String>();
        char[] chars = UnsafeHelper.getChars(line);
        int offset, len = chars.length;
        StringBuilder stringBuilder = null;
        char ch;
        for (int i = 0; i < len; ++i) {
            while ((ch = chars[i]) == ' ') {
                ++i;
            }
            if (ch == '"') {
                offset = i + 1;
                boolean escape = false;
                if (stringBuilder == null) {
                    stringBuilder = new StringBuilder();
                }
                stringBuilder.setLength(0);
                do {
                    // 双引号模式
                    while (i < len && (ch = chars[++i]) != '"') ;
                    if (ch != '"') {
                        throw new UnsupportedOperationException("ERROR CSV missing closing '\"' from offset " + offset);
                    }
                    int j = i;
                    // trim
                    while (++i < len && (ch = chars[i]) == ' ') ;
                    boolean isEnd = i == len;
                    if (ch == ',' || isEnd) {
                        rowValues.add(escape ? stringBuilder.append(chars, offset, j - offset).toString() : new String(chars, offset, j - offset));
                        break;
                    } else {
                        if (ch != '"') {
                            throw new UnsupportedOperationException("ERROR CSV offset " + i + " expected '\"' ");
                        }
                        stringBuilder.append(chars, offset, i - offset);
                        offset = i + 1;
                        // 转义
                        escape = true;
                    }
                } while (true);
            } else {
                offset = i;
                while (++i < len && (ch = chars[i]) != ',') ;
                int j = i;
                while (chars[j - 1] == ' ') {
                    --j;
                }
                rowValues.add(new String(chars, offset, j - offset));
                if (ch != ',') {
                    // i == len
                    break;
                }
            }
        }
        return new CSVRow(csvTable, rowValues);
    }

    public static CSVTable read(byte[] data) {
        return read(new String(data));
    }

    public static CSVTable read(byte[] data, String charsetName) {
        return read(new String(data, Charset.forName(charsetName)));
    }

    /**
     * 读取完整的CSV内容
     *
     * @param content
     * @return
     */
    public static CSVTable read(String content) {
        CSVTable csvTable = new CSVTable();
        List<CSVRow> csvRows = new ArrayList<CSVRow>();
        char[] chars = UnsafeHelper.getChars(content);
        int i = 0, offset, endIndex = chars.length - 1;
        if (chars[endIndex] != '\n') {
            chars = Arrays.copyOf(chars, endIndex + 1);
            chars[++endIndex] = '\n';
        }
        List<String> rowValues = new ArrayList<String>();
        StringBuilder stringBuilder = null;
        char ch;
        for (; i <= endIndex; ++i) {
            while ((ch = chars[i]) == ' ') {
                ++i;
            }
            if (ch == '"') {
                offset = i + 1;
                boolean escape = false;
                if (stringBuilder == null) {
                    stringBuilder = new StringBuilder();
                }
                stringBuilder.setLength(0);
                do {
                    // 双引号模式
                    while (i < endIndex && (ch = chars[++i]) != '"') ;
                    if (ch != '"') {
                        throw new UnsupportedOperationException("ERROR CSV missing closing '\"' from offset " + offset);
                    }
                    int j = i;
                    // trim
                    while ((ch = chars[++i]) == ' ') ;
                    if (ch == '\r') {
                        ch = chars[++i];
                        if (ch != '\n') {
                            throw new UnsupportedOperationException("ERROR CSV expected '\\n' after '\\r'" + new String(chars, offset, i - offset));
                        }
                    }
                    boolean isLineBreak = ch == '\n';
                    if (ch == ',' || isLineBreak) {
                        rowValues.add(escape ? stringBuilder.append(chars, offset, j - offset).toString() : new String(chars, offset, j - offset));
                        if (isLineBreak) {
                            csvRows.add(new CSVRow(csvTable, rowValues));
                        }
                        break;
                    } else {
                        if (ch != '"') {
                            throw new UnsupportedOperationException("ERROR CSV offset " + i + " expected '\"' ");
                        }
                        stringBuilder.append(chars, offset, i - offset);
                        offset = i + 1;
                        // 转义
                        escape = true;
                    }
                } while (true);
            } else {
                offset = i;
                while ((ch = chars[++i]) != ',' && ch != '\r' && ch != '\n') ;
                int j = i;
                while (chars[j - 1] == ' ') {
                    --j;
                }
                rowValues.add(new String(chars, offset, j - offset));
                if (ch != ',') {
                    if (ch == '\r') {
                        ch = chars[++i];
                    }
                    if (ch != '\n') {
                        throw new UnsupportedOperationException("ERROR CSV offset " + i + " expected '\\n' " + new String(chars, offset, i - offset));
                    }
                    csvRows.add(new CSVRow(csvTable, rowValues));
                    // new
                    rowValues = new ArrayList<String>();
                }
            }
        }
        csvTable.setRows(csvRows);
        return csvTable;
    }

    /**
     * 将列表对象转为CSV文件
     *
     * @param objList
     * @param file
     */
    public static void writeObjectTo(List<?> objList, File file) {
        try {
            writeObjectTo(objList, new FileOutputStream(file), Charset.defaultCharset());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将列表对象转为CSV文件
     *
     * @param objList
     * @param os
     * @param charsetName
     */
    public static void writeObjectTo(List<?> objList, OutputStream os, String charsetName) {
        writeObjectTo(objList, os, Charset.forName(charsetName));
    }

    /**
     * 将集合对象转为CSV字符串
     *
     * @param objList
     * @return
     */
    public static String toCSVString(List<?> objList) {
        if(objList == null || objList.size() == 0) return null;
        StringBuilder builder = new StringBuilder();
        try {
            writeObjectTo(builder, objList);
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将对象转为CSV文件
     *
     * @param objList
     * @param os
     * @param charset
     */
    static void writeObjectTo(List<?> objList, OutputStream os, Charset charset) {
        if(objList == null || objList.size() == 0) return;
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(os, charset));
        try {
            writeObjectTo(bufferedWriter, objList);
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void writeObjectTo(Appendable appendable, List<?> objList) throws IOException {
        // 获取第一个元素的属性作为CSV的字段信息
        Object first = objList.get(0);
        List<String> csvColumnNames = new ArrayList<String>();
        List<String> objectKeys = new ArrayList<String>();
        getColumnNames(first, csvColumnNames, objectKeys);
        writeRow(appendable, csvColumnNames);
        for (Object obj : objList) {
            int i = 0;
            for (String key : objectKeys) {
                String value = String.valueOf(ObjectUtils.get(obj, key));
                if (i++ > 0) {
                    appendable.append(',');
                }
                writeValue(appendable, value);
            }
            appendable.append('\n');
        }
    }

    private static void getColumnNames(Object obj, List<String> csvColumnNames, List<String> objectKeys) {
        Class aClass = obj.getClass();
        ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(aClass);
        if (classCategory == ReflectConsts.ClassCategory.MapCategory) {
            // map
            Map map = (Map) obj;
            Set<Object> keySet = map.keySet();
            for (Object key : keySet) {
                csvColumnNames.add(String.valueOf(key));
            }
            objectKeys.addAll(csvColumnNames);
        } else if(classCategory == ReflectConsts.ClassCategory.ObjectCategory) {
            ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(aClass);
            List<GetterInfo> getterInfos = classStructureWrapper.getGetterInfos(classStructureWrapper.isForceUseFields());
            for (GetterInfo getterInfo : getterInfos) {
                CSVColumn csvColumn = (CSVColumn) getterInfo.getAnnotation(CSVColumn.class);
                String name;
                if(csvColumn != null && (name = csvColumn.value().trim()).length() > 0) {
                    csvColumnNames.add(name);
                } else {
                    csvColumnNames.add(getterInfo.getName());
                }
                objectKeys.add(getterInfo.getName());
            }
        }
    }

    /**
     * 写入一行
     *
     * @param appendable
     * @param values
     * @throws IOException
     */
    static void writeRow(Appendable appendable, List<String> values) throws IOException {
        int i = 0;
        for (String value : values) {
            if (i++ > 0) {
                appendable.append(',');
            }
            writeValue(appendable, value);
        }
        appendable.append('\n');
    }

    /**
     * 写入一个字段
     *
     * @param appendable
     * @param value
     * @throws IOException
     */
    static void writeValue(Appendable appendable, String value) throws IOException {
        char[] buf = UnsafeHelper.getChars(value);
        char ch = 0;
        int j = 0, len = buf.length, begin = 0;
        boolean escape = false;
        for (; ; ) {
            // 遇到逗号或者双引号需要转义
            while (j < len && (ch = buf[j]) != ',' && ch != '"') {
                ++j;
            }
            if (j == len) {
                appendable.append(value, begin, j);
                if (escape) {
                    appendable.append('"');
                }
                break;
            } else {
                ++j;
                escape = true;
                if (begin == 0) {
                    appendable.append('"');
                    appendable.append(value, 0, j);
                } else {
                    appendable.append(value, begin, j - begin);
                }
                if (ch == '"') {
                    appendable.append('"');
                }
                begin = j;
            }
        }
    }
}
