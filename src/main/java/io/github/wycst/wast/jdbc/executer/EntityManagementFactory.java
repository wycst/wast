package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.common.utils.ClassUtils;
import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.jdbc.annotations.*;
import io.github.wycst.wast.jdbc.exception.EntityException;
import io.github.wycst.wast.jdbc.exception.OqlParematerException;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @Author wangyunchao
 * @Date 2022/12/3 12:59
 */
public class EntityManagementFactory {

    /**
     * 实体扫描目录
     */
    private String entityScanPackages;

    /**
     * 实体映射信息
     */
    private final Map<Class<?>, EntitySqlMapping> entitySqlMappings = new HashMap<Class<?>, EntitySqlMapping>();

    // 单例
    final static EntityManagementFactory Default = new EntityManagementFactory();

    // 关键字需要使用``包含转义(mysql)
    final static List<String> MysqlDatabaseKeyWords =
            Arrays.asList(
                    "table", "column", "describe", "order", "asc", "desc", "current_date", "terminated",
                    "by", "cursor", "distinct", "explain", "fulltext", "mod", "xor", "range", "limit", "rename"
            );

    private EntityManagementFactory() {
    }

    public static EntityManagementFactory defaultManagementFactory() {
        return Default;
    }

//    public Map<Class<?>, EntitySqlMapping> getEntitySqlMappings() {
//        return entitySqlMappings;
//    }

    /***
     * 设置实体扫描目录
     *
     * @param entityScanPackages
     * @return
     */
    public EntityManagementFactory setEntityScanPackages(String entityScanPackages) {
        this.entityScanPackages = entityScanPackages;
        return this;
    }

    /***
     * 初始化所有实体类
     */
    public final EntityManagementFactory init() {
        // 检查扫描配置
        checkScanPackages();
        this.scanEntitys();
        return this;
    }

    private void checkScanPackages() {
        if (StringUtils.isEmpty(entityScanPackages)) {
            throw new EntityException("Entity scan packages not specified, setEntityScanPackages(arg0) should be call before invoke init() ");
        }
    }

    private Collection<Field> getEntityFields(Class<?> entityCls) {
        Map<String, Field> fieldMap = new LinkedHashMap<String, Field>();
        // 当前定义的属性
        Field[] fields = entityCls.getDeclaredFields();
        for (Field field : fields) {
            fieldMap.put(field.getName(), field);
        }
        Class<?> parentClass = entityCls.getSuperclass();
        while (parentClass != null) {
            // Support inheritance
            if (parentClass.isAnnotationPresent(MapperClass.class)) {
                break;
            }
            parentClass = parentClass.getSuperclass();
        }
        if (parentClass != null) {
            for (Field field : parentClass.getDeclaredFields()) {
                if (!fieldMap.containsKey(field.getName())) {
                    fieldMap.put(field.getName(), field);
                }
            }
        }
        return fieldMap.values();
    }

