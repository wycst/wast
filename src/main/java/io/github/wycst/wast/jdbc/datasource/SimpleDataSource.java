package io.github.wycst.wast.jdbc.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据库数据源（仅仅获取连接）
 *
 * @Author wangyunchao
 * @Date 2021/9/3 17:40
 */
public class SimpleDataSource extends AbstractDataSource {

    private String driverClass;
    private String jdbcUrl;
    private String username;
    private String password;
    private boolean driverCheckState;

    public SimpleDataSource() {
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
        try {
            Class.forName(driverClass);
            driverCheckState = true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            driverCheckState = false;
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(username, password);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        checkDriverClassState();
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private void checkDriverClassState() throws SQLException {
        if (!driverCheckState) {
            throw new SQLException("Driver class not specified or not available ");
        }
    }
}
