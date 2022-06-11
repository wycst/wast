package io.github.wycst.wast.log;

import io.github.wycst.wast.common.beans.Date;
import io.github.wycst.wast.common.utils.StringUtils;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class TextFormatter extends Formatter {

    public String formatDate(long millis) {
        return new Date(millis).format("Y-M-d H:m:s.S");
    }

    public String formatLoggerName(LogRecord record) {
        return record.getLoggerName();
    }

    @Override
    public String format(LogRecord record) {

        StringBuffer sb = new StringBuffer();
        sb.append(formatDate(record.getMillis()));
        sb.append(" [");
        sb.append(Thread.currentThread().getName());
        sb.append("] ");
        sb.append(formatLevel(record.getLevel()));
        sb.append(" ");
        sb.append(formatLoggerName(record));
        sb.append(" - ");

        // message
        String message = record.getMessage();
        java.util.ResourceBundle catalog = record.getResourceBundle();
        if (catalog != null) {
            try {
                message = catalog.getString(record.getMessage());
            } catch (java.util.MissingResourceException ex) {
                // Drop through.  Use record message as format
                message = record.getMessage();
            }
        }

        // parse parameters
        Object[] parameters = record.getParameters();
        if (parameters != null && parameters.length > 0) {
            // 替换占位符
            message = StringUtils.replacePlaceholder(message, "{}", parameters);
        }
        sb.append(message);
        sb.append("\n");

        boolean isException = record.getThrown() != null;
        if (isException) {
            sb.append(StringUtils.getThrowableContent(record.getThrown()));
        }

        String result = sb.toString();
        // 处理极端情况下日志文件没有权限生成情况，通过console输出
        if ("true".equals(System.getProperty(LogFactory.LOG_ON_CONSOLE))) {
            if (isException) {
                System.err.println(result);
            } else {
                System.out.print(result);
            }
        }

        return result;
    }

    private String formatLevel(Level level) {
        String levelName = level.getName();
        if (level == Level.INFO) {
            return levelName + " ";
        } else if (level == Level.WARNING) {
            return "WARN ";
        } else if (level == Level.CONFIG) {
            return "DEBUG";
        } else if (level == Level.SEVERE) {
            return "ERROR";
        }
        return levelName;
    }

}
