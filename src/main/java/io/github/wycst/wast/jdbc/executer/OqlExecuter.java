package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.jdbc.query.page.Page;
import io.github.wycst.wast.jdbc.util.StreamCursor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 面向对象查询执行器
 *
 * @Author: wangy
 * @Date: 2020/6/29 17:44
 * @Description:
 */
public interface OqlExecuter {

    /**
     * 获取注入的SqlExecutor
     *
     * @return
     */
    DefaultSqlExecuter getSqlExecuter();

    /**
     * 根据id查询对象，表结构通过cls解析
     *
     * @param cls
     * @param id
     * @param <E>
     * @return
     */
    public <E> E get(Class<E> cls, Serializable id);

    /**
     * 根据id查询对象，表结构通过cls解析
     * fetch决定是否加载与当前实体有关联的属性域
     *
     * @param cls
     * @param id
     * @param <E>
     * @param fetch 是否提取关联
     * @return
     */
    public <E> E get(Class<E> cls, Serializable id, boolean fetch);

    /**
     * 查询所有实体
     *
     * @param cls
     * @return
     */
    public <E> List<E> queryAll(Class<E> cls);

    /***
     * 获取条件对象（返回第一条）
     *
     * @param cls
     * @param params
     * @return
     */
    public <E> E queryOne(Class<E> cls, Map<String, Object> params);

    /***
     * 获取条件对象（返回第一条）
     *
     * @param cls
     * @param params
     * @return
     */
    public <E> E queryOne(Class<E> cls, E params);

    /**
     * 查询总数
     *
     * @param entityCls
     * @return
     * @param <E>
     */
    public <E> long queryCount(Class<E> entityCls);

    /**
     * 查询总数
     *
     * @param cls
     * @param params
     * @param <E>
     * @return
     */
    public <E> long queryCount(Class<E> cls, Map<String, Object> params);

    /**
     * 查询总数
     *
     * @param cls
     * @param params
     * @param <E>
     * @return
     */
    public <E> long queryCount(Class<E> cls, E params);

    /**
     * 查询总数
     *
     * @param cls
     * @param query
     * @param params
     * @return
     * @param <E>
     */
    public <E> long queryCount(Class<E> cls, OqlQuery query, Object params);

    /***
     * 获取唯一实体
     * <p> 如果不唯一抛出sql异常
     * @param cls
     * @param params
     * @return
     */
    public <E> E queryUnique(Class<E> cls, Map<String, Object> params);

    /***
     * 获取唯一实体
     * <p> 如果不唯一抛出sql异常
     * @param cls
     * @param params
     * @return
     */
    public <E> E queryUnique(Class<E> cls, E params);

    /***
     * 执行条件查询
     *
     * @param cls
     * @param query
     * @param params
     * @return
     */
    public <E> List<E> queryList(Class<E> cls, OqlQuery query, Object params);

    /**
     * 基于流式查询海里数据
     *
     * @param cls
     * @param params
     * @param <E>
     * @return
     */
    public <E> StreamCursor<E> queryStreamBy(Class<E> cls, Map<String, Object> params);

    /**
     * 基于流式查询海里数据
     *
     * @param cls
     * @param params
     * @param <E>
     * @return
     */
    public <E> StreamCursor<E> queryStreamBy(Class<E> cls, E params);

    /**
     * 基于流式查询海里数据
     *
     * @param cls
     * @param query
     * @param params
     * @param <E>
     * @return
     */
    public <E> StreamCursor<E> queryStream(Class<E> cls, OqlQuery query, Object params);

    /***
     * 执行map条件查询
     *
     * @param cls
     * @param params
     * @return
     */
    public <E> List<E> queryBy(Class<E> cls, Map<String, Object> params);

    /***
     * 执行实体非空条件查询
     *
     * @param cls
     * @param params
     * @return
     */
    public <E> List<E> queryBy(Class<E> cls, E params);

    /***
     * 根据id列表查询
     *
     * @param cls
     * @param ids
     * @return
     */
    public <E> List<E> queryByIds(Class<E> cls, List<? extends Serializable> ids);

    /***
     * 根据id列表查询
     *
     * @param cls
     * @param ids
     * @return
     */
    public <E> List<E> queryByIds(Class<E> cls, Serializable... ids);

    /**
     * 分页查询
     *
     * @param page
     * @param query
     * @param params
     * @return
     */
    public <E> Page<E> queryPage(Page<E> page, OqlQuery query, Object params);

    /**
     * 查询分页
     *
     * @param page
     * @return
     * @param <E>
     */
    public <E> Page<E> queryPage(Page<E> page);

