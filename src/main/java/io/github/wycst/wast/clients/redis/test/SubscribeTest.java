package io.github.wycst.wast.clients.redis.test;

import io.github.wycst.wast.clients.redis.client.RedisClient;
import io.github.wycst.wast.clients.redis.client.SimpleRedisClient;
import io.github.wycst.wast.clients.redis.listener.Subscriber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

/**
 * @Author: wangy
 * @Date: 2020/5/29 17:20
 * @Description:
 */
public class SubscribeTest {


    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {

        final RedisClient redisClient = new SimpleRedisClient("localhost", 6379);
//        final RedisClient redisClient = new SimpleRedisClient("49.233.166.49", 6379);
//        redisClient.auth("49#123456#49");
        redisClient.sync();

        final Subscriber subscriber = new Subscriber("bbc","bbe") {
            @Override
            public void onMessage(String channal, String topic, String message) {
                System.out.println("收到消息了: " + channal + "/" + topic + "/" + message);
            }

            public void onError(String error) {
                System.out.println("============= > " + error);
            }
        };

//        new Thread(new Runnable() {
//            public void run() {
//                redisClient.psubscribe(subscriber);
//            }
//        }).start();

        redisClient.subscribe(subscriber);


        // redisClient.unsubscribe();

        System.out.println("========== 发起了订阅 ");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        for (; ; ) {
            String s = in.readLine();
            if (s.equalsIgnoreCase("quit")) {
                break;
            }
            if(s.equalsIgnoreCase("unsubscribe")) {
                redisClient.unsubscribe();
                System.out.println("============== 取消订阅");
            }
        }


    }
}
