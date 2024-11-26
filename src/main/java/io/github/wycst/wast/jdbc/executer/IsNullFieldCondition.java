package io.github.wycst.wast.jdbc.executer;

import java.io.Serializable;
import java.util.List;

public class IsNullFieldCondition extends FieldCondition {

    boolean notNull;

    public IsNullFieldCondition(String field) {
        super(field, null);
    }

    public IsNullFieldCondition(String field, boolean notNull) {
        super(field, null);
        this.notNull = true;
    }

    @Override
    protected String valueSpace() {
        return "";
    }

    @Override
    public String getOperator() {
        return notNull ? "IS NOT NULL" : "IS NULL";
    }

    @Override
    public Serializable getValue() {
        return 1;
    }

    @Override
    public void appendWhereValue(StringBuilder whereBuilder, List<Object> paramValues, Object conditionValue) {
    }

}
