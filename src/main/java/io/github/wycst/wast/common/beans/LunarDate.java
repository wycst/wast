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
 * 农历
 * 注：当前干支纪年默认从正月初一开始，可切换为立春
 *
 * @author wangyunchao
 */
public class LunarDate extends Date {

    // 农历年
    private int lunarYear;

    // 农历月
    private int lunarMonth;

    // 农历日
    private int lunarDay;

    /**
     * 当前农历月份是否为闰月
     */
    private boolean leapMonth;

    /**
     * 年天干（0-9）
     */
    private int stemsYearIndex;

    /**
     * 年地支（0-11）
     */
    private int branchesYearIndex;

    /**
     * 月天干（0-9）
     */
    private int stemsMonthIndex;

    /**
     * 月地支（0-11）
     */
    private int branchesMonthIndex;

    /**
     * 日天干（0-9）
     */
    private int stemsDayIndex;

    /**
     * 日地支（0-11）
     */
    private int branchesDayIndex;

    // 干支纪年开始类型，默认以春节开始
    public static StemsBranchesType stemsBranchesType = StemsBranchesType.SpringFestival;

    /**
     * 所属十二生肖与天干年一一对应
     */
    private String zodiac;

    /**
     * 星座于十二（节）气日一一对应
     */
    private String constellation;

    /**
     * 当日如果为十二节气（二十四节气中的12节气）交接日则值为当月月份，否则为0
     */
    private int solarTerms;

    private static final String[] LUNAR_MONTHS = {"正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月"};

    /**
     * (立春), 雨水, (惊蛰), 春分, (清明), 谷雨, (立夏), 小满 , (芒种), 夏至, (小暑), 大暑, (立秋), 处暑, (白露), 秋分, (寒露), 霜降, (立冬), 小雪, (大雪), 冬至, (小寒), 大寒
     */
    private static final String[] SOLAR_TERMS = {
            "小寒", "大寒",
            "立春", "雨水",
            "惊蛰", "春分",
            "清明", "谷雨",
            "立夏", "小满",
            "芒种", "夏至",
            "小暑", "大暑",
            "立秋", "处暑",
            "白露", "秋分",
            "寒露", "霜降",
            "立冬", "小雪",
            "大雪", "冬至"
    };

    // 农历纪年天
    private static final String[] LUNAR_DAYS = {
            "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"};

