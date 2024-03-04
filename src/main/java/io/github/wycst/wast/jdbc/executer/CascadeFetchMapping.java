package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.jdbc.annotations.CascadeFetch;

import java.lang.reflect.Field;

/**
 * @Author wangyunchao
 * @Date 2021/8/5 14:35
 */
public class CascadeFetchMapping {

    private final CascadeFetch cascadeFetch;
    // 属性域名
    private final String fieldName;

//    // 当前实体类中用作关联的field（@CascadeFetch中的field对应的域），一般为主键域或者唯一域
//    private Field field;

    // 当前实体类中@CascadeFetch注解标记的属性域
    private final Field cascadeFetchField;

    // 反射类型 fieldType = 1 单个实体； fieldType = 2 返回list
    private final int fieldType;

    // 目标实体类中用作关联的field名称
    private final String targetFieldName;

    // 目标实体类
    private final Class<?> targetEntityClass;

    private final boolean cascade;
    private final boolean fetch;
    private final SetterInfo setterInfo;

    // 提取属性名称列表
    private String[] fetchFieldNames;

    CascadeFetchMapping(Class<?> targetEntityClass, Field cascadeFetchField, int fieldTypeValue, CascadeFetch cascadeFetch) {
        this.targetEntityClass = targetEntityClass;
        this.cascadeFetchField = cascadeFetchField;
        this.fieldType = fieldTypeValue;
        this.cascadeFetch = cascadeFetch;
        this.fieldName = cascadeFetch.field();
        this.targetFieldName = cascadeFetch.targetField();
        this.cascade = cascadeFetch.cascade();
        this.fetch = cascadeFetch.fetch();
        this.setterInfo = SetterInfo.fromField(cascadeFetchField);
    }

    public String getFieldName() {
        return fieldName;
    }

//    public Field getField() {
//        return field;
//    }
//
//    public void setField(Field field) {
//        this.field = field;
//    }

    public Field getCascadeFetchField() {
        return cascadeFetchField;
    }

    public int getFieldType() {
        return fieldType;
    }

    public Class<?> getTargetEntityClass() {
        return targetEntityClass;
    }

    public String getTargetFieldName() {
        return targetFieldName;
    }

    public boolean isCascade() {
        return cascade;
    }

    public boolean isFetch() {
        return fetch;
    }

    public String[] getFetchFieldNames() {
        return fetchFieldNames;
    }

    public void setFetchFieldNames(String[] fetchFieldNames) {
        this.fetchFieldNames = fetchFieldNames;
    }

    void setFetchFieldValue(Object target, Object fetchFieldVal) {
        setterInfo.invoke(target, fetchFieldVal);
    }
}
