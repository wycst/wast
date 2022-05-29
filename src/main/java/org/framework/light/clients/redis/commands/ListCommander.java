package org.framework.light.clients.redis.commands;

import org.framework.light.clients.redis.data.entry.KeyValueEntry;

import java.util.Collection;
import java.util.List;

/**
 * @Author: wangy
 * @Date: 2020/5/27 22:32
 * @Description:
 */
public interface ListCommander {

    /**
     * LPOP阻塞调用返回第一个弹出的值得列表key和值
     * 输入命令： BLPOP key [key ...] timeout
     *
     * @param keys
     * @return
     */
    public KeyValueEntry blpop(String...keys);

    /**
     * LPOP阻塞调用返回第一个弹出的值得列表key和值
     * 输入命令： BLPOP key [key ...] timeout
     *
     * @param keys
     * @return
     */
    public KeyValueEntry blpop(Collection<String> keys);

    /**
     * LPOP阻塞调用返回第一个弹出的值得列表key和值
     * 输入命令： BLPOP key [key ...] timeout
     *
     * @param keys
     * @param timeout 超时时间，单位秒；如果是0表示无限期
     * @return
     */
    public KeyValueEntry blpop(String[] keys, int timeout);

    /**
     * LPOP阻塞调用返回第一个弹出的值得列表key和值
     * 输入命令： BLPOP key [key ...] timeout
     *
     * @param keys
     * @param timeout
     * @return
     */
    public KeyValueEntry blpop(Collection<String> keys, int timeout);

    /**
     * 移除并返回列表 key 的头元素
     * 输入命令：LPOP key
     * 返回值：  列表左边被弹出的元素
     *
     * @param key
     * @return
     */
    public String lpop(String key);

    /**
     * 移除并返回列表 key 的尾部元素
     * 输入命令：RPOP key
     * 返回值：  列表右边被弹出的元素
     *
     * @param key
     * @return
     */
    public String rpop(String key);

    /**
     * 将一个或多个值 value 插入到列表 key 的表头
     * 输入命令：LPUSH key v1 v2 v3
     * 返回值：  列表元素数量
     *
     * @param key
     * @param values
     * @return
     */
    public long lpush(String key, String... values);

    /**
     * 将值 value 插入到列表 key 的表头，当且仅当 key 存在并且是一个列表
     * 输入命令：LPUSHX key value
     * 返回值：  列表元素数量
     *
     * @param key
     * @param value
     * @return
     */
    public long lpushx(String key, String value);

    /**
     * 将一个或多个值 value 插入到列表 key 的表尾(最右边)。
     * 输入命令：RPUSH key v1 v2 v3
     * 返回值：  列表右边被弹出的元素
     *
     * @param key
     * @param values
     * @return
     */
    public long rpush(String key, String... values);

    /**
     * 将值 value 插入到列表 key 的表尾，当且仅当 key 存在并且是一个列表
     * 输入命令：RPUSHX key value
     * 返回值：  列表元素数量
     *
     * @param key
     * @param value
     * @return
     */
    public long rpushx(String key, String value);


    /**
     * 返回列表 key 中，下标为 index 的元素
     * 输入命令：LINDEX key index
     * 返回值：  列表中下标为 index 的元素,如果超出区间返回null
     *
     * @param key
     * @return
     */
    public String lindex(String key, int index);

    /**
     * 将值 value 插入到列表 key 当中，位于值 pivot 之前或之后
     * 输入命令：LINSERT key BEFORE|AFTER pivot value
     *
     * @param key    列表的key 如果不存在返回0，如果类型错误抛出异常
     * @param direct 0 = 左边； 1 = 右边
     * @param pivot  列表中是否存在的值，如果不存在操作无效返回-1
     * @param value  待插入的值
     * @return
     */
    public long linsert(String key, int direct, String pivot, String value);

    /**
     * 返回列表 key 的长度
     * 输入命令：LLEN key
     *
     * @param key 列表的key 如果不存在返回0，如果类型错误抛出异常
     * @return
     */
    public long llen(String key);

    /**
     * 返回列表 key 的长度
     * 输入命令：LRANGE key start stop
     *
     * @param key   列表的key 如果不存在返回0，如果类型错误抛出异常
     * @param start
     * @param stop
     * @return
     */
    public List<String> lrange(String key, int start, int stop);

    /**
     * 根据参数 count 的值，移除列表中与参数 value 相等的元素
     * count > 0 : 从表头开始向表尾搜索，移除与 value 相等的元素，数量为 count 。
     * count < 0 : 从表尾开始向表头搜索，移除与 value 相等的元素，数量为 count 的绝对值。
     * count = 0 : 移除表中所有与 value 相等的值。
     * 输入命令： LREM key count value
     *
     * @param key
     * @param count
     * @param value
     * @return
     */
    public long lrem(String key, int count, String value);

    /**
     * 将列表 key 下标为 index 的元素的值设置为 value 。
     * 输入命令： LSET key index value
     *
     * @param key
     * @param index
     * @param value
     */
    public void lset(String key, int index, String value);

    /**
     * 对一个列表进行修剪(trim)，就是说，让列表只保留指定区间内的元素，不在指定区间之内的元素都将被删除。
     * 输入命令： LTRIM key start stop
     *
     * @param key
     * @param start
     * @param stop
     */
    public void ltrim(String key, int start, int stop);

    /**
     * 在一个原子时间：
     * 将列表 source 中的最后一个元素(尾元素)弹出，并返回给客户端。
     * 将 source 弹出的元素插入到列表 destination ，作为 destination 列表的的头元素。
     * 输入命令： RPOPLPUSH source destination
     *
     *
     * @param source
     * @param destination
     * @return
     */
    public String rpoplpush(String source, String destination);

}
