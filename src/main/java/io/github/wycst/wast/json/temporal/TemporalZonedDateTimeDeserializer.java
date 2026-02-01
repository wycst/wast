package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.NumberUtils;
import io.github.wycst.wast.json.JSONParseContext;
import io.github.wycst.wast.json.JSONTemporalDeserializer;
import io.github.wycst.wast.json.exceptions.JSONException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ZonedDateTime反序列化
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 */
public class TemporalZonedDateTimeDeserializer extends JSONTemporalDeserializer {

    // 注：全局可变
    private static TimeZone defaultTimezone = UnsafeHelper.getDefaultTimeZone();
    private static ZoneId defaultZoneId = defaultTimezone.toZoneId();
    static final ZoneId ZERO = ZoneId.of("Z");
    static final ZoneOffset DEFAULT_ZONE_OFFSET = (ZoneOffset) ZERO;

    private static Map<String, ZoneId> zoneIdMap = new ConcurrentHashMap<String, ZoneId>();

    public final static Object defaultZoneId() throws Exception {
        TimeZone timeZone = UnsafeHelper.getDefaultTimeZone();
        if (timeZone == defaultTimezone) {
            return defaultZoneId;
        }
        defaultTimezone = timeZone;
        return defaultZoneId = defaultTimezone.toZoneId();
    }

    public static ZoneId ofZoneId(String zoneId) throws Exception {
        ZoneId value = zoneIdMap.get(zoneId);
        if (value == null) {
            value = ZoneId.of(zoneId);
            zoneIdMap.put(zoneId, value);
        }
        return value;
    }

    public TemporalZonedDateTimeDeserializer(TemporalConfig temporalConfig) {
        super(temporalConfig);
    }

    protected void checkClass(GenericParameterizedType<?> genericParameterizedType) {
    }

    // use dateTemplate
    protected Object deserializeTemporal(char[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        String zoneId = null;
        int j = endIndex, ch;
        while (j > fromIndex + 20) {
            if ((ch = buf[--j]) == '.') break;
            if (ch == '+' || ch == '-' || ch == 'Z') {
                zoneId = new String(buf, j, endIndex - j);
                endIndex = j;
                break;
            }
            if (ch == '[' && buf[endIndex - 1] == ']') {
                // eg: [Asia/Shanghai]
                zoneId = new String(buf, j + 1, endIndex - j - 2);
                endIndex = j;
                break;
            }
        }
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        Object zoneObject;
        if (zoneId == null) {
            // default
            zoneObject = getDefaultZoneId();
        } else {
            zoneObject = ofZoneId(zoneId);
        }
        return ofTemporalDateTime(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay(), generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000, zoneObject);
    }

    protected Object deserializeTemporal(byte[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        String zoneId = null;
        int j = endIndex, ch;
        while (j > fromIndex + 20) {
            if ((ch = buf[--j]) == '.') break;
            if (ch == '+' || ch == '-' || ch == 'Z') {
                zoneId = new String(buf, j, endIndex - j);
                endIndex = j;
                break;
            }
            if (ch == '[' && buf[endIndex - 1] == ']') {
                // eg: [Asia/Shanghai]
                zoneId = new String(buf, j + 1, endIndex - j - 2);
                endIndex = j;
                break;
            }
        }
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        Object zoneObject;
        if (zoneId == null) {
            // default
            zoneObject = getDefaultZoneId();
        } else {
            zoneObject = ofZoneId(zoneId);
        }
        return ofTemporalDateTime(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay(), generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000, zoneObject);
    }

    // default format yyyy-MM-ddTHH:mm:ss.SSS+08:00[Asia/Shanghai] not supported 'T'
    // ymd support yyyy-MM-dd, yyyy/MM/dd, yyyy.MM.dd, yyyy~MM~dd
    // hms support HH:mm:ss.SSS
    @Override
    protected Object deserializeDefault(char[] buf, int offset, char endToken, JSONParseContext parseContext) throws Exception {
        LocalDateTime localDateTime = parseLocalDateTime(buf, offset, parseContext);
        int i = parseContext.endIndex;
        char c = buf[i];
        Object zoneObject;
        if (c == 'Z' || c == 'z') {
            zoneObject = ZERO;
            c = buf[++i];
        } else if (c == '+' || c == '-') {
            int zoneBeginOff = i;
            // parse +08:00
            while (NumberUtils.isDigit(c = buf[++i]) || c == ':') ;
            zoneObject = ofZoneId(new String(buf, zoneBeginOff, i - zoneBeginOff));
        } else {
            zoneObject = getDefaultZoneId();
        }
        if (c == '[') {
            if (supportedZoneRegion()) {
                int zoneRegionOff = i;
                while (buf[++i] != ']') ;
                zoneObject = ofZoneId(new String(buf, zoneRegionOff + 1, i - zoneRegionOff - 1));
                c = buf[++i];
            } else {
                while (buf[++i] != ']') ;
                c = buf[++i];
            }
        }
        if (c == endToken) {
            parseContext.endIndex = i;
            return ofTemporalDateTime(localDateTime, zoneObject);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + c + "', expected '" + endToken + "'");
    }

    // default format yyyy*MM*dd*HH*mm*ss.SSS+08:00[Asia/Shanghai] not supported 'T'
    @Override
    protected final Temporal deserializeDefault(byte[] buf, int offset, byte endToken, JSONParseContext parseContext) throws Exception {
        LocalDateTime localDateTime = parseLocalDateTime(buf, offset, parseContext);
        int i = parseContext.endIndex;
        byte c = buf[i];
        Object zoneObject;
        if (c == 'Z' || c == 'z') {
            zoneObject = ZERO;
            c = buf[++i];
        } else if (c == '+' || c == '-') {
            int zoneBeginOff = i;
            // parse +08:00
            while (NumberUtils.isDigit(c = buf[++i]) || c == ':') ;
            zoneObject = ofZoneId(new String(buf, zoneBeginOff, i - zoneBeginOff));
        } else {
            zoneObject = getDefaultZoneId();
        }
        if (c == '[') {
            if (supportedZoneRegion()) {
                int zoneRegionOff = i;
                while (buf[++i] != ']') ;
                zoneObject = ofZoneId(new String(buf, zoneRegionOff + 1, i - zoneRegionOff - 1));
                c = buf[++i];
            } else {
                while (buf[++i] != ']') ;
                c = buf[++i];
            }
        }
        if (c == endToken) {
            parseContext.endIndex = i;
            return ofTemporalDateTime(localDateTime, zoneObject);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) c + "', expected '" + (char) endToken + "'");
    }

    protected boolean supportedZoneRegion() {
        return true;
    }

    protected Object getDefaultZoneId() throws Exception {
        return defaultZoneId();
    }

    protected Temporal ofTemporalDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond, Object zone) throws Exception {
        return ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, (ZoneId) zone);
    }

    protected Temporal ofTemporalDateTime(LocalDateTime localDateTime, Object zone) throws Exception {
        return ZonedDateTime.of(localDateTime, (ZoneId) zone);
    }

    @Override
    protected Object valueOf(String value, Class<?> actualType) throws Exception {
        return ZonedDateTime.parse(value);
    }
}
