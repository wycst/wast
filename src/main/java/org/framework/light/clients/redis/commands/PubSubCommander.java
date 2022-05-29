package org.framework.light.clients.redis.commands;

import org.framework.light.clients.redis.listener.Subscriber;

import java.util.List;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2020/5/28 23:38
 * @Description:
 */
public interface PubSubCommander {

    /**
     * PUBLISH channel message
     * 将信息 message 发送到指定的频道 channel 。
     *
     * @param topic
     * @param message
     * @return
     */
    public long publish(String topic, String message);

    /**
     * 订阅给定的一个或多个频道的信息
     * SUBSCRIBE channel [channel ...]
     *
     * @param subscriber
     */
    public void subscribe(Subscriber subscriber);

    /**
     * 指示客户端退订给定的频道
     * UNSUBSCRIBE [channel [channel ...]]
     *
     * @param channels
     */
    public void unsubscribe(String... channels);

    /**
     * 订阅一个或多个符合给定模式的频道。
     * PSUBSCRIBE pattern [pattern ...]
     *
     * @param subscriber
     */
    public void psubscribe(Subscriber subscriber);

    /**
     * 指示客户端退订给定的频道
     * PUNSUBSCRIBE [pattern [pattern ...]]
     *
     * @param channels
     */
    public void punsubscribe(String... channels);

    /**
     * 列出当前的活跃频道
     * PUBSUB CHANNELS [pattern]
     *
     * @param pattern
     */
    public List<String> pubsubChannels(String pattern);

    /**
     * 返回给定频道的订阅者数量
     * PUBSUB NUMSUB [channel-1 ... channel-N]
     *
     * @param channels
     */
    public Map<String, Long> pubsubNumsub(String... channels);

    /**
     * 返回订阅模式的数量。
     * PUBSUB NUMPAT
     */
    public long pubsubNumpat();


}
