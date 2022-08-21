package io.github.wycst.wast.json.temporal;

/**
 * jdk1.6没有Temporal模块，提供Temporal接口方案代替反射
 *
 * @Author: wangy
 * @Date: 2022/8/21 0:15
 * @Description:
 */
interface TemporalInterface {

    /**
     * 获取默认的zoneId
     *
     * @return
     */
    Object getDefaultZoneId() throws Exception;

    /**
     * 获取zoneId
     *
     * @param zonedDateTime
     * @return
     */
    Object getZoneId(Object zonedDateTime) throws Exception;

    /**
     * 构造ZoneId对象
     *
     * @param zoneId
     * @return
     * @throws Exception
     */
    Object ofZoneId(String zoneId) throws Exception;

    Number getLocalDateYear(Object value) throws Exception;

    Number getLocalDateMonth(Object value) throws Exception;

    Number getLocalDateDay(Object value) throws Exception;

    Number getLocalDateTimeYear(Object value) throws Exception;

    Number getLocalDateTimeMonth(Object value) throws Exception;

    Number getLocalDateTimeDay(Object value) throws Exception;

    Number getLocalDateTimeHour(Object value) throws Exception;

    Number getLocalDateTimeMinute(Object value) throws Exception;

    Number getLocalDateTimeSecond(Object value) throws Exception;

    Number getLocalDateTimeNano(Object value) throws Exception;

    Number getLocalTimeHour(Object value) throws Exception;

    Number getLocalTimeMinute(Object value) throws Exception;

    Number getLocalTimeSecond(Object value) throws Exception;

    Number getLocalTimeNano(Object value) throws Exception;

    Number getInstantEpochMilli(Object value) throws Exception;

    Number getZonedDateTimeYear(Object value) throws Exception;

    Number getZonedDateTimeMonth(Object value) throws Exception;

    Number getZonedDateTimeDay(Object value) throws Exception;

    Number getZonedDateTimeHour(Object value) throws Exception;

    Number getZonedDateTimeMinute(Object value) throws Exception;

    Number getZonedDateTimeSecond(Object value) throws Exception;

    Number getZonedDateTimeNano(Object value) throws Exception;

    /**
     * 构造instant
     *
     * @param millis
     * @return
     * @throws Exception
     */
    Object ofInstant(long millis) throws Exception;

    /**
     * 构造ZonedDateTime对象
     *
     * @param year
     * @param month
     * @param dayOfMonth
     * @param hour
     * @param minute
     * @param second
     * @param nanoOfSecond
     * @param zoneId
     * @return
     * @throws Exception
     */
    Object ofZonedDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond, Object zoneId) throws Exception;

    /**
     * 解析字符串返回ZonedDateTime
     *
     * @param charSequence
     * @return
     * @throws Exception
     */
    Object parseZonedDateTime(CharSequence charSequence) throws Exception;

    /**
     * 构造LocalDateTime对象
     *
     * @param year
     * @param month
     * @param dayOfMonth
     * @param hour
     * @param minute
     * @param second
     * @param nanoOfSecond
     * @return
     * @throws Exception
     */
    Object ofLocalDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond) throws Exception;

    /**
     * 构造LocalTime对象
     *
     * @param hour
     * @param minute
     * @param second
     * @param nanoOfSecond
     * @return
     * @throws Exception
     */
    Object ofLocalTime(int hour, int minute, int second, int nanoOfSecond) throws Exception;

    /**
     * 构造LocalDate对象
     *
     * @param year
     * @param month
     * @param day
     * @return
     * @throws Exception
     */
    Object ofLocalDate(int year, int month, int day) throws Exception;
}
