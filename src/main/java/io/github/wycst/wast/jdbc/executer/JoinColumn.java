package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.jdbc.annotations.JoinField;

/**
 * @Author: wangy
 * @Date: 2021/7/25 21:35
 * @Description:
 */
public class JoinColumn {

    /**
     * 配置注解meta信息
     */
    private final JoinField joinField;

    /**
     * 属性名称（as）
     */
    private String fieldName;

    /**
     * 映射的字段属性
     */
    private FieldColumn joinFieldColumn;

    public JoinColumn(JoinField joinField) {
        this.joinField = joinField;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public FieldColumn getJoinFieldColumn() {
        return joinFieldColumn;
    }

    public void setJoinFieldColumn(FieldColumn joinFieldColumn) {
        this.joinFieldColumn = joinFieldColumn;
    }

    /**
     * 使用子查询替换join(left)
     *
     * @return
     */
    public boolean useSubQuery() {
        return joinField.useSubQuery();
    }

    public JoinField getJoinField() {
        return joinField;
    }
}
