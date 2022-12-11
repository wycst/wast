package io.github.wycst.wast.jdbc.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2021/7/25 22:22
 * @Description:
 */
public class JoinEntityMapping {

    /**
     * 表名
     */
    private String tableName;

    /***
     * 条件关联on属性映射列表
     *
     */
    private Map<String, String> joinOnFieldKeys = new HashMap<String, String>();

    /***
     * 条件关联on字段映射列表
     *
     */
    private Map<String, String> joinOnColumnKeys = new HashMap<String, String>();

    /***
     * queryFields
     */
    private List<JoinColumn> joinColumns = new ArrayList<JoinColumn>();

    public Map<String, String> getJoinOnFieldKeys() {
        return joinOnFieldKeys;
    }

    public void setJoinOnFieldKeys(Map<String, String> joinOnFieldKeys) {
        this.joinOnFieldKeys = joinOnFieldKeys;
    }

    public List<JoinColumn> getJoinColumns() {
        return joinColumns;
    }

    public void setJoinColumns(List<JoinColumn> joinColumns) {
        this.joinColumns = joinColumns;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Map<String, String> getJoinOnColumnKeys() {
        return joinOnColumnKeys;
    }

    public void setJoinOnColumnKeys(Map<String, String> joinOnColumnKeys) {
        this.joinOnColumnKeys = joinOnColumnKeys;
    }
}
