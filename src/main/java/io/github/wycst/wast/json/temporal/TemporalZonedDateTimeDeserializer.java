package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.NumberUtils;
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
    protected Object deserializeDefault(char[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset;
        int year, month, day, hour, minute, second;
        char c1, c2, c3, c4;
        if (NumberUtils.isDigit(c1 = buf[i]) && NumberUtils.isDigit(c2 = buf[++i]) && NumberUtils.isDigit(c3 = buf[++i]) && NumberUtils.isDigit(c4 = buf[++i])) {
            year = fourDigitsValue(c1 & 0xf, c2 & 0xf, c3 & 0xf, c4);
        } else {
            if (c1 == '-' && NumberUtils.isDigit(c1 = buf[++i]) && NumberUtils.isDigit(c2 = buf[++i]) && NumberUtils.isDigit(c3 = buf[++i]) && NumberUtils.isDigit(c4 = buf[++i])) {
                year = -fourDigitsValue(c1 & 0xf, c2 & 0xf, c3 & 0xf, c4);
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', year field error ");
            }
        }
        while (NumberUtils.isDigit(c1 = buf[++i])) {
            year = year * 10 + (c1 & 0xf);
        }
        boolean isDigitFlag;
        if ((isDigitFlag = NumberUtils.isDigit(c1 = buf[++i])) && NumberUtils.isDigit(c2 = buf[++i])) {
            month = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if (isDigitFlag) {
                month = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(c1 = buf[++i])) && NumberUtils.isDigit(c2 = buf[++i])) {
            day = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if (isDigitFlag) {
                day = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', day field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(c1 = buf[++i])) && NumberUtils.isDigit(c2 = buf[++i])) {
            hour = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if (isDigitFlag) {
                hour = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', hour field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(c1 = buf[++i])) && NumberUtils.isDigit(c2 = buf[++i])) {
            minute = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if (isDigitFlag) {
                minute = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', minute field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(c1 = buf[++i])) && NumberUtils.isDigit(c2 = buf[++i])) {
            second = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if (isDigitFlag) {
                second = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', second field error ");
            }
        }

        int nanoOfSecond = 0;
        char c = buf[i];
        if (c == '.') {
            int cnt = 9;
            while ((isDigitFlag = NumberUtils.isDigit(c = buf[++i])) && NumberUtils.isDigit(c1 = buf[++i])) {
                cnt -= 2;
                nanoOfSecond = nanoOfSecond * 100 + twoDigitsValue(c, c1);
                ;
            }
            if (isDigitFlag) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + (c & 0xf);
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
                while (NumberUtils.isDigit(c = buf[++i]) || c == ':') ;
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
    protected Object deserializeDefault(byte[] buf, int offset, byte endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset;
        int year, month, day, hour, minute, second;
        byte b1, b2;
        if ((year = parseFourDigitsYear(buf, i)) != -1) {
            i += 3;
        } else {
            if (buf[i] == '-' && (year = parseFourDigitsYear(buf, i + 1)) != -1) {
                year = -year;
                i += 4;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', year field error ");
            }
        }
        while (NumberUtils.isDigit(b1 = buf[++i])) {
            year = year * 10 + (b1 & 0xf);
        }
        boolean isDigitFlag;
        if ((isDigitFlag = NumberUtils.isDigit(b1 = buf[++i])) && NumberUtils.isDigit(b2 = buf[++i])) {
            month = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if (isDigitFlag) {
                month = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(b1 = buf[++i])) && NumberUtils.isDigit(b2 = buf[++i])) {
            day = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if (isDigitFlag) {
                day = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', day field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(b1 = buf[++i])) && NumberUtils.isDigit(b2 = buf[++i])) {
            hour = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if (isDigitFlag) {
                hour = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', hour field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(b1 = buf[++i])) && NumberUtils.isDigit(b2 = buf[++i])) {
            minute = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if (isDigitFlag) {
                minute = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', minute field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(b1 = buf[++i])) && NumberUtils.isDigit(b2 = buf[++i])) {
            second = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if (isDigitFlag) {
                second = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', second field error ");
            }
        }
        int nanoOfSecond = 0;
        byte c = buf[i];
        if (c == '.') {
            nanoOfSecond = parseNanoOfSecond(buf, i + 1, jsonParseContext);
            c = buf[i = jsonParseContext.endIndex];
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
                while (NumberUtils.isDigit(c = buf[++i]) || c == ':') ;
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

    @Override
    protected Object valueOf(String value, Class<?> actualType) throws Exception {
        return ZonedDateTime.parse(value);
    }
}
