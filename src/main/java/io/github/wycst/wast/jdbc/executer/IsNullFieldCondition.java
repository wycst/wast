package io.github.wycst.wast.jdbc.executer;

import java.io.Serializable;
import java.util.List;

public class IsNullFieldCondition extends FieldCondition {

    public IsNullFieldCondition(String field) {
        super(field, null);
    }

    @Override
    public String getOperator() {
        return "IS NULL";
    }

    @Override
    public Serializable getValue() {
        return 1;
    }

    @Override
    public void appendWhereValue(StringBuilder whereBuilder, List<Object> paramValues, Object conditionValue) {
    }

}
