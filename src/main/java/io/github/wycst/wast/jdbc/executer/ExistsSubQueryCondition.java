package io.github.wycst.wast.jdbc.executer;

public class ExistsSubQueryCondition extends SubQueryCondition {

    ExistsSubQueryCondition(SubOqlQuery subOqlQuery) {
        super(subOqlQuery);
    }

    @Override
    public String getField() {
        return null;
    }

    @Override
    public String getSymbol() {
        return "EXISTS";
    }
}
