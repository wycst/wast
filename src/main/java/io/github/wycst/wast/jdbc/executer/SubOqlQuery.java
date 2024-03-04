package io.github.wycst.wast.jdbc.executer;

/**
 * 子查询支持（where）
 *
 * @param <T>
 */
public class SubOqlQuery<T> extends OqlQuery {

    private final Class<T> subClass;

    SubOqlQuery(Class<T> subClass) {
        EntityManagementFactory.defaultManagementFactory().checkEntityClass(subClass);
        this.subClass = subClass;
    }

    public static <T> SubOqlQuery<T> create(Class<T> subClass) {
        return new SubOqlQuery<T>(subClass);
    }

    public EntitySqlMapping getSubEntitySqlMapping() {
        return EntityManagementFactory.defaultManagementFactory().getEntitySqlMapping(subClass);
    }
}
