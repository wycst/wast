package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.time.*;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提供Temporal的api访问
 * 注：此类需通过类加载器加载
 *
 * @Author: wangy
 * @Date: 2022/8/21 0:35
 * @Description:
 */
class TemporalInterfaceImplProvider implements TemporalInterface {

    // 注：全局可变
    private TimeZone defaultTimezone = UnsafeHelper.getDefaultTimeZone();
    private ZoneId defaultZoneId = defaultTimezone.toZoneId();

    private static Map<String, ZoneId> zoneIdMap = new ConcurrentHashMap<String, ZoneId>();

    @Override
    public Object getDefaultZoneId() throws Exception {
        TimeZone timeZone = UnsafeHelper.getDefaultTimeZone();
        if (timeZone == defaultTimezone) {
            return defaultZoneId;
        }
        defaultTimezone = timeZone;
        return defaultZoneId = defaultTimezone.toZoneId();
    }

    @Override
    public Object getZoneId(Object zonedDateTime) throws Exception {
        ZonedDateTime zonedDateTime0 = (ZonedDateTime) zonedDateTime;
        return zonedDateTime0.getZone();
    }

    @Override
    public Object ofZoneId(String zoneId) throws Exception {
        ZoneId value = zoneIdMap.get(zoneId);
        if(value == null) {
            value = ZoneId.of(zoneId);
            zoneIdMap.put(zoneId, value);
        }
        return value;
    }

    @Override
    public Number getLocalDateYear(Object value) throws Exception {
        LocalDate localDate = (LocalDate) value;
        return localDate.getYear();
    }

    @Override
    public Number getLocalDateMonth(Object value) throws Exception {
        LocalDate localDate = (LocalDate) value;
        return localDate.getMonthValue();
    }

    @Override
    public Number getLocalDateDay(Object value) throws Exception {
        LocalDate localDate = (LocalDate) value;
        return localDate.getDayOfMonth();
    }

    @Override
    public Number getLocalDateTimeYear(Object value) throws Exception {
        LocalDateTime localDateTime = (LocalDateTime) value;
        return localDateTime.getYear();
    }

    @Override
    public Number getLocalDateTimeMonth(Object value) throws Exception {
        LocalDateTime localDateTime = (LocalDateTime) value;
        return localDateTime.getMonthValue();
    }

    @Override
    public Number getLocalDateTimeDay(Object value) throws Exception {
        LocalDateTime localDateTime = (LocalDateTime) value;
        return localDateTime.getDayOfMonth();
    }

    @Override
    public Number getLocalDateTimeHour(Object value) throws Exception {
        LocalDateTime localDateTime = (LocalDateTime) value;
        return localDateTime.getHour();
    }

    @Override
    public Number getLocalDateTimeMinute(Object value) throws Exception {
        LocalDateTime localDateTime = (LocalDateTime) value;
        return localDateTime.getMinute();
    }

    @Override
    public Number getLocalDateTimeSecond(Object value) throws Exception {
        LocalDateTime localDateTime = (LocalDateTime) value;
        return localDateTime.getSecond();
    }

    @Override
    public Number getLocalDateTimeNano(Object value) throws Exception {
        LocalDateTime localDateTime = (LocalDateTime) value;
        return localDateTime.getNano();
    }

    @Override
    public Number getLocalTimeHour(Object value) throws Exception {
        LocalTime localTime = (LocalTime) value;
        return localTime.getHour();
    }

    @Override
    public Number getLocalTimeMinute(Object value) throws Exception {
        LocalTime localTime = (LocalTime) value;
        return localTime.getMinute();
    }

    @Override
    public Number getLocalTimeSecond(Object value) throws Exception {
        LocalTime localTime = (LocalTime) value;
        return localTime.getSecond();
    }

    @Override
    public Number getLocalTimeNano(Object value) throws Exception {
        LocalTime localTime = (LocalTime) value;
        return localTime.getNano();
    }

    @Override
    public Number getInstantEpochMilli(Object value) throws Exception {
        Instant instant = (Instant) value;
        return instant.toEpochMilli();
    }

    @Override
    public Number getZonedDateTimeYear(Object value) throws Exception {
        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
        return zonedDateTime.getYear();
    }

    @Override
    public Number getZonedDateTimeMonth(Object value) throws Exception {
        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
        return zonedDateTime.getMonthValue();
    }

    @Override
    public Number getZonedDateTimeDay(Object value) throws Exception {
        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
        return zonedDateTime.getDayOfMonth();
    }

    @Override
    public Number getZonedDateTimeHour(Object value) throws Exception {
        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
        return zonedDateTime.getHour();
    }

    @Override
    public Number getZonedDateTimeMinute(Object value) throws Exception {
        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
        return zonedDateTime.getMinute();
    }

    @Override
    public Number getZonedDateTimeSecond(Object value) throws Exception {
        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
        return zonedDateTime.getSecond();
    }

    @Override
    public Number getZonedDateTimeNano(Object value) throws Exception {
        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
        return zonedDateTime.getNano();
    }

    @Override
    public Object ofInstant(long millis) throws Exception {
        return Instant.ofEpochMilli(millis);
    }

    @Override
    public Object ofZonedDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond, Object zoneId) throws Exception {
        return ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, (ZoneId) zoneId);
    }

    @Override
    public Object parseZonedDateTime(CharSequence charSequence) throws Exception {
        return ZonedDateTime.parse(charSequence);
    }

    @Override
    public Object ofLocalDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond) throws Exception {
        return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
    }

    @Override
    public Object ofLocalTime(int hour, int minute, int second, int nanoOfSecond) throws Exception {
        return LocalTime.of(hour, minute, second, nanoOfSecond);
    }

    @Override
    public Object ofLocalDate(int year, int month, int day) throws Exception {
        return LocalDate.of(year, month, day);
    }
}
