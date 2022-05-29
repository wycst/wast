package org.framework.light.clients.redis.connection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GenericFutureListener;
import org.framework.light.clients.redis.data.future.KeepAliveRedisFuture;
import org.framework.light.clients.redis.data.future.RedisFuture;
import org.framework.light.clients.redis.data.future.ResultRedisFuture;
import org.framework.light.clients.redis.data.future.SubscriberRedisFuture;
import org.framework.light.clients.redis.exception.RedisConnectionException;
import org.framework.light.clients.redis.exception.RedisException;
import org.framework.light.clients.redis.listener.Subscriber;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * redis连接对象
 *
 * @Author: wangy
 * @Date: 2020/5/18 17:11
 * @Description:
 */
public abstract class RedisConnection {

    private Channel channel;
    private boolean closed;
    // 是否同步（阻塞）
    private boolean isSynchronized;
    // 是否启用管道
    private boolean pipelined;
    // 是否开启了事务
    private boolean isMulti;
    // 订阅中响应
    private RedisFuture subscribeFuture;
    // 记录管道命令队列数量
    private int pipelineCount;
    // 缓冲命令区
    private StringBuffer commandBuffer = new StringBuffer();
    // 队列
    private Queue<RedisFuture> futureQueue = new LinkedList<RedisFuture>();

    // 是否订阅中
    private final AtomicBoolean inSubscribe = new AtomicBoolean(false);
    // 执行中状态
    private final AtomicBoolean inProgress = new AtomicBoolean(false);


    public final String id;
    public final String channelId;
    public final static AttributeKey<RedisConnection> REDIS_CONNECTION_ATTRIBUTE_KEY = AttributeKey.valueOf(RedisConnection.class, "REDIS_CONNECTION_ATTRIBUTE_KEY");

    public final static char M = '*';
    public final static char C = '$';
    public final static String SEPARATOR = "\r\n";
    public final static String SELECT = "SELECT";
    public final static String AUTH = "AUTH";
    public final static String MULTI = "MULTI";
    public final static String EXEC = "EXEC";
    public final static String DISCARD = "DISCARD";

    RedisConnection(Channel channel) {
        this(channel, false);
    }

    protected RedisConnection(Channel channel, boolean isSynchronized) {
        this.channel = channel;
        this.id = UUID.randomUUID().toString();
        this.channelId = channel.id().asShortText();
        this.isSynchronized = isSynchronized;
        channel.attr(REDIS_CONNECTION_ATTRIBUTE_KEY).set(this);
    }

    public abstract boolean recycleable();

    public final boolean isSynchronized() {
        return isSynchronized;
    }

    public void setSynchronized(boolean aSynchronized) {
        isSynchronized = aSynchronized;
    }

    protected final Channel getChannel() {
        return channel;
    }

    public String getId() {
        return this.id;
    }

    public void pipeline() {
        clear();
        this.pipelined = true;
    }

    public void executePipeline() {
        if (isPipelined()) {
            flush();
        }
        sync();
        pipelined = false;
    }

    public boolean completePipelined() {
        return --pipelineCount == 0;
    }

    public RedisFuture auth(String password) {
        clear();
        RedisFuture redisFuture = flush(AUTH + " " + password, new KeepAliveRedisFuture());
        return redisFuture;
    }

    public RedisFuture<String> select(int database) {
        clear();
        RedisFuture<String> redisFuture = flush(SELECT + " " + database, new KeepAliveRedisFuture());
        String result = redisFuture.getResult();
        if (!"OK".equals(result)) {
            throw new RedisException(result);
        }
        return redisFuture;
    }

    public RedisFuture multi() {
        clear();
        RedisFuture redisFuture = flush(MULTI, new KeepAliveRedisFuture());
        this.isMulti = true;
        return redisFuture;
    }

    public RedisFuture exec() {
        clear();
        RedisFuture redisFuture = flush(EXEC);
        this.isMulti = false;
        return redisFuture;
    }

    public void discard() {
        clear();
        flush(DISCARD);
        this.isMulti = false;
    }

    public void cancelPipeline() {
        clear();
        this.pipelined = false;
    }

    public RedisFuture writeAndFlush(String... commanders) {
        write(commanders);
        return flush();
    }

    public RedisFuture flush() {
        checkIfBusy();
        RedisFuture redisFuture = flush(commandBuffer.toString());
        commandBuffer.setLength(0);
        return redisFuture;
    }

    public void clear() {
    }

    private RedisFuture flush(String s) {
        return flush(s, new ResultRedisFuture());
    }

    private RedisFuture flush(String s, RedisFuture redisFuture) {
        checkIfClosed();
        if (isSynchronized()) {
        }
        channelFlush(s);
        futureQueue.add(redisFuture);
//        inProgress.set(false);
        return redisFuture;
    }

