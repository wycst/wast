package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.jdbc.annotations.Column;
import io.github.wycst.wast.jdbc.annotations.Id;
import io.github.wycst.wast.jdbc.transform.TypeTransformer;
import io.github.wycst.wast.jdbc.transform.TypeTransformerFactory;
import io.github.wycst.wast.jdbc.transform.UndoTypeTransformer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2021/2/15 16:24
 * @Description:
 */
public class FieldColumn {

    private final Column column;
    private final Field field;
    private final String columnName;
    // 类型转化器
    private final TypeTransformer typeTransformer;
    private final boolean useTypeTransformer;
    private final GetterInfo getterInfo;
    private final SetterInfo setterInfo;
    private final boolean equalName;
    private final String queryColumnSyntax;

    private final boolean disabledOnSelect;
    private final boolean disabledOnUpdate;
    private final boolean disabledOnInsert;

    private final String placeholderOnReadTemplate;
    private final String placeholderOnWriteTemplate;

    private Class<?> fetchEntityClass;
    private Id id;
    private boolean primary;

    FieldColumn(Field field, Column column, String columnName, SetterInfo setterInfo, GetterInfo getterInfo) {
        this.field = field;
        this.column = column;
        this.columnName = columnName;
        this.getterInfo = getterInfo;
        this.setterInfo = setterInfo;
        TypeTransformer typeTransformer = null;
        if (column != null && column.transformer() != UndoTypeTransformer.class) {
            typeTransformer = TypeTransformerFactory.getTransformer(column, setterInfo);
        }
        this.disabledOnInsert = column != null && column.disabledOnInsert();
        this.disabledOnUpdate = column != null && column.disabledOnUpdate();
        this.disabledOnSelect = column != null && column.disabledOnSelect();
        this.typeTransformer = typeTransformer;
        this.useTypeTransformer = typeTransformer != null;
        this.equalName = field.getName().equals(columnName);

        String placeholderOnReadTemplate = null;
        String placeholderOnWriteTemplate = null;
        if(column != null) {
            String placeholderOnRead = column.placeholderOnRead().trim();
            if(StringUtils.isNotEmpty(placeholderOnRead)) {
                Map<String, String> context = new HashMap<String, String>();
                context.put("value", "${alias}." + columnName);

                placeholderOnReadTemplate = Expression.renderTemplate(placeholderOnRead, context);
                if(placeholderOnReadTemplate.equals(placeholderOnRead)) {
                    placeholderOnReadTemplate = null;
                }
            }

            String placeholderOnWrite = column.placeholderOnWrite().trim();
            if(StringUtils.isNotEmpty(placeholderOnWrite)) {
                Map<String, String> context = new HashMap<String, String>();
                context.put("value", "${" + field.getName() + "}");

                placeholderOnWriteTemplate = Expression.renderTemplate(placeholderOnWrite, context);
                if(placeholderOnWriteTemplate.equals(placeholderOnWrite)) {
                    placeholderOnWriteTemplate = null;
                }
            }
        }

        this.placeholderOnReadTemplate = placeholderOnReadTemplate;
        this.placeholderOnWriteTemplate = placeholderOnWriteTemplate;

        String queryColumnSyntax;
        if(placeholderOnReadTemplate == null) {
            queryColumnSyntax = equalName ? "t." + columnName + "," : "t." + columnName + " as \"" + field.getName() + "\",";
        } else {
            Map<String, String> context = new HashMap<String, String>();
            context.put("alias", "t");
            // 占位表达式
            queryColumnSyntax = Expression.renderTemplate(placeholderOnReadTemplate, context) + " as \"" + field.getName() + "\",";
        }
        this.queryColumnSyntax = queryColumnSyntax;
    }

    boolean usePlaceholderOnWrite() {
        return placeholderOnWriteTemplate != null;
    }

    String getPlaceholderOnWriteTemplate() {
        return placeholderOnWriteTemplate;
    }

    Object getFieldValue(Object entity) {
        return getterInfo.invoke(entity);
    }

    <E> void setFieldValue(E entity, Object value) {
        setterInfo.invoke(entity, value);
    }

    TypeTransformer getTypeTransformer() {
        return typeTransformer;
    }

    public Class<?> getFetchEntityClass() {
        return fetchEntityClass;
    }

    public void setFetchEntityClass(Class<?> fetchEntityClass) {
        this.fetchEntityClass = fetchEntityClass;
    }

    public Field getField() {
        return field;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public Id getId() {
        return id;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public Column getColumn() {
        return column;
    }

    public boolean isEqualName() {
        return equalName;
    }

    String getQueryColumnSyntax() {
        return this.queryColumnSyntax;
    }

    String getQueryColumnSyntax(String tableAlias) {
        if(placeholderOnReadTemplate == null) {
            return tableAlias + "." + getColumnName();
        } else {
            Map<String, String> context = new HashMap<String, String>();
            context.put("alias", tableAlias);
            return Expression.renderTemplate(placeholderOnReadTemplate, context);
        }
    }

    public SetterInfo getSetterInfo() {
        return setterInfo;
    }

    public boolean isUseTypeTransformer() {
        return useTypeTransformer;
    }

    public Object transform(Object fieldValue) {
        return typeTransformer.toJavaField(fieldValue);
    }

    boolean isDisabledOnSelect() {
        return disabledOnSelect;
    }

    boolean isDisabledOnUpdate() {
        return disabledOnUpdate;
    }

    boolean isDisabledOnInsert() {
        return disabledOnInsert;
    }
}

