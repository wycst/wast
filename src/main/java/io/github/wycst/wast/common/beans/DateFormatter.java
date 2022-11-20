package io.github.wycst.wast.common.beans;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 常用日期格式化
 *
 * @Author wangyunchao
 */
public class DateFormatter {

    private DateTemplate dateTemplate;

    public static final DateFormatter YMDHMS_S_17 = new DateFormatterYMDHMS_S_17();
    public static final DateFormatter YMDHMS_14 = new DateFormatterYMDHMS_14();
    public static final DateFormatter YMD_8 = new DateFormatterYMD_8();
    public static final DateFormatter HMS_6 = new DateFormatterHMS_6();
    private static Map<String, DateFormatter> dateFormatterMap = new HashMap<String, DateFormatter>();

    static {

        DateFormatter temp;
        dateFormatterMap.put("yyyy-MM-dd HH:mm:ss", temp = DateFormatter.of('-', ':', ' '));
        dateFormatterMap.put("Y-M-d H:m:s", temp);
        dateFormatterMap.put("yyyy-MM-ddTHH:mm:ss", temp = DateFormatter.of('-', ':', 'T'));
        dateFormatterMap.put("yyyy-MM-dd'T'HH:mm:ss", temp);
        dateFormatterMap.put("yyyy/MM/dd HH:mm:ss", temp = DateFormatter.of('/', ':', ' '));
        dateFormatterMap.put("Y/M/d H:m:s", temp);
        dateFormatterMap.put("yyyy/MM/ddTHH:mm:ss", temp = DateFormatter.of('/', ':', 'T'));
        dateFormatterMap.put("yyyy/MM/dd'T'HH:mm:ss", temp);

        dateFormatterMap.put("yyyy-MM-dd HH:mm:ss.S", temp = DateFormatter.of('-', ':', ' ', true));
        dateFormatterMap.put("yyyy-MM-dd HH:mm:ss.SSS", temp);
        dateFormatterMap.put("yyyy-MM-ddTHH:mm:ss.S", temp = DateFormatter.of('-', ':', 'T', true));
        dateFormatterMap.put("yyyy-MM-ddTHH:mm:ss.SSS", temp);
        dateFormatterMap.put("yyyy-MM-dd'T'HH:mm:ss.S", temp);
        dateFormatterMap.put("yyyy-MM-dd'T'HH:mm:ss.SSS", temp);
        dateFormatterMap.put("yyyy/MM/dd HH:mm:ss.S", temp = DateFormatter.of('/', ':', ' ', true));
        dateFormatterMap.put("yyyy/MM/dd HH:mm:ss.SSS", temp);
        dateFormatterMap.put("yyyy/MM/ddTHH:mm:ss.S", temp = DateFormatter.of('/', ':', 'T', true));
        dateFormatterMap.put("yyyy/MM/ddTHH:mm:ss.SSS", temp);
        dateFormatterMap.put("yyyy/MM/dd'T'HH:mm:ss.S", temp);
        dateFormatterMap.put("yyyy/MM/dd'T'HH:mm:ss.SSS", temp);

        dateFormatterMap.put("yyyyMMddHHmmss", DateFormatter.YMDHMS_14);
        dateFormatterMap.put("YMdHms", DateFormatter.YMDHMS_14);
        dateFormatterMap.put("yyyyMMddHHmmssS", DateFormatter.YMDHMS_S_17);
        dateFormatterMap.put("yyyyMMddHHmmssSSS", DateFormatter.YMDHMS_S_17);
        dateFormatterMap.put("yyyyMMdd", DateFormatter.YMD_8);
        dateFormatterMap.put("YMd", DateFormatter.YMD_8);
        dateFormatterMap.put("HHmmss", DateFormatter.HMS_6);
        dateFormatterMap.put("Hms", DateFormatter.HMS_6);

        dateFormatterMap.put("yyyy-MM-dd", temp = DateFormatter.ofDate('-'));
        dateFormatterMap.put("Y-M-d", temp);
        dateFormatterMap.put("yyyy/MM/dd", temp = DateFormatter.ofDate('/'));
        dateFormatterMap.put("Y/M/d", temp);

        dateFormatterMap.put("HH:mm:ss", temp = DateFormatter.ofTime(':'));
        dateFormatterMap.put("H:m:s", temp);
        dateFormatterMap.put("HH/mm/ss", temp = DateFormatter.ofTime('/'));
        dateFormatterMap.put("H/m/s", temp);
    }

