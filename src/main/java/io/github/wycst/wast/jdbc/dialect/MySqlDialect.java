package io.github.wycst.wast.jdbc.dialect;

import java.sql.*;

/**
 * mysql 语言
 *
 * @author wangyunchao
 */
public class MySqlDialect extends DialectImpl implements Dialect {

    protected static final String SQL_END_SYMBOL = ";";

    public boolean supportsLimit() {
        return true;
    }

    @Override
    public boolean supportsBackquote() {
        return true;
    }

    public String getLimitString(String sql, boolean hasOffset) {

        sql = sql.trim();
        // 如果存在结束符号，重构后还原结束符
        boolean hasSqlEndSymbol = sql.endsWith(SQL_END_SYMBOL);

        StringBuffer buffer = new StringBuffer(sql.length() + 10);
        buffer.append(sql);

        String limit = hasOffset ? " limit ?,? " : " limit ? ";
        if (hasSqlEndSymbol) {
            buffer.insert(buffer.length() - 1, limit);
        } else {
            buffer.append(limit);
        }

        return buffer.toString();
    }

    public String getLimitString(String sql, long offset, int limit) {

        sql = sql.trim();
        // 如果存在结束符号，重构后还原结束符
        boolean hasSqlEndSymbol = sql.endsWith(SQL_END_SYMBOL);

        StringBuffer buffer = new StringBuffer(sql.length() + 10);
        buffer.append(sql);
        if (hasSqlEndSymbol) {
            buffer.delete(buffer.length() - SQL_END_SYMBOL.length(), buffer.length());
        }
        if (offset > 0) {
            buffer.append(" limit ").append(offset).append(',').append(limit);
        } else {
            buffer.append(" limit ").append(limit);
        }
        if (hasSqlEndSymbol) {
            buffer.append(SQL_END_SYMBOL);
        }
        return buffer.toString();
    }

    @Override
    public PreparedStatement prepareStatement(Connection conn, String sql, int type, int resultSetConcurrency) throws SQLException {
        if (type > -1 && resultSetConcurrency > -1) {
            PreparedStatement statement = conn.prepareStatement(sql, type, resultSetConcurrency);
            if (type == ResultSet.TYPE_FORWARD_ONLY && resultSetConcurrency == ResultSet.CONCUR_READ_ONLY) {
                statement.setFetchSize(Integer.MIN_VALUE);
            }
            return statement;
        } else if (type == Statement.RETURN_GENERATED_KEYS) {
            return conn.prepareStatement(sql, type);
        } else {
            return conn.prepareStatement(sql);
        }
    }

}
