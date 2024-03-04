package io.github.wycst.wast.jdbc.dialect;

/**
 * 缺省方言
 *
 * @Author: wangy
 * @Date: 2023/2/27 20:14
 * @Description:
 */
public class DefaultDialect extends DialectImpl implements Dialect {

    public DefaultDialect() {
    }

    public DefaultDialect(PageDialectAgent pageDialectAgent) {
        this.setPageDialectAgent(pageDialectAgent);
    }

    @Override
    public String getLimitString(String sql, boolean hasOffset) {
        return getPageDialectAgent().getLimitSql(sql, hasOffset);
    }

    @Override
    public String getLimitString(String sql, long offset, int limit) {
        return getPageDialectAgent().getLimitSql(sql, offset, limit);
    }

}
