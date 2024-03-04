package io.github.wycst.wast.jdbc.executer;

public class InSubQueryCondition extends SubQueryCondition {

    private final String field;

    InSubQueryCondition(String field, SubOqlQuery subOqlQuery) {
        super(subOqlQuery);
        this.field = field;
    }

    @Override
    public String getField() {
        return field;
    }

    @Override
    public String getSymbol() {
        return "IN";
    }
}
