package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.lang.reflect.Method;
import java.util.TimeZone;

/**
 * @Author: wangy
 * @Date: 2022/8/14 17:15
 * @Description:
 */
public class TemporalAloneInvoker {

    private static final TemporalInterface temporalInstance;

    protected final static Class<?> localTimeClass;
    protected final static Class<?> localDateClass;
    protected final static Class<?> localDateTimeClass;
    protected final static Class<?> zonedDateTimeClass;
    protected final static Class<?> instantClass;
    protected final static Class<?> zoneIdClass;

    @Deprecated
    private final static Method staticLocalDateOf;
    @Deprecated
    private static Method localDateYear;
    @Deprecated
    private static Method localDateMonth;
    @Deprecated
    private static Method localDateDay;

    @Deprecated
    private final static Method staticLocalTimeOf;
    @Deprecated
    private static Method localTimeHour;
    @Deprecated
    private static Method localTimeMinute;
    @Deprecated
    private static Method localTimeSecond;
    @Deprecated
    private static Method localTimeNano;

    @Deprecated
    private final static Method staticLocalDateTimeOf;
    @Deprecated
    private static Method localDateTimeYear;
    @Deprecated
    private static Method localDateTimeMonth;
    @Deprecated
    private static Method localDateTimeDay;
    @Deprecated
    private static Method localDateTimeHour;
    @Deprecated
    private static Method localDateTimeMinute;
    @Deprecated
    private static Method localDateTimeSecond;
    @Deprecated
    private static Method localDateTimeNano;

    @Deprecated
    private final static Method staticZonedDateTimeParse;
    @Deprecated
    private final static Method staticZonedDateTimeOf;
    @Deprecated
    private static Method zonedDateTimeYear;
    @Deprecated
    private static Method zonedDateTimeMonth;
    @Deprecated
    private static Method zonedDateTimeDay;
    @Deprecated
    private static Method zonedDateTimeHour;
    @Deprecated
    private static Method zonedDateTimeMinute;
    @Deprecated
    private static Method zonedDateTimeSecond;
    @Deprecated
    private static Method zonedDateTimeNano;
    @Deprecated
    private static Method zonedDateTimeZone;

    private static Method staticInstantCreate;
    @Deprecated
    private static Method staticInstantOf;
    @Deprecated
    private static Method instantEpochMilli;

    @Deprecated
    private static Method staticZoneIdOf;
    @Deprecated
    private static Method staticToZoneId;


