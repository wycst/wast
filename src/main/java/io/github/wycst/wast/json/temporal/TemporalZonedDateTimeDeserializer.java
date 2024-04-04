package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.utils.NumberUtils;
import io.github.wycst.wast.json.JSONParseContext;
import io.github.wycst.wast.json.JSONTemporalDeserializer;
import io.github.wycst.wast.json.exceptions.JSONException;

import java.time.temporal.Temporal;

/**
 * 参考java.util.Date反序列化，使用反射实现
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 */
public class TemporalZonedDateTimeDeserializer extends JSONTemporalDeserializer {

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
            zoneObject = TemporalAloneInvoker.ofZoneId(zoneId);
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
            zoneObject = TemporalAloneInvoker.ofZoneId(zoneId);
        }
        return ofTemporalDateTime(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay(), generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000, zoneObject);
    }

    // default format yyyy-MM-ddTHH:mm:ss.SSS+08:00[Asia/Shanghai] not supported 'T'
    @Override
    protected Object deserializeDefaultTemporal(char[] buf, int offset, char endChar, JSONParseContext jsonParseContext) throws Exception {
        int year = NumberUtils.parseInt4(buf, offset);
        int month = NumberUtils.parseInt2(buf, offset + 5);
        int day = NumberUtils.parseInt2(buf, offset + 8);
        int hour = NumberUtils.parseInt2(buf, offset + 11);
        int minute = NumberUtils.parseInt2(buf, offset + 14);
        int second = NumberUtils.parseInt2(buf, offset + 17);
        int nanoOfSecond = 0;
        offset += 19;
        char c = buf[offset];
        if (c == '.') {
            int cnt = 9;
            while (isDigit((c = buf[++offset]))) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c - 48;
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
                zoneObject = TemporalAloneInvoker.getZeroZoneId();
                c = buf[++offset];
                break;
            }
            case '+':
            case '-': {
                int zoneBeginOff = offset;
                // parse +08:00
                while (isDigit(c = buf[++offset]) || c == ':') ;
                zoneObject = TemporalAloneInvoker.ofZoneId(new String(buf, offset, offset - zoneBeginOff));
                break;
            }
        }
        if (c == '[') {
            if (supportedZoneRegion()) {
                int zoneRegionOff = offset;
                while (buf[++offset] != ']') ;
                zoneObject = TemporalAloneInvoker.ofZoneId(new String(buf, zoneRegionOff + 1, offset - zoneRegionOff - 1));
                c = buf[++offset];
            } else {
                while (buf[++offset] != ']') ;
                c = buf[++offset];
            }
        }
        if (c == endChar) {
            jsonParseContext.endIndex = offset;
            return ofTemporalDateTime(year, month, day, hour, minute, second, nanoOfSecond, zoneObject);
        }

        String errorContextTextAt = createErrorContextText(buf, offset);
        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', unexpected token '" + c + "', expected '" + endChar + "'");
    }

    // default format yyyy*MM*dd*HH*mm*ss.SSS+08:00[Asia/Shanghai] not supported 'T'
    @Override
    protected Object deserializeDefaultTemporal(byte[] buf, int offset, char endChar, JSONParseContext jsonParseContext) throws Exception {
        int year = NumberUtils.parseInt4(buf, offset);
        int month = NumberUtils.parseInt2(buf, offset + 5);
        int day = NumberUtils.parseInt2(buf, offset + 8);
        int hour = NumberUtils.parseInt2(buf, offset + 11);
        int minute = NumberUtils.parseInt2(buf, offset + 14);
        int second = NumberUtils.parseInt2(buf, offset + 17);
        int nanoOfSecond = 0;
        offset += 19;
        byte c = buf[offset];
        if (c == '.') {
            int cnt = 9;
            while (isDigit((c = buf[++offset]))) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c - 48;
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
                zoneObject = TemporalAloneInvoker.getZeroZoneId();
                c = buf[++offset];
                break;
            }
            case '+':
            case '-': {
                int zoneBeginOff = offset;
                // parse +08:00
                while (isDigit(c = buf[++offset]) || c == ':') ;
                zoneObject = TemporalAloneInvoker.ofZoneId(new String(buf, offset, offset - zoneBeginOff));
                break;
            }
        }
        if (c == '[') {
            if (supportedZoneRegion()) {
                int zoneRegionOff = offset;
                while (buf[++offset] != ']') ;
                zoneObject = TemporalAloneInvoker.ofZoneId(new String(buf, zoneRegionOff + 1, offset - zoneRegionOff - 1));
                c = buf[++offset];
            } else {
                while (buf[++offset] != ']') ;
                c = buf[++offset];
            }
        }
        if (c == endChar) {
            jsonParseContext.endIndex = offset;
            return ofTemporalDateTime(year, month, day, hour, minute, second, nanoOfSecond, zoneObject);
        }

        String errorContextTextAt = createErrorContextText(buf, offset);
        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) c + "', expected '" + endChar + "'");
    }

    protected boolean supportedZoneRegion() {
        return true;
    }

    protected Object getDefaultZoneId() throws Exception {
        return TemporalAloneInvoker.getDefaultZoneId();
    }

    protected Temporal ofTemporalDateTime(int year, int month, int dayOfMonth, int hour, int minute, int second, int nanoOfSecond, Object zone) throws Exception {
        return (Temporal) TemporalAloneInvoker.ofZonedDateTime(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, zone);
    }

    @Override
    protected Object valueOf(String value, Class<?> actualType) throws Exception {
        return TemporalAloneInvoker.parseZonedDateTime(value);
    }
}
