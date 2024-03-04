package io.github.wycst.wast.clients.http.provider.consul;

import io.github.wycst.wast.clients.http.definition.HttpClientConfig;
import io.github.wycst.wast.clients.http.definition.HttpClientResponse;
import io.github.wycst.wast.clients.http.provider.CloudServiceProvider;
import io.github.wycst.wast.clients.http.provider.FetchPropertiesCallback;
import io.github.wycst.wast.clients.http.provider.ServerZone;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.log.Log;
import io.github.wycst.wast.log.LogFactory;

import java.util.*;

/**
 * @Author wangyunchao
 * @Date 2023/4/20 21:32
 */
public class ConsulServiceProvider extends CloudServiceProvider {

    private static Log log = LogFactory.getLog(ConsulServiceProvider.class);

    private final String CLOUD_CONSUL_SERVER_ADDR_KEY = "cloud.consul.server_addr";
    private final String CLOUD_CONSUL_USERNAME_KEY = "cloud.consul.username";
    private final String CLOUD_CONSUL_PASSWORD_KEY = "cloud.consul.password";
    private final String CLOUD_CONSUL_ACL_ENABLED_KEY = "cloud.consul.acl.enabled";
    private final String CLOUD_CONSUL_ACL_TOKEN_KEY = "cloud.consul.acl.token";

    // 实例注册配置key
    // 实例IP
    private final String CLOUD_CONSUL_INSTANCE_IP_KEY = "cloud.consul.instance.ip";
    // consul注册服务实例名称
    private final String CLOUD_CONSUL_INSTANCE_SERVICE_NAME_KEY = "cloud.consul.instance.serviceName";
    // 当前服务的端口（如果获取不到获取本地服务的server.port）
    private final String CLOUD_CONSUL_INSTANCE_SERVICE_PORT_KEY = "cloud.consul.instance.servicePort";
    // 命名空间id
    private final String CLOUD_CONSUL_INSTANCE_NAMESPACE_ID_KEY = "cloud.consul.instance.namespaceId";
    private final String CLOUD_CONSUL_INSTANCE_HEALTH_CHECK_URI_KEY = "cloud.consul.instance.health-check-uri";
    // 检查间隔（客户端检查consul服务是否在线的频率）
    private final String CLOUD_CONSUL_INSTANCE_CHECK_HEALTHY_INTERVAL_KEY = "cloud.consul.instance.checkHealthyInterval";
    // 检查间隔（上报健康状态，当consul服务端定时调用checkUri）
    private final String CLOUD_CONSUL_SERVER_CHECK_HEALTHY_INTERVAL_KEY = "cloud.consul.server.checkHealthyInterval";
    // 是否启用注册
    private final String CLOUD_CONSUL_INSTANCE_ENABLE_KEY = "cloud.consul.instance.enable";

    private String serverAddr;
    // consul服务名称
    private String consulServerName;

    private String username;
    private String password;
    private boolean aclEnabled;
    private String aclToken;

    private String instanceIp;
    private String instanceServiceName;
    private String instancePort;
    private String instanceNamespaceId;
    private String serverContextPath;
    private String checkUri;
    // 检查consul服务间隔
    private long serverCheckHealthyInterval;
    private boolean status;

    private boolean serverUp;

    public ConsulServiceProvider(Properties consulProperties, FetchPropertiesCallback fetchPropertiesCallback) {
        super(consulProperties, fetchPropertiesCallback);
    }

    public ConsulServiceProvider(FetchPropertiesCallback fetchPropertiesCallback) {
        this(null, fetchPropertiesCallback);
    }

    public ConsulServiceProvider(Properties consulProperties) {
        this(consulProperties, null);
    }

    @Override
    protected void initBase() {
        // 加载consul配置
        this.serverAddr = getProperty(CLOUD_CONSUL_SERVER_ADDR_KEY);
        if (this.serverAddr == null) {
            log.info("consul config '{}' is required ", CLOUD_CONSUL_SERVER_ADDR_KEY);
            return;
        }
        this.enableClient = true;

        log.info("consul serverAddr {}", this.serverAddr);
        this.username = getProperty(CLOUD_CONSUL_USERNAME_KEY);
        this.password = getProperty(CLOUD_CONSUL_PASSWORD_KEY);

        this.aclEnabled = "true".equals(getProperty(CLOUD_CONSUL_ACL_ENABLED_KEY));
        if (this.aclEnabled) {
            this.aclToken = getProperty(CLOUD_CONSUL_ACL_TOKEN_KEY);
        }

        this.checkUri = getProperty(CLOUD_CONSUL_INSTANCE_HEALTH_CHECK_URI_KEY);
        if (this.checkUri == null) {
            this.checkUri = "/";
        } else {
            this.checkUri = this.checkUri.startsWith("/") ? this.checkUri : "/" + this.checkUri;
        }

        if (this.serverAddr.indexOf(",") == -1) {
            this.consulServerName = this.serverAddr;
        } else {
            // multi node mode
            String[] servers = this.serverAddr.split(",");
            this.consulServerName = "consul-cluster";
            ServerZone serverZone = new ServerZone(this.consulServerName, servers, true);
            this.setHttpClientServiceProvider();
            registerServer(serverZone);
        }

    }

