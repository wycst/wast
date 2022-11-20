/*
 * Copyright [2020-2022] [wangyunchao]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.github.wycst.wast.common.beans;

import java.util.TimeZone;

/**
 * <pre>
 * - 关于闰年算法
 * 闰年只是为了修正平年（365天）和回归年（太阳转一圈）误差，没有绝对的算法。
 * 地球公转一周 365.24219天（回归年）
 * 什么时候该闰年？ 平年365天，闰年366天
 * 每4年实际天数： 365.24219 * 4 = 365 * 4 + 0.96876，即4个平年少算：0.96876天，所以把这差不多一天少算时间放在第四年作为闰年
 *
 * - 为什么被100整除但不被400整除的年不是闰年原因？
 * 假定0004年是闰年，多算0.03124天（0.03124 = 1 - 0.96876，把实际不够一天的时间当一天算，等于借了未来时间）
 * 按四年一闰则每个闰周期多算0.03124天，经过25个周期（假定100年是闰年）则会多算 25 * 0.03124 = 0.781天（借未来时间）,估算差不多1天
 * 此时将100年定为平年来抵消（除去本来应该定为闰年的一天还上未来时间0.781天，还剩下0.219天），所以100年变成平年了（排除早期闰年算法错误原因）。
 *
 * - 为什么被400整除的又是闰年？
 * 按100年是平年则每100年多出来0.219天（上文剩下来的），经过4个周期（400年），则会多出来0.876天，估算差不多一天，所以又将400年定为闰年。
 * 其实500年误差似乎更小一些，但500年会超过一天，不利于后面的周期运算。
 *
 * - 3200年是否为闰年？
 * 按400年一闰法则来计算，每400年向未来借0.124天（0.124 = 1 - 0.876）经过8个周期则会借0.992天，则把3200年定为平年能正好抵消（还完还剩下0.008天）
 * 所以理论上3200是平年。
 * </pre>
 *
 * <p>
 * 以3200年为一个周期（按3200年为平年）（每个周期剩下0.008天，则经过125个周期后即40万年，整整剩出1天(0.008 * 125)
 * 如按此周期年算，40万年又是闰年 ？？
 * 至此按365.24219的精度来算误差已经没了，即40万年可能是一个完整的闰年周期。
 *
 * <p>注： 目前支持公元元年开始以后的日期（0001-01-01 00:00:00.000+)
 *
 * @author wangyunchao
 * @see java.util.Calendar
 */
public class Date extends GeneralDate implements java.io.Serializable, Comparable<Date> {

    // 最小时间：公元元年时间（0001-01-01 00:00:00.000）
    public static final GeneralDate MIN_DATE = GeneralDate.of(1, 1, 1, 0, 0, 0, 0);

    // 星期
    protected int dayOfWeek;
    // 当月第几个星期
    protected int weekOfMonth;
    // 当年第几个星期
    protected int weekOfYear;

//    // 以下2个属性后续放在LunarDate中
//    // 以2019年24节气中时刻（毫秒数）作为参考
//    // 从小寒开始，冬至结束
//    public final static long[] SOLAR_TERMS_2019 = new long[24];

    // 闰年算法： 0~1582之前按每4年一润  1582-？ 按最新算法
    // 删除1582年 10月5日至14日共 10天

    public Date() {
        this(System.currentTimeMillis());
    }

    public Date(long timeMills) {
        this(timeMills, null);
    }

    public Date(TimeZone timeZone) {
        this(System.currentTimeMillis(), timeZone);
    }

    public Date(long timeMills, TimeZone timeZone) {
        super(timeZone);
        setTime(timeMills);
    }

    public Date(int year, int month, int day) {
        this(year, month, day, 0, 0, 0, 0, (TimeZone) null);
    }

    public Date(int year, int month, int day, int hour, int minute, int second,
                int millsecond) {
        this(year, month, day, hour, minute, second, millsecond, (TimeZone) null);
    }

    public Date(int year, int month, int day, int hour, int minute, int second,
                int millisecond, TimeZone timeZone) {
        super(timeZone);
        this.set(year, month, day, hour, minute, second, millisecond, timeZone);
    }

    /**
     * 支持格式： {'yyyy-MM-dd', 'yyyy-MM-dd HH:mm:ss'}
     * 年月日必须
     *
     * @param dateStr
     * @return
     */
    public static Date parse(String dateStr) {
        GeneralDate generalDate = parseGeneralDate(dateStr, null);
        return new Date(generalDate.year, generalDate.month, generalDate.dayOfMonth, generalDate.hourOfDay, generalDate.minute, generalDate.second, 0);
    }

    /**
     * 以指定模板解析日期
     * 和format方法逆向处理
     *
     * @param dateStr
     * @param template
     * @return
     * @see Date#format(String)
     */
    public static Date parse(String dateStr, String template) {
        if (template == null)
            return parse(dateStr);
        char[] dateBuf = dateStr.toCharArray();
        return parse(dateBuf, 0, dateBuf.length, template);
    }

