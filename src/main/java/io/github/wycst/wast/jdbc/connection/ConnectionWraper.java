package io.github.wycst.wast.jdbc.connection;

import io.github.wycst.wast.jdbc.transaction.TransactionUtils;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionWraper {

    private Connection physicalConn;

    private long currentInfluencingRows;

    private boolean transaction;

    public ConnectionWraper(Connection physicalConn) {
        this.physicalConn = physicalConn;
    }

    public void setConnection(Connection physicalConn) {
        this.physicalConn = physicalConn;
    }

    public Connection getConnection() {
        return physicalConn;
    }

    public long getCurrentInfluencingRows() {
        return currentInfluencingRows;
    }

    public void setCurrentInfluencingRows(long currentInfluencingRows) {
        this.currentInfluencingRows = currentInfluencingRows;
    }

    public boolean isTransaction() {
        return transaction;
    }

    public void setTransaction(boolean transaction) {
        this.transaction = transaction;
    }

    public void close() {
        try {
            if (physicalConn != null && !physicalConn.isClosed()) {
                physicalConn.setAutoCommit(true);
                physicalConn.close();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        physicalConn = null;
    }

    /**
     * 所有的数据库操作完成后都会回调此方法
     */
    public void handlerCloseAction() {
        // 如果当前不在事务下，关闭连接
        if (!TransactionUtils.isTransactionActive()) {
            close();
        }
    }

    public void addInfluencingRows(int effect) {
        if (TransactionUtils.isTransactionActive()) {
            this.currentInfluencingRows += effect;
        }
    }

    public void clear() {
        currentInfluencingRows = 0;
        physicalConn = null;
        transaction = false;
    }

}