    static {

        String temporalInterfaceImplClassName = TemporalInterface.class.getPackage().getName() + ".TemporalInterfaceImplProvider";
        TemporalInterface instance = null;
        try {
            Class<?> temporalImpl = Class.forName(temporalInterfaceImplClassName);
            instance = (TemporalInterface) temporalImpl.newInstance();
        } catch (Throwable e) {
        }
        temporalInstance = instance;

        Class<?> localTimeCls = null;
        Class<?> localDateCls = null;
        Class<?> localDateTimeCls = null;
        Class<?> zonedDateTimeCls = null;
        Class<?> instantCls = null;
        Class<?> zoneIdCls = null;

        Method localTimeOf = null;
        Method localDateOf = null;
        Method localDateTimeOf = null;
        Method zonedDateTimeParse = null;
        Method zonedDateTimeOf = null;
        Method instantCreateOrParse = null;
        Method zoneIdOf = null;
        Method toZoneId = null;
        try {
            localTimeCls = Class.forName("java.time.LocalTime");
            localDateCls = Class.forName("java.time.LocalDate");
            localDateTimeCls = Class.forName("java.time.LocalDateTime");
            zonedDateTimeCls = Class.forName("java.time.ZonedDateTime");
            instantCls = Class.forName("java.time.Instant");
            zoneIdCls = Class.forName("java.time.ZoneId");

            // public static LocalTime of(int hour, int minute, int second, int nanoOfSecond)
            // nanoOfSecond = millisecond * 1000000;
            localTimeOf = localTimeCls.getMethod("of", int.class, int.class, int.class, int.class);
            localTimeHour = localTimeCls.getMethod("getHour");
            localTimeMinute = localTimeCls.getMethod("getMinute");
            localTimeSecond = localTimeCls.getMethod("getSecond");
            localTimeNano = localTimeCls.getMethod("getNano");

            // public static LocalDate of(int year, int month, int dayOfMonth)
            localDateOf = localDateCls.getMethod("of", int.class, int.class, int.class);
            localDateYear = localDateCls.getMethod("getYear");
            localDateMonth = localDateCls.getMethod("getMonthValue");
            localDateDay = localDateCls.getMethod("getDayOfMonth");

            // public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond)
            localDateTimeOf = localDateTimeCls.getMethod("of", int.class, int.class, int.class, int.class, int.class, int.class, int.class);
            localDateTimeYear = localDateTimeCls.getMethod("getYear");
            localDateTimeMonth = localDateTimeCls.getMethod("getMonthValue");
            localDateTimeDay = localDateTimeCls.getMethod("getDayOfMonth");
            localDateTimeHour = localDateTimeCls.getMethod("getHour");
            localDateTimeMinute = localDateTimeCls.getMethod("getMinute");
            localDateTimeSecond = localDateTimeCls.getMethod("getSecond");
            localDateTimeNano = localDateTimeCls.getMethod("getNano");

            // public static ZonedDateTime parse(CharSequence text)
            zonedDateTimeParse = zonedDateTimeCls.getMethod("parse", CharSequence.class);
            // public static ZonedDateTime of(int year, int month, int dayOfMonth,int hour, int minute, int second, int nanoOfSecond, ZoneId zone)
            zonedDateTimeOf = zonedDateTimeCls.getMethod("of", int.class, int.class, int.class, int.class, int.class, int.class, int.class, zoneIdCls);
            zonedDateTimeYear = zonedDateTimeCls.getMethod("getYear");
            zonedDateTimeMonth = zonedDateTimeCls.getMethod("getMonthValue");
            zonedDateTimeDay = zonedDateTimeCls.getMethod("getDayOfMonth");
            zonedDateTimeHour = zonedDateTimeCls.getMethod("getHour");
            zonedDateTimeMinute = zonedDateTimeCls.getMethod("getMinute");
            zonedDateTimeSecond = zonedDateTimeCls.getMethod("getSecond");
            zonedDateTimeNano = zonedDateTimeCls.getMethod("getNano");
            zonedDateTimeZone = zonedDateTimeCls.getMethod("getZone");

            try {
                // reflect private static Instant create(long seconds, int nanoOfSecond);
                instantCreateOrParse = instantCls.getMethod("create", long.class, int.class);
                staticInstantCreate = instantCreateOrParse;
            } catch (Throwable throwable) {
                // if exception reflect public static Instant ofEpochMilli(long epochMilli);
                instantCreateOrParse = instantCls.getMethod("ofEpochMilli", long.class);
                staticInstantOf = instantCreateOrParse;
            }

            // milli
            instantEpochMilli = instantCls.getMethod("toEpochMilli");

            // public static ZoneId of(String zoneId)
            zoneIdOf = zoneIdCls.getMethod("of", String.class);
            toZoneId = TimeZone.class.getMethod("toZoneId");

            if (!UnsafeHelper.setAccessible(localTimeOf)) {
                localTimeOf.setAccessible(true);
                localDateOf.setAccessible(true);
                localDateTimeOf.setAccessible(true);
                zonedDateTimeParse.setAccessible(true);
                zonedDateTimeOf.setAccessible(true);
                instantCreateOrParse.setAccessible(true);
                zoneIdOf.setAccessible(true);
                toZoneId.setAccessible(true);

                localTimeHour.setAccessible(true);
                localTimeMinute.setAccessible(true);
                localTimeSecond.setAccessible(true);
                localTimeNano.setAccessible(true);

                localDateYear.setAccessible(true);
                localDateMonth.setAccessible(true);
                localDateDay.setAccessible(true);

                localDateTimeYear.setAccessible(true);
                localDateTimeMonth.setAccessible(true);
                localDateTimeDay.setAccessible(true);
                localDateTimeHour.setAccessible(true);
                localDateTimeMinute.setAccessible(true);
                localDateTimeSecond.setAccessible(true);
                localDateTimeNano.setAccessible(true);

                instantEpochMilli.setAccessible(true);

                zonedDateTimeYear.setAccessible(true);
                zonedDateTimeMonth.setAccessible(true);
                zonedDateTimeDay.setAccessible(true);
                zonedDateTimeHour.setAccessible(true);
                zonedDateTimeMinute.setAccessible(true);
                zonedDateTimeSecond.setAccessible(true);
                zonedDateTimeNano.setAccessible(true);
                zonedDateTimeZone.setAccessible(true);
            } else {
                UnsafeHelper.setAccessible(localDateOf);
                UnsafeHelper.setAccessible(localDateTimeOf);
                UnsafeHelper.setAccessible(zonedDateTimeParse);
                UnsafeHelper.setAccessible(zonedDateTimeOf);
                UnsafeHelper.setAccessible(instantCreateOrParse);
                UnsafeHelper.setAccessible(zoneIdOf);
                UnsafeHelper.setAccessible(toZoneId);

                UnsafeHelper.setAccessibleList(localTimeHour, localTimeMinute, localTimeSecond, localTimeNano);
                UnsafeHelper.setAccessibleList(localDateYear, localDateMonth, localDateDay);
                UnsafeHelper.setAccessibleList(localDateTimeYear, localDateTimeMonth, localDateTimeDay, localDateTimeHour, localDateTimeMinute, localDateTimeSecond, localDateTimeNano);
                UnsafeHelper.setAccessibleList(instantEpochMilli);
                UnsafeHelper.setAccessibleList(zonedDateTimeYear, zonedDateTimeMonth, zonedDateTimeDay, zonedDateTimeHour, zonedDateTimeMinute, zonedDateTimeSecond, zonedDateTimeNano, zonedDateTimeZone);
            }
        } catch (Throwable e) {
            throw new UnsupportedOperationException("not support java.time.*");
        }
        localTimeClass = localTimeCls;
        localDateClass = localDateCls;
        localDateTimeClass = localDateTimeCls;
        zonedDateTimeClass = zonedDateTimeCls;
        instantClass = instantCls;
        zoneIdClass = zoneIdCls;

        staticLocalTimeOf = localTimeOf;
        staticLocalDateOf = localDateOf;
        staticLocalDateTimeOf = localDateTimeOf;
        staticZonedDateTimeParse = zonedDateTimeParse;
        staticZonedDateTimeOf = zonedDateTimeOf;
        staticZoneIdOf = zoneIdOf;
        staticToZoneId = toZoneId;
    }

