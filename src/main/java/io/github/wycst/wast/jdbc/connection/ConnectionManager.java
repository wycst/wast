package io.github.wycst.wast.jdbc.connection;

/**
 * 数据库连接管理
 *
 * @author wangyunchao
 */
public interface ConnectionManager {

    /**
     * 获取连接
     *
     * @return
     */
    public ConnectionWraper getConnectionWraper();

    /**
     * 开启事务
     */
    public void beginTransaction();

    /**
     * 提交事务，关闭连接
     */
    public void commitTransaction();

    /**
     * 提交事务，根据参数是否关闭连接
     *
     * @param closeConnection
     */
    public void commitTransaction(boolean closeConnection);

    /**
     * 回滚事务
     */
    public void rollbackTransaction();

    /**
     * 回滚事务，根据参数是否关闭连接
     *
     * @param closeConnection
     */
    public void rollbackTransaction(boolean closeConnection);

    /**
     * 结束事务
     */
    public void endTransaction();

    /**
     * 关闭数据库连接
     */
    public void closeConnection(ConnectionWraper wraper);

    /**
     * 清除当前线程绑定的信息
     */
    public void clear();
}
