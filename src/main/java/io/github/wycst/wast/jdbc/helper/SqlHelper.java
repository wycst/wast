package io.github.wycst.wast.jdbc.helper;

import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.jdbc.commands.SqlExecuteCall;
import io.github.wycst.wast.jdbc.executer.DefaultSqlExecuter;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * sql帮助api
 *
 * @Author: wangy
 * @Date: 2021/2/14 17:16
 * @Description:
 */
public class SqlHelper {

    /**
     * 根据表名生成sql模板,实体类和映射等信息
     *
     * @param tableName
     * @return
     */
    public static Map<String, String> reverse(DefaultSqlExecuter sqlExecuter, final String tableName,
                                              final String[] includeInsertColumns, final String[] includeSelectColumns) {
        final Map<String, String> reverseResults = new HashMap<String, String>();
        final String querySql = String.format("select * from %s where 1 = 2 ", tableName);
        // 插入sql
        StringBuilder insertStatement = new StringBuilder("insert into " + tableName);
        final StringBuilder insertColumnNames = new StringBuilder();
        final StringBuilder columnValues = new StringBuilder();

        // 查询sql
        StringBuilder selectStatement = new StringBuilder("select  ");
        final StringBuilder selectColumnNames = new StringBuilder();

        final StringBuilder entityBuffer = new StringBuilder();
        entityBuffer.append("\nimport io.github.wycst.wast.jdbc.annotations.*;\n");
        entityBuffer.append("\nimport io.github.wycst.wast.jdbc.annotations.Id.GenerationType;\n");
        entityBuffer.append("@lombok.Data\n");
        entityBuffer.append("@Table(name = \"" + tableName + "\")\n");
        entityBuffer.append(
                "public class " + StringUtils.getCamelCase(tableName, true) + " implements java.io.Serializable {\n\n");

        sqlExecuter.executePipelined(new SqlExecuteCall<Object>() {
            @Override
            public Object execute(Connection connection) throws SQLException {
                try {
                    PreparedStatement ps = connection.prepareStatement(querySql);
                    ResultSet rs = ps.executeQuery();
                    ResultSetMetaData rsmd = rs.getMetaData();
                    int columnCount = rsmd.getColumnCount();

//                    String schemaName = rsmd.getSchemaName(0);
//                    String catalogName = rsmd.getCatalogName(0);
                    String primaryColumnName = null;
                    try {
                        // 获取主键
                        DatabaseMetaData databaseMetaData = connection.getMetaData();
                        ResultSet pkRSet = databaseMetaData.getPrimaryKeys(null, null, tableName);

                        if (pkRSet.next()) {
                            primaryColumnName = pkRSet.getString(4);
                        }
                        pkRSet.close();

                        // 根据表名获得外键
//            			ResultSet fks = databaseMetaData.getImportedKeys(null, null, tableName);
//            			ResultSetMetaData fkmd = fks.getMetaData();
//            			while(fks.next()){
//            				for(int i = 1;i <= fkmd.getColumnCount();i ++){
//            					System.out.println(fkmd.getColumnName(i)+"\t"+fks.getString(i));
//            				}
//            			}

                    } catch (Throwable throwable) {
                    }

                    int deleteColumnNameDotIndex = -1;
                    int deleteColumValueDotIndex = -1;
                    int deleteColumSelectDotIndex = -1;

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rsmd.getColumnLabel(i);
                        String columnCamelCase = StringUtils.getCamelCase(columnName);
                        if (includeInsertColumns == null || StringUtils.contains(includeInsertColumns, columnName)) {
                            insertColumnNames.append(columnName).append(",");
                            columnValues.append("#{" + columnCamelCase + "},");
                            deleteColumnNameDotIndex = insertColumnNames.length() - 1;
                            deleteColumValueDotIndex = columnValues.length() - 1;
                        }
                        if (includeSelectColumns == null || StringUtils.contains(includeSelectColumns, columnName)) {
                            selectColumnNames.append("t." + columnName + ",");
                            deleteColumSelectDotIndex = selectColumnNames.length() - 1;
                        }

                        // 字段长度
                        int size = rsmd.getColumnDisplaySize(i);
                        // 字段类型
                        int columnType = rsmd.getColumnType(i);
                        String javaType = "Integer";
                        if (columnType == Types.TIMESTAMP || columnType == Types.DATE) {
                            javaType = "java.util.Date";
                        } else if (columnType == Types.DOUBLE || columnType == Types.FLOAT) {
                            javaType = "Double";
                        } else if (columnType == Types.BIGINT) {
                            javaType = "Long";
                        } else if (columnType == Types.INTEGER) {
                            javaType = "Integer";
                        } else if (columnType == Types.VARCHAR || columnType == Types.NVARCHAR
                                || columnType == Types.CLOB || columnType == Types.LONGVARCHAR) {
                            javaType = "String";
                        } else if (columnType == Types.CHAR) {
                            // javaType = "boolean";
                            javaType = "String";
                        } else if (columnType == Types.LONGVARBINARY) {
                            javaType = "byte[]";
                        } else {
                            System.out.println(columnName);
                            throw new RuntimeException(" type  validate error ");
                        }

                        if (primaryColumnName != null && primaryColumnName.equalsIgnoreCase(columnName)) {
                            entityBuffer.append("    @Id\n");
                        }
                        entityBuffer.append("    @Column(name = \"" + columnName + "\")\n");
                        entityBuffer.append("    private " + javaType + " " + columnCamelCase + ";\n\n");

                        // 创建日期字段
                        if (columnCamelCase.indexOf("create") > -1 && javaType.equals("java.util.Date")) {
                            reverseResults.put("createDateColumn", columnCamelCase);
                        }
                        // 修改日期字段
                        if (columnCamelCase.matches(".*([uU]pdate|[mM]odify).*") && javaType.equals("java.util.Date")) {
                            reverseResults.put("updateDateColumn", columnCamelCase);
                        }

                    }
                    if (deleteColumnNameDotIndex > -1) {
                        insertColumnNames.deleteCharAt(deleteColumnNameDotIndex);
                    }
                    if (deleteColumValueDotIndex > -1) {
                        columnValues.deleteCharAt(deleteColumValueDotIndex);
                    }
                    if (deleteColumSelectDotIndex > -1) {
                        selectColumnNames.deleteCharAt(deleteColumSelectDotIndex);
                    }

                    rs.close();

                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
                return null;
            }
        });

        insertStatement.append("(" + insertColumnNames + ") values(" + columnValues + ")");
        reverseResults.put("insert-sql", insertStatement.toString());

        selectStatement.append(selectColumnNames).append(" from ").append(tableName).append(" t ");
        reverseResults.put("select-sql", selectStatement.toString());

        entityBuffer.append("\n");
        entityBuffer.append("}");
        reverseResults.put("entity-code", entityBuffer.toString());


        return reverseResults;
    }

    /**
     * 根据实体类生成sql模板
     *
     * @param tableEntity
     * @return
     */
    public static String generateSqlByEntity(Object tableEntity, final String[] includeInsertFields,
                                             final String[] includeSelectFields) {


        return null;
    }

}
