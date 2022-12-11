package io.github.wycst.wast.jdbc.commands;

import io.github.wycst.wast.jdbc.connection.ConnectionWraper;

import java.sql.SQLException;

public abstract class OperationSqlExecuteCommand<T> {
    public abstract T doExecute(ConnectionWraper wraper) throws SQLException;

    public boolean closeable() {
        return true;
    }
}
