package io.github.wycst.wast.jdbc.query;

import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.jdbc.commands.ResultSetCommand;
import io.github.wycst.wast.jdbc.executer.EntityManagementFactory;
import io.github.wycst.wast.jdbc.executer.EntitySqlMapping;
import io.github.wycst.wast.jdbc.executer.FieldColumn;
import io.github.wycst.wast.jdbc.util.StreamCursor;
import io.github.wycst.wast.log.Log;
import io.github.wycst.wast.log.LogFactory;

import java.sql.*;
import java.util.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class QueryExecutor {

    // log 类
    private Log log = LogFactory.getLog(QueryExecutor.class);

    /**
     * 查询对象，如果结果集多个返回第一个
     *
     * @param cls
     * @param statement
     * @return
     * @throws SQLException
     */
    public <E> E queryObject(final Class<E> cls, PreparedStatement statement)
            throws SQLException {

        return executeQuery(statement, new ResultSetCommand<E>() {

            public E doResultSet(ResultSet resultSet, ResultSetMetaData rsmd) throws Exception {
                Object result = null;
                if (resultSet.next()) {
                    result = parseResultSet(resultSet, rsmd, cls);
                }
                return (E) result;
            }

        });
    }

    /**
     * 返回集合对象
     *
     * @param cls
     * @param statement
     * @param <E>
     * @return
     * @throws SQLException
     */
    public <E> List<E> queryList(final Class<E> cls, PreparedStatement statement)
            throws SQLException {

        return executeQuery(statement, new ResultSetCommand<List<E>>() {

            public List<E> doResultSet(ResultSet resultSet, ResultSetMetaData rsmd) throws Exception {
                List<E> collection = new ArrayList<E>();
                while (resultSet.next()) {
                    E e = (E) parseResultSet(resultSet, rsmd, cls);
                    collection.add(e);
                }
                return collection;
            }

        });


    }

    /**
     * 查询唯一对象，如果结果集多个抛出异常，如果没有返回null
     *
     * @param cls
     * @param statement
     * @param <E>
     * @return
     * @throws SQLException
     */
    public <E> E queryUniqueObject(final Class<E> cls, PreparedStatement statement)
            throws SQLException {

        return executeQuery(statement, new ResultSetCommand<E>() {

            public E doResultSet(ResultSet resultSet, ResultSetMetaData rsmd) throws Exception {
                resultSet.last();
                int row = resultSet.getRow();
                if (row > 1) {
                    throw new SQLException("期望返回1个记录，实际返回了" + row + "个记录");
                }
                if (row == 0)
                    return null;

                return (E) parseResultSet(resultSet, rsmd, cls);
            }

        });

    }

    private <E> E executeQuery(PreparedStatement statement, ResultSetCommand<E> command) throws SQLException {

        ResultSet resultSet = null;
        try {
            // execute query
            resultSet = statement.executeQuery();
            // cls
            ResultSetMetaData rsmd = resultSet.getMetaData();
            E result = command.doResultSet(resultSet, rsmd);
            return result;
        } catch (Exception e) {
            throw new SQLException(e);
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }


    <E> Object parseResultSet(ResultSet resultSet, ResultSetMetaData rsmd, Class<E> cls) throws Exception {

        boolean isMap = false;
        Map<String, Object> mapData = null;
        E instance = null;

        ClassStructureWrapper classStructureWrapper = null;
        EntitySqlMapping entitySqlMapping = null;
        // is map
        if (Map.class.isAssignableFrom(cls)) {
            isMap = true;
            // 如果是map接口，使用LinkedHashMap实例化
            if (cls.isInterface()) {
                mapData = new LinkedHashMap<String, Object>();
            } else {
                mapData = (Map) cls.newInstance();
            }
        } else {
            classStructureWrapper = ClassStructureWrapper.get(cls);
            instance = (E) classStructureWrapper.newInstance();
            entitySqlMapping = EntityManagementFactory.defaultManagementFactory().getEntitySqlMapping(cls);
        }

        int columnCount = rsmd.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String fieldName = rsmd.getColumnLabel(i).trim();
            int columnType = rsmd.getColumnType(i);
            Object fieldValue = getColumnValue(resultSet, columnType, i);
            if (isMap) {
                mapData.put(fieldName, fieldValue);
            } else {
                FieldColumn fieldColumn;
                if (entitySqlMapping != null && (fieldColumn = entitySqlMapping.getFieldColumn(fieldName)) != null) {
                    SetterInfo setterInfo = fieldColumn.getSetterInfo();
                    Object value;
                    if (fieldColumn.isUseTypeTransformer()) {
                        value = fieldColumn.transform(fieldValue);
                    } else {
                        value = ObjectUtils.toType(fieldValue, setterInfo.getParameterType(), setterInfo.getGenericParameterizedType().getActualClassCategory());
                    }
                    setterInfo.invoke(instance, value);
                } else {
                    SetterInfo setterInfo = classStructureWrapper.getSetterInfo(fieldName);
                    // 默认直接查找fieldName，如果没有，将fieldName转化为小写驼峰格式比如INDEX_NAME-> indexName
                    if (setterInfo == null) {
                        String camelCase = StringUtils.getCamelCase(fieldName);
                        setterInfo = classStructureWrapper.getSetterInfo(camelCase);
                    }
                    if (setterInfo != null) {
                        setterInfo.invoke(instance, ObjectUtils.toType(fieldValue, setterInfo.getParameterType(), setterInfo.getGenericParameterizedType().getActualClassCategory()));
                    } else {
                        // 判断是否需要解析为a.b.c
                        // select语句中需要双引号 as "a.b.c"
                        if (fieldName.matches("(\\w|[$¥])+([.](\\w|[$¥])+)+")) {
                            // 解析对象属性
                            parseObjectField(fieldName, fieldValue, instance, classStructureWrapper);
                        }
                    }
                }
            }
        }

        return isMap ? mapData : instance;
    }

    /**
     * 解析对象属性
     * select t.city_name as "city.cityName"
     *
     * @param fieldName    eg : city.cityName
     * @param fieldValue
     * @param instance     entity
     * @param classWrapper
     * @throws Exception
     */
    private void parseObjectField(String fieldName, Object fieldValue, Object instance,
                                  ClassStructureWrapper classWrapper) throws Exception {

        int dotIndex = fieldName.indexOf(".");
        SetterInfo setterInfo;
        if (dotIndex == -1) {
            // set
            setterInfo = classWrapper.getSetterInfo(fieldName);
            if (setterInfo != null) {
                setterInfo.invoke(instance, ObjectUtils.toType(fieldValue, setterInfo.getParameterType(), setterInfo.getGenericParameterizedType().getActualClassCategory()));
            }
        } else {
            // 对象属性标识符名称
            String topFieldName = fieldName.substring(0, dotIndex);
            // 待下次递归的fieldName
            String nextFieldName = fieldName.substring(dotIndex + 1);
            // 对应的setter方法
            setterInfo = classWrapper.getSetterInfo(topFieldName);
            // 如果存在setter方法，查询是否存在getter方法，并调用获取属性对象是否已创建
            // 如果已创建，获取属性对象，递归调用解析，否则调用类的newInstance创建属性对象，并调用setter
            // 递归调用解析
            if (setterInfo != null) {
                Class<?> parameterType = setterInfo.getParameterType();
                // 如果是基本类型，string，number，date,数组等直接不处理
                // 推导为map和pojo类型
                ReflectConsts.ClassCategory classCategory = setterInfo.getGenericParameterizedType().getActualClassCategory();
                if (classCategory != ReflectConsts.ClassCategory.ObjectCategory
                        && classCategory != ReflectConsts.ClassCategory.MapCategory) {
                    return;
                }

                Object target = null;
                GetterInfo getterInfo = classWrapper.getGetterInfo(topFieldName);
                if (getterInfo != null) {
                    target = getterInfo.invoke(instance);
                    if (target == null) {
                        // 如果对象属性是个map类型（约定好只能声明为java.util.Map）
                        if (Map.class == parameterType) {
                            target = new HashMap<String, Object>();
                        } else {
                            // 自定义对象类型
                            target = parameterType.newInstance();
                        }
                        setterInfo.invoke(instance, target);
                    }
                    if (target instanceof Map) {
                        Map<String, Object> targetMap = (Map<String, Object>) target;
                        targetMap.put(nextFieldName, fieldValue);
                    } else {
                        if (parameterType.isInstance(target)) {
                            ClassStructureWrapper nextClassStructureWrapper = ClassStructureWrapper.get(parameterType);
                            // 递归调用
                            parseObjectField(nextFieldName, fieldValue, target, nextClassStructureWrapper);
                        }
                    }
                }
            }
        }

    }

    public <E> E queryValue(final Class<E> valueClass, PreparedStatement statement) throws SQLException {

        return executeQuery(statement, new ResultSetCommand<E>() {

            public E doResultSet(ResultSet resultSet, ResultSetMetaData rsmd) throws Exception {

                resultSet.last();
                int row = resultSet.getRow();
                if (row > 1) {
                    throw new SQLException("Expected to return 1 record, but actually returned " + row + " record");
                }
                if (row == 0)
                    return null;

                int columnCount = rsmd.getColumnCount();

                if (columnCount > 1) {
                    throw new SQLException("Expected to return 1 column, but actually returned " + columnCount + " column, please call queryObject ");
                }

                int columnIndex = 1;
                int columnType = rsmd.getColumnType(columnIndex);
                Object columnValue = getColumnValue(resultSet, columnType, columnIndex);
                if (valueClass == null) {
                    return (E) columnValue;
                }
                return ObjectUtils.toType(columnValue, valueClass);
            }

        });
    }

    /**
     * 获取字段值
     *
     * @param resultSet
     * @param columnType
     * @param columnIndex
     * @return
     */
    Object getColumnValue(ResultSet resultSet, int columnType, int columnIndex) throws SQLException {

        Object fieldValue;
        switch (columnType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NVARCHAR: {
                fieldValue = resultSet.getString(columnIndex);
                break;
            }
            case Types.NUMERIC:
            case Types.DECIMAL: {
                fieldValue = resultSet.getBigDecimal(columnIndex);
                break;
            }
            case Types.BIT: {
                fieldValue = resultSet.getBoolean(columnIndex);
                break;
            }
            case Types.TINYINT: {
                fieldValue = resultSet.getByte(columnIndex);
                break;
            }
            case Types.SMALLINT: {
                fieldValue = resultSet.getShort(columnIndex);
                break;
            }
            case Types.INTEGER: {
                fieldValue = resultSet.getInt(columnIndex);
                break;
            }
            case Types.BIGINT: {
                fieldValue = resultSet.getLong(columnIndex);
                break;
            }
            case Types.REAL: {
                fieldValue = resultSet.getFloat(columnIndex);
                break;
            }
            case Types.FLOAT:
            case Types.DOUBLE: {
                fieldValue = resultSet.getDouble(columnIndex);
                break;
            }
            case Types.LONGVARBINARY:
            case Types.VARBINARY:
            case Types.BINARY: {
                fieldValue = resultSet.getBytes(columnIndex);
                break;
            }
            case Types.DATE: {
                fieldValue = resultSet.getDate(columnIndex);
                break;
            }
            case Types.TIME: {
                fieldValue = resultSet.getTime(columnIndex);
                break;
            }
            case Types.TIMESTAMP: {
                fieldValue = resultSet.getTimestamp(columnIndex);
                break;
            }
            default: {
                fieldValue = resultSet.getObject(columnIndex);
                break;
            }
        }
        return fieldValue;
    }

    public <E> StreamCursor<E> queryStreamCursor(Class<E> cls, PreparedStatement statement, Connection conn) throws SQLException {
        try {
            ResultSet resultSet = statement.executeQuery();
            ResultSetMetaData rsmd = resultSet.getMetaData();
            return new StreamCursorImpl<E>(resultSet, rsmd, cls, conn, this);
        } catch (Throwable throwable) {
            throw new SQLException(throwable.getMessage(), throwable);
        }
    }
}