    // 十天干
    private static final String[] HEAVENLY_STEMS = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};

    // 十二地支
    private static final String[] EARTHLY_BRANCHES = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};

    // 十二生肖
    private static final String[] CHINESE_ZODIAC = {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};

    // 十二星座
    private final static int[] CONSTELLATIONS_OFFSETS = new int[] { 20, 19, 21, 20, 21, 22, 23, 23, 23, 24, 23, 22 };
    private static final String[] TWELVE_CONSTELLATIONS = {"水瓶座", "双鱼座", "白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座", "天秤座", "天蝎座", "射手座", "摩羯座" };

    // 天文台数据(1900~2049)
    public final static long[] LUNAR_DATA = new long[]{0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0,
            0x09ad0, 0x055d2, 0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
            0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970, 0x06566, 0x0d4a0,
            0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950, 0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0,
            0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557, 0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573,
            0x052d0, 0x0a9a8, 0x0e950, 0x06aa0, 0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950,
            0x05b57, 0x056a0, 0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
            0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570, 0x04af5, 0x04970,
            0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0, 0x0c960, 0x0d954, 0x0d4a0, 0x0da50,
            0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5, 0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0,
            0x0a5b0, 0x15176, 0x052b0, 0x0a930, 0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260,
            0x0ea65, 0x0d530, 0x05aa0, 0x076a3, 0x096d0, 0x04bd7, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
            0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0};

    // 开始
    public final static long LUNAR_DATA_TIME_BEGIN;
    // 结束
    public final static long LUNAR_DATA_TIME_END;

    // 农历每年天数
    private final static int[] DAYS_COUNT_OF_YEAR = new int[LUNAR_DATA.length];

    // 农历每月天数
    private final static int[][] DAYS_COUNT_OF_MONTHS = new int[LUNAR_DATA.length][];

    // 农历每年闰月月份位置 如果为0 说明当年没有闰月
    private final static int[] LEAP_MONTH_INDEXOF_YEARS = new int[LUNAR_DATA.length];

    // 农历前19年春节（正月初一所在年位置）
    private final static int[] SPRING_FESTIVAL_DAY_INDEXS = new int[19];

    // 以下2个属性理应放在LightDate中（阳历）中，考虑到与干支纪年有关，暂放在农历中
    // 以2019年24节气中时刻（毫秒数）作为参考
    // 从小寒开始，冬至结束
    // 12星座基本和节气中的12气对应
    private final static long[] SOLAR_TERMS_2019 = new long[24];

    // 地球公转一周年毫秒数(计算24节气)
    public final static long YEAR_TIMEMILLS = 31556925216l;

    // 是否禁用自动update
    private boolean disableAutoUpdate;

    public enum StemsBranchesType {
        // 春节
        SpringFestival,
        // 立春
        BeginningOFSpring
    }

    static {
        // 计算1900～2049年公历每年
        // 1900.1.31（1900.1.1） ～ 2050.1.23（2050.1.1 不包含）
        for (int i = 0; i < LUNAR_DATA.length; i++) {
            // 16进制 共5 * 4 20位
            long lunarOfYear = LUNAR_DATA[i];

            // 尾4位 0代表没有闰月，非闰年，其他代表闰的月份
            long leapMonth = lunarOfYear & 0xf;

            // 头4位，如果1代表闰年是大月，0代表闰年是小月（前提是闰年情况）
            long leapType = lunarOfYear >> 16;

            // 中间12位代表每个月是大月还是小月
            long monthTypes = (lunarOfYear & 0xfff0) >> 4;
            // 12个月每个月是大月（1）还是小月（0）
            int[] monthTypeArr = getMonthTypeArr(monthTypes);

            // 每个月的天数
            int[] daysOfMonth = new int[leapMonth > 0 ? 13 : 12];

            int daysOfYear = 0;
            for (int j = 0; j < 12; j++) {
                int days = monthTypeArr[j] == 1 ? 30 : 29;
                daysOfYear += days;
                if (leapMonth > 0) {
                    daysOfMonth[j > leapMonth - 1 ? j + 1 : j] = days;
                } else {
                    daysOfMonth[j] = days;
                }
            }

            // 如果闰月存在
            if (leapMonth > 0) {
                // 闰月
                int days = leapType == 0 ? 29 : 30;
                daysOfYear += days;
                daysOfMonth[(int) leapMonth] = days;
            }

            LEAP_MONTH_INDEXOF_YEARS[i] = (int) leapMonth;
            DAYS_COUNT_OF_YEAR[i] = daysOfYear;
            DAYS_COUNT_OF_MONTHS[i] = daysOfMonth;

        }

        // 根据农历每年天数计算前19年春节（正月初一）所在的阳历日
        // 比如农历1900.1.1 年是阳历1900.1.31日
        SPRING_FESTIVAL_DAY_INDEXS[0] = 31;
        // 1901.1.1 是阳历1901.2.19
        Date date = new Date(1900, 1, 31);
        for (int next = 0; next < 18; next++) {
            int days = DAYS_COUNT_OF_YEAR[next];
            date.add(DAY_OF_MONTH, days);
            SPRING_FESTIVAL_DAY_INDEXS[next + 1] = date.getDaysOfYear();
        }

        // 初始化2019年节气时间
        // 2019/01/05 23:38:57 小寒 太阳黄经285°
        SOLAR_TERMS_2019[0] = date.set(2019, 1, 5, 23, 38, 57, 0).getTime();
        // 2019/01/20 16:59:32 大寒 太阳黄经300°
        SOLAR_TERMS_2019[1] = date.set(2019, 1, 20, 16, 59, 32, 0).getTime();
        // 2019/02/04 11:14:19 立春 太阳黄经315°
        SOLAR_TERMS_2019[2] = date.set(2019, 2, 4, 11, 14, 19, 0).getTime();
        // 2019/02/19 07:03:56  雨水 太阳黄经330°
        SOLAR_TERMS_2019[3] = date.set(2019, 2, 19, 7, 3, 56, 0).getTime();
        // 2019/03/06 05:09:44  惊蛰 太阳黄经345°
        SOLAR_TERMS_2019[4] = date.set(2019, 3, 6, 5, 9, 44, 0).getTime();
        // 2019/03/21 05:58:25 春分 太阳黄经0°
        SOLAR_TERMS_2019[5] = date.set(2019, 3, 21, 5, 38, 25, 0).getTime();
        // 2019/04/05 09:51:26 清明 太阳黄经15°
        SOLAR_TERMS_2019[6] = date.set(2019, 4, 5, 9, 51, 26, 0).getTime();
        // 2019/04/20 16:55:15 谷雨 太阳黄经30°
        SOLAR_TERMS_2019[7] = date.set(2019, 4, 20, 16, 55, 15, 0).getTime();
        // 2019/05/06 03:02:45 立夏 太阳黄经45°
        SOLAR_TERMS_2019[8] = date.set(2019, 5, 6, 3, 2, 45, 0).getTime();
        // 2019/05/21 15:59:06 小满 太阳黄经60°
        SOLAR_TERMS_2019[9] = date.set(2019, 5, 21, 15, 59, 6, 0).getTime();
        // 2019/06/06 07:06:23 芒种 太阳黄经75°
        SOLAR_TERMS_2019[10] = date.set(2019, 6, 6, 7, 6, 23, 0).getTime();
        // 2019/06/21 23:54:14 夏至 太阳黄经90°
        SOLAR_TERMS_2019[11] = date.set(2019, 6, 21, 23, 54, 14, 0).getTime();
        // 2019/07/07 17:20:30 小暑 太阳黄经105°
        SOLAR_TERMS_2019[12] = date.set(2019, 7, 7, 17, 20, 30, 0).getTime();
        // 2019/07/23 10:50:21 大暑 太阳黄经120°
        SOLAR_TERMS_2019[13] = date.set(2019, 7, 23, 10, 50, 21, 0).getTime();
        // 2019/08/08 03:13:02 立秋 太阳黄经135°
        SOLAR_TERMS_2019[14] = date.set(2019, 8, 8, 3, 13, 2, 0).getTime();
        // 2019/08/23 18:01:58 处暑 太阳黄经150°
        SOLAR_TERMS_2019[15] = date.set(2019, 8, 23, 18, 1, 58, 0).getTime();
        // 2019/09/08 06:16:51 白露 太阳黄经165°
        // 2019/09/23 15:50:07 秋分 太阳黄经180°
        SOLAR_TERMS_2019[16] = date.set(2019, 9, 8, 6, 16, 51, 0).getTime();
        SOLAR_TERMS_2019[17] = date.set(2019, 9, 23, 15, 50, 7, 0).getTime();
        // 2019/10/08 22:05:37 寒露 太阳黄经195°
        // 2019/10/24 01:19:42 霜降 太阳黄经210°
        SOLAR_TERMS_2019[18] = date.set(2019, 10, 8, 22, 5, 37, 0).getTime();
        SOLAR_TERMS_2019[19] = date.set(2019, 10, 24, 1, 19, 42, 0).getTime();
        // 2019/11/08 01:24:20 立冬 太阳黄经225°
        // 2019/11/22 22:58:53 小雪 太阳黄经240°
        SOLAR_TERMS_2019[20] = date.set(2019, 11, 8, 1, 24, 20, 0).getTime();
        SOLAR_TERMS_2019[21] = date.set(2019, 11, 22, 22, 58, 53, 0).getTime();
        // 2019/12/07 18:18:26 大雪 太阳黄经255°
        // 2019/12/22 12:19:23 冬至 太阳黄经270°
        SOLAR_TERMS_2019[22] = date.set(2019, 12, 7, 18, 18, 26, 0).getTime();
        SOLAR_TERMS_2019[23] = date.set(2019, 12, 22, 12, 19, 23, 0).getTime();

        // 1900.1.31（1900.1.1） ～ 2050.1.23 (2050.1.1)
        LUNAR_DATA_TIME_BEGIN = date.set(1900, 1, 31, 0, 0, 0, 0).getTime();
        LUNAR_DATA_TIME_END = date.set(2050, 1, 23, 0, 0, 0, 0).getTime();

    }

    public LunarDate() {
    }

    public LunarDate(Date date) {
        fromDate(date);
    }

    private void fromDate(Date date) {
        this.year = date.year;
        this.month = date.month;
        this.dayOfMonth = date.dayOfMonth;
        this.daysOfYear = date.daysOfYear;
        this.hourOfDay = date.hourOfDay;
        this.minute = date.minute;
        this.second = date.second;
        this.millisecond = date.millisecond;
        this.leapYear = date.leapYear;
        this.timeMills = date.timeMills;
        this.currentOffset = date.currentOffset;
        compute();
    }

    /**
     * 根据年月日（公历）构建农历日期
     *
     * @param year
     * @param month
     * @param day
     */
    public LunarDate(int year, int month, int day) {
        this(year, month, day, null);
    }

    /**
     * 根据年月日（公历）时间构建农历日期
     *
     * @param year
     * @param month
     * @param day
     * @param timeZone
     */
    public LunarDate(int year, int month, int day, TimeZone timeZone) {
        this(year, month, day, 0, 0, 0, timeZone);
    }

    public LunarDate(int year, int month, int day, int hour, int minute, int second, TimeZone timeZone) {
        super(year, month, day, hour, minute, second, 0, null);
    }

    public LunarDate(int year, int month, int day, int hour, int minute, int second) {
        this(year, month, day, hour, minute, second, null);
    }

    private static int[] getMonthTypeArr(long monthTypes) {
        // 转换成int数组
        String monthTypesStr = Long.toBinaryString(monthTypes);
        int[] monthTypeArr = new int[12];
        int len = monthTypesStr.length();
        for (int i = 12 - len, j = 0; j < len; i++, j++) {
            monthTypeArr[i] = monthTypesStr.charAt(j) == '0' ? 0 : 1;
        }
        return monthTypeArr;
    }

    private void compute() {

        int year = getYear(), daysOfYear = getDaysOfYear();
        // 天文台数据范围 ： 1900.1.31（1900.1.1） ～ 2050.1.23（2050.1.1）
        if (timeMills >= LUNAR_DATA_TIME_BEGIN && timeMills < LUNAR_DATA_TIME_END) {
            // 计算 1900.1.1 到 year.1.1 之间的天数 +  daysOfYear 得到距离1900.1.1的精确天数
            // 然后累加农历年天数，得出农历日期
            // (year - 1900) * 365 + leapCount
            int leapCount = (year - 1) / 4 - (year - 1) / 100 + (year - 1) / 400 - (1900 / 4 - 1900 / 100 + 1900 / 400);
            // 待计算的日期距离1900.1.1的天数
            int days = (year - 1900) * 365 + leapCount + daysOfYear - 1;

            // 距离1900.1.31（正月初一）
            days = days - 30;
            // 下面开始累加农历年数据，直到累加值 = days

            int total = 0;
            for (int i = 0; i < 150; i++) {
                int daysCountOfYear = DAYS_COUNT_OF_YEAR[i];
                if (total + daysCountOfYear > days) {
                    lunarYear = i + 1900;
                    int[] monthDays = DAYS_COUNT_OF_MONTHS[i];
                    for (int j = 0; j < monthDays.length; j++) {
                        int daysOfMonth = monthDays[j];
                        total += daysOfMonth;
                        if (total > days) {
                            int day = daysOfMonth - (total - days) + 1;
                            int leapMonthIndex = LEAP_MONTH_INDEXOF_YEARS[i];
                            if (leapMonthIndex == 0 || j < leapMonthIndex) {
                                lunarMonth = j + 1;
                            } else {
                                // j >= leapMonthIndex
                                lunarMonth = j;
                                if (j == leapMonthIndex) {
                                    leapMonth = true;
                                }
                            }
                            lunarDay = day;
                            break;
                        }
                    }
                    break;
                } else {
                    total += daysCountOfYear;
                }
            }
        } else {
            // 只计算年份
            // 1899  ---- 1899 + 19 1918
            int index = ((year - 1900) % 19 + 19) % 19;
            int springFestivalDayIndex = SPRING_FESTIVAL_DAY_INDEXS[index];
            if (daysOfYear < springFestivalDayIndex) {
                lunarYear = year - 1;
            } else {
                lunarYear = year;
            }
            // 如果没有天文台数据无法精确计算月份和天
            // 定位到1900 + {index} + 19 年
            monthLocationAt(index + 19, year - lunarYear);
        }

        // 初始化干支
        initStemsBranches();

        // 计算星座
        int constellationIndex = month - 1;
        int offset = CONSTELLATIONS_OFFSETS[constellationIndex];
        if(dayOfMonth < offset) {
            constellationIndex += 11;
        }
        this.constellation = TWELVE_CONSTELLATIONS[constellationIndex % 12];
    }

    /**
     * @param yearIndex 相对1900（0）年的位置
     * @param offset
     */
    private void monthLocationAt(int yearIndex, int offset) {

        // 计算1900+offsetIndex年month月day日所在的农历月份
        int leapCount = (yearIndex - 1) / 4;

        int beginYear = year;
        int endYear = beginYear + yearIndex;

        int daysOfYear = this.daysOfYear;
        // 计算daysOfYear是否一致（最多因为闰年差1天）
        // 2.28（31+28）
        if (endYear % 4 == 0) {
            if (daysOfYear > 59) {
                daysOfYear++;
            }
        } else if (isLeapYear()) {
            // 31 + 29
            if (daysOfYear > 60) {
                daysOfYear--;
            }
        }
        // 先计算距离1900.1.1天数
        int days = yearIndex * 365 + leapCount + daysOfYear - 1;

        // 阳历1900.1.31对应农历1900正月初一
        // days = 距离1900.1.31天数，也就是距离农历1900正月初一的天数，如果为0就是正月初一
        // 则 lunarDay = days（累减到目标月份后的值） + 1
        days = days - 30;

        // daysCountOfYear[0]开始对应1900年1月31日
        int actualIndex = yearIndex - offset;
        for (int i = 0; i < actualIndex; i++) {
            days -= DAYS_COUNT_OF_YEAR[i];
        }
        // 农历该年每月天数情况
        int[] monthDays = DAYS_COUNT_OF_MONTHS[actualIndex];
        for (int i = 0; i < monthDays.length; i++) {
            int daysOfMonth = monthDays[i];
            if (days <= daysOfMonth - 1) {
                if (days > 0 && days < daysOfMonth - 1) {
                    // 月份为i
                    int leapMonthIndex = LEAP_MONTH_INDEXOF_YEARS[actualIndex];
                    if (leapMonthIndex == 0 || i < leapMonthIndex) {
                        lunarMonth = i + 1;
                    } else {
                        // j >= leapMonthIndex
                        lunarMonth = i;
                        if (lunarMonth == leapMonthIndex) {
                            leapMonth = true;
                        }
                    }
                }
                break;
            } else {
                days -= daysOfMonth;
            }
        }
    }

    /**
     * 初始化年月日天干地支信息
     */
    private void initStemsBranches() {

        clear();

        // 天干地支从公元4年开始算(甲子年 干支偏移量都为0)
        int years = year - 4;
        stemsYearIndex = (years % 10 + 10) % 10;
        branchesYearIndex = (years % 12 + 12) % 12;

        // 干支年份从农历的春节（农历正月初一即1.1）开始
        // 如果农历年份小月公历年份取上一年的干支年
        // 如果以正月初一作为干支年开始：判断农历年份是否小于公历年份
        boolean isPrevStemsBranches = false;
        if (stemsBranchesType == null || stemsBranchesType == StemsBranchesType.SpringFestival) {
            // 以春节开始判断农历年份是否小于公历年份
            isPrevStemsBranches = lunarYear < year;
        } else {
            // 以立春开始判断时间前后即可
            // 立春是24节气第3个
            int solarTermsIndex = 2;
            long solarTermsTimemills = SOLAR_TERMS_2019[solarTermsIndex] + (year - 2019) * YEAR_TIMEMILLS;

            // 跳到当年立春日
            // Date date = tempLightDate(solarTermsTimemills);
            // 判断当前日是否为在立春日之前（忽略小时数）
            if (isSameDay(solarTermsTimemills, timeMills)) {
                // 记录立春月份
                this.solarTerms = 2;
            } else {
                // 如果不是立春日，判断是否在立春时间前面
                isPrevStemsBranches = timeMills < solarTermsTimemills;
            }
        }
        // 是否干支上一年
        if (isPrevStemsBranches) {
            stemsYearIndex = stemsYearIndex == 0 ? 9 : stemsYearIndex - 1;
            branchesYearIndex = branchesYearIndex == 0 ? 11 : branchesYearIndex - 1;
        }
        // 如果以立春作为开始，只需要算出当年立春（2月）的日期，判断当前日期是否小于立春日期即可

        // 干支月计算
        //#######################################################################
        // 计算干支月参考从1900.1 中气（丁丑月）日到当前日期开始计算距离月份数（公历）
        // 丁丑月： 偏移 量干（3） 支（1）
        int months = (year - 1900) * 12 + month - 1;

        // 得出月天干和月地支 & +10和+12是为了处理边界和负数
        stemsMonthIndex = (months % 10 + 3 + 10) % 10;
        branchesMonthIndex = (months % 12 + 1 + 12) % 12;

        /**
         * (立春), 雨水, (惊蛰), 春分, (清明), 谷雨, (立夏), 小满 , (芒种), 夏至, (小暑), 大暑, (立秋), 处暑, (白露), 秋分, (寒露), 霜降, (立冬), 小雪, (大雪), 冬至, (小寒), 大寒
         * */
        // 每个干支月对应一个节气开始到结束（下一个节气交接）
        int solarTermsType = 3;
        if (month == 2) {
            // 立春 2.3～2.5
            if (dayOfMonth < 3) {
                solarTermsType = 1;
            } else if (dayOfMonth >= 5) {
                solarTermsType = 2;
            }
        } else if (month == 3) {
            // 惊蛰 3.5～3.7
            if (dayOfMonth < 5) {
                solarTermsType = 1;
            } else if (dayOfMonth >= 7) {
                solarTermsType = 2;
            }
        } else if (month == 4) {
            // 清明 4.4~4.6
            if (dayOfMonth < 4) {
                solarTermsType = 1;
            } else if (dayOfMonth > 6) {
                solarTermsType = 2;
            }
        } else if (month == 5) {
            // 立夏 5.5~5.7
            if (dayOfMonth < 5) {
                solarTermsType = 1;
            } else if (dayOfMonth >= 7) {
                solarTermsType = 2;
            }
        } else if (month == 6) {
            // 芒种 6.5~6.7
            if (dayOfMonth < 5) {
                solarTermsType = 1;
            } else if (dayOfMonth >= 7) {
                solarTermsType = 2;
            }
        } else if (month == 7) {
            // 小暑 7.6~7.8
            if (dayOfMonth < 6) {
                solarTermsType = 1;
            } else if (dayOfMonth >= 8) {
                solarTermsType = 2;
            }
        } else if (month == 8) {
            // 立秋 8.7~8.9
            if (dayOfMonth < 7) {
                solarTermsType = 1;
            } else if (dayOfMonth >= 9) {
                solarTermsType = 2;
            }
        } else if (month == 9) {
            // 白露 9.7~9.9
            if (dayOfMonth < 7) {
                solarTermsType = 1;
            } else if (dayOfMonth >= 9) {
                solarTermsType = 2;
            }
        } else if (month == 10) {
            // 寒露 10.8~10.9
            if (dayOfMonth < 8) {
                solarTermsType = 1;
            } else if (dayOfMonth >= 9) {
                solarTermsType = 2;
            }
        } else if (month == 11) {
            // 立冬 11.7~11.8
            if (dayOfMonth < 7) {
                solarTermsType = 1;
            } else if (dayOfMonth >= 8) {
                solarTermsType = 2;
            }
        } else if (month == 12) {
            // 大雪 12.6~12.8
            if (dayOfMonth < 6) {
                solarTermsType = 1;
            } else if (dayOfMonth >= 8) {
                solarTermsType = 2;
            }
        } else if (month == 1) {
            // 小寒 1.5~1.7
            if (dayOfMonth < 5) {
                solarTermsType = 1;
            } else if (dayOfMonth >= 7) {
                solarTermsType = 2;
            }
        }

        // 节气前取上个月的干支，节气中取当前结果干支
        if (solarTermsType == 1) {
            // 节气前
            stemsMonthIndex = stemsMonthIndex == 0 ? 9 : stemsMonthIndex - 1;
            branchesMonthIndex = branchesMonthIndex == 0 ? 11 : branchesMonthIndex - 1;
        } else if (solarTermsType == 3) {
            // 不确定计算
            // 月份month与24节气中12节气位置关系
            int solarTermsIndex = (month - 1) * 2;
            // 当前月节气所在日
            long solarTermsTimemills = SOLAR_TERMS_2019[solarTermsIndex] + (year - 2019) * YEAR_TIMEMILLS;

            // 判断当天和节气日是否同一天
            if (isSameDay(solarTermsTimemills, timeMills)/*date.getDay() == dayOfMonth*/) {
                // 当天是节气日（不考虑具体时刻）记录月份
                this.solarTerms = month;
            }
            if (solarTermsTimemills > timeMills) {
                // 节气前
                stemsMonthIndex = stemsMonthIndex == 0 ? 9 : stemsMonthIndex - 1;
                branchesMonthIndex = branchesMonthIndex == 0 ? 11 : branchesMonthIndex - 1;
            } else {
                // 节气后
            }
        }

        // 计算干支日参考从1900.1.1（甲戌日第11个）日到当前日期开始计算距离天数
        // (year - 1)/4 - (year - 1)/100 + (year - 1)/400 - (1900/4 - 1900/100 + 1900/400)
        int leapCount = (year - 1) / 4 - (year - 1) / 100 + (year - 1) / 400 - (1900 / 4 - 1900 / 100 + 1900 / 400);
        // 待计算的日期距离1900.1.1的天数
        int days = (year - 1900) * 365 + leapCount + daysOfYear - 1;

        // 甲戌日: 天干偏移 天（0）干 （10）
        // 天 （0 + 10（去负数））
        stemsDayIndex = (days % 10 + 10) % 10;
        // 地 (10 + 12（去负数）)
        branchesDayIndex = (days % 12 + 22) % 12;

        // 十二生肖
        zodiac = CHINESE_ZODIAC[branchesYearIndex];
    }

    private void clear() {
        // 节气月份
        this.solarTerms = 0;
    }

    /**
     * 定位到指定农历日期，并更新公历信息（公历定位通过set方法即可）
     *
     * @param lunarYear
     * @param lunarMonth
     * @param lunarDay
     */
    public void locationTo(int lunarYear, int lunarMonth, int lunarDay) {

        if (lunarYear >= 1900 && lunarYear < 2050) {
            // 通过天文台数据可以精确定位
            int yearIndex = lunarYear - 1900;
            int days = 0;
            for (int i = 0; i < yearIndex; i++) {
                days += DAYS_COUNT_OF_YEAR[i];
            }
            int[] daysCountOfMonth = DAYS_COUNT_OF_MONTHS[yearIndex];
            for (int i = 0; i < daysCountOfMonth.length; i++) {
                if (i < lunarMonth - 1) {
                    days += daysCountOfMonth[i];
                } else {
                    days += lunarDay - 1;
                    break;
                }
            }
            // 停用自动update
            this.disableAutoUpdate = true;
            // days为距离1900.1.31（1900.1.1）的天数
            this.set(1900, 1, 31).add(DAY_OF_MONTH, days);
            compute();
            this.disableAutoUpdate = false;
        } else {
            // 如果没有数据表无法准确定位，只能估计定位
            int index = ((lunarYear - 1900) % 19 + 19) % 19;
            // 春节前
            int springFestivalDayIndex = SPRING_FESTIVAL_DAY_INDEXS[index];
            // 公历年份
            int year = lunarYear;
            // 重新定位
            locationTo(index + 19 + 1900, lunarMonth, lunarDay);
            // 判断年份是否
            int daysOfYear = getDaysOfYear();
            if (daysOfYear < springFestivalDayIndex) {
                // 如果在春节之前，公历年份++
                year++;
            }
            set(year, month, dayOfMonth);
            if (this.lunarYear == lunarYear && this.lunarMonth == lunarMonth) {
                dayOfMonth = 0;
            } else {
                dayOfMonth = 0;
                month = 0;
            }

        }
    }


    // 从公元1900年1.31 对应的农历为： 1900年1月1日（正月初一 庚子年 【鼠年】丁丑月 甲辰日）
    // 开始推算

    // 干支纪年从公元4年开始，已经公元年份y，那么干支年 = y % 60 - 3： 得到的值就是上表（1-60）序列值
    // 以2019年为例子 2019 % 60 = 39 39 - 3 = 36 得出己亥年

    @Override
    protected void afterDateChange() {
        if (!disableAutoUpdate) {
            compute();
        }
    }

    public String toString() {

        StringBuilder writer = new StringBuilder();
        writer.append(super.toString());
        writer.append(' ');

        if (lunarYear > 0) {
            writer.append("农历").append(lunarYear).append('年');
            if (lunarMonth > 0) {
                if (leapMonth) {
                    writer.append('闰');
                }
                writer.append(LUNAR_MONTHS[lunarMonth - 1]);
            }
            if (lunarDay > 0) {
                writer.append(LUNAR_DAYS[lunarDay - 1]).append(" ");
            }
        }
        writer.append(" ");
        writer.append(HEAVENLY_STEMS[stemsYearIndex]).append(EARTHLY_BRANCHES[branchesYearIndex]).append('年')
                .append("（").append(zodiac).append('年').append("）")
                .append(HEAVENLY_STEMS[stemsMonthIndex]).append(EARTHLY_BRANCHES[branchesMonthIndex]).append('月')
                .append(HEAVENLY_STEMS[stemsDayIndex]).append(EARTHLY_BRANCHES[branchesDayIndex]).append('日')
                .append(' ');

        if (solarTerms > 0) {
            writer.append(" ").append(SOLAR_TERMS[(solarTerms - 1) * 2]).append(" ");
        }

        writer.append(constellation).append(" ");

        return writer.toString();
    }

    /**
     * 获取农历年份
     *
     * @return
     */
    public int getLunarYear() {
        return lunarYear;
    }

    /**
     * 获取农历月份
     *
     * @return
     */
    public int getLunarMonth() {
        return lunarMonth;
    }

    /**
     * 获取农历日
     *
     * @return
     */
    public int getLunarDay() {
        return lunarDay;
    }

    /**
     * 是否闰年
     *
     * @return
     */
    public boolean isLeap() {
        return leapMonth;
    }

    /**
     * 获取年天干
     *
     * @return
     */
    public int getStemsYearIndex() {
        return stemsYearIndex;
    }

    /**
     * 获取年地支
     *
     * @return
     */
    public int getBranchesYearIndex() {
        return branchesYearIndex;
    }

    /**
     * 获取月天干
     *
     * @return
     */
    public int getStemsMonthIndex() {
        return stemsMonthIndex;
    }

    /**
     * 获取月地支
     *
     * @return
     */
    public int getBranchesMonthIndex() {
        return branchesMonthIndex;
    }

    /**
     * 获取日天干
     *
     * @return
     */
    public int getStemsDayIndex() {
        return stemsDayIndex;
    }

    /**
     * 获取日地支
     *
     * @return
     */
    public int getBranchesDayIndex() {
        return branchesDayIndex;
    }

    /**
     * 获取生肖
     *
     * @return
     */
    public String getZodiac() {
        return zodiac;
    }

    /**
     * 获取星座
     *
     * @return
     */
    public String getConstellation() {
        return constellation;
    }

    /**
     * 获取节气日索引，如果不是节气日返回-1
     *
     * @return
     */
    public int getSolarTerms() {
        return solarTerms;
    }

    public static void main(String[] args) {
        System.out.println(new LunarDate().add(DAY_OF_MONTH, 30));
    }
}
