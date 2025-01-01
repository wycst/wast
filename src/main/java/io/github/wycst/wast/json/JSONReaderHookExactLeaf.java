package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.utils.ObjectUtils;

/**
 * @Date 2024/10/9 14:04
 * @Created by wangyc
 */
final class JSONReaderHookExactLeaf extends JSONReaderHook {

    private final String exactPath;
    private final GenericParameterizedType parameterizedType;

    public JSONReaderHookExactLeaf(String exactPath, GenericParameterizedType parameterizedType) {
        this.exactPath = exactPath;
        this.parameterizedType = parameterizedType;
        parameterizedType.getClass();
    }

    protected boolean filter(String path, int type) {
        return true;
    }

    @Override
    protected void parseValue(String key, Object value, Object host, int elementIndex, String path, int type) throws Exception {
        if (type > 2 && path.equals(exactPath)) {
            results.add(ObjectUtils.toType(value, parameterizedType.getActualType(), parameterizedType.getActualClassCategory()));
            abort();
        }
    }
}
