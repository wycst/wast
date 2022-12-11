package io.github.wycst.wast.jdbc.oql;

import java.io.Serializable;

/**
 * @Author: wangy
 * @Date: 2021/2/16 11:44
 * @Description:
 */
public class FieldCondition {

    private final String field;
    private final Operator operator;
    private String value;
    private String logicType = "and";

    public FieldCondition(String field, Operator ops, Serializable value) {
        this.field = field;
        this.operator = ops == null ? Operator.Equal : ops;
        if (value == null) {
            setValueExpr();
        } else {
            if (value instanceof Number) {
                this.value = String.valueOf(value);
            } else if (value instanceof String) {
                if (((String) value).matches("#\\{.*?\\}")) {
                    this.value = ((String) value).trim();
                } else {
                    this.value = "'" + ((String) value).trim() + "'";
                }
            }
        }
    }

    private void setValueExpr() {
        if(operator == Operator.Like) {
            this.value = "'%${" + field + "}%'";
        } else if(operator == Operator.LeftLike) {
            this.value = "'%${" + field + "}'";
        } else if(operator == Operator.RightLike) {
            this.value = "'${" + field + "}%'";
        } else {
            this.value = "#{" + field + "}";
        }
    }

    public FieldCondition(String field, Operator ops) {
        this.field = field;
        this.operator = ops == null ? Operator.Equal : ops;
        setValueExpr();
    }

    public String getField() {
        return field;
    }

    public String getOperator() {
        switch (operator) {
            case Gt:
                return ">";
            case Lt:
                return "<";
            case Equal:
                return "=";
            case NotEqual:
                return "!=";
            case GtOrEqual:
                return ">=";
            case LtOrEqual:
                return "<=";
            case Like:
                return "like";
            case LeftLike:
                return "like";
            case RightLike:
                return "like";
        }
        return "=";
    }

    public String getLogicType() {
        return logicType;
    }

    public void setLogicType(String logicType) {
        this.logicType = logicType;
    }

    public String getValue() {
        return value;
    }

    public enum Operator {
        Like,LeftLike,RightLike, Gt, Lt, Equal, GtOrEqual, LtOrEqual, NotEqual
    }
}
