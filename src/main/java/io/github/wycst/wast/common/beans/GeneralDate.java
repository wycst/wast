package io.github.wycst.wast.common.beans;

import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.NumberUtils;

import java.lang.reflect.Field;
import java.util.TimeZone;

/**
 * 日期基础类
 *
 * <p> 1582年历法按4年一润，当年删去了10天(10月5日～10月14日) ，1582-？ 按最新历法
 * <p>
 * Asia/Harbin, Asia/Shanghai, Asia/Chongqing, Asia/Urumqi, Asia/Kashgar：
 * <li> 1986年至1991年，每年四月的第2个星期日早上2点，到九月的第2个星期日早上2点之间。
 * <li> 1986年5月4日至9月14日（1986年因是实行夏令时的第一年，从5月4日开始到9月14日结束）
 * <li> 1987年4月12日至9月13日，
 * <li> 1988年4月10日至9月11日，
 * <li> 1989年4月16日至9月17日，
 * <li> 1990年4月15日至9月16日，
 * <li> 1991年4月14日至9月15日。
 * <li> 1992年起，夏令时暂停实行
 * <p>
 * 注：[Asia/*]时区下Calendar消失的时间段（不存在的时间,无法通过设置时间域得到对应的时间点）
 * 以Asia/Shanghai为例，时区文件： %JRE_HOME%/lib/zi/Asia/Shanghai
 * 1900[1900-01-01 08:00:00, 1900-01-01 08:05:42)  5分43秒
 * 1940[1940-06-03 00:00:00, 1940-06-03 00:59:59]  1小时
 * 1941[1940-03-16 00:00:00, 1940-03-16 00:59:59]  1小时
 * 以及夏令时每年开始第一个小时:
 * 1986[1986-05-04 00:00:00, 1986-05-04 00:59:59] 一个小时区间
 * 1987[1987-04-12 00:00:00, 1987-04-12 00:59:59] 一个小时区间
 * ...
 * 1991[1991-04-14 00:00:00, 1991-04-14 00:59:59] 一个小时区间
 * <pre>:
 *
 *   TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");
 *   TimeZone.setDefault(timeZone);
 *
 *   Calendar calendar = Calendar.getInstance();
 *   calendar.set(1900, 0, 1, 8, 0, 0);
 *   calendar.set(Calendar.MILLISECOND, 0);
 *
 *   System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime())); // 1900-01-01 08:05:43
 *   System.out.println(calendar.get(Calendar.HOUR_OF_DAY)); // 8
 *   System.out.println(calendar.get(Calendar.MINUTE));      // 5
 *   System.out.println(calendar.get(Calendar.SECOND));      // 43
 *
 *
 * </pre>
 *
 * @Author: wangy
 * @Date: 2022/8/11 22:53
 * @Description:
 */
public class GeneralDate {

    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int DAY_OF_MONTH = 3;
    public static final int HOURS = 4;
    public static final int MINUTE = 5;
    public static final int SECOND = 6;
    public static final int MILLISECOND = 7;

    protected int year;
    protected int month;
    protected int dayOfMonth;

    protected int hourOfDay;
    protected int minute;
    protected int second;
    protected int millisecond;

    // 是否闰年
    protected boolean leapYear;
    // 当年第多少天
    protected int daysOfYear;

    // 距离1970.1.1 - 时区标准毫秒数
    protected long standardMills;
    // 距离1970.1.1 - 时区校对后的毫秒数
    protected long timeMills = -1;

    // 公元元年（0001）.1.1 ~ 1970.1.1 相对天数
    // 计算来源：1969 * 365 + 1969 / 4 - 1969 / 100 + 1969 / 400 + (1582 / 100 - 1582 / 400 - 10)
    public final static long RELATIVE_DAYS = 719164l;

    // 公元元年（0001）.1.1 ~ 1970.1.1  相对毫秒数
    // 计算来源: RELATIVE_DAYS * 24 * 3600 * 1000
    public final static long RELATIVE_MILLS = 62135769600000l;

    // 以1970.1.1  周四 作为参考
    public final static int RELATIVE_DAY_OF_WEEK = 5;

    // 时钟
    protected TimeZone timeZone;
    // 可变时差
    protected int currentOffset;
    private final static Field defaultTimeZoneField;
    // 公元元年（0001）.1.1 ~ current 相对天数
    protected long currentDays;