    /**
     * 通用表达式
     *
     * @param pattern
     * @return
     */
    public static DateFormatter of(String pattern) {
        if (pattern == null) return null;
        // from cache
        if (dateFormatterMap.containsKey(pattern)) return dateFormatterMap.get(pattern);

        // not cache
        DateFormatter dateFormatter = new DateFormatter();
        dateFormatter.dateTemplate = new DateTemplate(pattern);
        return dateFormatter;
    }

    /**
     * 支持yyyy?MM?dd?HH?mm?ss
     *
     * @param dateToken
     * @param timeToken
     * @param concat
     * @return
     */
    public static DateFormatter of(char dateToken, char timeToken, char concat) {
        return new DateFormatterYMDHMS_19(dateToken, timeToken, concat);
    }

    /**
     * 构建带毫秒的日期时间格式化器
     *
     * @param dateToken
     * @param timeToken
     * @param concat
     * @param millis
     * @return
     */
    public static DateFormatter of(char dateToken, char timeToken, char concat, boolean millis) {
        return new DateFormatterYMDHMS_S_23(new DateFormatterYMDHMS_19(dateToken, timeToken, concat));
    }

    /**
     * yyyy?mm?dd
     *
     * @param dateToken
     * @return
     */
    public static DateFormatter ofDate(char dateToken) {
        return new DateFormatterYMD_10(dateToken);
    }

    /**
     * HH?mm?ss
     *
     * @param timeToken
     * @return
     */
    public static DateFormatter ofTime(char timeToken) {
        return new DateFormatterHMS_8(timeToken);
    }

    /**
     * 格式化日期
     *
     * @param date
     * @return
     */
    public String format(Date date) {
        StringBuilder builder = new StringBuilder();
        dateTemplate.formatTo(date, builder);
        return builder.toString();
    }

    /**
     * 通用格式化(不带毫秒)
     *
     * @param year
     * @param month
     * @param dayOfMonth
     * @param hour
     * @param minute
     * @param second
     * @return
     */
    public String format(int year,
                         int month,
                         int dayOfMonth,
                         int hour,
                         int minute,
                         int second) {
        StringBuilder builder = new StringBuilder();
        dateTemplate.formatTo(year, month, dayOfMonth, hour, minute, second, 0, builder);
        return builder.toString();
    }

    /**
     * 通用格式化（带毫秒）
     *
     * @param year
     * @param month
     * @param dayOfMonth
     * @param hour
     * @param minute
     * @param second
     * @param millisecond
     * @return
     */
    public String format(int year,
                         int month,
                         int dayOfMonth,
                         int hour,
                         int minute,
                         int second,
                         int millisecond) {
        StringBuilder builder = new StringBuilder();
        dateTemplate.formatTo(year, month, dayOfMonth, hour, minute, second, millisecond, builder);
        return builder.toString();
    }

    /**
     * 格式化日期
     *
     * @param date
     * @return
     */
    public void formatTo(Date date, Appendable appendable) {
        dateTemplate.formatTo(date, appendable);
    }

    /**
     * 格式化到指定appendable
     *
     * @param year
     * @param month
     * @param dayOfMonth
     * @param hour
     * @param minute
     * @param second
     * @param appendable
     */
    public void formatTo(int year,
                         int month,
                         int dayOfMonth,
                         int hour,
                         int minute,
                         int second,
                         Appendable appendable) {
        dateTemplate.formatTo(year, month, dayOfMonth, hour, minute, second, 0, appendable);
    }

