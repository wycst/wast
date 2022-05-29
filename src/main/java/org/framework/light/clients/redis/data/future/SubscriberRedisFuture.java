package org.framework.light.clients.redis.data.future;

import org.framework.light.clients.redis.exception.RedisException;
import org.framework.light.clients.redis.listener.Subscriber;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @Author: wangy
 * @Date: 2020/5/29 18:25
 * @Description:
 */
public class SubscriberRedisFuture<E> extends RedisFuture<List> {

    private Subscriber subscriber;

    public SubscriberRedisFuture(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void set(Object result) {
        if (result instanceof String) {
            subscriber.onError((String) result);
        } else if (result instanceof List) {
            List<String> msgs = (List<String>) result;
            if (msgs.size() < 3) {
                subscriber.onError("Unkown Error ");
            } else {
                String type = msgs.get(0);
                if (type.endsWith("subscribe")) {
                    return;
                }
                String channel = msgs.get(1);
                String topic = null;
                String message = null;
                if ("message".equals(type)) {
                    topic = channel;
                    message = msgs.get(2);
                } else if ("pmessage".equals(type)) {
                    topic = msgs.get(2);
                    message = msgs.get(3);
                } else {
                    subscriber.onError("Unkown Error ");
                }
                subscriber.onMessage(channel, topic, message);
            }
        } else {
            subscriber.onError("Unkown Error ");
        }
    }

    @Override
    public List get() throws InterruptedException, ExecutionException {
        throw new RedisException("Call not supported");
    }

    @Override
    public List get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new RedisException("Call not supported");
    }

    // 不阻塞
    @Override
    public List getResult() {
        throw new RedisException("Call not supported");
    }

    public boolean isKeepAlive() {
        return false;
    }

}
