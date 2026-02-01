package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.ClassStrucWrap;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.utils.NumberUtils;
import io.github.wycst.wast.json.exceptions.JSONException;

import java.time.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * <p> java.time.* 注册模块 </p>
 * <p> jdk8+ </p>
 *
 * @Author: wangy
 * @see ZoneId
 * @see Duration
 * @see Period
 * @see Year
 * @see Month
 * @see JSONTemporalDeserializer#getTemporalDeserializerInstance
 * @see JSONTemporalSerializer#getTemporalSerializerInstance(ClassStrucWrap, JSONPropertyDefinition)
 */
public class JSONTypeModuleJavaTime implements JSONTypeModule {

    @Override
    public void register(final JSONTypeRegistor registor) {
        // 注: ZoneId::of, Duration::parse, Period::parse 等函数变量语法 jdk6 编译无法通过， 使用内部类替换；
        // jdk8+以上的应用使用不受限制；

        // java.time.ZoneId
        // registor.register(ZoneId.class, JSONTypeRegistorUtils.fromString(ZoneId::of),  true);
        registor.registerInternal(ZoneId.class, JSONTypeRegistorUtils.fromString(new Function<String, ZoneId>() {
            @Override
            public ZoneId apply(String value) {
                return ZoneId.of(value);
            }
        }), true);
        registor.register(ZoneId.class, JSONTypeRegistorUtils.TO_STRING, true);

        // java.time.Duration
        registor.registerInternal(Duration.class, new JSONTypeDeserializer() {
            @Override
            protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object defaultValue, int endToken, JSONParseContext parseContext) throws Exception {
                return parseDuration(buf, fromIndex, parseContext);
            }

            @Override
            protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, GenericParameterizedType<?> parameterizedType, Object defaultValue, int endToken, JSONParseContext parseContext) throws Exception {
                return parseDuration(bytes, fromIndex, parseContext);
            }
        }, false);
        registor.register(Duration.class, new JSONTypeSerializer() {
            @Override
            protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                Duration duration = (Duration) value;
                writer.writeJSONDuration(duration.getSeconds(), duration.getNano());
            }
        });

        // java.time.Period
        registor.registerInternal(Period.class, new JSONTypeDeserializer() {
            @Override
            protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object defaultValue, int endToken, JSONParseContext parseContext) throws Exception {
                return parsePeriod(buf, fromIndex, parseContext);
            }

            @Override
            protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, GenericParameterizedType<?> parameterizedType, Object defaultValue, int endToken, JSONParseContext parseContext) throws Exception {
                return parsePeriod(bytes, fromIndex, parseContext);
            }
        }, false);
        registor.register(Period.class, new JSONTypeSerializer() {
            @Override
            protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                Period period = (Period) value;
                writer.writeJSONPeriod(period.getYears(), period.getMonths(), period.getDays());
            }
        });

        // java.time.Year
        registor.registerInternal(Year.class, JSONTypeRegistorUtils.fromInteger(new Function<Integer, Year>() {
            @Override
            public Year apply(Integer value) {
                return value == null ? null : Year.of(value);
            }
        }), false);
        registor.register(Year.class, JSONTypeRegistorUtils.toInt(new ToIntFunction<Year>() { // Year::getValue
            @Override
            public int applyAsInt(Year value) {
                return value.getValue();
            }
        }));

        // java.time.Month
        registor.registerInternal(Month.class, JSONTypeRegistorUtils.fromInteger(new Function<Integer, Month>() {
            @Override
            public Month apply(Integer value) {
                return value == null ? null : Month.of(value);
            }
        }), false);
        registor.register(Month.class, JSONTypeRegistorUtils.toInt(new ToIntFunction<Month>() { // Month::getValue
            @Override
            public int applyAsInt(Month value) {
                return value.getValue();
            }
        }));
    }

    final static Duration parseDuration(char[] buf, final int fromIndex, JSONParseContext parseContext) throws Exception {
        int offset = fromIndex;
        int begin = buf[offset++];
        if (begin == 'n') {
            JSONTypeDeserializer.parseNull(buf, fromIndex, parseContext);
            return null;
        }
        if ((begin == '"' || begin == '\'')) {
            if (buf[offset] == 'P') {
                // 循环读取
                int c;
                int hours = 0, minutes = 0, seconds = 0, nano = 0;
                long days = 0, value = 0;
                do {
                    c = buf[++offset];
                    if (c == begin) {
                        break;
                    }
                    if (c == 'T') {
                        c = buf[++offset];
                    }
                    boolean negate = c == '-';
                    if (negate) {
                        c = buf[++offset];
                    }
                    // read integer
                    while (NumberUtils.isDigit(c)) {
                        value = (value << 3) + (value << 1) + (c & 0xF);
                        c = buf[++offset];
                    }
                    switch (c | 0x20) {
                        case 'd':
                            days = negate ? -value : value;
                            value = 0;
                            continue;
                        case 'h':
                            hours = negate ? (int) -value : (int) value;
                            value = 0;
                            continue;
                        case 'm':
                            minutes = negate ? (int) -value : (int) value;
                            value = 0;
                            continue;
                        case 's':
                            seconds = negate ? (int) -value : (int) value;
                            value = 0;
                            continue;
                    }
                    if (c == '.') {
                        int decimals = 9;
                        while (NumberUtils.isDigit(c = buf[++offset])) {
                            --decimals;
                            nano = (nano << 3) + (nano << 1) + (c & 0xF);
                        }
                        if (decimals > 0) {
                            nano *= JSONTemporalDeserializer.NANO_OF_SECOND_PADDING[decimals];
                        }
                        if (c == 'S' || c == 's') {
                            continue;
                        }
                        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + JSONGeneral.createErrorContextText(buf, offset) + "', unexpected '" + (char) c + "', expected 'S'");
                    }
                    throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + JSONGeneral.createErrorContextText(buf, fromIndex) + "', unexpected '" + (char) c + "', expected 'yYmMwWdD'");
                } while (true);
                parseContext.endIndex = offset;
                return Duration.ofSeconds(days * 86400L + hours * 3600L + minutes * 60L + seconds, Math.floorMod(nano, 1000000000));
            } else if (buf[offset] == begin) {
                parseContext.endIndex = offset;
                return null;
            }
        }
        String errorContextTextAt = JSONGeneral.createErrorContextText(buf, fromIndex);
        throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) begin + "', expected '\"'");
    }

    final static Duration parseDuration(byte[] buf, final int fromIndex, JSONParseContext parseContext) throws Exception {
        int offset = fromIndex;
        int begin = buf[offset++];
        if (begin == 'n') {
            JSONTypeDeserializer.parseNull(buf, fromIndex, parseContext);
            return null;
        }
        if ((begin == '"' || begin == '\'')) {
            if (buf[offset] == 'P') {
                // 循环读取
                int c;
                int hours = 0, minutes = 0, seconds = 0, nano = 0;
                long days = 0, value = 0;
                do {
                    c = buf[++offset];
                    if (c == begin) {
                        break;
                    }
                    if (c == 'T') {
                        c = buf[++offset];
                    }
                    boolean negate = c == '-';
                    if (negate) {
                        c = buf[++offset];
                    }
                    // read integer
                    while (NumberUtils.isDigit(c)) {
                        value = (value << 3) + (value << 1) + (c & 0xF);
                        c = buf[++offset];
                    }
                    switch (c | 0x20) {
                        case 'd':
                            days = negate ? -value : value;
                            value = 0;
                            continue;
                        case 'h':
                            hours = negate ? (int) -value : (int) value;
                            value = 0;
                            continue;
                        case 'm':
                            minutes = negate ? (int) -value : (int) value;
                            value = 0;
                            continue;
                        case 's':
                            seconds = negate ? (int) -value : (int) value;
                            value = 0;
                            continue;
                    }
                    if (c == '.') {
                        int decimals = 9;
                        // 循环读取
                        while (NumberUtils.isDigit(c = buf[++offset])) {
                            --decimals;
                            nano = (nano << 3) + (nano << 1) + (c & 0xF);
                        }
                        if (decimals > 0) {
                            nano *= JSONTemporalDeserializer.NANO_OF_SECOND_PADDING[decimals];
                        }
                        if (c == 'S' || c == 's') {
                            continue;
                        }
                        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + JSONGeneral.createErrorContextText(buf, offset) + "', unexpected '" + (char) c + "', expected 'S'");
                    }
                    throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + JSONGeneral.createErrorContextText(buf, fromIndex) + "', unexpected '" + (char) c + "', expected 'yYmMwWdD'");
                } while (true);
                parseContext.endIndex = offset;
                return Duration.ofSeconds(days * 86400L + hours * 3600L + minutes * 60L + seconds, Math.floorMod(nano, 1000000000));
            } else if (buf[offset] == begin) {
                parseContext.endIndex = offset;
                return null;
            }
        }
        String errorContextTextAt = JSONGeneral.createErrorContextText(buf, fromIndex);
        throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) begin + "', expected '\"'");
    }

    final static Period parsePeriod(char[] buf, final int fromIndex, JSONParseContext parseContext) throws Exception {
        int offset = fromIndex;
        int begin = buf[offset++];
        if (begin == 'n') {
            JSONTypeDeserializer.parseNull(buf, fromIndex, parseContext);
            return null;
        }
        if ((begin == '"' || begin == '\'')) {
            if (buf[offset] == 'P') {
                // 循环读取
                int c;
                int years = 0, months = 0, weeks = 0, days = 0, value = 0;
                do {
                    c = buf[++offset];
                    if (c == begin) {
                        parseContext.endIndex = offset;
                        if (weeks != 0) {
                            days += weeks * 7;
                        }
                        return Period.of(years, months, days);
                    }
                    boolean negate = c == '-';
                    if (negate) {
                        c = buf[++offset];
                    }
                    // read integer
                    while (NumberUtils.isDigit(c)) {
                        value = (value << 3) + (value << 1) + (c & 0xF);
                        c = buf[++offset];
                    }
                    switch (c | 0x20) {
                        case 'y':
                            years = negate ? -value : value;
                            value = 0;
                            continue;
                        case 'm':
                            months = negate ? -value : value;
                            value = 0;
                            continue;
                        case 'w':
                            weeks = negate ? -value : value;
                            value = 0;
                            continue;
                        case 'd':
                            days = negate ? -value : value;
                            value = 0;
                            continue;
                    }
                    throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + JSONGeneral.createErrorContextText(buf, fromIndex) + "', unexpected '" + (char) c + "', expected 'yYmMwWdD'");
                } while (true);
            } else if (buf[offset] == begin) {
                parseContext.endIndex = offset;
                return null;
            }
        }
        String errorContextTextAt = JSONGeneral.createErrorContextText(buf, fromIndex);
        throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) begin + "', expected '\"'");
    }

    final static Period parsePeriod(byte[] buf, int fromIndex, JSONParseContext parseContext) {
        int offset = fromIndex;
        int begin = buf[offset++];
        if (begin == 'n') {
            JSONTypeDeserializer.parseNull(buf, fromIndex, parseContext);
            return null;
        }
        if ((begin == '"' || begin == '\'')) {
            if (buf[offset] == 'P') {
                // 循环读取
                int c;
                int years = 0, months = 0, weeks = 0, days = 0, value = 0;
                do {
                    c = buf[++offset];
                    if (c == begin) {
                        parseContext.endIndex = offset;
                        if (weeks != 0) {
                            days += weeks * 7;
                        }
                        return Period.of(years, months, days);
                    }
                    boolean negate = c == '-';
                    if (negate) {
                        c = buf[++offset];
                    }
                    // read integer
                    while (NumberUtils.isDigit(c)) {
                        value = (value << 3) + (value << 1) + (c & 0xF);
                        c = buf[++offset];
                    }
                    switch (c | 0x20) {
                        case 'y':
                            years = negate ? -value : value;
                            value = 0;
                            continue;
                        case 'm':
                            months = negate ? -value : value;
                            value = 0;
                            continue;
                        case 'w':
                            weeks = negate ? -value : value;
                            value = 0;
                            continue;
                        case 'd':
                            days = negate ? -value : value;
                            value = 0;
                            continue;
                    }
                    throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + JSONGeneral.createErrorContextText(buf, fromIndex) + "', unexpected '" + (char) c + "', expected 'yYmMwWdD'");
                } while (true);
            } else {
                if (buf[offset] == begin) {
                    parseContext.endIndex = offset;
                    return null;
                }
            }
        }
        String errorContextTextAt = JSONGeneral.createErrorContextText(buf, fromIndex);
        throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) begin + "', expected '\"'");
    }
}
