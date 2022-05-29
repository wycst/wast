package org.framework.light.clients.redis.commands;

/**
 * 事务命令
 *
 * @Author: wangy
 * @Date: 2020/6/2 9:35
 * @Description:
 */
public interface TransactionCommander {

    /**
     * MULTI
     * <p>
     * 标记一个事务块的开始。
     * 事务块内的多条命令会按照先后顺序被放进一个队列当中，最后由 EXEC 命令原子性(atomic)地执行。
     */
    public void multi();

    /**
     * EXEC
     * <p>
     * 执行所有事务块内的命令。
     * <p>
     * 假如某个(或某些) key 正处于 WATCH 命令的监视之下，且事务块中有和这个(或这些) key 相关的命令，那么 EXEC 命令只在这个(或这些) key 没有被其他命令所改动的情况下执行并生效，否则该事务被打断(abort)。
     *
     * @return
     */
    public Object exec();

    /**
     * DISCARD
     * <p>
     * 取消事务，放弃执行事务块内的所有命令。
     * <p>
     * 如果正在使用 WATCH 命令监视某个(或某些) key，那么取消所有监视，等同于执行命令 UNWATCH 。
     */
    public void discard();

    /**
     * WATCH key [key ...]
     * <p>
     * 监视一个(或多个) key ，如果在事务执行之前这个(或这些) key 被其他命令所改动，那么事务将被打断。
     *
     * @param keys
     */
    public void watch(String... keys);

    /**
     * UNWATCH
     * <p>
     * 取消 WATCH 命令对所有 key 的监视。
     */
    public void unwatch();
}
