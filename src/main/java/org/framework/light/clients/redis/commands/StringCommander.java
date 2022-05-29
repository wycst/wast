package org.framework.light.clients.redis.commands;

import org.framework.light.clients.redis.options.SetOptions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * redis字符串操作命令
 *
 * @Author: wangy
 * @Date: 2020/5/10 20:53
 * @Description:
 */
public interface StringCommander {

    /**
     * 给指定key设置value(永久)
     * 命令： SET key value
     *
     * @param key
     * @param value
     */
    public void set(String key, Object value);

    /**
     * 给指定key设置value并指定过期时间
     * 命令： SET key value PX milliseconds
     *
     * @param key
     * @param value
     * @param milliseconds 毫秒
     */
    public void set(String key, Object value, long milliseconds);

    /**
     * 给指定key设置value并指定过期时间
     * 命令： SET key value EX/PX count
     *
     * @param key
     * @param value
     * @param count
     * @param timeUnit
     */
    public void set(String key, Object value, long count, TimeUnit timeUnit);

    /**
     * 给指定key设置value并指定过期时间
     * 命令： SET key value EX/PX count NX/XX
     *
     * @param key
     * @param value
     * @param setOptions
     * @return 成功或失败
     */
    public boolean set(String key, Object value, SetOptions setOptions);

    /**
     * SETEX key seconds value
     * 将值 value 关联到 key ，并将 key 的生存时间设为 seconds (以秒为单位)。
     *
     * @param key
     * @param value
     * @param seconds
     */
    public void setex(String key, Object value, long seconds);

    /**
     * PSETEX key milliseconds value
     * 这个命令和 SETEX 命令相似，但它以毫秒为单位设置 key 的生存时间，而不是像 SETEX 命令那样，以秒为单位
     *
     * @param key
     * @param value
     * @param milliseconds
     */
    public void psetex(String key, Object value, long milliseconds);


    /**
     * 给指定key追加value,当key不存在时等价于set(String key, String value)
     * 命令： APPEND key value
     *
     * @param key
     * @param value
     */
    public long append(String key, Object value);

    /**
     * 给指定key设置value并返回原值,相当于先get再set
     * 命令： GETSET key value
     *
     * @param key
     * @param value
     */
    public String getset(String key, Object value);

    /**
     * 返回指定key值得长度
     * 命令： STRLEN key
     *
     * @param key
     */
    public long strLen(String key);

    /**
     * 获取指定key的值
     * 命令： GET key
     *
     * @param key
     */
    public String get(String key);

    /**
     * 返回key中字符串值的子字符串
     * 命令： GETRANGE key fromIndex toIndex
     *
     * @param key
     */
    public String getRange(String key, int fromIndex, int toIndex);

    /**
     * 批量设置key/value
     * 命令：MSET k1 v1 k2 v2 k3 v3 ...
     *
     * @param values
     */
    public void mset(Map<String, ? extends Object> values);

    /**
     * 批量设置key/value
     * 命令：MSET k1 v1 k2 v2 k3 v3 ...
     *
     * @param values
     */
    public boolean msetnx(Map<String, ? extends Object> values);

    /**
     * 批量获取value
     * 命令：MGET k1 k2 k3 ...
     *
     * @param keys
     */
    public List<String> mget(String... keys);

    /**
     * BITCOUNT key [start] [end]
     * <p>
     * 计算给定字符串中，被设置为 1 的比特位的数量。
     *
     * @param key
     * @return
     */
    public long bitcount(String key);

    /**
     * BITCOUNT key [start] [end]
     * <p>
     * 计算给定字符串中，被设置为 1 的比特位的数量。
     *
     * @param key
     * @param start
     * @param end
     * @return
     */
    public long bitcount(String key, int start, int end);

    /**
     * BITOP AND destkey key [key ...]
     * <p>
     * 对一个或多个保存二进制位的字符串 key 进行位元操作，并将结果保存到 destkey 上。
     *
     * @param destkey
     * @param keys    不能为空
     * @return
     */
    public long bitopAnd(String destkey, String... keys);

    /**
     * BITOP OR destkey key [key ...]
     * <p>
     * 对一个或多个保存二进制位的字符串 key 进行位元操作，并将结果保存到 destkey 上。
     *
     * @param destkey
     * @param keys    不能为空
     * @return
     */
    public long bitopOr(String destkey, String... keys);

    /**
     * BITOP XOR destkey key [key ...]
     * <p>
     * 对一个或多个保存二进制位的字符串 key 进行位元操作，并将结果保存到 destkey 上。
     *
     * @param destkey
     * @param keys    不能为空
     * @return
     */
    public long bitopXor(String destkey, String... keys);

    /**
     * BITOP NOT destkey key [key ...]
     * <p>
     * 对一个或多个保存二进制位的字符串 key 进行位元操作，并将结果保存到 destkey 上。
     *
     * @param destkey
     * @param key     不能为空
     * @return
     */
    public long bitopNot(String destkey, String key);

    /**
     * SETBIT key offset value
     * <p>
     * 对 key 所储存的字符串值，设置或清除指定偏移量上的位(bit)。
     *
     * @param key
     * @param offset 大于0小于Integer.MAX
     * @param value  0/1
     * @return
     */
    public long setbit(String key, int offset, int value);

    /**
     * GETBIT key offset
     * 对 key 所储存的字符串值，获取指定偏移量上的位(bit)。
     *
     * @param key
     * @param offset
     * @return
     */
    public long getbit(String key, int offset);

    /**
     * DECR key
     * 将 key 中储存的数字值减一
     *
     * @param key
     * @return
     */
    public long decr(String key);

    /**
     * INCR key
     * <p>
     * 将 key 中储存的数字值增一
     *
     * @param key
     * @return
     */
    public long incr(String key);

    /**
     * DECR key decrement
     * 将 key 中储存的数字值减一
     *
     * @param key
     * @param decrement
     * @return
     */
    public long decrby(String key, long decrement);

    /**
     * INCR key increment
     * <p>
     * 将 key 中储存的数字值增一
     *
     * @param key
     * @param increment
     * @return
     */
    public long incrby(String key, long increment);

    /**
     * INCR key increment
     * <p>
     * 将 key 中储存的数字值增一
     *
     * @param key
     * @param increment
     * @return
     */
    public double incrbyfloat(String key, double increment);

}
