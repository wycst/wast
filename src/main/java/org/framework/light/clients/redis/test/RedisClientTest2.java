package org.framework.light.clients.redis.test;

import org.framework.light.clients.redis.client.RedisClient;
import org.framework.light.clients.redis.client.SimpleRedisClient;
import org.framework.light.clients.redis.listener.Subscriber;
import org.framework.light.clients.redis.netty.RedisBootstrap;
import org.framework.light.clients.redis.options.CommandOptions;
import org.framework.light.clients.redis.conf.RedisConfig;
import org.framework.light.clients.redis.connection.RedisConnectionPool;
import org.framework.light.clients.redis.options.SetOptions;
import org.framework.light.clients.redis.options.SortOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @Author: wangy
 * @Date: 2020/5/16 19:03
 * @Description:
 */
public class RedisClientTest2 {

    private static RedisConnectionPool redisConnectionPool;

    static {

        RedisConfig redisConfig = new RedisConfig();
        redisConfig.setDatabase(1);
        redisConfig.setHost("49.233.166.49");
        redisConfig.setPort(6379);
        redisConfig.setPoolMaxActive(1000);
        redisConfig.setPoolMaxWait(3000);
        redisConfig.setPassword("49#123456#49");
        redisConfig.setTimeout(30000);

        redisConnectionPool = new RedisConnectionPool(redisConfig);


        for(int i = 0 ; i < 10; i ++) {
            try {
                redisConnectionPool.connection(false);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        System.out.println("ssss");

    }


    public static void main(String[] args) throws ExecutionException, InterruptedException {

//        final RedisClient redisClient = new SimpleRedisClient("localhost", 6379);
        final RedisClient redisClient = new SimpleRedisClient("192.168.30.10", 6379);
        redisClient.auth("10#123456");
//        final RedisClient redisClient = new SimpleRedisClient("49.233.166.49", 6379);
//        redisClient.auth("49#123456#49");
        redisClient.sync();
//        RedisClient redisClient = new PooledRedisClient(redisConnectionPool);
//        redisClient.sync();

        // redisClient.sync().set("user","张三");

        String user = redisClient.get("user");
        System.out.println("===============user1  " + user);
        redisClient.set("user","aaaaaaa");

        redisClient.pipeline();
        redisClient.set("user","bbbbbb张三bccccccc2");
        redisClient.set("user","bbbbbbbccccccc1");
        redisClient.executePipeline();

        user = redisClient.get("user");
        System.out.println("===============user2  " + user);


        user = redisClient.get("user");
        System.out.println("===============user3  " + user);

        redisClient.hashSet("persion-a","bb", "1");
        redisClient.hashSet("persion-a","ee", "2");
        System.out.println(redisClient.hashSet("persion-a","ff", ""));


        Map<String, String> vs = redisClient.hashGetAll("persion-a");
        System.out.println("hashGetAll:" + vs);

        List l = redisClient.hashMultiGet("persion-a","bb","ee","fff");
        System.out.println(l);

        List keys = redisClient.hashKeys("persion-a");
        System.out.println("hashKeys:" + keys);

        List vals = redisClient.hashVals("persion-a");
        System.out.println("hashVals:" + vals);

        long len = redisClient.hashLen("persion-a");
        System.out.println("==== len " + len);


        System.out.println(redisClient.exists("pppewe"));
        System.out.println(redisClient.exists("persion-a"));

        redisClient.rename("user1","user4");

        redisClient.expire("user4",20);

        Map<String,Object> values = new HashMap<String, Object>();
        values.put("aaa1q","bbb1");
        values.put("aaa2q","bbb2");
        values.put("aaa3q","bbb3");

        boolean ss = redisClient.msetnx(values);
        System.out.println(ss);

        List<String> values2 = redisClient.mget("aaa1","aaa2","user3");
        System.out.println(values2);

        redisClient.lpush("testlist1","a","b","c");
        redisClient.lpush("testlist2","d","e","f");

        redisClient.rpoplpush("testlist1","testlist2");

        redisClient.set("msg","haha");

//        redisClient.migrate("127.0.0.1", 6379, "msg", 3,2000,true,true);
        redisClient.move("msg",3);

        redisClient.set("age","201");
        System.out.println(redisClient.get("age"));
        System.out.println(redisClient.object(CommandOptions.REFCOUNT,"ag33e"));

        SortOptions sortOptions = new SortOptions();
        sortOptions.alpha(true).by("sfdf").limit(0,6).desc();
        System.out.println(redisClient.sort("testlist2",sortOptions));


        redisClient.multi();
        redisClient.set("a","b1");
        redisClient.set("c","d1");
        redisClient.get("");
        System.out.println("============== > empty " + redisClient.del(""));
        redisClient.exec();

        System.out.println("============ type1 " + redisClient.type("testlist1sss"));
        System.out.println("============ type2 " + redisClient.type("persion-a"));

        System.out.println("============ scan " + redisClient.scan(0));
        System.out.println("============ scan " + redisClient.scan(15));

        System.out.println("----- set " + redisClient.set("qwessd222sdqwwews","sdsds1",new SetOptions().xx(true)));

        Object rrr = redisClient.incrbyfloat("asqwerettyy",0.01);


        redisClient.publish("a.ssds","aa ");
        redisClient.publish("b.3","abbbbbb ");
        redisClient.publish("c.3","hallpererereraaa ");
        redisClient.publish("bbc","bbbbbbbbbbbbbbbbbbbbbbbbcccccccccc ");

        System.out.println(redisClient.pubsubChannels(null));
        System.out.println(redisClient.pubsubNumsub("bbc","bbe"));
        System.out.println(redisClient.pubsubNumpat());
        System.out.println("========== 阻塞完成");

        redisClient.close();
        RedisBootstrap.bootstrap().shutdown();

        System.exit(0);


    }

}
