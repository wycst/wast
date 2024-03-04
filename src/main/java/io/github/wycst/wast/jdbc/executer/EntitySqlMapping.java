package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.idgenerate.providers.IdGenerator;
import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.jdbc.annotations.Id;
import io.github.wycst.wast.jdbc.annotations.Table;
import io.github.wycst.wast.jdbc.exception.EntityException;
import io.github.wycst.wast.jdbc.exception.OqlParematerException;
import io.github.wycst.wast.jdbc.query.sql.Sql;
import io.github.wycst.wast.jdbc.transform.TypeTransformer;

import java.io.Serializable;
import java.util.*;

/**
 * @Author: wangy
 * @Date: 2021/2/15 12:26
 * @Description:
 */
public class EntitySqlMapping {

    // 实体class
    private final Class<?> entityClass;
    private final Table table;
    // 表名
    private final String tableName;
    // 属性和字段关系映射
    private final Map<String, FieldColumn> fieldColumnMapping;
    // 主键
    private final FieldColumn primary;
    private final EntityHandler entityHandler;

    /**
     * 生成join语句映射配置
     * <p>
     * JoinEntityMapping (LinkHashMap)
     */
    private final Map<Class<?>, JoinEntityMapping> joinEntityMappings;

    // 级联提取配置
    private final List<CascadeFetchMapping> cascadeFetchMappings;

    // 预置查询sql模板
    private String selectByPrimarySql;
    private String selectSql;
    private String insertSql;
    private String insertSqlPrefix;
    private String insertSqlSuffix;
    private String updateByPrimarySql;
    private String deleteByPrimarySql;
    private List<FieldColumn> insertColumns = new ArrayList<FieldColumn>();
    private List<FieldColumn> updateColumns = new ArrayList<FieldColumn>();
    private Map<String, FieldColumn> placeholderColumns = new HashMap<String, FieldColumn>();
    private boolean usePlaceholderOnInsert;
    private boolean usePlaceholderOnUpdate;

    public EntitySqlMapping(Class<?> entityClass, String tableName, Map<String, FieldColumn> fieldColumnMapping,
                            FieldColumn primary, Map<Class<?>, JoinEntityMapping> joinEntityMappings, List<CascadeFetchMapping> cascadeFetchMappings, Table table) {
        this.entityClass = entityClass;
        this.tableName = tableName;
        this.fieldColumnMapping = fieldColumnMapping;
        this.primary = primary;
        this.joinEntityMappings = joinEntityMappings;
        this.cascadeFetchMappings = cascadeFetchMappings;
        this.table = table;
        this.entityHandler = table.cacheable() ? new CacheableEntityHandler(this) : new EntityHandler(this);
        // 初始化
        this.init();
    }

    /**
     * 根据field获取FieldColumn对象
     *
     * @param field
     * @return
     */
    public FieldColumn getFieldColumn(String field) {
        return fieldColumnMapping.get(field);
    }

    void init() {
        initSelectSql();
        initInsertSql();
        initUpdateSql();
        initDeleteSql();
    }

    Map<Class<?>, JoinEntityMapping> getJoinEntityMappings() {
        return joinEntityMappings;
    }

    private void appendQueryFieldColumn(FieldColumn fieldColumn, StringBuilder columnsBuilder) {
//        String fieldName = fieldColumn.getField().getName();
//        String columnName = fieldColumn.getColumnName();
//        if (fieldColumn.isEqualName()) {
//            columnsBuilder.append("t.").append(columnName).append(",");
//        } else {
//            columnsBuilder.append("t.").append(columnName).append(" as \"").append(fieldName).append("\",");
//        }
//
        columnsBuilder.append(fieldColumn.getQueryColumnSyntax());
    }

    Sql getSelectSqlObject(Object params) {
        return getSelectSqlObject(params, false);
    }

    Sql getSelectSqlObject(Object params, boolean total) {
        OqlQuery oqlQuery = OqlQuery.create();
        if (params != null) {
            oqlQuery.addConditions(getParamsFields(params));
        }
        return getSelectSqlObject(oqlQuery, params, total);
    }

    Sql getSelectSqlObject(OqlQuery oqlQuery, Object params) {
        return getSelectSqlObject(oqlQuery, params, false);
    }