    /**
     * 格式化到指定appendable
     *
     * @param year
     * @param month
     * @param dayOfMonth
     * @param hour
     * @param minute
     * @param second
     * @param millisecond
     * @param appendable
     */
    public void formatTo(int year,
                         int month,
                         int dayOfMonth,
                         int hour,
                         int minute,
                         int second,
                         int millisecond,
                         Appendable appendable) {
        dateTemplate.formatTo(year, month, dayOfMonth, hour, minute, second, millisecond, appendable);
    }

    static abstract class PatternedFormatter extends DateFormatter {

        @Override
        public String format(int year, int month, int dayOfMonth, int hour, int minute, int second) {
            StringBuilder appendable = new StringBuilder();
            formatTo(year, month, dayOfMonth, hour, minute, second, appendable);
            return appendable.toString();
        }

        @Override
        public void formatTo(Date date, Appendable appendable) {
            formatTo(date.year, date.month, date.dayOfMonth, date.hourOfDay, date.minute, date.second, appendable);
        }

        @Override
        public void formatTo(int year, int month, int dayOfMonth, int hour, int minute, int second, int millisecond, Appendable appendable) {
            formatTo(year, month, dayOfMonth, hour, minute, second, appendable);
        }

        protected void appendMillisecond(Appendable appendable, int millisecond) throws IOException {
            char[] DigitTens = DateTemplate.DigitTens;
            char[] DigitOnes = DateTemplate.DigitOnes;
            char s1 = (char) (millisecond / 100 + 48);
            int v = millisecond % 100;
            appendable.append(s1);
            appendable.append(DigitTens[v]);
            appendable.append(DigitOnes[v]);
        }
    }

    static class DateFormatterYMDHMS_19 extends PatternedFormatter {
        public static final String DatePattern = "yyyy?MM?dd?HH?mm?ss";

        private final char dateToken;
        private final char timeToken;
        private final char concat;

        private DateFormatterYMDHMS_19(char dateToken, char timeToken, char concat) {
            this.dateToken = dateToken;
            this.timeToken = timeToken;
            this.concat = concat;
        }

        @Override
        public void formatTo(int year, int month, int dayOfMonth, int hour, int minute, int second, Appendable appendable) {
            try {
                if (year < 0) {
                    appendable.append('-');
                    year = -year;
                }
                int y1 = year / 100, y2 = year - y1 * 100;
                char[] DigitTens = DateTemplate.DigitTens;
                char[] DigitOnes = DateTemplate.DigitOnes;
                appendable.append(DigitTens[y1]);
                appendable.append(DigitOnes[y1]);
                appendable.append(DigitTens[y2]);
                appendable.append(DigitOnes[y2]);
                appendable.append(dateToken);
                appendable.append(DigitTens[month]);
                appendable.append(DigitOnes[month]);
                appendable.append(dateToken);
                appendable.append(DigitTens[dayOfMonth]);
                appendable.append(DigitOnes[dayOfMonth]);
                appendable.append(concat);
                appendable.append(DigitTens[hour]);
                appendable.append(DigitOnes[hour]);
                appendable.append(timeToken);
                appendable.append(DigitTens[minute]);
                appendable.append(DigitOnes[minute]);
                appendable.append(timeToken);
                appendable.append(DigitTens[second]);
                appendable.append(DigitOnes[second]);
            } catch (IOException e) {
                throw new UnsupportedOperationException(e);
            }
        }
    }

    static class DateFormatterYMDHMS_S_23 extends PatternedFormatter {
        public static final String DatePattern = "yyyy?MM?dd?HH?mm?ss.S+";

        private final DateFormatterYMDHMS_19 dateFormatterYMDHMS_19;

        private DateFormatterYMDHMS_S_23(DateFormatterYMDHMS_19 dateFormatterYMDHMS_19) {
            dateFormatterYMDHMS_19.getClass();
            this.dateFormatterYMDHMS_19 = dateFormatterYMDHMS_19;
        }

