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
            throw new OqlParematerException("配置错误：" + entityCls + "可能没有定义@Id,请检查配置");
        }
        return sqlExecuter.queryObject(selectSql, entityCls, id);
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
            throw new OqlParematerException("配置错误：" + entitySqlMapping.getEntityClass() + "可能没有定义@Id,请检查配置");
        }
        return sqlExecuter.update(sqlObject.getFormalSql(), sqlObject.getParamValues());
    }

    <E> int updateEntity(DefaultSqlExecuter sqlExecuter, E entity) {
        String sqlStringFormat = sqlExecuter.sqlTemplates[SqlType.UPDATE.ordinal()];

        Sql sqlObject = entitySqlMapping.getUpdateSqlObject(sqlStringFormat, entity);
        if (sqlObject == null) {
            throw new OqlParematerException("配置错误：" + entitySqlMapping.getEntityClass() + "可能没有定义@Id,请检查配置");
        }
        return sqlExecuter.update(sqlObject.getFormalSql(), sqlObject.getParamValues());
    }

    <E> void afterUpdate(DefaultSqlExecuter sqlExecuter, E entity) {
    }

    void afterBatchDelete() {
    }

    <E> List<E> executeQueryBy(DefaultSqlExecuter sqlExecuter, Class<E> entityCls, Object params) {
        Sql sqlObject = entitySqlMapping.getSelectSqlObject(params);
        return sqlExecuter.queryList(sqlObject.getFormalSql(), entityCls, sqlObject.getParamValues());
    }
}
