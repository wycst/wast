package io.github.wycst.wast.json;

import java.util.*;

/**
 * @Date 2024/10/9 14:04
 * @Created by wangyc
 */
final class JSONReaderHookExact extends JSONReaderHook {

    private final String exactPath;

    public JSONReaderHookExact(String exactPath) {
        this.exactPath = exactPath;
    }

    @Override
    protected Map createdMap(String path) {
        return path.equals(exactPath) ? new LinkedHashMap() : null;
    }

    @Override
    protected Collection<?> createdCollection(String path) {
        return path.equals(exactPath) ? new ArrayList<Object>() : null;
    }

    @Override
    protected void parseValue(String key, Object value, Object host, int elementIndex, String path, int type) throws Exception {
        if (type > 2 && path.equals(exactPath)) {
            // leaf node
            results.add(value);
            abort();
            return;
        }
        if (host == null) return;
        if (host instanceof Map) {
            ((Map) host).put(key, value);
        } else {
            ((List) host).add(value);
        }
    }

    @Override
    protected boolean isAboredOnParsed(Object value, String path, int type) {
        boolean matched = path.equals(exactPath);
        if (matched) {
            results.add(value);
        }
        return matched;
    }
}
