package io.github.wycst.wast.clients.http.provider.nacos;

import io.github.wycst.wast.clients.http.HttpClient;
import io.github.wycst.wast.clients.http.definition.HttpClientConfig;
import io.github.wycst.wast.clients.http.impl.DefaultServiceProvider;
import io.github.wycst.wast.clients.http.provider.ServerZone;
import io.github.wycst.wast.common.utils.ExecutorServiceUtils;
import io.github.wycst.wast.json.JSON;
import io.github.wycst.wast.log.Log;
import io.github.wycst.wast.log.LogFactory;
import io.github.wycst.wast.yaml.YamlDocument;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * nacos客户端服务（httpClient提供者）
 *
 * <p> 配置和服务实例获取
 * <p> 实例注册
 * <p> 集成httpclient
 *
 * @Author wangyunchao
 * @Date 2022/7/13 9:35
 */
public class NacosServiceProvider extends DefaultServiceProvider {

    private static Log log = LogFactory.getLog(NacosServiceProvider.class);

    private final HttpClient httpClient = new HttpClient();
    private final ScheduledExecutorService scheduledExecutorService;
    private final Properties nacosProperties;

    // 配置加载回调
    private FetchPropertiesCallback fetchPropertiesCallback;

    private final String CLOUD_NACOS_SERVER_ADDR_KEY = "cloud.nacos.server_addr";
    private final String CLOUD_NACOS_USERNAME_KEY = "cloud.nacos.username";
    private final String CLOUD_NACOS_PASSWORD_KEY = "cloud.nacos.password";
    private final String CLOUD_NACOS_AUTH_KEY = "cloud.nacos.auth.enabled";

    private final String CLOUD_NACOS_CONFIG_DATAID_KEY = "cloud.nacos.config.dataIds";
    private final String CLOUD_NACOS_CONFIG_GROUP_KEY = "cloud.nacos.config.groups";
    private final String CLOUD_NACOS_CONFIG_TENANT_KEY = "cloud.nacos.config.tenants";

    // 实例注册配置key
    // 实例IP
    private final String CLOUD_NACOS_INSTANCE_IP_KEY = "cloud.nacos.instance.ip";
    // nacos注册服务实例名称
    private final String CLOUD_NACOS_INSTANCE_SERVICE_NAME_KEY = "cloud.nacos.instance.serviceName";
    // 当前服务的端口（如果获取不到获取本地服务的server.port）
    private final String CLOUD_NACOS_INSTANCE_SERVICE_PORT_KEY = "cloud.nacos.instance.servicePort";
    // 命名空间id
    private final String CLOUD_NACOS_INSTANCE_NAMESPACE_ID_KEY = "cloud.nacos.instance.namespaceId";
    // 通过注册代替发送心跳
    private final String CLOUD_NACOS_INSTANCE_BEAT_METHOD_KEY = "cloud.nacos.instance.beat.method";

    // 检查间隔（上报健康状态，当nacos重启后会自动重连）
    private final String CLOUD_NACOS_INSTANCE_CHECK_HEALTHY_INTERVAL_KEY = "cloud.nacos.instance.checkHealthyInterval";
    // 是否启用注册
    private final String CLOUD_NACOS_INSTANCE_ENABLE_KEY = "cloud.nacos.instance.enable";

    private String serverAddr;
    private String username;
    private String password;
    private boolean auth;
    private String accessTokenKeyAndValue = "";
    private String nacosServerName;
    private boolean enableNacosClient;

    private String[] dataIds;
    private String[] groups;
    private String[] tenants;
    private int configCount;

    private boolean instanceBeatByRegister;
    private String instanceIp;
    private String instanceServiceName;
    private String instancePort;
    private String instanceNamespaceId;
    private long instanceCheckHealthyInterval;
    private boolean instanceEnable;
    private boolean status;

    private final long serviceUpdateInterval = 60;

    private String nacosAuthUrl;
    private String nacosInstanceUrl;
    private String nacosConfigsUrl;
    private String nacosCheckHealthyUrl;

    private ServiceListResponse serviceListResponse;

    public NacosServiceProvider(Properties nacosProperties) {
        nacosProperties.getClass();
        this.nacosProperties = nacosProperties;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
        resolverProperties();
    }

    public void setFetchPropertiesCallback(FetchPropertiesCallback fetchPropertiesCallback) {
        this.fetchPropertiesCallback = fetchPropertiesCallback;
    }

