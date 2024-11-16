package io.github.wycst.wast.json;

import java.util.ArrayList;
import java.util.List;

/***
 * Response parsing process through callback (subscription) mode
 * Hook mode, non asynchronous call
 */
public abstract class JSONReaderHook {

    private boolean abored;
    protected List<Object> results = new ArrayList<Object>();

    private String filterRegular;

    public JSONReaderHook() {
    }

    public void setFilterRegular(String... pathRegularSegments) {
        this.filterRegular = buildRegular(pathRegularSegments);
    }

    public void clearFilterRegular(String... pathRegularSegments) {
        this.filterRegular = null;
    }

    final static String buildRegular(String... pathRegularSegments) {
        StringBuilder regularBuilder = new StringBuilder();
        int i = 0;
        for (String pathRegularSegment : pathRegularSegments) {
            if (i++ > 0) {
                regularBuilder.append("(");
            }
            regularBuilder.append("\\/");
            regularBuilder.append(pathRegularSegment);
        }
        while (--i > 0) {
            regularBuilder.append(")?");
        }
        return regularBuilder.toString();
    }

    /**
     * 是否跳过
     *
     * @param path
     * @param type
     * @return
     */
    protected boolean filter(String path, int type) {
        if (filterRegular == null) {
            return true;
        }
        return path == "" || path.matches(filterRegular);
    }

    /**
     * Give the initiative to build the object to the caller. If the type is 1, create a map or object. If the type is 2, create a collection object
     *
     * @param path JSON Absolute Path
     * @param type 1. Object type; 2 Collection type
     * @return
     * @throws Exception
     */
    protected Object created(String path, int type) throws Exception {
        return null;
    }

    /**
     * Assign property settings to the caller.
     * If you want to interrupt and exit the read, you can call abort()
     *
     * @param key          the key if object({}), otherwise null
     * @param value        map/collection/string/number
     * @param host         object or collection
     * @param elementIndex the index if collection, otherwise -1
     * @param path         JSON Absolute Path
     * @param type
     * @throws Exception
     */
    protected abstract void parseValue(String key, Object value, Object host, int elementIndex, String path, int type) throws Exception;

    public void reset() {
        if (results != null) {
            results.clear();
        }
        this.abored = false;
    }

    /**
     * Parse completed callback
     *
     * @param result
     */
    protected void onCompleted(Object result) {
    }

    /**
     * terminate read operation
     */
    protected final void abort() {
        this.abored = true;
    }

    protected final boolean isAbored() {
        return abored;
    }

    /**
     * path解析完成触发调用;
     *
     * <p> 如果返回true则会终止读取；子类可重写实现自定义终止的时机;
     *
     * @param value
     * @param path JSON Absolute Path
     * @param type  1 对象； 2 数组；
     * @return abort if true;
     */
    protected boolean isAboredOnParsed(Object value, String path, int type) {
        return false;
    }

    /**
     * 构建JSONReaderCallback
     *
     * @param regular 正则表达式
     * @return
     */
    public final static JSONReaderHook regularPath(String regular) {
        return new JSONReaderHookRegular(regular);
    }

    public final static JSONReaderHook regularPath(String regular, boolean onlyLeaf) {
        return new JSONReaderHookRegular(regular, onlyLeaf);
    }

    /**
     * 构建JSONReaderCallback
     *
     * @param exactPath 路径
     * @return
     */
    public final static JSONReaderHook exactPath(String exactPath) {
        return new JSONReaderHookExact(exactPath);
    }

    public final List<Object> getResults() {
        return results;
    }
}
