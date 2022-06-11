//package io.github.wycst.wast.log;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.security.AccessController;
//import java.security.PrivilegedAction;
//import java.util.logging.ErrorManager;
//import java.util.logging.FileHandler;
//import java.util.logging.Level;
//import java.util.logging.LogRecord;
//
///**
// * @Author: wangy
// * @Date: 2020/5/6 16:04
// * @Description:
// */
//public class LogFileHandler extends FileHandler {
//
//    public LogFileHandler() throws IOException, SecurityException {
//        this.checkPatternFileDir();
//    }
//
//    private void checkPatternFileDir() {
//    }
//
//    /**
//     * copy FileHandler 实现
//     * 修改rotate函数
//     *
//     * @param record
//     */
//    @Override
//    public synchronized void publish(LogRecord record) {
//        if (!isLoggable(record)) {
//            return;
//        }
//
//        super.publish(record);
//        flush();
//        AccessController.doPrivileged(new PrivilegedAction() {
//            public Object run() {
//                return null;
//            }
//        });
//
//    }
//}
