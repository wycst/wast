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
 * @author wangyunchao
 */
public class Date implements java.io.Serializable, Comparable<Date> {

    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int DAY_OF_MONTH = 3;
    public static final int HOURS = 4;
    public static final int MINUTE = 5;
    public static final int SECOND = 6;

    protected int year;
    protected int month;
    protected int dayOfMonth;

    protected int hourOfDay;
    protected int minute;
    protected int second;
    protected int millisecond;

    // 当年第多少天
    protected int daysOfYear;
    // 星期
    protected int dayOfWeek;
    // 是否闰年
    protected boolean leapYear;
    // 距离1970.1.1 毫秒数
    protected long timeMills = -1;
    // 公元元年（0001）.1.1 ~ 1970.1.1  相对毫秒数
    public final static long RELATIVE_MILLS;
    // 公元元年（0001）.1.1 ~ 1970.1.1 相对天数
    public final static long RELATIVE_DAYS;
    // 以1970.1.1  周四 作为参考
    public final static int RELATIVE_DAY_OF_WEEK = 5;
    // 默认时差
    public static final int DEFAULT_OFFSET;
    // 可变时差
    protected int currentOffset;

    // 以下2个属性后续放在LightDate中
    // 以2019年24节气中时刻（毫秒数）作为参考
    // 从小寒开始，冬至结束
    public final static long[] SOLAR_TERMS_2019 = new long[24];
    // 地球公转一周年毫秒数
    public final static long YEAR_TIMEMILLS = 31556925216l;