    private void resolverProperties() {
        this.init();
        if (!this.enableNacosClient) {
            return;
        }
        // load config
        this.fetchConfig();
        // 设置服务提供者
        this.setHttpClientServiceProvider();
        // 开启任务加载服务
        this.beginFetchService();

        // 初始化实例信息
        this.initInstanceInfo();
        if (this.instanceEnable) {
            // 注册实例(注意再获取配置结束后)
            this.registerServiceInstance();
            // 开启健康检查
            this.beginHealthyCheck();
        }
    }

    private void beginFetchService() {
        this.scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
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

    private void setHttpClientServiceProvider() {
        httpClient.setServiceProvider(this);
        httpClient.setEnableLoadBalance(true);
    }

    private void fetchServiceInstanceList() {
        try {
            String serviceListUrl = String.format("http://%s/nacos/v1/ns/service/list?pageNo=1&pageSize=100000&%s", this.nacosServerName, accessTokenKeyAndValue);
            if (this.instanceNamespaceId != null) {
                serviceListUrl += "&namespaceId=" + this.instanceNamespaceId;
            }

            this.serviceListResponse = httpClient.get(serviceListUrl, ServiceListResponse.class);
            // nacos/v1/ns/instance/list?serviceName=nacos.test.1&healthyOnly=true
            List<String> doms = this.serviceListResponse.getDoms();
            clearIfNotExist(doms);
            for (String serviceName : doms) {
                String instanceListUrl = String.format("http://%s/nacos/v1/ns/instance/list?serviceName=%s&healthyOnly=true&%s", this.nacosServerName, serviceName, accessTokenKeyAndValue);
                if (this.instanceNamespaceId != null) {
                    instanceListUrl += "&namespaceId=" + this.instanceNamespaceId;
                }
                ServiceInstanceResponse serviceInstanceResponse = httpClient.get(instanceListUrl, ServiceInstanceResponse.class);
                List<Map> hosts = serviceInstanceResponse.getHosts();
                List<String> serviceUrls = new ArrayList<String>();
                if (hosts != null && hosts.size() > 0) {
                    for (Map host : hosts) {
                        String ip = (String) host.get("ip");
                        Integer port = (Integer) host.get("port");
                        serviceUrls.add(ip + ":" + port);
                    }
                    ServerZone serverZone = new ServerZone(serviceName, serviceUrls);
                    registerServer(serverZone);
                }
            }
        } catch (Throwable throwable) {
            log.debug("fetchServiceInstanceList error: {}", throwable.getMessage());
        }
    }

    public void shutdownExecutorService() {
        ExecutorServiceUtils.shutdownExecutorService(scheduledExecutorService);
    }

    private void beginHealthyCheck() {
        if (this.instanceCheckHealthyInterval > 0) {
            this.scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    doHealthyCheck();
                }
            }, 0, this.instanceCheckHealthyInterval, TimeUnit.SECONDS);
        }
    }

    private void doHealthyCheck() {
        try {
            if (!this.status) {
                this.registerServiceInstance();
            } else {
                if (instanceBeatByRegister) {
                    this.registerServiceInstance();
                } else {
                    Map<String, String> beatInfo = new HashMap<String, String>();
                    beatInfo.put("port", this.instancePort);
                    beatInfo.put("ip", this.instanceIp);
                    beatInfo.put("port", this.instancePort);
                    beatInfo.put("serviceName", this.instanceServiceName);
                    beatInfo.put("namespaceId", this.instanceNamespaceId);
                    beatInfo.put("healthy", "true");
                    beatInfo.put("weight", "1.0");

                    HttpClientConfig requestConfig = new HttpClientConfig();
                    requestConfig.addTextParameter("serviceName", this.instanceServiceName);
                    requestConfig.addTextParameter("ephemeral", "false");
                    requestConfig.addTextParameter("beat", JSON.toJsonString(beatInfo));

                    Map result = httpClient.put(nacosCheckHealthyUrl, Map.class, requestConfig);
                    this.status = result != null && result.containsKey("clientBeatInterval");
                    log.debug("healthy check result: {}", result);
                }
            }

        } catch (Throwable throwable) {
            this.status = false;
            log.debug("healthy check error: {}", throwable.getMessage());
        }
    }

    private void fetchConfig() {
        log.info("configCount {}", this.configCount);
        if (this.configCount > 0) {
            for (int i = 0; i < this.configCount; i++) {
                try {
                    String dataId = dataIds[i].trim();
                    boolean isYaml = dataId.toLowerCase().endsWith(".yml") || dataId.toLowerCase().endsWith(".yaml");
                    String configUrl = String.format("%sdataId=%s&group=%s&tenant=%s", this.nacosConfigsUrl, dataId, groups[i], tenants[i]);
                    log.info("fetch configUrl {}", configUrl);
                    InputStream is = httpClient.get(configUrl, InputStream.class);
                    if (fetchPropertiesCallback != null) {
                        if (isYaml) {
                            Properties properties = YamlDocument.loadProperties(is);
                            fetchPropertiesCallback.loadProperties(properties);
                        } else {
                            fetchPropertiesCallback.loadProperties(is);
                        }
                    }
                } catch (Throwable throwable) {
                    log.debug(throwable.getMessage());
                }
            }
        }
    }

    public Properties fetchConfig(String dataId, String group, String tenant) throws IOException {
        boolean isYaml = dataId.toLowerCase().endsWith(".yml") || dataId.toLowerCase().endsWith(".yaml");
        String configUrl = String.format("%sdataId=%s&group=%s&tenant=%s", this.nacosConfigsUrl, dataId, group, tenant);
        log.info("fetch configUrl {}", configUrl);
        InputStream is = httpClient.get(configUrl, InputStream.class);
        if (isYaml) {
            return YamlDocument.loadProperties(is);
        } else {
            Properties properties = new Properties();
            properties.load(is);
            return properties;
        }
    }

    public interface FetchPropertiesCallback {
        void loadProperties(InputStream is);

        void loadProperties(Properties properties);
    }

    /**
     * 注册实例
     */
    private void registerServiceInstance() {

        try {
            // 注册地址 http://${serverAddr}/nacos/v1/ns/instance?port=8848&healthy=true&ip=11.11.11.11&weight=1.0&serviceName=nacos.test.3&encoding=GBK&namespaceId=n1
            HttpClientConfig requestConfig = new HttpClientConfig();
            requestConfig.addTextParameter("port", this.instancePort);
            requestConfig.addTextParameter("ip", this.instanceIp);
            requestConfig.addTextParameter("serviceName", this.instanceServiceName);
            requestConfig.addTextParameter("namespaceId", this.instanceNamespaceId);
            requestConfig.addTextParameter("healthy", "true");
            requestConfig.addTextParameter("weight", "1.0");
            requestConfig.addTextParameter("metadata", "{}");

            String result = httpClient.post(nacosInstanceUrl, String.class, requestConfig);
            log.debug("result {}", result);
            this.status = "OK".equalsIgnoreCase(result);
        } catch (Throwable throwable) {
            log.debug("nacos register fail - {}", nacosInstanceUrl);
            log.debug(throwable.getMessage());
        }
    }

    private void init() {
        // load serverAddr
        this.serverAddr = getProperty(CLOUD_NACOS_SERVER_ADDR_KEY);
        if (this.serverAddr == null) {
            log.info("nacos config {} is required ", CLOUD_NACOS_SERVER_ADDR_KEY);
            return;
        }
        this.enableNacosClient = true;
        log.info("nacos serverAddr {}", this.serverAddr);
        this.username = getProperty(CLOUD_NACOS_USERNAME_KEY);
        this.password = getProperty(CLOUD_NACOS_PASSWORD_KEY);
        this.auth = "true".equals(getProperty(CLOUD_NACOS_AUTH_KEY));

        // init urls
        if (this.serverAddr.indexOf(",") == -1) {
            // 登录url
            // curl -X POST '127.0.0.1:8848/nacos/v1/auth/login' -d 'username=nacos&password=nacos'
            // {"accessToken":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuYWNvcyIsImV4cCI6MTYwNTYyOTE2Nn0.2TogGhhr11_vLEjqKko1HJHUJEmsPuCxkur-CfNojDo","tokenTtl":18000,"globalAdmin":true}
            this.nacosAuthUrl = String.format("http://%s/nacos/v1/auth/login", this.serverAddr);
            // single node mode
            this.nacosInstanceUrl = String.format("http://%s/nacos/v1/ns/instance", this.serverAddr);
            this.nacosCheckHealthyUrl = String.format("http://%s/nacos/v1/ns/instance/beat", this.serverAddr);
            this.nacosConfigsUrl = String.format("http://%s/nacos/v1/cs/configs?", this.serverAddr);
            this.nacosServerName = this.serverAddr;
        } else {
            // multi node mode
            String[] servers = serverAddr.split(",");
            this.nacosServerName = "nacos-cluster";

            ServerZone serverZone = new ServerZone(this.nacosServerName, servers, true);
            this.setHttpClientServiceProvider();
            registerServer(serverZone);

            this.nacosAuthUrl = String.format("http://%s/nacos/v1/auth/login", this.nacosServerName);
            this.nacosInstanceUrl = String.format("http://%s/nacos/v1/ns/instance", this.nacosServerName);
            this.nacosCheckHealthyUrl = String.format("http://%s/nacos/v1/ns/instance/beat", this.nacosServerName);
            this.nacosConfigsUrl = String.format("http://%s/nacos/v1/cs/configs?", this.nacosServerName);
        }

        log.info("cloud.nacos.auth.enabled {}", auth);
        if (this.auth) {

            // 提前获取accessToken
            HttpClientConfig clientConfig = new HttpClientConfig();
            clientConfig.addTextParameter("username", username);
            clientConfig.addTextParameter("password", password);
            String accessTokenKeyAndValue = "";
            try {
                Map map = httpClient.post(this.nacosAuthUrl, Map.class, clientConfig);
                if (map != null && map.containsKey("accessToken")) {
                    String accessToken = map.get("accessToken").toString();
                    accessTokenKeyAndValue = "accessToken=" + accessToken;
                    this.nacosInstanceUrl += "?" + accessTokenKeyAndValue;
                    this.nacosCheckHealthyUrl += "?" + accessTokenKeyAndValue;
                    this.nacosConfigsUrl += accessTokenKeyAndValue + "&";
                } else {
                    log.warn("Failed to get Nacos accessToken ");
                }

            } catch (Throwable throwable) {
            }
            this.accessTokenKeyAndValue = accessTokenKeyAndValue;
        }

        this.initConfigParams();
    }

    private void initInstanceInfo() {
        this.instanceIp = getProperty(CLOUD_NACOS_INSTANCE_IP_KEY);
        // 校验ip是否是本机IP
        this.instanceServiceName = getProperty(CLOUD_NACOS_INSTANCE_SERVICE_NAME_KEY);
        this.instancePort = getProperty(CLOUD_NACOS_INSTANCE_SERVICE_PORT_KEY);
        if (this.instancePort == null) {
            this.instancePort = getProperty("server.port");
        }
        this.instanceNamespaceId = getProperty(CLOUD_NACOS_INSTANCE_NAMESPACE_ID_KEY);
        this.instanceBeatByRegister = getProperty(CLOUD_NACOS_INSTANCE_BEAT_METHOD_KEY) != null;

        String instanceCheckHealthyInterval = getProperty(CLOUD_NACOS_INSTANCE_CHECK_HEALTHY_INTERVAL_KEY);
        try {
            this.instanceCheckHealthyInterval = Long.parseLong(instanceCheckHealthyInterval.trim());
        } catch (Throwable throwable) {
            this.instanceCheckHealthyInterval = 30; // 30s
        }
        String enable = getProperty(CLOUD_NACOS_INSTANCE_ENABLE_KEY);
        this.instanceEnable = !"false".equals(enable);
    }

    private void initConfigParams() {
        try {
            String dataIds = getProperty(CLOUD_NACOS_CONFIG_DATAID_KEY);
            String groups = getProperty(CLOUD_NACOS_CONFIG_GROUP_KEY);
            String tenants = getProperty(CLOUD_NACOS_CONFIG_TENANT_KEY);
            this.dataIds = dataIds.trim().split(",");
            this.groups = groups.trim().split(",");
            this.tenants = tenants.trim().split(",");
            this.configCount = Math.min(this.dataIds.length, Math.min(this.groups.length, this.tenants.length));
        } catch (Throwable throwable) {
            log.debug("init config error： {}", throwable.getMessage());
        }
    }

    /**
     * 子类需要重写此方法来实现动态占位符表达式取值
     *
     * @param key
     * @return
     */
    public String getProperty(String key) {
        return nacosProperties.getProperty(key);
    }

    public void destroy() {
        this.shutdownExecutorService();
    }
}
