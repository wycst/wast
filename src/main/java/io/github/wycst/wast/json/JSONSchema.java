package io.github.wycst.wast.json;

import io.github.wycst.wast.json.annotations.JsonProperty;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * JSONSchema简化版实现
 * <p>
 * 参考文档: <br>
 * <a href="https://json-schema.org/understanding-json-schema/reference/type">https://json-schema.org/understanding-json-schema/reference/type</a>
 *
 * <br>
 * <p>
 * JSON 字段定义/校验, 绑定JSONNode实现.
 *
 * @Author: wangy
 */
public final class JSONSchema extends JSONSchemaBase implements Serializable {

    private String id;
    private String $schema;
    private String $ref;
    // 适配JSONSchema官网定义: 标题
    private String title;
    // 适配JSONSchema官网定义: 描述
    private String description;
    // 适配JSONSchema官网定义: 类型
    @JsonProperty(possibleTypes = {JSONType.class, JSONType[].class})
    private final Serializable type;

    @JsonProperty(name = "enum")
    private String[] enums;

    // 数据格式: url | email | date | time | datetime | regex | ip | ipv4 | ipv6 | hostname | uri | uri-reference
    // 当前仅仅支持: url | email | date
    private String format;
    private Map<Serializable, JSONSchema> definitions;
    @JsonProperty(deserialize = false, serialize = false)
    Map<Serializable, JSONSchema> __definitions;
    // 适配JSONSchema官网定义: 属性映射
    private Map<Serializable, JSONSchema> properties;
    // 适配JSONSchema官网定义: 必填字段编码集合
    private Set<String> required;

    // 数组（集合）长度固定的场景下可以给每个元素指定不同的Schema
    @JsonProperty(possibleTypes = {JSONSchema.class, JSONSchema[].class})
    private Object items;

    // 适配JSONSchema官网定义: 是否允许附加属性取值boolean或者JSONSchema类型（对象）
    @JsonProperty(possibleTypes = {Boolean.class, JSONSchema.class})
    private Object additionalProperties;
    // when the type is number, define min/max number
    private Number maximum;
    private Boolean exclusiveMaximum;
    private Number minimum;
    private Boolean exclusiveMinimum;
    // when the type is string, define min/max string length
    private Integer maxLength;
    private Integer minLength;
    // when the type is string, define pattern
    private String pattern;
    @JsonProperty(deserialize = false, serialize = false)
    private Pattern patternObject;

    // when the type is array, define min/max array size
    private Integer minItems;
    private Integer maxItems;
    // 数组唯一性
    private Boolean uniqueItems;

    @JsonProperty(possibleTypes = {JSONSchema.class, JSONSchema[].class})
    private Object allOf;

    @JsonProperty(possibleTypes = {JSONSchema.class, JSONSchema[].class})
    private Object anyOf;

    @JsonProperty(possibleTypes = {JSONSchema[].class, JSONSchema.class})
    private Object oneOf;

    @JsonProperty(name = "default")
    private Serializable defaultValue;

    private Map<String, Object> dependencies;

    // when the type is array, define child elementSchema
    private JSONSchema elementSchema;

    /**
     * 是否禁止出现fields中不存在的字段
     */
    private Boolean disableExtra;

    /**
     * 当前是否必填(和required冲突改名)
     */
    private Boolean must;

    /**
     * 校验规则列表
     */
    private List<JSONSchemaRule> rules;

    /**
     * 根据字符串构建JSONSchema
     *
     * @param schemaJson
     * @return
     */
    public static JSONSchema of(String schemaJson) {
        JSONSchema schema = JSON.parseObject(schemaJson, JSONSchema.class, PERFECT_READ_OPTIONS);
        schema.setRoot(schema);
        return schema;
    }

    public JSONSchema(JSONType type) {
        this.type = type;
    }

    public String get$schema() {
        return $schema;
    }

