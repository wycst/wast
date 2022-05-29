package org.framework.light.clients.redis.commands;

import org.framework.light.clients.redis.data.entry.ScanEntry;
import org.framework.light.clients.redis.data.entry.UnionSortedSet;
import org.framework.light.clients.redis.data.entry.ZSetMember;
import org.framework.light.clients.redis.options.Aggregate;
import org.framework.light.clients.redis.options.ScoreOptions;

import java.util.List;

/**
 * 有序集合
 *
 * @Author: wangy
 * @Date: 2020/6/28 15:53
 * @Description:
 */
public interface SortedSetCommander {


    /**
     * ZADD key score member [[score member] [score member] ...]
     * <p>
     * 将一个或多个 member 元素及其 score 值加入到有序集 key 当中。
     * <p>
     * 如果某个 member 已经是有序集的成员，那么更新这个 member 的 score 值，并通过重新插入这个 member 元素，来保证该 member 在正确的位置上。
     * <p>
     * score 值可以是整数值或双精度浮点数。
     * <p>
     * 如果 key 不存在，则创建一个空的有序集并执行 ZADD 操作。
     * <p>
     * 当 key 存在但不是有序集类型时，返回一个错误。
     * <p>
     * 对有序集的更多介绍请参见 sorted set 。
     *
     * @param key
     * @param score
     * @param member
     * @return
     */
    public long zAdd(String key, double score, String member);

    /**
     * ZADD key score member [[score member] [score member] ...]
     * <p>
     * 将一个或多个 member 元素及其 score 值加入到有序集 key 当中。
     * <p>
     * 如果某个 member 已经是有序集的成员，那么更新这个 member 的 score 值，并通过重新插入这个 member 元素，来保证该 member 在正确的位置上。
     * <p>
     * score 值可以是整数值或双精度浮点数。
     * <p>
     * 如果 key 不存在，则创建一个空的有序集并执行 ZADD 操作。
     * <p>
     * 当 key 存在但不是有序集类型时，返回一个错误。
     * <p>
     * 对有序集的更多介绍请参见 sorted set 。
     *
     * @param key
     * @param member
     * @param members
     * @return
     */
    public long zAdd(String key, ZSetMember member, ZSetMember... members);

    /**
     * ZCARD key
     * <p>
     * 返回有序集合 key 的基数(集合中元素的数量)。
     *
     * @param key
     * @return
     */
    public long zCard(String key);

    /**
     * ZCOUNT key min max
     * <p>
     * 返回有序集 key 中， score 值在 min 和 max 之间(默认包括 score 值等于 min 或 max )的成员的数量。
     * <p>
     * 关于参数 min 和 max 的详细使用方法，请参考 ZRANGEBYSCORE 命令。
     *
     * @param key
     * @return
     */
    public long zCount(String key, double min, double max);

    /**
     * ZINCRBY key increment member
     * <p>
     * 为有序集 key 的成员 member 的 score 值加上增量 increment 。
     * <p>
     * 可以通过传递一个负数值 increment ，让 score 减去相应的值，比如 ZINCRBY key -5 member ，就是让 member 的 score 值减去 5 。
     * <p>
     * 当 key 不存在，或 member 不是 key 的成员时， ZINCRBY key increment member 等同于 ZADD key increment member 。
     * <p>
     * 当 key 不是有序集类型时，返回一个错误。
     * <p>
     * score 值可以是整数值或双精度浮点数。
     *
     * @param key
     * @param increment
     * @param member
     * @return
     */
    public String zIncrby(String key, double increment, String member);

    /**
     * ZRANGE key start stop [WITHSCORES]
     * <p>
     * 返回有序集 key 中，指定区间内的成员。
     * <p>
     * 其中成员的位置按 score 值递增(从小到大)来排序。
     * <p>
     * 具有相同 score 值的成员按字典序(lexicographical order )来排列。
     * <p>
     * 如果你需要成员按 score 值递减(从大到小)来排列，请使用 ZREVRANGE 命令。
     * <p>
     * 下标参数 start 和 stop 都以 0 为底，也就是说，以 0 表示有序集第一个成员，以 1 表示有序集第二个成员，以此类推。
     * 你也可以使用负数下标，以 -1 表示最后一个成员， -2 表示倒数第二个成员，以此类推。
     * 超出范围的下标并不会引起错误。
     * 比如说，当 start 的值比有序集的最大下标还要大，或是 start > stop 时， ZRANGE 命令只是简单地返回一个空列表。
     * 另一方面，假如 stop 参数的值比有序集的最大下标还要大，那么 Redis 将 stop 当作最大下标来处理。
     * 可以通过使用 WITHSCORES 选项，来让成员和它的 score 值一并返回，返回列表以 value1,score1, ..., valueN,scoreN 的格式表示。
     * 客户端库可能会返回一些更复杂的数据类型，比如数组、元组等。
     *
     * @param key
     * @param start
     * @param stop
     * @return
     */
    public List<String> zRange(String key, long start, long stop);

