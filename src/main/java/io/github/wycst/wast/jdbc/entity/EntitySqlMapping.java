package io.github.wycst.wast.jdbc.entity;

import io.github.wycst.wast.common.idgenerate.providers.IdGenerator;
import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.jdbc.annotations.Column;
import io.github.wycst.wast.jdbc.annotations.Id;
import io.github.wycst.wast.jdbc.exception.OqlParematerException;
import io.github.wycst.wast.jdbc.oql.FieldCondition;
import io.github.wycst.wast.jdbc.oql.FieldOrder;
import io.github.wycst.wast.jdbc.oql.OqlQuery;

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
    // 表名
    private final String tableName;
    // 属性和字段关系映射
    private final Map<String, FieldColumn> fieldColumnMapping;
    // 主键
    private final FieldColumn primary;
    // 主键生成策略
    private Id.GenerationType generationType;

    /**
     * 生成join语句映射配置
     * <p>
     * JoinEntityMapping (LinkHashMap)
     */
    private final Map<Class<?>, JoinEntityMapping> joinEntityMappings;

    // 级联提取配置
    private final List<CascadeFetchMapping> cascadeFetchMappings;

    public EntitySqlMapping(Class<?> entityClass, String tableName, Map<String, FieldColumn> fieldColumnMapping,
                            FieldColumn primary, Map<Class<?>, JoinEntityMapping> joinEntityMappings, List<CascadeFetchMapping> cascadeFetchMappings) {
        this.entityClass = entityClass;
        this.tableName = tableName;
        this.fieldColumnMapping = fieldColumnMapping;
        this.primary = primary;
        this.joinEntityMappings = joinEntityMappings;
        this.cascadeFetchMappings = cascadeFetchMappings;
        // 初始化
        this.init();
    }

    public Map<Class<?>, JoinEntityMapping> getJoinEntityMappings() {
        return joinEntityMappings;
    }

    // 预置查询sql模板
    private String selectSql;
    // 预置插入sql模板
    private String insertTemplate;
    // 预置删除sql模板
    private String deleteTemplate;
    // 预置更新sql模板
    private String updateTemplate;

    private String initDeleteTemplate() {
        return getDeleteTemplate("DELETE FROM %s WHERE %s = #{%s}");
    }

    String getSelectSql(Collection<String> queryFields) {
        return getSelectTemplate("SELECT %s FROM %s t %s WHERE t.%s = ?", queryFields);
    }

    public String getSelectTemplate(Collection<String> queryFields) {
        return getSelectTemplate("SELECT %s FROM %s t %s WHERE t.%s = #{%s}", queryFields);
    }

    private void appendQueryFieldColumn(String queryField, StringBuilder columnsBuffer) {
        FieldColumn fieldColumn = fieldColumnMapping.get(queryField);
        String columnName = fieldColumn.getColumnName();
        if (columnName.equals(queryField)) {
            columnsBuffer.append("t.").append(columnName).append(",");
        } else {
            // columnsBuffer.append("t.").append(columnName).append(" as \"" + queryField + "\"").append(",");
            columnsBuffer.append("t.").append(columnName).append(" as \"").append(queryField).append("\",");
            ;
        }
    }

    /**
     * 根据主键生成查询sql模板
     *
     * @param formatTemplate
     * @param queryFields
     * @return
     */
    public String getSelectTemplate(String formatTemplate, Collection<String> queryFields) {
        if (this.primary == null) {
            return null;
        }
        StringBuilder columnsBuffer = new StringBuilder();
        int deleteDotIndex = -1;
        for (String queryField : queryFields) {
            if (!fieldColumnMapping.containsKey(queryField)) {
                continue;
            }
            appendQueryFieldColumn(queryField, columnsBuffer);
            deleteDotIndex = columnsBuffer.length() - 1;
        }

        StringBuilder onClause = new StringBuilder();

        if (joinEntityMappings != null && joinEntityMappings.size() > 0) {
            int i = 1;
            for (Map.Entry<Class<?>, JoinEntityMapping> entry : joinEntityMappings.entrySet()) {
                JoinEntityMapping joinEntityMapping = entry.getValue();

                List<JoinColumn> joinColumns = joinEntityMapping.getJoinColumns();
                String tableName = joinEntityMapping.getTableName();
                String tableAlias = "t" + i;
                for (JoinColumn joinColumn : joinColumns) {
                    String fieldName = joinColumn.getFieldName();
                    FieldColumn fieldColumn = joinColumn.getJoinFieldColumn();
                    columnsBuffer.append(tableAlias).append(".").append(fieldColumn.getColumnName())
                            .append(" as \"").append(fieldName).append("\",");
                    deleteDotIndex = columnsBuffer.length() - 1;
                }

                Map<String, String> joinOnColumnKeys = joinEntityMapping.getJoinOnColumnKeys();
                if (joinOnColumnKeys.size() > 0) {
                    onClause.append(" left join ").append(tableName).append(" ").append(tableAlias).append(" on ");
                    int j = 0;
                    for (Map.Entry<String, String> columnKeyEntry : joinOnColumnKeys.entrySet()) {
                        String columnName = columnKeyEntry.getKey();
                        String joinColumnName = columnKeyEntry.getValue();
                        if (j++ > 0) {
                            onClause.append(" and ");
                        }
                        onClause.append("t.").append(columnName).append(" = ").append(tableAlias).append(".")
                                .append(joinColumnName);
                    }
                }
                i++;
            }
        }

        if (deleteDotIndex > -1) {
            columnsBuffer.deleteCharAt(deleteDotIndex);
        }

        return StringUtils.replacePlaceholder(formatTemplate, "%s", columnsBuffer.toString(), tableName, onClause.toString(),
                primary.getColumnName(), primary.getField().getName());
    }

    /**
     * 定制查询选项
     *
     * @param selectTemplate
     * @param oqlQuery
     * @return
     */
    public String getSelectTemplate(String selectTemplate, OqlQuery oqlQuery) {

        StringBuilder columnsBuffer = new StringBuilder();
        int deleteDotIndex = -1;

        boolean disableJoin = oqlQuery.isDisableJoin();
        Collection<String> selectFields = oqlQuery.getSelectFields();
        List<FieldCondition> fieldConditions = oqlQuery.getConditions();
        if (selectFields.size() == 0) {
            // 没有指定查询字段列表时查询全部字段
            selectFields = fieldColumnMapping.keySet();
        } else {
//			// 指定了查询列
//			disableJoin = true;
        }

        for (String queryField : selectFields) {
            if (!fieldColumnMapping.containsKey(queryField)) {
                continue;
            }
            appendQueryFieldColumn(queryField, columnsBuffer);
            deleteDotIndex = columnsBuffer.length() - 1;
        }

        StringBuilder onClause = new StringBuilder();

        if (!disableJoin && joinEntityMappings != null && joinEntityMappings.size() > 0) {
            int i = 1;
            for (Map.Entry<Class<?>, JoinEntityMapping> entry : joinEntityMappings.entrySet()) {
                JoinEntityMapping joinEntityMapping = entry.getValue();
                List<JoinColumn> joinColumns = joinEntityMapping.getJoinColumns();
                String tableName = joinEntityMapping.getTableName();
                String tableAlias = "t" + i;
                for (JoinColumn joinColumn : joinColumns) {
                    String fieldName = joinColumn.getFieldName();
                    FieldColumn fieldColumn = joinColumn.getJoinFieldColumn();
                    columnsBuffer.append(tableAlias).append(".").append(fieldColumn.getColumnName())
                            .append(" as \"").append(fieldName).append("\",");
                    deleteDotIndex = columnsBuffer.length() - 1;
                }

                Map<String, String> joinOnColumnKeys = joinEntityMapping.getJoinOnColumnKeys();
                if (joinOnColumnKeys.size() > 0) {
                    onClause.append(" left join ").append(tableName).append(" ").append(tableAlias).append(" on ");
                    int j = 0;
                    for (Map.Entry<String, String> columnKeyEntry : joinOnColumnKeys.entrySet()) {
                        String columnName = columnKeyEntry.getKey();
                        String joinColumnName = columnKeyEntry.getValue();
                        if (j++ > 0) {
                            onClause.append(" and ");
                        }
                        onClause.append("t.").append(columnName).append(" = ").append(tableAlias).append(".")
                                .append(joinColumnName);
                    }
                }
                i++;
            }
        }

        if (deleteDotIndex > -1) {
            columnsBuffer.deleteCharAt(deleteDotIndex);
        }

        // 拼接where条件
        StringBuilder whereBuffer = new StringBuilder();
        appendWhereClause(whereBuffer, fieldConditions);

        // 排序
        List<FieldOrder> orders = oqlQuery.getOrders();
        StringBuilder orderBuffer = new StringBuilder();
        boolean appendFlag = false;
        for (FieldOrder fieldOrder : orders) {
            String field = fieldOrder.getField();
            if (!fieldColumnMapping.containsKey(field)) {
                continue;
            }
            FieldColumn fieldColumn = fieldColumnMapping.get(field);
            if (appendFlag) {
                orderBuffer.append(",");
            } else {
                orderBuffer.append(" ORDER BY ");
                appendFlag = true;
            }
            orderBuffer.append("t.").append(fieldColumn.getColumnName()).append(" ").append(fieldOrder.getOrder());
        }

        return StringUtils.replacePlaceholder(selectTemplate, "%s", columnsBuffer.toString(), tableName, onClause.toString(),
                whereBuffer.toString(), orderBuffer.toString());
    }

    public String getSelectTemplate(OqlQuery oqlQuery) {
        if (oqlQuery == null) {
            oqlQuery = OqlQuery.create();
        }
        String selectTemplate = "SELECT %s FROM %s t %s %s %s";
        return getSelectTemplate(selectTemplate, oqlQuery);
    }

    public String getUpdateTemplate(String updateTemplate, Collection<String> updateFields) {
        return getUpdateTemplate(updateTemplate, updateFields, false);
    }

    // default format "UPDATE %s SET %s WHERE %s = #{%s}";
    public String getUpdateTemplate(String updateTemplate, Collection<String> fields, boolean isExclude) {
        if (this.primary == null) {
            return null;
        }

        if (updateTemplate == null) {
            updateTemplate = "UPDATE %s SET %s WHERE %s = #{%s}";
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

        StringBuilder columnsBuffer = new StringBuilder();
        int deleteDotIndex = -1;
        for (String updateField : updateFields) {
            if (!fieldColumnMapping.containsKey(updateField)) {
                continue;
            }
            if (updateField.equals(primary.getField().getName())) {
                continue;
            }
            FieldColumn fieldColumn = fieldColumnMapping.get(updateField);
            String placeholder = getPlaceholder(fieldColumn);
//			placeholder = getPlaceholder(fieldColumn);
//			columnsBuffer.append(fieldColumn.getColumnName()).append(" = #{").append(fieldColumn.getField().getName())
//					.append("},");
            columnsBuffer.append(fieldColumn.getColumnName()).append(" = ").append(placeholder).append(",");

            deleteDotIndex = columnsBuffer.length() - 1;
        }
        if (deleteDotIndex > -1) {
            columnsBuffer.deleteCharAt(deleteDotIndex);
        }
        return StringUtils.replacePlaceholder(updateTemplate, "%s", tableName, columnsBuffer.toString(), primary.getColumnName(),
                primary.getField().getName());
    }

    private String getPlaceholder(FieldColumn fieldColumn) {
        Column column = fieldColumn.getColumn();
        String placeholder = "#{" + fieldColumn.getField().getName() + "}";
        if (column != null && column.placeholder().trim().length() > 0) {
            String ph = column.placeholder().trim();
            // 替换?为placeholder
            placeholder = ph.replace("?", placeholder);
        }
        return placeholder;
    }

    public String getUpdateTemplate(Collection<String> updateFields) {
        String updateTemplate = "UPDATE %s SET %s WHERE %s = #{%s}";
        return getUpdateTemplate(updateTemplate, updateFields);
    }

    public String getInsertTemplate(Collection<String> insertFields) {
        String insertTemplate = "INSERT INTO %s(%s) VALUES(%s)";
        StringBuilder columnsNameBuffer = new StringBuilder();
        StringBuilder columnsValueBuffer = new StringBuilder();
        int deleteDotOfNameIndex = -1;
        int deleteDotOfValueIndex = -1;
        for (String queryField : insertFields) {
            if (!fieldColumnMapping.containsKey(queryField)) {
                continue;
            }
            FieldColumn fieldColumn = fieldColumnMapping.get(queryField);
            if (fieldColumn.isPrimary()) {
                Id id = fieldColumn.getId();
                Id.GenerationType type = id.strategy();
                // None,Identity, UUID, Sequence(暂时不支持), AutoAlg（雪花算法）
                this.generationType = type;
                if (type == Id.GenerationType.Identity) {
                    // 如果自增跳过
                    continue;
                } else if (type == Id.GenerationType.UUID) {
                    columnsNameBuffer.append(fieldColumn.getColumnName()).append(",");
                    columnsValueBuffer.append("'%s',");
                    // "+UUID.randomUUID().toString()+"
                } else if (type == Id.GenerationType.AutoAlg) {
                    columnsNameBuffer.append(fieldColumn.getColumnName()).append(",");
                    Class<?> primaryType = fieldColumn.getField().getType();
                    if (primaryType == String.class) {
                        columnsValueBuffer.append("'%s',");
                        // IdGenerator.getInstance().generateHexString()
                    } else {
                        columnsValueBuffer.append("%d,");
                        // IdGenerator.getInstance().generateId()+
                    }
                } else {
                    columnsNameBuffer.append(fieldColumn.getColumnName()).append(",");
                    columnsValueBuffer.append("#{").append(fieldColumn.getField().getName()).append("},");
                }
            } else {
                columnsNameBuffer.append(fieldColumn.getColumnName()).append(",");
                columnsValueBuffer.append("#{").append(fieldColumn.getField().getName()).append("},");
            }

            deleteDotOfNameIndex = columnsNameBuffer.length() - 1;
            deleteDotOfValueIndex = columnsValueBuffer.length() - 1;
        }
        if (deleteDotOfNameIndex > -1) {
            columnsNameBuffer.deleteCharAt(deleteDotOfNameIndex);
        }
        if (deleteDotOfValueIndex > -1) {
            columnsValueBuffer.deleteCharAt(deleteDotOfValueIndex);
        }
        return StringUtils.replacePlaceholder(insertTemplate, "%s", tableName, columnsNameBuffer.toString(), columnsValueBuffer.toString());
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getTableName() {
        return tableName;
    }

    public String getSelectSql() {
        return selectSql;
    }

    public boolean isPrimaryCodeGenerate() {
        return this.generationType == Id.GenerationType.UUID || this.generationType == Id.GenerationType.AutoAlg;
    }

    public String getInsertTemplate() {
        if (this.generationType == Id.GenerationType.UUID) {
            return StringUtils.replacePlaceholder(insertTemplate, "%s", UUID.randomUUID().toString());
        } else if (this.generationType == Id.GenerationType.AutoAlg) {
            Class<?> idType = primary.getField().getType();
            if (idType == String.class) {
                return StringUtils.replacePlaceholder(insertTemplate, "%s", IdGenerator.getInstance().generateHexString());
            } else {
                return StringUtils.replacePlaceholder(insertTemplate, "%s", IdGenerator.getInstance().generateId());
            }
        }
        // 静态插入sql
        return insertTemplate;
    }

    public String getInsertTemplate(Object entity) {
        if (isPrimaryCodeGenerate()) {
            Serializable pk = null;
            String sql = null;
            if (this.generationType == Id.GenerationType.UUID) {
                pk = UUID.randomUUID().toString();
            } else if (this.generationType == Id.GenerationType.AutoAlg) {
                Class<?> idType = primary.getField().getType();
                if (idType == String.class) {
                    pk = IdGenerator.getInstance().generateHexString();
                } else {
                    pk = IdGenerator.getInstance().generateId();
                }
            }
            // 给entity设置pk
            if (entity != null) {
                primary.getField().setAccessible(true);
                try {
                    primary.getField().set(entity, pk);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return StringUtils.replacePlaceholder(insertTemplate, "%s", pk);
        } else {
            return insertTemplate;
        }
    }

    public String getDeleteTemplate() {
        return deleteTemplate;
    }

    public String getUpdateTemplate() {
        return updateTemplate;
    }

    public String getUpdateTemplate(String sqlStringFormat) {
        if (sqlStringFormat == null) {
            return this.updateTemplate;
        }
        return getUpdateTemplate(sqlStringFormat, fieldColumnMapping.keySet());
    }

    public Map<String, FieldColumn> getFieldColumnMapping() {
        return fieldColumnMapping;
    }

    public FieldColumn getPrimary() {
        return primary;
    }

    public String getDeleteTemplate(String formatTemplate) {
        if (this.primary == null) {
            return null;
        }
        if (formatTemplate == null) {
            return deleteTemplate;
        }
        return StringUtils.replacePlaceholder(formatTemplate, "%s", tableName, primary.getColumnName(), primary.getField().getName());
    }

    public void init() {
        Collection<String> fields = fieldColumnMapping.keySet();
        this.selectSql = getSelectSql(fields);
        this.updateTemplate = getUpdateTemplate(fields);
        this.insertTemplate = getInsertTemplate(fields);
        this.deleteTemplate = initDeleteTemplate();
    }

    public String getDeleteTemplateBy(Object params) {
        String deleteSql = "DELETE FROM %s%s";
        StringBuilder whereClause = new StringBuilder();
        if (params != null) {

            boolean isBeginer = true;
            Collection<String> fields = this.getParamsFields(params);

            for (String fieldName : fields) {
                if (!fieldColumnMapping.containsKey(fieldName)) {
                    // 针对删除类似写入的操作需要严格判断字段是否存在，否则会误删数据
                    // 如果是查询可以跳过
                    throw new OqlParematerException("条件params中存在实体类中未定义的属性域： " + fieldName);
                }
                if (isBeginer) {
                    whereClause.append(" WHERE ");
                } else {
                    whereClause.append(" AND ");
                }
                FieldColumn fieldColumn = fieldColumnMapping.get(fieldName);
                whereClause.append(fieldColumn.getColumnName()).append(" = ");
                whereClause.append("#{").append(fieldName).append("}");
                isBeginer = false;
            }
        }
        return StringUtils.replacePlaceholder(deleteSql, "%s", tableName, whereClause.toString());
    }

    private Collection<String> getParamsFields(Object params) {
        Collection<String> fields = null;
        if (params instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) params;
            fields = map.keySet();
        } else {
            fields = ObjectUtils.getNonEmptyFields(params);
        }
        return fields;
    }

    public String getSelectTemplateBy(Object params) {
        OqlQuery oqlQuery = OqlQuery.create();
        if (params != null) {
            oqlQuery.addConditions(getParamsFields(params));
        }
        return getSelectTemplate(oqlQuery);
    }

    public String getCountTemplateBy(Object params) {
        OqlQuery oqlQuery = OqlQuery.create();
        if (params != null) {
            oqlQuery.addConditions(getParamsFields(params));
        }
        String countTemplate = "SELECT count(*) FROM %s t %s ";
        List<FieldCondition> fieldConditions = oqlQuery.getConditions();
        StringBuilder whereBuffer = new StringBuilder();
        appendWhereClause(whereBuffer, fieldConditions);
        return StringUtils.replacePlaceholder(countTemplate, "%s", tableName, whereBuffer.toString().trim());
    }

    private void appendWhereClause(StringBuilder whereBuffer, List<FieldCondition> fieldConditions) {
        boolean appendFlag = false;
        for (FieldCondition fieldCondition : fieldConditions) {
            String field = fieldCondition.getField();
            if (!fieldColumnMapping.containsKey(field)) {
                continue;
            }
            FieldColumn fieldColumn = fieldColumnMapping.get(field);
            if (appendFlag) {
                whereBuffer.append(" AND ");
            } else {
                whereBuffer.append(" WHERE ");
                appendFlag = true;
            }
            whereBuffer.append("t.").append(fieldColumn.getColumnName())
                    .append(" ").append(fieldCondition.getOperator()).append(" ").append(fieldCondition.getValue());
        }
    }

    public String getSelectSqlByIds(Collection ids) {
        OqlQuery emptyQuery = OqlQuery.create();
        StringBuilder baseSelectSql = new StringBuilder(getSelectTemplate(emptyQuery).trim());
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

    public List<CascadeFetchMapping> getCascadeFetchMappings() {
        return cascadeFetchMappings;
    }
}
