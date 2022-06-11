package io.github.wycst.wast.clients.redis.commands;

import io.github.wycst.wast.clients.redis.data.entry.ScanEntry;

import java.util.List;
import java.util.Map;

/**
 * hash操作命令
 *
 * @Author: wangy
 * @Date: 2020/5/10 23:23
 * @Description:
 */
public interface HashCommander {

    /**
     * 给指定hash 设置field/value
     * 命令： HSET key field value
     *
     * @param key
     * @param value
     */
    public long hashSet(String key, String field, Object value);

    /**
     * HSETNX key field value
     * 将哈希表 key 中的域 field 的值设置为 value ，当且仅当域 field 不存在。
     *
     * @param key
     * @param field
     * @param value
     * @return
     */
    public boolean hashSetnx(String key, String field, Object value);

    /**
     * 获取hash指定fields的值
     * 命令： HMSET key field_i value_i
     *
     * @param key
     * @param values
     */
    public void hashMultiSet(String key, Map<String, ? extends Object> values);

    /**
     * 获取hash指定field的值
     * 命令： HGET key field
     *
     * @param key
     * @param field
     */
    public String hashGet(String key, String field);

    /**
     * 获取hash指定fields的值
     * 命令： HMGET key field1,field2,field3
     *
     * @param key
     * @param fields
     */
    public List<String> hashMultiGet(String key, String... fields);

    /**
     * 获取hash所有field的值
     * 命令： HGETALL key
     *
     * @param key
     */
    public Map<String, String> hashGetAll(String key);

    /**
     * 获取hash所有key的集合
     * 命令： HKEYS key
     *
     * @param key
     */
    public List<String> hashKeys(String key);

    /**
     * 获取hash所有value的集合
     * 命令： HVALS key
     *
     * @param key
     */
    public List<String> hashVals(String key);


    /**
     * 获取hash的size
     * 命令： HLEN key
     *
     * @param key
     */
    public long hashLen(String key);

    /**
     * 删除hash中指定fields
     * 命令： HDEL key field1 field2 field3 ...
     *
     * @param key
     * @param fields
     * @return
     */
    public long hashDel(String key, String... fields);


    /**
     * 判断指定key的field是否存在
     * 命令： HEXISTS key field
     *
     * @param key
     * @param field
     * @return
     */
    public boolean hashExists(String key, String field);

    /**
     * HINCRBY key field increment
     * 为哈希表 key 中的域 field 的值加上增量 increment 。
     *
     * @param key
     * @param field
     * @param increment
     * @return
     */
    public long hashIncrby(String key, String field, long increment);

    /**
     * HINCRBYFLOAT key field increment
     * <p>
     * 为哈希表 key 中的域 field 加上浮点数增量 increment 。
     *
     * @param key
     * @param field
     * @param increment
     * @return
     */
    public double hashIncrbyfloat(String key, String field, double increment);

    /**
     * HSCAN 命令用于迭代当前数据库中的数据库键。
     *
     * @param cursor
     * @return
     */
    public ScanEntry hashScan(String key, long cursor);

    /**
     * HSCAN 命令用于迭代当前数据库中的数据库键。
     *
     * @param cursor
     * @param pattern
     * @param count
     * @return
     */
    public ScanEntry hashScan(String key, long cursor, String pattern, long count);

}
