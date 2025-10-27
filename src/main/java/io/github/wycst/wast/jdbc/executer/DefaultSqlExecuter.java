package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.common.idgenerate.providers.IdGenerator;
import io.github.wycst.wast.jdbc.commands.OperationSqlExecuteCommand;
import io.github.wycst.wast.jdbc.commands.SqlExecuteCall;
import io.github.wycst.wast.jdbc.connection.ConnectionManager;
import io.github.wycst.wast.jdbc.connection.ConnectionWraper;
import io.github.wycst.wast.jdbc.connection.DefaultConnectionManager;
import io.github.wycst.wast.jdbc.dialect.*;
import io.github.wycst.wast.jdbc.exception.SqlExecuteException;
import io.github.wycst.wast.jdbc.interceptor.SqlInterceptor;
import io.github.wycst.wast.jdbc.query.QueryExecutor;
import io.github.wycst.wast.jdbc.query.page.Page;
import io.github.wycst.wast.jdbc.util.SqlUtil;
import io.github.wycst.wast.jdbc.util.StreamCursor;
import io.github.wycst.wast.log.Log;
import io.github.wycst.wast.log.LogFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultSqlExecuter {

    final static Log log = LogFactory.getLog(DefaultSqlExecuter.class);

    /*唯一id*/
    final String uid = IdGenerator.hex();

    /**
     * sql执行器属性
     */
    private final SqlExecuterProperties executerProperties;

    /**
     * dataSource
     */
    private DataSource dataSource;

    /**
     * if use spring
     */
    private boolean useSpringTransactionManager = false;

    /**
     * conn manager
     */
    private ConnectionManager connectionManager;

    // query
    private final QueryExecutor queryExecutor = new QueryExecutor();

    private Dialect dialect;
    private PageDialectAgent pageDialectAgent;
    private String databaseProductName;

    boolean supportBatchInsert;
    boolean clickHouse;
    boolean mysql;
    boolean oracle;
    boolean gbase;
    boolean kingbase;
    String[] sqlTemplates = new String[SqlFunctionType.values().length];

    // api based on sql template
    private TemplateSqlExecuter templateExecutor = new TemplateSqlExecuter(this);

    // 实体操作
    private EntityExecuter entityExecuter = new EntityExecuter(this);

    // interceptor
    private SqlInterceptor sqlInterceptor;

    public DefaultSqlExecuter() {
        this(new SqlExecuterProperties());
    }

    public DefaultSqlExecuter(SqlExecuterProperties executerProperties) {
        this.executerProperties = executerProperties == null ? new SqlExecuterProperties() : executerProperties;
    }

    public void setDataSource(DataSource dataSource) {
        dataSource.getClass();
        if (dataSource != this.dataSource) {
            this.dataSource = dataSource;
            this.initDefaultConnectionManager();
            this.initialDialect();
        }
    }

    public void setSqlInterceptor(SqlInterceptor sqlInterceptor) {
        this.sqlInterceptor = sqlInterceptor;
    }

    public void setPageDialectAgent(PageDialectAgent pageDialectAgent) {
        this.pageDialectAgent = pageDialectAgent;
        if (this.dialect != null) {
            this.dialect.setPageDialectAgent(pageDialectAgent);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    private void initDefaultConnectionManager() {
        if (this.connectionManager == null) {
            // create default if null
            this.connectionManager = new DefaultConnectionManager(dataSource);
        }
    }

    public TemplateSqlExecuter getTemplateExecutor() {
        return templateExecutor;
    }

    public EntityExecuter getEntityExecuter() {
        return entityExecuter;
    }

    public void setUseSpringTransactionManager(boolean useSpringTransactionManager) {
        this.useSpringTransactionManager = useSpringTransactionManager;
        doCreateSpringConnectionManager();
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    private ConnectionManager currentConnectionManager() {
        if (connectionManager == null) {
            throw new NullPointerException("ConnectionManager error, Please set the datasource or connectionManager !");
        }
        return connectionManager;
    }

    private void doCreateSpringConnectionManager() {

        if (useSpringTransactionManager) {
            // force to create SpringConnectionManager running at spring env
            try {
                Class<?> connectionManagerCls = Class.forName("io.github.wycst.wast.jdbc.spring.connection.SpringConnectionManager");
                Constructor<?> constructor = connectionManagerCls.getConstructor(DataSource.class);
                constructor.setAccessible(true);

                this.connectionManager = (ConnectionManager) constructor.newInstance(dataSource);

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @param batchSize
     */
    public void setBatchSize(int batchSize) {
        executerProperties.setBatchSize(batchSize);
    }

    public void setQueryTimeout(int queryTimeout) {
        executerProperties.setQueryTimeout(queryTimeout);
    }

    public int getBatchSize() {
        return executerProperties.getBatchSize();
    }

    public void clear() {
        currentConnectionManager().clear();
    }

    public void beginTransaction() {
        currentConnectionManager().beginTransaction();
    }

    /**
     * default close connection when commit a transaction
     */
    public void commitTransaction() {
        commitTransaction(true);
    }

    /**
     * @param closeConnection if close connection
     */
    public void commitTransaction(boolean closeConnection) {
        currentConnectionManager().commitTransaction(closeConnection);
    }

    /**
     * if end a Transaction ,force close current connection if state of connection is active
     */
    public void endTransaction() {
        currentConnectionManager().endTransaction();
    }

    public void rollbackTransaction() {
        rollbackTransaction(true);
    }

    public void rollbackTransaction(boolean closeConnection) {
        currentConnectionManager().rollbackTransaction(closeConnection);
    }

    protected ConnectionWraper getConnectionWraper() {
        return currentConnectionManager().getConnectionWraper();
    }

    Dialect getDialect() {
        return dialect;
    }

    public final String getDatabaseProductName() {
        return databaseProductName;
    }

    public final String getUid() {
        return uid;
    }

    private void initialDialect() {

        Connection physicalConn = null;
        try {
            physicalConn = dataSource.getConnection();
            DatabaseMetaData dmd = physicalConn.getMetaData();
            String databaseProductName = dmd.getDatabaseProductName();
            this.databaseProductName = databaseProductName;

            boolean supportBatchInsert = false;
            if (databaseProductName != null) {
                String upperName = databaseProductName.toUpperCase();
                if (upperName.contains("MYSQL")) {
                    mysql = true;
                    this.dialect = new MySqlDialect();
                } else if (upperName.contains("ORACLE")) {
                    oracle = true;
                    this.dialect = new OracleDialect();
                } else if (upperName.contains("CLICKHOUSE")) {
                    clickHouse = true;
                    // ClickHouse
                    this.dialect = new ClickHouseDialect();
                    sqlTemplates[SqlFunctionType.UPDATE_BY_ID.ordinal()] = "ALTER TABLE %s UPDATE %s WHERE %s = %s";
                    sqlTemplates[SqlFunctionType.UPDATE_BY_PARAMS.ordinal()] = "ALTER TABLE %s t UPDATE %s %s";
                    sqlTemplates[SqlFunctionType.DELETE_BY_ID.ordinal()] = "ALTER TABLE %s DELETE WHERE %s = %s";
                    sqlTemplates[SqlFunctionType.DELETE_BY_PARAMS.ordinal()] = "ALTER TABLE %s DELETE %s";
                } else if (upperName.contains("KINGBASEES")) {
                    // use mysql
                    kingbase = true;
                    this.dialect = new MySqlDialect();
                } else if (upperName.contains("GBASE")) {
                    gbase = true;
                    // gbase
                    this.dialect = new GbaseDialect();
                } else {
                    // default
                    this.dialect = new DefaultDialect(pageDialectAgent);
                }
                supportBatchInsert = mysql || clickHouse || kingbase;
            }
            this.supportBatchInsert = supportBatchInsert;

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {

            if (physicalConn != null) {
                try {
                    physicalConn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * create connnection of current thread
     *
     * @return
     */
    protected Connection getConnection() {
        return getConnectionWraper().getConnection();
    }


    public boolean isShowSql() {
        if (isDevelopment()) {
            return true;
        }
        return executerProperties.getShowSql() == Boolean.TRUE;
    }

    public void setShowSql(boolean showSql) {
        executerProperties.setShowSql(showSql);
    }

    public boolean isFormatSql() {
        return executerProperties.getFormatSql() == Boolean.TRUE;
    }

    public void setFormatSql(boolean formatSql) {
        executerProperties.setFormatSql(formatSql);
    }

    public boolean isShowParameters() {
        if (isDevelopment()) {
            return true;
        }
        return executerProperties.getShowParameters() == Boolean.TRUE;
    }

    public void setDevelopment(boolean development) {
        executerProperties.setDevelopment(development);
    }

    public boolean isDevelopment() {
        return executerProperties.getDevelopment() == Boolean.TRUE;
    }

    public void setShowSqlParameters(boolean showSqlParameters) {
        executerProperties.setShowParameters(showSqlParameters);
    }

    final static ThreadLocal<Boolean> DISABLE_LOG_FLAG_TL = new ThreadLocal<Boolean>();

    private <E> E execute(
            OperationSqlExecuteCommand<E> command, String sql, Object params, SqlExecuteContext executeContext) {

        ConnectionWraper wraper = getConnectionWraper();
        E entity = null;
        long beginMillis = System.currentTimeMillis();
        boolean success = false;
        try {
            if (sqlInterceptor != null) {
                sqlInterceptor.before(sql, params, executeContext);
            }
            if (!executeContext.disableLog) {
                if (isShowSql() && sql != null) {
                    log.info("sql: {}", sql);
                }
                if (isShowParameters() && params != null) {
                    List paramList = getParamList(params);
                    log.info("parameters: {}", paramList);
                }
            }
            // 拦截器设计实现
            entity = command.doExecute(wraper);
            success = true;
            return entity;
        } catch (Throwable e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new SqlExecuteException(e.getMessage(), e);
        } finally {
            if (sqlInterceptor != null) {
                sqlInterceptor.after(sql, params, executeContext.result(entity));
            }
            if (isDevelopment()) {
                long endMillis = System.currentTimeMillis();
                log.info("api:[{}], exec: {}ms, success: {}", executeContext.apiName, endMillis - beginMillis, success);
            }
            // handler close
            if (wraper != null && wraper.autoClose() && command.closeable()) {
                connectionManager.closeConnection(wraper);
            }
        }
    }

    private List getParamList(Object params) {
        List<Object> paramList = null;
        if (params instanceof Object[]) {
            Object[] paramArr = (Object[]) params;
            paramList = Arrays.asList(paramArr);
        } else if (params instanceof List) {
            List<Object> list = (List<Object>) params;
            paramList = new ArrayList(list.size());
            for (int i = 0; i < list.size(); i++) {
                Object param = list.get(i);
                if (param instanceof Object[]) {
                    Object[] p = (Object[]) param;
                    paramList.add(Arrays.asList(p));
                } else {
                    paramList.add(param);
                }
            }
        } else {
            paramList = Arrays.asList(params);
        }
        return paramList;
    }

    /**
     * sql执行流水线(同一个连接)
     *
     * @param sqlExecuteCall
     * @param <E>
     */
    public <E> void executePipelined(SqlExecuteCall<E> sqlExecuteCall) {

        ConnectionWraper wraper = getConnectionWraper();
        try {
            sqlExecuteCall.execute(wraper.getConnection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            // handler close
            if (wraper != null && wraper.autoClose()) {
                connectionManager.closeConnection(wraper);
            }
        }
    }

    private PreparedStatement prepareStatement(Connection conn, String sql, Object[] params) throws SQLException {
        return prepareStatement(conn, sql, params, -1, -1);
    }

    private PreparedStatement prepareStatement(Connection conn, String sql, Object[] params, int type,
                                               int resultSetConcurrency) throws SQLException {
        PreparedStatement ps = dialect.prepareStatement(conn, sql, type, resultSetConcurrency);
        if (params != null && params.length > 0) {
            int index = 1;
            for (Object param : params) {
                dialect.setParameter(ps, index++, param);
            }
        }
        // set timeout
        ps.setQueryTimeout(executerProperties.getQueryTimeout());
        return ps;
    }

    public Serializable insert(final String sql, final boolean returnGeneratedKeys, final Object... params) {
        return insert(sql, returnGeneratedKeys, SqlExecuteContext.of("insert"), params);
    }

    final Serializable insertWithContext(final String sql, final boolean returnGeneratedKeys, SqlExecuteContext context, final Object... params) {

        return execute(new OperationSqlExecuteCommand<Serializable>() {

            public Serializable doExecute(ConnectionWraper wraper) throws SQLException {
                Connection conn = wraper.getConnection();
                PreparedStatement statement = null;
                try {
                    statement = prepareStatement(conn, sql, params, returnGeneratedKeys ? PreparedStatement.RETURN_GENERATED_KEYS : -1, -1);
                    int effect = statement.executeUpdate();
                    wraper.addInfluencingRows(effect);
                    Serializable generateKey = null;
                    if (returnGeneratedKeys) {
                        ResultSet generatedKeys = statement.getGeneratedKeys();
                        if (generatedKeys != null) {
                            if (generatedKeys.next()) {
                                generateKey = (Serializable) generatedKeys.getObject(1);
                            }
                            generatedKeys.close();
                        }
                    }
                    return generateKey;
                } finally {
                    if (statement != null) {
                        statement.close();
                    }
                }
            }
        }, sql, params, context);
    }

    /**
     * do update
     *
     * @param sql
     * @param params
     * @return
     */
    public int update(final String sql, final Object... params) {
        return updateWithContext(sql, SqlExecuteContext.of("update"), params);
    }

    /**
     * do update
     *
     * @param sql
     * @param context
     * @param params
     * @return
     */
    final int updateWithContext(final String sql, SqlExecuteContext context, final Object... params) {

        return execute(new OperationSqlExecuteCommand<Integer>() {

            public Integer doExecute(ConnectionWraper wraper) throws SQLException {
                Connection conn = wraper.getConnection();
                PreparedStatement statement = null;
                try {
                    statement = prepareStatement(conn, sql, params);
                    int effect = statement.executeUpdate();
                    wraper.addInfluencingRows(effect);

                    return effect;
                } finally {
                    if (statement != null) {
                        statement.close();
                    }
                }
            }
        }, sql, params, context);
    }

    public void updateCollection(final String sql, final List<Object[]> dataList) {
        updateCollectionWithContext(sql, dataList, SqlExecuteContext.of("updateCollection"));
    }

    /**
     * batch update
     *
     * @param sql
     * @param dataList
     * @return
     */
    final void updateCollectionWithContext(final String sql, final List<Object[]> dataList, SqlExecuteContext context) {

        execute(new OperationSqlExecuteCommand<Integer>() {

            public Integer doExecute(ConnectionWraper wraper) throws SQLException {
                Connection conn = wraper.getConnection();
                boolean isTransaction = wraper.isTransaction();
                if (!isTransaction) {
                    if (conn.getAutoCommit()) {
                        conn.setAutoCommit(false);
                    }
                }
                PreparedStatement statement = null;
                try {
                    statement = prepareStatement(conn, sql, null);
                    int rowIndex = 0;
                    for (Object[] values : dataList) {
                        int index = 1;
                        for (Object value : values) {
                            // statement.setObject(index++, value);
                            dialect.setParameter(statement, index++, value);
                        }
                        statement.addBatch();
                        if (++rowIndex % getBatchSize() == 0) {
                            statement.executeBatch();
                            if (!isTransaction) {
                                conn.commit();
                            }
                            statement.clearBatch();
                        }
                    }

                    statement.executeBatch();
                    if (!isTransaction) {
                        conn.commit();
                        // reset autocommit
                        conn.setAutoCommit(true);
                    }
                    statement.clearBatch();

                } finally {
                    if (statement != null) {
                        statement.close();
                    }
                }
                return 0;
            }
        }, sql, dataList, context);
    }


    /**
     * query single value
     *
     * @param sql
     * @param params
     * @return
     */
    public Object queryValue(final String sql, final Object... params) {
        return queryValue(sql, Object.class, params);
    }

    /**
     * query single value
     *
     * @param sql
     * @param valueClass
     * @param params
     * @return
     */
    public <E> E queryValue(final String sql, final Class<E> valueClass, final Object... params) {
        return queryValueWithContext(sql, valueClass, SqlExecuteContext.of("queryValue"), params);
    }

    <E> E queryValueWithContext(final String sql, final Class<E> valueClass, final SqlExecuteContext context, final Object... params) {
        return execute(new OperationSqlExecuteCommand<E>() {

            public E doExecute(ConnectionWraper wraper) throws SQLException {
                Connection conn = wraper.getConnection();
                PreparedStatement statement = prepareStatement(conn, sql, params, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                return queryExecutor.queryValue(valueClass, statement);
            }
        }, sql, params, context);
    }

    /**
     * query Map
     */
    public Map queryMap(final String sql, final Object... params) {
        return queryObject(sql, Map.class, params);
    }

    /**
     * query Object
     */
    public <E> E queryObject(final String sql, final Class<E> cls, final Object... params) {
        return queryObjectWithContext(sql, cls, SqlExecuteContext.of("queryObject"), params);
    }

    /**
     * query Object
     */
    final <E> E queryObjectWithContext(final String sql, final Class<E> cls, SqlExecuteContext executeContext, final Object... params) {
        return execute(new OperationSqlExecuteCommand<E>() {
            public E doExecute(ConnectionWraper wraper) throws SQLException {
                Connection conn = wraper.getConnection();
                PreparedStatement statement = prepareStatement(conn, sql, params, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                return queryExecutor.queryObject(cls, statement);
            }
        }, sql, params, executeContext);
    }

    /**
     * query Unique Object
     */
    public <E> E queryUniqueObject(final String sql, final Class<E> cls, final Object... params) {
        return queryUniqueObjectWithContext(sql, cls, SqlExecuteContext.of("queryUniqueObject"), params);
    }

    /**
     * query Unique Object
     */
    final <E> E queryUniqueObjectWithContext(final String sql, final Class<E> cls, SqlExecuteContext context, final Object... params) {
        return execute(new OperationSqlExecuteCommand<E>() {

            public E doExecute(ConnectionWraper wraper) throws SQLException {
                Connection conn = wraper.getConnection();
                PreparedStatement statement = prepareStatement(conn, sql, params, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                return queryExecutor.queryUniqueObject(cls, statement);
            }
        }, sql, params, context);
    }

    /**
     * query Collection
     *
     * @param cls    集合元素类型
     * @param sql
     * @param params
     * @return
     */
    public <E> List<E> queryList(final String sql, final Class<E> cls, final Object... params) {
        return queryListWithContext(sql, cls, SqlExecuteContext.of("queryList"), params);
    }

    /**
     * query Collection
     *
     * @param cls     集合元素类型
     * @param sql
     * @param context
     * @param params
     * @return
     */
    <E> List<E> queryListWithContext(final String sql, final Class<E> cls, final SqlExecuteContext context, final Object... params) {
        return execute(new OperationSqlExecuteCommand<List<E>>() {

            public List<E> doExecute(ConnectionWraper wraper) throws SQLException {
                Connection conn = wraper.getConnection();
                PreparedStatement statement = prepareStatement(conn, sql, params, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                return queryExecutor.queryList(cls, statement);
            }
        }, sql, params, context);
    }

    public <E> StreamCursor<E> queryStream(final String sql, final Class<E> cls, final Object... params) {
        return queryStreamWithContext(sql, cls, SqlExecuteContext.of("queryStream"), params);
    }

    final <E> StreamCursor<E> queryStreamWithContext(final String sql, final Class<E> cls, final SqlExecuteContext context, final Object... params) {
        return execute(new OperationSqlExecuteCommand<StreamCursor<E>>() {
            public StreamCursor<E> doExecute(ConnectionWraper wraper) throws SQLException {
                Connection conn = wraper.getConnection();
                PreparedStatement statement = prepareStatement(conn, sql, params, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                return queryExecutor.queryStreamCursor(cls, statement, conn);
            }

            @Override
            public boolean closeable() {
                return false;
            }
        }, sql, params, context);
    }


    /**
     * query Collection
     *
     * @param sql
     * @param params
     * @return
     */
    public List<Map> queryList(final String sql, final Object... params) {
        return queryList(sql, Map.class, params);
    }

    /**
     * @param sql
     * @param offset
     * @param pageSize
     * @return
     */
    public String getLimitSql(String sql, long offset, int pageSize) {
        return this.dialect.getLimitString(sql, offset, pageSize);
    }

    /**
     * query page
     *
     * @param page
     * @param sql
     * @param cls
     * @param params
     */
    private <E> void queryPage(Page page, final String sql, final Class<E> cls, final Object... params) {

        // 分页的sql
        final String queryLimitSql = this.getLimitSql(sql, page.getOffset(), page.getPageSize());
        // 记录列表
        List<E> rows = queryList(queryLimitSql, cls, params);
        page.setRows(rows);

        // 解析sql 获取查询总数的 queryTotalSql
        final String queryTotalSql = SqlUtil.getTotalSql(sql);
        long total = queryValue(queryTotalSql, long.class, params);
        page.setTotal(total);

    }

    /**
     * query page
     *
     * @param page
     * @param sql
     * @param params
     */
    public <E> void queryPage(Page<E> page, final String sql, final Object... params) {
        Class<E> cls = page.actualType();
        queryPage(page, sql, cls == null ? Map.class : cls, params);
    }

    /**
     * query page
     *
     * @param sql
     * @param pageNum
     * @param limit
     * @param cls
     * @param params
     * @return
     */
    public <E> Page<E> queryPage(final String sql, long pageNum, int limit, final Class<E> cls, final Object... params) {
        Page<E> page = Page.pageInstance(cls);
        page.setPage(pageNum);
        page.setPageSize(limit);

        queryPage(page, sql, cls == null ? Map.class : cls, params);
        return page;
    }

    /**
     * query page [map]
     *
     * @param sql
     * @param pageNum
     * @param limit
     * @param params
     * @return
     */
    public Page<Map> queryPage(final String sql, long pageNum, int limit, final Object... params) {
        return queryPage(sql, pageNum, limit, Map.class, params);
    }

    public boolean isSupportBatchInsert() {
        return supportBatchInsert;
    }

    public void close() {
        this.clear();
    }

    /**
     * 执行sql
     *
     * @param sql
     * @return
     */
    public int executeUpdate(final String sql) {
        return execute(new OperationSqlExecuteCommand<Integer>() {

            public Integer doExecute(ConnectionWraper wraper) throws SQLException {
                Connection conn = wraper.getConnection();
                PreparedStatement statement = null;
                try {
                    statement = conn.prepareStatement(sql);
                    return statement.executeUpdate();
                } finally {
                    if (statement != null) {
                        statement.close();
                    }
                }
            }
        }, sql, null, SqlExecuteContext.of("executeUpdate"));
    }

    /**
     * 执行脚本文件（文件/流），多行文本使用分号+换行组合分隔
     *
     * @param is
     */
    public void executeScript(InputStream is) throws IOException {
        final List<String> sqlList = SqlUtil.readSqlScripts(is);
        execute(new OperationSqlExecuteCommand<Integer>() {
            public Integer doExecute(ConnectionWraper wraper) throws SQLException {
                Connection conn = wraper.getConnection();
                Statement statement = null;
                try {
                    statement = conn.createStatement();
                    for (String sql : sqlList) {
                        if (isShowSql()) {
                            log.info("\n{}", sql);
                        }
                        statement.addBatch(sql);
                    }
                    statement.executeBatch();
                    return 0;
                } finally {
                    if (statement != null) {
                        statement.close();
                    }
                }
            }
        }, null, null, SqlExecuteContext.of("executeScript"));
    }

    public boolean isClickHouse() {
        return clickHouse;
    }

    public boolean isMysql() {
        return mysql;
    }

    public boolean isOracle() {
        return oracle;
    }

    public boolean isGbase() {
        return gbase;
    }

    public boolean isKingbase() {
        return kingbase;
    }
}
