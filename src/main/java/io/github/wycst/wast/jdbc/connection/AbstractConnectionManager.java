package io.github.wycst.wast.jdbc.connection;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * 连接管理
 */
public abstract class AbstractConnectionManager implements ConnectionManager {

    // 数据源
    private DataSource dataSource;

    public AbstractConnectionManager(DataSource dataSource) {
        dataSource.getClass();
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public abstract ConnectionWraper getConnectionWraper();

    @Override
    public void beginTransaction() {
        // TODO Auto-generated method stub

    }

    @Override
    public void commitTransaction() {
        commitTransaction(true);
    }

    @Override
    public void rollbackTransaction() {
        rollbackTransaction(true);
    }

    @Override
    public void commitTransaction(boolean closeConnection) {

        try {
            ConnectionWraper wraper = currentConnectionWraper();
            if (wraper != null) {
                if (wraper.isTransaction()) {
                    wraper.setCurrentInfluencingRows(0);
                    if (wraper.getConnection() != null) {
                        wraper.getConnection().commit();
                    }
                } else {
                    // if not in transaction
                }

                // close
                if (closeConnection)
                    wraper.close();
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void rollbackTransaction(boolean closeConnection) {

        try {
            ConnectionWraper wraper = currentConnectionWraper();
            if (wraper != null) {
                if (wraper.isTransaction()) {
                    wraper.setCurrentInfluencingRows(0);
                    if (wraper.getConnection() != null) {
                        wraper.getConnection().rollback();
                    }
                }

                // close
                if (closeConnection)
                    wraper.close();
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void endTransaction() {
        ConnectionWraper wraper = currentConnectionWraper();
        if (wraper != null) {
            wraper.setCurrentInfluencingRows(0);
            wraper.close();
        }
    }

    @Override
    public abstract void closeConnection(ConnectionWraper wraper);

    @Override
    public void clear() {

    }

    protected ConnectionWraper currentConnectionWraper() {
        return ConnectionWraperUtils.currentConnectionWraper(dataSource);
    }

    protected void bindConnectionWraper(ConnectionWraper wraper) {
        ConnectionWraperUtils.bindConnectionWraper(wraper, dataSource);
    }


}