    /**
     * 构造LocalDate对象(无视时区)
     *
     * @param year
     * @param month
     * @param day
     * @return
     * @throws Exception
     */
    protected static Object ofLocalDate(int year, int month, int day) throws Exception {
//        return staticLocalDateOf.invoke(null, year, month, day);
        return temporalInstance.ofLocalDate(year, month, day);
    }

    /**
     * 构造LocalTime对象(无视时区)
     *
     * @param hour
     * @param minute
     * @param second
     * @param nanoOfSecond
     * @return
     * @throws Exception
     */
    protected static Object ofLocalTime(int hour, int minute, int second, int nanoOfSecond) throws Exception {
//        return staticLocalTimeOf.invoke(null, hour, minute, second, nanoOfSecond);
        return temporalInstance.ofLocalTime(hour, minute, second, nanoOfSecond);
    }

    /**
     * 构造LocalDateTime对象(无视时区)
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
    protected static Object ofLocalDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond) throws Exception {
//        return staticLocalDateTimeOf.invoke(null, year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
        return temporalInstance.ofLocalDateTime(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
    }

    /**
     * 构造ZonedDateTime对象
     * <p>
     * Obtains an instance of ZonedDateTime from a text string such as 2007-12-03T10:15:30+01:00[Europe/Paris].
     * The string must represent a valid date-time and is parsed using DateTimeFormatter.ISO_ZONED_DATE_TIME.
     *
     * @param charSequence
     * @return
     * @throws Exception
     */
    protected static Object parseZonedDateTime(CharSequence charSequence) throws Exception {
//        return staticZonedDateTimeParse.invoke(null, charSequence);
        return temporalInstance.parseZonedDateTime(charSequence);
    }

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
    protected static Object ofZonedDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond, Object zoneId) throws Exception {
//        return staticZonedDateTimeOf.invoke(null, year, month, dayOfMonth, hour, minute, second, nanoOfSecond, zoneId);
        return temporalInstance.ofZonedDateTime(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, zoneId);
    }

    /**
     * 构造Instant对象
     *
     * @param millis
     * @return
     * @throws Exception
     */
    protected static Object createOrOfInstant(long millis) throws Exception {
        if (staticInstantCreate != null) {
            // Instant create(long seconds, int nanoOfSecond);
            long seconds = millis / 1000;
            int ms = (int) millis % 1000;
            int nanoOfSecond = ms * 1000000;
            return staticInstantCreate.invoke(null, seconds, nanoOfSecond);
        } else {
//            return staticInstantOf.invoke(null, millis);
            return temporalInstance.ofInstant(millis);
        }
    }

    /**
     * 构造ZoneId对象
     *
     * @param zoneId
     * @return
     * @throws Exception
     */
    protected static Object ofZoneId(String zoneId) throws Exception {
//        return staticZoneIdOf.invoke(null, zoneId);
        return temporalInstance.ofZoneId(zoneId);
    }

    public static Number invokeLocalDateYear(Object value) throws Exception {
//        return (Number) localDateYear.invoke(value);
        return temporalInstance.getLocalDateYear(value);
    }

    public static Number invokeLocalDateMonth(Object value) throws Exception {
//        return (Number) localDateMonth.invoke(value);
        return temporalInstance.getLocalDateMonth(value);
    }

    public static Number invokeLocalDateDay(Object value) throws Exception {
//        return (Number) localDateDay.invoke(value);
        return temporalInstance.getLocalDateDay(value);
    }

    public static Number invokeLocalDateTimeYear(Object value) throws Exception {
//        return (Number) localDateTimeYear.invoke(value);
        return temporalInstance.getLocalDateTimeYear(value);
    }

    public static Number invokeLocalDateTimeMonth(Object value) throws Exception {
//        return (Number) localDateTimeMonth.invoke(value);
        return temporalInstance.getLocalDateTimeMonth(value);
    }

    public static Number invokeLocalDateTimeDay(Object value) throws Exception {
//        return (Number) localDateTimeDay.invoke(value);
        return temporalInstance.getLocalDateTimeDay(value);
    }

    public static Number invokeLocalDateTimeHour(Object value) throws Exception {
//        return (Number) localDateTimeHour.invoke(value);
        return temporalInstance.getLocalDateTimeHour(value);
    }

    public static Number invokeLocalDateTimeMinute(Object value) throws Exception {
//        return (Number) localDateTimeMinute.invoke(value);
        return temporalInstance.getLocalDateTimeMinute(value);
    }

    public static Number invokeLocalDateTimeSecond(Object value) throws Exception {
//        return (Number) localDateTimeSecond.invoke(value);
        return temporalInstance.getLocalDateTimeSecond(value);
    }

    public static Number invokeLocalDateTimeNano(Object value) throws Exception {
//        return (Number) localDateTimeNano.invoke(value);
        return temporalInstance.getLocalDateTimeNano(value);
    }

    public static Number invokeLocalTimeHour(Object value) throws Exception {
//        return (Number) localTimeHour.invoke(value);
        return temporalInstance.getLocalTimeHour(value);
    }

    public static Number invokeLocalTimeMinute(Object value) throws Exception {
//        return (Number) localTimeMinute.invoke(value);
        return temporalInstance.getLocalTimeMinute(value);
    }

    public static Number invokeLocalTimeSecond(Object value) throws Exception {
//        return (Number) localTimeSecond.invoke(value);
        return temporalInstance.getLocalTimeSecond(value);
    }

    public static Number invokeLocalTimeNano(Object value) throws Exception {
//        return (Number) localTimeNano.invoke(value);
        return temporalInstance.getLocalTimeNano(value);
    }

    public static Number invokeInstantEpochMilli(Object value) throws Exception {
//        return (Number) instantEpochMilli.invoke(value);
        return temporalInstance.getInstantEpochMilli(value);
    }

    public static Number invokeZonedDateTimeYear(Object value) throws Exception {
//        return (Number) zonedDateTimeYear.invoke(value);
        return temporalInstance.getZonedDateTimeYear(value);
    }

    public static Number invokeZonedDateTimeMonth(Object value) throws Exception {
//        return (Number) zonedDateTimeMonth.invoke(value);
        return temporalInstance.getZonedDateTimeMonth(value);
    }

    public static Number invokeZonedDateTimeDay(Object value) throws Exception {
//        return (Number) zonedDateTimeDay.invoke(value);
        return temporalInstance.getZonedDateTimeDay(value);
    }

    public static Number invokeZonedDateTimeHour(Object value) throws Exception {
//        return (Number) zonedDateTimeHour.invoke(value);
        return temporalInstance.getZonedDateTimeHour(value);
    }

    public static Number invokeZonedDateTimeMinute(Object value) throws Exception {
//        return (Number) zonedDateTimeMinute.invoke(value);
        return temporalInstance.getZonedDateTimeMinute(value);
    }

    public static Number invokeZonedDateTimeSecond(Object value) throws Exception {
//        return (Number) zonedDateTimeSecond.invoke(value);
        return temporalInstance.getZonedDateTimeSecond(value);
    }

    public static Number invokeZonedDateTimeNano(Object value) throws Exception {
//        return (Number) zonedDateTimeNano.invoke(value);
        return temporalInstance.getZonedDateTimeNano(value);
    }

    public static Object invokeZonedDateTimeZone(Object value) throws Exception {
//        return zonedDateTimeZone.invoke(value);
        return temporalInstance.getZoneId(value);
    }

    public static Object getDefaultZoneId() throws Exception {
        return temporalInstance.getDefaultZoneId();
    }

}