    private static final int[] DaysOfYearOffset = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
    private static final int[] DaysOfLeapYearOffset = {0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335};

    // 一天(24小时)实际毫秒数
    protected static final long MILLS_DAY = 86400000; // 24 * 3600 * 1000
    // 平年一年(365天)实际毫秒数:
    protected static final long MILLS_365_DAY = 365 * MILLS_DAY;
    // 闰年一年（366天）毫秒数
    protected static final long MILLS_366_DAY = 366 * MILLS_DAY;
    protected static final long Seconds_1991_09_14_23_59_59 = 62820658799l;
    protected static final long Seconds_1900_01_01_07_59_59 = 59926809599l;

    protected static final YearMeta[] Positive_Year_Metas = new YearMeta[2500];

    // TODO 公元元年之前
    protected static final YearMeta[] Negative_Year_Metas = new YearMeta[2500];

    public GeneralDate(TimeZone timeZone) {
        ofTimeZone(timeZone);
    }

    GeneralDate(int year, int month, int day, int hour, int minute, int second,
                int millisecond, TimeZone timeZone) {
        ofTimeZone(timeZone);
        this.year = year;
        this.month = month;
        this.dayOfMonth = day;
        this.hourOfDay = hour;
        this.minute = minute;
        this.second = second;
        this.millisecond = millisecond;
    }

    public static GeneralDate of(int year, int month, int day, int hour, int minute, int second,
                                 int millisecond) {
        GeneralDate generalDate = new GeneralDate(year, month, day, hour, minute, second, millisecond, (TimeZone) null);
        generalDate.updateTime();
        return generalDate;
    }

