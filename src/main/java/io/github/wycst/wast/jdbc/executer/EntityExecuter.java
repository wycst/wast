package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.jdbc.exception.EntityException;
import io.github.wycst.wast.jdbc.exception.OqlParematerException;
import io.github.wycst.wast.jdbc.exception.SqlExecuteException;
import io.github.wycst.wast.jdbc.query.page.Page;
import io.github.wycst.wast.jdbc.query.sql.Sql;
import io.github.wycst.wast.jdbc.util.StreamCursor;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 实体类执行器
 *
 * @Author wangyunchao
 * @Date 2022/12/3 12:24
 */
public final class EntityExecuter implements OqlExecuter {

    private final DefaultSqlExecuter sqlExecuter;

    EntityExecuter(DefaultSqlExecuter sqlExecuter) {
        this.sqlExecuter = sqlExecuter;
    }

    @Override
    public DefaultSqlExecuter getSqlExecuter() {
        return sqlExecuter;
    }

    @Override
    public <E> E get(Class<E> entityCls, Serializable id) {
        return get(entityCls, id, false);
    }

    @Override
    public <E> E get(Class<E> entityCls, Serializable id, boolean fetch) {
        id.getClass();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        E result = entitySqlMapping.entityHandler.getById(sqlExecuter, entityCls, id); // getById(entitySqlMapping, entityCls, id);
        if (fetch && result != null) {
            this.handleFetch(entitySqlMapping, result);
        }
        return result;
    }

    @Override
    public <E> List<E> queryAll(Class<E> cls) {
        return queryBy(cls, (Map) null);
    }

    @Override
    public <E> E queryOne(Class<E> entityCls, Map<String, Object> params) {
        return executeQueryOne(entityCls, params);
    }

    @Override
    public <E> E queryOne(Class<E> entityCls, E params) {
        return executeQueryOne(entityCls, params);
    }

    @Override
    public <E> long queryCount(Class<E> entityCls) {
        return queryCount(entityCls, (Map<String, Object>) null);
    }

    @Override
    public <E> long queryCount(Class<E> entityCls, Map<String, Object> params) {
        return executeQueryCount(entityCls, params);
    }

    @Override
    public <E> long queryCount(Class<E> entityCls, E params) {
        return executeQueryCount(entityCls, params);
    }

    public <E> long queryCount(Class<E> cls, OqlQuery query, Object params) {
        checkEntityClass(cls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(cls);
        Sql countSqlObject = entitySqlMapping.getCountSqlObject(query, params);
        return sqlExecuter.queryValueWithContext(countSqlObject.getFormalSql(), long.class, entitySqlMapping.createContext("EntityExecuter#queryCount"), countSqlObject.getParamValues());
    }

    @Override
    public <E> E queryUnique(Class<E> entityCls, Map<String, Object> params) {
        return executeQueryUnique(entityCls, params);
    }

    @Override
    public <E> E queryUnique(Class<E> entityCls, E params) {
        return executeQueryUnique(entityCls, params);
    }

    @Override
    public <E> List<E> queryList(Class<E> entityCls, OqlQuery query, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);

        Sql sqlObject = entitySqlMapping.getSelectSqlObject(query, params);
        return sqlExecuter.queryListWithContext(sqlObject.getFormalSql(), entityCls, entitySqlMapping.createContext("EntityExecuter#queryList"), sqlObject.getParamValues());
    }

    @Override
    public <E> List<E> queryList(Class<E> cls, OqlQuery query) {
        return queryList(cls, query, new HashMap<String, Object>());
    }

    @Override
    public <E> StreamCursor<E> queryStreamBy(Class<E> entityCls, Map<String, Object> params) {
        return executeQueryStreamBy(entityCls, params);
    }

    @Override
    public <E> StreamCursor<E> queryStreamBy(Class<E> entityCls, E params) {
        return executeQueryStreamBy(entityCls, params);
    }

    @Override
    public <E> StreamCursor<E> queryStream(Class<E> entityCls, OqlQuery query, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);