    /**
     * 提取日期字符转化为日期对象
     *
     * @param buf
     * @param offset
     * @param len
     * @param template
     * @return
     */
    public static Date parse(char[] buf, int offset, int len, String template) {
        DateTemplate dateTemplate = new DateTemplate(template);
        return parse(buf, offset, len, dateTemplate);
    }

    public static Date parse(char[] buf, int offset, int len, DateTemplate dateTemplate) {
        dateTemplate.getClass();
        return dateTemplate.parse(buf, offset, len);
    }

    /**
     * 以当前时间为轴左右
     *
     * @param type
     * @param count
     */
    public Date add(int type, int count) {
        switch (type) {
            case YEAR:
                this.year += count;
                this.updateTime();
                break;
            case MONTH:
                this.month += count;
                this.updateTime();
                break;
            case DAY_OF_MONTH:
                // 如果是添加的天数
                long timeMills = this.timeMills + count * 24l * 3600 * 1000;
                setTime(timeMills);
                break;
            default:
                break;
        }
        return this;
    }

    public long interval(Date target) {
        return target.getTime() - this.timeMills;
    }

    public long intervalDays(Date target) {
        return (target.getTime() - this.timeMills) / 86400000l;
    }

    public long intervalHours(Date target) {
        return (target.getTime() - this.timeMills) / 3600000l;
    }

    public Date set(int year, int month, int day) {
        return this.set(year, month, day, hourOfDay, minute, second, millisecond, null);
    }

    public Date set(int year, int month, int day, int hour, int minute, int second,
                    int millisecond) {
        return this.set(year, month, day, hour, minute, second, millisecond, null);
    }

    public Date set(int year, int month, int day, int hour, int minute, int second,
                    int millisecond, TimeZone timeZone) {
        ofTimeZone(timeZone);
        this.year = year;
        this.month = month;
        this.dayOfMonth = day;
        this.hourOfDay = hour;
        this.minute = minute;
        this.second = second;
        this.millisecond = millisecond;
        updateTime();
        return this;
    }

    protected void updateTime() {
        super.updateTime();
        int dayOfWeek = (int) ((RELATIVE_DAY_OF_WEEK + currentDays - RELATIVE_DAYS - 1) % 7 + 1);
        if (dayOfWeek <= 0) {
            dayOfWeek = dayOfWeek + 7;
        }
        // 星期
        this.dayOfWeek = dayOfWeek;
        // 当年第几个星期
        this.weekOfYear = daysOfYear % 7 == 0 ? daysOfYear / 7 : daysOfYear / 7 + 1;
        // 校验日期回设
        if (!validate()) {
            setTime(this.timeMills, true);
        }
        // 当月第几个星期
        this.weekOfMonth = dayOfMonth % 7 == 0 ? dayOfMonth / 7 : dayOfMonth / 7 + 1;
        afterDateChange();
    }

    protected void afterDateChange() {
    }

    private boolean validate() {
        // 去除year的校验(<1)，支持公元元年之前的日期
        if (/*this.year <= 0
                ||*/ this.month < 1 || this.month > 12
                || this.dayOfMonth < 1 || this.dayOfMonth > 31
                || this.hourOfDay < 0 || this.hourOfDay > 23
                || this.minute < 0 || this.minute > 59
                || this.second < 0 || this.second > 59
                || this.millisecond < 0 || this.millisecond > 999
        )
            return false;
        if (this.month == 4 || this.month == 6 || this.month == 9 || this.month == 11) {
            if (dayOfMonth == 31) return false;
        } else if (this.month == 2) {
            if (dayOfMonth > 29) return false;
            if (!leapYear && dayOfMonth == 29) return false;
        }
        return true;
    }

    public Date setYear(int year) {
        if (this.year == year) {
            return this;
        }
        this.year = year;
        this.updateTime();
        return this;
    }

    public Date setMonth(int month) {
        if (this.month == month)
            return this;
        this.month = month;
        updateTime();
        return this;
    }

    public Date setDay(int day) {
        if (this.dayOfMonth == day) return this;
        this.dayOfMonth = day;
        updateTime();
        return this;
    }

    public Date setHourOfDay(int hourOfDay) {
        if (this.hourOfDay == hourOfDay) return this;
        this.hourOfDay = hourOfDay;
        updateTime();
        return this;
    }

    public Date setMinute(int minute) {
        if (this.minute == minute) return this;
        this.minute = minute;
        updateTime();
        return this;
    }

    public Date setSecond(int second) {
        if (this.second == second) return this;
        this.second = second;
        updateTime();
        return this;
    }

    public Date setMillisecond(int millisecond) {
        if (this.millisecond == millisecond) return this;
        this.millisecond = millisecond;
        updateTime();
        return this;
    }