    public void scanPackages(String... scanPackages) {
        Set<Class<?>> entityClsSet = ClassUtils.findClasses(scanPackages, Object.class, Table.class, false);
        Map<Class<?>, Map<String, JoinField>> entityJoinFields = new HashMap<Class<?>, Map<String, JoinField>>();

        // 根据table注解反射实体的sql映射
        for (Class<?> entityCls : entityClsSet) {

            if (entityCls.isAnnotationPresent(MapperClass.class)) {
                throw new EntityException(" Entity Class " + entityCls + " AnnotationPresent[MapperClass] are not allowed ! ");
            }

            Table table = entityCls.getAnnotation(Table.class);
            String tableName = table.name().trim();
            String schame = table.schame();
            if (StringUtils.isEmpty(tableName)) {
                tableName = StringUtils.camelCaseToSymbol(entityCls.getSimpleName());
            }
            if (StringUtils.isNotEmpty(schame)) {
                tableName = schame + "." + tableName;
            }

            Map<String, FieldColumn> fieldColumnMapping = new LinkedHashMap<String, FieldColumn>();
            Map<Class<?>, JoinEntityMapping> joinEntityMappings = new LinkedHashMap<Class<?>, JoinEntityMapping>();

            FieldColumn primary = null;
            Collection<Field> fields = this.getEntityFields(entityCls);

            ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(entityCls);

            Map<String, JoinField> joinFields = new HashMap<String, JoinField>();
            List<CascadeFetchMapping> cascadeFetchMappings = new ArrayList<CascadeFetchMapping>();

            for (Field field : fields) {
                // 字段不参与sql
                if (field.getAnnotation(Transient.class) != null) {
                    continue;
                }
                String fieldName = field.getName();
                // 非setter方法跳过
                if (!classStructureWrapper.containsSetterKey(fieldName)) {
                    continue;
                }
                SetterInfo setterInfo = classStructureWrapper.getSetterInfo(fieldName);
                GetterInfo getterInfo = classStructureWrapper.getGetterInfo(fieldName);

                // join处理
                if (field.getAnnotation(JoinField.class) != null) {
                    joinFields.put(fieldName, field.getAnnotation(JoinField.class));
                    continue;
                }
                // cascade处理
                if (field.isAnnotationPresent(CascadeFetch.class)) {
                    // 属性类型
                    int fieldTypeValue = 0;
                    Class<?> targetEntityClass = null;
                    Class<?> fieldType = field.getType();
                    if (entityClsSet.contains(fieldType)) {
                        fieldTypeValue = 1;
                        targetEntityClass = fieldType;
                    } else if (List.class.isAssignableFrom(fieldType)) {
                        // 如果是列表获取泛型类
                        Class<?> actualType = setterInfo.getActualTypeArgument();
                        if (entityClsSet.contains(actualType)) {
                            fieldTypeValue = 2;
                            targetEntityClass = actualType;
                        }
                    }
                    if (targetEntityClass != null) {
                        CascadeFetch cascadeFetch = field.getAnnotation(CascadeFetch.class);
                        CascadeFetchMapping cascadeFetchMapping = new CascadeFetchMapping(targetEntityClass, field, fieldTypeValue, cascadeFetch);
                        cascadeFetchMappings.add(cascadeFetchMapping);
                        continue;
                    }

                    throw new EntityException(" Entity " + entityCls + " " + field + " annotationPresent @CascadeFetch but the field type is not be an entity or the list generic is not an entity type");
                }
                String columnName;
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    columnName = column.name();
                    if (StringUtils.isEmpty(columnName)) {
                        columnName = StringUtils.camelCaseToSymbol(fieldName);
                    }
                } else {
                    columnName = StringUtils.camelCaseToSymbol(fieldName);
                }
                // todo only mysql mode
//                if (MysqlDatabaseKeyWords.contains(columnName.toLowerCase())) {
//                    char[] chars = new char[columnName.length() + 2];
//                    chars[0] = chars[columnName.length() + 1] = '`';
//                    columnName.getChars(0, columnName.length(), chars, 1);
//                    columnName = new String(chars);
//                }
                FieldColumn fieldColumn = new FieldColumn(field, column, columnName, setterInfo, getterInfo);
                fieldColumnMapping.put(fieldName, fieldColumn);

                if (field.getAnnotation(Id.class) != null) {
                    primary = fieldColumn;
                    Id id = field.getAnnotation(Id.class);
                    primary.setId(id);
                    primary.setPrimary(true);
                }

                Join join = field.getAnnotation(Join.class);
                if (join != null) {
                    Class<?> target = join.target();
                    if (!entityClsSet.contains(target)) {
                        throw new EntityException(" Entity Class " + entityCls + " , field " + field + " Annotation@Join target class " + target + " is not a Table Entity");
                    }
                    // use when on condition
                    String fieldKey = join.field();
                    JoinEntityMapping joinEntityMapping = joinEntityMappings.get(target);
                    if (joinEntityMapping == null) {
                        joinEntityMapping = new JoinEntityMapping();
                        joinEntityMappings.put(target, joinEntityMapping);
                    }
                    // add join Fields mapping
                    joinEntityMapping.getJoinOnFieldKeys().put(fieldName, fieldKey);
                }
            }
            // joinQueryFields
            if (joinFields.size() > 0) {
                entityJoinFields.put(entityCls, joinFields);
            }

            EntitySqlMapping entitySqlMapping = new EntitySqlMapping(entityCls, tableName, fieldColumnMapping, primary, joinEntityMappings, cascadeFetchMappings, table);
            entitySqlMappings.put(entityCls, entitySqlMapping);
        }

