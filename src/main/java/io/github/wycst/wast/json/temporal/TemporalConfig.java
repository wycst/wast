package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.JSONPropertyDefinition;

public class TemporalConfig {

    final GenericParameterizedType<?> genericParameterizedType;
    final String pattern;

    TemporalConfig(GenericParameterizedType<?> genericParameterizedType, JSONPropertyDefinition propertyDefinition) {
        this.genericParameterizedType = genericParameterizedType;
        String pattern = null;
        if (propertyDefinition != null) {
            pattern = propertyDefinition.pattern().trim();
            if (pattern.isEmpty()) {
                pattern = null;
            }
        }
        this.pattern = pattern;
    }

    public static TemporalConfig of(GenericParameterizedType<?> genericParameterizedType, JSONPropertyDefinition propertyDefinition) {
        return new TemporalConfig(genericParameterizedType, propertyDefinition);
    }

    public GenericParameterizedType<?> getGenericParameterizedType() {
        return genericParameterizedType;
    }

    public String getDatePattern() {
        return pattern;
    }
}
