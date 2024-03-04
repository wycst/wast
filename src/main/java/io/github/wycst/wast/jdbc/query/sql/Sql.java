package io.github.wycst.wast.jdbc.query.sql;

import java.util.List;

/**
 * 解析根据数据和模版sql得到的sql结构对象
 *
 * @author wangyunchao
 */
public class Sql {

    private String originalSql;

    private String formalSql;

    private String totalSql;

    private List<String> paramNames;

    private Object[] paramValues;

    private List<Object[]> paramValuesList;

    // Variable replace
    private boolean replaced;

    public String getOriginalSql() {
        return originalSql;
    }

    public void setOriginalSql(String originalSql) {
        this.originalSql = originalSql;
    }

    public String getFormalSql() {
        return formalSql;
    }

    public void setFormalSql(String formalSql) {
        this.formalSql = formalSql;
    }

    public List<String> getParamNames() {
        return paramNames;
    }

    public void setParamNames(List<String> paramNames) {
        this.paramNames = paramNames;
    }

    public Object[] getParamValues() {
        return paramValues;
    }

    public void setParamValues(Object[] paramValues) {
        this.paramValues = paramValues;
    }

    public boolean isReplaced() {
        return replaced;
    }

    public void setReplaced(boolean replaced) {
        this.replaced = replaced;
    }

    public List<Object[]> getParamValuesList() {
        return paramValuesList;
    }

    public void setParamValuesList(List<Object[]> paramValuesList) {
        this.paramValuesList = paramValuesList;
    }

    public String getTotalSql() {
        return totalSql;
    }

    public void setTotalSql(String totalSql) {
        this.totalSql = totalSql;
    }
}
