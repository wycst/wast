package io.github.wycst.wast.jdbc.interceptor;

import io.github.wycst.wast.jdbc.executer.SqlExecuteContext;

/**
 * @Author: wangy
 * @Date: 2021/8/7 10:52
 * @Description:
 */
public abstract class SqlInterceptor {

    /**
     * sql执行之前
     *
     * @param sql
     * @param params
     * @param executeContext
     */
    public abstract void before(String sql, Object params, SqlExecuteContext executeContext);

    /**
     * sql执行之后
     *
     * @param sql
     * @param params
     * @param executeContext
     * @return
     */
    public abstract Object after(String sql, Object params, SqlExecuteContext executeContext);
}
