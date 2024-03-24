//package io.github.wycst.wast.clients.http.service;
//
//import io.github.wycst.wast.clients.http.HttpClient;
//import io.github.wycst.wast.clients.http.definition.HttpClientConfig;
//import io.github.wycst.wast.clients.http.provider.DefaultServiceProvider;
//import io.github.wycst.wast.clients.http.provider.ServerZone;
//import io.github.wycst.wast.clients.http.provider.ServiceProvider;
//import io.github.wycst.wast.clients.http.service.annotations.EnableHttpServiceClient;
//import io.github.wycst.wast.clients.http.service.annotations.HttpServiceClient;
//import io.github.wycst.wast.clients.http.service.annotations.ServiceEndpoint;
//import io.github.wycst.wast.clients.http.service.annotations.type.*;
//import io.github.wycst.wast.clients.http.service.entity.ServiceEndpointMapping;
//import io.github.wycst.wast.clients.http.service.interceptor.HttpServiceClientMethodInterceptor;
//import io.github.wycst.wast.common.utils.CollectionUtils;
//import io.github.wycst.wast.common.utils.ObjectUtils;
//import io.github.wycst.wast.common.utils.StringUtils;
//import io.github.wycst.wast.core.annotations.Registration;
//import io.github.wycst.wast.core.bean.BeanDefinition;
//import io.github.wycst.wast.core.bean.BeanRegistrationBridge;
//import io.github.wycst.wast.core.bean.BeanRegistrationHandler;
//import io.github.wycst.wast.core.config.PropertiesHandler;
//import io.github.wycst.wast.core.factory.BeanFactory;
//import io.github.wycst.wast.core.util.AsmUtils;
//
//import java.io.File;
//import java.lang.annotation.Annotation;
//import java.lang.reflect.Method;
//import java.util.*;
//
///**
// * http service clientservice handler
// *
// * @Author: wangy
// * @Date: 2021/8/14 20:43
// * @Description:
// */
//@Registration(EnableHttpServiceClient.class)
//public class HttpServiceClientRegistrationHandler extends HttpServiceClientMethodInterceptor implements BeanRegistrationHandler {
//
//    private BeanRegistrationBridge beanRegistrationBridge;
//    private HttpClient httpClient;
//    private Map<Method, ServiceEndpointMapping> serviceEndpointMappingMap = new HashMap<Method, ServiceEndpointMapping>();
//
//    @Override
//    public void doHandleRegister(BeanRegistrationBridge beanRegistrationBridge) {
//        this.beanRegistrationBridge = beanRegistrationBridge;
//        Set<Class<?>> httpServiceClientClsSet = beanRegistrationBridge.find(null, HttpServiceClient.class, false);
//        List<BeanDefinition> beanDefinitions = new ArrayList<BeanDefinition>();
//        HttpClient httpClient = beanRegistrationBridge.getComponentBean(HttpClient.class);
//        if (httpClient == null) {
//            httpClient = new HttpClient();
//            BeanDefinition beanDefinition = new BeanDefinition();
//            beanDefinition.setBean(httpClient);
//            beanDefinition.setSourceTarget(httpClient);
//            beanDefinition.setSourceClass(HttpClient.class);
//            beanDefinitions.add(beanDefinition);
//        }
//        this.httpClient = httpClient;
//        if (!CollectionUtils.isEmpty(httpServiceClientClsSet)) {
//            for (Class<?> httpServiceClientCls : httpServiceClientClsSet) {
//                Object httpServiceClient = createHttpServiceClient(httpServiceClientCls);
//                doRegisterEndpoints(httpServiceClientCls);
//                BeanDefinition beanDefinition = new BeanDefinition();
//                beanDefinition.setBean(httpServiceClient);
//                beanDefinition.setSourceTarget(httpServiceClient);
//                beanDefinition.setSourceClass(httpServiceClientCls);
//                beanDefinitions.add(beanDefinition);
//            }
//            beanRegistrationBridge.registerComponents(beanDefinitions);
//        }
//    }
//
//    @Override
//    public void afterFetchExternal(BeanFactory beanFactory, PropertiesHandler propertiesHandler) {
//
//    }
//
//    private void doRegisterEndpoints(Class<?> httpServiceClientCls) {
//        Method[] methods = httpServiceClientCls.getMethods();
//        HttpServiceClient client = httpServiceClientCls.getAnnotation(HttpServiceClient.class);
//        String serviceName = client.serviceName().trim();
//        String contextPath = client.contextPath().trim();
//        String[] serverHosts = client.serverHosts();
//        long globalTimeout = client.timeout();
//        boolean keepAliveOnTimeout = client.keepAliveOnTimeout();
//
//        ServiceProvider serviceProvider = httpClient.getServiceProvider();
//        if (serviceProvider == null) {
//            serviceProvider = new DefaultServiceProvider();
//            httpClient.setServiceProvider(serviceProvider);
//        }
//        httpClient.setEnableLoadBalance(true);
//        if (serverHosts.length > 0) {
//            serviceProvider.registerServer(new ServerZone(serviceName, serverHosts, true));
//        }
//        if (!contextPath.startsWith("/") && contextPath.length() > 0) {
//            contextPath = "/" + contextPath;
//        }
//        String protocol = client.protocol();
//        for (Method method : methods) {
//            if (!method.isAnnotationPresent(ServiceEndpoint.class)) {
//                continue;
//            }
//            ServiceEndpoint serviceEndpoint = method.getAnnotation(ServiceEndpoint.class);
//            long timeout = serviceEndpoint.timeout();
//            String uri = serviceEndpoint.uri();
//            if (!uri.startsWith("/")) {
//                uri = "/" + uri;
//            }
//            ServiceEndpointMapping serviceEndpointMapping = new ServiceEndpointMapping();
//            String url = "%s://%s%s%s";
//            serviceEndpointMapping.setUrl(String.format(url, protocol, serviceName, contextPath, uri));
//            serviceEndpointMapping.setMappingClass(httpServiceClientCls);
//            serviceEndpointMapping.setReturnType(method.getReturnType());
//            serviceEndpointMapping.setServiceEndpoint(serviceEndpoint);
//            serviceEndpointMapping.setTimeout(timeout > 0 ? timeout : globalTimeout);
//            serviceEndpointMapping.setKeepAliveOnTimeout(keepAliveOnTimeout);
//
//            Class<?>[] parameterTypes = method.getParameterTypes();
//            String[] parameterAliasNames = new String[parameterTypes.length];
//
//            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
//
//            String[] parameterNames = AsmUtils.getMethodParameterNames(httpServiceClientCls, method);
//            ServiceEndpointMapping.ParamType[] paramTypes = new ServiceEndpointMapping.ParamType[parameterTypes.length];
//            for (int i = 0; i < parameterTypes.length; i++) {
//                Annotation[] annotations = parameterAnnotations[i];
//                parameterAliasNames[i] = parameterNames[i];
//                Class<?> parameterType = parameterTypes[i];
//                Annotation annotation = null;
//                if ((annotation = getAnnotation(annotations, PathField.class)) != null) {
//                    PathField pathField = (PathField) annotation;
//                    String name = pathField.name();
//                    paramTypes[i] = ServiceEndpointMapping.ParamType.PathField;
//                    parameterAliasNames[i] = name;
//                } else if (getAnnotation(annotations, RequestBody.class) != null) {
//                    serviceEndpointMapping.setRequestBody(true);
//                    paramTypes[i] = ServiceEndpointMapping.ParamType.RequestBody;
//                } else if (getAnnotation(annotations, FormObject.class) != null) {
//                    paramTypes[i] = ServiceEndpointMapping.ParamType.FormObject;
//                } else if ((annotation = getAnnotation(annotations, FileField.class)) != null || parameterType == File.class) {
//                    if (annotation != null) {
//                        FileField fileField = (FileField) annotation;
//                        parameterAliasNames[i] = fileField.name();
//                    }
//                    serviceEndpointMapping.setMultipart(true);
//                    paramTypes[i] = ServiceEndpointMapping.ParamType.FileField;
//                } else {
//                    if ((annotation = getAnnotation(annotations, FormField.class)) != null) {
//                        FormField formField = (FormField) annotation;
//                        parameterAliasNames[i] = formField.name();
//                    }
//                    paramTypes[i] = ServiceEndpointMapping.ParamType.FormField;
//                }
//            }
//
//            serviceEndpointMapping.setParameterAliasNames(parameterAliasNames);
//            serviceEndpointMapping.setParameterNames(parameterNames);
//            serviceEndpointMapping.setParameterTypes(parameterTypes);
//            serviceEndpointMapping.setParamTypes(paramTypes);
//
//            serviceEndpointMappingMap.put(method, serviceEndpointMapping);
//        }
//    }
//
//    private <T> T getAnnotation(Annotation[] annotations, Class<T> annotationClass) {
//        for (Annotation annotation : annotations) {
//            if (annotation.annotationType() == annotationClass) {
//                return (T) annotation;
//            }
//        }
//        return null;
//    }
//
//    @Override
//    protected Object invoke(Object obj, Method method, Object[] objects) throws Throwable {
//        ServiceEndpointMapping serviceEndpointMapping = serviceEndpointMappingMap.get(method);
//        if (serviceEndpointMapping == null) {
//            return null;
//        }
//        String url = serviceEndpointMapping.getUrl();
//        String contentType = serviceEndpointMapping.getContentType();
//        Class<?> returnType = serviceEndpointMapping.getReturnType();
//        ServiceEndpoint serviceEndpoint = serviceEndpointMapping.getServiceEndpoint();
//        HttpClientConfig clientConfig = new HttpClientConfig();
//        if (contentType != null && contentType.length() > 0) {
//            clientConfig.setContentType(contentType);
//        }
//
//        if (serviceEndpointMapping.isMultipart()) {
//            clientConfig.setMultipart(true);
//        }
//
//        long timeout = serviceEndpointMapping.getTimeout();
//        if (serviceEndpointMapping.getTimeout() > 0) {
//            clientConfig.setMaxConnectTimeout(timeout);
//            clientConfig.setMaxReadTimeout(timeout);
//        }
//        clientConfig.setKeepAliveOnTimeout(serviceEndpointMapping.isKeepAliveOnTimeout());
//
//        Map<String, Object> context = new HashMap<String, Object>();
//        String[] aliasNames = serviceEndpointMapping.getParameterAliasNames();
//        Class<?>[] parameterTypes = serviceEndpointMapping.getParameterTypes();
//        ServiceEndpointMapping.ParamType[] paramTypes = serviceEndpointMapping.getParamTypes();
//        // parse params
//        for (int i = 0; i < objects.length; i++) {
//            Object param = objects[i];
//            if (param == null) {
//                continue;
//            }
//            String key = aliasNames[i];
//            ServiceEndpointMapping.ParamType paramType = paramTypes[i];
//            Class<?> parameterType = parameterTypes[i];
//            if (paramType == ServiceEndpointMapping.ParamType.PathField) {
//                // 路径参数，替换${name} -> name
//                context.put(key, String.valueOf(param));
//            } else {
//                if (serviceEndpointMapping.isMultipart() && parameterType == File.class) {
//                    clientConfig.addFileParameter(key == null ? "file" : key, (File) param, null);
//                } else if (serviceEndpointMapping.isRequestBody() && paramType == ServiceEndpointMapping.ParamType.RequestBody) {
//                    clientConfig.setRequestBody(param, "application/json", true);
//                } else if (paramType == ServiceEndpointMapping.ParamType.FormObject) {
//                    List<String> fields = ObjectUtils.getNonEmptyFields(param);
//                    for (String field : fields) {
//                        try {
//                            clientConfig.addTextParameter(field, String.valueOf(ObjectUtils.get(param, field)));
//                        } catch (Throwable throwable) {
//                        }
//                    }
//                } else {
//                    if (Number.class.isInstance(param) || parameterType == String.class) {
//                        clientConfig.addTextParameter(key, String.valueOf(param));
//                    }
//                }
//            }
//        }
//
//        // replace path
//        if (context.size() > 0) {
//            String groupRegex = "[$][{](.*?)[}]";
//            url = StringUtils.replaceGroupRegex(url, groupRegex, context);
//        }
//
//        // web client data from objects
//        return httpClient.request(url, serviceEndpoint.method(), returnType, clientConfig);
//    }
//
//}
