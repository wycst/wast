package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;

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
        return (Temporal) TemporalAloneInvoker.ofOffsetDateTime(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, zone);
    }

    protected Object getDefaultZoneId() throws Exception {
        return TemporalAloneInvoker.getDefaultZoneOffset();
    }

    protected boolean supportedZoneRegion() {
        return false;
    }
}