    Sql getSelectSqlObject(OqlQuery oqlQuery, Object params, boolean total) {
        Sql sqlObject = new Sql();

        // paramValues(where)
        List<Object> paramValues = new ArrayList<Object>();

        String selectTemplate = "SELECT %s FROM %s t%s%s%s";
        StringBuilder columnsBuilder = new StringBuilder();
        int deleteDotIndex = -1;

        Collection<String> selectFields = oqlQuery.getSelectFields();
        boolean selectAll = selectFields.size() == 0;
        if (selectAll) {
            // 没有指定查询字段列表时查询全部字段
            selectFields = fieldColumnMapping.keySet();
        }

        for (String queryField : selectFields) {
            FieldColumn fieldColumn = fieldColumnMapping.get(queryField);
            if (fieldColumn == null) {
                continue;
            }
            if (selectAll && fieldColumn.isDisabledOnSelect()) continue;
            appendQueryFieldColumn(fieldColumn, columnsBuilder);
            deleteDotIndex = columnsBuilder.length() - 1;
        }

        StringBuilder onClause = new StringBuilder();
        boolean disableJoin = oqlQuery.isDisableJoin();
        if (!disableJoin && joinEntityMappings != null && joinEntityMappings.size() > 0) {
            onClause.append(" ");
            handleJoinSql(columnsBuilder, onClause, deleteDotIndex);
        } else {
            if (deleteDotIndex > -1) {
                columnsBuilder.deleteCharAt(deleteDotIndex);
            }
        }

        // append where conditions
        StringBuilder whereBuilder = new StringBuilder();
        appendWhereClause(whereBuilder, oqlQuery, params, paramValues);

        // order
        List<FieldOrder> orders = oqlQuery.getOrders();
        StringBuilder orderBuilder = new StringBuilder();
        boolean appendFlag = false;
        for (FieldOrder fieldOrder : orders) {
            String field = fieldOrder.getField();
            FieldColumn fieldColumn = fieldColumnMapping.get(field);
            if (fieldColumn == null) {
                // Eliminate injection vulnerabilities
                continue;
            }
            if (appendFlag) {
                orderBuilder.append(",");
            } else {
                orderBuilder.append(" ORDER BY ");
                appendFlag = true;
            }
            orderBuilder.append("t.").append(fieldColumn.getColumnName()).append(" ").append(fieldOrder.getOrder());
        }

        String sql = StringUtils.replacePlaceholder(selectTemplate, "%s", columnsBuilder.toString(), tableName, onClause.toString(),
                whereBuilder.toString(), orderBuilder.toString());

        if(total) {
            String totalSql = StringUtils.replacePlaceholder("SELECT count(*) FROM %s t %s", "%s", tableName, whereBuilder.toString());
            sqlObject.setTotalSql(totalSql);
        }

        sqlObject.setFormalSql(sql);
        sqlObject.setParamValues(paramValues.toArray());

        return sqlObject;
    }

    private void handleJoinSql(StringBuilder columnsBuilder, StringBuilder onClause, int deleteDotIndex) {
        int i = 1;
        for (Map.Entry<Class<?>, JoinEntityMapping> entry : joinEntityMappings.entrySet()) {
            JoinEntityMapping joinEntityMapping = entry.getValue();

            List<JoinColumn> joinColumns = joinEntityMapping.getJoinColumns();
            Map<String, String> joinOnColumnKeys = joinEntityMapping.getJoinOnColumnKeys();
            String tableName = joinEntityMapping.getTableName();
            String tableAlias = "t" + i;

            JoinColumn onlyOneJoinColumn;
            if (joinColumns.size() == 1 && (onlyOneJoinColumn = joinColumns.get(0)).useSubQuery()) {
                String fieldName = onlyOneJoinColumn.getFieldName();
                FieldColumn fieldColumn = onlyOneJoinColumn.getJoinFieldColumn();
                StringBuilder subQueryWhere = new StringBuilder();
                // reference on conditional syntax to build where sql
                if (joinOnColumnKeys.size() > 0) {
                    subQueryWhere.append(" WHERE ");
                    int s = 0;
                    for (Map.Entry<String, String> columnKeyEntry : joinOnColumnKeys.entrySet()) {
                        String columnName = columnKeyEntry.getKey();
                        String joinColumnName = columnKeyEntry.getValue();
                        if (s++ > 0) {
                            subQueryWhere.append(" AND ");
                        }
                        subQueryWhere.append("t.").append(columnName).append(" = ").append(tableAlias).append(".")
                                .append(joinColumnName);
                    }
                }

                fieldColumn.getQueryColumnSyntax(tableAlias);

                columnsBuilder
                        .append("(SELECT ").append(fieldColumn.getQueryColumnSyntax(tableAlias))
                        .append(" FROM ").append(tableName).append(" ").append(tableAlias).append(subQueryWhere).append(")").append(" as \"").append(fieldName).append("\",");
                deleteDotIndex = columnsBuilder.length() - 1;
            } else {
                for (JoinColumn joinColumn : joinColumns) {
                    String fieldName = joinColumn.getFieldName();
                    FieldColumn fieldColumn = joinColumn.getJoinFieldColumn();
                    columnsBuilder.append(fieldColumn.getQueryColumnSyntax(tableAlias))
                            .append(" as \"").append(fieldName).append("\",");
                    deleteDotIndex = columnsBuilder.length() - 1;
                }

                if (joinOnColumnKeys.size() > 0) {
                    onClause.append(" LEFT JOIN ").append(tableName).append(" ").append(tableAlias).append(" ON ");
                    int j = 0;
                    for (Map.Entry<String, String> columnKeyEntry : joinOnColumnKeys.entrySet()) {
                        String columnName = columnKeyEntry.getKey();
                        String joinColumnName = columnKeyEntry.getValue();
                        if (j++ > 0) {
                            onClause.append(" AND ");
                        }
                        onClause.append("t.").append(columnName).append(" = ").append(tableAlias).append(".")
                                .append(joinColumnName);
                    }
                }
            }
            i++;
        }

        if (deleteDotIndex > -1) {
            columnsBuilder.deleteCharAt(deleteDotIndex);
        }
    }