        @Override
        public void formatTo(int year, int month, int dayOfMonth, int hour, int minute, int second, Appendable appendable) {
            formatTo(year, month, dayOfMonth, hour, minute, second, 0, appendable);
        }

        @Override
        public void formatTo(int year, int month, int dayOfMonth, int hour, int minute, int second, int millisecond, Appendable appendable) {
            try {
                dateFormatterYMDHMS_19.formatTo(year, month, dayOfMonth, hour, minute, second, appendable);
                appendable.append('.');
                appendMillisecond(appendable, millisecond);
            } catch (IOException e) {
                throw new UnsupportedOperationException(e);
            }
        }
    }

    /**
     * yyyyMMddHHmmss
     */
    static class DateFormatterYMDHMS_14 extends PatternedFormatter {
        public static final String DatePattern = "yyyyMMddHHmmss";

        private DateFormatterYMDHMS_14() {
        }

        @Override
        public void formatTo(int year, int month, int dayOfMonth, int hour, int minute, int second, Appendable appendable) {
            try {
                if (year < 0) {
                    appendable.append('-');
                    year = -year;
                }
                int y1 = year / 100, y2 = year - y1 * 100;
                char[] DigitTens = DateTemplate.DigitTens;
                char[] DigitOnes = DateTemplate.DigitOnes;
                appendable.append(DigitTens[y1]);
                appendable.append(DigitOnes[y1]);
                appendable.append(DigitTens[y2]);
                appendable.append(DigitOnes[y2]);
                appendable.append(DigitTens[month]);
                appendable.append(DigitOnes[month]);
                appendable.append(DigitTens[dayOfMonth]);
                appendable.append(DigitOnes[dayOfMonth]);
                appendable.append(DigitTens[hour]);
                appendable.append(DigitOnes[hour]);
                appendable.append(DigitTens[minute]);
                appendable.append(DigitOnes[minute]);
                appendable.append(DigitTens[second]);
                appendable.append(DigitOnes[second]);
            } catch (IOException e) {
                throw new UnsupportedOperationException(e);
            }
        }
    }

    /**
     * yyyyMMddHHmmssSSS
     */
    static class DateFormatterYMDHMS_S_17 extends PatternedFormatter {
        public static final String DatePattern = "yyyyMMddHHmmssSSS";

        private DateFormatterYMDHMS_S_17() {
        }

        @Override
        public void formatTo(int year, int month, int dayOfMonth, int hour, int minute, int second, int millisecond, Appendable appendable) {
            try {
                if (year < 0) {
                    appendable.append('-');
                    year = -year;
                }
                int y1 = year / 100, y2 = year - y1 * 100;
                char[] DigitTens = DateTemplate.DigitTens;
                char[] DigitOnes = DateTemplate.DigitOnes;
                appendable.append(DigitTens[y1]);
                appendable.append(DigitOnes[y1]);
                appendable.append(DigitTens[y2]);
                appendable.append(DigitOnes[y2]);
                appendable.append(DigitTens[month]);
                appendable.append(DigitOnes[month]);
                appendable.append(DigitTens[dayOfMonth]);
                appendable.append(DigitOnes[dayOfMonth]);
                appendable.append(DigitTens[hour]);
                appendable.append(DigitOnes[hour]);
                appendable.append(DigitTens[minute]);
                appendable.append(DigitOnes[minute]);
                appendable.append(DigitTens[second]);
                appendable.append(DigitOnes[second]);
                appendMillisecond(appendable, millisecond);
            } catch (IOException e) {
                throw new UnsupportedOperationException(e);
            }
        }

        @Override
        public void formatTo(int year, int month, int dayOfMonth, int hour, int minute, int second, Appendable appendable) {
            formatTo(year, month, dayOfMonth, hour, minute, second, 0, appendable);
        }
    }