    /**
     * 分页查询（map得key&value）
     *
     * @param page
     * @param params
     * @return
     */
    public <E> Page<E> queryPage(Page<E> page, Map<String, Object> params);

    /**
     * 分页查询(实体非空属性)
     *
     * @param page
     * @param params
     * @return
     */
    public <E> Page<E> queryPage(Page<E> page, E params);


    /**
     * 插入对象，表结构通过对象解析
     *
     * @param e
     * @param <E>
     * @return
     */
    public <E> Serializable insert(E e);

    /**
     * 插入对象列表，表结构通过对象解析
     *
     * @param list
     * @param <E>
     */
    public <E> void insertList(List<E> list);

    /**
     * 支持mysql的values列表批量插入
     * <p> 实体id策略可以自增或者指定，如果使用算法生成将回退到常规批量插入
     *
     * @param list
     * @param <E>
     */
    public <E> int mysqlBatchInsert(List<E> list);

    /**
     * 更新对象，表结构通过对象解析
     *
     * @param e
     * @param <E>
     * @return
     */
    public <E> int update(E e);

    /**
     * 更新对象（指定字段）
     *
     * @param e
     * @param fields
     * @param <E>
     * @return
     */
    <E> int updateFields(E e, String... fields);

    /**
     * 根据查询结果更新（指定字段）
     *
     * @param entityCls
     * @param query
     * @param entity
     * @param fields
     * @return
     * @param <E>
     */
    <E> int updateBy(Class<E> entityCls, OqlQuery query, E entity, String... fields);

    /**
     * 根据查询结果更新（指定字段）
     *
     * @param entityCls
     * @param query
     * @param params
     * @param fields
     * @return
     * @param <E>
     */
    <E> int updateBy(Class<E> entityCls, OqlQuery query, Map<String,Object> params, String... fields);

    /**
     * 更新对象（排除字段）
     *
     * @param e
     * @param isExclude
     * @param fields
     * @return
     * @param <E>
     */
    <E> int updateFields(E e, boolean isExclude, String... fields);

    /***
     * 更新对象（指定字段）
     *
     * @param e
     * @param fields
     * @param <E>
     * @return
     */
    public <E> int updateFields(E e, List<String> fields);

    /***
     * 更新对象
     *
     * @param e
     * @param fields
     * @param isExclude 是否排除或者包含指定字段
     * @param <E>
     * @return
     */
    public <E> int updateFields(E e, List<String> fields, boolean isExclude);

    /**
     * 删除实体的所有记录
     *
     * @param entityCls
     * @param <E>
     * @return
     */
    public <E> int deleteAll(Class<E> entityCls);

    /**
     * 删除对象
     *
     * @param cls
     * @param id
     * @param <E>
     */
    public <E> int delete(Class<E> cls, Serializable id);

    /**
     * 删除对象
     *
     * @param cls
     * @param id
     * @param <E>
     * @param cascade 级联删除
     */
    public <E> int delete(Class<E> cls, Serializable id, boolean cascade);

    /**
     * 删除列表对象
     *
     * @param list
     */
    public <E> int deleteList(List<E> list);

    /**
     * 根据ids列表删除
     *
     * @param cls
     * @param ids
     */
    public <E> int deleteByIds(Class<E> cls, List<? extends Serializable> ids);

    /**
     * 根据ids（可变）数组删除
     *
     * @param cls
     * @param ids
     */
    public <E> int deleteByIds(Class<E> cls, Serializable... ids);

    /**
     * 根据实体非空属性作为条件删除（eq）
     *
     * @param cls
     * @param params
     * @param <E>
     */
    public <E> int deleteBy(Class<E> cls, E params);

    /**
     * 根据map参数(key为字段，value为值)作为条件删除（eq）
     *
     * @param cls
     * @param params
     * @param <E>
     */
    public <E> int deleteBy(Class<E> cls, Map<String, Object> params);

    /**
     * 根据自定义查询条件删除（先查询再通过id删除）
     *
     * @param cls
     * @param query  查询器
     * @param params map或者对象
     * @param <E>
     */
    public <E> int deleteBy(Class<E> cls, OqlQuery query, Object params);

    /**
     * 反转生成删除sql
     *
     * @param e
     * @param <E>
     * @return
     */
    public <E> String reverseDeleteSQL(E e);

    /**
     * 反转生成插入sql
     *
     * @param e
     * @param <E>
     * @return
     */
    public <E> String reverseInsertSQL(E e);

}
