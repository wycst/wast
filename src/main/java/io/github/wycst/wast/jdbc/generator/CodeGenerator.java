package io.github.wycst.wast.jdbc.generator;

import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.template.StringTemplate;
import io.github.wycst.wast.common.template.StringTemplateManager;
import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.jdbc.commands.SqlExecuteCall;
import io.github.wycst.wast.jdbc.exception.SqlExecuteException;
import io.github.wycst.wast.jdbc.executer.DefaultSqlExecuter;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.*;

/**
 * 基于数据库的代码生成器
 *
 * @Author wangyunchao
 * @Date 2021/9/3 17:32
 */
public class CodeGenerator {

    private static final String controllerTemplate;
    private static final String serviceTemplate;
    private static final String serviceImplTemplate;
    private static final String apiJsTemplate;
    private static final StringTemplate vueTemplate;

    static {
        controllerTemplate = StringUtils.fromResource("/generator/tpl/Controller.tpl");
        serviceTemplate = StringUtils.fromResource("/generator/tpl/Service.tpl");
        serviceImplTemplate = StringUtils.fromResource("/generator/tpl/ServiceImpl.tpl");
        apiJsTemplate = StringUtils.fromResource("/generator/tpl/ApiJs.tpl");
        vueTemplate = StringTemplateManager.getStringTemplate("/generator/tpl/Vue.tpl");
    }

