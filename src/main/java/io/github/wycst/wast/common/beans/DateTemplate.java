package io.github.wycst.wast.common.beans;

import io.github.wycst.wast.common.utils.NumberUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * 日期模板（提供解析结构），支持年月日时分秒以及毫秒等字段（YMdHmsS）
 * <p> 线程安全
 * <p> 兼容SimpleDateFormat的常用pattern,性能是SimpleDateFormat的5-10倍左右，JDK8 DateTimeFormatter的3-5倍左右
 *
 * <p>
 * Y(y) 4位数年份(2位数年份,如果y个数大于2时转为Y处理)
 * M 格式化2位月份（解析时兼容1位）
 * d 格式化2位天（解析时兼容1位）
 * H(h) 格式化24制2位小时数（解析时兼容1位）
 * m 格式化2位分钟（解析时兼容1位）
 * s 格式化2位秒（解析时兼容1位）
 * S 格式化3位毫秒
 * <p>
 * 例如，pattern yyyy-MM-dd HH:mm:ss 能兼容2022-2-3 11:12:6和2022-02-03 11:12:06
 *
 * @Author: wangy
 * @Description:
 * @see java.text.SimpleDateFormat#parse(String)
 */
public final class DateTemplate {

    private final String pattern;
    private int yearIndex = -1;
    private int fullYearIndex = -1;
    private int monthIndex = -1;
    private int dayIndex = -1;
    private int hourIndex = -1;
    private int minuteIndex = -1;
    private int secondIndex = -1;
    private int millisecondIndex = -1;

    private final List<DateFieldIndex> fieldIndexs = new ArrayList<DateFieldIndex>();