    /**
     * ZRANGE key start stop [WITHSCORES]
     * <p>
     * 返回有序集 key 中，指定区间内的成员。
     * <p>
     * 其中成员的位置按 score 值递增(从小到大)来排序。
     * <p>
     * 具有相同 score 值的成员按字典序(lexicographical order )来排列。
     * <p>
     * 如果你需要成员按 score 值递减(从大到小)来排列，请使用 ZREVRANGE 命令。
     * <p>
     * 下标参数 start 和 stop 都以 0 为底，也就是说，以 0 表示有序集第一个成员，以 1 表示有序集第二个成员，以此类推。
     * 你也可以使用负数下标，以 -1 表示最后一个成员， -2 表示倒数第二个成员，以此类推。
     * 超出范围的下标并不会引起错误。
     * 比如说，当 start 的值比有序集的最大下标还要大，或是 start > stop 时， ZRANGE 命令只是简单地返回一个空列表。
     * 另一方面，假如 stop 参数的值比有序集的最大下标还要大，那么 Redis 将 stop 当作最大下标来处理。
     * 可以通过使用 WITHSCORES 选项，来让成员和它的 score 值一并返回，返回列表以 value1,score1, ..., valueN,scoreN 的格式表示。
     * 客户端库可能会返回一些更复杂的数据类型，比如数组、元组等。
     *
     * @param key
     * @param start
     * @param stop
     * @param withscores
     * @return
     */
    public List<? extends Object> zRange(String key, long start, long stop, boolean withscores);

    /**
     * ZRANGEBYSCORE key min max [WITHSCORES] [LIMIT offset count]
     * <p>
     * 返回有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员。有序集成员按 score 值递增(从小到大)次序排列。
     * <p>
     * 具有相同 score 值的成员按字典序(lexicographical order)来排列(该属性是有序集提供的，不需要额外的计算)。
     * <p>
     * 可选的 LIMIT 参数指定返回结果的数量及区间(就像SQL中的 SELECT LIMIT offset, count )，注意当 offset 很大时，定位 offset 的操作可能需要遍历整个有序集，此过程最坏复杂度为 O(N) 时间。
     *
     * @param key
     * @param min
     * @param max
     * @return
     */
    public List<? extends Object> zRangeByScore(String key, double min, double max, ScoreOptions scoreOptions);

    /**
     * ZRANGEBYSCORE key min max [WITHSCORES] [LIMIT offset count]
     * <p>
     * 返回有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员。有序集成员按 score 值递增(从小到大)次序排列。
     * <p>
     * 具有相同 score 值的成员按字典序(lexicographical order)来排列(该属性是有序集提供的，不需要额外的计算)。
     * <p>
     * 可选的 LIMIT 参数指定返回结果的数量及区间(就像SQL中的 SELECT LIMIT offset, count )，注意当 offset 很大时，定位 offset 的操作可能需要遍历整个有序集，此过程最坏复杂度为 O(N) 时间。
     *
     * @param key
     * @param minExpr
     * @param maxExpr
     * @param scoreOptions
     * @return
     */
    public List<? extends Object> zRangeByScore(String key, String minExpr, String maxExpr, ScoreOptions scoreOptions);

    /**
     * ZRANK key member
     * <p>
     * 返回有序集 key 中成员 member 的排名。其中有序集成员按 score 值递增(从小到大)顺序排列。
     * <p>
     * 排名以 0 为底，也就是说， score 值最小的成员排名为 0 。
     * <p>
     * 使用 ZREVRANK 命令可以获得成员按 score 值递减(从大到小)排列的排名。
     * <p>
     * 可用版本：
     * >= 2.0.0
     *
     * @param key
     * @param member
     * @return
     */
    public long zRank(String key, String member);

