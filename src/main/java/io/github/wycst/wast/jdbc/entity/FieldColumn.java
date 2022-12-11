package io.github.wycst.wast.jdbc.entity;

import java.lang.reflect.Field;

import io.github.wycst.wast.jdbc.annotations.Column;
import io.github.wycst.wast.jdbc.annotations.Id;

/**
 * @Author: wangy
 * @Date: 2021/2/15 16:24
 * @Description:
 */
public class FieldColumn {
	
    private Field field;
    private String columnName;
    private Class<?> fetchEntityClass;
    private Id id;
    private boolean primary;
    private Column column;

    public Class<?> getFetchEntityClass() {
        return fetchEntityClass;
    }

    public void setFetchEntityClass(Class<?> fetchEntityClass) {
        this.fetchEntityClass = fetchEntityClass;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
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

    public void setColumn(Column column) {
        this.column = column;
    }

    public Column getColumn() {
        return column;
    }
}

