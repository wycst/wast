package org.framework.light.clients.redis.test;

import org.framework.light.clients.redis.client.RedisClient;
import org.framework.light.clients.redis.client.SimpleRedisClient;
import org.framework.light.clients.redis.conf.RedisConfig;
import org.framework.light.clients.redis.connection.RedisConnection;
import org.framework.light.clients.redis.connection.RedisConnectionPool;
import org.framework.light.clients.redis.data.future.RedisFuture;

import java.util.concurrent.ExecutionException;

/**
 * @Author: wangy
 * @Date: 2020/5/16 19:03
 * @Description:
 */
public class RedisClientTest {

    private static RedisConnectionPool redisConnectionPool;

    static {

        RedisConfig redisConfig = new RedisConfig();
        redisConfig.setDatabase(1);
        redisConfig.setHost("49.233.166.49");
        redisConfig.setPort(6379);
        redisConfig.setPoolMaxActive(20);
        redisConfig.setPoolMaxWait(3000);
        redisConfig.setPassword("49#123456#49");
        redisConfig.setTimeout(30000);

        redisConnectionPool = new RedisConnectionPool(redisConfig);
    }

    RedisConnection redisConnection;

    {
        try {
            redisConnection = redisConnectionPool.connection(false);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private RedisFuture sendCmd(final String s) throws ExecutionException, InterruptedException {

        String[] commands = ((String) s).split("\\s+");
        RedisFuture redisFuture = redisConnection.writeAndFlush(commands);
        return redisFuture;
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {

//        RedisConnection conn = new SimpleRedisConnection("49.233.166.49",6379,true);
//        RedisFuture future1 = conn.writeAndFlush("auth 49#123456#49");
//        RedisFuture future2 = conn.writeAndFlush("1");
//        RedisFuture future3 = conn.writeAndFlush("get a");
//        conn.writeAndFlush("set a 5");
//        System.out.println("============= " + conn.writeAndFlush("get a").get());
//
//        conn.writeAndFlush("set a 6");
//
//        conn.close();
//
//        RedisBootstrap.bootstrap().shutdown();
//
//        if(true) {
//            return;
//        }

//        RedisClient redisClient = new PooledRedisClient(redisConnectionPool);

        RedisClient redisClient = new SimpleRedisClient(redisConnectionPool.getHost(), redisConnectionPool.getPort());
        redisClient.auth(redisConnectionPool.getAuth());
        redisClient.sync();

        long begin = System.currentTimeMillis();
        redisClient.pipeline();

        for (int i = 0; i < 11; i++) {
            redisClient.hashSet("persion-a", "name" + i, "zhangsan" + i);
        }

        // redisClient.cancelPipeline();

        redisClient.executePipeline();
        long end = System.currentTimeMillis();
        System.out.println("============= " + (end - begin));

        redisClient.close();

        RedisClientTest client = new RedisClientTest();
        long beginTime = System.currentTimeMillis();

        String str = "*2\r\n$4\r\nAUTH\r\n$12\r\n49#123456#49";
        str = "\r\n*3\r\n$3\r\nset\r\n$1\r\nu\r\n$1\r\n1";
        str += "\r\n*3\r\n$3\r\nset\r\n$1\r\nu\r\n$1\r\n1";
        str += "\r\n*3\r\n$3\r\nset\r\n$1\r\nu\r\n$1\r\n1";

        System.out.println("======= sendCmd ");
        client.sendCmd(str);

        System.out.println(client.sendCmd("set a 2").get());
        client.sendCmd("set a 2");
        System.out.println("========= " + client.sendCmd("get a"));
        System.out.println("========= " + client.sendCmd("get a"));
        System.out.println("========= " + client.sendCmd("get a"));
        client.sendCmd("get a");
        client.sendCmd("set a 3");
        client.sendCmd("get a");
        client.sendCmd("set a 4");
        client.sendCmd("get a");
        client.sendCmd("set a 5");
        client.sendCmd("get a");
        client.sendCmd("set a 6");
        RedisFuture redisFuture6 = client.sendCmd("get a");
        client.sendCmd("set a 7");
        client.sendCmd("get a");
        client.sendCmd("set a 8");
        RedisFuture redisFuture8 = client.sendCmd("get a");

        long endTime = System.currentTimeMillis();
        System.out.println(endTime - beginTime);

        System.out.println("hhh ");

        System.out.println("====================redisFuture6 " + redisFuture6.get());
        System.out.println("====================redisFuture8 " + redisFuture8.get());


        client.sendCmd("set a 1");
        client.sendCmd("set a 1");
        client.sendCmd("set a 5");
        client.sendCmd("set b 1");
        client.sendCmd("get a");
        client.sendCmd("get b");
        client.sendCmd("get  c");
        client.sendCmd("get  d");
        client.sendCmd("set a 7");
        System.out.println("====================redisFuture ffff " + client.sendCmd("get a").get());

        System.out.println("... over ....... ");
        // redisConnectionPool.close();
    }

}
