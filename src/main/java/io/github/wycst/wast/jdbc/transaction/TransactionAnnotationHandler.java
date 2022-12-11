package io.github.wycst.wast.jdbc.transaction;

import io.github.wycst.wast.common.annotation.AnnotationAspect;
import io.github.wycst.wast.common.annotation.AnnotationHandler;
import io.github.wycst.wast.jdbc.annotations.EnableTransaction;
import io.github.wycst.wast.jdbc.annotations.Transaction;
import io.github.wycst.wast.jdbc.connection.ConnectionWraperUtils;

import java.lang.annotation.Annotation;
import java.sql.SQLException;

/**
 * @Transaction aop注解处理
 * @Author: wangy
 * @Date: 2019/12/8 9:53
 * @Description:
 */
@AnnotationAspect(aspectType = Transaction.class, enableControlType = EnableTransaction.class)
public class TransactionAnnotationHandler implements AnnotationHandler<Transaction, TransactionPoint> {

    @Override
    public TransactionPoint beforeAnnotationHandle(Transaction annotation) {

        TransactionPoint transactionPoint = null;

        // 当前事务点
        TransactionPoint currentTransactionPoint = TransactionUtils.currentTransactionPoint();

        TransactionType transactionType = annotation.value();

        if (currentTransactionPoint == null) {

            // 如果当前没有事务，类型NONE不启用事务
            if (transactionType == TransactionType.NONE) {
                TransactionUtils.setTransactionActive(false);
                return null;
            }

            // 清除线程数据
            doReset();
            // NEW和DEFAULT 代表要开启事务
            currentTransactionPoint = new TransactionPoint();
            currentTransactionPoint.setTop(true);
            currentTransactionPoint.setTransactionActive(true);

            // 暂时不创建连接（什么时候使用，什么时候创建，创建时读取事务是否active状态决定是否开启事务）
            TransactionUtils.bindTransactionPoint(currentTransactionPoint);
            transactionPoint = currentTransactionPoint;

            // 设置当前事务状态active为true
            TransactionUtils.setTransactionActive(true);

        } else {
            // 是否嵌套了独立事务
            boolean independent = transactionType == TransactionType.NEW;
            if (independent) {
                transactionPoint = new TransactionPoint();
                // 建立父子关系
                transactionPoint.setParent(currentTransactionPoint);
                transactionPoint.setIndependent(true);
                transactionPoint.setTransactionActive(true);

                currentTransactionPoint.getConnectionWrapers().clear();
                // 备份
                currentTransactionPoint.getConnectionWrapers().putAll(ConnectionWraperUtils.getCurrentConnectionWrapers());
                // 解绑
                ConnectionWraperUtils.reBindAll(null);

                // 设置当前事务状态active为true
                TransactionUtils.setTransactionActive(true);
                // 设置当前的事务点
                TransactionUtils.bindTransactionPoint(transactionPoint);

            } else if (transactionType == TransactionType.NONE) {

                // 如果已经开启了事务，暂停
                // 创建非事务点
                transactionPoint = new TransactionPoint();
                transactionPoint.setParent(currentTransactionPoint);

                currentTransactionPoint.getConnectionWrapers().clear();
                // 备份
                currentTransactionPoint.getConnectionWrapers().putAll(ConnectionWraperUtils.getCurrentConnectionWrapers());
                // 解绑
                ConnectionWraperUtils.reBindAll(null);

                TransactionUtils.bindTransactionPoint(transactionPoint);
                TransactionUtils.setTransactionActive(false);

            } else {
                // DEFAULT 或  Nested
                // 默认或嵌套事务
                // 设置当前事务状态active为true
                TransactionUtils.setTransactionActive(true);
            }
        }

        return transactionPoint;
    }

    @Override
    public void afterWithResult(Annotation annotation, TransactionPoint transactionPoint, Throwable err) {
        // 加入异常
        if (err != null) {
            // 当前事务点
            TransactionPoint currentTransactionPoint = TransactionUtils.currentTransactionPoint();
            currentTransactionPoint.getTransactionStatus().setStatus(1);
            currentTransactionPoint.getTransactionStatus().addNestedException(err);
        }

        if (transactionPoint == null) {
            return;
        }

        if (transactionPoint.isTop()) {
            try {
                TransactionStatus currentTransactionStatus = transactionPoint.getTransactionStatus();
                handlerAllTransactions(currentTransactionStatus);
                // 如果是top 重置当前线程绑定的信息
                doReset();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 判断是否独立事务
            boolean independent = transactionPoint.isIndependent();
            if (independent) {
                // 提交或回滚独立事务
                try {
                    TransactionStatus currentTransactionStatus = transactionPoint.getTransactionStatus();
                    handlerAllTransactions(currentTransactionStatus);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            // 还原
            TransactionPoint parent = transactionPoint.getParent();
            if (parent != null) {
                // 重新绑定
                TransactionUtils.bindTransactionPoint(parent);
                ConnectionWraperUtils.reBindAll(parent.getConnectionWrapers());

                // 还原当前事务状态 parent
                TransactionUtils.setTransactionActive(parent.isTransactionActive());
            }
        }
    }

    private void handlerAllTransactions(TransactionStatus currentTransactionStatus) throws SQLException {

        // 1  提交或回滚事务
        if (currentTransactionStatus.getStatus() == 0) {
            // 提交所有事务
            ConnectionWraperUtils.commitAll();
        } else {
            // 回滚所有事务
            ConnectionWraperUtils.rollbackAll();
            // 暂时不额外输出异常信息
        }

        // 2 如果注册了回调事件，执行注册的回调事件
        // 待设计
    }

    private void doReset() {
        TransactionUtils.doReset();
        ConnectionWraperUtils.doReset();
    }


}