    // 闰年算法： 0~1582之前按每4年一润  1582-？ 按最新算法
    // 删除1582年 10月5日至14日共 10天
    static {
        DEFAULT_OFFSET = TimeZone.getDefault().getRawOffset();
        // 计算 1-1-1 00:00:00 ~ 1970-1-1 00:00:00 之间得 relativeMills
        // 1582年历法按4年一润，当年删去了10天 ，1582-？ 按最新历法
        // 范围： 0001~1969（时间戳从1970年开始）
        RELATIVE_DAYS = 1969 * 365 + 1969 / 4 - 1969 / 100 + 1969 / 400 + (1582 / 100 - 1582 / 400 - 10);
        RELATIVE_MILLS = RELATIVE_DAYS * 24 * 3600 * 1000;
    }

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
        if (timeZone != null) {
            this.currentOffset = timeZone.getRawOffset();
        } else {
            this.currentOffset = DEFAULT_OFFSET;
        }
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
        dateStr.getClass();
        int length = dateStr.length();
        int year, month, day, hour = 0, minute = 0, second = 0;
        try {
            if (length == 10) {
                year = Integer.parseInt(dateStr.substring(0, 4));
                month = Integer.parseInt(dateStr.substring(5, 7));
                day = Integer.parseInt(dateStr.substring(8, 10));
            } else if (length == 19) {
                year = Integer.parseInt(dateStr.substring(0, 4));
                month = Integer.parseInt(dateStr.substring(5, 7));
                day = Integer.parseInt(dateStr.substring(8, 10));
                hour = Integer.parseInt(dateStr.substring(11, 13));
                minute = Integer.parseInt(dateStr.substring(14, 16));
                second = Integer.parseInt(dateStr.substring(17, 19));
            } else {
                throw new UnsupportedOperationException(" Date Format Error, only supported 'yyyy-MM-dd' or 'yyyy-MM-dd HH:mm:ss'");
            }
            return new Date(year, month, day, hour, minute, second, 0);
        } catch (Throwable throwable) {
            throw new UnsupportedOperationException("Date Format Error, default parse only supported 'yyyy-MM-dd' or 'yyyy-MM-dd HH:mm:ss'");
        }
    }

    /**
     * 以指定模板解析日期
     * 和format方法逆向处理
     *
     * @param dateStr
     * @param template
     * @return
     */
    public static Date parse(String dateStr, String template) {
        if (template == null)
            return parse(dateStr);

        // 18:03 12/01/2021  ->H:m M/d/Y
        // 解析模板中每个指标（年月日时分秒）位置得索引
        int yearIndex = -1, fullYearIndex = -1, monthIndex = -1, dayIndex = -1, hourIndex = -1, minuteIndex = -1, secondIndex = -1, millisecondIndex = -1;
        int year, month, day, hour = 0, minute = 0, second = 0, millisecond = 0;

        int length = template.length();
        int count = 0;
        for (int i = 0; i < length; i++) {
            char ch = template.charAt(i);
            switch (ch) {
                case 'Y':
                    fullYearIndex = i + count;
                    count += 3;
                    continue;
                case 'y':
                    yearIndex = i + count;
                    count++;
                    continue;
                case 'M':
                    monthIndex = i + count;
                    count++;
                    continue;
                case 'd':
                    dayIndex = i + count;
                    count++;
                    continue;
                case 'H':
                    hourIndex = i + count;
                    count++;
                    continue;
                case 'm':
                    minuteIndex = i + count;
                    count++;
                    continue;
                case 's':
                    secondIndex = i + count;
                    count++;
                    continue;
                case 'S':
                    millisecondIndex = i + count;
                    count += 2;
                    continue;
                default:
//                    count++;
                    continue;
            }
        }

        if (fullYearIndex > -1) {
            year = Integer.parseInt(dateStr.substring(fullYearIndex, fullYearIndex + 4));
        } else if (yearIndex > -1) {
            // 年份
            year = Integer.parseInt(dateStr.substring(yearIndex, yearIndex + 2));
            year += new Date().getYear() / 100 * 100;
        } else {
            throw new UnsupportedOperationException(" Date Format Error, 'Y' or 'y' for year is not found ");
        }
        if (monthIndex > -1) {
            month = Integer.parseInt(dateStr.substring(monthIndex, monthIndex + 2));
        } else {
            throw new UnsupportedOperationException(" Date Format Error, 'M' for month is not found ");
        }
        if (dayIndex > -1) {
            day = Integer.parseInt(dateStr.substring(dayIndex, dayIndex + 2));
        } else {
            throw new UnsupportedOperationException(" Date Format Error, 'd' for day is not found ");
        }

        if (hourIndex > -1) {
            hour = Integer.parseInt(dateStr.substring(hourIndex, hourIndex + 2));
        }
        if (minuteIndex > -1) {
            minute = Integer.parseInt(dateStr.substring(minuteIndex, minuteIndex + 2));
        }
        if (secondIndex > -1) {
            second = Integer.parseInt(dateStr.substring(secondIndex, secondIndex + 2));
        }
        if (millisecondIndex > -1) {
            millisecond = Integer.parseInt(dateStr.substring(millisecondIndex, millisecondIndex + 2));
        }

        return new Date(year, month, day, hour, minute, second, millisecond);
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

    /**
     * 通过 TimeZone对象设置时差
     * 时间戳不变，重置各个系数
     *
     * @param timeZone
     * @return
     */
    public Date setTimeZone(TimeZone timeZone) {
        int rawOffset = timeZone.getRawOffset();
        if (this.currentOffset != rawOffset) {
            this.currentOffset = rawOffset;
            setTime(this.timeMills, true);
        }
        return this;
    }

    /**
     * 通过表达式设置
     * 时间戳不变，重置各个系数
     *
     * @param offsetExpr +-{hour}:{minute}?
     *                   Z
     * @return
     */
    public Date setTimeZone(String offsetExpr) {
        if (offsetExpr.startsWith("GMT")) {
            return setTimeZone(TimeZone.getTimeZone(offsetExpr));
        }
        return setTimeZone(TimeZone.getTimeZone("GMT" + offsetExpr));
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
        if (timeZone != null) {
            this.currentOffset = timeZone.getRawOffset();
        } else {
            this.currentOffset = DEFAULT_OFFSET;
        }
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

    private void updateTime() {
        // 逆行转换时溢出处理(暂时只针对月份)
        doOverflowConvert();
        // 是否闰年
        boolean remainder4 = (year - (year >> 2 << 2)) == 0;
        boolean isLeapYear = year > 1582 ? remainder4 && (year % 100 != 0 || year % 400 == 0) : remainder4;
        if (year == 3200) {
            isLeapYear = false;
        }

        // 0001.1.1~{year}.1.1的天数
        long days = 0;
        // 获取天数
        if (year > 1582) {
            days = (year - 1) * 365 + (year - 1) / 4 - (year - 1) / 100 + (year - 1) / 400 + 2;
        } else {
            days = (year - 1) * 365 + (year - 1) / 4;
        }
        int daysOfYear = 0; // 1582年只有355天 此年10月5日～10月14日不存在
        if (month == 1) {
            daysOfYear = dayOfMonth;
        } else if (month == 2) {
            daysOfYear = 31 + dayOfMonth;
        } else if (month == 3) {
            daysOfYear = (isLeapYear ? 60 : 59) + dayOfMonth;
        } else if (month == 4) {
            daysOfYear = (isLeapYear ? 91 : 90) + dayOfMonth;
        } else if (month == 5) {
            daysOfYear = (isLeapYear ? 121 : 120) + dayOfMonth;
        } else if (month == 6) {
            daysOfYear = (isLeapYear ? 152 : 151) + dayOfMonth;
        } else if (month == 7) {
            daysOfYear = (isLeapYear ? 182 : 181) + dayOfMonth;
        } else if (month == 8) {
            daysOfYear = (isLeapYear ? 213 : 212) + dayOfMonth;
        } else if (month == 9) {
            daysOfYear = (isLeapYear ? 244 : 243) + dayOfMonth;
        } else if (month == 10) {
            daysOfYear = (isLeapYear ? 274 : 273) + dayOfMonth;
            if (year == 1582) {
                if (dayOfMonth > 14) {
                    daysOfYear -= 10;
                } else if (dayOfMonth > 4 && dayOfMonth <= 14) {
                    // 不存在的10天，按java日历的标准 +10
                    dayOfMonth += 10;
                }
            }
        } else if (month == 11) {
            daysOfYear = (isLeapYear ? 305 : 304) + dayOfMonth;
            if (year == 1582) {
                daysOfYear -= 10;
            }
        } else if (month == 12) {
            daysOfYear = (isLeapYear ? 335 : 334) + dayOfMonth;
            if (year == 1582) {
                daysOfYear -= 10;
            }
        }
        // 加上当年得天数，为什么-1？ 因为days已经代表了当年的1月1日
        days += daysOfYear - 1;
        int dayOfWeek = (int) ((RELATIVE_DAY_OF_WEEK + days - RELATIVE_DAYS - 1) % 7 + 1);
        if (dayOfWeek <= 0) {
            dayOfWeek = dayOfWeek + 7;
        }
        long hours = days * 24 + hourOfDay;
        // 通过hours获取minutes
        long minutes = hours * 60 + minute;
        // 通过minutes获取seconds
        long seconds = minutes * 60 + second;
        // 一年第多少天
        this.daysOfYear = daysOfYear;
        // 星期
        this.dayOfWeek = dayOfWeek;
        // 当前时间毫秒数（距离1970.1.1)
        this.timeMills = seconds * 1000 + millisecond - this.currentOffset - RELATIVE_MILLS;
        // 是否闰年
        this.leapYear = isLeapYear;
        // 校验日期参数合法性进行回设
        if (!validate()) {
            setTime(this.timeMills, true);
        }
        afterDateChange();
    }

    protected void afterDateChange() {
    }

    // 溢出处理不必考虑固定量值如日（24h）时（60m）分（60s）秒（1000ms）毫秒（只要给定了值就可直接换算为固定毫秒数）</p>
    private void doOverflowConvert() {
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

    private boolean validate() {
        if (this.year <= 0
                || this.month < 1 || this.month > 12
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

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        if (this.year == year) {
            return;
        }
        this.year = year;
        this.updateTime();
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        if (this.month == month)
            return;
        this.month = month;
        updateTime();
    }

    public int getDay() {
        return dayOfMonth;
    }

    public void setDay(int day) {
        if (this.dayOfMonth == day) return;
        this.dayOfMonth = day;
        updateTime();
    }

    public int getHourOfDay() {
        return hourOfDay;
    }

    public void setHourOfDay(int hourOfDay) {
        if (this.hourOfDay == hourOfDay) return;
        this.hourOfDay = hourOfDay;
        updateTime();
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        if (this.minute == minute) return;
        this.minute = minute;
        updateTime();
    }

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        if (this.second == second) return;
        this.second = second;
        updateTime();
    }

    public int getMillisecond() {
        return millisecond;
    }

    public void setMillisecond(int millisecond) {
        if (this.millisecond == millisecond) return;
        this.millisecond = millisecond;
        updateTime();
    }

    public int getDaysOfYear() {
        return daysOfYear;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public long getTime() {
        return timeMills;
    }

    public boolean isLeapYear() {
        return leapYear;
    }

    public void setTime(long timeMills, boolean reset) {

        // 如果毫秒没有变化不计算（注意-1不做处理）
        if (this.timeMills == timeMills && !reset) {
            return;
        }
        this.timeMills = timeMills;
        // 补上毫秒差
        timeMills += RELATIVE_MILLS + currentOffset;
        long seconds = timeMills / 1000;
        // 毫秒
        int millisecond = (int) (timeMills - seconds * 1000);
        // 分钟数
        long minutes = seconds / 60;
        // 秒
        int second = (int) (seconds - minutes * 60);
        // 所有的小时数hours
        long hours = minutes / 60;
        // 分
        int minute = (int) (minutes - hours * 60);
        // 天数 (距离0001.1.1的天数）
        long days = hours / 24;
        // 时 (time / 1000 / 60 / 60 + 8) % 24
        int hourOfDay = (int) (hours - days * 24);
        int leafYearCount = 0;
        // 先估算一个年份 ，因为年份从0001.1.1开始算起，所以后面+1
        int year = (int) (days / 365) + 1;
        int deleteDays = 0;
        // 577815~578180 以4年一润初始化闰年天数（1582年分界点）
        leafYearCount = (year - 1) / 4;
        if (days >= 577815) {
            // 1583.1.1 年以后
            deleteDays = 10;
            if (days >= 578180) {
                // 闰年个数，完整公式：  (year - 1) / 4 - (year - 1)/100 + (year - 1)/400 +（1582 / 100 - 1582 / 400）
                leafYearCount = (year - 1) / 4 - (year - 1) / 100 + (year - 1) / 400 + 12;
            }
        }
        // 以星期四（5）作为参考，星期六（7） 星期日（1）   -6~0   1-7
        int dayOfWeek = (int) ((RELATIVE_DAY_OF_WEEK + days - RELATIVE_DAYS - 1) % 7 + 1);
        if (dayOfWeek <= 0) {
            dayOfWeek = dayOfWeek + 7;
        }
        // 如果已知除数情况，减法&乘法比再使用%求余性能稍微块点
        int remainder = (int) (days - (year - 1) * 365);
        // 当年第多少天 = 余数 + 1 - 闰年数 + （删除的天数）
        int daysOfYear = remainder + 1 - leafYearCount + deleteDays;
        Boolean isLeapYear = null;

        // 以上while处理在daysOfYear很小很小（比如80万年左右的时间戳时由于循环次数太多，性能会降下来）
        if (daysOfYear < 1) {
            // 算法逻辑：每次年数减一，把这一年的天数补上 tempDaysOfYear，直到tempDaysOfYear为正数为止
            int negativeCount = (daysOfYear + 1) / 365 - 1;
            // 得到减去后的年份
            int targetYear = year + negativeCount;
            // 计算2个年份之间的闰年个数为少加的天数 1988~1988 1个 1989~1988 也是1个1990~1988 1ge
            // targetYear targetYear + 1 ..... targetYear + negativeCount 之间闰年个数（理论上negativeCount / 4  + 1）
            // 1582 年
            int lt = year > 1582 ? ((year - 1) / 4 - (year - 1) / 100 + (year - 1) / 400 - ((targetYear - 1) / 4 - (targetYear - 1) / 100 + (targetYear - 1) / 400)) : ((year - 1) / 4 - (targetYear - 1) / 4);
            daysOfYear = daysOfYear - negativeCount * 365 + lt;

            // 如果daysOfYear = 366 ，且 year 是闰年
            int increaseCount = (daysOfYear - 1) / 365;
            targetYear += increaseCount;
            year = targetYear;
            if (year > 1582) {
                negativeCount = (year - 1) / 4 - (year - 1) / 100 + (year - 1) / 400 - ((year - increaseCount - 1) / 4 - (year - increaseCount - 1) / 100 + (year - increaseCount - 1) / 400);
            } else {
                negativeCount = (year - 1) / 4 - (year - increaseCount - 1) / 4;
            }
            // 如果2个时间段闰年太多lt太大，daysOfYear2 是365的数倍以上，又要回算年份
            // daysOfYear = (daysOfYear - 1) % 365 + 1 - (increaseCount / 4 - increaseCount / 100 + increaseCount / 400) ;
            daysOfYear = (daysOfYear - 1) % 365 + 1 - negativeCount;
            if (daysOfYear < 1) {
                // 继续 53～52
                year -= 1;
                if (isLeapYear == null) {
                    isLeapYear = year > 1582 ? (year - (year >> 2 << 2)) == 0 && (year % 100 != 0 || year % 400 == 0) : year % 4 == 0;
                }
                daysOfYear += isLeapYear ? 366 : 365;
            }
        }
        // targetYear 要比 year小
        // 计算   targetYear~year之间闰年的个数t(不包括year这一年但包括targetYear这一年即从 targetYear ~ year - 1)
        // year = targetYear;   daysOfYear = daysOfYear * targetYear + t;
        // 当前年是否为闰年
        if (isLeapYear == null) {
            isLeapYear = year > 1582 ? (year - (year >> 2 << 2)) == 0 && (year % 100 != 0 || year % 400 == 0) : year % 4 == 0;
        }
        int month = -1;
        int day = -1;
        int boundaryDays = 0;
        if (daysOfYear >= (boundaryDays = isLeapYear ? 336 : 335)) {
            month = 12;
            day = daysOfYear - boundaryDays + 1;
        } else if (daysOfYear >= (boundaryDays = isLeapYear ? 306 : 305)) {
            month = 11;
            day = daysOfYear - boundaryDays + 1;
        } else if (daysOfYear >= (boundaryDays = isLeapYear ? 275 : 274)) {
            month = 10;
            day = daysOfYear - boundaryDays + 1;
        } else if (daysOfYear >= (boundaryDays = isLeapYear ? 245 : 244)) {
            month = 9;
            day = daysOfYear - boundaryDays + 1;
        } else if (daysOfYear >= (boundaryDays = isLeapYear ? 214 : 213)) {
            month = 8;
            day = daysOfYear - boundaryDays + 1;
        } else if (daysOfYear >= (boundaryDays = isLeapYear ? 183 : 182)) {
            month = 7;
            day = daysOfYear - boundaryDays + 1;
        } else if (daysOfYear >= (boundaryDays = isLeapYear ? 153 : 152)) {
            month = 6;
            day = daysOfYear - boundaryDays + 1;
        } else if (daysOfYear >= (boundaryDays = isLeapYear ? 122 : 121)) {
            month = 5;
            day = daysOfYear - boundaryDays + 1;
        } else if (daysOfYear >= (boundaryDays = isLeapYear ? 92 : 91)) {
            month = 4;
            day = daysOfYear - boundaryDays + 1;
        } else if (daysOfYear >= (boundaryDays = isLeapYear ? 61 : 60)) {
            month = 3;
            day = daysOfYear - boundaryDays + 1;
        } else if (daysOfYear >= 32) {
            month = 2;
            day = daysOfYear - 31;
        } else {
            month = 1;
            day = daysOfYear;
        }

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

        this.year = year;
        this.month = month;
        this.dayOfMonth = day;
        this.hourOfDay = hourOfDay;
        this.minute = minute;
        this.second = second;
        this.millisecond = millisecond;

        this.daysOfYear = daysOfYear;
        this.dayOfWeek = dayOfWeek;

        this.leapYear = isLeapYear;

        this.afterDateChange();
    }

    public void setTime(long timeMills) {
        setTime(timeMills, false);
    }

    /**
     * 是否上午
     *
     * @return
     */
    public boolean isAm() {
        return this.hourOfDay < 12;
    }

    /**
     * 是否同一天（不考虑时辰）,需要加上时差
     *
     * @param sourceTimemills
     * @param targetTimeMills
     * @return
     */
    public static boolean isSameDay(long sourceTimemills, long targetTimeMills) {
        return (sourceTimemills + RELATIVE_MILLS + DEFAULT_OFFSET) / 86400000 == (targetTimeMills + RELATIVE_MILLS + DEFAULT_OFFSET) / 86400000;
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
        StringBuilder buff = new StringBuilder();

        if (year < 10) {
            buff.append("000");
        } else if (year < 100) {
            buff.append("00");
        } else if (year < 1000) {
            buff.append("0");
        }
        buff.append(year);

        if (month > 0) {
            buff.append(dateSyntax);
            if (month < 10) {
                buff.append(0);
            }
            buff.append(month);
        }

        if (dayOfMonth > 0) {
            buff.append(dateSyntax);
            if (dayOfMonth < 10) {
                buff.append(0);
            }
            buff.append(dayOfMonth);
        }

        buff.append(' ');
        if (hourOfDay < 10) {
            buff.append(0);
        }
        buff.append(hourOfDay).append(timeSyntax);
        if (minute < 10) {
            buff.append(0);
        }
        buff.append(minute).append(timeSyntax);
        if (second < 10) {
            buff.append(0);
        }
        buff.append(second);

        return buff.toString();
    }

    /**
     * Y 4位数年份
     * y 2位年份
     * M 格式化2位月份
     * d 格式化2位天
     * H 格式化24制小时数
     * m 格式化2位分钟
     * s 格式化2位秒
     * S 格式化3位毫秒
     *
     * @param template
     * @return
     */
    public String format(String template) {

        if (template == null) return null;
        StringBuilder writer = new StringBuilder();
        int length = template.length();
        int beginIndex = 0;
        for (int i = 0; i < length; i++) {
            char ch = template.charAt(i);
            switch (ch) {
                case 'Y':
                    writer.append(template, beginIndex, i);
                    writer.append(year);
                    beginIndex = i + 1;
                    continue;
                case 'y':
                    writer.append(template, beginIndex, i);
                    writer.append(year % 100);
                    beginIndex = i + 1;
                    continue;
                case 'M':
                    writer.append(template, beginIndex, i);
                    if (month < 10) {
                        writer.append("0");
                    }
                    writer.append(month);
                    beginIndex = i + 1;
                    continue;
                case 'd':
                    writer.append(template, beginIndex, i);
                    if (dayOfMonth < 10) {
                        writer.append("0");
                    }
                    writer.append(dayOfMonth);
                    beginIndex = i + 1;
                    continue;
                case 'H':
                    writer.append(template, beginIndex, i);
                    if (hourOfDay < 10) {
                        writer.append("0");
                    }
                    writer.append(hourOfDay);
                    beginIndex = i + 1;
                    continue;
                case 'm':
                    writer.append(template, beginIndex, i);
                    if (minute < 10) {
                        writer.append("0");
                    }
                    writer.append(minute);
                    beginIndex = i + 1;
                    continue;
                case 's':
                    writer.append(template, beginIndex, i);
                    if (second < 10) {
                        writer.append("0");
                    }
                    writer.append(second);
                    beginIndex = i + 1;
                    continue;
                case 'S':
                    writer.append(template, beginIndex, i);
                    if (millisecond < 10) {
                        writer.append("00");
                    } else if (millisecond < 100) {
                        writer.append("0");
                    }
                    writer.append(millisecond);
                    beginIndex = i + 1;
                    continue;
                default:
                    continue;
            }
        }

        writer.append(template, beginIndex, length);

        return writer.toString();
    }

    public int compareTo(Date o) {
        if (timeMills == o.timeMills) {
            return 0;
        }
        return timeMills > o.timeMills ? 1 : -1;
    }

    public static void main(String[] args) {
        System.out.println(new Date().format("Y/M/d H:m:s"));
    }

}