    //    private final static String[] FORMAT_DIGITS = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09"};
    private final static String[] WEEK_DAYS = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
    final static char[] DigitOnes = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    };

    final static char[] DigitTens = {
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
            '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
            '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
            '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
            '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
            '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
            '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
            '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
            '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
    };

    public static class DateFieldIndex implements Comparable<DateFieldIndex> {
        final int field;
        final Integer index;
        final int len;

        DateFieldIndex(int field, int index, int len) {
            this.field = field;
            this.index = index;
            this.len = len;
        }

        public int compareTo(DateFieldIndex o) {
            return index.compareTo(o.index);
        }
    }

    public DateTemplate(String template) {
        template.getClass();
        this.pattern = template;
        int length = template.length();
        int count = 0;
        int lowerYearNum = 0;
        for (int i = 0, j = 0; i < length; i++) {
            char ch = template.charAt(i);
            switch (ch) {
                case 'Y':
                    if (fullYearIndex == -1) {
                        fullYearIndex = j++ + count;
                        count += 3;
                    }
                    continue;
                case 'y':
                    if (yearIndex == -1) {
                        yearIndex = j + count;
                        count++;
                    } else {
                        j++;
                    }
                    lowerYearNum++;
                    continue;
                case 'M':
                    if (monthIndex == -1) {
                        monthIndex = j++ + count;
                        count++;
                    }
                    continue;
                case 'd':
                    if (dayIndex == -1) {
                        dayIndex = j++ + count;
                        count++;
                    }
                    continue;
                case 'H':
                    if (hourIndex == -1) {
                        hourIndex = j++ + count;
                        count++;
                    }
                    continue;
                case 'm':
                    if (minuteIndex == -1) {
                        minuteIndex = j++ + count;
                        count++;
                    }
                    continue;
                case 's':
                    if (secondIndex == -1) {
                        secondIndex = j++ + count;
                        count++;
                    }
                    continue;
                case 'S':
                    if (millisecondIndex == -1) {
                        millisecondIndex = j++ + count;
                        count += 2;
                    }
                    continue;
                default:
                    j++;
                    continue;
            }
        }

        if (yearIndex > -1 && lowerYearNum > 2) {
            fullYearIndex = yearIndex;
        }
        if (fullYearIndex > -1) {
            fieldIndexs.add(new DateFieldIndex(Date.YEAR, fullYearIndex, 4));
        } else if (yearIndex > -1) {
            fieldIndexs.add(new DateFieldIndex(Date.YEAR, yearIndex, 2));
        }
        if (monthIndex > -1) {
            fieldIndexs.add(new DateFieldIndex(Date.MONTH, monthIndex, 2));
        }
        if (dayIndex > -1) {
            fieldIndexs.add(new DateFieldIndex(Date.DAY_OF_MONTH, dayIndex, 2));
        }
        if (hourIndex > -1) {
            fieldIndexs.add(new DateFieldIndex(Date.HOURS, hourIndex, 2));
        }
        if (minuteIndex > -1) {
            fieldIndexs.add(new DateFieldIndex(Date.MINUTE, minuteIndex, 2));
        }
        if (secondIndex > -1) {
            fieldIndexs.add(new DateFieldIndex(Date.SECOND, secondIndex, 2));
        }
        if (millisecondIndex > -1) {
            fieldIndexs.add(new DateFieldIndex(Date.MILLISECOND, millisecondIndex, 3));
        }
        Collections.sort(fieldIndexs);
    }

    public GeneralDate parseGeneralDate(char[] buf, int offset, int len, TimeZone timeZone) {
        int year = 0, month = 0, day = 0, hour = 0, minute = 0, second = 0, millisecond = 0;
        int factor = 0, bufLength = offset + len;
        for (DateFieldIndex fieldIndex : fieldIndexs) {
            switch (fieldIndex.field) {
                case Date.YEAR: {
                    int yearLen = fieldIndex.len;
                    // todo 如果要解析的字符中，年份是负数，需要判断第一个字符是否为'-',然后factor++,有需求再实现
                    if (yearLen == 2) {
                        year = NumberUtils.parseInt2(buf, yearIndex + offset + factor);
                        year += new Date().getYear() / 100 * 100;
                    } else {
                        year = NumberUtils.parseInt4(buf, fullYearIndex + offset + factor);
                    }
                    continue;
                }
                case Date.MONTH: {
                    int monOffset = monthIndex + offset + factor;
                    month = NumberUtils.parseInt1(buf, monOffset++);
                    if (monOffset < bufLength) {
                        int mon2 = NumberUtils.digitDecimal(buf[monOffset]);
                        if (mon2 == -1) {
                            factor--;
                        } else {
                            month = month * 10 + mon2;
                        }
                    }
                    continue;
                }
                case Date.DAY_OF_MONTH: {
                    int dayOffset = dayIndex + offset + factor;
                    day = NumberUtils.parseInt1(buf, dayOffset++);
                    if (dayOffset < bufLength) {
                        int d2 = NumberUtils.digitDecimal(buf[dayOffset]);
                        if (d2 == -1) {
                            factor--;
                        } else {
                            day = day * 10 + d2;
                        }
                    }
                    continue;
                }
                case Date.HOURS: {
                    int hourOffset = hourIndex + offset + factor;
                    hour = NumberUtils.parseInt1(buf, hourOffset++);
                    if (hourOffset < bufLength) {
                        int h2 = NumberUtils.digitDecimal(buf[hourOffset]);
                        if (h2 == -1) {
                            factor--;
                        } else {
                            hour = hour * 10 + h2;
                        }
                    }
                    continue;
                }
                case Date.MINUTE: {
                    int minOffset = minuteIndex + offset + factor;
                    minute = NumberUtils.parseInt1(buf, minOffset++);
                    if (minOffset < bufLength) {
                        int minute2 = NumberUtils.digitDecimal(buf[minOffset]);
                        if (minute2 == -1) {
                            factor--;
                        } else {
                            minute = minute * 10 + minute2;
                        }
                    }
                    continue;
                }
                case Date.SECOND: {
                    int secOffset = secondIndex + offset + factor;
                    second = NumberUtils.parseInt1(buf, secOffset++);
                    if (secOffset < bufLength) {
                        int s2 = NumberUtils.digitDecimal(buf[secOffset]);
                        if (s2 == -1) {
                            factor--;
                        } else {
                            second = second * 10 + s2;
                        }
                    }
                    continue;
                }
                case Date.MILLISECOND: {
                    // 只处理最多3位毫秒
                    int msOffset = millisecondIndex + offset + factor;
                    int digit = NumberUtils.digitDecimal(buf[msOffset++]);
                    if (digit != -1) {
                        millisecond = digit;
                    }
                    if (msOffset < bufLength) {
                        int v2 = NumberUtils.digitDecimal(buf[msOffset++]);
                        if (v2 != -1) {
                            millisecond = millisecond * 10 + v2;
                            if (msOffset < bufLength) {
                                int v3 = NumberUtils.digitDecimal(buf[msOffset]);
                                if (v3 != -1) {
                                    millisecond = millisecond * 10 + v3;
                                } else {
                                    factor--;
                                }
                            }
                        } else {
                            factor -= 2;
                        }
                    }
                    continue;
                }
            }
        }
        return new GeneralDate(year, month, day, hour, minute, second, millisecond, timeZone);
    }

    public GeneralDate parseGeneralDate(byte[] buf, int offset, int len, TimeZone timeZone) {
        int year = 0, month = 0, day = 0, hour = 0, minute = 0, second = 0, millisecond = 0;
        int factor = 0, bufLength = offset + len;
        for (DateFieldIndex fieldIndex : fieldIndexs) {
            switch (fieldIndex.field) {
                case Date.YEAR: {
                    int yearLen = fieldIndex.len;
                    // todo 如果要解析的字符中，年份是负数，需要判断第一个字符是否为'-',然后factor++,有需求再实现
                    if (yearLen == 2) {
                        year = NumberUtils.parseInt2(buf, yearIndex + offset + factor);
                        year += new Date().getYear() / 100 * 100;
                    } else {
                        year = NumberUtils.parseInt4(buf, fullYearIndex + offset + factor);
                    }
                    continue;
                }
                case Date.MONTH: {
                    int monOffset = monthIndex + offset + factor;
                    month = NumberUtils.parseInt1(buf, monOffset++);
                    if (monOffset < bufLength) {
                        int mon2 = NumberUtils.digitDecimal((char) buf[monOffset]);
                        if (mon2 == -1) {
                            factor--;
                        } else {
                            month = month * 10 + mon2;
                        }
                    }
                    continue;
                }
                case Date.DAY_OF_MONTH: {
                    int dayOffset = dayIndex + offset + factor;
                    day = NumberUtils.parseInt1(buf, dayOffset++);
                    if (dayOffset < bufLength) {
                        int d2 = NumberUtils.digitDecimal((char) buf[dayOffset]);
                        if (d2 == -1) {
                            factor--;
                        } else {
                            day = day * 10 + d2;
                        }
                    }
                    continue;
                }
                case Date.HOURS: {
                    int hourOffset = hourIndex + offset + factor;
                    hour = NumberUtils.parseInt1(buf, hourOffset++);
                    if (hourOffset < bufLength) {
                        int h2 = NumberUtils.digitDecimal(buf[hourOffset]);
                        if (h2 == -1) {
                            factor--;
                        } else {
                            hour = hour * 10 + h2;
                        }
                    }
                    continue;
                }
                case Date.MINUTE: {
                    int minOffset = minuteIndex + offset + factor;
                    minute = NumberUtils.parseInt1(buf, minOffset++);
                    if (minOffset < bufLength) {
                        int minute2 = NumberUtils.digitDecimal(buf[minOffset]);
                        if (minute2 == -1) {
                            factor--;
                        } else {
                            minute = minute * 10 + minute2;
                        }
                    }
                    continue;
                }
                case Date.SECOND: {
                    int secOffset = secondIndex + offset + factor;
                    second = NumberUtils.parseInt1(buf, secOffset++);
                    if (secOffset < bufLength) {
                        int s2 = NumberUtils.digitDecimal(buf[secOffset]);
                        if (s2 == -1) {
                            factor--;
                        } else {
                            second = second * 10 + s2;
                        }
                    }
                    continue;
                }
                case Date.MILLISECOND: {
                    // 只处理最多3位毫秒
                    int msOffset = millisecondIndex + offset + factor;
                    int digit = NumberUtils.digitDecimal(buf[msOffset++]);
                    if (digit != -1) {
                        millisecond = digit;
                    }
                    if (msOffset < bufLength) {
                        int v2 = NumberUtils.digitDecimal(buf[msOffset++]);
                        if (v2 != -1) {
                            millisecond = millisecond * 10 + v2;
                            if (msOffset < bufLength) {
                                int v3 = NumberUtils.digitDecimal(buf[msOffset]);
                                if (v3 != -1) {
                                    millisecond = millisecond * 10 + v3;
                                } else {
                                    factor--;
                                }
                            }
                        } else {
                            factor -= 2;
                        }
                    }
                    continue;
                }
            }
        }
        return new GeneralDate(year, month, day, hour, minute, second, millisecond, timeZone);
    }

    /**
     * 解析日期返回日期在指定时区下的时间戳
     *
     * @param buf
     * @param offset
     * @param len
     * @param timeZone
     * @return
     */
    public long parseTime(char[] buf, int offset, int len, TimeZone timeZone) {
        GeneralDate generalDate = parseGeneralDate(buf, offset, len, timeZone);
        generalDate.updateTime();
        return generalDate.timeMills;
    }

    /**
     * 解析日期返回日期在指定时区下的时间戳
     *
     * @param buf
     * @param offset
     * @param len
     * @param timeZone
     * @return
     */
    public long parseTime(byte[] buf, int offset, int len, TimeZone timeZone) {
        GeneralDate generalDate = parseGeneralDate(buf, offset, len, timeZone);
        generalDate.updateTime();
        return generalDate.timeMills;
    }

    /**
     * 解析日期
     *
     * @param buf      字符数组
     * @param offset   字符开始
     * @param len      字符长度
     * @param timeZone 时钟
     * @return
     */
    public Date parse(char[] buf, int offset, int len, TimeZone timeZone) {
        GeneralDate generalDate = parseGeneralDate(buf, offset, len, timeZone);
        return new Date(generalDate.year, generalDate.month, generalDate.dayOfMonth, generalDate.hourOfDay, generalDate.minute, generalDate.second, generalDate.millisecond, timeZone);
    }

    /**
     * 解析日期
     *
     * @param buf    字符数组
     * @param offset 字符开始
     * @param len    字符长度
     * @return
     */
    public Date parse(char[] buf, int offset, int len) {
        return parse(buf, offset, len, null);
    }

    /**
     * 解析日期
     *
     * @param dateStr  字符串
     * @param timeZone 时钟
     * @return
     */
    public Date parse(String dateStr, TimeZone timeZone) {
        char[] buf = dateStr.toCharArray();
        return parse(buf, 0, buf.length, timeZone);
    }

    /**
     * 解析日期
     *
     * @param dateStr 字符串
     * @return
     */
    public Date parse(String dateStr) {
        return parse(dateStr, null);
    }

    /**
     * 格式化日期对象为字符串
     *
     * @param date
     * @return
     * @see Date#format(String)
     * @see Date#formatTo(String, Appendable)
     */
    public String format(Date date) {
        StringBuilder writer = new StringBuilder();
        date.formatTo(pattern, writer);
        return writer.toString();
    }

    /**
     * 格式化日期对象为字符串
     *
     * @param date
     * @param appendable
     * @see Date#formatTo(String, Appendable)
     */
    public void formatTo(Date date, Appendable appendable) {
        date.formatTo(pattern, appendable);
    }

    /**
     * 格式化日期对象为字符串
     *
     * @param date
     * @param appendable
     * @param escapeQuot 是否转义双引号
     * @see Date#formatTo(String, Appendable)
     */
    public void formatTo(Date date, Appendable appendable, boolean escapeQuot) {
        date.formatTo(pattern, appendable, escapeQuot);
    }

    static void formatTo(int year,
                         int month,
                         int dayOfMonth,
                         int hour,
                         int minute,
                         int second,
                         int millisecond,
                         int dayOfWeek,
                         int daysOfYear,
                         int weekOfMonth,
                         int weekOfYear,
                         TimeZone timeZone,
                         String template,
                         Appendable appendable
    ) {
        formatTo(year, month, dayOfMonth, hour, minute, second, millisecond, dayOfWeek, daysOfYear, weekOfMonth, weekOfYear, timeZone, template, appendable, false);
    }

    static void formatTo(int year,
                         int month,
                         int dayOfMonth,
                         int hour,
                         int minute,
                         int second,
                         int millisecond,
                         int dayOfWeek,
                         int daysOfYear,
                         int weekOfMonth,
                         int weekOfYear,
                         TimeZone timeZone,
                         String template,
                         Appendable appendable,
                         boolean escape) {
        try {
            String pattern = template.trim();
            int len = pattern.length();
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
                            if (year < 0) {
                                appendable.append('-');
                                year = -year;
                            }
                            int y2 = year % 100;
                            if (count == 2) {
                                // 输出2位数年份
                                appendable.append(DigitTens[y2]);
                                appendable.append(DigitOnes[y2]);
                            } else {
                                int y1 = year / 100;
                                // 输出完整的年份
                                appendable.append(DigitTens[y1]);
                                appendable.append(DigitOnes[y1]);
                                appendable.append(DigitTens[y2]);
                                appendable.append(DigitOnes[y2]);
                            }
                            break;
                        }
                        case 'M': {
                            // 月份
                            appendable.append(DigitTens[month]);
                            appendable.append(DigitOnes[month]);
                            break;
                        }
                        case 'd': {
                            appendable.append(DigitTens[dayOfMonth]);
                            appendable.append(DigitOnes[dayOfMonth]);
                            break;
                        }
                        case 'A':
                        case 'a': {
                            // 上午/下午
                            if (hour < 12) {
                                appendable.append("上午");
                            } else {
                                appendable.append("下午");
                            }
                            break;
                        }
                        case 'H': {
                            // 0-23
                            appendable.append(DigitTens[hour]);
                            appendable.append(DigitOnes[hour]);
                            break;
                        }
                        case 'h': {
                            // 1-12 小时格式
                            int h = hour % 12;
                            if (h == 0)
                                h = 12;
                            appendable.append(DigitTens[h]);
                            appendable.append(DigitOnes[h]);
                            break;
                        }
                        case 'm': {
                            // 分钟 0-59
                            appendable.append(DigitTens[minute]);
                            appendable.append(DigitOnes[minute]);
                            break;
                        }
                        case 's': {
                            // 秒 0-59
                            appendable.append(DigitTens[second]);
                            appendable.append(DigitOnes[second]);
                            break;
                        }
                        case 'S': {
                            // 统一3位毫秒
                            char s1 = (char) (millisecond / 100 + 48);
                            int v = millisecond % 100;
                            appendable.append(s1);
                            appendable.append(DigitTens[v]);
                            appendable.append(DigitOnes[v]);
                            break;
                        }
                        case 'E': {
                            // 星期
                            appendable.append(WEEK_DAYS[(dayOfWeek - 1) & 7]);
                            break;
                        }
                        case 'D': {
                            // daysOfYear
                            appendable.append(String.valueOf(daysOfYear));
                            break;
                        }
                        case 'F': {
                            // weekOfMonth
                            appendable.append(String.valueOf(weekOfMonth));
                            break;
                        }
                        case 'W': {
                            // actualWeekOfMonth: weekOfMonth or weekOfMonth + 1
                            // 当天星期数如果小于当月第一天星期数时需要+1，否则直接为weekOfMonth
                            int firstDayOfWeek = (dayOfWeek + 7 - (dayOfMonth % 7)) % 7 + 1;
                            if (dayOfWeek < firstDayOfWeek) {
                                appendable.append(String.valueOf(weekOfMonth + 1));
                            } else {
                                appendable.append(String.valueOf(weekOfMonth));
                            }
                            break;
                        }
                        case 'w': {
                            // weekOfYear
                            appendable.append(String.valueOf(weekOfYear));
                            break;
                        }
                        case 'z': {
                            // timezone
                            TimeZone tz = timeZone == null ? GeneralDate.getDefaultTimeZone() : timeZone;
                            appendable.append(tz.getID());
                            break;
                        }
                        default: {
                            // 其他输出
                            if (prevChar != '\0') {
                                // 输出count个 prevChar
                                int n = count;
                                while (n-- > 0) {
                                    if (escape && prevChar == '"') {
                                        appendable.append('\\');
                                    }
                                    appendable.append(prevChar);
                                }
                            }
                        }
                    }
                    count = 1;
                }
                prevChar = ch;
            }
        } catch (Throwable throwable) {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            throw new IllegalStateException(throwable.getMessage(), throwable);
        }
    }

    /**
     * 以模板输出日期信息
     *
     * @param year
     * @param month
     * @param day
     * @param hour
     * @param minute
     * @param second
     * @param millisecond
     * @param appendable
     */
    public void formatTo(int year, int month, int day, int hour, int minute, int second, int millisecond, Appendable appendable) {
        formatTo(year, month, day, hour, minute, second, millisecond, 0, 0, 0, 0, null, pattern, appendable);
    }

    public void formatTo(int year, int month, int day, int hour, int minute, int second, int millisecond, Appendable appendable, boolean escape) {
        formatTo(year, month, day, hour, minute, second, millisecond, 0, 0, 0, 0, null, pattern, appendable, escape);
    }
}
