package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.JSONTemporalDeserializer;
import io.github.wycst.wast.json.options.JSONParseContext;

/**
 * 参考java.util.Date反序列化，使用反射实现
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 */
public class TemporalZonedDateTimeDeserializer extends JSONTemporalDeserializer {

    public TemporalZonedDateTimeDeserializer(GenericParameterizedType genericParameterizedType) {
        super(genericParameterizedType);
    }

    protected void checkClass(GenericParameterizedType genericParameterizedType) {
        if (genericParameterizedType.getActualType() != TemporalAloneInvoker.zonedDateTimeClass) {
            throw new UnsupportedOperationException("Not Support for class " + genericParameterizedType.getActualType());
        }
    }

    protected Object deserializeTemporal(char[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        if (patternType == 0) {
            return autoMatchZoneDateTime(buf, fromIndex, endIndex, jsonParseContext);
        } else {
            // Resolve the offset of zoneId
            /* Parsing matches the zone ID step by step as follows.
             * <ul>
             * <li>If the zone ID equals 'Z', the result is {@code ZoneOffset.UTC}.
             * <li>If the zone ID consists of a single letter, the zone ID is invalid
             *  and {@code DateTimeException} is thrown.
             * <li>If the zone ID starts with '+' or '-', the ID is parsed as a
             *  {@code ZoneOffset} using {@link ZoneOffset#of(String)}.
             * <li>If the zone ID equals 'GMT', 'UTC' or 'UT' then the result is a {@code ZoneId}
             *  with the same ID and rules equivalent to {@code ZoneOffset.UTC}.
             * <li>If the zone ID starts with 'UTC+', 'UTC-', 'GMT+', 'GMT-', 'UT+' or 'UT-'
             *  then the ID is a prefixed offset-based ID. The ID is split in two, with
             *  a two or three letter prefix and a suffix starting with the sign.
             *  The suffix is parsed as a {@link ZoneOffset#of(String) ZoneOffset}.
             *  The result will be a {@code ZoneId} with the specified UTC/GMT/UT prefix
             *  and the normalized offset ID as per {@link ZoneOffset#getId()}.
             *  The rules of the returned {@code ZoneId} will be equivalent to the
             *  parsed {@code ZoneOffset}.
             * <li>All other IDs are parsed as region-based zone IDs. Region IDs must
             *  match the regular expression <code>[A-Za-z][A-Za-z0-9~/._+-]+</code>
             *  otherwise a {@code DateTimeException} is thrown. If the zone ID is not
             *  in the configured set of IDs, {@code ZoneRulesException} is thrown.
             *  The detailed format of the region ID depends on the group supplying the data.
             *  The default set of data is supplied by the IANA Time Zone Database (TZDB).
             *  This has region IDs of the form '{area}/{city}', such as 'Europe/Paris' or 'America/New_York'.
             *  This is compatible with most IDs from {@link java.util.TimeZone}.
             */
            String zoneId = null;
            // yyyy-MM-ddTHH:mm:ss.SSS+XX:YY
            int j = endIndex, ch;
            // len = endIndex - fromIndex - 1 > 19
            while (j > fromIndex + 20) {
                // Check whether the time zone ID exists in the date
                // Check for '+' or '-' or 'Z' or '['
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

            GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, null);
            Object zoneObject;
            if (zoneId == null) {
                // default
                zoneObject = TemporalAloneInvoker.getDefaultZoneId();
            } else {
                zoneObject = TemporalAloneInvoker.ofZoneId(zoneId);
            }
            return TemporalAloneInvoker.ofZonedDateTime(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay(), generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000, zoneObject);
        }
    }

    protected Object deserializeTemporal(byte[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        if (patternType == 0) {
            return autoMatchZoneDateTime(buf, fromIndex, endIndex, jsonParseContext);
        } else {
            String zoneId = null;
            // yyyy-MM-ddTHH:mm:ss.SSS+XX:YY
            int j = endIndex, ch;
            // len = endIndex - fromIndex - 1 > 19
            while (j > fromIndex + 20) {
                // Check whether the time zone ID exists in the date
                // Check for '+' or '-' or 'Z' or '['
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

            GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, null);
            Object zoneObject;
            if (zoneId == null) {
                // default
                zoneObject = TemporalAloneInvoker.getDefaultZoneId();
            } else {
                zoneObject = TemporalAloneInvoker.ofZoneId(zoneId);
            }
            return TemporalAloneInvoker.ofZonedDateTime(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay(), generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000, zoneObject);
        }
    }

    private Object autoMatchZoneDateTime(char[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        int len = endIndex - fromIndex - 1;
        if (len == 19) {
            GeneralDate generalDate = GeneralDate.parseGeneralDate_Standard_19(buf, fromIndex + 1, null);
            Object zoneObject = TemporalAloneInvoker.getDefaultZoneId();
            return TemporalAloneInvoker.ofZonedDateTime(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay(), generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000, zoneObject);
        }
        // reflect public static ZonedDateTime parse(CharSequence text)
        return TemporalAloneInvoker.parseZonedDateTime(new String(buf, fromIndex + 1, endIndex - fromIndex - 1));
    }

    private Object autoMatchZoneDateTime(byte[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        int len = endIndex - fromIndex - 1;
        if (len == 19) {
            GeneralDate generalDate = GeneralDate.parseGeneralDate_Standard_19(buf, fromIndex + 1, null);
            Object zoneObject = TemporalAloneInvoker.getDefaultZoneId();
            return TemporalAloneInvoker.ofZonedDateTime(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay(), generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000, zoneObject);
        }
        // reflect public static ZonedDateTime parse(CharSequence text)
        return TemporalAloneInvoker.parseZonedDateTime(new String(buf, fromIndex + 1, endIndex - fromIndex - 1));
    }


}
