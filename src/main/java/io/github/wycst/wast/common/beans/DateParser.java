package io.github.wycst.wast.common.beans;

/**
 * java日期类转化
 *
 * @Author wangyunchao
 * @Date 2022/12/4 22:08
 */
public class DateParser {

    /**
     * 年月日时分秒毫秒-21bits+
     */
    private static DateTemplate[] pattern_21bit =
            new DateTemplate[]{new DateTemplate("yyyy.MM.dd HH:mm:ss.S")};
//            new DateTemplate[]{new DateTemplate("yyyy-MM-dd HH:mm:ss.S"), new DateTemplate("yyyy.MM.dd HH:mm:ss.S"), new DateTemplate("yyyy/MM/dd HH:mm:ss.S")};

    // 年月日时分秒-19bits
    private static DateTemplate[] pattern_19bit =
            new DateTemplate[]{new DateTemplate("yyyy.MM.dd HH:mm:ss")};
//            new DateTemplate[]{new DateTemplate("yyyy-MM-dd HH:mm:ss"), new DateTemplate("yyyy.MM.dd HH:mm:ss"), new DateTemplate("yyyy/MM/dd HH:mm:ss")};

    /**
     * 年月日-10bits
     */
    private static DateTemplate[] pattern_10bit =
            new DateTemplate[]{new DateTemplate("yyyy.MM.dd")};
//            new DateTemplate[]{new DateTemplate("yyyy-MM-dd"), new DateTemplate("yyyy.MM.dd"), new DateTemplate("yyyy/MM/dd")};

    /**
     * 年月日-8位
     */
    private static DateTemplate[] pattern_8bit =
            new DateTemplate[]{new DateTemplate("yyyyMMdd")};

    /**
     * 其他（Y/M/d, Y-M-d）兼容字段中一位或者两位组合使用，例如: 1998/1/12, 1998/1/12 1:22, 1998/1/12 1:22:3
     */
    private static DateTemplate[] pattern_others = new DateTemplate[]{
            new DateTemplate("Y.M.d H:m:s"),
            // 07/01/1999
            new DateTemplate("M.d.Y H:m:s"),
            new DateTemplate("Y.M.d H:m"),
            new DateTemplate("M.d.Y H:m"),
            new DateTemplate("Y.M.d"),
            new DateTemplate("M.d.Y"),
    };

    public static long parseTime(String originDate) {
        int len = (originDate = originDate.trim()).length();
        switch (len) {
            case 8:
                return parseTime(originDate, pattern_8bit);
            case 10:
                return parseTime(originDate, pattern_10bit);
            case 19:
                return parseTime(originDate, pattern_19bit);
            case 21:
            case 22:
            case 23:
                return parseTime(originDate, pattern_21bit);
            default:
                return parseTime(originDate, pattern_others);
        }
    }

    public static GregorianDate parseDate(String originDate) {
        int len = (originDate = originDate.trim()).length();
        switch (len) {
            case 8:
                return parseDate(originDate, pattern_8bit);
            case 10:
                return parseDate(originDate, pattern_10bit);
            case 19:
                return parseDate(originDate, pattern_19bit);
            case 21:
            case 22:
            case 23:
                return parseDate(originDate, pattern_21bit);
            default:
                return parseDate(originDate, pattern_others);
        }
    }

    private static long parseTime(String originDate, DateTemplate[] dateTemplates) {
        return parseDate(originDate, dateTemplates).getTime();
    }

    private static GregorianDate parseDate(String originDate, DateTemplate[] dateTemplates) {
        for (DateTemplate dateTemplate : dateTemplates) {
            try {
                return dateTemplate.parse(originDate);
            } catch (Exception e) {
            }
        }
        throw new IllegalArgumentException(String.format("date input '%s' not matched any format", originDate));
    }

}