        Sql sqlObject = entitySqlMapping.getSelectSqlObject(query, params);
        return sqlExecuter.queryStreamWithContext(sqlObject.getFormalSql(), entityCls, entitySqlMapping.createContext("EntityExecuter#queryStream"), sqlObject.getParamValues());
    }

    @Override
    public <E> List<E> queryBy(Class<E> entityCls, Map<String, Object> params) {
        return executeQueryBy(entityCls, params);
    }

    @Override
    public <E> List<E> queryBy(Class<E> entityCls, E params) {
        return executeQueryBy(entityCls, params);
    }

    @Override
    public <E> List<E> queryByIds(Class<E> entityCls, List<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList();
        }
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        // select * from t where id in(?, ?, ?)
        String selectInSql = entitySqlMapping.getSelectSqlByIds(ids);
        return sqlExecuter.queryListWithContext(selectInSql, entityCls, entitySqlMapping.createContext("EntityExecuter#queryByIds"), ids.toArray());
    }

    @Override
    public <E> List<E> queryByIds(Class<E> entityCls, Serializable... ids) {
        if (ids.length == 0) {
            return new ArrayList();
        }
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        // select * from t where id in(?, ?, ?)
        Collection list = Arrays.asList(ids);
        String selectInSql = entitySqlMapping.getSelectSqlByIds(list);
        return sqlExecuter.queryListWithContext(selectInSql, entityCls, entitySqlMapping.createContext("EntityExecuter#queryByIds"), ids);
    }

    @Override
    public <E> Page<E> queryPage(Page<E> page, OqlQuery query, Object params) {
        Class<E> entityCls = page.actualType();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);

        Sql sqlObject = entitySqlMapping.getSelectSqlObject(query, params, true);
        this.executeQueryPage(page, entityCls, sqlObject, entitySqlMapping);
        return page;
    }

    @Override
    public <E> Page<E> queryPage(Page<E> page) {
        return queryPage(page, (Map<String, Object>) null);
    }

    @Override
    public <E> Page<E> queryPage(Page<E> page, Map<String, Object> params) {
        return executeQueryPage(page, params);
    }

    @Override
    public <E> Page<E> queryPage(Page<E> page, E params) {
        return executeQueryPage(page, params);
    }

    @Override
    public <E> Serializable insert(E entity) {
        Class entityCls = entity.getClass();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);

        Sql sqlObject = entitySqlMapping.getInsertSqlObject(entity);
        return sqlExecuter.insertWithContext(sqlObject.getFormalSql(), true, entitySqlMapping.createContext("EntityExecuter#insert"), sqlObject.getParamValues());
    }

    @Override
    public <E> void insertList(List<E> entityList) {
        if (entityList.size() == 0) {
            return;
        }
        Class entityCls = entityList.get(0).getClass();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        SqlExecuteContext context = entitySqlMapping.createContext("EntityExecuter#insertList");
        if (entitySqlMapping.isUsePlaceholderOnInsert()) {
            if (isSupportBatchInsert()) {
                Sql batchInsertSqlObject = entitySqlMapping.getBatchInsertSqlObject(entityList);
                sqlExecuter.updateWithContext(batchInsertSqlObject.getFormalSql(), context, batchInsertSqlObject.getParamValues());
            } else {
                for (E e : entityList) {
                    Sql sqlObject = entitySqlMapping.getInsertSqlObject(e);
                    sqlExecuter.insertWithContext(sqlObject.getFormalSql(), true, context, sqlObject.getParamValues());
                }
            }
        } else {
            Sql sqlObject = entitySqlMapping.getInsertSqlObjectList(entityList);
            sqlExecuter.updateCollectionWithContext(sqlObject.getFormalSql(), sqlObject.getParamValuesList(), context);
        }
    }

    @Override
    public <E> int mysqlBatchInsert(List<E> entityList) {
        if (!isSupportBatchInsert()) {
            throw new SqlExecuteException("the current database does not support batchInsert");
        }
        if (entityList.size() == 0) {
            return 0;
        }
        Class entityCls = entityList.get(0).getClass();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);

        Sql batchInsertSqlObject = entitySqlMapping.getBatchInsertSqlObject(entityList);
        return sqlExecuter.updateWithContext(batchInsertSqlObject.getFormalSql(), entitySqlMapping.createContext("EntityExecuter#mysqlBatchInsert"), batchInsertSqlObject.getParamValues());
    }

    @Override
    public <E> int update(E entity) {
        Class entityCls = entity.getClass();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        return entitySqlMapping.entityHandler.updateEntity(sqlExecuter, entity);
    }

    @Override
    public <E> int updateBy(Class<E> entityCls, OqlQuery query, E params, String... fields) {
        return handleUpdateByParams(entityCls, query, params, fields);
    }

    @Override
    public <E> int updateBy(Class<E> entityCls, OqlQuery query, Map<String, Object> params, String... fields) {
        return handleUpdateByParams(entityCls, query, params, fields);
    }

    <E> int handleUpdateByParams(Class<E> entityCls, OqlQuery query, Object params, String... fields) {
        if (fields.length == 0) return 0;
        if (params == null) {
            throw new SqlExecuteException("params is null");
        }

        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);

        String sqlStringFormat = getSqlStringFormat(SqlFunctionType.UPDATE_BY_PARAMS);
        Sql sqlObject = entitySqlMapping.getUpdateSqlObject(sqlStringFormat, query, params, fields);
        return sqlExecuter.updateWithContext(sqlObject.getFormalSql(), entitySqlMapping.createContext("EntityExecuter#updateBy"), sqlObject.getParamValues());
    }

    @Override
    public <E> int updateFields(E e, String... fields) {
        return updateFields(e, Arrays.asList(fields), false);
    }

    @Override
    public <E> int updateFields(E e, boolean isExclude, String... fields) {
        return updateFields(e, Arrays.asList(fields), isExclude);
    }

    @Override
    public <E> int updateFields(E entity, List<String> fields) {
        return updateFields(entity, fields, false);
    }

    @Override
    public <E> int updateFields(E entity, List<String> fields, boolean isExclude) {
        Class entityCls = entity.getClass();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String sqlStringFormat = getSqlStringFormat(SqlFunctionType.UPDATE_BY_ID);

        Sql sqlObject = entitySqlMapping.getUpdateSqlObject(sqlStringFormat, entity, fields, isExclude);
        if (sqlObject == null) {
            throw new OqlParematerException("configuration error: " + entityCls + " may not have a column defined @Id, please check the annotation configuration");
        }
        try {
            return sqlExecuter.updateWithContext(sqlObject.getFormalSql(), entitySqlMapping.createContext("EntityExecuter#updateFields"), sqlObject.getParamValues());
        } finally {
            entitySqlMapping.entityHandler.afterUpdate(sqlExecuter, entity);
        }
    }

    @Override
    public <E> int deleteAll(Class<E> entityCls) {
        return deleteBy(entityCls, (Map<String, Object>) null);
    }

    @Override
    public <E> int delete(Class<E> entityCls, Serializable id) {
        return delete(entityCls, id, false);
    }

    @Override
    public <E> int delete(Class<E> entityCls, Serializable id, boolean cascade) {
        id.getClass();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        if (cascade) {
            // handle cascadeDelete
            this.executeCascadeDelete(entitySqlMapping, id);
        }
        return entitySqlMapping.entityHandler.deleteById(sqlExecuter, entityCls, id);
    }

    @Override
    public <E> int deleteList(List<E> entityList) {
        if (entityList == null) {
            return 0;
        }
        int influenceRows = 0;
        Class<?> entityCls = entityList.get(0).getClass();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String sqlStringFormat = getSqlStringFormat(SqlFunctionType.DELETE_BY_ID);
        String deleteSql = entitySqlMapping.getDeleteSql(sqlStringFormat);
        if (deleteSql == null) {
            throw new OqlParematerException("configuration error: " + entityCls + " may not have a column defined @Id, please check the annotation configuration");
        }
        SqlExecuteContext context = entitySqlMapping.createContext("EntityExecuter#deleteById");
        for (Object entity : entityList) {
            Serializable id = entitySqlMapping.getId(entity);
            influenceRows += sqlExecuter.updateWithContext(deleteSql, context, id);
        }
        if (influenceRows > 0) {
            entitySqlMapping.entityHandler.afterBatchDelete();
        }
        return influenceRows;
    }

    @Override
    public <E> int deleteByIds(Class<E> entityCls, List<? extends Serializable> ids) {
        if (ids == null || ids.size() == 0) {
            return 0;
        }
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String sqlStringFormat = getSqlStringFormat(SqlFunctionType.DELETE_BY_ID);
        String deleteSql = entitySqlMapping.getDeleteSql(sqlStringFormat);
        if (deleteSql == null) {
            throw new OqlParematerException("configuration error: " + entityCls + " may not have a column defined @Id, please check the annotation configuration");
        }
        List<Object[]> dataList = new ArrayList<Object[]>();
        for (Serializable id : ids) {
            dataList.add(new Object[]{id});
        }
        sqlExecuter.updateCollectionWithContext(deleteSql, dataList, entitySqlMapping.createContext("EntityExecuter#deleteByIds"));
        return ids.size();
    }

    @Override
    public <E> int deleteByIds(Class<E> cls, Serializable... ids) {
        return deleteByIds(cls, Arrays.asList(ids));
    }

    @Override
    public <E> int deleteBy(Class<E> entityCls, E params) {
        return handleDeleteBy(entityCls, params);
    }

    @Override
    public <E> int deleteBy(Class<E> entityCls, Map<String, Object> params) {
        return handleDeleteBy(entityCls, params);
    }

    <E> int handleDeleteBy(Class<E> entityCls, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);

        String sqlTemplate = getSqlStringFormat(SqlFunctionType.DELETE_BY_PARAMS);
        Sql sqlObject = entitySqlMapping.getDeleteSqlObjectByParams(sqlTemplate, params);
        try {
            return sqlExecuter.updateWithContext(sqlObject.getFormalSql(), entitySqlMapping.createContext("EntityExecuter#deleteBy"), sqlObject.getParamValues());
        } finally {
            entitySqlMapping.entityHandler.afterBatchDelete();
        }
    }

    @Override
    public <E> int deleteBy(Class<E> entityCls, OqlQuery query, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String sqlTemplate = getSqlStringFormat(SqlFunctionType.DELETE_BY_PARAMS);
        Sql sqlObject = entitySqlMapping.getDeleteSqlObject(sqlTemplate, query, params);
        try {
            return sqlExecuter.updateWithContext(sqlObject.getFormalSql(), entitySqlMapping.createContext("EntityExecuter#deleteBy"), sqlObject.getParamValues());
        } finally {
            entitySqlMapping.entityHandler.afterBatchDelete();
        }
    }

    @Override
    public <E> String reverseDeleteSQL(E entity) {
        checkEntityClass(entity.getClass());
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entity.getClass());
        return generateDeleteSql(entitySqlMapping, entity);
    }

    @Override
    public <E> String reverseInsertSQL(E entity) {
        checkEntityClass(entity.getClass());
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entity.getClass());
        return generateInsertSql(entitySqlMapping, entity);
    }

    void checkEntityClass(Class<?> entityCls) {
        EntityManagementFactory.defaultManagementFactory().checkEntityClass(entityCls);
    }

    EntitySqlMapping getEntitySqlMapping(Class<?> entityCls) {
        return EntityManagementFactory.defaultManagementFactory().getEntitySqlMapping(entityCls);
    }

    void handleFetch(EntitySqlMapping entitySqlMapping, Object result) {
        List<CascadeFetchMapping> cascadeFetchMappings = entitySqlMapping.getCascadeFetchMappings();
        for (CascadeFetchMapping cascadeFetchMapping : cascadeFetchMappings) {
            if (!cascadeFetchMapping.isFetch()) {
                continue;
            }
            Map<String, Object> params = getCascadeFetchParams(cascadeFetchMapping, entitySqlMapping, result);
            if (params == null) {
                return;
            }
            int fieldTypeValue = cascadeFetchMapping.getFieldType();
            Class<?> targetEntityClass = cascadeFetchMapping.getTargetEntityClass();
            List list = executeQueryBy(targetEntityClass, params);
            Object fetchFieldVal = null;
            if (fieldTypeValue == 1) {
                if (list.size() > 0) {
                    fetchFieldVal = list.get(0);
                }
            } else {
                fetchFieldVal = list;
            }
            if (fetchFieldVal != null) {
                cascadeFetchMapping.setFetchFieldValue(result, fetchFieldVal);
            }
        }
    }

    Map<String, Object> getCascadeFetchParams(CascadeFetchMapping cascadeFetchMapping, EntitySqlMapping entitySqlMapping, Object result) {
        String fieldName = cascadeFetchMapping.getFieldName();
        FieldColumn fieldColumn = entitySqlMapping.getFieldColumnMapping().get(fieldName);
        if (fieldColumn == null) {
            Field fetchField = cascadeFetchMapping.getCascadeFetchField();
            Class<?> clazz = entitySqlMapping.getEntityClass();
            throw new EntityException("Entity Class " + clazz + " and field[" + fetchField.getName() + "] is AnnotationPresent @CascadeFetch, but field '" + fieldName + "' is not exist");
        }
        Map<String, Object> params = null;
        Object val = entitySqlMapping.getFieldColumnValue(false, fieldColumn, result);
        if (val != null) {
            String targetFieldName = cascadeFetchMapping.getTargetFieldName();
            params = new HashMap<String, Object>();
            params.put(targetFieldName, val);
        }

        return params;
    }

    <E> E executeQueryOne(Class<E> entityCls, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);

        Sql sqlObject = entitySqlMapping.getSelectSqlObject(params);
        return sqlExecuter.queryObjectWithContext(sqlObject.getFormalSql(), entityCls, entitySqlMapping.createContext("EntityExecuter#queryOne"), sqlObject.getParamValues());
    }

    long executeQueryCount(Class<?> entityCls, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);

        Sql countSqlObject = entitySqlMapping.getCountSqlObject(params);
        return sqlExecuter.queryValueWithContext(countSqlObject.getFormalSql(), long.class, entitySqlMapping.createContext("EntityExecuter#queryCount"), countSqlObject.getParamValues());
    }

    <E> E executeQueryUnique(Class<E> entityCls, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);

        Sql sqlObject = entitySqlMapping.getSelectSqlObject(params);
        return sqlExecuter.queryUniqueObjectWithContext(sqlObject.getFormalSql(), entityCls, entitySqlMapping.createContext("EntityExecuter#queryUnique"), sqlObject.getParamValues());
    }

    <E> StreamCursor<E> executeQueryStreamBy(Class<E> entityCls, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        Sql sqlObject = entitySqlMapping.getSelectSqlObject(params);
        return sqlExecuter.queryStreamWithContext(sqlObject.getFormalSql(), entityCls, entitySqlMapping.createContext("EntityExecuter#queryStreamBy"), sqlObject.getParamValues());
    }

    <E> List<E> executeQueryBy(Class<E> entityCls, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        return entitySqlMapping.entityHandler.executeQueryBy(sqlExecuter, entityCls, params);
    }

    <E> Page<E> executeQueryPage(Page<E> page, Object params) {
        Class<E> entityCls = page.actualType();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);

        Sql sqlObject = entitySqlMapping.getSelectSqlObject(params, true);
        executeQueryPage(page, entityCls, sqlObject, entitySqlMapping);
        return page;
    }

    <E> void executeQueryPage(Page<E> page, Class<E> entityCls, Sql sqlObject, EntitySqlMapping entitySqlMapping) {
        String totalSql = sqlObject.getTotalSql();
        String formalSql = sqlObject.getFormalSql();
        Object[] paramValues = sqlObject.getParamValues();

        // 分页的sql
        final String limitSql = sqlExecuter.getLimitSql(formalSql, page.getOffset(), page.getPageSize());
        // 列表
        List<E> rows = sqlExecuter.queryListWithContext(limitSql, entityCls, entitySqlMapping.createContext("EntityExecuter#queryPage#rows"), paramValues);
        page.setRows(rows);

        long total = sqlExecuter.queryValueWithContext(totalSql, long.class, entitySqlMapping.createContext("EntityExecuter#queryPage#total"), paramValues);
        page.setTotal(total);
    }


    String getSqlStringFormat(SqlFunctionType sqlFunctionType) {
        return sqlExecuter.sqlTemplates[sqlFunctionType.ordinal()];
    }

    void executeCascadeDelete(EntitySqlMapping entitySqlMapping, Serializable id) {
        List<CascadeFetchMapping> cascadeFetchMappings = entitySqlMapping.getCascadeFetchMappings();
        Class<?> clazz = entitySqlMapping.getEntityClass();
        Object result = entitySqlMapping.entityHandler.getById(sqlExecuter, clazz, id);
        for (CascadeFetchMapping cascadeFetchMapping : cascadeFetchMappings) {
            if (!cascadeFetchMapping.isCascade()) {
                continue;
            }
            Map<String, Object> params = getCascadeFetchParams(cascadeFetchMapping, entitySqlMapping, result);
            if (params == null) {
                return;
            }
            Class<?> entityClass = cascadeFetchMapping.getTargetEntityClass();
            deleteBy(entityClass, params);
        }
    }

    /***
     * 生成删除sql
     *
     * @return
     */
    String generateDeleteSql(EntitySqlMapping entitySqlMapping, Object entity) {
        if (entity == null) {
            return null;
        }
        String template = "DELETE FROM `%s` WHERE %s = ";
        if (sqlExecuter.clickHouse) {
            template = "ALTER TABLE %s DELETE WHERE %s = ";
        }
        Object primaryValue = ObjectUtils.get(entity, entitySqlMapping.getPrimary().getField().getName());
        if (primaryValue instanceof String) {
            primaryValue = "'" + primaryValue + "'";
        }
        String sql = StringUtils.replacePlaceholder(template, "%s", entitySqlMapping.getTableName(),
                entitySqlMapping.getPrimary().getColumnName());
        return sql + primaryValue;
    }

    /**
     * insert sql
     *
     * @return
     */
    private String generateInsertSql(EntitySqlMapping entitySqlMapping, Object entity) {
        if (entity == null) {
            return null;
        }
        String tmpSql = "INSERT INTO `%s` (%s) VALUES (%s)";
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        int index = 0;
        Map<String, FieldColumn> fieldColumnMapping = entitySqlMapping.getFieldColumnMapping();
        int columnLength = fieldColumnMapping.size();
        for (Map.Entry<String, FieldColumn> entry : fieldColumnMapping.entrySet()) {
            String fieldName = entry.getKey();
            FieldColumn fieldColumn = entry.getValue();
            String columnName = fieldColumn.getColumnName();
            Object value = ObjectUtils.get(entity, fieldName);
            columns.append(columnName);
            if (value == null || value instanceof Number) {
                values.append(value);
            } else {
                if (value instanceof Date) {
                    value = new GregorianDate(((Date) value).getTime()).format();
                } else if (value instanceof String) {
                    // 一个\使用\\\\替换
                    value = ((String) value).replace("\\", "\\\\");
                    // 换行问题
                    value = ((String) value).replace("\n", "\\n");
                    // 处理引号问题
                    value = ((String) value).replace("\"", "\\\"");
                    // 单引号 -> \'
                    value = ((String) value).replace("'", "\\'");
                }
                values.append("'").append(value).append("'");
            }
            if (++index < columnLength) {
                columns.append(", ");
                values.append(", ");
            }
        }

        return StringUtils.replacePlaceholder(tmpSql, "%s", entitySqlMapping.getTableName(), columns.toString(), values.toString());
    }

    public boolean isSupportBatchInsert() {
        return sqlExecuter.isSupportBatchInsert();
    }

    public <E> void clearCache(Class<E> entityCls) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        if (entitySqlMapping.isCacheable()) {
            entitySqlMapping.getCacheableEntityHandler().resetCaches();
        }
    }

    SqlExecuteContext contextOf(String apiName, EntitySqlMapping entitySqlMapping) {
        return SqlExecuteContext.of(apiName, entitySqlMapping.disableLog);
    }

    public <E> void clearAllCache() {
        EntityManagementFactory.defaultManagementFactory().clearAllCaches();
    }

    public void beginTransaction() {
        sqlExecuter.beginTransaction();
    }

    public void endTransaction() {
        sqlExecuter.endTransaction();
    }

    public void rollbackTransaction() {
        rollbackTransaction(true);
    }

    public void rollbackTransaction(boolean closeConnection) {
        sqlExecuter.rollbackTransaction(closeConnection);
    }

    public void commitTransaction() {
        sqlExecuter.commitTransaction();
    }

    public void commitTransaction(boolean closeConnection) {
        sqlExecuter.commitTransaction(closeConnection);
    }

    public void close() {
        sqlExecuter.close();
    }
}
