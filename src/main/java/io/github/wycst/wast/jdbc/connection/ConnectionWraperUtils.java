package io.github.wycst.wast.jdbc.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ConnectionWraperUtils {

    // 1 如果开启了事务保证当前线程内同一个数据源使用同一个连接
    // 2 如果未开启事务，连接使用完就关闭（使用的数据源，连接本来就是可复用，不必考虑重复取连接的性能问题，否则处理数据清除太头疼）
    // 3 通过线程中绑定的连接提交事务或回滚
    private static final ThreadLocal<Map<Object, ConnectionWraper>> connectionWrapersOfThread = new ThreadLocal<Map<Object, ConnectionWraper>>();

    static void bindConnectionWraper(ConnectionWraper wraper, Object key) {

        Map<Object, ConnectionWraper> connectionWrapers = connectionWrapersOfThread.get();
        if (connectionWrapers == null) {
            connectionWrapers = new HashMap<Object, ConnectionWraper>();
            connectionWrapersOfThread.set(connectionWrapers);
        }
        connectionWrapers.put(key, wraper);
    }

    static ConnectionWraper currentConnectionWraper(Object key) {

        Map<Object, ConnectionWraper> connectionWrapers = connectionWrapersOfThread.get();

        if (connectionWrapers == null)
            return null;

        return connectionWrapers.get(key);
    }

    public static void commitAll() throws SQLException {
        Map<Object, ConnectionWraper> connectionWrapers = connectionWrapersOfThread.get();
        if (connectionWrapers == null)
            return;

        for (ConnectionWraper wraper : connectionWrapers.values()) {
            Connection conn = wraper.getConnection();
            if (conn != null && !conn.isClosed()) {
                conn.commit();
                conn.setAutoCommit(true);
                wraper.close();
            }
        }


    }

    public static void rollbackAll() throws SQLException {
        Map<Object, ConnectionWraper> connectionWrapers = connectionWrapersOfThread.get();
        if (connectionWrapers == null)
            return;

        for (ConnectionWraper wraper : connectionWrapers.values()) {
            Connection conn = wraper.getConnection();
            if (conn != null && !conn.isClosed()) {
                conn.rollback();
                conn.setAutoCommit(true);
                wraper.close();
            }
        }
    }

    public static void doReset() {
        Map<Object, ConnectionWraper> connectionWrapers = connectionWrapersOfThread.get();
        if (connectionWrapers == null)
            return;
        for (ConnectionWraper wraper : connectionWrapers.values()) {
            wraper.clear();
        }
        connectionWrapers.clear();
    }

    /**
     * 返回所有绑定的连接信息
     *
     * @return
     */
    public static Map<Object, ConnectionWraper> getCurrentConnectionWrapers() {
        return connectionWrapersOfThread.get();
    }

    /**
     * 清除现有所有绑定
     *
     * @param connectionWrapers
     */
    public static void reBindAll(Map<Object, ConnectionWraper> connectionWrapers) {
        Map<Object, ConnectionWraper> currentConnectionWrapers = connectionWrapersOfThread.get();
        if (currentConnectionWrapers == null) {
            currentConnectionWrapers = new HashMap<Object, ConnectionWraper>();
            connectionWrapersOfThread.set(currentConnectionWrapers);
        }
        currentConnectionWrapers.clear();

        if (connectionWrapers != null) {
            currentConnectionWrapers.putAll(connectionWrapers);
        }
    }
}
