package io.github.wycst.wast.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Date 2024/10/9 14:04
 * @Created by wangyc
 */
class JSONReaderCallbackImpl extends JSONReaderCallback {

    private final String pathMatcher;
    private final boolean onlyLeaf;

    public JSONReaderCallbackImpl(String pathMatcher) {
        this(pathMatcher, false);
    }

    public JSONReaderCallbackImpl(String pathMatcher, boolean onlyLeaf) {
        this.pathMatcher = pathMatcher;
        this.onlyLeaf = onlyLeaf;
    }

    @Override
    protected Object created(String path, int type) throws Exception {
        if (onlyLeaf || !path.matches(pathMatcher)) return null;
        if ((type == 1)) {
            return new LinkedHashMap();
        } else {
            return new ArrayList();
        }
    }

    @Override
    protected void parseValue(String key, Object value, Object host, int elementIndex, String path) throws Exception {
        if (path.matches(pathMatcher)) {
            results.add(value);
        }
        if (!onlyLeaf && host != null) {
            if (host instanceof Map) {
                ((Map) host).put(key, value);
            } else {
                ((List) host).add(value);
            }
        }
    }
}
