package io.github.wycst.wast.log;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * @Author: wangy
 * @Date: 2021/8/22 18:32
 * @Description:
 */
public class LoggerManagerHandler {

    private static final Map<String, String> properties = new HashMap<String, String>();
    private static final String LEVEL_KEY = "logger.level";
    private static Level level;

    private static Map<String, FileHandlerHolder> fileHandlerHolders = new ConcurrentHashMap<String, FileHandlerHolder>();
    private static Map<String, Level> levelMap = new HashMap<String, Level>();
    private static ConsoleHandler consoleHandler;

    private static final Map<String, String> loggerProperties = new HashMap<String, String>();
    private static String consolePackages;

    static void init() {
//        // 设置自定义的LogManager
//        System.setProperty("java.util.logging.manager", LoggerManager.class.getName());
//        InputStream is = LogFactory.class.getResourceAsStream("/logging.properties");
//        if (is != null) {
//            try {
//                LogManager.getLogManager().reset();
//                LogManager.getLogManager().readConfiguration(is);
//            } catch (IOException e) {
//            }
//        }
        InputStream is = LogFactory.class.getResourceAsStream("/logging.properties");
        if (is != null) {
            Properties properties = new Properties();
            try {
                properties.load(is);
                parseLoggerHandlers(properties);
            } catch (IOException e) {
            }
        }

    }

    private static void parseLoggerHandlers(Properties properties) {
        // 控制台日志输出
        consolePackages = properties.getProperty("logger.console.packages");
        consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new ConsoleFormatter());
        Level consoleLevel = getLevelProperty(properties, "logger.console.level", null);
        if (consoleLevel != null) {
            consoleHandler.setLevel(consoleLevel);
        }

        String loggerFileHandlers = properties.getProperty("logger.file.handlers");
        if (loggerFileHandlers != null) {
            String[] fileHandleNames = loggerFileHandlers.split(",");
            for (String fileHandleName : new HashSet<String>(Arrays.asList(fileHandleNames))) {
                String handlerPackages = properties.getProperty("logger.handler." + fileHandleName + ".packages");
                String handlerPattern = properties.getProperty("logger.handler." + fileHandleName + ".pattern");
                int limit = getIntProperty(properties, "logger.handler." + fileHandleName + ".limit", Integer.MAX_VALUE);
                int count = getIntProperty(properties, "logger.handler." + fileHandleName + ".count", 1);
                Level level = getLevelProperty(properties, "logger.handler." + fileHandleName + ".level", null);
                if (handlerPattern != null) {
                    try {
                        FileHandlerHolder fileHandlerHolder = new FileHandlerHolder(fileHandleName, handlerPattern, limit, count);
                        if (level != null) {
                            fileHandlerHolder.setLevel(level);
                        }
                        fileHandlerHolder.setPackages(handlerPackages);
                        fileHandlerHolders.put(fileHandleName, fileHandlerHolder);
                    } catch (IOException e) {
                    }
                }
            }
        }

        for (String key : properties.stringPropertyNames()) {
            if (key.endsWith(".level")) {
                String customPackageLevel = key.substring(0, key.length() - 6);
                Level level = getLevelProperty(properties, key, null);
                if (level != null) {
                    levelMap.put(customPackageLevel, level);
                }
            }
        }

    }

    static List<Handler> matchHandlers(Class<?> logCls) {
        List<Handler> fileHandlers = new ArrayList<Handler>();
        // 通过class匹配文件handler
        String logClassName = logCls.getName();
        if (matchPackages(logClassName, consolePackages)) {
            fileHandlers.add(consoleHandler);
        }
        for (FileHandlerHolder fileHandlerHolder : fileHandlerHolders.values()) {
            FileHandler fileHandler = fileHandlerHolder.getFileHandler();
            String packages = fileHandlerHolder.getPackages();
            if (matchPackages(logClassName, packages)) {
                fileHandlers.add(fileHandler);
            }
        }
        return fileHandlers;
    }

    static Level matchLevel(Class<?> logCls) {
        String logClassName = logCls.getName();
        for (String pk : levelMap.keySet()) {
            if (logClassName.startsWith(pk) || logClassName.matches(pk)) {
                return levelMap.get(pk);
            }
        }
        return null;
    }

    private static boolean matchPackages(String logClassName, String packages) {
        if (packages == null || packages.length() == 0) {
            return true;
        }
        String[] pcks = packages.split(",");
        for (String pck : pcks) {
            if (logClassName.startsWith(pck)) {
                return true;
            }
        }
        return false;
    }

    private static Level getLevelProperty(Properties properties, String levelKey, Level level) {
        String levelName = properties.getProperty(levelKey);
        if (levelName == null || levelName.length() == 0) {
            return level;
        }
        return Level.parse(levelName);
    }

    private static int getIntProperty(Properties properties, String key, int i) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (Throwable throwable) {
        }
        return i;
    }

    public static void putAll(Map<String, String> logProps) {
        if (logProps != null) {
            properties.putAll(logProps);
        }
    }

    static Set<String> logKeySet() {
        return properties.keySet();
    }

    static void putLogProperty(String key, String value) {
        properties.put(key, value);
    }

    static String getLogProperty(String key) {
        return properties.get(key);
    }

    public static void clear() {
        properties.clear();
        levelMap.clear();
        for (FileHandlerHolder handlerHolder : fileHandlerHolders.values()) {
            handlerHolder.clear();
        }
        fileHandlerHolders.clear();
    }

    public static Level getLogLevel() {
        String levelName = properties.get(LEVEL_KEY);
        if (levelName == null) {
            return null;
        }
        if (level != null && levelName.equals(level.getName())) {
            return level;
        }
        try {
            level = Level.parse(levelName);
            return level;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return null;
    }
}
