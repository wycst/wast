package io.github.wycst.wast.jdbc.executer;

/**
 * @Date 2024/12/13 20:02
 * @Created by wangyc
 */
public final class SqlExecuteContext {

    final String apiName;
    final boolean disableLog;
    Object result;

    public String getApiName() {
        return apiName;
    }

    public boolean isDisableLog() {
        return disableLog;
    }

    public Object getResult() {
        return result;
    }

    public SqlExecuteContext result(Object result) {
        this.result = result;
        return this;
    }

    SqlExecuteContext(String apiName, boolean disableLog) {
        this.apiName = apiName;
        this.disableLog = disableLog;
    }

    public static SqlExecuteContext of(String apiName, boolean disableLog) {
        return new SqlExecuteContext(apiName, disableLog);
    }

    public static SqlExecuteContext of(String apiName) {
        return new SqlExecuteContext(apiName, false);
    }
}