    <E> Sql getUpdateSqlObject(String updateTemplate, E entity, Collection<String> fields, boolean isExclude) {
        if (this.primary == null) {
            return null;
        }
        Sql sqlObject = new Sql();
        List<Object> values = new ArrayList<Object>();

        if (updateTemplate == null) {
            updateTemplate = "UPDATE %s SET %s WHERE %s = %s";
        }

        Collection<String> updateFields = null;
        Set<String> fullFields = fieldColumnMapping.keySet();
        if (isExclude) {
            // 全量字段更新中排除指定字段列表
            updateFields = new HashSet<String>(fullFields);
            if (fields != null) {
                updateFields.removeAll(fields);
            }
        } else {
            // 更新字段列表
            if (fields == null || fields.size() == 0) {
                // 没有指定查询字段列表时查询全部字段
                updateFields = fullFields;
            } else {
                updateFields = fields;
            }
        }

        StringBuilder columnsBuilder = new StringBuilder();
        int deleteDotIndex = -1;
        for (String updateField : updateFields) {
            if (!fieldColumnMapping.containsKey(updateField)) {
                continue;
            }
            if (updateField.equals(primary.getField().getName())) {
                continue;
            }
            FieldColumn fieldColumn = fieldColumnMapping.get(updateField);
            // String placeholder = getPlaceholder(fieldColumn);
            columnsBuilder.append(fieldColumn.getColumnName()).append(" = ?,");
            values.add(getFieldColumnValue(false, fieldColumn, entity));
            deleteDotIndex = columnsBuilder.length() - 1;
        }
        if (deleteDotIndex > -1) {
            columnsBuilder.deleteCharAt(deleteDotIndex);
        }

        values.add(getId(entity));
        String sql = StringUtils.replacePlaceholder(updateTemplate, "%s", tableName, columnsBuilder.toString(), primary.getColumnName(), "?");

        sqlObject.setFormalSql(sql);
        sqlObject.setParamValues(values.toArray());
        return sqlObject;
    }

    private void initSelectSql() {
        if (this.primary == null) {
            return;
        }
        StringBuilder columnsBuilder = new StringBuilder();
        StringBuilder onClause = new StringBuilder();
        handleSelectColumns(columnsBuilder, onClause);

        this.selectSql = StringUtils.replacePlaceholder("SELECT %s FROM %s t %s", "%s", columnsBuilder.toString(), tableName, onClause.toString()).trim();
        this.selectByPrimarySql = StringUtils.replacePlaceholder("SELECT %s FROM %s t %s WHERE t.%s = ?", "%s", columnsBuilder.toString(), tableName, onClause.toString(),
                primary.getColumnName());
    }

    void handleSelectColumns(StringBuilder columnsBuilder, StringBuilder onClause) {
        int deleteDotIndex = -1;
        for (FieldColumn fieldColumn : fieldColumnMapping.values()) {
            if (fieldColumn.isDisabledOnSelect()) {
                continue;
            }
            appendQueryFieldColumn(fieldColumn, columnsBuilder);
            deleteDotIndex = columnsBuilder.length() - 1;
        }
        if (joinEntityMappings != null && joinEntityMappings.size() > 0) {
            handleJoinSql(columnsBuilder, onClause, deleteDotIndex);
        } else {
            if (deleteDotIndex > -1) {
                columnsBuilder.deleteCharAt(deleteDotIndex);
            }
        }
    }