    /**
     * 代码生成实现
     *
     * @param context
     * @param dataSource
     */
    public static void generate(GeneratorContext context, DataSource dataSource) {
        DefaultSqlExecuter sqlExecuter = new DefaultSqlExecuter();
        sqlExecuter.setDataSource(dataSource);

        final List<GeneratorTable> generatorTables = new ArrayList<GeneratorTable>();
        final String[] tableNames = context.getTableNames();
        if (tableNames != null) {
            sqlExecuter.executePipelined(new SqlExecuteCall<Object>() {
                @Override
                public Object execute(Connection connection) throws SQLException {
                    for (String tableName : tableNames) {
                        GeneratorTableOption tableOption = new GeneratorTableOption();
                        tableOption.setTableName(tableName);
                        GeneratorTable generatorTable = new GeneratorTable();
                        generatorTable.setTableColumns(generateTableColumns(connection, tableOption));
                        generatorTables.add(generatorTable);
                    }
                    return null;
                }
            });
        } else {
            Map<String, GeneratorTableOption> tableOptionMap = context.getTableOptions();
            if (tableOptionMap != null) {
                for (GeneratorTableOption tableOption : tableOptionMap.values()) {
                    GeneratorTable generatorTable = generateTable(tableOption, context, dataSource);
                    generatorTables.add(generatorTable);
                }
            }
        }

        context.setGeneratorTables(generatorTables);

        try {
            context.writeFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成某一个表的实体信息和字段信息
     *
     * @param tableName
     * @param context
     * @param dataSource
     * @return
     */
    public static GeneratorTable generateTable(String tableName, GeneratorContext context, DataSource dataSource) {
        GeneratorTableOption tableOption = new GeneratorTableOption();
        tableOption.setTableName(tableName);
        return generateTable(tableOption, context, dataSource);
    }

    /**
     * 生成某一个表的实体信息和字段信息
     *
     * @param tableOption
     * @param context
     * @param dataSource
     * @return
     */
    public static GeneratorTable generateTable(final GeneratorTableOption tableOption, GeneratorContext context, DataSource dataSource) {

        final String tableName = tableOption.getTableName();
        final int primaryPolicy = tableOption.getPrimaryPolicy();
        String deletePrefixAsModule = tableOption.getDeletePrefixAsModule();

        String author = context.getAuthor();
        String basePackage = context.getBasePackage();
        String entityPackage = context.getEntityPackage();
        boolean usePackage = basePackage != null || entityPackage != null;
        if (usePackage) {
            if (entityPackage == null) {
                entityPackage = basePackage + ".entitys";
            }
        }

        final GeneratorTable generatorTable = new GeneratorTable();
        generatorTable.setTableName(tableName);

        boolean useLombok = context.isUseLombok();
        String deletePrefix = context.getDeletePrefixAsEntity();
        String entityName = StringUtils.getCamelCase(tableName.startsWith(deletePrefix) ? tableName.substring(deletePrefix.length()) : deletePrefix, true);
        generatorTable.setEntityName(entityName);
        generatorTable.setUpperCaseModuleName(deletePrefixAsModule == null ? entityName : entityName.substring(deletePrefixAsModule.length()));
        generatorTable.setLowerCaseModuleName(StringUtils.getCamelCase(generatorTable.getUpperCaseModuleName()));

        String modulePath = StringUtils.camelCaseToSymbol(generatorTable.getLowerCaseModuleName(), "-");
        generatorTable.setModulePath(modulePath);

        final StringBuilder entityBuffer = new StringBuilder();
        if (usePackage) {
            entityBuffer.append(String.format("package %s;\n\n", entityPackage));
        }
        entityBuffer.append("import io.github.wycst.wast.jdbc.annotations.*;\n");
        entityBuffer.append("import io.github.wycst.wast.jdbc.annotations.Id.GenerationType;\n");

        entityBuffer.append(String.format("/**\n * <p> Table: %s\n *\n * @author       %s\n * @date         %s\n */\n", tableName, author, new java.util.Date().toString()));
        if (useLombok) {
            entityBuffer.append("@lombok.Data\n");
        }
        entityBuffer.append("@Table(name = \"" + tableName + "\")\n");
        entityBuffer.append(
                "public class " + entityName + " implements java.io.Serializable {\n\n");

        DefaultSqlExecuter sqlExecuter = new DefaultSqlExecuter();
        sqlExecuter.setDataSource(dataSource);
        sqlExecuter.executePipelined(new SqlExecuteCall<Object>() {
            @Override
            public Object execute(Connection connection) throws SQLException {
                Map<String, GeneratorTableColumn> tableColumns = generateTableColumns(connection, tableOption);
                generatorTable.setTableColumns(tableColumns);
                return null;
            }
        });

        StringBuilder fieldsBuffer = new StringBuilder();
        StringBuilder getterSetterBuffer = new StringBuilder();

        Collection<GeneratorTableColumn> tableColumns = generatorTable.getTableColumns().values();
        int queryIndex = 0;
        for (GeneratorTableColumn tableColumn : tableColumns) {
            String columnName = tableColumn.getColumnName();
            String javaField = tableColumn.getJavaField();
            int javaType = tableColumn.getJavaType();
            String javaTypeName = tableColumn.getJavaTypeName();
            boolean primary = tableColumn.isPrimary();
            if (primary) {
                if (primaryPolicy == GeneratorTableOption.PRIMARY_POLICY_IDENTITY) {
                    fieldsBuffer.append("    @Id(strategy = GenerationType.Identity)\n");
                } else if (primaryPolicy == GeneratorTableOption.PRIMARY_POLICY_UUID) {
                    fieldsBuffer.append("    @Id(strategy = GenerationType.UUID)\n");
                } else if (primaryPolicy == GeneratorTableOption.PRIMARY_POLICY_ALG) {
                    fieldsBuffer.append("    @Id(strategy = GenerationType.AutoAlg)\n");
                } else if (primaryPolicy == GeneratorTableOption.PRIMARY_POLICY_SEQUENCE) {
                    fieldsBuffer.append("    @Id(strategy = GenerationType.Sequence)\n");
                } else {
                    fieldsBuffer.append("    @Id\n");
                }
            }
            fieldsBuffer.append("    @Column(name = \"" + columnName + "\")\n");
            fieldsBuffer.append("    private " + javaTypeName + " " + javaField + ";\n\n");

            if (!useLombok) {

                // 生成getter prefix
                String getterPrefix = javaTypeName.equals("boolean") ? "is" : "get";
                // getter or setter suffix
                String getterSuffix = javaField;
                if (javaField.length() == 1 || !Character.isUpperCase(javaField.charAt(1))) {
                    char[] chars = javaField.toCharArray();
                    chars[0] = Character.toUpperCase(chars[0]);
                    getterSuffix = new String(chars);
                }

                getterSetterBuffer.append("    public " + javaTypeName + " " + getterPrefix + getterSuffix + "() {\n");
                getterSetterBuffer.append("        return ").append(javaField).append(";\n");
                getterSetterBuffer.append("    }\n\n");

                getterSetterBuffer.append("    public void set" + getterSuffix + String.format("(%s %s) {\n", javaTypeName, javaField));
                getterSetterBuffer.append("        this.").append(javaField).append(" = ").append(javaField).append(";\n");
                getterSetterBuffer.append("    }\n\n");
            }

            GeneratorColumnOption columnOption = new GeneratorColumnOption();
            if (!primary) {
                if (queryIndex++ < 2) {
                    columnOption.setQuery(true);
                }
                columnOption.setUpdate(!primary);
                columnOption.setDisplay(javaType == 1);
            }
            tableColumn.setColumnOption(columnOption);
        }

        entityBuffer.append(fieldsBuffer).append(getterSetterBuffer);
        entityBuffer.append("}");
        generatorTable.setEntityCode(entityBuffer.toString());

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("basePackage", basePackage == null ? "" : basePackage);
        vars.put("entityPackage", entityPackage);
        vars.put("tableName", tableName);
        vars.put("entityName", entityName);
        vars.put("upperCaseModuleName", generatorTable.getUpperCaseModuleName());
        vars.put("lowerCaseModuleName", generatorTable.getLowerCaseModuleName());
        vars.put("modulePath", modulePath);
        vars.put("author", context.getAuthor());
        vars.put("date", new Date().toString());
        vars.put("setCreateDateCode", "");
        vars.put("setUpdateDateCode", "");
        vars.put("columns", tableColumns);

        if (context.isGenerateController() && controllerTemplate != null) {
            String controllerCode = Expression.renderTemplate(controllerTemplate, vars);
            generatorTable.setControllerCode(controllerCode);
        }

        if (context.isGenerateService()) {
            if (serviceTemplate != null) {
                String serviceInfCode = Expression.renderTemplate(serviceTemplate, vars);
                generatorTable.setServiceInfCode(serviceInfCode);
            }
            if (serviceImplTemplate != null) {
                String serviceImplCode = Expression.renderTemplate(serviceImplTemplate, vars);
                generatorTable.setServiceImplCode(serviceImplCode);
            }
        }

        if (context.isGenerateViews()) {
            if (apiJsTemplate != null) {
                String apiJsCode = Expression.renderTemplate(apiJsTemplate, vars);
                generatorTable.setApiJsCode(apiJsCode);
            }
            if (vueTemplate != null) {
                // columns
                String vueCode = vueTemplate.render(vars);
                generatorTable.setVueCode(vueCode);
            }
        }

        return generatorTable;
    }

    private static Map<String, GeneratorTableColumn> generateTableColumns(Connection connection, GeneratorTableOption tableOption) {
        final String tableName = tableOption.getTableName();
        final String querySql = String.format("select * from %s where 1 = 2 ", tableName);
        final Map<String, GeneratorTableColumn> tableColumns = new LinkedHashMap<String, GeneratorTableColumn>();
        try {
            PreparedStatement ps = connection.prepareStatement(querySql);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            String primaryColumnName = null;
            try {
                // 获取主键
                DatabaseMetaData databaseMetaData = connection.getMetaData();
                ResultSet pkRSet = databaseMetaData.getPrimaryKeys(null, null, tableName);
                if (pkRSet.next()) {
                    primaryColumnName = pkRSet.getString(4);
                }
                pkRSet.close();
            } catch (Throwable throwable) {
            }

            for (int i = 1; i <= columnCount; i++) {
                String columnName = rsmd.getColumnLabel(i);
                String columnCamelCase = StringUtils.getCamelCase(columnName);

                GeneratorTableColumn tableColumn = new GeneratorTableColumn();
                tableColumn.setColumnName(columnName);
                tableColumn.setJavaField(columnCamelCase);

                tableColumns.put(columnCamelCase, tableColumn);
                // 字段长度
                int size = rsmd.getColumnDisplaySize(i);
                // 字段类型
                int columnType = rsmd.getColumnType(i);
                tableColumn.setColumnType(columnType);
                tableColumn.setColumnSize(size);

                String javaType;
                switch (columnType) {
                    case Types.CHAR:
                    case Types.VARCHAR:
                    case Types.LONGVARCHAR:
                    case Types.NVARCHAR: {
                        javaType = "String";
                        tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_STRING);
                        break;
                    }
                    case Types.NUMERIC:
                    case Types.DECIMAL: {
                        javaType = "BigDecimal";
                        tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_NUMBER);
                        break;
                    }
                    case Types.BIT: {
                        javaType = "boolean";
                        tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_BOOL);
                        break;
                    }
                    case Types.TINYINT: {
                        javaType = "byte";
                        tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_NUMBER);
                        break;
                    }
                    case Types.SMALLINT: {
                        javaType = "short";
                        tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_NUMBER);
                        break;
                    }
                    case Types.INTEGER: {
                        javaType = "int";
                        tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_NUMBER);
                        break;
                    }
                    case Types.BIGINT: {
                        javaType = "long";
                        tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_NUMBER);
                        break;
                    }
                    case Types.REAL: {
                        javaType = "float";
                        tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_NUMBER);
                        break;
                    }
                    case Types.FLOAT:
                    case Types.DOUBLE: {
                        javaType = "double";
                        tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_NUMBER);
                        break;
                    }
                    case Types.LONGVARBINARY:
                    case Types.VARBINARY:
                    case Types.BINARY: {
                        javaType = "byte[]";
                        tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_BINARY);
                        break;
                    }
                    case Types.DATE:
                    case Types.TIME: {
                        javaType = "java.util.Date";
                        tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_DATE);
                        break;
                    }
                    case Types.TIMESTAMP: {
                        javaType = "java.sql.Timestamp";
                        tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_DATE);
                        break;
                    }
                    default: {
                        System.out.println(columnName);
                        throw new RuntimeException(" type  validate error ");
                    }
                }