    public void set$schema(String $schema) {
        this.$schema = $schema;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    void setRoot(JSONSchema root) {
        this.root = root;
        if (definitions != null) {
            __definitions = new HashMap<Serializable, JSONSchema>();
            Set<Map.Entry<Serializable, JSONSchema>> entrySet = definitions.entrySet();
            for (Map.Entry<Serializable, JSONSchema> entry : entrySet) {
                String key = entry.getKey().toString();
                JSONSchema value = entry.getValue();
                if (value != null) {
                    value.setRoot(root);
                }
                __definitions.put("#/definitions/" + key, value);
            }
        }
        if (properties != null) {
            Set<Map.Entry<Serializable, JSONSchema>> entrySet = properties.entrySet();
            for (Map.Entry<Serializable, JSONSchema> entry : entrySet) {
                JSONSchema value = entry.getValue();
                if (value != null) {
                    value.setRoot(root);
                }
            }
        }
        if (elementSchema != null) {
            elementSchema.setRoot(root);
        }
        setRoot(root, items);
        setRoot(root, additionalProperties);
        setRoot(root, anyOf);
        setRoot(root, allOf);
        setRoot(root, oneOf);
    }

    void setRoot(JSONSchema root, Object target) {
        if (target instanceof JSONSchema) {
            JSONSchema value = (JSONSchema) target;
            value.setRoot(root);
        } else if (target instanceof JSONSchema[]) {
            JSONSchema[] values = (JSONSchema[]) target;
            for (JSONSchema value : values) {
                value.setRoot(root);
            }
        }
    }

    public String get$ref() {
        return $ref;
    }

    public void set$ref(String $ref) {
        this.$ref = $ref;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getType() {
        return type;
    }

    public String[] getEnums() {
        return enums;
    }

    public void setEnums(String[] enums) {
        this.enums = enums;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Map<Serializable, JSONSchema> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(Map<Serializable, JSONSchema> definitions) {
        this.definitions = definitions;
    }

    public Map<Serializable, JSONSchema> getProperties() {
        return properties;
    }

    public void setProperties(Map<Serializable, JSONSchema> properties) {
        this.properties = properties;
    }

    public Boolean getDisableExtra() {
        return disableExtra != null && disableExtra;
    }

    public void setDisableExtra(boolean disableExtra) {
        this.disableExtra = disableExtra;
    }

    public Boolean getMust() {
        return must != null && must;
    }

    public void setMust(Boolean must) {
        this.must = must;
    }

    public Set<String> getRequired() {
        return required;
    }

    public void setRequired(Set<String> required) {
        this.required = required;
    }

    public List<JSONSchemaRule> getRules() {
        return rules;
    }

    public void setRules(List<JSONSchemaRule> rules) {
        this.rules = rules;
    }

    public final boolean hasElementSchema() {
        return elementSchema != null || items != null;
    }

    public JSONSchema getElementSchemaAt(int index) {
        if (items == null) return elementSchema;
        if (items instanceof JSONSchema) {
            return (JSONSchema) items;
        }
        JSONSchema[] schemaItems = (JSONSchema[]) items;
        return index < schemaItems.length ? schemaItems[index] : null;
    }

    public JSONSchema getElementSchema() {
        return elementSchema;
    }

    public void setElementSchema(JSONSchema elementSchema) {
        this.elementSchema = elementSchema;
    }

    public Object getItems() {
        return items;
    }

    public void setItems(Object items) {
        this.items = items;
    }

    public Object getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Object additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public Integer getMinItems() {
        return minItems;
    }

    public void setMinItems(Integer minItems) {
        this.minItems = minItems;
    }

    public Integer getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
    }

    public Number getMaximum() {
        return maximum;
    }

    public void setMaximum(Number maximum) {
        this.maximum = maximum;
    }

    public Number getMinimum() {
        return minimum;
    }

    public void setMinimum(Number minimum) {
        this.minimum = minimum;
    }

    public Boolean getExclusiveMinimum() {
        return exclusiveMinimum == Boolean.TRUE;
    }

    public void setExclusiveMinimum(Boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public Boolean getExclusiveMaximum() {
        return exclusiveMaximum == Boolean.TRUE;
    }

    public void setExclusiveMaximum(Boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    public Boolean getUniqueItems() {
        return uniqueItems;
    }

    public void setUniqueItems(Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
    }

    public Object getOneOf() {
        return oneOf;
    }

    public void setOneOf(Object oneOf) {
        this.oneOf = oneOf;
    }

    public Object getAnyOf() {
        return anyOf;
    }

    public void setAnyOf(Object anyOf) {
        this.anyOf = anyOf;
    }

    public Object getAllOf() {
        return allOf;
    }

    public void setAllOf(Object allOf) {
        this.allOf = allOf;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public String getPattern() {
        return pattern;
    }

    public Pattern patternObject() {
        if (pattern == null) return null;
        if (patternObject != null) return patternObject;
        return patternObject = Pattern.compile(pattern);
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Serializable getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Serializable defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Map<String, Object> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Map<String, Object> dependencies) {
        this.dependencies = dependencies;
    }

    public void addRule(JSONSchemaRule rule) {
        if (rules == null) {
            rules = new ArrayList<JSONSchemaRule>();
        }
        rules.add(rule.self());
    }

    public void clearRules() {
        if (rules != null) {
            rules.clear();
        }
    }

    public void addProperty(String property, JSONSchema schema) {
        if (type != JSONType.OBJECT) {
            throw new UnsupportedOperationException("schema type not supported");
        }
        if (properties == null) {
            properties = new HashMap<Serializable, JSONSchema>();
        }
        properties.put(property, schema);
    }

    public void removeProperty(String property) {
        if (type != JSONType.OBJECT) {
            throw new UnsupportedOperationException("schema type not supported");
        }
        if (properties != null) {
            properties.remove(property);
        }
    }

    /**
     * schema校验
     *
     * @param json
     * @return
     */
    public JSONSchemaResult validate(String json) {
        try {
            return JSONNode.parse(json).validateSchema(this);
        } catch (Throwable throwable) {
            return JSONSchemaResult.fail(throwable.getMessage());
        }
    }

    /**
     * schema校验
     *
     * @param json
     * @return
     */
    public boolean validateSuccess(String json) {
        try {
            return JSONNode.parse(json).validateSchema(this, true).isSuccess();
        } catch (Throwable throwable) {
            return false;
        }
    }

    /**
     * schema校验
     *
     * @param node 节点
     * @return
     */
    public JSONSchemaResult validate(JSONNode node) {
        try {
            return node.validateSchema(this);
        } catch (Throwable throwable) {
            return JSONSchemaResult.fail(throwable.getMessage());
        }
    }

    /**
     * schema校验
     *
     * @param node 节点
     * @return
     */
    public boolean validateSuccess(JSONNode node) {
        try {
            return node.validateSchema(this, true).isSuccess();
        } catch (Throwable throwable) {
            return false;
        }
    }

    public boolean ifAnyOf() {
        return anyOf != null;
    }

    public boolean ifAllOf() {
        return allOf != null;
    }

    public boolean ifOneOf() {
        return oneOf != null;
    }

    public boolean ifRef() {
        return $ref != null;
    }

    public JSONSchema refSchema() {
        if ($ref == null/*|| !$ref.startsWith("#/definitions/")*/) return null;
        Map<Serializable, JSONSchema> __root_definitions = root.__definitions;
        if (__root_definitions == null) {
            return null;
        }
        return __root_definitions.get($ref);
    }

    public boolean ifTypeEnums() {
        return enums != null && enums.length > 0;
    }
}
