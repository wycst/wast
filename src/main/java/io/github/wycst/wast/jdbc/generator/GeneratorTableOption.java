package io.github.wycst.wast.jdbc.generator;

import java.util.List;

/**
 * @Author: wangy
 * @Date: 2021/9/2 0:14
 * @Description:
 */
public class GeneratorTableOption {

    public final static int PRIMARY_POLICY_IDENTITY = 1;
    public final static int PRIMARY_POLICY_UUID = 2;
    public final static int PRIMARY_POLICY_ALG = 3;
    public final static int PRIMARY_POLICY_SEQUENCE = 4;
    public final static int PRIMARY_POLICY_NONE = 5;

    // 表名
    private String tableName;
    // 实体名称，如果没有指定使用tableName根据驼峰规则自动生成
    private String entityName;
    // 主键策略（自增/UUID/分布式算法/无）
    private int primaryPolicy;
    // 删除实体的部分前缀作为模块的名称
    private String deletePrefixAsModule;
    // 构建字段选项
    private List<GeneratorColumnOption> columnOptions;

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

    public int getPrimaryPolicy() {
        return primaryPolicy;
    }

    public void setPrimaryPolicy(int primaryPolicy) {
        this.primaryPolicy = primaryPolicy;
    }

    public String getDeletePrefixAsModule() {
        return deletePrefixAsModule;
    }

    public void setDeletePrefixAsModule(String deletePrefixAsModule) {
        this.deletePrefixAsModule = deletePrefixAsModule;
    }

    public List<GeneratorColumnOption> getColumnOptions() {
        return columnOptions;
    }

    public void setColumnOptions(List<GeneratorColumnOption> columnOptions) {
        this.columnOptions = columnOptions;
    }
}
