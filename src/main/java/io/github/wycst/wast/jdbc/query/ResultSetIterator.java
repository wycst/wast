package io.github.wycst.wast.jdbc.query;

import io.github.wycst.wast.jdbc.exception.SqlExecuteException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * @Author wangyunchao
 * @Date 2021/8/27 17:19
 */
public class ResultSetIterator<E> implements Iterator {

    private final ResultSetMetaData rsmd;
    private ResultSet resultSet;
    private Class<E> entityClass;
    private Connection connection;
    private QueryExecutor queryExecutor;

    private boolean isClosed = false;

    ResultSetIterator(ResultSet resultSet, ResultSetMetaData rsmd, Class<E> cls, Connection connection, QueryExecutor queryExecutor) {
        this.resultSet = resultSet;
        this.rsmd = rsmd;
        this.entityClass = cls;
        this.connection = connection;
        this.queryExecutor = queryExecutor;
    }

    @Override
    public boolean hasNext() {
        checkConnection();
        try {
            boolean hasNext = resultSet.next();
            if (!hasNext) {
                close();
            }
            return hasNext;
        } catch (SQLException throwables) {
            close();
            return false;
        }
    }

    private void checkConnection() {
        if (this.isClosed) {
            throw new UnsupportedOperationException(" connection is closed ");
        }
    }

    public void close() {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException throwables) {
            }
            resultSet = null;
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException throwables) {
            }
            connection = null;
        }
        this.isClosed = true;
    }

    @Override
    public E next() {
        checkConnection();
        try {
            return (E) queryExecutor.parseResultSet(resultSet, rsmd, entityClass);
        } catch (Exception e) {
            close();
            throw new SqlExecuteException(e.getMessage(), e);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
