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

        StringBuilder builder = new StringBuilder(sql.length() + 10);
        builder.append(sql);

        String limit = hasOffset ? " limit ?,? " : " limit ? ";
        if (hasSqlEndSymbol) {
            builder.insert(builder.length() - 1, limit);
        } else {
            builder.append(limit);
        }

        return builder.toString();
    }

    public String getLimitString(String sql, long offset, int limit) {

        sql = sql.trim();
        // 如果存在结束符号，重构后还原结束符
        boolean hasSqlEndSymbol = sql.endsWith(SQL_END_SYMBOL);

        StringBuilder builder = new StringBuilder(sql.length() + 10);
        builder.append(sql);
        if (hasSqlEndSymbol) {
            builder.delete(builder.length() - SQL_END_SYMBOL.length(), builder.length());
        }
        if (offset > 0) {
            builder.append(" limit ").append(offset).append(',').append(limit);
        } else {
            builder.append(" limit ").append(limit);
        }
        if (hasSqlEndSymbol) {
            builder.append(SQL_END_SYMBOL);
        }
        return builder.toString();
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
