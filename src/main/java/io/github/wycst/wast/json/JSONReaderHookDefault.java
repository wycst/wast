package io.github.wycst.wast.json;

import java.util.*;

/**
 * @Date 2024/10/9 14:04
 * @Created by wangyc
 */
public class JSONReaderHookDefault extends JSONReaderHook {

    public JSONReaderHookDefault() {
    }

    @Override
    protected Map createdMap(String path) {
        return new LinkedHashMap();
    }

    @Override
    protected Collection createdCollection(String path) {
        return new ArrayList();
    }

    @Override
    protected void parseValue(String key, Object value, Object host, int elementIndex, String path, int type) throws Exception {
        if (host instanceof Map) {
            ((Map) host).put(key, value);
        } else {
            ((List) host).add(value);
        }
    }
}
