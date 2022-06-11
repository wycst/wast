package io.github.wycst.wast.log;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;

/**
 * 日志文件handler
 *
 * @Author wangyunchao
 * @Date 2021/8/26 16:53
 */
public class FileHandlerHolder {

    private final String handlerName;
    //    private String pattern;
//    private int limit;
//    private int count;
//    private boolean append = true;
    private final FileHandler fileHandler;
    private Level level;
    private String packages;

    FileHandlerHolder(String handlerName, String pattern, int limit, int count) throws IOException {
        this(handlerName, pattern, limit, count, true);
    }

    FileHandlerHolder(String handlerName, String pattern, int limit, int count, boolean append) throws IOException {
        this.handlerName = handlerName;
        if (pattern == null || pattern.length() == 0) {
            throw new IllegalArgumentException("请指定日志文件的名称表达式");
        }
        this.fileHandler = new FileHandler(pattern, limit, count, append);
        fileHandler.setFormatter(new TextFormatter());
    }

    public void setLevel(Level level) {
        this.level = level;
        fileHandler.setLevel(level);
    }

    public Level getLevel() {
        return level;
    }

    public FileHandler getFileHandler() {
        return fileHandler;
    }

    public void setPackages(String packages) {
        this.packages = packages;
    }

    public String getPackages() {
        return packages;
    }

    public void clear() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
}
