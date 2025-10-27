package io.github.wycst.wast.json;

/**
 * 使用枚举定义JSON得6种数据类型
 *
 * @Author: wangy
 */
public enum JSONType {
    OBJECT(1, "object"),
    ARRAY(2, "array"),
    STRING(3, "string"),
    NUMBER(4, "number"),
    NUMBER_INTEGER(4, "integer"),
    BOOLEAN(5, "boolean"),
    NULL(6, "null");
    final int value;
    final String type;

    JSONType(int value, String type) {
        this.value = value;
        this.type = type;
    }

    public int getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public static JSONType typeOf(String type) {
        for (JSONType value : values()) {
            if (value.type.equalsIgnoreCase(type)) {
                return value;
            }
        }
        return null;
    }
}
