package org.framework.light.clients.redis.client;

import io.netty.channel.Channel;
import org.framework.light.clients.redis.connection.RedisConnection;
import org.framework.light.clients.redis.netty.RedisBootstrap;

/**
 * 基于单个连接的客户端
 *
 * @Author: wangy
 * @Date: 2020/5/21 23:22
 * @Description:
 */
public class SimpleRedisClient extends RedisClient {

    private RedisConnection connection;
    private String host;
    private int port;

    public SimpleRedisClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.connection = new SimpleRedisConnection(host, port);
    }

    protected RedisConnection getConnection() {
        return connection;
    }

    protected RedisConnection createConnection() {
        return connection;
    }

    class SimpleRedisConnection extends RedisConnection {

        SimpleRedisConnection(Channel channel, boolean await) {
            super(channel, await);
        }

        SimpleRedisConnection(String host, int port) {
            this(host, port, false);
        }

        public final boolean recycleable() {
            return false;
        }

        public SimpleRedisConnection(String host, int port, boolean await) {
            this(RedisBootstrap.bootstrap().channel(host, port), await);
        }

        @Override
        public void close() {
            getChannel().close();
            super.close();
        }
    }

}
