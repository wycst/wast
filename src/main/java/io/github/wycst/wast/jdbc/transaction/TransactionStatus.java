package io.github.wycst.wast.jdbc.transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * 事务状态
 *
 * @author wangy
 */
public class TransactionStatus {

    /**
     * 状态
     */
    private int status;

    /**
     * 嵌套异常集合
     */
    private List<Throwable> nestedExceptions = new ArrayList<Throwable>();

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<Throwable> getNestedExceptions() {
        return nestedExceptions;
    }

    public void setNestedExceptions(List<Throwable> nestedExceptions) {
        this.nestedExceptions = nestedExceptions;
    }

    public void addNestedException(Throwable throwable) {
        this.nestedExceptions.add(throwable);
    }

}
