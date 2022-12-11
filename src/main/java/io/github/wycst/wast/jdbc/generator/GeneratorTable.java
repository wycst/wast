package io.github.wycst.wast.jdbc.generator;

import java.util.List;
import java.util.Map;

/**
 * 实体和表结构（正向和逆向数据载体）
 *
 * @Author wangyunchao
 * @Date 2021/9/3 17:34
 */
public class GeneratorTable {

    /***
     * 表名
     */
    private String tableName;

    /**
     * 类名
     */
    private String entityName;

    /**
     * 大写开头模块名称，基本和entityName一致
     */
    private String upperCaseModuleName;

    /**
     * 小写字母开头的模块
     */
    private String lowerCaseModuleName;

    /**
     * 模块路径
     */
    private String modulePath;

    /**
     * 表结构代码
     */
    private String tableCode;

    /**
     * 生成的实体代码
     */
    private String entityCode;

    private String controllerCode;

    private String serviceInfCode;

    private String serviceImplCode;

    private String apiJsCode;

    private String vueCode;

    /**
     * 所有的字段映射
     */
    private Map<String, GeneratorTableColumn> tableColumns;

    /**
     * 查询域(表单查询条件)
     */
    private List<String> queryFields;

    /**
     * 表格显示字段
     */
    private List<String> displayFields;

    /**
     * 可修改编辑字段(新增/编辑)
     */
    private List<String> updateFields;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getUpperCaseModuleName() {
        return upperCaseModuleName;
    }

    public void setUpperCaseModuleName(String upperCaseModuleName) {
        this.upperCaseModuleName = upperCaseModuleName;
    }

    public String getLowerCaseModuleName() {
        return lowerCaseModuleName;
    }

    public void setLowerCaseModuleName(String lowerCaseModuleName) {
        this.lowerCaseModuleName = lowerCaseModuleName;
    }

    public String getModulePath() {
        return modulePath;
    }

    public void setModulePath(String modulePath) {
        this.modulePath = modulePath;
    }

    public String getTableCode() {
        return tableCode;
    }

    public void setTableCode(String tableCode) {
        this.tableCode = tableCode;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public Map<String, GeneratorTableColumn> getTableColumns() {
        return tableColumns;
    }

    public void setTableColumns(Map<String, GeneratorTableColumn> tableColumns) {
        this.tableColumns = tableColumns;
    }

    public List<String> getQueryFields() {
        return queryFields;
    }

    public void setQueryFields(List<String> queryFields) {
        this.queryFields = queryFields;
    }

    public List<String> getDisplayFields() {
        return displayFields;
    }

    public void setDisplayFields(List<String> displayFields) {
        this.displayFields = displayFields;
    }

    public List<String> getUpdateFields() {
        return updateFields;
    }

    public void setUpdateFields(List<String> updateFields) {
        this.updateFields = updateFields;
    }

    public String getControllerCode() {
        return controllerCode;
    }

    public void setControllerCode(String controllerCode) {
        this.controllerCode = controllerCode;
    }

    public String getServiceInfCode() {
        return serviceInfCode;
    }

    public void setServiceInfCode(String serviceInfCode) {
        this.serviceInfCode = serviceInfCode;
    }

    public String getServiceImplCode() {
        return serviceImplCode;
    }

    public void setServiceImplCode(String serviceImplCode) {
        this.serviceImplCode = serviceImplCode;
    }

    public String getApiJsCode() {
        return apiJsCode;
    }

    public void setApiJsCode(String apiJsCode) {
        this.apiJsCode = apiJsCode;
    }

    public String getVueCode() {
        return vueCode;
    }

    public void setVueCode(String vueCode) {
        this.vueCode = vueCode;
    }
}
