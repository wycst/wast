package io.github.wycst.wast.jdbc.query;

import io.github.wycst.wast.jdbc.util.StreamCursor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Iterator;

/**
 * @Author wangyunchao
 * @Date 2021/8/27 17:57
 */
public class StreamCursorImpl<E> implements StreamCursor {

    private final ResultSetIterator<E> iterator;

    public StreamCursorImpl(ResultSet resultSet, ResultSetMetaData rsmd, Class<E> cls, Connection connection, QueryExecutor queryExecutor) {
        iterator = new ResultSetIterator<E>(resultSet, rsmd, cls, connection, queryExecutor);
    }

    @Override
    public Iterator<E> iterator() {
        return iterator;
    }

    @Override
    public void close() {
        iterator.close();
    }
}
