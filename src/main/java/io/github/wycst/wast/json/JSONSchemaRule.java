package io.github.wycst.wast.json;

import io.github.wycst.wast.common.utils.StringUtils;

public final class JSONSchemaRule {
    // 构建内置正则校验器（如果同时配置正则校验器和表达式校验器优先使用正则校验器）
    private String regular;
    // 构建内置的表达式校验器
    private String expression;
    // 验证器(支持通过setter设置自定义验证器)
    private JSONSchemaRuleValidator validator;

    // 错误信息
    private String message;

    public JSONSchemaRuleValidator validator() {
        if (validator == null) {
            // 正则校验器优先
            if (!StringUtils.isEmpty(regular)) {
                return validator = new JSONSchemaRuleValidator.RegularImpl(regular);
            }
            if (!StringUtils.isEmpty(expression)) {
                return validator = new JSONSchemaRuleValidator.ExpressionImpl(expression);
            }
        }
        return validator;
    }

    public void setValidator(JSONSchemaRuleValidator validator) {
        this.validator = validator;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getRegular() {
        return regular;
    }

    public void setRegular(String regular) {
        this.regular = regular;
    }

    public JSONSchemaRule self() {
        return this;
    }
}
