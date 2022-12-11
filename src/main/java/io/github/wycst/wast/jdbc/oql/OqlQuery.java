package io.github.wycst.wast.jdbc.oql;

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

    private OqlQuery() {
    }

    // 查询指定字段列表
    private final List<String> selectFields = new ArrayList<String>();
    // 条件
    private final List<FieldCondition> fieldConditions = new ArrayList<FieldCondition>();
    // 排序信息
    private final List<FieldOrder> orders = new ArrayList<FieldOrder>();
    // 禁止join
    private boolean disableJoin;

    public static OqlQuery create() {
        return new OqlQuery();
    }

    public OqlQuery order(String field) {
        order(field, FieldOrder.Order.ASC);
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
            addCondition(field, FieldCondition.Operator.Equal);
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
            addCondition(field, FieldCondition.Operator.Equal);
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
}
