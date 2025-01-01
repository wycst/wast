package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.ReflectConsts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    protected GenericParameterizedType getParameterizedType(String path) {
        return null;
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
     * The method will no longer be used in future versions. Please use createdMap and createdCollection instead
     *
     * @param path JSON Absolute Path
     * @param type 1. Object type; 2 Collection type
     * @return
     * @throws Exception
     * @see #createdMap(String)
     * @see #createdCollection(String)
     */
    @Deprecated
    protected Object created(String path, int type) throws Exception {
        return null;
    }

    protected Map createdMap(String path) {
        return null;
    }

    /**
     * @param path JSON Absolute Path
     * @return
     * @throws Exception
     */
    protected Collection createdCollection(String path) {
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
     * path解析完成触发调用(仅限对象或者数组);
     *
     * <p> 如果返回true则会终止读取；子类可重写实现自定义终止的时机;
     *
     * @param value
     * @param path  JSON Absolute Path
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

    /**
     * 构建JSONReaderCallback
     *
     * @param exactPath  路径
     * @param actualType 目标类型
     * @return
     */
    public final static JSONReaderHook exactPathAs(String exactPath, Class<?> actualType) {
        return exactPathAs(exactPath, GenericParameterizedType.actualType(actualType));
    }

    /**
     * 构建JSONReaderCallback
     *
     * @param exactPath         路径
     * @param parameterizedType 目标类型
     * @return
     */
    public final static JSONReaderHook exactPathAs(String exactPath, GenericParameterizedType parameterizedType) {
        ReflectConsts.ClassCategory classCategory = parameterizedType.getActualClassCategory();
        switch (classCategory) {
            case ANY:
                return new JSONReaderHookExact(exactPath);
            case ArrayCategory:
            case CollectionCategory:
            case MapCategory:
            case ObjectCategory:
                return new JSONReaderHookExactComplex(exactPath, parameterizedType);
            default:
                return new JSONReaderHookExactLeaf(exactPath, parameterizedType);
        }
    }

    public final List<Object> getResults() {
        return results;
    }

    public final <E> E first() {
        return results.isEmpty() ? null : (E) results.get(0);
    }
}
