package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.json.options.JsonConfig;

import java.io.IOException;
import java.io.Writer;
import java.sql.Time;
import java.util.Date;

/**
 * @Author: wangy
 * @Date: 2022/6/18 5:59
 * @Description:
 */
abstract class JSONTypeSerializer extends JSONGeneral {

    final static int LENGTH = 7;
    final static JSONTypeSerializer[] TYPE_SERIALIZERS = new JSONTypeSerializer[LENGTH];

    static {
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.CharSequence.ordinal()] = new CharSequenceSerializer();
        JSONTypeSerializer SIMPLE = new SimpleSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.Number.ordinal()] = SIMPLE;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.Bool.ordinal()] = SIMPLE;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.Date.ordinal()] = new DateSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.Class.ordinal()] = new ClassSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.Enum.ordinal()] = new EnumSerializer();
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.Annotation.ordinal()] = new AnnotationSerializer();
    }

    /***
     * 序列化接口
     *
     * @param value
     * @param writer
     * @param jsonConfig
     * @param indent
     */
    abstract void serialize(Object value, Writer writer, GetterInfo getterInfo, JsonConfig jsonConfig, int indent) throws Exception;

    // 0、字符串序列化
    private static class CharSequenceSerializer extends JSONTypeSerializer {

        void serialize(Object value, Writer content, GetterInfo getterInfo, JsonConfig jsonConfig, int indent) throws Exception {
            String strValue;
            boolean isCharSequence = value instanceof CharSequence;
            char[] chars;
            int len;
            if (isCharSequence) {
                strValue = value.toString();
                len = strValue.length();
                chars = getChars(strValue);
            } else {
                if (value instanceof char[]) {
                    chars = (char[]) value;
                    len = chars.length;
                } else {
                    char c = (Character) value;
                    chars = new char[]{c};
                    len = 1;
                }
            }

            content.append('"');
            int beginIndex = 0;

            if (!jsonConfig.isDisableEscapeValidate()) {
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
        }
    }

    // 1、number和bool序列化
    private static class SimpleSerializer extends JSONTypeSerializer {

        void serialize(Object value, Writer content, GetterInfo getterInfo, JsonConfig jsonConfig, int indent) throws Exception {
            content.append(value.toString());
        }
    }

    // 3、日期
    private static class DateSerializer extends JSONTypeSerializer {

        void serialize(Object value, Writer content, GetterInfo getterInfo, JsonConfig jsonConfig, int indent) throws Exception {
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
        }

        /***
         * 日期序列化
         *
         * @param date
         * @param pattern
         * @param timezone
         * @param content
         * @throws IOException
         */
        private static void writeDate(Date date, String pattern, String timezone, Writer content) throws IOException {

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

            if (pattern == null) {
                if(date instanceof Time) {
                    writeDefaultFormatTime(hourOfDay, minute, second, content);
                } else {
                    writeDefaultFormatDate(year, month, day, hourOfDay, minute, second, content);
                }
                return;
            }

            millisecond = commonDate.getMillisecond();
            boolean isAm = commonDate.isAm();

            pattern = pattern.trim();
            int len = pattern.length();
            content.append('"');
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

                    // switch & case
                    switch (prevChar) {
                        case 'y': {
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
                            break;
                        }
                        case 'M': {
                            // 月份
                            if (count == 1 || month >= 10) {
                                // 输出实际month
                                content.write(String.valueOf(month));
                            } else {
                                // 输出完整的month
                                content.write(FORMAT_DIGITS[month]);
                            }
                            break;
                        }
                        case 'd': {
                            if (count == 1 || day >= 10) {
                                // 输出实际day
                                content.write(String.valueOf(day));
                            } else {
                                // 输出完整的day
                                content.write(FORMAT_DIGITS[day]);
                            }
                            break;
                        }
                        case 'a': {
                            // 上午/下午
                            if (isAm) {
                                content.append("am");
                            } else {
                                content.append("pm");
                            }
                            break;
                        }
                        case 'H': {
                            // 0-23
                            if (count == 1 || hourOfDay >= 10) {
                                // 输出实际hourOfDay
                                content.write(String.valueOf(hourOfDay));
                            } else {
                                // 输出完整的hourOfDay
                                content.write(FORMAT_DIGITS[hourOfDay]);
                            }
                            break;
                        }
                        case 'h': {
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
                            break;
                        }
                        case 'm': {
                            // 分钟 0-59
                            if (count == 1 || minute >= 10) {
                                // 输出实际分钟
                                content.write(String.valueOf(minute));
                            } else {
                                // 输出2位分钟数
                                content.write(FORMAT_DIGITS[minute]);
                            }
                            break;
                        }
                        case 's': {
                            // 秒 0-59
                            if (count == 1 || second >= 10) {
                                // 输出实际秒
                                content.write(String.valueOf(second));
                            } else {
                                // 输出2位秒
                                content.write(FORMAT_DIGITS[second]);
                            }
                            break;
                        }
                        case 'S': {
                            // 毫秒
                            content.write(String.valueOf(millisecond));
                            break;
                        }
                        default: {
                            // 其他输出
                            if (prevChar != '\0') {
                                // 输出count个 prevChar
                                int n = count;
                                while (n-- > 0)
                                    content.append(prevChar);
                            }
                        }
                    }
                    count = 1;
                }
                prevChar = ch;
                // 计数+1
            }
            content.append('"');
        }

        // "yyyy-MM-dd HH:mm:ss"
        private static void writeDefaultFormatDate(int year, int month, int day, int hourOfDay, int minute, int second, Writer content) throws IOException {
            content.append('"');
            if(year < 0) {
                content.append('-');
                year *= -1;
            }
            if (year < 10) {
                content.append("000");
            } else if (year < 100) {
                content.append("00");
            } else if (year < 1000) {
                content.append('0');
            }
            content.write(String.valueOf(year));
            content.write('-');

            if(month >= 10) {
                content.write(String.valueOf(month));
            } else {
                // 输出完整的month
                content.write(FORMAT_DIGITS[month]);
            }
            content.write('-');
            if(day >= 10) {
                content.write(String.valueOf(day));
            } else {
                // 输出完整的day
                content.write(FORMAT_DIGITS[day]);
            }
            content.write(' ');

            if(hourOfDay >= 10) {
                content.write(String.valueOf(hourOfDay));
            } else {
                content.write(FORMAT_DIGITS[hourOfDay]);
            }
            content.write(':');

            if(minute >= 10) {
                content.write(String.valueOf(minute));
            } else {
                content.write(FORMAT_DIGITS[minute]);
            }
            content.write(':');

            if(second >= 10) {
                content.write(String.valueOf(second));
            } else {
                content.write(FORMAT_DIGITS[second]);
            }
            content.append('"');
        }

        // "HH:mm:ss"
        private static void writeDefaultFormatTime(int hourOfDay, int minute, int second, Writer content) throws IOException {
            content.append('"');
            if(hourOfDay >= 10) {
                content.write(String.valueOf(hourOfDay));
            } else {
                content.write(FORMAT_DIGITS[hourOfDay]);
            }
            content.write(':');
            if(minute >= 10) {
                content.write(String.valueOf(minute));
            } else {
                content.write(FORMAT_DIGITS[minute]);
            }
            content.write(':');
            if(second >= 10) {
                content.write(String.valueOf(second));
            } else {
                content.write(FORMAT_DIGITS[second]);
            }
            content.append('"');
        }

    }

    private static class EnumSerializer extends JSONTypeSerializer {

        @Override
        void serialize(Object value, Writer content, GetterInfo getterInfo, JsonConfig jsonConfig, int indent) throws Exception {
            Enum enmuValue = (Enum) value;
            // ordinal
            if (jsonConfig.isWriteEnumAsOrdinal()) {
                content.append(String.valueOf(enmuValue.ordinal()));
            } else {
                // name
                content.append('"').append(enmuValue.name()).append('"');
            }
        }
    }

    private static class ClassSerializer extends JSONTypeSerializer {
        @Override
        void serialize(Object value, Writer content, GetterInfo getterInfo, JsonConfig jsonConfig, int indent) throws Exception {
            content.append('"').append(((Class) value).getName()).append('"');
        }
    }

    private static class AnnotationSerializer extends JSONTypeSerializer {
        @Override
        void serialize(Object value, Writer content, GetterInfo getterInfo, JsonConfig jsonConfig, int indent) throws Exception {
            content.append('"').append(value.toString()).append('"');
        }
    }
}
