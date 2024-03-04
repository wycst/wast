package io.github.wycst.wast.clients.http.provider;

import io.github.wycst.wast.clients.http.HttpClient;
import io.github.wycst.wast.common.utils.ExecutorServiceUtils;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @Author wangyunchao
 * @Date 2023/4/20 21:35
 */
public abstract class CloudServiceProvider extends DefaultServiceProvider {

    protected final HttpClient httpClient = new HttpClient();
    protected final ScheduledExecutorService scheduledExecutorService;
    protected ScheduledFuture<?> serviceScheduledFuture;
    protected ScheduledFuture<?> healthyCheckScheduledFuture;
    protected ScheduledFuture<?> tokenRefreshScheduledFuture;
    protected final Properties properties;

    // 配置加载回调
    protected FetchPropertiesCallback fetchPropertiesCallback;

    // 是否启用客户端
    protected boolean enableClient;
    protected long instanceCheckHealthyInterval;
    protected boolean instanceEnable;
    protected boolean status;

    private final long serviceUpdateInterval = 60;

    public CloudServiceProvider(Properties nacosProperties, FetchPropertiesCallback fetchPropertiesCallback) {
        // nacosProperties.getClass();
        this.properties = nacosProperties;
        this.fetchPropertiesCallback = fetchPropertiesCallback;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(3);
        init();
    }

    public void setFetchPropertiesCallback(FetchPropertiesCallback fetchPropertiesCallback) {
        this.fetchPropertiesCallback = fetchPropertiesCallback;
    }

    private void init() {
        // nacos base
        this.initBase();
        if (!this.enableClient) {
            return;
        }
        // load nacos config
        this.fetchClientConfig();
        // 设置服务提供者
        this.setHttpClientServiceProvider();
        if (this.autoSchedule()) {
            this.scheduleTask();
        }
    }

    protected abstract void fetchClientConfig();

    public void scheduleTask() {
        if (!this.enableClient) {
            return;
        }
        // 初始化实例信息
        this.initInstanceConfig();
        // 加载服务列表
        this.fetchServiceList();
        if (this.instanceEnable) {
            // 注册实例(注意再获取配置结束后)
            this.registerInstance();
            // 开启健康检查
            this.beginHealthyCheck();
        }
    }

    public void fetchServiceList() {
        this.cancelServiceScheduled();
        this.serviceScheduledFuture = this.scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                if (status) {
                    fetchServiceInstanceList();
                }
            }
        }, 0, this.serviceUpdateInterval, TimeUnit.SECONDS);

        // first fetch
        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                fetchServiceInstanceList();
            }
        });
    }

    protected abstract void fetchServiceInstanceList();

    protected final void setHttpClientServiceProvider() {
        httpClient.setServiceProvider(this);
        httpClient.setEnableLoadBalance(true);
    }

    protected boolean autoSchedule() {
        return true;
    }

    protected abstract void initBase();

    protected abstract void initInstanceConfig();

    protected abstract void registerInstance();

    /**
     * 子类需要重写此方法来实现动态占位符表达式取值
     *
     * @param key
     * @return
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    private void cancelServiceScheduled() {
        if (this.serviceScheduledFuture != null) {
            this.serviceScheduledFuture.cancel(true);
        }
    }

    private void cancelHealthyCheckScheduled() {
        if (this.healthyCheckScheduledFuture != null) {
            this.healthyCheckScheduledFuture.cancel(true);
        }
    }

    private void cancelTokenRefreshScheduled() {
        if (this.tokenRefreshScheduledFuture != null) {
            this.tokenRefreshScheduledFuture.cancel(true);
        }
    }

    public void shutdownExecutorService() {
        ExecutorServiceUtils.shutdownExecutorService(scheduledExecutorService);
    }

    private void beginHealthyCheck() {
        this.cancelHealthyCheckScheduled();
        if (this.instanceCheckHealthyInterval > 0) {
            this.scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    doHealthyCheck();
                }
            }, 0, this.instanceCheckHealthyInterval, TimeUnit.SECONDS);
        }
    }

    protected abstract void doHealthyCheck();

    public void destroy() {
        super.clear();
        if (properties != null) {
            properties.clear();
        }
        this.cancelScheduled();
        this.shutdownExecutorService();
    }

    /**
     * 取消任务
     *
     */
    public void cancelScheduled() {
        this.cancelServiceScheduled();
        this.cancelHealthyCheckScheduled();
        this.cancelTokenRefreshScheduled();
    }
}
