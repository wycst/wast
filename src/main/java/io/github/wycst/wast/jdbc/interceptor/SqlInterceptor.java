package io.github.wycst.wast.jdbc.interceptor;

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
     * @param methodName
     */
    public abstract void before(String sql, Object params, String methodName);

    /**
     * sql执行之后
     *
     * @param sql
     * @param params
     * @param methodName
     * @param result     sql结果
     * @return
     */
    public abstract Object after(String sql, Object params, String methodName, Object result);
}
