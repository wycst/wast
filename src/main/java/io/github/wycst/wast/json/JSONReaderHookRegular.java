package io.github.wycst.wast.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @Date 2024/10/9 14:04
 * @Created by wangyc
 */
final class JSONReaderHookRegular extends JSONReaderHook {

    private final String regular;
    final Pattern pattern;
    private final boolean onlyLeaf;
    public JSONReaderHookRegular(String regular) {
        this(regular, false);
    }

    public JSONReaderHookRegular(String regular, boolean onlyLeaf) {
        this.pattern = Pattern.compile(this.regular = regular);
        this.onlyLeaf = onlyLeaf;
    }

    @Override
    protected Object created(String path, int type) throws Exception {
        if (onlyLeaf || !pattern.matcher(path).matches()) return null;
        if ((type == 1)) {
            return new LinkedHashMap();
        } else {
            return new ArrayList();
        }
    }

    @Override
    protected void parseValue(String key, Object value, Object host, int elementIndex, String path, int type) throws Exception {
        if (pattern.matcher(path).matches()) {
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
