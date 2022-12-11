package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.jdbc.entity.CascadeFetchMapping;
import io.github.wycst.wast.jdbc.entity.EntitySqlMapping;
import io.github.wycst.wast.jdbc.entity.FieldColumn;
import io.github.wycst.wast.jdbc.entity.SqlType;
import io.github.wycst.wast.jdbc.exception.EntityException;
import io.github.wycst.wast.jdbc.exception.OqlParematerException;
import io.github.wycst.wast.jdbc.exception.SqlExecuteException;
import io.github.wycst.wast.jdbc.oql.OqlExecuter;
import io.github.wycst.wast.jdbc.oql.OqlQuery;
import io.github.wycst.wast.jdbc.query.page.Page;
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

    private DefaultSqlExecuter sqlExecuter;

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
        E result = getById(entitySqlMapping, entityCls, id);
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
    public <E> long queryCount(Class<E> entityCls, Map<String, Object> params) {
        return executeQueryCount(entityCls, params);
    }

    @Override
    public <E> long queryCount(Class<E> entityCls, E params) {
        return executeQueryCount(entityCls, params);
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
        String selectTemplate = entitySqlMapping.getSelectTemplate(query);
        return sqlExecuter.getTemplateExecutor().queryList(selectTemplate, params, entityCls);
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
        String selectTemplate = entitySqlMapping.getSelectTemplate(query);
        return sqlExecuter.getTemplateExecutor().queryStream(selectTemplate, params, entityCls);
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
        return sqlExecuter.queryList(selectInSql, entityCls, ids.toArray());
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
        return sqlExecuter.queryList(selectInSql, entityCls, ids);
    }

    @Override
    public <E> void queryPage(Page<E> page, OqlQuery query, Object params) {
        Class entityCls = page.actualType();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String selectTemplate = entitySqlMapping.getSelectTemplate(query);
        sqlExecuter.getTemplateExecutor().queryPage(page, selectTemplate, params);
    }

    @Override
    public <E> void queryPage(Page<E> page, Map<String, Object> params) {
        executeQueryPage(page, params);
    }

    @Override
    public <E> void queryPage(Page<E> page, E params) {
        executeQueryPage(page, params);
    }

    @Override
    public <E> Serializable insert(E entity) {
        Class entityCls = entity.getClass();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String insertTemplate = entitySqlMapping.getInsertTemplate(entity);
        return sqlExecuter.getTemplateExecutor().insert(insertTemplate, true, entity);
    }

    @Override
    public <E> void insertList(List<E> entityList) {
        if (entityList.size() == 0) {
            return;
        }
        Class entityCls = entityList.get(0).getClass();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        if (entitySqlMapping.isPrimaryCodeGenerate()) {
            // 如果主键使用代码生成需要遍历实体，为每个实体单独生成主键
            // 无法使用jdbc的批量保存api
            for (Object entity : entityList) {
                String insertTemplate = entitySqlMapping.getInsertTemplate(entity);
                sqlExecuter.getTemplateExecutor().insert(insertTemplate, false, entity);
            }
        } else {
            String insertTemplate = entitySqlMapping.getInsertTemplate();
            sqlExecuter.getTemplateExecutor().updateCollection(insertTemplate, entityList);
        }
    }

    @Override
    public <E> int mysqlBatchInsert(List<E> entityList) {
        if (!isSupportBatchInsert()) {
            throw new SqlExecuteException("当前数据库不支持批量插入");
        }
        if (entityList.size() == 0) {
            return 0;
        }
        Class entityCls = entityList.get(0).getClass();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        if (entitySqlMapping.isPrimaryCodeGenerate()) {
            for (Object entity : entityList) {
                String insertTemplate = entitySqlMapping.getInsertTemplate(entity);
                sqlExecuter.getTemplateExecutor().insert(insertTemplate, false, entity);
            }
            return 0;
        } else {
            String insertTemplate = entitySqlMapping.getInsertTemplate();
            return sqlExecuter.getTemplateExecutor().mysqlBatchInsert(insertTemplate, entityList);
        }
    }

    @Override
    public <E> int update(E entity) {
        Class entityCls = entity.getClass();
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String sqlStringFormat = getSqlStringFormat(SqlType.UPDATE);
        String updateTemplate = entitySqlMapping.getUpdateTemplate(sqlStringFormat);
        if (updateTemplate == null) {
            throw new OqlParematerException("配置错误：" + entityCls + "可能没有定义@Id,请检查配置");
        }
        return sqlExecuter.getTemplateExecutor().update(updateTemplate, entity);
    }

    @Override
    public <E> int updateFields(E e, String... fields) {
        return updateFields(e, Arrays.asList(fields), false);
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
        String sqlStringFormat = getSqlStringFormat(SqlType.UPDATE);
        String updateTemplate = entitySqlMapping.getUpdateTemplate(sqlStringFormat, fields, isExclude);
        if (updateTemplate == null) {
            throw new OqlParematerException("配置错误：" + entityCls + "可能没有定义@Id,请检查配置");
        }
        return sqlExecuter.getTemplateExecutor().update(updateTemplate, entity);
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
        String sqlStringFormat = getSqlStringFormat(SqlType.DELETE);
        String deleteTemplate = null;
        deleteTemplate = entitySqlMapping.getDeleteTemplate(sqlStringFormat);
        if (deleteTemplate == null) {
            throw new OqlParematerException("配置错误：" + entityCls + "可能没有定义@Id,请检查配置");
        }
        if (cascade) {
            // handle cascadeDelete
            this.executeCascadeDelete(entitySqlMapping, id);
        }
        return sqlExecuter.getTemplateExecutor().delete(deleteTemplate, id);
    }

    @Override
    public <E> int deleteList(List<E> entityList) {
        if (entityList == null) {
            return 0;
        }
        int influenceRows = 0;
        for (Object entity : entityList) {
            Class<?> entityCls = entity.getClass();
            checkEntityClass(entityCls);
            EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
            String sqlStringFormat = getSqlStringFormat(SqlType.DELETE);
            String deleteTemplate = null;
            deleteTemplate = entitySqlMapping.getDeleteTemplate(sqlStringFormat);
            if (deleteTemplate == null) {
                throw new OqlParematerException("配置错误：" + entityCls + "可能没有定义@Id,请检查配置");
            }
            influenceRows += sqlExecuter.getTemplateExecutor().delete(deleteTemplate, entity);
        }
        return influenceRows;
    }

    @Override
    public <E> int deleteByIds(Class<E> entityCls, List<Serializable> ids) {
        if (ids == null || ids.size() == 0) {
            return 0;
        }
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String sqlStringFormat = getSqlStringFormat(SqlType.DELETE);
        String deleteTemplate = entitySqlMapping.getDeleteTemplate(sqlStringFormat);
        if (deleteTemplate == null) {
            throw new OqlParematerException("配置错误：" + entityCls + "可能没有定义@Id,请检查配置");
        }
        sqlExecuter.getTemplateExecutor().updateCollection(deleteTemplate, ids);
        return 0;
    }

    @Override
    public <E> int deleteByIds(Class<E> cls, Serializable... ids) {
        return deleteByIds(cls, Arrays.asList(ids));
    }

    @Override
    public <E> int deleteBy(Class<E> entityCls, E params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String deleteTemplateBy = entitySqlMapping.getDeleteTemplateBy(params);
        return sqlExecuter.getTemplateExecutor().delete(deleteTemplateBy, params);
    }

    @Override
    public <E> int deleteBy(Class<E> entityCls, Map<String, Object> params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String deleteTemplateBy = entitySqlMapping.getDeleteTemplateBy(params);
        return sqlExecuter.getTemplateExecutor().delete(deleteTemplateBy, params);
    }

    @Override
    public <E> int deleteBy(Class<E> entityCls, OqlQuery query, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String selectTemplate = entitySqlMapping.getSelectTemplate(query);
        List<?> entityList = sqlExecuter.getTemplateExecutor().queryList(selectTemplate,
                params, entityCls);
        int influenceRows = 0;

        String sqlStringFormat = getSqlStringFormat(SqlType.DELETE);
        String deleteTemplateById = entitySqlMapping.getDeleteTemplate(sqlStringFormat);
        if (deleteTemplateById == null) {
            throw new OqlParematerException("配置错误：" + entityCls + "可能没有定义@Id,请检查配置");
        }
        for (Object entity : entityList) {
            // delete by id
            influenceRows += sqlExecuter.getTemplateExecutor().delete(deleteTemplateById, entity);
        }
        return influenceRows;
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
        entityCls.getClass();
        if (!EntityManagementFactory.defaultManagementFactory().existEntity(entityCls)) {
            throw new OqlParematerException("参数错误：" + entityCls + "没有纳入对象sql管理，请检查实体扫描配置");
        }
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
            Field fetchField = cascadeFetchMapping.getCascadeFetchField();
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
                // list
                fetchField.setAccessible(true);
                try {
                    fetchField.set(result, fetchFieldVal);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    Map<String, Object> getCascadeFetchParams(CascadeFetchMapping cascadeFetchMapping, EntitySqlMapping entitySqlMapping, Object result) {
        String targetFieldName = cascadeFetchMapping.getTargetFieldName();
        String fieldName = cascadeFetchMapping.getFieldName();
        FieldColumn fieldColumn = entitySqlMapping.getFieldColumnMapping().get(fieldName);
        Field fetchField = cascadeFetchMapping.getCascadeFetchField();
        if (fieldColumn == null) {
            Class<?> clazz = entitySqlMapping.getEntityClass();
            throw new EntityException(" Entity Class " + clazz + " and field[" + fetchField.getName() + "] is AnnotationPresent @CascadeFetch, but field '" + fieldName + "' is not exist");
        }
        Field field = fieldColumn.getField();
        field.setAccessible(true);
        Map<String, Object> params = null;
        try {
            Object val = field.get(result);
            if (val != null) {
                params = new HashMap<String, Object>();
                params.put(targetFieldName, val);
            }
        } catch (IllegalAccessException e) {
        }
        return params;
    }

    <E> E getById(EntitySqlMapping entitySqlMapping, Class<E> entityCls, Serializable id) {
        String selectSql = entitySqlMapping.getSelectSql();
        if (selectSql == null) {
            throw new OqlParematerException("配置错误：" + entityCls + "可能没有定义@Id,请检查配置");
        }
        return sqlExecuter.queryObject(selectSql, entityCls, id);
    }

    <E> E executeQueryOne(Class<E> entityCls, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String selectTemplate = entitySqlMapping.getSelectTemplateBy(params);
        return sqlExecuter.getTemplateExecutor().queryObject(selectTemplate, params, entityCls);
    }

    long executeQueryCount(Class<?> entityCls, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String countTemplate = entitySqlMapping.getCountTemplateBy(params);
        return sqlExecuter.getTemplateExecutor().queryValue(countTemplate, params, long.class);
    }

    <E> E executeQueryUnique(Class<E> entityCls, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String selectTemplate = entitySqlMapping.getSelectTemplateBy(params);
        return sqlExecuter.getTemplateExecutor().queryUniqueObject(selectTemplate, params, entityCls);
    }

    <E> StreamCursor<E> executeQueryStreamBy(Class<E> entityCls, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String selectTemplate = entitySqlMapping.getSelectTemplateBy(params);
        return sqlExecuter.getTemplateExecutor().queryStream(selectTemplate, params, entityCls);
    }

    <E> List<E> executeQueryBy(Class<E> entityCls, Object params) {
        checkEntityClass(entityCls);
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String selectTemplate = entitySqlMapping.getSelectTemplateBy(params);
        return sqlExecuter.getTemplateExecutor().queryList(selectTemplate, params, entityCls);
    }

    <E> void executeQueryPage(Page<E> page, Object params) {
        Class entityCls = page.actualType();
        checkEntityClass(entityCls);
        OqlQuery query = OqlQuery.create();
        query.addConditions(ObjectUtils.getNonEmptyFields(params));
        EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
        String selectTemplate = entitySqlMapping.getSelectTemplate(query);
        sqlExecuter.getTemplateExecutor().queryPage(page, selectTemplate, params);
    }

    String getSqlStringFormat(SqlType sqlType) {
        return sqlExecuter.sqlTemplates[sqlType.ordinal()];
    }

    void executeCascadeDelete(EntitySqlMapping entitySqlMapping, Serializable id) {
        List<CascadeFetchMapping> cascadeFetchMappings = entitySqlMapping.getCascadeFetchMappings();
        Class<?> clazz = entitySqlMapping.getEntityClass();
        Object result = getById(entitySqlMapping, clazz, id);
        for (CascadeFetchMapping cascadeFetchMapping : cascadeFetchMappings) {
            if (!cascadeFetchMapping.isCascade()) {
                continue;
            }
            Map<String, Object> params = getCascadeFetchParams(cascadeFetchMapping, entitySqlMapping, result);
            if (params == null) {
                return;
            }
            Class<?> entityClass = cascadeFetchMapping.getTargetEntityClass();
            // 调用deleteTemplateBy
            EntitySqlMapping targetEntitySqlMapping = getEntitySqlMapping(entityClass);
            String deleteTemplateBy = targetEntitySqlMapping.getDeleteTemplateBy(params);
            sqlExecuter.getTemplateExecutor().delete(deleteTemplateBy, params);
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
        StringBuffer columns = new StringBuffer();
        StringBuffer values = new StringBuffer();
        int index = 0;
        Map<String, FieldColumn> fieldColumnMapping = entitySqlMapping.getFieldColumnMapping();
        int columnLength = fieldColumnMapping.size();
        for (String fieldName : fieldColumnMapping.keySet()) {
            FieldColumn fieldColumn = fieldColumnMapping.get(fieldName);
            String columnName = fieldColumn.getColumnName();
            Object value = ObjectUtils.get(entity, fieldName);
            columns.append(columnName);
            if (value == null || value instanceof Number) {
                values.append(value);
            } else {
                if (value instanceof Date) {
                    value = new io.github.wycst.wast.common.beans.Date(((Date) value).getTime()).format();
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
}