    private void channelFlush(Object s) {
        ChannelFuture future = channel.writeAndFlush(s).addListener(new GenericFutureListener<ChannelFuture>() {
            public void operationComplete(ChannelFuture future) {
                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
                }
            }
        });
        // 确保命令入栈队列和futureQueue一致,损失部分异步性能
        future.awaitUninterruptibly();
    }

    private void sync(RedisFuture redisFuture) {
        try {
            redisFuture.sync();
        } catch (Throwable e) {
            throw new RedisConnectionException(e);
        }
    }

    private void checkIfClosed() {
        if (this.isClosed()) {
            throw new RedisConnectionException("connection is closed ");
        }
    }

    private void checkIfBusy() {
        if (isBusy()) {
            throw new RedisConnectionException("Connection is busy, response is not supported temporarily ");
        }
    }

    public boolean isClosed() {
        return closed ? true : !channel.isActive();
    }

    public boolean isPipelined() {
        return pipelined;
    }

    public boolean isProgress() {
        return inProgress.get();
    }

    public boolean isMulti() {
        return isMulti;
    }

    public void close() {
        checkIfClosed();
        this.clear();
        channel.attr(REDIS_CONNECTION_ATTRIBUTE_KEY).set(null);
        this.channel = null;
        this.closed = true;
    }

    public void write(String... commands) {
        checkIfBusy();
        writeCommands(commands);
    }

    private void writeCommands(String[] commands) {
        if (!pipelined) {
            commandBuffer.setLength(0);
        }
        appendCommands(commands);
        if (pipelined && commands.length > 0) {
            pipelineCount++;
        }
    }

    private void appendCommands(String... commands) {
        commandBuffer.append(M).append(commands.length).append(SEPARATOR);
        for (String commander : commands) {
            commander = commander == null ? "" : commander.trim();
            int byteLength = commander.getBytes().length;
            commandBuffer.append(C).append(byteLength).append(SEPARATOR);
            commandBuffer.append(commander).append(SEPARATOR);
        }
    }

    /**
     * 连接是否能关闭
     *
     * @return
     */
    protected boolean beforeClose() {
        if (isMulti() || isPipelined() || isProgress()) {
            return false;
        }
        if (isBusy()) {
            return false;
        }
        return recycleable() && emptyQueue();
    }

    private boolean emptyQueue() {
        return futureQueue.size() == 0;
    }

    private boolean isBusy() {
        return inSubscribe.get();
    }

    protected void afterQueueResponse() {
    }

    public void sync() {
        setSynchronized(true);
        ArrayList<RedisFuture> futureList = new ArrayList(futureQueue);
        if (!futureList.isEmpty()) {
            for (RedisFuture redisFuture : futureList) {
                sync(redisFuture);
            }
            futureList.clear();
        }
    }

    public void closeSync() {
        setSynchronized(false);
    }

    void setProgress(boolean progress) {
        this.inProgress.set(progress);
    }

    void handleQueueResponse(Object result) {

        RedisFuture redisFuture = next();

        boolean keepAlive = false;
        if (redisFuture != null) {
            redisFuture.set(result);
            keepAlive = redisFuture.isKeepAlive();
        }

        boolean waitPipelined = false;
        if (isPipelined()) {
            waitPipelined = true;
            if (completePipelined()) {
                waitPipelined = false;
            }
        }

        afterQueueResponse();
        if (keepAlive) {
            return;
        }

        if (!waitPipelined && beforeClose()) {
            // close();
        }

    }

    private RedisFuture next() {
        if (inSubscribe.get()) {
            return subscribeFuture;
        }
        return futureQueue.poll();
    }

    public void subscribe(String[] subCommands, Subscriber subscriber) {
        // 一旦订阅成功，当前通道（channal）被占用，除非调用 unsubscribe取消订阅all
        sync();
        RedisFuture redisFuture = this.writeAndFlush(subCommands);
        Object result = redisFuture.getResult();
        if(result instanceof String) {
            throw new RedisException((String) result);
        }
        inSubscribe.set(true);
//        subscriber.ok((List<String>) result);
        this.subscribeFuture = new SubscriberRedisFuture(subscriber);
    }

    public void unsubscribe(String command, String[] channels) {

        boolean isEmptyChannels = channels == null || channels.length == 0;
        if (isEmptyChannels) {
            channelFlush(command);
            // resume
            inSubscribe.set(false);
        } else {
            StringBuffer buffer = new StringBuffer(command);
            buffer.append(" ");
            for (String channel : channels) {
                buffer.append(channel).append(" ");
            }
            channelFlush(buffer.toString());
        }
    }

    @Override
    public String toString() {
        return "RedisConnection{" +
                "closed=" + closed +
                ", id='" + id + '\'' +
                ", channel='" + channelId + '\'' +
                '}';
    }
}
