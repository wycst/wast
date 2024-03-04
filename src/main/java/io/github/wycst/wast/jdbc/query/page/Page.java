package io.github.wycst.wast.jdbc.query.page;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

@SuppressWarnings({"unchecked"})
public abstract class Page<E> {

    private long page = 1;

    private int pageSize = 10;

    private List<E> rows;

    private long total;

    private Long offset;

    private Class<E> cls;

    public Page() {
    }

    public Page(Class<E> cls) {
        this.cls = cls;
    }

    public static <T> Page<T> pageInstance(Class<T> cls) {
        return new Page<T>(cls) {
        };
    }

    public static <T> Page<T> pageInstance(Class<T> cls, long pageNum, int pageSize) {
        Page<T> pageInstance = new Page<T>(cls) {
        };
        pageInstance.setPageSize(pageSize);
        pageInstance.setPage(pageNum);
        return pageInstance;
    }

    public Class<E> actualType() {
        if (this.cls != null) {
            return cls;
        }
        ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
        Type[] types = parameterizedType.getActualTypeArguments();
        if (types != null && types.length > 0) {
            if (types[0] instanceof Class) {
                return (Class<E>) types[0];
            }
        }
        return null;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<E> getRows() {
        return rows;
    }

    public void setRows(List<E> rows) {
        this.rows = rows;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getCurrentPage() {
        return this.page;
    }

    public long getOffset() {

        if (offset == null) {
            offset = (page - 1) * pageSize;
        }

        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

}