    private void initInsertSql() {
        this.insertColumns.clear();
        Collection<String> insertFields = fieldColumnMapping.keySet();
        String insertTemplate = "INSERT INTO %s(%s) VALUES(%s)";
        StringBuilder columnsNameBuilder = new StringBuilder();
        StringBuilder columnsValueBuilder = new StringBuilder();
        int deleteDotOfNameIndex = -1;
        int deleteDotOfValueIndex = -1;
        for (String field : insertFields) {
            FieldColumn fieldColumn = fieldColumnMapping.get(field);
            if (fieldColumn.isPrimary()) {
                Id id = fieldColumn.getId();
                Id.GenerationType type = id.strategy();
                // None,Identity, UUID, Sequence(暂时不支持), AutoAlg（雪花算法）
                if (type == Id.GenerationType.Identity) {
                    continue;
                }
            }
            if (fieldColumn.isDisabledOnInsert()) continue;

            columnsNameBuilder.append(fieldColumn.getColumnName()).append(",");

            if(fieldColumn.usePlaceholderOnWrite()) {
                this.usePlaceholderOnInsert = true;
                columnsValueBuilder.append(fieldColumn.getPlaceholderOnWriteTemplate()).append(",");
                placeholderColumns.put(field, fieldColumn);
            } else {
                this.insertColumns.add(fieldColumn);
                columnsValueBuilder.append("?,");
            }

            deleteDotOfNameIndex = columnsNameBuilder.length() - 1;
            deleteDotOfValueIndex = columnsValueBuilder.length() - 1;
        }
        if (deleteDotOfNameIndex > -1) {
            columnsNameBuilder.deleteCharAt(deleteDotOfNameIndex);
        }
        if (deleteDotOfValueIndex > -1) {
            columnsValueBuilder.deleteCharAt(deleteDotOfValueIndex);
        }

        this.insertSqlPrefix = StringUtils.replacePlaceholder("INSERT INTO %s(%s) VALUES", "%s", tableName, columnsNameBuilder.toString());
        this.insertSqlSuffix = "(" + columnsValueBuilder + ")";
        this.insertSql = StringUtils.replacePlaceholder(insertTemplate, "%s", tableName, columnsNameBuilder.toString(), columnsValueBuilder.toString());
    }

    private void initUpdateSql() {
        if (this.primary == null) {
            return;
        }
        // default
        String updateTemplate = "UPDATE %s SET %s WHERE %s = ?";
        List<FieldColumn> updateFiledColumns = new ArrayList<FieldColumn>();
        Collection<FieldColumn> fullFieldColumns = fieldColumnMapping.values();

        StringBuilder columnsBuilder = new StringBuilder();
        int deleteDotIndex = -1;
        for (FieldColumn fieldColumn : fullFieldColumns) {
            if (fieldColumn == primary) {
                continue;
            }
            if (fieldColumn.isDisabledOnUpdate()) {
                continue;
            }
            if(fieldColumn.usePlaceholderOnWrite()) {
                this.usePlaceholderOnUpdate = true;
                columnsBuilder.append(fieldColumn.getColumnName()).append(" = ").append(fieldColumn.getPlaceholderOnWriteTemplate()).append(",");
                placeholderColumns.put(fieldColumn.getField().getName(), fieldColumn);
            } else {
                columnsBuilder.append(fieldColumn.getColumnName()).append(" = ?,");
                updateFiledColumns.add(fieldColumn);
            }

            deleteDotIndex = columnsBuilder.length() - 1;
        }
        if (deleteDotIndex > -1) {
            columnsBuilder.deleteCharAt(deleteDotIndex);
        }
        // add primary last
        updateFiledColumns.add(primary);

        this.updateColumns = updateFiledColumns;
        this.updateByPrimarySql = StringUtils.replacePlaceholder(updateTemplate, "%s", tableName, columnsBuilder.toString(), primary.getColumnName());
    }

