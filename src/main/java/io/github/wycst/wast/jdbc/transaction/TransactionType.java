package io.github.wycst.wast.jdbc.transaction;

public enum TransactionType {

    /**
     * 默认事务类型：
     */
    DEFAULT,

    /**
     * 如果当前有事务，挂起当前事务，开启新事务
     */
    NEW,

    /**
     * 如果当前有事务，挂起事务，以非事务状态处理数据库操作
     */
    NONE

}
