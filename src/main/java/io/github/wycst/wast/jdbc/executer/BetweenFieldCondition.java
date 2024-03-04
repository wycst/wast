package io.github.wycst.wast.jdbc.executer;

import java.io.Serializable;
import java.util.List;

class BetweenFieldCondition extends FieldCondition {

    final Serializable left;
    final Serializable right;

    public BetweenFieldCondition(String field, Serializable left, Serializable right) {
        super(field, null);
        left.getClass();
        right.getClass();
        this.left = left;
        this.right = right;
    }

    @Override
    public String getOperator() {
        return "BETWEEN";
    }

    @Override
    public void appendWhereValue(StringBuilder whereBuilder, List<Object> paramValues, Object conditionValue) {
        whereBuilder.append("? AND ?");
        paramValues.add(left);
        paramValues.add(right);
    }

    @Override
    public Serializable getValue() {
        return 1;
    }
}



