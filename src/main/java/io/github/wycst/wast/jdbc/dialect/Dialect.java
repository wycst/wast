package io.github.wycst.wast.jdbc.dialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 数据库方言接口
 *
 * @author wangy
 */
public interface Dialect {

    public boolean supportsLimit();

    public String getLimitString(String sql, boolean hasOffset);

    public String getLimitString(String sql, long offset, int limit);

    /**
     * 设置参数
     */
    public void setParameter(PreparedStatement ps, int index, Object param) throws SQLException;

    PreparedStatement prepareStatement(Connection conn, String sql, int type, int resultSetConcurrency) throws SQLException;

}