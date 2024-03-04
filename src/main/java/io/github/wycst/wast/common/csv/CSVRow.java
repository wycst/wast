package io.github.wycst.wast.common.csv;

import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.common.utils.ObjectUtils;

import java.util.*;

public class CSVRow {

    transient final CSVTable csvTable;

    List<String> values;

    CSVRow(CSVTable csvTable, List<String> values) {
        this.values = values;
        this.csvTable = csvTable;
    }

    public String get(int index) {
        return values.get(index);
    }

    public String get(String name) {
        int index = csvTable.getColumnIndex(name.trim());
        if (index == -1) return null;
        return values.get(index);
    }

    /**
     * 转换为实体bean
     *
     * @param eClass
     * @param <E>
     * @return
     */
    public <E> E toBean(Class<E> eClass) {
        return toBean(eClass, null);
    }

    /**
     * 转换为实体bean
     *
     * @param eClass
     * @param <E>
     * @return
     */
    public <E> E toBean(Class<E> eClass, Map<String, String> columnMapping) {
        ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(eClass);
        if (classCategory != ReflectConsts.ClassCategory.ObjectCategory) {
            throw new UnsupportedOperationException("class " + eClass + " is not supported ");
        }
        ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(eClass);
        List<String> columns = csvTable.getColumns().values;
        Map<String, CSVColumnMapper> annotationedColumnMap = validatedColumnAnnotationed(classStructureWrapper, columns);
        try {
            E e = (E) classStructureWrapper.newInstance();
            int columnIndex = 0, size = values.size();
            for (String column : columns) {
                if(columnIndex < size) {
                    SetterInfo setterInfo;
                    boolean checkRequired = false;
                    Class<? extends CSVTypeHandler> typeHandlerCls = null;
                    if(annotationedColumnMap.containsKey(column)) {
                        CSVColumnMapper csvColumnMapper = annotationedColumnMap.get(column);
                        setterInfo = csvColumnMapper.setterInfo;
                        checkRequired = csvColumnMapper.csvColumn.required();
                        typeHandlerCls = csvColumnMapper.csvColumn.handler();
                    } else {
                        String name = columnMapping == null ? null : columnMapping.get(column);
                        setterInfo = classStructureWrapper.getSetterInfo(name == null ? column : name);
                    }
                    String stringVal = values.get(columnIndex);
                    if (setterInfo != null) {
                        Object value;
                        Class<?> type = setterInfo.getParameterType();
                        if(typeHandlerCls == null || typeHandlerCls == CSVTypeHandler.DefaultCSVTypeHandler.class) {
                            value = ObjectUtils.toType(stringVal, type);
                        } else {
                            try {
                                CSVTypeHandler typeHandler = typeHandlerCls.newInstance();
                                value = typeHandler.handle(stringVal, type);
                                if(value != null) {
                                    if(!type.isPrimitive() && !type.isInstance(value)) {
                                        throw new CSVException("value '" + value + "'  from handler is not matched type " + type);
                                    }
                                }
                            } catch (Throwable throwable) {
                                throw new CSVException(throwable.getMessage(), throwable);
                            }
                        }
                        if(checkRequired && value == null) {
                            throw new CSVException("value for column '" + column + "' is required but null");
                        }
                        setterInfo.invoke(e, value);
                    }
                }
                ++columnIndex;
            }
            return e;
        } catch (Exception ex) {
            throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
        }
    }

    private Map<String, CSVColumnMapper> validatedColumnAnnotationed(ClassStructureWrapper classStructureWrapper, List<String> columns) {
        Map<String, CSVColumnMapper> annotationedMap = new HashMap<String, CSVColumnMapper>();
        Set<SetterInfo> setterInfoSet = classStructureWrapper.setterSet();
        for (SetterInfo setterInfo : setterInfoSet) {
            CSVColumn csvColumn = (CSVColumn) setterInfo.getAnnotation(CSVColumn.class);
            if(csvColumn == null) continue;
            String value = csvColumn.value().trim();
            if(value.length() == 0) {
                value = setterInfo.getName();
            }
            if(csvColumn.required() && columns.indexOf(value) == -1) {
                throw new CSVException("column '" + value + "' is required");
            }
            CSVColumnMapper csvColumnMapper = new CSVColumnMapper();
            csvColumnMapper.csvColumn = csvColumn;
            csvColumnMapper.setterInfo = setterInfo;
            annotationedMap.put(value, csvColumnMapper);
        }
        return annotationedMap;
    }

    /**
     * 转换为实体bean
     *
     * @return
     */
    public Map toMap() {
        Map map = new LinkedHashMap();
        List<String> columns = csvTable.getColumns().values;
        try {
            int columnIndex = 0;
            for (String column : columns) {
                map.put(column, values.get(columnIndex));
                ++columnIndex;
            }
            return map;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public int indexOf(String value) {
        return values.indexOf(value);
    }

    public void set(int columnIndex, String value) {
        values.set(columnIndex, value);
    }

    static class CSVColumnMapper {
        CSVColumn csvColumn;
        SetterInfo setterInfo;
    }
}
