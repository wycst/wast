package org.framework.light.clients.redis.test;

import org.framework.light.clients.redis.client.RedisClient;
import org.framework.light.clients.redis.client.SimpleRedisClient;
import org.framework.light.clients.redis.connection.RedisConnectionPool;
import org.framework.light.clients.redis.data.entry.UnionSortedSet;
import org.framework.light.clients.redis.options.Aggregate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @Author: wangy
 * @Date: 2020/5/16 19:03
 * @Description:
 */
public class RedisClientSetCommand {

    private static RedisConnectionPool redisConnectionPool;

    public static void main(String[] args) throws ExecutionException, InterruptedException {

//        final RedisClient redisClient = new SimpleRedisClient("localhost", 6379);
        final RedisClient redisClient = new SimpleRedisClient("49.233.166.49", 6379);
        redisClient.sync();
//        redisClient.auth("szlx@321");
        redisClient.auth("49#123456#49");

        redisClient.sAdd("testSet", "a", "x", "y", "z", "r");
        System.out.println(redisClient.sAdd("testSet1", "a", "b", "c"));
        System.out.println(redisClient.sAdd("testSet1", "a", "b", "c", "d"));
        System.out.println(redisClient.sAdd("testSet1", "e", "f", "g", "h"));

        System.out.println(redisClient.sCard("testSet1"));
        System.out.println(redisClient.sDiff("testSet", "testSet1"));
        System.out.println(redisClient.sDiffStore("testSet2", "testSet", "testSet1"));

        System.out.println(redisClient.sInter("testSet", "testSet1"));
        System.out.println(redisClient.sIsMember("testSet", "x"));
        System.out.println(redisClient.sMembers("testSet"));

        System.out.println(redisClient.sUnionStore("testSet3", "testSet", "testSet1"));

        System.out.println(redisClient.sScan("testSet3", 0, "a*", -1));
        System.out.println(redisClient.sScan("testSet3", 3, "a*", -1));

        redisClient.zAdd("zSetKey", 60, "a");
        redisClient.zAdd("zSetKey", 70, "rrt");
        redisClient.zAdd("zSetKey", 40, "uu");

        System.out.println(redisClient.zRange("zSetKey",0,-1));
        System.out.println(redisClient.zRange("zSetKey",0,-1,true));

        redisClient.zRank("zSetKey","uu");

        List<UnionSortedSet> unionSortedSets = new ArrayList<UnionSortedSet>();
        UnionSortedSet unionSortedSet = new UnionSortedSet();
        unionSortedSet.setKey("zSetKey");
        unionSortedSet.setWeight(3);
        unionSortedSets.add(unionSortedSet);

        redisClient.zUnionStore("LKDFDF", unionSortedSets, Aggregate.MAX);


        System.out.println("finish ");

    }

}
