package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.jdbc.commands.SqlExecuteCall;
import io.github.wycst.wast.jdbc.query.page.Page;
import io.github.wycst.wast.jdbc.query.sql.Sql;
import io.github.wycst.wast.jdbc.util.SqlUtil;
import io.github.wycst.wast.jdbc.util.StreamCursor;

import java.io.Serializable;
import java.util.*;

/**
 * 支持mybatis等的sql风格
 *
 * @author wangy
 */
@SuppressWarnings({"rawtypes"})
public class TemplateSqlExecuter {

    private DefaultSqlExecuter sqlExecuter;

    TemplateSqlExecuter(DefaultSqlExecuter sqlExecuter) {
        this.sqlExecuter = sqlExecuter;
    }

    /**
     * 批量更新（或插入） <br>
     * 注意批量操作最好不要有${},请使用#{}
     *
     * @param tmpSql
     * @param paramList
     * @return
     */
    public void updateCollection(String tmpSql, Collection<?> paramList) {
        Sql sqlObject = SqlUtil.getSqlObject(tmpSql, null);
        String sql = sqlObject.getFormalSql();
        List<Object[]> dataList = new ArrayList<Object[]>();
        for (Object target : paramList) {
            Object[] values = null;
            List<String> paramNames = sqlObject.getParamNames();
            if (paramNames.size() == 1 && (target instanceof CharSequence
                    || target instanceof Number
                    || target instanceof Boolean)) {
                values = new Object[]{target};
            } else {
                values = ObjectUtils.get(target, sqlObject.getParamNames());
            }
            dataList.add(values);
        }

        sqlExecuter.updateCollection(sql, dataList);
    }

    /***
     * @param tmpSql eg: INSERT INTO user(name, age)  VALUES(#{name},#{age});
     * @param paramList
     * @return
     */

    public int mysqlBatchInsert(String tmpSql, Collection<?> paramList) {
        Sql sqlObject = SqlUtil.getSqlObject(tmpSql, null);
        String sql = sqlObject.getFormalSql().trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }
        String[] sqlFragments = sql.split("[Vv][Aa][Ll][Uu][Ee][Ss]");
        String placeholder = sqlFragments[1];

