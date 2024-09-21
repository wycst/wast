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

    private static final Map<String, String> LOGGER_PROPERTIES = new HashMap<String, String>();
    private static final String LEVEL_KEY = "logger.level";
    private static Level level;

    private static Map<String, FileHandlerHolder> fileHandlerHolders = new ConcurrentHashMap<String, FileHandlerHolder>();
    private static Map<String, Level> levelMap = new HashMap<String, Level>();
    private static ConsoleHandler consoleHandler;
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
                LOGGER_PROPERTIES.putAll((Map) properties);
                parseLoggerHandlers();
            } catch (IOException e) {
            }
        } else {
            // default
            parseLoggerHandlers();
        }
    }

    static void setConsoleLevel(Level level) {
        consoleHandler.setLevel(level);
    }

    private static void parseLoggerHandlers() {
        setConsoleHandler();
        setFileHandlers();
        initLevelMap();
    }

    private synchronized static void setConsoleHandler() {
        if (consoleHandler == null) {
            consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new ConsoleFormatter());
        }
        consolePackages = LOGGER_PROPERTIES.get("logger.console.packages");
        Level consoleLevel = getLevel(LOGGER_PROPERTIES.get("logger.console.level"), null);
        if (consoleLevel != null) {
            consoleHandler.setLevel(consoleLevel);
        }
    }

    private static void setFileHandlers() {
        fileHandlerHolders.clear();
        String loggerFileHandlers = LOGGER_PROPERTIES.get("logger.file.handlers");
        if (loggerFileHandlers != null) {
            String[] fileHandleNames = loggerFileHandlers.split(",");
            for (String fileHandleName : new HashSet<String>(Arrays.asList(fileHandleNames))) {
                String handlerPackages = LOGGER_PROPERTIES.get("logger.handler." + fileHandleName + ".packages");
                String handlerPattern = LOGGER_PROPERTIES.get("logger.handler." + fileHandleName + ".pattern");
                int limit = getInt(LOGGER_PROPERTIES.get("logger.handler." + fileHandleName + ".limit"), Integer.MAX_VALUE);
                int count = getInt(LOGGER_PROPERTIES.get("logger.handler." + fileHandleName + ".count"), 1);
                Level level = getLevel(LOGGER_PROPERTIES.get("logger.handler." + fileHandleName + ".level"), null);
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
    }

    private synchronized static void initLevelMap() {
        levelMap.clear();
        for (Map.Entry<String, String> entry : LOGGER_PROPERTIES.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith(".level")) {
                String customPackageLevel = key.substring(0, key.length() - 6);
                Level level = getLevel(entry.getValue(), null);
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
            if(consoleHandler != null) {
                fileHandlers.add(consoleHandler);
            }
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
        for (Map.Entry<String, Level> entry : levelMap.entrySet()) {
            String pk = entry.getKey();
            if (logClassName.startsWith(pk) || logClassName.matches(pk)) {
                return entry.getValue();
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

//    private static Level getLevelProperty(Properties properties, String levelKey, Level level) {
//        String levelName = properties.getProperty(levelKey);
//        if (levelName == null || levelName.length() == 0) {
//            return level;
//        }
//        return Level.parse(levelName);
//    }

    private static Level getLevel(String levelName, Level level) {
        if (levelName == null || levelName.length() == 0) {
            return level;
        }
        return Level.parse(levelName);
    }


    private static int getInt(String value, int i) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable throwable) {
        }
        return i;
    }

//    private static int getIntProperty(Properties properties, String key, int i) {
//        try {
//            return Integer.parseInt(properties.getProperty(key));
//        } catch (Throwable throwable) {
//        }
//        return i;
//    }

    public static void putAll(Map<String, String> logProps) {
        if (logProps != null) {
            LOGGER_PROPERTIES.putAll(logProps);
            parseLoggerHandlers();
        }
    }

    static Set<String> logKeySet() {
        return LOGGER_PROPERTIES.keySet();
    }

    static void putLogProperty(String key, String value) {
        LOGGER_PROPERTIES.put(key, value);
    }

    static String getLogProperty(String key) {
        return LOGGER_PROPERTIES.get(key);
    }

    public static void clear() {
        LOGGER_PROPERTIES.clear();
        levelMap.clear();
        for (FileHandlerHolder handlerHolder : fileHandlerHolders.values()) {
            handlerHolder.clear();
        }
        fileHandlerHolders.clear();
    }

    public static Level getLogLevel() {
        String levelName = LOGGER_PROPERTIES.get(LEVEL_KEY);
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
