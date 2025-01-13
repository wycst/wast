package io.github.wycst.wast.json;

import io.github.wycst.wast.json.exceptions.JSONException;

import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;

public class JSONTemporalExtension {
    static {
        // ZoneId
        JSON.registerTypeMapper(ZoneId.class, new JSONTypeMapper<ZoneId>() {
            @Override
            public ZoneId readOf(Object value) throws Exception {
                if (value == null) return null;
                if (value instanceof String) return ZoneId.of((String) value);
                throw new JSONException("invalid zoneId input value: " + value);
            }

            @Override
            public JSONValue<?> writeAs(ZoneId zoneId, JSONConfig jsonConfig) throws Exception {
                return zoneId == null ? null : JSONValue.of(zoneId.toString());
            }
        });

        // Duration
        JSON.registerTypeMapper(Duration.class, new JSONTypeMapper<Duration>() {
            @Override
            public Duration readOf(Object value) throws Exception {
                if (value == null) return null;
                if (value instanceof String) return Duration.parse((String) value);
                if (value instanceof Number) return Duration.ofMillis(((Number) value).longValue());
                throw new JSONException("invalid duration input value: " + value);
            }

            @Override
            public JSONValue<?> writeAs(Duration duration, JSONConfig jsonConfig) throws Exception {
                return duration == null ? null : JSONValue.of(duration.toString());
            }
        });

        // Period
        JSON.registerTypeMapper(Period.class, new JSONTypeMapper<Period>() {
            @Override
            public Period readOf(Object value) throws Exception {
                if (value == null) return null;
                if (value instanceof String) return Period.parse((String) value);
                throw new JSONException("invalid period input value: " + value);
            }

            @Override
            public JSONValue<?> writeAs(Period period, JSONConfig jsonConfig) throws Exception {
                return period == null ? null : JSONValue.of(period.toString());
            }
        });
    }
}
