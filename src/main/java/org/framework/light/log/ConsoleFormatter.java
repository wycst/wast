package org.framework.light.log;

import java.util.logging.LogRecord;

/**
 * @Author: wangy
 * @Date: 2020/5/6 20:08
 * @Description:
 */
public class ConsoleFormatter extends TextFormatter {

//    @Override
//    public String formatDate(long millis) {
//        return DateUtils.formatSimpleTime(new Date(millis), true);
//    }

    @Override
    public String formatLoggerName(LogRecord record) {
        String loggerName = record.getLoggerName();
        return loggerName;
    }
}
