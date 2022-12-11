package io.github.wycst.wast.jdbc.transaction;

import io.github.wycst.wast.jdbc.connection.ConnectionWraper;

import java.util.HashMap;
import java.util.Map;

/**
 * 事务点
 *
 * @author wangy
 */
public class TransactionPoint {

    /**
     * 是否顶层事务
     */
    private boolean top;

    /**
     * 是否独立事务
     */
    private boolean independent;

    /**
     * 事务启用状态
     */
    private boolean transactionActive;

    /**
     * 嵌套事务中的父事务数据
     */
    private TransactionPoint parent;

    /**
     * 状态信息
     */
    private final TransactionStatus transactionStatus = new TransactionStatus();

    /**
     * 当前线程绑定的连接信息，使用独立事务时启用
     */
    private Map<Object, ConnectionWraper> connectionWrapers = new HashMap<Object, ConnectionWraper>();

    public boolean isTop() {
        return top;
    }

    public void setTop(boolean top) {
        this.top = top;
    }

    public boolean isIndependent() {
        return independent;
    }

    public void setIndependent(boolean independent) {
        this.independent = independent;
    }

    public TransactionPoint getParent() {
        return parent;
    }

    public void setParent(TransactionPoint parent) {
        this.parent = parent;
    }

    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    public Map<Object, ConnectionWraper> getConnectionWrapers() {
        return connectionWrapers;
    }

    public boolean isTransactionActive() {
        return transactionActive;
    }

    public void setTransactionActive(boolean transactionActive) {
        this.transactionActive = transactionActive;
    }

}
