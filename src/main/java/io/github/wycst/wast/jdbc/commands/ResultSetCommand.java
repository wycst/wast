package io.github.wycst.wast.jdbc.commands;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public interface ResultSetCommand<E> {

    public E doResultSet(ResultSet resultSet, ResultSetMetaData rsmd) throws Exception;

}
