package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.jdbc.exception.SqlExecuteException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @Author: wangy
 * @Date: 2021/2/16 10:34
 * @Description:
 */
public class OqlQuery {

    OqlQuery() {
    }

    // 查询指定字段列表
    private final List<String> selectFields = new ArrayList<String>();

    // 属性条件
    private final List<FieldCondition> fieldConditions = new ArrayList<FieldCondition>();
    // 子查询条件
    private final List<SubQueryCondition> subQueryConditions = new ArrayList<SubQueryCondition>();
    // 排序信息
    private final List<FieldOrder> orders = new ArrayList<FieldOrder>();
    // 禁止join
    private boolean disableJoin;

    // 暂时支持 AND/OR
    // private String logicType = "AND";
    private String logicTypeSpace = " AND ";

    // private String tableAlias = "t";

    public static OqlQuery create() {
        return new OqlQuery();
    }

    public void reset() {
        clear();
        and().fetchJoin();
    }

    public OqlQuery and() {
        // this.logicType = "AND";
        this.logicTypeSpace = " AND ";
        return this;
    }

    public OqlQuery or() {
        // this.logicType = "OR";
        this.logicTypeSpace = " OR ";
        return this;
    }

    public String getLogicTypeSpace() {
        return logicTypeSpace;
    }

    public OqlQuery order(String field) {
        order(field, FieldOrder.Order.ASC);
        return this;
    }

    public OqlQuery orderAsc(String field) {
        order(field, FieldOrder.Order.ASC);
        return this;
    }

    public OqlQuery orderDesc(String field) {
        order(field, FieldOrder.Order.DESC);
        return this;
    }

    public OqlQuery order(String field, FieldOrder.Order by) {
        orders.add(new FieldOrder(field, by));
        return this;
    }

    public void clearSelectFields() {
        selectFields.clear();
    }

    public void clearConditions() {
        fieldConditions.clear();
        subQueryConditions.clear();
    }

    private void clearOrders() {
        orders.clear();
    }

    public void clear() {
        clearSelectFields();
        clearConditions();
        clearOrders();
    }

    /***
     * 添加查询字段
     * @param fields
     */
    public OqlQuery addSelectFields(String... fields) {
        for (String field : fields) {
            if (!selectFields.contains(field)) {
                selectFields.add(field);
            }
        }
        return this;
    }

    /**
     * 添加查询字段(可变数组)： value设置指定值或者自定义的占位符
     *
     * @param fields
     */
    public OqlQuery addConditions(String...fields) {
        for (String field: fields) {
            addCondition(field, FieldCondition.Operator.EQ);
        }
        return this;
    }
    
    /**
     * 添加查询字段（列表集合）： value设置指定值或者自定义的占位符
     *
     * @param fields
     */
    public OqlQuery addConditions(Collection<String> fields) {
        for (String field: fields) {
            addCondition(field, FieldCondition.Operator.EQ);
        }
        return this;
    }

    /**
     * 添加查询字段： value设置指定值或者自定义的占位符
     *
     * @param field
     * @param ops
     * @param value
     */
    public OqlQuery addCondition(String field, FieldCondition.Operator ops, Serializable value) {
        fieldConditions.add(new FieldCondition(field, ops, value));
        return this;
    }

    /**
     * 添加查询条件： value使用#{field}占位符
     *
     * @param field
     * @param ops
     * @return
     */
    public OqlQuery addCondition(String field, FieldCondition.Operator ops) {
        fieldConditions.add(new FieldCondition(field, ops));
        return this;
    }

    /**
     * 添加查询条件： value使用#{field}占位符
     *
     * @param field
     * @param left
     * @param right
     * @return
     */
    public OqlQuery between(String field, Serializable left, Serializable right) {
        fieldConditions.add(new BetweenFieldCondition(field, left, right));
        return this;
    }

