package io.github.wycst.wast.jdbc.dialect;

/**
 * @Author: wangy
 * @Date: 2023/2/28 22:14
 * @Description:
 */
public interface PageDialectAgent {

    /**
     * 将sql转化为分页sql
     *
     * @param sql
     * @param hasOffset
     * @return
     */
    public String getLimitSql(String sql, boolean hasOffset);

    /**
     * 将sql转化为分页sql
     *
     * @param sql
     * @param offset
     * @param limit
     * @return
     */
    public String getLimitSql(String sql, long offset, int limit);

}