    static class DateFormatterYMD_10 extends PatternedFormatter {
        public static final String DatePattern = "yyyy?MM?dd";
        private final char dateToken;

        private DateFormatterYMD_10(char dateToken) {
            this.dateToken = dateToken;
        }

        @Override
        public void formatTo(int year, int month, int dayOfMonth, int hour, int minute, int second, Appendable appendable) {
            try {
                if (year < 0) {
                    appendable.append('-');
                    year = -year;
                }
                int y1 = year / 100, y2 = year - y1 * 100;
                char[] DigitTens = DateTemplate.DigitTens;
                char[] DigitOnes = DateTemplate.DigitOnes;
                appendable.append(DigitTens[y1]);
                appendable.append(DigitOnes[y1]);
                appendable.append(DigitTens[y2]);
                appendable.append(DigitOnes[y2]);
                appendable.append(dateToken);
                appendable.append(DigitTens[month]);
                appendable.append(DigitOnes[month]);
                appendable.append(dateToken);
                appendable.append(DigitTens[dayOfMonth]);
                appendable.append(DigitOnes[dayOfMonth]);
            } catch (IOException e) {
                throw new UnsupportedOperationException(e);
            }
        }
    }

    static class DateFormatterYMD_8 extends PatternedFormatter {
        public static final String DatePattern = "yyyyMMdd";

        private DateFormatterYMD_8() {
        }

        @Override
        public void formatTo(int year, int month, int dayOfMonth, int hour, int minute, int second, Appendable appendable) {
            try {
                if (year < 0) {
                    appendable.append('-');
                    year = -year;
                }
                int y1 = year / 100, y2 = year - y1 * 100;
                char[] DigitTens = DateTemplate.DigitTens;
                char[] DigitOnes = DateTemplate.DigitOnes;
                appendable.append(DigitTens[y1]);
                appendable.append(DigitOnes[y1]);
                appendable.append(DigitTens[y2]);
                appendable.append(DigitOnes[y2]);
                appendable.append(DigitTens[month]);
                appendable.append(DigitOnes[month]);
                appendable.append(DigitTens[dayOfMonth]);
                appendable.append(DigitOnes[dayOfMonth]);
            } catch (IOException e) {
                throw new UnsupportedOperationException(e);
            }
        }
    }

    static class DateFormatterHMS_6 extends PatternedFormatter {
        public static final String DatePattern = "HHmmss";

        @Override
        public void formatTo(int year, int month, int dayOfMonth, int hour, int minute, int second, Appendable appendable) {
            try {
                char[] DigitTens = DateTemplate.DigitTens;
                char[] DigitOnes = DateTemplate.DigitOnes;
                appendable.append(DigitTens[hour]);
                appendable.append(DigitOnes[hour]);
                appendable.append(DigitTens[minute]);
                appendable.append(DigitOnes[minute]);
                appendable.append(DigitTens[second]);
                appendable.append(DigitOnes[second]);
            } catch (IOException e) {
                throw new UnsupportedOperationException(e);
            }
        }
    }

    static class DateFormatterHMS_8 extends PatternedFormatter {
        public static final String DatePattern = "HH?mm?ss";
        private char timeToken;

        private DateFormatterHMS_8(char timeToken) {
            this.timeToken = timeToken;
        }

        @Override
        public void formatTo(int year, int month, int dayOfMonth, int hour, int minute, int second, Appendable appendable) {
            try {
                char[] DigitTens = DateTemplate.DigitTens;
                char[] DigitOnes = DateTemplate.DigitOnes;
                appendable.append(DigitTens[hour]);
                appendable.append(DigitOnes[hour]);
                appendable.append(timeToken);
                appendable.append(DigitTens[minute]);
                appendable.append(DigitOnes[minute]);
                appendable.append(timeToken);
                appendable.append(DigitTens[second]);
                appendable.append(DigitOnes[second]);
            } catch (IOException e) {
                throw new UnsupportedOperationException(e);
            }
        }
    }
}
