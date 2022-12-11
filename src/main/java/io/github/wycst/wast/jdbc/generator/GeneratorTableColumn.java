package io.github.wycst.wast.jdbc.generator;

/**
 * 表字段信息
 *
 * @Author wangyunchao
 * @Date 2021/9/3 18:26
 */
public class GeneratorTableColumn {

    public final static int JAVA_TYPE_STRING = 1;
    public final static int JAVA_TYPE_NUMBER = 2;
    public final static int JAVA_TYPE_DATE = 3;
    public final static int JAVA_TYPE_BINARY = 4;

    /**
     * 字段配置项
     */
    private GeneratorColumnOption columnOption;

    /**
     * 表字段名称
     */
    private String columnName;

    /**
     * 字段类型
     */
    private int columnType;

    /**
     * 字段定义
     */
    private String columnDefinition;

    /**
     * 是否主键
     */
    private boolean primary;

    /**
     * java属性域名
     */
    private String javaField;

    /**
     * java类型
     * 0未知 / 1字符串 / 2数字 / 3日期
     */
    private int javaType;

    /**
     * 类型名称
     */
    private String javaTypeName;

    /**
     * 是否字典数据（下拉框呈现）
     */
    private boolean dict;

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public int getColumnType() {
        return columnType;
    }

    public void setColumnType(int columnType) {
        this.columnType = columnType;
    }

    public String getColumnDefinition() {
        return columnDefinition;
    }

    public void setColumnDefinition(String columnDefinition) {
        this.columnDefinition = columnDefinition;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public String getJavaField() {
        return javaField;
    }

    public void setJavaField(String javaField) {
        this.javaField = javaField;
    }

    public int getJavaType() {
        return javaType;
    }

    public void setJavaType(int javaType) {
        this.javaType = javaType;
    }

    public boolean isDict() {
        return dict;
    }

    public void setDict(boolean dict) {
        this.dict = dict;
    }

    public String getJavaTypeName() {
        return javaTypeName;
    }

    public void setJavaTypeName(String javaTypeName) {
        this.javaTypeName = javaTypeName;
    }

    public GeneratorColumnOption getColumnOption() {
        return columnOption;
    }

    public void setColumnOption(GeneratorColumnOption columnOption) {
        this.columnOption = columnOption;
    }
}
