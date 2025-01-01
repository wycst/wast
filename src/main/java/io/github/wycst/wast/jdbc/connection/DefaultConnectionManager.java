package io.github.wycst.wast.jdbc.connection;

import io.github.wycst.wast.jdbc.transaction.TransactionUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 默认数据库管理实现
 *
 * @author wangyunchao
 */
public class DefaultConnectionManager extends AbstractConnectionManager implements ConnectionManager {

    public DefaultConnectionManager(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public ConnectionWraper getConnectionWraper() {

        Connection physicalConn = null;
        DataSource dataSource = getDataSource();
        dataSource.getClass();

        // 如果未开启事务，直接返回临时ConnectionWraper不做绑定操作
        boolean isTransactionActive = TransactionUtils.isTransactionActive();
        if (!isTransactionActive) {
            try {
                physicalConn = dataSource.getConnection();
                return new ConnectionWraper(physicalConn);
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(" create connection error for ...", e);
            }

        }

        // 事务中
        ConnectionWraper wraper = currentConnectionWraper();

        try {
            if (wraper != null && (physicalConn = wraper.getConnection()) != null) {
                if (!physicalConn.isClosed()) {
                    return wraper;
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // physicalConn is null
        try {
            // physical conn
            physicalConn = dataSource.getConnection();
            // 取消自动提交（开启事务）
            physicalConn.setAutoCommit(false);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        physicalConn.getClass();

        if (wraper == null) {
            wraper = new ConnectionWraper(physicalConn);
            wraper.setTransaction(true);
        } else {
            wraper.setConnection(physicalConn);
        }

        // bind
        bindConnectionWraper(wraper);

        return wraper;
    }

    @Override
    public void closeConnection(ConnectionWraper wraper) {
        wraper.handlerCloseAction();
    }

    @Override
    public void beginTransaction() {
        // 设置事务激活状态
        TransactionUtils.setTransactionActive(true);
        super.beginTransaction();
    }

    @Override
    public void endTransaction() {
        // 重置事务活动标记
        TransactionUtils.setTransactionActive(false);
        super.endTransaction();
    }

    @Override
    public void clear() {

        ConnectionWraper wraper = currentConnectionWraper();
        if (wraper != null) {
            // rollbackTransaction
            if (wraper.isTransaction() && wraper.getCurrentInfluencingRows() > 0) {
                // rollback and close conn
                rollbackTransaction();
            }
            wraper.clear();
        }

        super.clear();
    }


}
