package io.github.wycst.wast.jdbc.dialect;

/**
 * oracle 语言
 *
 * @author wangyunchao
 */
public class OracleDialect extends DialectImpl implements Dialect {

    public String getLimitString(String sql, boolean hasOffset) {

        sql = sql.trim();
        boolean isForUpdate = false;
        if (sql.toLowerCase().endsWith(" for update")) {
            sql = sql.substring(0, sql.length() - 11);
            isForUpdate = true;
        }

        StringBuilder pagingSelect = new StringBuilder(sql.length() + 100);
        if (hasOffset) {
            pagingSelect
                    .append("select * from ( select row_.*, rownum rownum_ from ( ");
        } else {
            pagingSelect.append("select * from ( ");
        }
        pagingSelect.append(sql);
        if (hasOffset) {
            pagingSelect.append(" ) row_ where rownum < ?) where rownum_ > =?");
        } else {
            pagingSelect.append(" ) where rownum < ?");
        }

        if (isForUpdate) {
            pagingSelect.append(" for update");
        }
        return pagingSelect.toString();
    }

    public String getLimitString(String sql, long offset, int limit) {
        sql = sql.trim();
        boolean isForUpdate = false;
        if (sql.toLowerCase().endsWith(" for update")) {
            sql = sql.substring(0, sql.length() - 11);
            isForUpdate = true;
        }

        StringBuilder pagingSelect = new StringBuilder(sql.length() + 100);
        if (offset > 1) {
            pagingSelect
                    .append("select * from ( select row_.*, rownum rownum_ from ( ");
        } else {
            // pagingSelect.append("select * from ( ");
            pagingSelect
                    .append("select * from ( select row_.*, rownum rownum_ from ( ");
        }
        pagingSelect.append(sql);

        if (offset > 1) {
            pagingSelect.append(" ) row_ where rownum <= " + (offset + limit)
                    + ") where rownum_ >" + offset);
        } else {
            // pagingSelect.append(" ) where rownum <= "+limit);
            pagingSelect.append(" ) row_ where rownum <= " + (offset + limit)
                    + ") where rownum_ >" + offset);
        }

        if (isForUpdate) {
            pagingSelect.append(" for update");
        }

        return pagingSelect.toString();
    }

    public boolean supportsLimit() {
        return true;
    }

}
