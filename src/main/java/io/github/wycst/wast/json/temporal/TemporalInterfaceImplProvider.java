//package io.github.wycst.wast.json.temporal;
//
//import io.github.wycst.wast.common.reflect.UnsafeHelper;
//
//import java.time.*;
//import java.util.Map;
//import java.util.TimeZone;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * 提供Temporal的api访问
// * 注：此类需通过类加载器加载
// *
// * @Author: wangy
// * @Date: 2022/8/21 0:35
// * @Description:
// */
//class TemporalInterfaceImplProvider implements TemporalInterface {
//
//    // 注：全局可变
//    private static TimeZone defaultTimezone = UnsafeHelper.getDefaultTimeZone();
//    private static ZoneId defaultZoneId = defaultTimezone.toZoneId();
//    private static final ZoneId ZERO = ZoneId.of("Z");
//    private static final ZoneOffset DEFAULT_ZONE_OFFSET = (ZoneOffset) ZERO;
//
//    private static Map<String, ZoneId> zoneIdMap = new ConcurrentHashMap<String, ZoneId>();
//
//    @Override
//    public Object getDefaultZoneId() throws Exception {
//        TimeZone timeZone = UnsafeHelper.getDefaultTimeZone();
//        if (timeZone == defaultTimezone) {
//            return defaultZoneId;
//        }
//        defaultTimezone = timeZone;
//        return defaultZoneId = defaultTimezone.toZoneId();
//    }
//
//    @Override
//    public Object getDefaultZoneOffset() {
//        return DEFAULT_ZONE_OFFSET;
//    }
//
//    @Override
//    public ZoneId getZeroZoneId() {
//        return ZERO;
//    }
//
//    @Override
//    public Object getZoneId(Object zonedDateTime) throws Exception {
//        ZonedDateTime zonedDateTime0 = (ZonedDateTime) zonedDateTime;
//        return zonedDateTime0.getZone();
//    }
//
//    @Override
//    public Object ofZoneId(String zoneId) throws Exception {
//        ZoneId value = zoneIdMap.get(zoneId);
//        if (value == null) {
//            value = ZoneId.of(zoneId);
//            zoneIdMap.put(zoneId, value);
//        }
//        return value;
//    }
//
//    @Override
//    public int getLocalDateYear(Object value) throws Exception {
//        LocalDate localDate = (LocalDate) value;
//        return localDate.getYear();
//    }
//
//    @Override
//    public int getLocalDateMonth(Object value) throws Exception {
//        LocalDate localDate = (LocalDate) value;
//        return localDate.getMonthValue();
//    }
//
//    @Override
//    public int getLocalDateDay(Object value) throws Exception {
//        LocalDate localDate = (LocalDate) value;
//        return localDate.getDayOfMonth();
//    }
//
//    @Override
//    public int getLocalDateTimeYear(Object value) throws Exception {
//        LocalDateTime localDateTime = (LocalDateTime) value;
//        return localDateTime.getYear();
//    }
//
//    @Override
//    public int getLocalDateTimeMonth(Object value) throws Exception {
//        LocalDateTime localDateTime = (LocalDateTime) value;
//        return localDateTime.getMonthValue();
//    }
//
//    @Override
//    public int getLocalDateTimeDay(Object value) throws Exception {
//        LocalDateTime localDateTime = (LocalDateTime) value;
//        return localDateTime.getDayOfMonth();
//    }
//
//    @Override
//    public int getLocalDateTimeHour(Object value) throws Exception {
//        LocalDateTime localDateTime = (LocalDateTime) value;
//        return localDateTime.getHour();
//    }
//
//    @Override
//    public int getLocalDateTimeMinute(Object value) throws Exception {
//        LocalDateTime localDateTime = (LocalDateTime) value;
//        return localDateTime.getMinute();
//    }
//
//    @Override
//    public int getLocalDateTimeSecond(Object value) throws Exception {
//        LocalDateTime localDateTime = (LocalDateTime) value;
//        return localDateTime.getSecond();
//    }
//
//    @Override
//    public int getLocalDateTimeNano(Object value) throws Exception {
//        LocalDateTime localDateTime = (LocalDateTime) value;
//        return localDateTime.getNano();
//    }
//
//    @Override
//    public int getLocalTimeHour(Object value) throws Exception {
//        LocalTime localTime = (LocalTime) value;
//        return localTime.getHour();
//    }
//
//    @Override
//    public int getLocalTimeMinute(Object value) throws Exception {
//        LocalTime localTime = (LocalTime) value;
//        return localTime.getMinute();
//    }
//
//    @Override
//    public int getLocalTimeSecond(Object value) throws Exception {
//        LocalTime localTime = (LocalTime) value;
//        return localTime.getSecond();
//    }
//
//    @Override
//    public int getLocalTimeNano(Object value) throws Exception {
//        LocalTime localTime = (LocalTime) value;
//        return localTime.getNano();
//    }
//
//    @Override
//    public long getInstantEpochMilli(Object value) throws Exception {
//        Instant instant = (Instant) value;
//        return instant.toEpochMilli();
//    }
//
//    @Override
//    public long getInstantEpochSeconds(Object value) throws Exception {
//        Instant instant = (Instant) value;
//        return instant.getEpochSecond();
//    }
//
//    @Override
//    public int getInstantNano(Object value) throws Exception {
//        Instant instant = (Instant) value;
//        return instant.getNano();
//    }
//
//    @Override
//    public int getZonedDateTimeYear(Object value) throws Exception {
//        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
//        return zonedDateTime.getYear();
//    }
//
//    @Override
//    public int getZonedDateTimeMonth(Object value) throws Exception {
//        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
//        return zonedDateTime.getMonthValue();
//    }
//
//    @Override
//    public int getZonedDateTimeDay(Object value) throws Exception {
//        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
//        return zonedDateTime.getDayOfMonth();
//    }
//
//    @Override
//    public int getZonedDateTimeHour(Object value) throws Exception {
//        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
//        return zonedDateTime.getHour();
//    }
//
//    @Override
//    public int getZonedDateTimeMinute(Object value) throws Exception {
//        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
//        return zonedDateTime.getMinute();
//    }
//
//    @Override
//    public int getZonedDateTimeSecond(Object value) throws Exception {
//        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
//        return zonedDateTime.getSecond();
//    }
//
//    @Override
//    public int getZonedDateTimeNano(Object value) throws Exception {
//        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
//        return zonedDateTime.getNano();
//    }
//
//    @Override
//    public int getOffsetDateTimeYear(Object value) throws Exception {
//        OffsetDateTime offsetDateTime = (OffsetDateTime) value;
//        return offsetDateTime.getYear();
//    }
//
//    @Override
//    public int getOffsetDateTimeMonth(Object value) throws Exception {
//        OffsetDateTime offsetDateTime = (OffsetDateTime) value;
//        return offsetDateTime.getMonthValue();
//    }
//
//    @Override
//    public int getOffsetDateTimeDay(Object value) throws Exception {
//        OffsetDateTime offsetDateTime = (OffsetDateTime) value;
//        return offsetDateTime.getDayOfMonth();
//    }
//
//    @Override
//    public int getOffsetDateTimeHour(Object value) throws Exception {
//        OffsetDateTime offsetDateTime = (OffsetDateTime) value;
//        return offsetDateTime.getHour();
//    }
//
//    @Override
//    public int getOffsetDateTimeMinute(Object value) throws Exception {
//        OffsetDateTime offsetDateTime = (OffsetDateTime) value;
//        return offsetDateTime.getMinute();
//    }
//
//    @Override
//    public int getOffsetDateTimeSecond(Object value) throws Exception {
//        OffsetDateTime offsetDateTime = (OffsetDateTime) value;
//        return offsetDateTime.getSecond();
//    }
//
//    @Override
//    public int getOffsetDateTimeNano(Object value) throws Exception {
//        OffsetDateTime offsetDateTime = (OffsetDateTime) value;
//        return offsetDateTime.getNano();
//    }
//
//    @Override
//    public Object getOffsetZoneId(Object value) throws Exception {
//        OffsetDateTime offsetDateTime = (OffsetDateTime) value;
//        return offsetDateTime.getOffset();
//    }
//
//    @Override
//    public Object ofInstant(long millis) throws Exception {
//        return Instant.ofEpochMilli(millis);
//    }
//
//    @Override
//    public Object ofInstant(long seconds, int nanoOfSecond) {
//        return Instant.ofEpochSecond(seconds, nanoOfSecond);
//    }
//
//    @Override
//    public Object ofZonedDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond, Object zoneId) throws Exception {
//        return ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, (ZoneId) zoneId);
//    }
//
//    @Override
//    public Object ofOffsetDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond, Object zoneOffset) throws Exception {
//        return OffsetDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, (ZoneOffset) zoneOffset);
//    }
//
//    @Override
//    public Object parseZonedDateTime(CharSequence charSequence) throws Exception {
//        return ZonedDateTime.parse(charSequence);
//    }
//
//    @Override
//    public Object parseOffsetDateTime(CharSequence charSequence) {
//        return OffsetDateTime.parse(charSequence);
//    }
//
//    @Override
//    public LocalDate parseLocalDate(CharSequence value) {
//        return LocalDate.parse(value);
//    }
//
//    @Override
//    public LocalDateTime parseLocalDateTime(CharSequence value) {
//        return LocalDateTime.parse(value);
//    }
//
//    @Override
//    public LocalTime parseLocalTime(CharSequence value) {
//        return LocalTime.parse(value);
//    }
//
//    @Override
//    public Instant parseInstant(CharSequence value) {
//        return Instant.parse(value);
//    }
//
//    @Override
//    public Object ofLocalDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond) throws Exception {
//        return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
//    }
//
//    @Override
//    public Object ofLocalTime(int hour, int minute, int second, int nanoOfSecond) throws Exception {
//        return LocalTime.of(hour, minute, second, nanoOfSecond);
//    }
//
//    @Override
//    public Object ofLocalDate(int year, int month, int day) throws Exception {
//        return LocalDate.of(year, month, day);
//    }
//}