    protected void ofTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        if (timeZone == null) {
            this.timeZone = getDefaultTimeZone();
        }
        this.currentOffset = this.timeZone.getRawOffset();
    }

    private static class YearMeta {
        final int year;
        final boolean leap;
        final long offsetDays;

        YearMeta(int year, boolean leap, long offsetDays) {
            this.year = year;
            this.leap = leap;
            this.offsetDays = offsetDays;
        }
    }


    static {
        // 计算 1-1-1 00:00:00 ~ 1970-1-1 00:00:00 之间得 relativeMills
        // 1582年历法按4年一润，当年删去了10天 ，1582-？ 按最新历法
        // 范围： 0001~1969（时间戳从1970年开始）
        // RELATIVE_DAYS = 1969 * 365 + 1969 / 4 - 1969 / 100 + 1969 / 400 + (1582 / 100 - 1582 / 400 - 10);
        // RELATIVE_MILLS = RELATIVE_DAYS * 24 * 3600 * 1000;
        Field timeZoneField = null;
        try {
            timeZoneField = TimeZone.class.getDeclaredField("defaultTimeZone");
            if (!UnsafeHelper.setAccessible(timeZoneField)) {
                timeZoneField.setAccessible(true);
            }
        } catch (Throwable throwable) {
            timeZoneField = null;
        }
        defaultTimeZoneField = timeZoneField;
        for (int year = 0; year < Positive_Year_Metas.length; year++) {
            Positive_Year_Metas[year] = createYearMeta(year);
        }
    }

    public final static long getTime(int year, int month, int day, int hour, int minute, int second,
                                     int millisecond, TimeZone timeZone) {
        GeneralDate generalDate = new GeneralDate(year, month, day, hour, minute, second, millisecond, timeZone);
        generalDate.updateTime();
        return generalDate.timeMills;
    }

    /**
     * 支持格式： {'yyyy-MM-dd', 'yyyy-MM-dd HH:mm:ss'}
     * 年月日必须
     *
     * @param dateStr
     * @return
     */
    public final static long parseTime(String dateStr) {
        return parseTime(dateStr, null);
    }

    /**
     * 支持格式： {'yyyy-MM-dd', 'yyyy-MM-dd HH:mm:ss'}
     * 年月日必须
     *
     * @param dateStr
     * @param timeZone
     * @return
     */
    public final static long parseTime(String dateStr, TimeZone timeZone) {
        GeneralDate generalDate = parseGeneralDate(dateStr, timeZone);
        generalDate.updateTime();
        return generalDate.timeMills;
    }

    /**
     * 支持格式： {'yyyy-MM-dd', 'yyyy-MM-dd HH:mm:ss'}
     * 年月日必须
     *
     * @param dateStr
     * @return
     */
    public final static GeneralDate parseGeneralDate(String dateStr, TimeZone timeZone) {
        dateStr.getClass();
        int length = dateStr.length();
        int year, month, day, hour = 0, minute = 0, second = 0;
        try {
            if (length == 19) {
                year = NumberUtils.parseInt4(dateStr.charAt(0), dateStr.charAt(1), dateStr.charAt(2), dateStr.charAt(3));
                month = NumberUtils.parseInt2(dateStr.charAt(5), dateStr.charAt(6));
                day = NumberUtils.parseInt2(dateStr.charAt(8), dateStr.charAt(9));
                hour = NumberUtils.parseInt2(dateStr.charAt(11), dateStr.charAt(12));
                minute = NumberUtils.parseInt2(dateStr.charAt(14), dateStr.charAt(15));
                second = NumberUtils.parseInt2(dateStr.charAt(17), dateStr.charAt(18));
            } else if (length == 10) {
                year = NumberUtils.parseInt4(dateStr.charAt(0), dateStr.charAt(1), dateStr.charAt(2), dateStr.charAt(3));
                month = NumberUtils.parseInt2(dateStr.charAt(5), dateStr.charAt(6));
                day = NumberUtils.parseInt2(dateStr.charAt(8), dateStr.charAt(9));
            } else {
                throw new UnsupportedOperationException(" Date Format Error, only supported 'yyyy-MM-dd' or 'yyyy-MM-dd HH:mm:ss'");
            }
            return new GeneralDate(year, month, day, hour, minute, second, 0, timeZone);
        } catch (Throwable throwable) {
            throw new UnsupportedOperationException("Date Format Error, default parse only supported 'yyyy-MM-dd' or 'yyyy-MM-dd HH:mm:ss'");
        }
    }

    public final static GeneralDate parseGeneralDate_Standard_19(char[] buf, int offset, TimeZone timeZone) {
        int year, month, day, hour = 0, minute = 0, second = 0;
        try {
            year = NumberUtils.parseInt4(buf[offset], buf[offset + 1], buf[offset + 2], buf[offset + 3]);
            month = NumberUtils.parseInt2(buf[offset + 5], buf[offset + 6]);
            day = NumberUtils.parseInt2(buf[offset + 8], buf[offset + 9]);
            hour = NumberUtils.parseInt2(buf[offset + 11], buf[offset + 12]);
            minute = NumberUtils.parseInt2(buf[offset + 14], buf[offset + 15]);
            second = NumberUtils.parseInt2(buf[offset + 17], buf[offset + 18]);
            return new GeneralDate(year, month, day, hour, minute, second, 0, timeZone);
        } catch (Throwable throwable) {
            throw new UnsupportedOperationException("Date Format Error, parseGeneralDate_Standard_19 only supported 'yyyy?MM?dd?HH:mm:ss'");
        }
    }

    public final static GeneralDate parseGeneralDate_Standard_10(char[] buf, int offset, TimeZone timeZone) {
        int year, month, day, hour = 0, minute = 0, second = 0;
        try {
            year = NumberUtils.parseInt4(buf[offset], buf[offset + 1], buf[offset + 2], buf[offset + 3]);
            month = NumberUtils.parseInt2(buf[offset + 5], buf[offset + 6]);
            day = NumberUtils.parseInt2(buf[offset + 8], buf[offset + 9]);
            return new GeneralDate(year, month, day, hour, minute, second, 0, timeZone);
        } catch (Throwable throwable) {
            throw new UnsupportedOperationException("Date Format Error, parseGeneralDate_Standard_19 only supported 'yyyy?MM?dd?HH:mm:ss'");
        }
    }

    public static TimeZone getDefaultTimeZone() {
        if (defaultTimeZoneField != null) {
            try {
                TimeZone defaultTimezone = (TimeZone) defaultTimeZoneField.get(null);
                if(defaultTimezone != null) {
                    return defaultTimezone;
                }
            } catch (IllegalAccessException exception) {
            }
        }
        return TimeZone.getDefault();
    }

    // 溢出处理不必考虑固定量值如日（24h）时（60m）分（60s）秒（1000ms）毫秒（只要给定了值就可直接换算为固定毫秒数）</p>
    // todo 是否存在因为过量溢出导致年份变化（是否闰年）带来的数据误差？
    private void overflow() {
        if (month > 12) {
            int increaseYear = (month - 1) / 12;
            year += increaseYear;
            month = month - increaseYear * 12;
        } else if (month < 1) {
            int increaseYear = month / 12 - 1;
            year += increaseYear;
            month = month - increaseYear * 12;
        }
    }

    private static boolean isLeapYear(int year) {
        boolean remainder4 = (year & 3) == 0;
        return year > 1582 ? remainder4 && (year % 100 != 0 || year % 400 == 0) : remainder4;
    }

    private static int getOffsetDays(int year) {
        if (year > 1582) {
            return (year - 1) * 365 + (year - 1) / 4 - (year - 1) / 100 + (year - 1) / 400 + 2;
        }
        return (year - 1) * 365 + (year - 1) / 4;
    }

    protected void updateTime() {
        // 溢出处理
        overflow();
        YearMeta meta = getYearMeta(year);
        boolean isLeapYear = meta.leap;
//        // 是否闰年
//        boolean isLeapYear = isLeapYear(year);
//        if (year == 3200) {
//            isLeapYear = false;
//        }

        // 0001.1.1~{year}.1.1的天数
        long days = meta.offsetDays;
        int offset = isLeapYear ? DaysOfLeapYearOffset[month - 1] : DaysOfYearOffset[month - 1];
        int daysOfYear = offset + dayOfMonth;

        // compute days
//        if (year > 1582) {
//            days = (year - 1) * 365 + (year - 1) / 4 - (year - 1) / 100 + (year - 1) / 400 + 2;
//        } else {
//            days = (year - 1) * 365 + (year - 1) / 4;
//        }
        // 1582年只有355天 此年10月5日～10月14日不存在
        if (year == 1582 && month > 9) {
            if (month == 10) {
                if (dayOfMonth > 14) {
                    daysOfYear -= 10;
                } else if (dayOfMonth > 4 && dayOfMonth <= 14) {
                    // 不存在的10天，按java日历的标准 +10
                    dayOfMonth += 10;
                }
            } else {
                daysOfYear -= 10;
            }
        }
        days += daysOfYear;
        long seconds = days * 86400 + hourOfDay * 3600 + minute * 60 + second;
        // 转化ms
        this.timeMills = seconds * 1000 + millisecond - this.currentOffset - RELATIVE_MILLS;

        // 当前时区（raw）下标准毫秒数（距离1970.1.1)
        this.standardMills = this.timeMills;

        // Asia/* 时区下区间offset调整
        if (seconds <= Seconds_1991_09_14_23_59_59 && seconds > Seconds_1900_01_01_07_59_59) {
            // 时区调整（Asia/* 夏令时调整）
            int actualOffset = timeZone.getOffset(this.timeMills);
            this.timeMills += this.currentOffset - actualOffset;
        }

        // 是否闰年
        this.leapYear = isLeapYear;
        // 一年第多少天
        this.daysOfYear = daysOfYear;
        // 公元元年（0001）.1.1 ~ current 相对天数
        this.currentDays = days;
    }

    public final static long getDefaultOffset() {
        return getDefaultTimeZone().getRawOffset();
    }

    private static YearMeta createYearMeta(int year) {
        boolean leap = isLeapYear(year);
        // 为什么-1？ 因为从0001-01-01开始，days已经代表了当年的1月1日
        long offsetDays = getOffsetDays(year) - 1;
        return new YearMeta(year, leap, offsetDays);
    }

    private static YearMeta getYearMeta(int year) {
        if (year > -1 && year < Positive_Year_Metas.length) {
            return Positive_Year_Metas[year];
        }
        return createYearMeta(year);
    }

    public final int getYear() {
        return year;
    }

    public final int getMonth() {
        return month;
    }

    public final int getDay() {
        return dayOfMonth;
    }

    public final int getHourOfDay() {
        return hourOfDay;
    }

    public final int getMinute() {
        return minute;
    }

    public final int getSecond() {
        return second;
    }

    public final int getMillisecond() {
        return millisecond;
    }

    public long getTime() {
        return timeMills;
    }

    public long getStandardTime() {
        return standardMills;
    }

    public int getCurrentOffset() {
        return currentOffset;
    }
}