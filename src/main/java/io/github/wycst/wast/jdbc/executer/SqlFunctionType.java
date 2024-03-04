package io.github.wycst.wast.jdbc.executer;

/**
 * 按sql功能区分
 */
public enum SqlFunctionType {

    BASE_INSERT,
    SELECT_BY_ID,
    UPDATE_BY_ID,
    UPDATE_BY_PARAMS,
    DELETE_BY_ID,
    DELETE_BY_PARAMS
}
