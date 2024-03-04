package io.github.wycst.wast.jdbc.executer;

public abstract class SubQueryCondition {

    private final SubOqlQuery subOqlQuery;

    SubQueryCondition(SubOqlQuery subOqlQuery) {
        this.subOqlQuery = subOqlQuery;
    }

    public SubOqlQuery getSubOqlQuery() {
        return subOqlQuery;
    }

    public abstract String getField();

    public abstract String getSymbol();
}
