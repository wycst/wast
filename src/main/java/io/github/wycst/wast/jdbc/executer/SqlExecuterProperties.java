package io.github.wycst.wast.jdbc.executer;

/**
 * sql查询配置属性
 *
 * @Author: wangy
 * @Date: 2021/8/6 23:19
 * @Description:
 */
public class SqlExecuterProperties {

    /**
     * 查询超时时间
     */
    private Integer queryTimeout = 0;

    /**
     * 显示sql
     */
    private Boolean showSql = true;

    /**
     * format sql
     */
    private Boolean formatSql = false;

    /**
     * 显示sql参数
     */
    private Boolean showParameters = false;

    /**
     * 开发模式
     *
     * <p>- 显示sql
     * <p>- 显示输入参数
     * <p>- 打印耗时
     */
    private Boolean development = false;

    /**
     * 批处理大小
     */
    private Integer batchSize = 5000;

    public Integer getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(Integer queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public Boolean getShowSql() {
        return showSql;
    }

    public void setShowSql(Boolean showSql) {
        this.showSql = showSql;
    }

    public Boolean getFormatSql() {
        return formatSql;
    }

    public void setFormatSql(Boolean formatSql) {
        this.formatSql = formatSql;
    }

    public Boolean getShowParameters() {
        return showParameters;
    }

    public void setShowParameters(Boolean showParameters) {
        this.showParameters = showParameters;
    }

    public Boolean getDevelopment() {
        return development;
    }

    public void setDevelopment(Boolean development) {
        this.development = development;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }
}
