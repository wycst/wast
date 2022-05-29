package org.framework.light.clients.redis.client;

import org.framework.light.clients.redis.commands.*;
import org.framework.light.clients.redis.connection.RedisConnection;
import org.framework.light.clients.redis.data.future.RedisFuture;
import org.framework.light.clients.redis.exception.RedisException;

/**
 * @Author: wangy
 * @Date: 2020/5/22 9:16
 * @Description:
 */
public abstract class ClientCommander implements
        KeyCommander,
        HashCommander,
        StringCommander,
        ListCommander,
        SetCommander,
        SortedSetCommander,
        PubSubCommander,
        TransactionCommander {

    // 订阅/取消
    private RedisConnection sharedConnection;
    private static ThreadLocal<CommandRuntimeEnv> runtimEnvHolder = new ThreadLocal<CommandRuntimeEnv>();

    protected abstract RedisConnection getConnection();

    protected abstract RedisConnection createConnection();

    protected synchronized final RedisConnection getSharedConnection() {
        if (sharedConnection == null) {
            sharedConnection = createConnection();
        }
        return sharedConnection;
    }

    public final KeyCommander keyCommander() {
        return this;
    }

    public final HashCommander hashCommander() {
        return this;
    }

    public final StringCommander stringCommander() {
        return this;
    }

    public final ListCommander listCommander() {
        return this;
    }

    public final SetCommander setCommander() {
        return this;
    }

    public final PubSubCommander pubsubCommander() {
        return this;
    }

    private CommandRuntimeEnv getCommandRuntimeEnv() {
        CommandRuntimeEnv commandRuntimeEnv = runtimEnvHolder.get();
        if (commandRuntimeEnv == null) {
            commandRuntimeEnv = new CommandRuntimeEnv();
            runtimEnvHolder.set(commandRuntimeEnv);
        }
        return commandRuntimeEnv;
    }

    public final ClientCommander pipeline() {
        getCommandRuntimeEnv().setPipelined(true);
        getConnection().pipeline();
        return this;
    }


    public final ClientCommander closeSync() {
        getCommandRuntimeEnv().setSynchronized(false);
        getConnection().closeSync();
        return this;
    }

    public final ClientCommander sync() {
        getCommandRuntimeEnv().setSynchronized(true);
        getConnection().sync();
        return this;
    }

    protected final boolean isPipelined() {
        return getCommandRuntimeEnv().isPipelined();
    }

    protected final boolean isSynchronized() {
        return getCommandRuntimeEnv().isSynchronized();
    }

    protected final boolean isMulti() {
        return getCommandRuntimeEnv().isMulti();
    }

    public void executePipeline() {
        if (isPipelined()) {
            getConnection().executePipeline();
            getCommandRuntimeEnv().setPipelined(false);
        }
    }

    protected final void beginMulti() {
        getCommandRuntimeEnv().setMulti(true);
    }

    protected final void endMulti() {
        getCommandRuntimeEnv().setMulti(false);
    }

    public void cancelPipeline() {
        getConnection().cancelPipeline();
        getCommandRuntimeEnv().setPipelined(false);
    }

    public void close() {
        getConnection().close();
    }

    protected boolean validateCommand(String command) {
        return command != null && command.trim().length() > 0;
    }

    protected String[] commands(String... commands) {
        return commands;
    }

    RedisFuture executeRedisCommand(String[] commands) {
        RedisConnection redisConnection = getConnection();
        redisConnection.write(commands);
        if (!isPipelined()) {
            return redisConnection.flush();
        }
        return null;
    }

    <E> E executeRedisCommand(String[] commands, Class<E> returnType, E asyncDefaultVal) {
        RedisFuture redisFuture = executeRedisCommand(commands);
        if (redisFuture != null) {
            if (isSynchronized()) {
                Object result = redisFuture.getResult();
                if(isMulti()) {
                    return asyncDefaultVal;
                }
                checkIfTypeError(result, returnType);
                return (E) result;
            }
        }
        return asyncDefaultVal;
    }

    private void checkIfTypeError(Object result, Class type) {
        if (result != null && !type.isInstance(result)) {
            throw new RedisException("Error Type:" + result.toString());
        }
    }


}