        StringBuffer batchSqlBuffer = new StringBuffer(sql);
        List<Object> dataList = new ArrayList<Object>();
        int i = 0, len = paramList.size();
        for (Object target : paramList) {
            if (i++ > 0) {
                batchSqlBuffer.append(",").append(placeholder);
            }
            List<String> paramNames = sqlObject.getParamNames();
            Object[] values = ObjectUtils.get(target, sqlObject.getParamNames());
            dataList.addAll(Arrays.asList(values));
        }
        return sqlExecuter.update(batchSqlBuffer.toString(), dataList.toArray());
    }

    /**
     * 根据模版sql执行更新或插入
     *
     * @param tmpSql      变量#{name} 常量${name}
     * @param paramObject
     * @return
     */
    public int update(String tmpSql, Object paramObject) {
        Sql sqlObject = SqlUtil.getSqlObject(tmpSql, paramObject);
        return sqlExecuter.update(sqlObject.getFormalSql(), sqlObject.getParamValues());
    }

    /**
     * 根据模版插入对象
     *
     * @param tmpSql
     * @param returnGeneratedKeys
     * @param paramObject
     * @return
     */
    public Serializable insert(final String tmpSql, final boolean returnGeneratedKeys, Object paramObject) {
        Sql sqlObject = SqlUtil.getSqlObject(tmpSql, paramObject);
        return sqlExecuter.insert(sqlObject.getFormalSql(), returnGeneratedKeys, sqlObject.getParamValues());
    }


    /**
     * @param tmpSql
     * @param paramObject
     * @return
     */
    public int delete(String tmpSql, Object paramObject) {
        Sql sqlObject = SqlUtil.getSqlObject(tmpSql, paramObject);
        return sqlExecuter.update(sqlObject.getFormalSql(), sqlObject.getParamValues());
    }

    /**
     * query single value
     *
     * @param tmpSql
     * @param paramObject
     * @return
     */
    public Object queryValue(String tmpSql, Object paramObject) {
        Sql sqlObject = SqlUtil.getSqlObject(tmpSql, paramObject);
        return sqlExecuter.queryValue(sqlObject.getFormalSql(), sqlObject.getParamValues());
    }

    /**
     * query single value
     *
     * @param tmpSql
     * @param paramObject
     * @param valueClass
     * @param <E>
     * @return
     */
    public <E> E queryValue(String tmpSql, Object paramObject, final Class<E> valueClass) {
        Sql sqlObject = SqlUtil.getSqlObject(tmpSql, paramObject);
        return sqlExecuter.queryValue(sqlObject.getFormalSql(), valueClass, sqlObject.getParamValues());
    }

    /**
     * query Map
     */
    public Map queryMap(final String tmpSql, final Object paramObject) {
        Sql sqlObject = SqlUtil.getSqlObject(tmpSql, paramObject);
        return sqlExecuter.queryMap(sqlObject.getFormalSql(), sqlObject.getParamValues());
    }

    /**
     * query Object
     */
    public <E> E queryObject(final String sql, final Object paramObject, final Class<E> cls) {
        Sql sqlObject = SqlUtil.getSqlObject(sql, paramObject);
        return sqlExecuter.queryObject(sqlObject.getFormalSql(), cls, sqlObject.getParamValues());
    }

    /**
     * query Unique Object
     */
    public <E> E queryUniqueObject(final String sql, final Object paramObject, final Class<E> cls) {
        Sql sqlObject = SqlUtil.getSqlObject(sql, paramObject);
        return sqlExecuter.queryUniqueObject(sqlObject.getFormalSql(), cls, sqlObject.getParamValues());
    }

    /**
     * query Collection
     *
     * @param sql
     * @param paramObject
     * @param cls         集合元素类型
     * @return
     */
    public <E> List<E> queryList(final String sql, final Object paramObject, final Class<E> cls) {
        Sql sqlObject = SqlUtil.getSqlObject(sql, paramObject);
        return sqlExecuter.queryList(sqlObject.getFormalSql(), cls, sqlObject.getParamValues());
    }

    /**
     * 基于流式查询
     * <p> 适用场景： 数据量比较大
     *
     * @param sql
     * @param paramObject
     * @param cls
     * @param <E>
     * @return
     */
    public <E> StreamCursor<E> queryStream(final String sql, final Object paramObject, final Class<E> cls) {
        Sql sqlObject = SqlUtil.getSqlObject(sql, paramObject);
        return sqlExecuter.queryStream(sqlObject.getFormalSql(), cls, sqlObject.getParamValues());
    }

    /**
     * query page
     *
     * @param page
     * @param sql
     * @param paramObject
     */
    public <E> void queryPage(Page<E> page, final String sql, final Object paramObject) {
        Sql sqlObject = SqlUtil.getSqlObject(sql, paramObject);
        sqlExecuter.queryPage(page, sqlObject.getFormalSql(), sqlObject.getParamValues());
    }

    /**
     * query page
     *
     * @param sql
     * @param pageNum
     * @param limit
     * @param paramObject
     * @param cls
     * @param <E>
     * @return
     */
    public <E> Page<E> queryPage(final String sql, long pageNum, int limit, final Object paramObject, final Class<E> cls) {
        Sql sqlObject = SqlUtil.getSqlObject(sql, paramObject);
        return sqlExecuter.queryPage(sqlObject.getFormalSql(), pageNum, limit, cls, sqlObject.getParamValues());
    }

    /**
     * query page [map]
     *
     * @param sql
     * @param pageNum
     * @param limit
     * @param paramObject
     * @return
     */
    public Page<Map> queryPage(final String sql, long pageNum, int limit, final Object paramObject) {
        return queryPage(sql, pageNum, limit, paramObject, Map.class);
    }


    public <E> void executePipelined(SqlExecuteCall<E> sqlExecuteCall) {
        sqlExecuter.executePipelined(sqlExecuteCall);
    }
}