    @Override
    protected void initInstanceConfig() {
        this.instanceIp = getProperty(CLOUD_CONSUL_INSTANCE_IP_KEY);
        this.instanceServiceName = getProperty(CLOUD_CONSUL_INSTANCE_SERVICE_NAME_KEY);
        this.instancePort = getProperty(CLOUD_CONSUL_INSTANCE_SERVICE_PORT_KEY);
        if (this.instancePort == null) {
            this.instancePort = getProperty("server.port");
        }
        // 上下文
        this.serverContextPath = getProperty("server.context-path");
        this.instanceNamespaceId = getProperty(CLOUD_CONSUL_INSTANCE_NAMESPACE_ID_KEY);
        String instanceCheckHealthyInterval = getProperty(CLOUD_CONSUL_INSTANCE_CHECK_HEALTHY_INTERVAL_KEY);
        try {
            this.instanceCheckHealthyInterval = Long.parseLong(instanceCheckHealthyInterval.trim());
        } catch (Throwable throwable) {
            this.instanceCheckHealthyInterval = 30; // 5s检查一次心跳
        }

        String serverCheckHealthyInterval = getProperty(CLOUD_CONSUL_SERVER_CHECK_HEALTHY_INTERVAL_KEY);
        try {
            this.serverCheckHealthyInterval = Long.parseLong(serverCheckHealthyInterval.trim());
        } catch (Throwable throwable) {
            this.serverCheckHealthyInterval = 15; // 服务端5s检查一次当前服务是否健康
        }

        String enable = getProperty(CLOUD_CONSUL_INSTANCE_ENABLE_KEY);
        this.instanceEnable = enable != null && !"false".equals(enable);
    }

    @Override
    protected void fetchClientConfig() {

    }

    /**
     * GET /v1/catalog/services(GET /v1/agent/services)
     */
    @Override
    protected void fetchServiceInstanceList() {

        try {
            // 获取所有services
//            String serviceListUrl = String.format("http://%s/v1/agent/services", this.serverAddr);
            String serviceListUrl = String.format("http://%s/v1/catalog/services%s", this.consulServerName, this.aclEnabled ? "?token=" + this.aclToken : "");
            Map<String, Object> services = httpClient.get(serviceListUrl, Map.class);

            // 遍历所有service获取健康实例列表
            Set<String> serverNames = services.keySet();
            Set<String> healthServerNames = new HashSet<String>();

            //GET /v1/health/service/%s?passing=true
            for (String serviceName : serverNames) {
                String instanceListUrl = String.format("http://%s/v1/health/service/%s?passing=true%s", this.consulServerName, serviceName, this.aclEnabled ? "&token=" + this.aclToken : "");
                HttpClientResponse clientResponse = httpClient.get(instanceListUrl);
                List<HealthServiceInstance> healthServiceInstances = clientResponse.getEntity(GenericParameterizedType.collectionType(List.class, HealthServiceInstance.class));

                List<String> serviceUrls = new ArrayList<String>();
                if (healthServiceInstances != null && healthServiceInstances.size() > 0) {
                    for (HealthServiceInstance healthServiceInstance : healthServiceInstances) {
                        ServiceInfo service = healthServiceInstance.getService();
                        String ip = service.getAddress();
                        if (ip.isEmpty()) continue;
                        int port = service.getPort();
                        serviceUrls.add(ip + ":" + port);
                    }
                    if (serviceUrls.size() > 0) {
                        ServerZone serverZone = new ServerZone(serviceName, serviceUrls);
                        registerServer(serverZone);
                        healthServerNames.add(serviceName);
                    }
                }
            }

            // 移除不健康的实例列表
            clearIfNotExist(healthServerNames);
        } catch (Throwable throwable) {
            log.debug("fetchServiceInstanceList error: {}", throwable.getMessage());
        }

    }

    /**
     * PUT /v1/agent/service/register HTTP/1.1
     * Content-Type: text/plain; charset=UTF-8
     */
    @Override
    protected void registerInstance() {
        try {
            // 注册地址 http://${serverAddr}/v1/agent/service/register
            Map<String, Object> body = new HashMap<String, Object>();
            body.put("ID", this.instanceIp + ":" + this.instancePort);
            body.put("Name", this.instanceServiceName);
            body.put("Address", this.instanceIp);
            body.put("Port", Integer.parseInt(this.instancePort));
            body.put("Tags", Arrays.asList("version=1.0.0"));

            Map<String, Object> check = new HashMap<String, Object>();
            check.put("Interval", this.serverCheckHealthyInterval + "s");
            check.put("HTTP", String.format("http://%s:%s%s", this.instanceIp, this.instancePort, this.checkUri));
            body.put("Check", check);

            String targetUrl = String.format("http://%s/v1/agent/service/register%s", this.consulServerName, this.aclEnabled ? "?token=" + this.aclToken : "");

            HttpClientConfig clientConfig = new HttpClientConfig();
            clientConfig.setRequestBody(body, "application/json", true);

            httpClient.put(targetUrl, String.class, clientConfig);
            // httpClient.putJson(targetUrl, String.class, body);
            this.status = true;
        } catch (Throwable throwable) {
            log.debug("consul register fail - {}");
            log.error(throwable.getMessage(), throwable);
        }
    }

    /**
     * 定时检查consul服务是否正常
     */
    @Override
    protected void doHealthyCheck() {
        try {
            if (this.serverUp && !this.status) {
                this.registerInstance();
            } else {
                String result = httpClient.get(String.format("http://%s/v1/status/leader", this.consulServerName), String.class);
                if (result != null) {
                    this.serverUp = true;
                }
            }
        } catch (Throwable throwable) {
            this.status = false;
            this.serverUp = false;
        }
    }

    @Override
    public void destroy() {
        this.deregister();
        super.destroy();
    }

    private void deregister() {
        if(this.status) {
            try {
                String serviceId = this.instanceIp + ":" + this.instancePort;
                this.httpClient.put(String.format("http://%s/v1/agent/service/deregister/%s%s", this.consulServerName, serviceId, this.aclEnabled ? "?token=" + this.aclToken : ""), String.class);
                log.debug("deregister success");
            } catch (Throwable throwable) {

            }
        }
    }
}
