package io.github.wycst.wast.jdbc.generator;

/**
 * @Author: wangy
 * @Date: 2021/9/2 0:16
 * @Description:
 */
public class GeneratorColumnOption {

    /**
     * 表字段名称
     */
    private String columnName;

    /**
     * java属性域名
     */
    private String javaField;

    /**
     * 是否作为查询条件字段
     */
    private boolean query;

    /**
     * 是否作为显示字段
     */
    private boolean display;

    /**
     * 是否支持更新（插入和更新）
     */
    private boolean update;

    /**
     * 字段是否来源字典数据
     */
    private boolean dict;

    /**
     * 字段名称描述-生成表头、查询条件等名称展示
     */
    private String columnDesc;

    public boolean isQuery() {
        return query;
    }

    public void setQuery(boolean query) {
        this.query = query;
    }

    public boolean isDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    public boolean isDict() {
        return dict;
    }

    public void setDict(boolean dict) {
        this.dict = dict;
    }

    public String getColumnDesc() {
        return columnDesc;
    }

    public void setColumnDesc(String columnDesc) {
        this.columnDesc = columnDesc;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getJavaField() {
        return javaField;
    }

    public void setJavaField(String javaField) {
        this.javaField = javaField;
    }
}
