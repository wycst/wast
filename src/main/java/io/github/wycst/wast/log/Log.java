package io.github.wycst.wast.log;

import java.util.logging.*;

/**
 * @Author: wangy
 * @Date: 2020/5/5 15:46
 * @Description:
 */
public final class Log {

    //    private final Class<?> nameClass;
    private final String loggerName;
    private final Logger logger;

    Log(Class<?> nameClass) {
        this.loggerName = getLoggerName(nameClass);
        logger = Logger.getLogger(loggerName);
        logger.setLevel(Level.ALL);
//        // 控制台日志
//        ConsoleHandler consoleHandler = new ConsoleHandler();
//        consoleHandler.setFormatter(new ConsoleFormatter());
//        logger.addHandler(consoleHandler);
//        // 文件输入日志
//        try {
//            FileHandler fileHandler = new FileHandler("log.txt");
//            fileHandler.setFormatter(new TextFormatter());
//            logger.addHandler(fileHandler);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        logger.setFilter(new LogFilter());
        logger.setUseParentHandlers(false);
    }

    void addHandler(Handler handler) {
        logger.addHandler(handler);
    }

    void removeHandler(Handler handler) {
        logger.removeHandler(handler);
    }

    private String getLoggerName(Class<?> nameClass) {
        String className = nameClass.getName();
        if (className.indexOf('.') == -1) {
            return className;
        }
        if (className.length() < 40) {
            return className;
        }

        return className.replaceAll("(\\w)\\w*[.]", "$1.");
    }

    public void debug(String msg, Object... args) {
        log(Level.CONFIG, msg, args);
    }

    public void info(String msg, Object... args) {
        log(Level.INFO, msg, args);
    }

    public void warn(String msg, Object... args) {
        log(Level.WARNING, msg, args);
    }

    public void error(String msg, Object... args) {
        log(Level.SEVERE, msg, args);
    }

    private void log(Level level, String msg, Object[] args) {
        if (logger.isLoggable(level)) {
            logger.log(level, msg, args);
        }
    }

    public void error(String msg, Throwable throwable, Object... args) {
        if (logger.isLoggable(Level.SEVERE)) {
            LogRecord logRecord = new LogRecord(Level.SEVERE, msg);
            logRecord.setParameters(args);
            logRecord.setLoggerName(loggerName);
            logRecord.setThrown(throwable);
            logger.log(logRecord);
        }
    }

    void setLevel(Level level) {
        logger.setLevel(level);
    }

    class LogFilter implements Filter {
        @Override
        public boolean isLoggable(LogRecord record) {
            Level level = record.getLevel();
            Level logLevel = LoggerManagerHandler.getLogLevel();
            if (logLevel != null && level.intValue() < logLevel.intValue()) {
                return false;
            }
            return true;
        }
    }
}
