package io.github.wycst.wast.jdbc.entity;

/**
 *
 * @Author: wangy
 * @Date: 2021/7/25 21:35
 * @Description:
 */
public class JoinColumn {

    /**
     *  属性名称（as）
     */
    private String fieldName;

    /**
     * 映射的字段属性
     */
    private FieldColumn joinFieldColumn;

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
}