    private void initDeleteSql() {
        this.deleteByPrimarySql = getDeleteSql("DELETE FROM %s WHERE %s = ?");
        // StringUtils.replacePlaceholder("DELETE FROM %s WHERE %s = ?", "%s", tableName, primary.getColumnName());
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getTableName() {
        return tableName;
    }

    String getSelectSql() {
        return selectByPrimarySql;
    }

    Map<String, FieldColumn> getFieldColumnMapping() {
        return fieldColumnMapping;
    }

    FieldColumn getPrimary() {
        return primary;
    }

    Sql getDeleteSqlObjectByParams(String sqlTemplate, Object params) {
        if (sqlTemplate == null) {
            sqlTemplate = "DELETE FROM %s%s";
        }
        Sql sqlObject = new Sql();
        StringBuilder whereClause = new StringBuilder();
        Object[] values = null;
        // entity or map instance
        if (params != null) {
            boolean isEntityInstance = entityClass.isInstance(params);
            boolean isBeginer = true;
            Collection<String> fields = this.getParamsFields(params);
            values = new Object[fields.size()];
            int index = -1;
            for (String fieldName : fields) {
                FieldColumn fieldColumn = fieldColumnMapping.get(fieldName);
                if (fieldColumn == null) {
                    // 针对删除类似写入的操作需要严格判断字段是否存在，否则会误删数据
                    // 如果是查询可以跳过
                    throw new OqlParematerException("条件params中存在实体类中未定义的属性域： " + fieldName);
                }
                if (isBeginer) {
                    whereClause.append(" WHERE ");
                } else {
                    whereClause.append(" AND ");
                }
                whereClause.append(fieldColumn.getColumnName()).append(" = ?");
                values[++index] = isEntityInstance ? getFieldColumnValue(false, fieldColumn, params) : ((Map) params).get(fieldName);
                isBeginer = false;
            }
        }
        String sql = StringUtils.replacePlaceholder(sqlTemplate, "%s", tableName, whereClause.toString());

        sqlObject.setParamValues(values);
        sqlObject.setFormalSql(sql);
        return sqlObject;
    }

    Collection<String> getParamsFields(Object params) {
        Collection<String> fields;
        if (params instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) params;
            fields = map.keySet();
        } else {
            fields = ObjectUtils.getNonEmptyFields(params);
        }
        return fields;
    }

//    public String getCountTemplateBy(Object params) {
//        OqlQuery oqlQuery = OqlQuery.create();
//        if (params != null) {
//            oqlQuery.addConditions(getParamsFields(params));
//        }
//        String countTemplate = "SELECT count(*) FROM %s t %s ";
//        List<FieldCondition> fieldConditions = oqlQuery.getConditions();
//        StringBuilder whereBuilder = new StringBuilder();
//        appendWhereClause(whereBuilder, fieldConditions);
//        return StringUtils.replacePlaceholder(countTemplate, "%s", tableName, whereBuilder.toString().trim());
//    }

    Sql getCountSqlObject(Object params) {
        OqlQuery oqlQuery = OqlQuery.create();
        if (params != null) {
            oqlQuery.addConditions(getParamsFields(params));
        }
        return getCountSqlObject(oqlQuery, params);
//        String countTemplate = "SELECT count(*) FROM %s t %s ";
//        List<FieldCondition> fieldConditions = oqlQuery.getConditions();
//        StringBuilder whereBuilder = new StringBuilder();
//        appendWhereClause(whereBuilder, fieldConditions, params, paramValues);
//        String sql = StringUtils.replacePlaceholder(countTemplate, "%s", tableName, whereBuilder.toString().trim());
//
//        sqlObject.setFormalSql(sql);
//        sqlObject.setParamValues(paramValues.toArray());
//        return sqlObject;
    }

    Sql getCountSqlObject(OqlQuery oqlQuery, Object params) {
        Sql sqlObject = new Sql();
        List<Object> paramValues = new ArrayList<Object>();
        String countTemplate = "SELECT count(*) FROM %s t %s ";
        StringBuilder whereBuilder = new StringBuilder();
        appendWhereClause(whereBuilder, oqlQuery, params, paramValues);
        String sql = StringUtils.replacePlaceholder(countTemplate, "%s", tableName, whereBuilder.toString().trim());

        sqlObject.setFormalSql(sql);
        sqlObject.setParamValues(paramValues.toArray());
        return sqlObject;
    }

    Sql getPageSqlObject(OqlQuery query, Object params) {
        Sql sql = getSelectSqlObject(query, params);
        return sql;
    }

//    private void appendWhereClause(StringBuilder whereBuilder, List<FieldCondition> fieldConditions) {
//        boolean appendFlag = false;
//        for (FieldCondition fieldCondition : fieldConditions) {
//            String field = fieldCondition.getField();
//            if (!fieldColumnMapping.containsKey(field)) {
//                continue;
//            }
//            FieldColumn fieldColumn = fieldColumnMapping.get(field);
//            if (appendFlag) {
//                whereBuilder.append(" AND ");
//            } else {
//                whereBuilder.append(" WHERE ");
//                appendFlag = true;
//            }
//            whereBuilder.append("t.").append(fieldColumn.getColumnName())
//                    .append(" ").append(fieldCondition.getOperator()).append(" ").append(fieldCondition.getValue());
//        }
//    }

