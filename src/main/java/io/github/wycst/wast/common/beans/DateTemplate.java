package io.github.wycst.wast.common.beans;

import io.github.wycst.wast.common.utils.NumberUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /**
     * 解析日期
     *
     * @param buf    字符数组
     * @param offset 字符开始
     * @param len    字符长度
     * @return
     */
    public Date parse(char[] buf, int offset, int len) {
        int year = 0, month = 0, day = 0, hour = 0, minute = 0, second = 0, millisecond = 0;
        int factor = 0, bufLength = offset + len;
        for (DateFieldIndex fieldIndex : fieldIndexs) {
            switch (fieldIndex.field) {
                case Date.YEAR: {
                    int yearLen = fieldIndex.len;
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
                    second = NumberUtils.parseInt1(buf, secOffset);
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
                    millisecond = NumberUtils.digitDecimal(buf[msOffset++]);
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
        return new Date(year, month, day, hour, minute, second, millisecond);
    }

    /**
     * 解析日期
     *
     * @param dateStr 字符串
     * @return
     */
    public Date parse(String dateStr) {
        char[] buf = dateStr.toCharArray();
        return parse(buf, 0, buf.length);
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
}
