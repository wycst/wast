//package io.github.wycst.wast.json.temporal;
//
//import io.github.wycst.wast.common.reflect.UnsafeHelper;
//
//import java.time.ZoneId;
//import java.time.ZoneOffset;
//import java.util.Map;
//import java.util.TimeZone;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * @Author: wangy
// * @Date: 2022/8/14 17:15
// * @Description:
// */
//public class TemporalAloneInvoker {
//
//
//    // 注：全局可变
//    private static TimeZone defaultTimezone = UnsafeHelper.getDefaultTimeZone();
//    private static ZoneId defaultZoneId = defaultTimezone.toZoneId();
//    private static final ZoneId ZERO = ZoneId.of("Z");
//    private static final ZoneOffset DEFAULT_ZONE_OFFSET = (ZoneOffset) ZERO;
//
//    private static Map<String, ZoneId> zoneIdMap = new ConcurrentHashMap<String, ZoneId>();
//
//    public static Object getDefaultZoneId() throws Exception {
//        TimeZone timeZone = UnsafeHelper.getDefaultTimeZone();
//        if (timeZone == defaultTimezone) {
//            return defaultZoneId;
//        }
//        defaultTimezone = timeZone;
//        return defaultZoneId = defaultTimezone.toZoneId();
//    }
//
//    public static Object getDefaultZoneOffset() {
//        return DEFAULT_ZONE_OFFSET;
//    }
//
//    public static ZoneId getZeroZoneId() {
//        return ZERO;
//    }
//
//    public static ZoneId ofZoneId(String zoneId) throws Exception {
//        ZoneId value = zoneIdMap.get(zoneId);
//        if (value == null) {
//            value = ZoneId.of(zoneId);
//            zoneIdMap.put(zoneId, value);
//        }
//        return value;
//    }
//
////    private static final TemporalInterface temporalInstance;
////
////    static {
////        String temporalInterfaceImplClassName = TemporalInterface.class.getPackage().getName() + ".TemporalInterfaceImplProvider";
////        TemporalInterface instance;
////        try {
////            Class<?> temporalImpl = Class.forName(temporalInterfaceImplClassName);
////            instance = (TemporalInterface) temporalImpl.newInstance();
////        } catch (Throwable e) {
////            throw new UnsupportedOperationException("temporal not supported");
////        }
////        temporalInstance = instance;
////    }
////
////    /**
////     * 构造LocalDate对象(无视时区)
////     *
////     * @param year
////     * @param month
////     * @param day
////     * @return
////     * @throws Exception
////     */
////    protected static Object ofLocalDate(int year, int month, int day) throws Exception {
////        return temporalInstance.ofLocalDate(year, month, day);
////    }
////
////    /**
////     * 构造LocalTime对象(无视时区)
////     *
////     * @param hour
////     * @param minute
////     * @param second
////     * @param nanoOfSecond
////     * @return
////     * @throws Exception
////     */
////    protected static Object ofLocalTime(int hour, int minute, int second, int nanoOfSecond) throws Exception {
////        return temporalInstance.ofLocalTime(hour, minute, second, nanoOfSecond);
////    }
////
////    /**
////     * 构造LocalDateTime对象(无视时区)
////     *
////     * @param year
////     * @param month
////     * @param dayOfMonth
////     * @param hour
////     * @param minute
////     * @param second
////     * @param nanoOfSecond
////     * @return
////     * @throws Exception
////     */
////    protected static Object ofLocalDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond) throws Exception {
////        return temporalInstance.ofLocalDateTime(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
////    }
////
////    /**
////     * 构造ZonedDateTime对象
////     * <p>
////     * Obtains an instance of ZonedDateTime from a text string such as 2007-12-03T10:15:30+01:00[Europe/Paris].
////     * The string must represent a valid date-time and is parsed using DateTimeFormatter.ISO_ZONED_DATE_TIME.
////     *
////     * @param charSequence
////     * @return
////     * @throws Exception
////     */
////    protected static Object parseZonedDateTime(CharSequence charSequence) throws Exception {
////        return temporalInstance.parseZonedDateTime(charSequence);
////    }
////
////    /**
////     * 构造OffsetDateTime对象
////     * <p>
////     * Obtains an instance of ZonedDateTime from a text string such as 2007-12-03T10:15:30+01:00[Europe/Paris].
////     * The string must represent a valid date-time and is parsed using DateTimeFormatter.ISO_ZONED_DATE_TIME.
////     *
////     * @param charSequence
////     * @return
////     * @throws Exception
////     */
////    protected static Object parseOffsetDateTime(CharSequence charSequence) throws Exception {
////        return temporalInstance.parseOffsetDateTime(charSequence);
////    }
////
////    public static Object parseLocalDate(CharSequence value) {
////        return temporalInstance.parseLocalDate(value);
////    }
////
////    public static Object parseLocalDateTime(CharSequence value) {
////        return temporalInstance.parseLocalDateTime(value);
////    }
////
////    public static Object parseLocalTime(CharSequence value) {
////        return temporalInstance.parseLocalTime(value);
////    }
////
////    public static Object parseInstant(CharSequence value) {
////        return temporalInstance.parseInstant(value);
////    }
////
////    /**
////     * 构造ZonedDateTime对象
////     *
////     * @param year
////     * @param month
////     * @param dayOfMonth
////     * @param hour
////     * @param minute
////     * @param second
////     * @param nanoOfSecond
////     * @param zoneId
////     * @return
////     * @throws Exception
////     */
////    protected static Object ofZonedDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond, Object zoneId) throws Exception {
//////        return staticZonedDateTimeOf.invoke(null, year, month, dayOfMonth, hour, minute, second, nanoOfSecond, zoneId);
////        return temporalInstance.ofZonedDateTime(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, zoneId);
////    }
////
////    /**
////     * 构造OffsetDateTime对象
////     *
////     * @param year
////     * @param month
////     * @param dayOfMonth
////     * @param hour
////     * @param minute
////     * @param second
////     * @param nanoOfSecond
////     * @param zoneId
////     * @return
////     * @throws Exception
////     */
////    protected static Object ofOffsetDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond, Object zoneId) throws Exception {
////        return temporalInstance.ofOffsetDateTime(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, zoneId);
////    }
////
////    /**
////     * 构造Instant对象
////     *
////     * @param millis
////     * @return
////     * @throws Exception
////     */
////    protected static Object createOrOfInstant(long millis) throws Exception {
////        return temporalInstance.ofInstant(millis);
////    }
////
////    public static Object ofEpochSecondInstant(long seconds, int nanoOfSecond) {
////        return temporalInstance.ofInstant(seconds, nanoOfSecond);
////    }
////
////    /**
////     * 构造ZoneId对象
////     *
////     * @param zoneId
////     * @return
////     * @throws Exception
////     */
////    protected static Object ofZoneId(String zoneId) throws Exception {
////        return temporalInstance.ofZoneId(zoneId);
////    }
////
////    public static int invokeLocalDateYear(Object value) throws Exception {
////        return temporalInstance.getLocalDateYear(value);
////    }
////
////    public static int invokeLocalDateMonth(Object value) throws Exception {
////        return temporalInstance.getLocalDateMonth(value);
////    }
////
////    public static int invokeLocalDateDay(Object value) throws Exception {
////        return temporalInstance.getLocalDateDay(value);
////    }
////
////    public static int invokeLocalDateTimeYear(Object value) throws Exception {
////        return temporalInstance.getLocalDateTimeYear(value);
////    }
////
////    public static int invokeLocalDateTimeMonth(Object value) throws Exception {
////        return temporalInstance.getLocalDateTimeMonth(value);
////    }
////
////    public static int invokeLocalDateTimeDay(Object value) throws Exception {
////        return temporalInstance.getLocalDateTimeDay(value);
////    }
////
////    public static int invokeLocalDateTimeHour(Object value) throws Exception {
////        return temporalInstance.getLocalDateTimeHour(value);
////    }
////
////    public static int invokeLocalDateTimeMinute(Object value) throws Exception {
////        return temporalInstance.getLocalDateTimeMinute(value);
////    }
////
////    public static int invokeLocalDateTimeSecond(Object value) throws Exception {
////        return temporalInstance.getLocalDateTimeSecond(value);
////    }
////
////    public static int invokeLocalDateTimeNano(Object value) throws Exception {
////        return temporalInstance.getLocalDateTimeNano(value);
////    }
////
////    public static int invokeLocalTimeHour(Object value) throws Exception {
////        return temporalInstance.getLocalTimeHour(value);
////    }
////
////    public static int invokeLocalTimeMinute(Object value) throws Exception {
////        return temporalInstance.getLocalTimeMinute(value);
////    }
////
////    public static int invokeLocalTimeSecond(Object value) throws Exception {
////        return temporalInstance.getLocalTimeSecond(value);
////    }
////
////    public static int invokeLocalTimeNano(Object value) throws Exception {
////        return temporalInstance.getLocalTimeNano(value);
////    }
////
////    public static long invokeInstantEpochMilli(Object value) throws Exception {
////        return temporalInstance.getInstantEpochMilli(value);
////    }
////
////    public static long invokeInstantEpochSeconds(Object value) throws Exception {
////        return temporalInstance.getInstantEpochSeconds(value);
////    }
////
////    public static int invokeInstantNano(Object value) throws Exception {
////        return temporalInstance.getInstantNano(value);
////    }
////
////    public static int invokeZonedDateTimeYear(Object value) throws Exception {
////        return temporalInstance.getZonedDateTimeYear(value);
////    }
////
////    public static int invokeZonedDateTimeMonth(Object value) throws Exception {
////        return temporalInstance.getZonedDateTimeMonth(value);
////    }
////
////    public static int invokeZonedDateTimeDay(Object value) throws Exception {
////        return temporalInstance.getZonedDateTimeDay(value);
////    }
////
////    public static int invokeZonedDateTimeHour(Object value) throws Exception {
////        return temporalInstance.getZonedDateTimeHour(value);
////    }
////
////    public static int invokeZonedDateTimeMinute(Object value) throws Exception {
////        return temporalInstance.getZonedDateTimeMinute(value);
////    }
////
////    public static int invokeZonedDateTimeSecond(Object value) throws Exception {
////        return temporalInstance.getZonedDateTimeSecond(value);
////    }
////
////    public static int invokeZonedDateTimeNano(Object value) throws Exception {
////        return temporalInstance.getZonedDateTimeNano(value);
////    }
////
////    public static Object invokeZonedDateTimeZone(Object value) throws Exception {
////        return temporalInstance.getZoneId(value);
////    }
////
////    public static int invokeOffsetDateTimeYear(Object value) throws Exception {
////        return temporalInstance.getOffsetDateTimeYear(value);
////    }
////
////    public static int invokeOffsetDateTimeMonth(Object value) throws Exception {
////        return temporalInstance.getOffsetDateTimeMonth(value);
////    }
////
////    public static int invokeOffsetDateTimeDay(Object value) throws Exception {
////        return temporalInstance.getOffsetDateTimeDay(value);
////    }
////
////    public static int invokeOffsetDateTimeHour(Object value) throws Exception {
////        return temporalInstance.getOffsetDateTimeHour(value);
////    }
////
////    public static int invokeOffsetDateTimeMinute(Object value) throws Exception {
////        return temporalInstance.getOffsetDateTimeMinute(value);
////    }
////
////    public static int invokeOffsetDateTimeSecond(Object value) throws Exception {
////        return temporalInstance.getOffsetDateTimeSecond(value);
////    }
////
////    public static int invokeOffsetDateTimeNano(Object value) throws Exception {
////        return temporalInstance.getOffsetDateTimeNano(value);
////    }
////
////    public static Object invokeOffsetDateTimeZone(Object value) throws Exception {
////        return temporalInstance.getOffsetZoneId(value);
////    }
////
////    public static Object getDefaultZoneId() throws Exception {
////        return temporalInstance.getDefaultZoneId();
////    }
////
////    public static Object getZeroZoneId() throws Exception {
////        return temporalInstance.getZeroZoneId();
////    }
////
////    public static Object getDefaultZoneOffset() {
////        return temporalInstance.getDefaultZoneOffset();
////    }
//}