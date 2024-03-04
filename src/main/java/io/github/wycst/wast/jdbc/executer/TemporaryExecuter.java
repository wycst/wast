package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.jdbc.connection.ConnectionWraper;
import io.github.wycst.wast.jdbc.exception.SqlExecuteException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 临时会话执行器(同一个数据库连接)
 * <p> 不要在多线程中使用同一个会话；
 * <p> 需要手动关闭连接；
 */
public class TemporaryExecuter extends DefaultSqlExecuter {

    private final TemporaryConnectionWraper connectionWraper;

    /**
     * 临时会话执行器(同一个数据库连接)
     * <p> 不要在多线程中使用同一个会话；
     * <p> 需要手动关闭连接；
     *
     * @param dataSource
     * @return
     */
    public static TemporaryExecuter create(DataSource dataSource) {
        return new TemporaryExecuter(dataSource);
    }

    /**
     * 临时会话执行器(同一个数据库连接)
     * <p> 不要在多线程中使用同一个会话；
     * <p> 需要手动关闭连接；
     *
     * @param dataSource
     * @param properties
     * @return
     */
    public static TemporaryExecuter create(DataSource dataSource, SqlExecuterProperties properties) {
        return new TemporaryExecuter(dataSource, properties);
    }

    TemporaryExecuter(DataSource dataSource) {
        dataSource.getClass();
        setDataSource(dataSource);
        this.connectionWraper = new TemporaryConnectionWraper(dataSource);
    }

    TemporaryExecuter(DataSource dataSource, SqlExecuterProperties properties) {
        super(properties);
        dataSource.getClass();
        setDataSource(dataSource);
        this.connectionWraper = new TemporaryConnectionWraper(dataSource);
    }

    @Override
    final protected Connection getConnection() {
        return connectionWraper.getConnection();
    }

    @Override
    protected final ConnectionWraper getConnectionWraper() {
        return connectionWraper;
    }

    @Override
    public final void beginTransaction() {
        connectionWraper.beginTransaction();
    }

    @Override
    public final void commitTransaction() {
        commitTransaction(false);
    }

    @Override
    public final void commitTransaction(boolean closeConnection) {
        connectionWraper.commitTransaction(closeConnection);
    }

    @Override
    public final void endTransaction() {
        connectionWraper.endTransaction();
    }

    @Override
    public final void rollbackTransaction() {
        rollbackTransaction(false);
    }

    @Override
    public final void rollbackTransaction(boolean closeConnection) {
        connectionWraper.rollbackTransaction(closeConnection);
    }

    @Override
    public void close() {
        connectionWraper.close();
    }

    final class TemporaryConnectionWraper extends ConnectionWraper {

        private DataSource dataSource;
        private boolean closed;

        TemporaryConnectionWraper(DataSource dataSource) {
            super(null);
            this.dataSource = dataSource;
        }

        @Override
        public boolean autoClose() {
            return false;
        }

        /**
         * 仅仅第一次使用时申请连接
         *
         * @return
         */
        @Override
        public synchronized Connection getConnection() {
            if (closed) {
                throw new SqlExecuteException("Connection is closed");
            }
            if (physicalConn == null) {
                try {
                    physicalConn = dataSource.getConnection();
                } catch (SQLException e) {
                    throw new SqlExecuteException(e.getMessage(), e);
                }
            }
            return physicalConn;
        }

        @Override
        public void close() {
            super.close();
            this.closed = true;
        }

        public void beginTransaction() {
            try {
                getConnection().setAutoCommit(false);
                setTransaction(true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public void commitTransaction(boolean closeConnection) {
            if (physicalConn != null) {
                try {
                    if (isTransaction()) {
                        physicalConn.commit();
                    }
                    if (closeConnection) {
                        physicalConn.close();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void rollbackTransaction(boolean closeConnection) {
            if (physicalConn != null) {
                try {
                    if (isTransaction()) {
                        physicalConn.rollback();
                    }
                    if (closeConnection) {
                        physicalConn.close();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void endTransaction() {
            try {
                physicalConn.setAutoCommit(true);
                setTransaction(false);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
