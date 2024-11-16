package io.github.wycst.wast.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Date 2024/10/9 14:04
 * @Created by wangyc
 */
public class JSONReaderHookDefault extends JSONReaderHook {

    public JSONReaderHookDefault() {
    }

    @Override
    protected Object created(String path, int type) throws Exception {
        if (type == 1) {
            return new LinkedHashMap();
        } else {
            return new ArrayList();
        }
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
