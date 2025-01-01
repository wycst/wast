package io.github.wycst.wast.jdbc.dialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * @Author: wangy
 * @Date: 2021/2/21 19:00
 * @Description:
 */
public class ClickHouseDialect extends MySqlDialect {

    @Override
    public PreparedStatement prepareStatement(Connection conn, String sql, int type, int resultSetConcurrency) throws SQLException {
        if (type > -1 && resultSetConcurrency > -1) {
            return conn.prepareStatement(sql, type, resultSetConcurrency);
        }
        return conn.prepareStatement(sql);
    }

    @Override
    public void setParameter(PreparedStatement ps, int index, Object param) throws SQLException {
        if (param instanceof String) {
            ps.setString(index, (String) param);
        } else if (param instanceof Timestamp) {
            ps.setTimestamp(index, (Timestamp) param);
        } else if (param instanceof Date) {
            ps.setDate(index, new java.sql.Date(((Date) param).getTime()));
        } else if (param instanceof Enum) {
            ps.setString(index, param.toString());
        } else {
            ps.setObject(index, param);
        }
    }
}