    /**
     * 添加查询条件： value使用#{field}占位符
     *
     * @param field
     * @param betweenValues
     * @return
     */
    public OqlQuery between(String field, List<? extends Serializable> betweenValues) {
        if(betweenValues == null || betweenValues.size() < 2) {
            throw new SqlExecuteException("BETWEEN syntax must provide 2 values");
        }
        fieldConditions.add(new BetweenFieldCondition(field, betweenValues.get(0), betweenValues.get(1)));
        return this;
    }

    /**
     * 添加查询条件： value使用#{field}占位符
     *
     * @param field
     * @param betweenValues
     * @return
     */
    public OqlQuery between(String field, Serializable... betweenValues) {
        if(betweenValues.length < 2) {
            throw new SqlExecuteException("BETWEEN syntax must provide 2 values");
        }
        fieldConditions.add(new BetweenFieldCondition(field, betweenValues[0], betweenValues[1]));
        return this;
    }

    public OqlQuery isNull(String field) {
        fieldConditions.add(new IsNullFieldCondition(field));
        return this;
    }

    public OqlQuery eq(String field, Serializable value) {
        fieldConditions.add(new FieldCondition(field, FieldCondition.Operator.EQ, value));
        return this;
    }

    public OqlQuery gt(String field, Serializable value) {
        fieldConditions.add(new FieldCondition(field, FieldCondition.Operator.GT, value));
        return this;
    }

    public OqlQuery lt(String field, Serializable value) {
        fieldConditions.add(new FieldCondition(field, FieldCondition.Operator.LT, value));
        return this;
    }

    public OqlQuery ge(String field, Serializable value) {
        fieldConditions.add(new FieldCondition(field, FieldCondition.Operator.GE, value));
        return this;
    }

    public OqlQuery le(String field, Serializable value) {
        fieldConditions.add(new FieldCondition(field, FieldCondition.Operator.LE, value));
        return this;
    }

    public OqlQuery in(String field, List<? extends Serializable> values) {
        fieldConditions.add(new InFieldCondition(field, values));
        return this;
    }

    public OqlQuery in(String field, SubOqlQuery subOqlQuery) {
        subQueryConditions.add(new InSubQueryCondition(field, subOqlQuery));
        return this;
    }

    public OqlQuery exists(SubOqlQuery subOqlQuery) {
        subQueryConditions.add(new ExistsSubQueryCondition(subOqlQuery));
        return this;
    }

    /**
     * 清除指定域的查询条件
     *
     * @param fields
     * @return
     */
    public OqlQuery clearConditions(String...fields) {
    	FieldCondition[] conditions = fieldConditions.toArray(new FieldCondition[fieldConditions.size()]);
    	List<String> fieldList = Arrays.asList(fields);
        for(FieldCondition fieldCondition : conditions) {
        	if(fieldList.contains(fieldCondition.getField())) {
        		fieldConditions.remove(fieldCondition);
        	}
        }
        return this;
    }

    public boolean isDisableJoin() {
        return disableJoin;
    }

    public void setDisableJoin(boolean disableJoin) {
        this.disableJoin = disableJoin;
    }

    public OqlQuery disableJoin() {
        this.disableJoin = true;
        return this;
    }

    public OqlQuery fetchJoin() {
        this.disableJoin = false;
        return this;
    }

    //    /**
//     * 添加查询条件： value使用#{field}占位符
//     *
//     * @param field
//     * @param ops
//     * @return
//     */
//    public OqlQuery addCondition(String field, FieldCondition.Operator ops, ) {
//        fieldConditions.add(new FieldCondition(field, ops));
//        return this;
//    }

    public List<String> getSelectFields() {
        return selectFields;
    }

    public List<FieldCondition> getConditions() {
        return fieldConditions;
    }

    public List<FieldOrder> getOrders() {
        return orders;
    }

    public List<SubQueryCondition> getSubQueryConditions() {
        return subQueryConditions;
    }
}
