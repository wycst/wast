package io.github.wycst.wast.json;

import io.github.wycst.wast.common.expression.Expression;

import java.util.regex.Pattern;

public abstract class JSONSchemaRuleValidator {

    public abstract boolean validate(JSONSchemaRule rule, JSONNode value);

    static class RegularImpl extends JSONSchemaRuleValidator {
        final Pattern pattern;

        public RegularImpl(String regular) {
            pattern = Pattern.compile(regular);
        }

        @Override
        public boolean validate(JSONSchemaRule rule, JSONNode value) {
            if (value.type == JSONNode.STRING) {
                return pattern.matcher(value.value.toString()).matches();
            }
            return false;
        }
    }

    static class ExpressionImpl extends JSONSchemaRuleValidator {
        final JSONNodePathFilter pathFilter;

        public ExpressionImpl(String elStr) {
            pathFilter = JSONNodePathFilter.expression(Expression.parse(elStr));
        }

        @Override
        public boolean validate(JSONSchemaRule rule, JSONNode value) {
            return pathFilter.doFilter(value);
        }
    }
}