    /**
     * ZREM key member [member ...]
     * <p>
     * 移除有序集 key 中的一个或多个成员，不存在的成员将被忽略。
     * <p>
     * 当 key 存在但不是有序集类型时，返回一个错误。
     *
     * @param key
     * @param member
     * @param members
     * @return
     */
    public long zRem(String key, String member, String... members);

    /**
     * 根据排名区间删除
     * <p>
     * ZREMRANGEBYRANK key start stop
     * <p>
     * 移除有序集 key 中，指定排名(rank)区间内的所有成员。
     * <p>
     * 区间分别以下标参数 start 和 stop 指出，包含 start 和 stop 在内。
     * <p>
     * 下标参数 start 和 stop 都以 0 为底，也就是说，以 0 表示有序集第一个成员，以 1 表示有序集第二个成员，以此类推。
     * 你也可以使用负数下标，以 -1 表示最后一个成员， -2 表示倒数第二个成员，以此类推。
     * 可用版本：
     * >= 2.0.0
     *
     * @param key
     * @param start
     * @param stop
     * @return
     */
    public long zRemRangeByRank(String key, long start, long stop);


    /**
     * ZREMRANGEBYSCORE key min max
     * <p>
     * 移除有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员。
     * <p>
     * 自版本2.1.6开始， score 值等于 min 或 max 的成员也可以不包括在内，详情请参见 ZRANGEBYSCORE 命令。
     *
     * @param key
     * @param min
     * @param max
     * @return
     */
    public long zRemRangeByScore(String key, double min, double max);

    /**
     * ZREMRANGEBYSCORE key min max
     * <p>
     * 移除有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员。
     * <p>
     * 自版本2.1.6开始， score 值等于 min 或 max 的成员也可以不包括在内，详情请参见 ZRANGEBYSCORE 命令。
     *
     * @param key
     * @param minExpr
     * @param maxExpr
     * @return
     */
    public long zRemRangeByScore(String key, String minExpr, String maxExpr);

    /**
     * ZREVRANGE key start stop [WITHSCORES]
     * <p>
     * 返回有序集 key 中，指定区间内的成员。
     * <p>
     * 其中成员的位置按 score 值递减(从大到小)来排列。
     * 具有相同 score 值的成员按字典序的逆序(reverse lexicographical order)排列。
     * 除了成员按 score 值递减的次序排列这一点外， ZREVRANGE 命令的其他方面和 ZRANGE 命令一样。
     * <p>
     * 可用版本：
     * >= 1.2.0
     *
     * @param key
     * @param start
     * @param stop
     * @return
     */
    public List<String> zRevRange(String key, long start, long stop);

    /**
     * ZREVRANGE key start stop [WITHSCORES]
     * <p>
     * 返回有序集 key 中，指定区间内的成员。
     * <p>
     * 其中成员的位置按 score 值递减(从大到小)来排列。
     * 具有相同 score 值的成员按字典序的逆序(reverse lexicographical order)排列。
     * 除了成员按 score 值递减的次序排列这一点外， ZREVRANGE 命令的其他方面和 ZRANGE 命令一样。
     * <p>
     * 可用版本：
     * >= 1.2.0
     *
     * @param key
     * @param start
     * @param stop
     * @param withscores
     * @return
     */
    public List<? extends Object> zRevRange(String key, long start, long stop, boolean withscores);

    /**
     * ZREVRANGEBYSCORE key max min [WITHSCORES] [LIMIT offset count]
     * <p>
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员。有序集成员按 score 值递减(从大到小)的次序排列。
     * <p>
     * 具有相同 score 值的成员按字典序的逆序(reverse lexicographical order )排列。
     * <p>
     * 除了成员按 score 值递减的次序排列这一点外， ZREVRANGEBYSCORE 命令的其他方面和 ZRANGEBYSCORE 命令一样。
     * <p>
     * 可用版本：
     * >= 2.2.0
     *
     * @param key
     * @param min
     * @param max
     * @param scoreOptions
     * @return
     */
    public List<? extends Object> zRevRangeByScore(String key, double min, double max, ScoreOptions scoreOptions);