//                if (columnType == Types.TIMESTAMP || columnType == Types.DATE) {
//                    javaType = "java.util.Date";
//                    tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_DATE);
//                } else if (columnType == Types.DOUBLE || columnType == Types.FLOAT) {
//                    javaType = "Double";
//                    tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_NUMBER);
//                } else if (columnType == Types.BIGINT) {
//                    javaType = "Long";
//                    tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_NUMBER);
//                } else if (columnType == Types.INTEGER) {
//                    javaType = "Integer";
//                    tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_NUMBER);
//                } else if (columnType == Types.VARCHAR || columnType == Types.NVARCHAR
//                        || columnType == Types.CLOB || columnType == Types.LONGVARCHAR) {
//                    javaType = "String";
//                    tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_STRING);
//                } else if (columnType == Types.CHAR) {
//                    // javaType = "boolean";
//                    javaType = "String";
//                    tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_STRING);
//                } else if (columnType == Types.LONGVARBINARY) {
//                    javaType = "byte[]";
//                    tableColumn.setJavaType(GeneratorTableColumn.JAVA_TYPE_BINARY);
//                } else {
//                    System.out.println(columnName);
//                    throw new RuntimeException(" type  validate error ");
//                }
                tableColumn.setJavaTypeName(javaType);
                if (primaryColumnName != null && primaryColumnName.equalsIgnoreCase(columnName)) {
                    tableColumn.setPrimary(true);
                }
            }
            rs.close();
            return tableColumns;
        } catch (Throwable throwable) {
            throw new SqlExecuteException(throwable.getMessage(), throwable);
        }
    }
}
