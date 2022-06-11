package io.github.wycst.wast.clients.redis.test;

import io.github.wycst.wast.clients.redis.client.RedisClient;
import io.github.wycst.wast.clients.redis.client.SimpleRedisClient;

/**
 * @Author: wangy
 * @Date: 2020/9/2 15:30
 * @Description:
 */
public class RedisClientTest3 {

    public static void main(String[] args) {

        final RedisClient redisClient = new SimpleRedisClient("121.89.210.22", 8789);
        redisClient.sync();
//        redisClient.auth("szlx@321");
        redisClient.auth("!QAZ@WSX123");


        String ss = redisClient.get("ab8533ed-4574-4b96-8ff6-24c4ff0ae99b");
        System.out.println(ss);

        redisClient.close();

    }


}
