package io.github.wycst.wast.json;

import java.util.ArrayList;
import java.util.List;

/***
 * Response parsing process through callback (subscription) mode
 * Hook mode, non asynchronous call
 */
public class JSONReaderCallback {

    private boolean abored;
    protected List<Object> results = new ArrayList<Object>();

    private String filterRegular;

    public JSONReaderCallback() {
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
     * @param path JSON XPATH
     * @param type 1. Object type; 2 Collection type
     * @return
     * @throws Exception
     */
    protected Object created(String path, int type) throws Exception {
        return null;
    }

    /**
     * Assign property settings to the caller
     *
     * @param key          the key if object({}), otherwise null
     * @param value        map/collection/string/number
     * @param host         object or collection
     * @param elementIndex the index if collection([]), otherwise -1
     * @param path         JSON XPATH
     * @throws Exception
     */
    protected void parseValue(String key, Object value, Object host, int elementIndex, String path) throws Exception {
    }

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

    final boolean isAbored() {
        return abored;
    }

    public final static JSONReaderCallback matcherOf(String matcher) {
        return new JSONReaderCallbackImpl(matcher);
    }

    public final static JSONReaderCallback matcherOf(String matcher, boolean onlyLeaf) {
        return new JSONReaderCallbackImpl(matcher, onlyLeaf);
    }

    public final List<Object> getResults() {
        return results;
    }
}