        // handle joinFields
        for (Map.Entry<Class<?>, Map<String, JoinField>> entityEntry : entityJoinFields.entrySet()) {
            // source from @JoinField
            Class<?> entityCls = entityEntry.getKey();
            Map<String, JoinField> joinFields = entityEntry.getValue();
            EntitySqlMapping entitySqlMapping = entitySqlMappings.get(entityCls);
            // source from @Join
            Map<Class<?>, JoinEntityMapping> joinEntityMappings = entitySqlMapping.getJoinEntityMappings();
            for (Map.Entry<Class<?>, JoinEntityMapping> joinEntry : joinEntityMappings.entrySet()) {
                Class<?> joinClass = joinEntry.getKey();
                JoinEntityMapping joinEntityMapping = joinEntry.getValue();
                Map<String, String> joinOnFieldKeys = joinEntityMapping.getJoinOnFieldKeys();
                EntitySqlMapping targetEntitySqlMapping = entitySqlMappings.get(joinClass);
                for (Map.Entry<String, String> joinFiledEntry : joinOnFieldKeys.entrySet()) {
                    String fieldName = joinFiledEntry.getKey();
                    String joinFieldName = joinFiledEntry.getValue();
                    String columnName = entitySqlMapping.getFieldColumnMapping().get(fieldName).getColumnName();
                    String joinColumnName = targetEntitySqlMapping.getFieldColumnMapping().get(joinFieldName).getColumnName();
                    joinEntityMapping.getJoinOnColumnKeys().put(columnName, joinColumnName);
                }
            }

            // 遍历同一个实体下面可能join多个字段来源多个关联的实体
            for (Map.Entry<String, JoinField> fieldEntry : joinFields.entrySet()) {
                String fieldName = fieldEntry.getKey();
                JoinField joinField = fieldEntry.getValue();
                Class target = joinField.target();
                if (!entityClsSet.contains(target)) {
                    throw new EntityException(" Entity Class '" + entityCls + "' , field '" + fieldName + "' annotation@JoinField target class '" + target + "' is not a Table Entity");
                }
                EntitySqlMapping targetEntitySqlMapping = entitySqlMappings.get(target);
                if (!joinEntityMappings.containsKey(target)) {
                    throw new EntityException(" Entity Class " + entityCls + " , field " + fieldName + " with annotation@JoinField target class '" + target + "' has not targetClass by annotation@Join to mapping the conditions of the relation table");
                }
                JoinEntityMapping joinEntityMapping = joinEntityMappings.get(target);
                joinEntityMapping.setTableName(targetEntitySqlMapping.getTableName());
                JoinColumn joinColumn = new JoinColumn(joinField);
                joinColumn.setFieldName(fieldName);

                // target join fieldName
                String joinFieldName = joinField.field();
                Map<String, FieldColumn> targetFieldColumnMapping = targetEntitySqlMapping.getFieldColumnMapping();
                if (!targetFieldColumnMapping.containsKey(joinFieldName)) {
                    throw new EntityException(" Entity Class " + entityCls + " , field " + fieldName + " annotation@JoinField field '" + joinFieldName + "' has not a field at class " + target);
                }
                FieldColumn fieldColumn = targetFieldColumnMapping.get(joinFieldName);
                joinColumn.setJoinFieldColumn(fieldColumn);
                joinEntityMapping.getJoinColumns().add(joinColumn);
            }

            // join初始化后重置模板sql
            entitySqlMapping.init();
        }
    }

    private void scanEntitys() {
        entitySqlMappings.clear();
        // 扫描entitys
        scanPackages(entityScanPackages.split(","));
    }

    public void clear() {
        entitySqlMappings.clear();
    }

    public void clearCaches(Class<?>... entityClassList) {
        for (Class<?> entityCls : entityClassList) {
            EntitySqlMapping entitySqlMapping = getEntitySqlMapping(entityCls);
            if (entitySqlMapping.isCacheable()) {
                entitySqlMapping.getCacheableEntityHandler().resetCaches();
            }
        }
    }

    public void clearAllCaches() {
        for (EntitySqlMapping entitySqlMapping : entitySqlMappings.values()) {
            if (entitySqlMapping.isCacheable()) {
                CacheableEntityHandler cacheableEntityHandler = (CacheableEntityHandler) entitySqlMapping.getEntityHandler();
                cacheableEntityHandler.resetCaches();
            }
        }
    }

    public void clearExpiredCaches() {
        for (EntitySqlMapping entitySqlMapping : entitySqlMappings.values()) {
            if (entitySqlMapping.isCacheable()) {
                CacheableEntityHandler cacheableEntityHandler = (CacheableEntityHandler) entitySqlMapping.getEntityHandler();
                cacheableEntityHandler.clearExpiredCaches();
            }
        }
    }

    public boolean existEntity(Class<?> entityCls) {
        return entitySqlMappings.containsKey(entityCls);
    }

    public EntitySqlMapping getEntitySqlMapping(Class entityCls) {
        return entitySqlMappings.get(entityCls);
    }

    public void checkEntityClass(Class<?> entityCls) {
        entityCls.getClass();
        if (!existEntity(entityCls)) {
            throw new OqlParematerException("参数错误：" + entityCls + "没有纳入对象sql管理，请检查实体扫描配置");
        }
    }
}