    public int getDaysOfYear() {
        return daysOfYear;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public boolean isLeapYear() {
        return leapYear;
    }

    public void setTime(long timeMills, boolean reset) {
        // 如果毫秒没有变化不计算（注意-1不做处理）
        if (this.timeMills == timeMills && !reset) {
            return;
        }
        super.setTime(timeMills, reset);

        // 以星期四（5）作为参考，星期六（7） 星期日（1）   -6~0   1-7
        int dayOfWeek = (int) ((RELATIVE_DAY_OF_WEEK + currentDays - RELATIVE_DAYS - 1) % 7 + 1);
        if (dayOfWeek <= 0) {
            dayOfWeek = dayOfWeek + 7;
        }
        this.dayOfWeek = dayOfWeek;
        this.weekOfMonth = dayOfMonth % 7 == 0 ? dayOfMonth / 7 : dayOfMonth / 7 + 1;
        this.weekOfYear = daysOfYear % 7 == 0 ? daysOfYear / 7 : daysOfYear / 7 + 1;

        this.afterDateChange();
    }

    public void setTime(long timeMills) {
        setTime(timeMills, false);
    }

    /**
     * 是否同一天（不考虑时辰）,需要加上时差
     *
     * @param sourceTimemills
     * @param targetTimeMills
     * @return
     */
    public static boolean isSameDay(long sourceTimemills, long targetTimeMills) {
        return (sourceTimemills + RELATIVE_MILLS + getDefaultOffset()) / 86400000 == (targetTimeMills + RELATIVE_MILLS + getDefaultOffset()) / 86400000;
    }

    @Override
    public String toString() {
        return format('-', ':');
    }

    public String toDateString() {
        return year + "-" + month + "-" + dayOfMonth + " " + hourOfDay + ":" + minute + ":" + second + "." + millisecond;
    }

    /**
     * 格式化
     *
     * @return
     */
    public String format() {
        return toString();
    }

    /**
     * 格式化
     *
     * @param dateSyntax 年月日分隔符 默认'-'
     * @param timeSyntax 时分秒分隔符 默认':'
     * @return
     */
    public String format(char dateSyntax, char timeSyntax) {
        StringBuilder buff = new StringBuilder(19);
        int year = this.year;
        if (year < 0) {
            buff.append("-");
            year = -year;
        }
        int y1 = year / 100;
        int y2 = year % 100;
        buff.append(DateTemplate.DigitTens[y1]);
        buff.append(DateTemplate.DigitOnes[y1]);
        buff.append(DateTemplate.DigitTens[y2]);
        buff.append(DateTemplate.DigitOnes[y2]);
        buff.append(dateSyntax);
        buff.append(DateTemplate.DigitTens[month]);
        buff.append(DateTemplate.DigitOnes[month]);
        buff.append(dateSyntax);
        buff.append(DateTemplate.DigitTens[dayOfMonth]);
        buff.append(DateTemplate.DigitOnes[dayOfMonth]);
        buff.append(' ');
        buff.append(DateTemplate.DigitTens[hourOfDay]);
        buff.append(DateTemplate.DigitOnes[hourOfDay]);
        buff.append(timeSyntax);
        buff.append(DateTemplate.DigitTens[minute]);
        buff.append(DateTemplate.DigitOnes[minute]);
        buff.append(timeSyntax);
        buff.append(DateTemplate.DigitTens[second]);
        buff.append(DateTemplate.DigitOnes[second]);
        return buff.toString();
    }

    /**
     * <p> Y 4位数年份
     * <p> y 2位年份
     * <p> M 格式化2位月份
     * <p> d 格式化2位天
     * <p> H 格式化24制小时数
     * <p> m 格式化2位分钟
     * <p> s 格式化2位秒
     * <p> S 格式化3位毫秒
     * <p> a 上午/下午
     *
     * @param template
     * @return
     */
    public String format(String template) {
        if (template == null) return format();
        StringBuilder writer = new StringBuilder();
        formatTo(template, writer);
        return writer.toString();
    }

    public void formatTo(String template, Appendable appendable) {
        DateTemplate.formatTo(year, month, dayOfMonth, hourOfDay, minute, second, millisecond, dayOfWeek, daysOfYear, weekOfMonth, weekOfYear, timeZone, template, appendable);
    }

    public void formatTo(String template, Appendable appendable, boolean escapeQuot) {
        DateTemplate.formatTo(year, month, dayOfMonth, hourOfDay, minute, second, millisecond, dayOfWeek, daysOfYear, weekOfMonth, weekOfYear, timeZone, template, appendable, escapeQuot);
    }

    public int compareTo(Date o) {
        if (timeMills == o.timeMills) {
            return 0;
        }
        return timeMills > o.timeMills ? 1 : -1;
    }

    public static void main(String[] args) {
        System.out.println(new Date().format("YYYY/~MM/pdd HH:mm:ss"));
        // Time_1991_09_15_00_00_00
        System.out.println(new Date(2199, 12, 31, 0, 0, 0, 0).timeMills);
        GeneralDate date = null;
        long l1 = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            date = new GeneralDate(l1, (TimeZone) null);
        }
        long l2 = System.currentTimeMillis();
        System.out.println(l2 - l1);
        System.out.println(date);
        System.out.println(new Date().format("YYYY/MM/dd HH:mm:ss"));
    }
}
