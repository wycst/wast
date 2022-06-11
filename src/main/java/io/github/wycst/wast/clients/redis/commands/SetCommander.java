package io.github.wycst.wast.clients.redis.commands;

import io.github.wycst.wast.clients.redis.data.entry.ScanEntry;

import java.util.List;

/**
 * @Author: wangy
 * @Date: 2020/6/27 13:46
 * @Description:
 */
public interface SetCommander {

    /**
     * SADD key member [member ...]
     * <p>
     * 将一个或多个 member 元素加入到集合 key 当中，已经存在于集合的 member 元素将被忽略。
     * <p>
     * 假如 key 不存在，则创建一个只包含 member 元素作成员的集合。
     *
     * @param key
     * @param values
     * @return
     */
    public long sAdd(String key, String... values);

    /**
     * SCARD key
     * <p>
     * 返回集合 key 的基数(集合中元素的数量)。
     *
     * @param key
     * @return
     */
    public long sCard(String key);

    /**
     * SDIFF key [key ...]
     * 返回一个集合的全部成员，该集合是所有给定集合之间的差集。
     *
     * @param key
     * @param keys
     * @return
     */
    public List<String> sDiff(String key, String... keys);

    /**
     * SDIFFSTORE destination key [key ...]
     * <p>
     * 这个命令的作用和 SDIFF 类似，但它将结果保存到 destination 集合，而不是简单地返回结果集。
     * <p>
     * 如果 destination 集合已经存在，则将其覆盖。
     * <p>
     * destination 可以是 key 本身。
     *
     * @param destination
     * @param key
     * @param keys
     * @return
     */
    public long sDiffStore(String destination, String key, String... keys);

    /**
     * SINTER key [key ...]
     * <p>
     * 返回一个集合的全部成员，该集合是所有给定集合的交集。
     * <p>
     * 不存在的 key 被视为空集。
     * <p>
     * 当给定集合当中有一个空集时，结果也为空集(根据集合运算定律)。
     *
     * @param key
     * @param keys
     * @return
     */
    public List<String> sInter(String key, String... keys);

    /**
     * SINTERSTORE destination key [key ...]
     * <p>
     * 这个命令类似于 SINTER 命令，但它将结果保存到 destination 集合，而不是简单地返回结果集。
     * <p>
     * 如果 destination 集合已经存在，则将其覆盖。
     * <p>
     * destination 可以是 key 本身。
     *
     * @param destination
     * @param key
     * @param keys
     * @return
     */
    public long sInterStore(String destination, String key, String... keys);

    /**
     * SISMEMBER key member
     * <p>
     * 判断 member 元素是否集合 key 的成员。
     *
     * @param key
     * @param member
     * @return
     */
    public boolean sIsMember(String key, String member);

    /**
     * SMEMBERS key
     * <p>
     * 返回集合 key 中的所有成员。
     * <p>
     * 不存在的 key 被视为空集合。
     *
     * @param key
     * @return
     */
    public List<String> sMembers(String key);

    /**
     * SMOVE source destination member
     * <p>
     * 将 member 元素从 source 集合移动到 destination 集合。
     * <p>
     * SMOVE 是原子性操作。
     * <p>
     * 如果 source 集合不存在或不包含指定的 member 元素，则 SMOVE 命令不执行任何操作，仅返回 0 。否则， member 元素从 source 集合中被移除，并添加到 destination 集合中去。
     * <p>
     * 当 destination 集合已经包含 member 元素时， SMOVE 命令只是简单地将 source 集合中的 member 元素删除。
     * <p>
     * 当 source 或 destination 不是集合类型时，返回一个错误。
     *
     * @param source
     * @param destination
     * @param member
     * @return
     */
    public boolean sMove(String source, String destination, String member);

    /**
     * SPOP key
     * <p>
     * 移除并返回集合中的一个随机元素。
     * <p>
     * 如果只想获取一个随机元素，但不想该元素从集合中被移除的话，可以使用 SRANDMEMBER 命令。
     *
     * @param key
     * @return
     */
    public String sPop(String key);

    /**
     * SRANDMEMBER key [count]
     * <p>
     * 如果命令执行时，只提供了 key 参数，那么返回集合中的一个随机元素。
     * <p>
     * 从 Redis 2.6 版本开始， SRANDMEMBER 命令接受可选的 count 参数：
     * <p>
     * 如果 count 为正数，且小于集合基数，那么命令返回一个包含 count 个元素的数组，数组中的元素各不相同。如果 count 大于等于集合基数，那么返回整个集合。
     * 如果 count 为负数，那么命令返回一个数组，数组中的元素可能会重复出现多次，而数组的长度为 count 的绝对值。
     * 该操作和 SPOP 相似，但 SPOP 将随机元素从集合中移除并返回，而 SRANDMEMBER 则仅仅返回随机元素，而不对集合进行任何改动。
     *
     * @param key
     * @return
     */
    public String sRandMember(String key);

    /**
     * SRANDMEMBER key [count]
     * <p>
     * 如果命令执行时，只提供了 key 参数，那么返回集合中的一个随机元素。
     * <p>
     * 从 Redis 2.6 版本开始， SRANDMEMBER 命令接受可选的 count 参数：
     * <p>
     * 如果 count 为正数，且小于集合基数，那么命令返回一个包含 count 个元素的数组，数组中的元素各不相同。如果 count 大于等于集合基数，那么返回整个集合。
     * 如果 count 为负数，那么命令返回一个数组，数组中的元素可能会重复出现多次，而数组的长度为 count 的绝对值。
     * 该操作和 SPOP 相似，但 SPOP 将随机元素从集合中移除并返回，而 SRANDMEMBER 则仅仅返回随机元素，而不对集合进行任何改动。
     *
     * @param key
     * @param count
     * @return
     */
    public List<String> sRandMember(String key, int count);

    /**
     * SREM key member [member ...]
     * <p>
     * 移除集合 key 中的一个或多个 member 元素，不存在的 member 元素会被忽略。
     * <p>
     * 当 key 不是集合类型，返回一个错误。
     *
     * @param key
     * @param member
     * @param members
     * @return
     */
    public long sRem(String key, String member, String... members);

    /**
     * SUNION key [key ...]
     * <p>
     * 返回一个集合的全部成员，该集合是所有给定集合的并集。
     * <p>
     * 不存在的 key 被视为空集。
     *
     * @param key
     * @param keys
     * @return
     */
    public List<String> sUnion(String key, String... keys);

    /**
     * SUNIONSTORE destination key [key ...]
     * <p>
     * 这个命令类似于 SUNION 命令，但它将结果保存到 destination 集合，而不是简单地返回结果集。
     * <p>
     * 如果 destination 已经存在，则将其覆盖。
     * <p>
     * destination 可以是 key 本身。
     *
     * @param destination
     * @param key
     * @param keys
     * @return
     */
    public long sUnionStore(String destination, String key, String... keys);

    /**
     * SSCAN 命令用于迭代当前数据库中的数据库键。
     *
     * @param cursor
     * @return
     */
    public ScanEntry sScan(String key, long cursor);

    /**
     * SSCAN 命令用于迭代当前数据库中的数据库键。
     *
     * @param cursor
     * @param pattern
     * @param count
     * @return
     */
    public ScanEntry sScan(String key, long cursor, String pattern, long count);

}
