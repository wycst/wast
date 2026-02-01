package io.github.wycst.wast.json;

import io.github.wycst.wast.json.annotations.JsonProperty;

/**
 * 字段定义，@JsonProperty注解的编码实现
 *
 * @Author: wangy
 * @Description: 字段定义
 * @Date: 2026/1/23 16:05
 * @see JsonProperty
 */
public class JSONPropertyDefinition {

    /**
     * @see JsonProperty#name()
     */
    String name;
    /**
     * @see JsonProperty#serialize()
     */
    boolean serialize;
    /**
     * @see JsonProperty#deserialize()
     */
    boolean deserialize;
    /**
     * @see JsonProperty#mapper()
     */
    Class<? extends JSONTypeFieldMapper> mapper;
    /**
     * @see JsonProperty#pattern()
     */
    String pattern;
    /**
     * @see JsonProperty#asTimestamp()
     */
    boolean asTimestamp;
    /**
     * @see JsonProperty#timezone()
     */
    String timezone;
    /**
     * @see JsonProperty#impl()
     */
    Class<?> impl;
    /**
     * @see JsonProperty#possibleTypes()
     */
    Class<?>[] possibleTypes;
    /**
     * @see JsonProperty#possibleExpression()
     */
    String possibleExpression;
    /**
     * @see JsonProperty#unfixedType()
     */
    boolean unfixedType;

    public JSONPropertyDefinition() {
        this("", true, true, JSONTypeFieldMapper.class, "", false, "", Object.class, new Class<?>[0], "", false);
    }

    public JSONPropertyDefinition(String name) {
        this(name, true, true, JSONTypeFieldMapper.class, "", false, "", Object.class, new Class<?>[0], "", false);
    }

    public JSONPropertyDefinition(String name, boolean serialize, boolean deserialize) {
        this(name, serialize, deserialize, null, "", false, "", Object.class, new Class<?>[0], "", false);
    }

    public static JSONPropertyDefinition of(JsonProperty jsonProperty) {
        return jsonProperty == null ? null : new JSONPropertyDefinition(
                jsonProperty.name().trim(),
                jsonProperty.serialize(),
                jsonProperty.deserialize(),
                jsonProperty.mapper(),
                jsonProperty.pattern().trim(),
                jsonProperty.asTimestamp(),
                jsonProperty.timezone().trim(),
                jsonProperty.impl(),
                jsonProperty.possibleTypes(),
                jsonProperty.possibleExpression(),
                jsonProperty.unfixedType());
    }

    public JSONPropertyDefinition(String name, boolean serialize, boolean deserialize, Class<? extends JSONTypeFieldMapper> mapper, String pattern, boolean asTimestamp, String timezone, Class<?> impl, Class<?>[] possibleTypes, String possibleExpression, boolean unfixedType) {
        this.name = name == null ? "" : name.trim();
        this.serialize = serialize;
        this.deserialize = deserialize;
        this.mapper = mapper == null ? JSONTypeFieldMapper.class : mapper;
        this.pattern = pattern == null ? "" : pattern.trim();
        this.asTimestamp = asTimestamp;
        this.timezone = timezone;
        this.impl = impl == null ? Object.class : impl;
        this.possibleTypes = possibleTypes == null ? new Class<?>[0] : possibleTypes;
        this.possibleExpression = possibleExpression == null ? "" : possibleExpression.trim();
        this.unfixedType = unfixedType;
    }

    public void merge(JSONPropertyDefinition source) {
        if (source == null) return;
        if (name().isEmpty()) {
            this.name = source.name();
        }
        if (mapper() == JSONTypeFieldMapper.class) {
            this.mapper = source.mapper();
        }
        if (pattern().isEmpty()) {
            this.pattern = source.pattern();
        }
        if (timezone().isEmpty()) {
            this.timezone = source.timezone();
        }
        if (impl() == Object.class) {
            this.impl = source.impl();
        }
        if (possibleTypes().length == 0) {
            this.possibleTypes = source.possibleTypes();
        }
        if (possibleExpression().isEmpty()) {
            this.possibleExpression = source.possibleExpression();
        }
    }

    public String name() {
        return name == null ? "" : name.trim();
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean serialize() {
        return serialize;
    }

    public void setSerialize(boolean serialize) {
        this.serialize = serialize;
    }

    public boolean deserialize() {
        return deserialize;
    }

    public void setDeserialize(boolean deserialize) {
        this.deserialize = deserialize;
    }

    public Class<? extends JSONTypeFieldMapper> mapper() {
        return mapper == null ? JSONTypeFieldMapper.class : mapper;
    }

    public void setMapper(Class<? extends JSONTypeFieldMapper> mapper) {
        this.mapper = mapper;
    }

    public String pattern() {
        return pattern == null ? "" : pattern.trim();
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean asTimestamp() {
        return asTimestamp;
    }

    public void setAsTimestamp(boolean asTimestamp) {
        this.asTimestamp = asTimestamp;
    }

    public String timezone() {
        return timezone == null ? "" : timezone.trim();
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Class<?> impl() {
        return impl == null ? Object.class : impl;
    }

    public void setImpl(Class<?> impl) {
        this.impl = impl;
    }

    public Class<?>[] possibleTypes() {
        return possibleTypes == null ? new Class<?>[0] : possibleTypes;
    }

    public void setPossibleTypes(Class<?>[] possibleTypes) {
        this.possibleTypes = possibleTypes;
    }

    public String possibleExpression() {
        return possibleExpression == null ? "" : possibleExpression.trim();
    }

    public void setPossibleExpression(String possibleExpression) {
        this.possibleExpression = possibleExpression;
    }

    public boolean unfixedType() {
        return unfixedType;
    }

    public void setUnfixedType(boolean unfixedType) {
        this.unfixedType = unfixedType;
    }
}
