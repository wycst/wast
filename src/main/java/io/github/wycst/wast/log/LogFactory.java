package io.github.wycst.wast.log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * @Author: wangy
 * @Date: 2020/5/5 15:46
 * @Description:
 */
public class LogFactory {

    private static Map<Class<?>, Log> loggers = new ConcurrentHashMap<Class<?>, Log>();
    public static final String LOG_ON_CONSOLE = "java.util.logging.console";
    private static Map<String, FileHandlerHolder> fileHandlerHolders = new ConcurrentHashMap<String, FileHandlerHolder>();

    static {
        LoggerManagerHandler.init();
    }

    // 重置所有的日志对象
    void reset() {
    }

    public static Log getLog(Class<?> logCls) {
        synchronized (logCls) {
            Log log = loggers.get(logCls);
            if (log == null) {
                log = new Log(logCls);
                List<Handler> handlers = LoggerManagerHandler.matchHandlers(logCls);
                Level level = LoggerManagerHandler.matchLevel(logCls);
                if (level != null) {
                    log.setLevel(level);
                }
                for (Handler handler : handlers) {
                    log.addHandler(handler);
                }
                loggers.put(logCls, log);
            }
            return log;
        }
    }

    public static void setConsoleLevel(String consoleLevel) {
        Level levelValue = Level.parse(consoleLevel);
        LoggerManagerHandler.setConsoleLevel(levelValue);
    }

}
