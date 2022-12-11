package io.github.wycst.wast.jdbc.util;

/**
 * @Author wangyunchao
 * @Date 2021/8/27 17:17
 */
public interface StreamCursor<E> extends Iterable<E> {

    /**
     * 关闭游标
     */
    void close();

}
