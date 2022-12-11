package io.github.wycst.wast.jdbc.entity;

import java.lang.reflect.Field;

/**
 * @Author wangyunchao
 * @Date 2021/8/5 14:35
 */
public class CascadeFetchMapping {

    // 属性域名
    private String fieldName;

    // 当前实体类中用作关联的field（@CascadeFetch中的field对应的域），一般为主键域或者唯一域
    private Field field;

    // 当前实体类中@CascadeFetch注解标记的属性域
    private Field cascadeFetchField;

    // 反射类型 fieldType = 1 单个实体； fieldType = 2 返回list
    private int fieldType;

    // 目标实体类中用作关联的field名称
    private String targetFieldName;

    // 目标实体类
    private Class<?> targetEntityClass;

    // 提取属性名称列表
    private String[] fetchFieldNames;

    private boolean cascade;
    private boolean fetch;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Field getCascadeFetchField() {
        return cascadeFetchField;
    }

    public void setCascadeFetchField(Field cascadeFetchField) {
        this.cascadeFetchField = cascadeFetchField;
    }

    public int getFieldType() {
        return fieldType;
    }

    public void setFieldType(int fieldType) {
        this.fieldType = fieldType;
    }

    public Class<?> getTargetEntityClass() {
        return targetEntityClass;
    }

    public void setTargetEntityClass(Class<?> targetEntityClass) {
        this.targetEntityClass = targetEntityClass;
    }

    public String[] getFetchFieldNames() {
        return fetchFieldNames;
    }

    public void setFetchFieldNames(String[] fetchFieldNames) {
        this.fetchFieldNames = fetchFieldNames;
    }

    public String getTargetFieldName() {
        return targetFieldName;
    }

    public void setTargetFieldName(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public boolean isCascade() {
        return cascade;
    }

    public void setCascade(boolean cascade) {
        this.cascade = cascade;
    }

    public boolean isFetch() {
        return fetch;
    }

    public void setFetch(boolean fetch) {
        this.fetch = fetch;
    }
}