    private void appendWhereClause(StringBuilder whereBuilder, OqlQuery oqlQuery, Object params, List<Object> paramValues) {
        boolean appendFlag = false;
        boolean isEntityInstance = entityClass.isInstance(params);
        List<FieldCondition> conditions = oqlQuery.getConditions();
        for (FieldCondition fieldCondition : conditions) {
            String field = fieldCondition.getField();
            FieldColumn fieldColumn = fieldColumnMapping.get(field);
            if (fieldColumn == null) {
                continue;
            }
            if (appendFlag) {
                whereBuilder.append(oqlQuery.getLogicTypeSpace());
            } else {
                whereBuilder.append(" WHERE ");
                appendFlag = true;
            }

            fieldCondition.appendWhereField(whereBuilder, fieldColumn);

            Object conditionValue = fieldCondition.getValue();
            if (conditionValue == null) {
                conditionValue = isEntityInstance ? getFieldColumnValue(false, fieldColumn, params) : ObjectUtils.getAttrValue(params, field);
            }
            if (fieldCondition.isLike()) {
                // Note the need to replace the characters in the content "'" to prevent injection vulnerabilities and incorrect syntax
                whereBuilder.append(fieldCondition.likeValueLeft()).append(getLikeConditionValue(conditionValue)).append(fieldCondition.likeValueRight());
            } else {
                fieldCondition.appendWhereValue(whereBuilder, paramValues, conditionValue);
            }
        }

        List<SubQueryCondition> subQueryConditions = oqlQuery.getSubQueryConditions();
        StringBuilder subBuilder = new StringBuilder();
        for (SubQueryCondition subQueryCondition : subQueryConditions) {

            String field = subQueryCondition.getField();
            String symbol = subQueryCondition.getSymbol();
            subBuilder.setLength(0);
            if(field != null) {
                FieldColumn fieldColumn = fieldColumnMapping.get(field);
                if (fieldColumn == null) {
                    continue;
                }
                subBuilder.append("t.").append(fieldColumn.getColumnName()).append(" ").append(symbol);
            } else {
                subBuilder.append(symbol);
            }

            if (appendFlag) {
                whereBuilder.append(oqlQuery.getLogicTypeSpace());
            } else {
                whereBuilder.append(" WHERE ");
                appendFlag = true;
            }

            // append left
            whereBuilder.append(subBuilder).append(" ");

            // append in or exists
            SubOqlQuery subOqlQuery = subQueryCondition.getSubOqlQuery();

            whereBuilder.append("(");

            EntitySqlMapping subEntitySqlMapping = subOqlQuery.getSubEntitySqlMapping();
            Sql subSql = subEntitySqlMapping.getSelectSqlObject(subOqlQuery, params);
            whereBuilder.append(subSql.getFormalSql());
            paramValues.addAll(Arrays.asList(subSql.getParamValues()));
            whereBuilder.append(")");
        }
    }

    /**
     * Resolve like condition value
     *
     * @param conditionValue
     * @return
     */
    private static String getLikeConditionValue(Object conditionValue) {
        if (conditionValue == null) return "";
        return getPlaceholderValue(conditionValue);
    }

    /**
     * Resolve SQL syntax issues while addressing potential injection vulnerabilities in SQL
     *
     * @param value
     * @return
     */
    private static String getPlaceholderValue(Object value) {
        String message = value.toString();
        int placeholderIndex;
        if ((placeholderIndex = message.indexOf('\'')) == -1) {
            return message;
        }
        StringBuilder builder = new StringBuilder();
        int fromIndex = 0;
        while (placeholderIndex > -1) {
            builder.append(message, fromIndex, placeholderIndex);
            builder.append("\\'");
            fromIndex = placeholderIndex + 1;
            placeholderIndex = message.indexOf('\'', fromIndex);
        }
        if (fromIndex < message.length()) {
            builder.append(message, fromIndex, message.length());
        }

        return builder.toString();
    }


    String getSelectSqlByIds(Collection ids) {
        StringBuilder baseSelectSql = new StringBuilder(selectSql);
        baseSelectSql.append(" WHERE t.").append(primary.getColumnName()).append(" in (");
        int len = ids.size();
        while (len-- > 0) {
            baseSelectSql.append("?");
            if (len > 0) {
                baseSelectSql.append(",");
            }
        }
        baseSelectSql.append(")");
        return baseSelectSql.toString();
    }

