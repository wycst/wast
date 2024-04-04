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
     * 获取默认的zoneOffset
     *
     * @return
     */
    Object getDefaultZoneOffset();

    /**
     * 获取Z
     * @return
     */
    Object getZeroZoneId();

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

    int getLocalDateYear(Object value) throws Exception;

    int getLocalDateMonth(Object value) throws Exception;

    int getLocalDateDay(Object value) throws Exception;

    int getLocalDateTimeYear(Object value) throws Exception;

    int getLocalDateTimeMonth(Object value) throws Exception;

    int getLocalDateTimeDay(Object value) throws Exception;

    int getLocalDateTimeHour(Object value) throws Exception;

    int getLocalDateTimeMinute(Object value) throws Exception;

    int getLocalDateTimeSecond(Object value) throws Exception;

    int getLocalDateTimeNano(Object value) throws Exception;

    int getLocalTimeHour(Object value) throws Exception;

    int getLocalTimeMinute(Object value) throws Exception;

    int getLocalTimeSecond(Object value) throws Exception;

    int getLocalTimeNano(Object value) throws Exception;

    long getInstantEpochMilli(Object value) throws Exception;

    long getInstantEpochSeconds(Object value) throws Exception;

    int getInstantNano(Object value) throws Exception;

    int getZonedDateTimeYear(Object value) throws Exception;

    int getZonedDateTimeMonth(Object value) throws Exception;

    int getZonedDateTimeDay(Object value) throws Exception;

    int getZonedDateTimeHour(Object value) throws Exception;

    int getZonedDateTimeMinute(Object value) throws Exception;

    int getZonedDateTimeSecond(Object value) throws Exception;

    int getZonedDateTimeNano(Object value) throws Exception;

    int getOffsetDateTimeYear(Object value) throws Exception;

    int getOffsetDateTimeMonth(Object value) throws Exception;

    int getOffsetDateTimeDay(Object value) throws Exception;

    int getOffsetDateTimeHour(Object value) throws Exception;

    int getOffsetDateTimeMinute(Object value) throws Exception;

    int getOffsetDateTimeSecond(Object value) throws Exception;

    int getOffsetDateTimeNano(Object value) throws Exception;

    /**
     * 获取zoneId
     *
     * @param value
     * @return
     */
    Object getOffsetZoneId(Object value) throws Exception;

    /**
     * 构造instant
     *
     * @param millis
     * @return
     * @throws Exception
     */
    Object ofInstant(long millis) throws Exception;

    /**
     * 通过秒和纳秒构建
     *
     * @param seconds
     * @param nanoOfSecond
     * @return
     */
    Object ofInstant(long seconds, int nanoOfSecond);

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
     * 构造ZonedDateTime对象
     *
     * @param year
     * @param month
     * @param dayOfMonth
     * @param hour
     * @param minute
     * @param second
     * @param nanoOfSecond
     * @param zoneOffset
     * @return
     * @throws Exception
     */
    Object ofOffsetDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond, Object zoneOffset) throws Exception;


    /**
     * 解析字符串返回ZonedDateTime
     *
     * @param charSequence
     * @return
     * @throws Exception
     */
    Object parseZonedDateTime(CharSequence charSequence) throws Exception;

    /**
     * 解析字符串返回OffsetDateTime
     *
     * @param charSequence
     * @return
     * @throws Exception
     */
    Object parseOffsetDateTime(CharSequence charSequence);

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

    /**
     * 通过字符串构建LocalDate对象
     *
     * @param value
     * @return
     */
    Object parseLocalDate(CharSequence value);

    Object parseLocalDateTime(CharSequence value);

    Object parseLocalTime(CharSequence value);

    Object parseInstant(CharSequence value);
}
