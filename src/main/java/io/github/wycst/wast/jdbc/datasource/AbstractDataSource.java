package io.github.wycst.wast.jdbc.datasource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * 自定义数据源（非连接池）
 * 暂时不可用
 *
 * @author wangy
 */
public abstract class AbstractDataSource implements DataSource {

    public AbstractDataSource() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public abstract Connection getConnection() throws SQLException;

    @Override
    public abstract Connection getConnection(String username, String password) throws SQLException;

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
