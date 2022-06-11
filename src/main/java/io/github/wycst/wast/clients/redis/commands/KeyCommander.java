package io.github.wycst.wast.clients.redis.commands;

import io.github.wycst.wast.clients.redis.data.entry.ScanEntry;
import io.github.wycst.wast.clients.redis.options.CommandOptions;
import io.github.wycst.wast.clients.redis.options.SortOptions;

import java.util.List;

/**
 * Redis key 操作命令
 *
 * @Author: wangy
 * @Date: 2020/5/10 13:24
 * @Description:
 */
public interface KeyCommander {

    /**
     * 删除指定的集合keys
     * 输入命令: DEL K1 K2 K3 ...
     * 返回值:   受影响数量，如果没有key，返回0
     *
     * @param keys
     */
    public long del(String... keys);

    /**
     * 判断指定key值是否存在
     * 输入命令: EXISTS k1
     * 返回值：  若 key 存在，返回 1 ，否则返回 0 。
     *
     * @param key
     * @return
     */
    public boolean exists(String key);


    /**
     * 当oldkey和newkey相同，或者oldkey不存在时，返回一个错误
     * 当newkey 已经存在时， RENAME 命令将覆盖旧值
     * 输入命令: RENAME oldKey newKey
     * 返回值：  OK/错误信息
     *
     * @param oldKey
     * @param newKey
     * @return
     */
    public boolean rename(String oldKey, String newKey);

    /**
     * 当且仅当 newkey 不存在时，将 key 改名为 newkey
     * 当 key 不存在时，返回一个错误
     * 输入命令： RENAMENX key newkey
     *
     * @param key
     * @param newKey
     * @return
     */
    public boolean renamenx(String key, String newKey);

    /**
     * 从当前数据库中随机返回(不删除)一个 key
     * <p>
     * 输入命令: RANDOMKEY
     * 返回值：  随机key
     *
     * @return
     */
    public String randomKey();

    /**
     * 设置指定key的过期时间
     * 输入命令：EXPIRE key1 100
     * 返回值：  返回 1成功，0 失败;
     *
     * @param key
     * @param seconds 单位秒
     * @return
     */
    public long expire(String key, long seconds);

    /**
     * 以秒为单位设置 key 的过期 unix 时间戳
     * 输入命令： EXPIREAT key timestamp
     * 返回值：  返回 1成功，0 失败;
     *
     * @param key
     * @param secondTimestamp 秒
     * @return
     */
    public long expireAt(String key, long secondTimestamp);

    /**
     * 设置指定key的过期时间
     * 输入命令：PEXPIRE key1 100
     * 返回值：  返回 1成功，0 失败;
     *
     * @param key
     * @param milliseconds 单位毫秒
     * @return
     */
    public long pexpire(String key, long milliseconds);

    /**
     * 这个命令和 EXPIREAT 命令类似，但它以毫秒为单位设置 key 的过期 unix 时间戳，而不是像 EXPIREAT 那样，以秒为单位。
     * 输入命令： PEXPIREAT key timestamp
     * 返回值：  返回 1成功，0 失败;
     *
     * @param key
     * @param millisecondTimestamp 毫秒
     * @return
     */
    public long pexpireAt(String key, long millisecondTimestamp);

    /**
     * 查询指定key的过期时间
     * 输入命令：TTL key1
     * 返回值：  单位秒;
     *
     * @param key
     * @return
     */
    public long ttl(String key);


    /**
     * 查询指定key的过期时间
     * 输入命令：PTTL key1
     * 返回值：  单位毫秒;
     *
     * @param key
     * @return
     */
    public long pttl(String key);

    /**
     * 序列化给定 key ，并返回被序列化的值，使用 RESTORE 命令可以将这个值反序列化为 Redis 键
     * 输入命令： DUMP key
     *
     * @param key
     * @return
     */
    public String dump(String key);

    /**
     * 反序列化给定的序列化值，并将它和给定的 key 关联。
     * 输入命令: RESTORE key ttl value
     *
     * @param key
     * @param ttl
     * @param value
     */
    public void restore(String key, long ttl, String value);

    /**
     * 查找所有符合给定模式 pattern 的 key
     * KEYS * 匹配数据库中所有 key 。
     * KEYS h?llo 匹配 hello ， hallo 和 hxllo 等。
     * KEYS h*llo 匹配 hllo 和 heeeeello 等。
     * KEYS h[ae]llo 匹配 hello 和 hallo ，但不匹配 hillo 。
     * 特殊符号用 \ 隔开
     *
     * @param pattern
     * @return
     */
    public List<String> keys(String pattern);

    /**
     * 将 key 原子性地从当前实例传送到目标实例的指定数据库上，一旦传送成功， key 保证会出现在目标实例上，而当前实例上的 key 会被删除
     * 输入命令： MIGRATE host port key destination-db timeout [COPY] [REPLACE]
     *
     * @param host
     * @param port
     * @param key
     * @param dbIndex 目标数据库索引（从0开始）
     * @param timeout 单位为毫秒
     */
    public void migrate(String host, int port, String key, int dbIndex, int timeout);

    /**
     * 将 key 原子性地从当前实例传送到目标实例的指定数据库上，一旦传送成功， key 保证会出现在目标实例上，而当前实例上的 key 会被删除
     * 输入命令： MIGRATE host port key destination-db timeout [COPY] [REPLACE]
     *
     * @param host
     * @param port
     * @param key
     * @param dbIndex            目标数据库索引（从0开始）
     * @param timeout            单位为毫秒
     * @param copySource
     * @param replaceDestination
     */
    public void migrate(String host, int port, String key, int dbIndex, int timeout, boolean copySource, boolean replaceDestination);

    /**
     * 将当前数据库的 key 移动到给定的数据库 db 当中
     * 输入命令： MOVE key dbIndex
     *
     * @param key
     * @param dbIndex
     * @return
     */
    public long move(String key, int dbIndex);

    /**
     * OBJECT 命令允许从内部察看给定 key 的 Redis 对象。
     * 输入命令： OBJECT subcommand [arguments [arguments]]
     *
     * @param commandOptions
     * @param key
     * @return
     */
    public <E> E object(CommandOptions<E> commandOptions, String key);

    /**
     * 移除给定 key 的生存时间，将这个 key 从『易失的』(带生存时间 key )转换成『持久的』(一个不带生存时间、永不过期的 key )
     * 输入命令： PERSIST key
     *
     * @param key
     * @return
     */
    public long persist(String key);

    /**
     * 输入命令： SORT key [BY pattern] [LIMIT offset count] [GET pattern [GET pattern ...]] [ASC | DESC] [ALPHA] [STORE destination]
     *
     * @param key
     * @param sortOptions
     * @return
     */
    public List<String> sort(String key, SortOptions sortOptions);

    /**
     * 输入命令： TYPE key
     * 返回值：
     * none (key不存在),string (字符串),list (列表),set (集合),zset (有序集),hash (哈希表)
     *
     * @param key
     * @return
     */
    public String type(String key);

    /**
     * SCAN 命令用于迭代当前数据库中的数据库键。
     *
     * @param cursor
     * @return
     */
    public ScanEntry scan(long cursor);

    /**
     * SCAN 命令用于迭代当前数据库中的数据库键。
     *
     * @param cursor
     * @param pattern
     * @param count
     * @return
     */
    public ScanEntry scan(long cursor, String pattern, long count);


}
