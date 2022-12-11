package io.github.wycst.wast.jdbc.dialect;

import java.sql.*;
import java.util.Date;

/**
 * @Author: wangy
 * @Date: 2021/8/21 21:08
 * @Description:
 */
public abstract class DialectImpl implements Dialect {

    @Override
    public boolean supportsLimit() {
        return false;
    }

    @Override
    public abstract String getLimitString(String sql, boolean hasOffset);

    @Override
    public abstract String getLimitString(String sql, long offset, int limit);

    @Override
    public void setParameter(PreparedStatement ps, int index, Object param) throws SQLException {
        if (param instanceof String) {
            ps.setString(index, (String) param);
        } else if (param instanceof Timestamp) {
            ps.setTimestamp(index, (Timestamp) param);
        } else if (param instanceof Date) {
            if (param.getClass() == Date.class) {
                ps.setTimestamp(index, new Timestamp(((Date) param).getTime()));
            } else {
                ps.setDate(index, new java.sql.Date(((Date) param).getTime()));
            }
        } else if(param instanceof Enum) {
            ps.setString(index, param.toString());
        } else {
            ps.setObject(index, param);
        }
    }

    @Override
    public PreparedStatement prepareStatement(Connection conn, String sql, int type, int resultSetConcurrency) throws SQLException {
        if (type > -1 && resultSetConcurrency > -1) {
            return conn.prepareStatement(sql, type, resultSetConcurrency);
        } else if (type == Statement.RETURN_GENERATED_KEYS) {
            return conn.prepareStatement(sql, type);
        } else {
            return conn.prepareStatement(sql);
        }
    }
}
