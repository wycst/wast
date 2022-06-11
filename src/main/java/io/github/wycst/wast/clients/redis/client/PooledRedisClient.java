package io.github.wycst.wast.clients.redis.client;

import io.github.wycst.wast.clients.redis.connection.RedisConnection;
import io.github.wycst.wast.clients.redis.connection.RedisConnectionHandler;
import io.github.wycst.wast.clients.redis.connection.RedisConnectionPool;
import io.github.wycst.wast.clients.redis.exception.RedisConnectionException;

/**
 * 基于连接池的redis客户端
 *
 * @Author: wangy
 * @Date: 2020/5/21 23:27
 * @Description:
 */
public class PooledRedisClient extends RedisClient {

    private RedisConnectionPool redisConnectionPool;
    // if pipeline or multi, A thread fixed connection is required
    private static ThreadLocal<RedisConnection> fixedConnectionHolder = new ThreadLocal<RedisConnection>();

    public PooledRedisClient(RedisConnectionPool redisConnectionPool) {
        this.redisConnectionPool = redisConnectionPool;
    }

    protected RedisConnection getConnection() {
        RedisConnection connection = getHoderConnection();
        if (connection == null) {
            connection = createConnection();
            fixedConnectionHolder.set(connection);
        }
        return connection;
    }

    protected RedisConnection createConnection() {
        try {
            return redisConnectionPool.connection(isSynchronized());
        } catch (Throwable e) {
            throw new RedisConnectionException(e);
        }
    }

    private RedisConnection getHoderConnection() {
        RedisConnection connection = fixedConnectionHolder.get();
        if (connection != null) {
            RedisConnectionHandler.signProgress(connection);
            if (connection.isClosed()) {
                return null;
            }
        }
        return connection;
    }

}
