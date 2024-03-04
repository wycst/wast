package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.annotations.JsonProperty;

public class TemporalConfig {

    final GenericParameterizedType genericParameterizedType;
    final String pattern;

    TemporalConfig(GenericParameterizedType genericParameterizedType, JsonProperty jsonProperty) {
        this.genericParameterizedType = genericParameterizedType;
        String pattern = null;
        if (jsonProperty != null) {
            pattern = jsonProperty.pattern().trim();
            if (pattern.length() == 0) {
                pattern = null;
            }
        }
        this.pattern = pattern;
    }

    public static TemporalConfig of(GenericParameterizedType genericParameterizedType, JsonProperty jsonProperty) {
        return new TemporalConfig(genericParameterizedType, jsonProperty);
    }

    public GenericParameterizedType getGenericParameterizedType() {
        return genericParameterizedType;
    }

    public String getDatePattern() {
        return pattern;
    }
}
