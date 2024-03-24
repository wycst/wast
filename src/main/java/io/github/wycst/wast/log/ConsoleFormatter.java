package io.github.wycst.wast.log;

import java.util.logging.LogRecord;

/**
 * @Author: wangy
 * @Date: 2020/5/6 20:08
 * @Description:
 */
public class ConsoleFormatter extends TextFormatter {

    @Override
    public String formatLoggerName(LogRecord record) {
        String loggerName = record.getLoggerName();
        return loggerName;
    }
}
