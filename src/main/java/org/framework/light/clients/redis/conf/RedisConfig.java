package org.framework.light.clients.redis.conf;

/**
 * redis 配置
 *
 * @Author: wangy
 * @Date: 2020/5/19 20:01
 * @Description:
 */
public class RedisConfig {

    // 连接库序号默认0
    private int database;
    private String host;
    private int port;
    private String password;
    // 获取连接读取数据超时时间
    private long timeout;

    // 最大连接数(最高并发支持)
    private int poolMaxActive;
    // 获取连接最大等待时间
    private long poolMaxWait;
    // 最大空闲连接
    private int poolMaxIdle;
    // 最小空闲连接
    private int poolMinIdle;

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public int getPoolMaxActive() {
        return poolMaxActive;
    }

    public void setPoolMaxActive(int poolMaxActive) {
        this.poolMaxActive = poolMaxActive;
    }

    public long getPoolMaxWait() {
        return poolMaxWait;
    }

    public void setPoolMaxWait(long poolMaxWait) {
        this.poolMaxWait = poolMaxWait;
    }

    public int getPoolMaxIdle() {
        return poolMaxIdle;
    }

    public void setPoolMaxIdle(int poolMaxIdle) {
        this.poolMaxIdle = poolMaxIdle;
    }

    public int getPoolMinIdle() {
        return poolMinIdle;
    }

    public void setPoolMinIdle(int poolMinIdle) {
        this.poolMinIdle = poolMinIdle;
    }
}
