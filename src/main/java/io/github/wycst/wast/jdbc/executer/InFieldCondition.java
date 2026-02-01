package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.jdbc.exception.SqlExecuteException;

import java.io.Serializable;
import java.util.List;

public class InFieldCondition extends FieldCondition {

    private final List<? extends Serializable> values;
    private String left;

    public InFieldCondition(String field, List<? extends Serializable> values) {
        super(field, null);
        field.getClass();
        if (values == null || values.isEmpty()) {
            throw new SqlExecuteException("IN(...) syntax must not provide empty values");
        }
        this.values = values;
    }

    @Override
    public String getOperator() {
        return "IN";
    }

    @Override
    public void appendWhereValue(StringBuilder whereBuilder, List<Object> paramValues, Object conditionValue) {
        whereBuilder.append("(");
        int deleteDotIndex = -1;
        for (Serializable value : values) {
            paramValues.add(value);
            whereBuilder.append("?,");
            deleteDotIndex = whereBuilder.length() - 1;
        }
        if (deleteDotIndex > -1) {
            whereBuilder.deleteCharAt(deleteDotIndex);
        }
        whereBuilder.append(")");
    }

    @Override
    public Serializable getValue() {
        return 1;
    }
}