    /**
     * ZREVRANGEBYSCORE key max min [WITHSCORES] [LIMIT offset count]
     * <p>
     * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员。有序集成员按 score 值递减(从大到小)的次序排列。
     * <p>
     * 具有相同 score 值的成员按字典序的逆序(reverse lexicographical order )排列。
     * <p>
     * 除了成员按 score 值递减的次序排列这一点外， ZREVRANGEBYSCORE 命令的其他方面和 ZRANGEBYSCORE 命令一样。
     * <p>
     * 可用版本：
     * >= 2.2.0
     *
     * @param key
     * @param minExpr
     * @param maxExpr
     * @param scoreOptions
     * @return
     */
    public List<? extends Object> zRevRangeByScore(String key, String minExpr, String maxExpr, ScoreOptions scoreOptions);

    /**
     * ZREVRANK key member
     * <p>
     * 返回有序集 key 中成员 member 的排名。其中有序集成员按 score 值递减(从大到小)排序。
     * <p>
     * 排名以 0 为底，也就是说， score 值最大的成员排名为 0 。
     * <p>
     * 使用 ZRANK 命令可以获得成员按 score 值递增(从小到大)排列的排名。
     * <p>
     * 可用版本：
     * >= 2.0.0
     *
     * @param key
     * @param member
     * @return
     */
    public long zRevRank(String key, String member);

    /**
     * ZSCORE key member
     * <p>
     * 返回有序集 key 中，成员 member 的 score 值。
     * <p>
     * 如果 member 元素不是有序集 key 的成员，或 key 不存在，返回 nil 。
     * <p>
     * 可用版本：
     * >= 1.2.0
     *
     * @param key
     * @param member
     * @return
     */
    public String zScore(String key, String member);

    /**
     * ZUNIONSTORE destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]
     * <p>
     * 计算给定的一个或多个有序集的并集，其中给定 key 的数量必须以 numkeys 参数指定，并将该并集(结果集)储存到 destination 。
     * <p>
     * 默认情况下，结果集中某个成员的 score 值是所有给定集下该成员 score 值之 和 。
     * <p>
     * WEIGHTS
     * <p>
     * 使用 WEIGHTS 选项，你可以为 每个 给定有序集 分别 指定一个乘法因子(multiplication factor)，每个给定有序集的所有成员的 score 值在传递给聚合函数(aggregation function)之前都要先乘以该有序集的因子。
     * <p>
     * 如果没有指定 WEIGHTS 选项，乘法因子默认设置为 1 。
     * <p>
     * AGGREGATE
     * <p>
     * 使用 AGGREGATE 选项，你可以指定并集的结果集的聚合方式。
     * <p>
     * 默认使用的参数 SUM ，可以将所有集合中某个成员的 score 值之 和 作为结果集中该成员的 score 值；使用参数 MIN ，可以将所有集合中某个成员的 最小 score 值作为结果集中该成员的 score 值；而参数 MAX 则是将所有集合中某个成员的 最大 score 值作为结果集中该成员的 score 值。
     * <p>
     * 可用版本：
     * >= 2.0.0
     *
     * @param destination
     * @param unionSortedSets
     * @param aggregate
     * @return
     */
    public long zUnionStore(String destination, List<UnionSortedSet> unionSortedSets, Aggregate aggregate);

    /**
     *
     * ZINTERSTORE destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]
     *
     * 计算给定的一个或多个有序集的交集，其中给定 key 的数量必须以 numkeys 参数指定，并将该交集(结果集)储存到 destination 。
     *
     * 默认情况下，结果集中某个成员的 score 值是所有给定集下该成员 score 值之和.
     *
     * 关于 WEIGHTS 和 AGGREGATE 选项的描述，参见 ZUNIONSTORE 命令。
     *
     * 可用版本：
     * >= 2.0.0
     *
     * @param destination
     * @param unionSortedSets
     * @param aggregate
     * @return
     */
    public long zInterStore(String destination, List<UnionSortedSet> unionSortedSets, Aggregate aggregate);

    /**
     * ZSCAN 命令用于迭代当前数据库中的数据库键。
     *
     * @param cursor
     * @return
     */
    public ScanEntry zScan(String key, long cursor);

    /**
     * ZSCAN 命令用于迭代当前数据库中的数据库键。
     *
     * @param cursor
     * @param pattern
     * @param count
     * @return
     */
    public ScanEntry zScan(String key, long cursor, String pattern, long count);
}
