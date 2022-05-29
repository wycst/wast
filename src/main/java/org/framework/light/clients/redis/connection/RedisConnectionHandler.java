package org.framework.light.clients.redis.connection;

/**
 * @Author: wangy
 * @Date: 2020/5/23 12:46
 * @Description:
 */
public class RedisConnectionHandler {

    public static void handleQueueResponse(RedisConnection connection,Object result) {
        connection.handleQueueResponse(result);
    }

    public static void signProgress(RedisConnection connection) {
        connection.setProgress(true);
    }

    public static void safeclose(RedisConnection connection) {
        if(connection.beforeClose()) {
            connection.close();
        }
    }

    public static void forceclose(RedisConnection connection) {
        connection.close();
    }


}
