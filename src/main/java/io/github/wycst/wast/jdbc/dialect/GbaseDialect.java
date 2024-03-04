package io.github.wycst.wast.jdbc.dialect;

/**
 * gbase supported
 *
 * @Author: wangy
 * @Date: 2023/2/27 21:19
 * @Description:
 */
public class GbaseDialect extends DialectImpl implements Dialect {

    private static char[] PAGE_CHARS_SKIP = " SKIP ? FIRST ?".toCharArray();
    private static char[] PAGE_CHARS = " FIRST ?".toCharArray();

    @Override
    public String getLimitString(String sql, boolean hasOffset) {
        sql = sql.trim();
        StringBuilder builder = new StringBuilder(sql);
        int insertIndex = builder.indexOf(" ");
        builder.insert(insertIndex, hasOffset ? PAGE_CHARS_SKIP : PAGE_CHARS);
        return builder.toString();
    }

    @Override
    public String getLimitString(String sql, long offset, int limit) {
        sql = sql.trim();
        StringBuilder builder = new StringBuilder(sql);
        int insertIndex = builder.indexOf(" ");
        StringBuilder childSequence = new StringBuilder();
        if (offset > 0) {
            childSequence.append(" SKIP ").append(offset).append(" FIRST ").append(limit);
        } else {
            childSequence.append(" FIRST ").append(limit);
        }
        builder.insert(insertIndex, childSequence);
        return builder.toString();
    }
}
