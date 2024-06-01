package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;

/**
 * 和ZonedDateTime实现相似
 *
 * @Date
 * @Created by wycst
 */
public class TemporalOffsetDateTimeDeserializer extends TemporalZonedDateTimeDeserializer {
    public TemporalOffsetDateTimeDeserializer(TemporalConfig temporalConfig) {
        super(temporalConfig);
    }

    protected void checkClass(GenericParameterizedType genericParameterizedType) {
    }

    @Override
    protected Temporal ofTemporalDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond, Object zone) throws Exception {
        return OffsetDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, (ZoneOffset) zone);
    }

    protected Object getDefaultZoneId() throws Exception {
        return DEFAULT_ZONE_OFFSET;
    }

    protected boolean supportedZoneRegion() {
        return false;
    }

    @Override
    protected Object valueOf(String value, Class<?> actualType) throws Exception {
        return OffsetDateTime.parse(value);
    }
}
