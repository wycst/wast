package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.jdbc.exception.OqlParematerException;
import io.github.wycst.wast.jdbc.query.sql.Sql;

import java.io.Serializable;
import java.util.List;

class EntityHandler {

    EntitySqlMapping entitySqlMapping;

    EntityHandler(EntitySqlMapping entitySqlMapping) {
        this.entitySqlMapping = entitySqlMapping;
    }

    /**
     * 根据主键查询实体对象
     *
     * @param sqlExecuter
     * @param entityCls
     * @param id
     * @param <E>
     * @return
     */
    <E> E getById(DefaultSqlExecuter sqlExecuter, Class<E> entityCls, Serializable id) {
        String selectSql = entitySqlMapping.getSelectSql();
        if (selectSql == null) {
            throw new OqlParematerException("configuration error: " + entityCls + " may not have a column defined @Id, please check the annotation configuration");
        }
        return sqlExecuter.queryObjectWithContext(selectSql, entityCls, entitySqlMapping.createContext("EntityExecuter#getById"), id);
    }

    /**
     * 根据id删除实体
     *
     * @param sqlExecuter
     * @param entityCls
     * @param id
     * @param <E>
     * @return
     */
    <E> int deleteById(DefaultSqlExecuter sqlExecuter, Class<E> entityCls, Serializable id) {
        String sqlStringFormat = sqlExecuter.sqlTemplates[SqlType.DELETE.ordinal()];
        Sql sqlObject = entitySqlMapping.getDeleteSqlObject(sqlStringFormat, id);
        if (sqlObject == null) {
            throw new OqlParematerException("configuration error: " + entityCls + " may not have a column defined @Id, please check the annotation configuration");
        }
        return sqlExecuter.updateWithContext(sqlObject.getFormalSql(), entitySqlMapping.createContext("EntityExecuter#deleteById"), sqlObject.getParamValues());
    }

    <E> int updateEntity(DefaultSqlExecuter sqlExecuter, E entity) {
        String sqlStringFormat = sqlExecuter.sqlTemplates[SqlType.UPDATE.ordinal()];

        Sql sqlObject = entitySqlMapping.getUpdateSqlObject(sqlStringFormat, entity);
        if (sqlObject == null) {
            throw new OqlParematerException("configuration error: " + entitySqlMapping.getEntityClass() + " may not have a column defined @Id, please check the annotation configuration");
        }
        return sqlExecuter.updateWithContext(sqlObject.getFormalSql(), entitySqlMapping.createContext("EntityExecuter#update"), sqlObject.getParamValues());
    }

    <E> void afterUpdate(DefaultSqlExecuter sqlExecuter, E entity) {
    }

    void afterBatchDelete() {
    }

    <E> List<E> executeQueryBy(DefaultSqlExecuter sqlExecuter, Class<E> entityCls, Object params) {
        Sql sqlObject = entitySqlMapping.getSelectSqlObject(params);
        return sqlExecuter.queryListWithContext(sqlObject.getFormalSql(), entityCls, entitySqlMapping.createContext("EntityExecuter#queryBy"), sqlObject.getParamValues());
    }
}