    /**
     * 获取主键id
     *
     * @param entity
     * @return
     */
    <E> Serializable getId(E entity) {
        if (entity == null || !entityClass.isInstance(entity)) {
            throw new EntityException("entity is null or not an instance of " + entityClass);
        }
        try {
            return (Serializable) primary.getFieldValue(entity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    List<CascadeFetchMapping> getCascadeFetchMappings() {
        return cascadeFetchMappings;
    }

    public boolean isCacheable() {
        return table.cacheable();
    }

    public long expires() {
        return table.expires();
    }

    EntityHandler getEntityHandler() {
        return this.entityHandler;
    }

    CacheableEntityHandler getCacheableEntityHandler() {
        return (CacheableEntityHandler) this.entityHandler;
    }

    <E> Sql getUpdateSqlObject(String sqlStringFormat, E entity) {
        if (primary == null) return null;
        Sql sql = new Sql();
        sql.setFormalSql(sqlStringFormat == null ? getUpdateByPrimarySql(entity) : getUpdateSql(sqlStringFormat));
        sql.setParamValues(getUpdateValues(entity));
        return sql;
    }

    <E> String getUpdateByPrimarySql(E entity) {
        if(this.usePlaceholderOnUpdate) {
            return renderPlaceholder(updateByPrimarySql, entity);
        } else {
            return updateByPrimarySql;
        }
    }

    Sql getUpdateSqlObject(String sqlStringFormat, OqlQuery oqlQuery, Object params, String[] fields) {

        Sql sqlObject = new Sql();
        List<Object> paramValues = new ArrayList<Object>();

        if(sqlStringFormat == null) {
            // default: UPDATE %s SET %s%s
            // (clickhouse : ALTER TABLE %s UPDATE %s %s)
            sqlStringFormat = "UPDATE %s t SET %s%s";
        }

        StringBuilder updateColumns = new StringBuilder();
        int deleteDotIndex = -1;
        boolean isEntityInstance = entityClass.isInstance(params);
        for (String field : fields) {
            FieldColumn fieldColumn = getFieldColumn(field);
            if (fieldColumn == null) {
                continue;
            }
            Object value = isEntityInstance ? getFieldColumnValue(false, fieldColumn, params) : ObjectUtils.getAttrValue(params, field);
            if(fieldColumn.usePlaceholderOnWrite()) {
                // append placeholder
                String placeholderTemplate = fieldColumn.getPlaceholderOnWriteTemplate();
                Map<String, Object> vars = new HashMap<String, Object>();
                vars.put(field, value == null || Number.class.isInstance(value) ? String.valueOf(value) : "'" + getPlaceholderValue(value) + "'");
                updateColumns.append(fieldColumn.getColumnName()).append(" = ").append(Expression.renderTemplate(placeholderTemplate, vars)).append(",");
            } else {
                updateColumns.append(fieldColumn.getColumnName()).append(" = ?,");
                paramValues.add(value);
            }
            deleteDotIndex = updateColumns.length() - 1;
        }
        if (deleteDotIndex > -1) {
            updateColumns.deleteCharAt(deleteDotIndex);
        }

        StringBuilder whereBuilder = new StringBuilder();
        appendWhereClause(whereBuilder, oqlQuery, params, paramValues);
        String sql = StringUtils.replacePlaceholder(sqlStringFormat, "%s", tableName, updateColumns.toString(), whereBuilder.toString());

        sqlObject.setFormalSql(sql);
        sqlObject.setParamValues(paramValues.toArray());
        return sqlObject;
    }

    Sql getDeleteSqlObject(String sqlStringFormat, Serializable id) {
        if (primary == null) return null;
        Sql sql = new Sql();
        sql.setFormalSql(sqlStringFormat == null ? deleteByPrimarySql : getDeleteSql(sqlStringFormat));
        sql.setParamValues(new Object[]{id});
        return sql;
    }

    // default: UPDATE %s SET %s WHERE %s = ?
    // if clickhouse: ALTER TABLE %s UPDATE %s WHERE %s = %s
    String getUpdateSql(String updateTemplate) {
        StringBuilder columnsBuilder = new StringBuilder();
        int deleteDotIndex = -1;
        for (FieldColumn fieldColumn : updateColumns) {
            if (fieldColumn == primary) {
                continue;
            }
            columnsBuilder.append(fieldColumn.getColumnName()).append(" = ?,");
            deleteDotIndex = columnsBuilder.length() - 1;
        }
        if (deleteDotIndex > -1) {
            columnsBuilder.deleteCharAt(deleteDotIndex);
        }
        return StringUtils.replacePlaceholder(updateTemplate, "%s", tableName, columnsBuilder.toString(), primary.getColumnName(), "?");
    }

    String getDeleteSql(String deleteTemplate) {
        if (primary == null) return null;
        return deleteTemplate == null ? deleteByPrimarySql : StringUtils.replacePlaceholder(deleteTemplate, "%s", tableName, primary.getColumnName());
    }

    <E> Sql getInsertSqlObject(E entity) {
        Sql sql = new Sql();
        sql.setFormalSql(getInsertSql(entity));
        sql.setParamValues(getInsertValues(entity));
        return sql;
    }

    <E> String getInsertSql(E entity) {
        return this.usePlaceholderOnInsert ? renderPlaceholder(insertSql, entity) : insertSql;
    }

    /**
     * not support placeholder
     *
     * @param entityList
     * @param <E>
     * @return
     */
    <E> Sql getInsertSqlObjectList(List<E> entityList) {
        Sql sql = new Sql();
        sql.setFormalSql(insertSql);
        List<Object[]> paramValuesList = new ArrayList<Object[]>(entityList.size());
        for (E entity : entityList) {
            paramValuesList.add(getInsertValues(entity));
        }
        sql.setParamValuesList(paramValuesList);
        return sql;
    }

    <E> Sql getBatchInsertSqlObject(List<E> entityList) {
        Sql sqlObject = new Sql();
        // INSERT INTO %s(%s) VALUES
        StringBuilder builder = new StringBuilder(insertSqlPrefix);
        Object[] paramValues = new Object[entityList.size() * insertColumns.size()];
        int index = -1;
        StringBuilder columnsValueBuilder = new StringBuilder();
        int deleteDotOfValueIndex = -1;
        for (E entity : entityList) {
            // values
            for (FieldColumn fieldColumn : insertColumns) {
                Object value = getFieldColumnValue(fieldColumn.isPrimary(), fieldColumn, entity);
                paramValues[++index] = value;
            }
            // (),
            if(this.usePlaceholderOnInsert) {
                columnsValueBuilder.append(renderPlaceholder(insertSqlSuffix, entity)).append(",");
            } else {
                columnsValueBuilder.append(insertSqlSuffix).append(",");
            }
            deleteDotOfValueIndex = columnsValueBuilder.length() - 1;
        }
        if (deleteDotOfValueIndex > -1) {
            columnsValueBuilder.deleteCharAt(deleteDotOfValueIndex);
        }
        builder.append(columnsValueBuilder);

        sqlObject.setFormalSql(builder.toString());
        sqlObject.setParamValues(paramValues);
        return sqlObject;
    }

    /**
     * 解析占位信息
     *
     * @param template
     * @param entity
     * @return
     */
    <E> String renderPlaceholder(String template, E entity) {
        Map<String, Object> vars = new HashMap<String, Object>();
        Set<Map.Entry<String, FieldColumn>> entrySet = placeholderColumns.entrySet();
        for (Map.Entry<String, FieldColumn> entry : entrySet) {
            String field = entry.getKey();
            FieldColumn fieldColumn = entry.getValue();
            Object fieldValue = getFieldColumnValue(false, fieldColumn, entity);
            vars.put(field, fieldValue == null || Number.class.isInstance(fieldValue) ? String.valueOf(fieldValue) : "'" + getPlaceholderValue(fieldValue) + "'");
        }
        return Expression.renderTemplate(template, vars);
    }

    <E> Object[] getInsertValues(E entity) {
        return getFieldValues(entity, insertColumns, true);
    }

    <E> Object[] getUpdateValues(E entity) {
        return getFieldValues(entity, updateColumns, false);
    }

    <E> Object[] getFieldValues(E entity, List<FieldColumn> fieldColumns, boolean insertMode) {
        Object[] values = new Object[fieldColumns.size()];
        int index = -1;
        for (FieldColumn fieldColumn : fieldColumns) {
            Object value = getFieldColumnValue(insertMode && fieldColumn.isPrimary(), fieldColumn, entity);
            values[++index] = value;
        }
        return values;
    }

    <E> Object getFieldColumnValue(boolean primaryFlag, FieldColumn fieldColumn, E entity) {
        Object value;
        if (primaryFlag) {
            // 插入模式主键需要根据策略自动生成
            Id id = fieldColumn.getId();
            Id.GenerationType type = id.strategy();
            if (type == Id.GenerationType.UUID) {
                value = UUID.randomUUID().toString();
            } else if (type == Id.GenerationType.AutoAlg) {
                Class<?> primaryType = fieldColumn.getField().getType();
                if (primaryType == String.class) {
                    value = IdGenerator.hex();
                } else {
                    value = IdGenerator.id();
                }
            } else {
                value = invokeFieldColumnValue(fieldColumn, entity);
            }
            fieldColumn.setFieldValue(entity, value);
            value = invokeFieldColumnValue(fieldColumn, entity);
        } else {
            value = invokeFieldColumnValue(fieldColumn, entity);
        }
        return value;
    }

    <E> Object invokeFieldColumnValue(FieldColumn fieldColumn, E entity) {
        Object fieldValue = fieldColumn.getFieldValue(entity);
        TypeTransformer typeTransformer = fieldColumn.getTypeTransformer();
        if (typeTransformer != null) {
            return typeTransformer.fromJavaField(fieldValue);
        }
        return fieldValue;
    }

    public boolean existField(String field) {
        return fieldColumnMapping.containsKey(field);
    }

    boolean existPrimary() {
        return primary != null;
    }

    boolean isUsePlaceholderOnInsert() {
        return usePlaceholderOnInsert;
    }
}
