package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.json.JSONParseContext;
import io.github.wycst.wast.json.JSONTemporalDeserializer;
import io.github.wycst.wast.json.exceptions.JSONException;

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

    public static Object defaultZoneId() throws Exception {
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

    protected void checkClass(GenericParameterizedType genericParameterizedType) {
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
    @Override
    protected Object deserializeDefaultTemporal(char[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset;
        int year, month, day, hour, minute, second;
        char c1, c2, c3, c4;
        if (isDigit(c1 = buf[i]) && isDigit(c2 = buf[++i]) && isDigit(c3 = buf[++i]) && isDigit(c4 = buf[++i])) {
            year = c1 * 1000 + c2 * 100 + c3 * 10 + c4 - 53328;
        } else {
            if (c1 == '-' && isDigit(c1 = buf[++i]) && isDigit(c2 = buf[++i]) && isDigit(c3 = buf[++i]) && isDigit(c4 = buf[++i])) {
                year = c1 * 1000 + c2 * 100 + c3 * 10 + c4 -53328;
                year = -year;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', year field error ");
            }
        }
        while (isDigit(c1 = buf[++i])) {
            year = year * 10 + c1 - 48;
        }
        if (isDigit(c1 = buf[++i])) {
            month = c1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
        }
        if (isDigit(c1 = buf[++i])) {
            month = (month << 3) + (month << 1) + c1 - 48;
        }
        ++i;
        if (isDigit(c1 = buf[++i])) {
            day = c1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', day field error ");
        }
        if (isDigit(c1 = buf[++i])) {
            day = (day << 3) + (day << 1) + c1 - 48;
        }
        ++i;
        if (isDigit(c1 = buf[++i])) {
            hour = c1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', hour field error ");
        }
        if (isDigit(c1 = buf[++i])) {
            hour = (hour << 3) + (hour << 1) + c1 - 48;
        }
        ++i;
        if (isDigit(c1 = buf[++i])) {
            minute = c1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', minute field error ");
        }
        if (isDigit(c1 = buf[++i])) {
            minute = (minute << 3) + (minute << 1) + c1 - 48;
        }

        ++i;
        if (isDigit(c1 = buf[++i])) {
            second = c1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', second field error ");
        }
        if (isDigit(c1 = buf[++i])) {
            second = (second << 3) + (second << 1) + c1 - 48;
            c1 = buf[++i];
        }

        int nanoOfSecond = 0;
        char c = c1;
        if (c == '.') {
            int cnt = 9;
            boolean isDigitFlag;
            while ((isDigitFlag = isDigit(c = buf[++i])) && isDigit(c1 = buf[++i])) {
                cnt -= 2;
                nanoOfSecond = nanoOfSecond * 100 + c * 10 + c1 - 528;
            }
            if(isDigitFlag) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c - 48;
                c = c1;
                --cnt;
            }
            if (cnt > 0) {
                nanoOfSecond *= NANO_OF_SECOND_PADDING[cnt];
            }
        }
        Object zoneObject = getDefaultZoneId();
        switch (c) {
            case 'z':
            case 'Z': {
                zoneObject = ZERO;
                c = buf[++i];
                break;
            }
            case '+':
            case '-': {
                int zoneBeginOff = i;
                // parse +08:00
                while (isDigit(c = buf[++i]) || c == ':') ;
                zoneObject = ofZoneId(new String(buf, zoneBeginOff, i - zoneBeginOff));
                break;
            }
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
            jsonParseContext.endIndex = i;
            return ofTemporalDateTime(year, month, day, hour, minute, second, nanoOfSecond, zoneObject);
        }

        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + c + "', expected '" + endToken + "'");
    }

    // default format yyyy*MM*dd*HH*mm*ss.SSS+08:00[Asia/Shanghai] not supported 'T'
    @Override
    protected Object deserializeDefaultTemporal(byte[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset;
        int year, month, day, hour, minute, second;
        byte b1, b2, b3, b4;
        if (isDigit(b1 = buf[i]) && isDigit(b2 = buf[++i]) && isDigit(b3 = buf[++i]) && isDigit(b4 = buf[++i])) {
            year = b1 * 1000 + b2 * 100 + b3 * 10 + b4 - 53328;
        } else {
            if (b1 == '-' && isDigit(b1 = buf[++i]) && isDigit(b2 = buf[++i]) && isDigit(b3 = buf[++i]) && isDigit(b4 = buf[++i])) {
                year = b1 * 1000 + b2 * 100 + b3 * 10 + b4 -53328;
                year = -year;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', year field error ");
            }
        }
        while (isDigit(b1 = buf[++i])) {
            year = year * 10 + b1 - 48;
        }
        if (isDigit(b1 = buf[++i])) {
            month = b1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
        }
        if (isDigit(b1 = buf[++i])) {
            month = (month << 3) + (month << 1) + b1 - 48;
        }
        ++i;
        if (isDigit(b1 = buf[++i])) {
            day = b1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', day field error ");
        }
        if (isDigit(b1 = buf[++i])) {
            day = (day << 3) + (day << 1) + b1 - 48;
        }
        ++i;
        if (isDigit(b1 = buf[++i])) {
            hour = b1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', hour field error ");
        }
        if (isDigit(b1 = buf[++i])) {
            hour = (hour << 3) + (hour << 1) + b1 - 48;
        }
        ++i;
        if (isDigit(b1 = buf[++i])) {
            minute = b1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', minute field error ");
        }
        if (isDigit(b1 = buf[++i])) {
            minute = (minute << 3) + (minute << 1) + b1 - 48;
        }

        ++i;
        if (isDigit(b1 = buf[++i])) {
            second = b1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', second field error ");
        }
        if (isDigit(b1 = buf[++i])) {
            second = (second << 3) + (second << 1) + b1 - 48;
            b1 = buf[++i];
        }

        int nanoOfSecond = 0;
        byte c = b1;
        if (c == '.') {
            int cnt = 9;
            boolean isDigitFlag;
            while ((isDigitFlag = isDigit(c = buf[++i])) && isDigit(b1 = buf[++i])) {
                cnt -= 2;
                nanoOfSecond = nanoOfSecond * 100 + c * 10 + b1 - 528;
            }
            if(isDigitFlag) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c - 48;
                c = b1;
                --cnt;
            }
            if (cnt > 0) {
                nanoOfSecond *= NANO_OF_SECOND_PADDING[cnt];
            }
        }
        Object zoneObject = getDefaultZoneId();
        switch (c) {
            case 'z':
            case 'Z': {
                zoneObject = ZERO;
                c = buf[++i];
                break;
            }
            case '+':
            case '-': {
                int zoneBeginOff = i;
                // parse +08:00
                while (isDigit(c = buf[++i]) || c == ':') ;
                zoneObject = ofZoneId(new String(buf, zoneBeginOff, i - zoneBeginOff));
                break;
            }
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
            jsonParseContext.endIndex = i;
            return ofTemporalDateTime(year, month, day, hour, minute, second, nanoOfSecond, zoneObject);
        }

        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) c + "', expected '" + endToken + "'");
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

    @Override
    protected Object valueOf(String value, Class<?> actualType) throws Exception {
        return ZonedDateTime.parse(value);
    }
}
