package io.github.wycst.wast.jdbc.transaction;

public class TransactionUtils {

    // 当前事务点
    private static ThreadLocal<TransactionPoint> transactionPointOfThread = new ThreadLocal<TransactionPoint>();

    // 事务状态为激活
    private static ThreadLocal<Boolean> transactionActiveOfThread = new ThreadLocal<Boolean>();

    public static TransactionPoint currentTransactionPoint() {
        return transactionPointOfThread.get();
    }

    public static boolean isTransactionActive() {
        Boolean active = transactionActiveOfThread.get();
        return active == Boolean.TRUE;
    }

    public static void setTransactionActive(boolean active) {
        transactionActiveOfThread.set(active);
    }

    public static void bindTransactionPoint(TransactionPoint currentTransactionPoint) {
        transactionPointOfThread.set(currentTransactionPoint);
    }

    public static void doReset() {
        setTransactionActive(false);
        transactionPointOfThread.set(null);
    }

}
