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
    // 最小时间：公元元年时间（0001-01-01 00:00:00.000）
//    public static final GeneralDate MIN_DATE = GeneralDate.of(1, 1, 1, 0, 0, 0, 0);

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

    // 地球公转一周年毫秒数(计算24节气)
    // 计算来源： 36524219 * 24 * 36
    public final static long YEAR_TIMEMILLS = 31556925216l;

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

    // 虽然是默认时钟，但默认时钟也可以被修改，这里初始化一个时钟，在没有时钟信息时会使用默认时钟
    private static TimeZone defaultTimeZone;
    // 获取实时的默认时钟
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

    protected static final long Time_1900_01_01_08_05_43 = -2208988800000l;
    protected static final long Time_1991_09_15_00_00_00 = 684864000000l;

    protected static final YearMeta[] Positive_Year_Metas = new YearMeta[2100];
    protected static final long MaxCacheOffsetDays;
    // TODO 公元元年之前
    protected static final YearMeta[] Negative_Year_Metas = new YearMeta[2100];
    protected static final MonthDayMeta[] Month_Day_Of_Year = new MonthDayMeta[365];
    protected static final MonthDayMeta[] Month_Day_Of_LeapYear = new MonthDayMeta[366];

    static final int OFFSET_DAYS_DIVISOR = 146097;

    public GeneralDate(TimeZone timeZone) {
        ofTimeZone(timeZone);
    }

    public GeneralDate(long time) {
        this(time, defaultTimeZone);
    }

    public GeneralDate(long time, TimeZone timeZone) {
        ofTimeZone(timeZone);
        setTime(time, true);
    }

    public GeneralDate(long time, String timeZone) {
        ofTimeZone(timeZone == null ? defaultTimeZone : getTimeZoneById(timeZone));
        setTime(time, true);
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

    /**
     * 通过 TimeZone对象设置时差
     * 时间戳不变，重置各个系数
     *
     * @param timeZone
     * @return
     */
    public GeneralDate setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        int rawOffset = timeZone.getRawOffset();
        if (this.currentOffset != rawOffset) {
            this.currentOffset = rawOffset;
            setTime(this.timeMills, true);
        }
        return this;
    }

    /**
     * 通过表达式设置GMT时钟
     *
     * @param offsetExpr +-{hour}:{minute}?
     *                   Z
     * @return
     */
    public GeneralDate setTimeZone(String offsetExpr) {
        return setTimeZone(getTimeZoneById(offsetExpr));
    }

    TimeZone getTimeZoneById(String offsetExpr) {
        if (offsetExpr.startsWith("GMT")) {
            return TimeZone.getTimeZone(offsetExpr);
        }
        return TimeZone.getTimeZone("GMT" + offsetExpr);
    }

    protected void ofTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        if (timeZone == null) {
            this.timeZone = getDefaultTimeZone();
        }
        this.currentOffset = this.timeZone.getRawOffset();
    }

    static class YearMeta {
        final int year;
        final boolean leap;
        final long offsetDays;

        YearMeta(int year, boolean leap, long offsetDays) {
            this.year = year;
            this.leap = leap;
            this.offsetDays = offsetDays;
        }
    }

    static class MonthDayMeta {
        final int month;
        final int day;

        public MonthDayMeta(int month, int day) {
            this.month = month;
            this.day = day;
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
        getDefaultTimeZone();
        for (int year = 0; year < Positive_Year_Metas.length; year++) {
            Positive_Year_Metas[year] = createYearMeta(year);
        }
        MaxCacheOffsetDays = Positive_Year_Metas[Positive_Year_Metas.length - 1].offsetDays;

        int monthOfYear = 1;
        int monthOfLeapYear = 1;
        int daysOfYearOffset = DaysOfYearOffset[monthOfYear - 1];
        int daysOfLeapYearOffset = DaysOfLeapYearOffset[monthOfLeapYear - 1];
        for (int i = 0; i < 366; i++) {
            if (i < 365) {
                int dayOfMonthAtYear = i - daysOfYearOffset + 1;
                // 4位（0-11） + 5位（0-30）
                Month_Day_Of_Year[i] = new MonthDayMeta(monthOfYear, dayOfMonthAtYear);
                if (monthOfYear < 12 && i == DaysOfYearOffset[monthOfYear] - 1) {
                    daysOfYearOffset = DaysOfYearOffset[monthOfYear++];
                }
            }
            int dayOfMonthAtLeapYear = i - daysOfLeapYearOffset + 1;
            Month_Day_Of_LeapYear[i] = new MonthDayMeta(monthOfLeapYear, dayOfMonthAtLeapYear);
            if (monthOfLeapYear < 12 && i == DaysOfLeapYearOffset[monthOfLeapYear] - 1) {
                daysOfLeapYearOffset = DaysOfLeapYearOffset[monthOfLeapYear++];
            }
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

    public final static GeneralDate parseGeneralDate_Standard_19(byte[] buf, int offset, TimeZone timeZone) {
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
            throw new UnsupportedOperationException("Date Format Error, parseGeneralDate_Standard_10 only supported 'yyyy?MM?dd'");
        }
    }

    public static TimeZone getDefaultTimeZone() {
        if (defaultTimeZoneField != null) {
            try {
                TimeZone defaultTimezone = (TimeZone) defaultTimeZoneField.get(null);
                if (defaultTimezone != null) {
                    return defaultTimeZone = defaultTimezone;
                }
            } catch (IllegalAccessException exception) {
            }
        }
        return defaultTimeZone = TimeZone.getDefault();
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

    private static long getOffsetDays(long year) {
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

    void setTime(long timeMills, boolean reset) {
        this.timeMills = timeMills;
        if (timeMills >= Time_1900_01_01_08_05_43 && timeMills < Time_1991_09_15_00_00_00) {
            // actual mills for compute
            int zoneOffset = timeZone.getOffset(timeMills);
            timeMills -= this.currentOffset - zoneOffset;
            this.standardMills = timeMills;
        }
        long offset = RELATIVE_MILLS + currentOffset;
        timeMills += offset;
        long seconds;
        int millisecond;
        int offsetYear = 0;
        if (timeMills > 0) {
            seconds = timeMills / 1000;
            millisecond = (int) (timeMills - seconds * 1000);
        } else {
            if (this.timeMills > Long.MAX_VALUE - offset) {
                // overflowing the maximum value of long
                int rem = (int) (this.timeMills % 1000);
                seconds = this.timeMills / 1000 + (offset + rem) / 1000;
                millisecond = (int) (rem + (offset % 1000)) % 1000;
            } else {
                // 如果是公元元年之前先计算年份，然后将timeMills补齐到正数在进行计算
                // 这里只做了初略的转正处理，确保time段(时分秒)解析正确
                // todo 公元元年之前的时间可以使用码表处理，或者可以使用通用码表
                do {
                    boolean leap = ((offsetYear + 1) & 3) == 0;
                    timeMills += leap ? MILLS_366_DAY : MILLS_365_DAY;
                    timeMills += MILLS_DAY;
                    offsetYear++;
                } while (timeMills < 0);
                seconds = timeMills / 1000;
                millisecond = (int) (timeMills - seconds * 1000);
            }
        }

        long days = seconds / 86400l;
        int secondsOfDay = (int) (seconds - days * 86400l);
        int hour = secondsOfDay / 3600;
        secondsOfDay = secondsOfDay - hour * 3600;
        int minute = secondsOfDay / 60;
        int second = secondsOfDay - minute * 60;

        int daysOfYear;
        boolean isLeapYear;
//        int year = (int) ((days + 1) * 100000 / 36524219) + 1;
        int year = (int) (days * 400) / OFFSET_DAYS_DIVISOR + 1;
        GeneralDate.YearMeta targetMeta = getYearMeta(year);
        long offsetDays = targetMeta.offsetDays;
        if (days <= offsetDays) {
            targetMeta = getYearMeta(--year);
            offsetDays = targetMeta.offsetDays;
        }
        daysOfYear = (int) (days - offsetDays);
        isLeapYear = targetMeta.leap;
        MonthDayMeta monthDayMeta;
        try {
            int dayIndex = daysOfYear - 1;
            if(isLeapYear) {
                if(daysOfYear > Month_Day_Of_LeapYear.length) {
                    daysOfYear -= 366;
                    dayIndex -= 366;
                    ++year;
                    isLeapYear = false;
                }
                monthDayMeta = Month_Day_Of_LeapYear[dayIndex];
            } else {
                if(daysOfYear > Month_Day_Of_Year.length) {
                    daysOfYear -= 365;
                    dayIndex -= 365;
                    ++year;
                    isLeapYear = isLeapYear(year);
                }
                monthDayMeta = Month_Day_Of_Year[dayIndex];
            }
            // monthDayMeta = isLeapYear ? Month_Day_Of_LeapYear[daysOfYear - 1] : Month_Day_Of_Year[daysOfYear - 1];
        } catch (Throwable throwable) {
            throw new IllegalArgumentException("error for " + this.timeMills);
        }
        int month = monthDayMeta.month;
        int day = monthDayMeta.day;

        // 1582年共355天 不存在的10天(1582.10.05~1582.10.14 对应278～287) 288～355
        if (year == 1582 && daysOfYear >= 278) {
            //  daysOfYear 必然 <= 355 ，12月通过算法得到day最大21
            day += 10;
            // 10～11月溢出处理
            if (month == 10 && day > 31) {
                month += 1;
                day = day - 31;
            } else if (month == 11 && day > 30) {
                month += 1;
                day = day - 30;
            }
        }

        this.year = year - offsetYear;
        this.month = month;
        this.dayOfMonth = day;
        this.hourOfDay = hour;
        this.minute = minute;
        this.second = second;
        this.millisecond = millisecond;

        this.daysOfYear = daysOfYear;
        this.leapYear = isLeapYear;
        this.currentDays = days;
    }

    public final static long getDefaultOffset() {
        return getDefaultTimeZone().getRawOffset();
    }

    private static YearMeta createYearMeta(int year) {
        boolean leap = isLeapYear(year);
        // starting from 0001-01-01, days have already represented January 1st of that year
        long offsetDays = getOffsetDays(year) - 1;
        return new YearMeta(year, leap, offsetDays);
    }

    static YearMeta getYearMeta(int year) {
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

    /**
     * 是否上午
     *
     * @return
     */
    public boolean isAm() {
        return this.hourOfDay < 12;
    }
}
