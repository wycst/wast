package org.framework.light.clients.redis.connection;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import org.framework.light.clients.redis.conf.RedisConfig;
import org.framework.light.clients.redis.data.future.RedisFuture;
import org.framework.light.clients.redis.exception.RedisConnectionException;
import org.framework.light.clients.redis.netty.RedisChannelPool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/**
 * redis连接池
 *
 * @Author: wangy
 * @Date: 2020/5/19 15:56
 * @Description:
 */
public class RedisConnectionPool extends AbstractConnectonPool {

    private ThreadLocal<CountDownLatch> currentCountDownLatchHolder = new ThreadLocal<CountDownLatch>();
    private AttributeKey<Boolean> loginFlag = AttributeKey.valueOf(Boolean.class, "loginFlag");

    public RedisConnectionPool(RedisConfig redisConfig) {
        super(new RedisChannelPool(redisConfig));
    }

    /**
     * 默认返回无阻塞的连接
     *
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public RedisConnection connection() throws InterruptedException, ExecutionException {
        return connection(false);
    }

    /**
     * 返回连接
     *
     * @param await 是否阻塞
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public RedisConnection connection(boolean await) throws InterruptedException, ExecutionException {

        CountDownLatch countDownLatch = currentCountDownLatchHolder.get();
        if (await) {
            if (countDownLatch != null && countDownLatch.getCount() > 0) {
                countDownLatch.await();
            }
            countDownLatch = new CountDownLatch(1);
            currentCountDownLatchHolder.set(countDownLatch);
        } else {
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
            currentCountDownLatchHolder.remove();
        }

        Channel channel = null;
        try {
            Future<Channel> f = acquire();
            channel = f.get();
        } catch (Throwable throwable) {
            if (await) {
                countDownLatch.countDown();
            }
            throw new ExecutionException(throwable);
        }

        PooledRedisConnection redisConnection = new PooledRedisConnection(channel, await);
        redisConnection.setNextCountDownLatch(countDownLatch);
        redisConnection.setChannelPool(getRedisChannelPool());

        // 判断是否登录
        final Attribute<Boolean> loginValue = channel.attr(loginFlag);
        if (loginValue.get() == null) {
            // await until finish login ops
            RedisFuture<String> redisFuture = redisConnection.auth(getAuth());
            String result = redisFuture.get();

            if (!"OK".equals(result)) {
                throw new RedisConnectionException(result);
            }

            // select default database
            redisFuture = redisConnection.select(getDatabase());
            redisFuture.get();
            loginValue.set(true);
        }

        return redisConnection;

    }

    public String getAuth() {
        return getRedisChannelPool().getRedisConfig().getPassword();
    }

    public String getHost() {
        return getRedisChannelPool().getRedisConfig().getHost();
    }

    public int getPort() {
        return getRedisChannelPool().getRedisConfig().getPort();
    }

    public int getDatabase() {
        return getRedisChannelPool().getRedisConfig().getDatabase();
    }
}
