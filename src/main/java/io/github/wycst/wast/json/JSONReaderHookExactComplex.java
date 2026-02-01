package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;

/**
 * @Date 2024/10/9 14:04
 * @Created by wangyc
 */
final class JSONReaderHookExactComplex extends JSONReaderHook {

    private final String exactPath;
    private final GenericParameterizedType<?> parameterizedType;

    public JSONReaderHookExactComplex(String exactPath, GenericParameterizedType<?> parameterizedType) {
        this.exactPath = exactPath;
        this.parameterizedType = parameterizedType;
        parameterizedType.getClass();
    }

    protected boolean filter(String path, int type) {
        return true;
    }

    @Override
    protected GenericParameterizedType<?> getParameterizedType(String path) {
        return path.equals(exactPath) ? parameterizedType : null;
    }

    @Override
    protected void parseValue(String key, Object value, Object host, int elementIndex, String path, int type) throws Exception {
    }

    @Override
    protected boolean isAboredOnParsed(Object value, String path, int type) {
        if (path.equals(exactPath)) {
            results.add(value);
            return true;
        }
        return false;
    }
}
