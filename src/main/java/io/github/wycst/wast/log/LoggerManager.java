//package io.github.wycst.wast.log;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * @Author: wangy
// * @Date: 2020/5/5 23:51
// * @Description:
// */
//public class LoggerManager extends java.util.logging.LogManager {
//
//    private String rootHandlers;
//
//    @Override
//    public void reset() throws SecurityException {
//        super.reset();
//        LoggerManagerHandler.clear();
//        rootHandlers = null;
//    }
//
//    /**
//     * 重写属性转换
//     * <p>
//     * logger.rootLogger=R,io.github.wycst.wast.log.LogConsoleHandler
//     * <p>
//     * logger.R.handler=io.github.wycst.wast.log.LogConsoleHandler
//     * logger.R.handler.level=FINER
//     * logger.R.handler.formatter=io.github.wycst.wast.log.TextFormatter
//     * <p>
//     * io.github.wycst.wast.log.LogConsoleHandler.level=FINER
//     * io.github.wycst.wast.log.LogConsoleHandler.formatter=io.github.wycst.wast.log.TextFormatter
//     * <p>
//     * io.github.wycst.wast.log.LogConsoleHandler.level -> logger.handler.R.level
//     *
//     * @param name
//     * @return
//     */
//    @Override
//    public String getProperty(String name) {
//        String propertyValue = super.getProperty(name);
//        if (propertyValue != null) {
//            return propertyValue;
//        }
//        if ("handlers".equals(name)) {
//            return rootHandlers;
//        }
//        // java.util.logging.FileHandler.pattern
//        for (String key : LoggerManagerHandler.logKeySet()) {
//            // name = io.github.wycst.wast.log.LogConsoleHandler.xxx
//            // propKey = logger.R.handler.level
//            // key = io.github.wycst.wast.log.LogConsoleHandler
//            // value = logger.R.handler
//            if (name.startsWith(key + ".")) {
//                String value = LoggerManagerHandler.getLogProperty(key);
//                String transformKey = name.replace(key, value);
//                propertyValue = super.getProperty(transformKey);
//                break;
//            }
//        }
//        if("java.util.logging.FileHandler.pattern".equals(name)) {
//            // 如果目录不存在创建目录
//            File file = new File(propertyValue);
//            if(!file.exists()) {
//                File parentFile = file.getParentFile();
//                if(!parentFile.exists()) {
//                    parentFile.mkdirs();
//                }
//            }
//        }
//        return propertyValue;
//    }
//
//    @Override
//    public void readConfiguration(InputStream ins) throws IOException, SecurityException {
//        super.readConfiguration(ins);
//
//        String rootLoggers = super.getProperty("logger.rootLogger");
//        if (rootLoggers != null) {
//            // R,io.github.wycst.wast.log.LogConsoleHandler
//            StringBuffer handlerBuff = new StringBuffer();
//            String[] rootLoggerNames = rootLoggers.split(",");
//            for (String rootLoggerName : rootLoggerNames) {
//                String key = "logger." + rootLoggerName + ".handler";
//                String loggerHandlerCls = super.getProperty(key);
//                if (loggerHandlerCls != null) {
//                    handlerBuff.append(loggerHandlerCls);
//                    LoggerManagerHandler.putLogProperty(loggerHandlerCls, key);
//                } else {
//                    handlerBuff.append(rootLoggerName);
//                }
//                handlerBuff.append(",");
//            }
//            handlerBuff.deleteCharAt(handlerBuff.length() - 1);
//            rootHandlers = handlerBuff.toString();
//        }
//    }
//
//
//}
