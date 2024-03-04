package io.github.wycst.wast.common.csv;

import io.github.wycst.wast.common.utils.ObjectUtils;

/**
 * @Date 2024/1/12 9:21
 * @Created by wangyc
 */
public abstract class CSVTypeHandler<T> {

    public final static class DefaultCSVTypeHandler extends CSVTypeHandler {
        @Override
        public Object handle(String input, Class type) {
            return ObjectUtils.toType(input, type);
        }
    }

    public CSVTypeHandler() {
    }

    public abstract T handle(String input, Class<T> type) throws Throwable;

}
