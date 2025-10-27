//package io.github.wycst.wast.clients.http.service.entity;
//
//import io.github.wycst.wast.clients.http.service.annotations.ServiceEndpoint;
//
//import java.lang.annotation.Annotation;
//
/// **
// * @Author: wangy
// * @Date: 2021/8/15 8:50
// * @Description:
// */
//public class ServiceEndpointMapping {
//
//    private String url;
//    private Class<?> mappingClass;
//    private String[] parameterNames;
//    private String[] parameterAliasNames;
//    private Class<?>[] parameterTypes;
//    private Annotation[][] parameterAnnotations;
//    private Class<?> returnType;
//    private String contentType;
//
//    // 是否上传
//    private boolean multipart;
//    // 是否使用requestBody
//    private boolean requestBody;
//    // 1 = from
//    private ParamType[] paramTypes;
//    private long timeout;
//    private boolean keepAliveOnTimeout;
//
//    private ServiceEndpoint serviceEndpoint;
//
//    public String getUrl() {
//        return url;
//    }
//
//    public void setUrl(String url) {
//        this.url = url;
//    }
//
//    public Class<?> getMappingClass() {
//        return mappingClass;
//    }
//
//    public void setMappingClass(Class<?> mappingClass) {
//        this.mappingClass = mappingClass;
//    }
//
//    public String[] getParameterNames() {
//        return parameterNames;
//    }
//
//    public void setParameterNames(String[] parameterNames) {
//        this.parameterNames = parameterNames;
//    }
//
//    public String[] getParameterAliasNames() {
//        return parameterAliasNames;
//    }
//
//    public void setParameterAliasNames(String[] parameterAliasNames) {
//        this.parameterAliasNames = parameterAliasNames;
//    }
//
//    public Class<?>[] getParameterTypes() {
//        return parameterTypes;
//    }
//
//    public void setParameterTypes(Class<?>[] parameterTypes) {
//        this.parameterTypes = parameterTypes;
//    }
//
//    public Annotation[][] getParameterAnnotations() {
//        return parameterAnnotations;
//    }
//
//    public void setParameterAnnotations(Annotation[][] parameterAnnotations) {
//        this.parameterAnnotations = parameterAnnotations;
//    }
//
//    public Class<?> getReturnType() {
//        return returnType;
//    }
//
//    public void setReturnType(Class<?> returnType) {
//        this.returnType = returnType;
//    }
//
//    public String getContentType() {
//        return contentType;
//    }
//
//    public void setContentType(String contentType) {
//        this.contentType = contentType;
//    }
//
//    public ServiceEndpoint getServiceEndpoint() {
//        return serviceEndpoint;
//    }
//
//    public void setServiceEndpoint(ServiceEndpoint serviceEndpoint) {
//        this.serviceEndpoint = serviceEndpoint;
//    }
//
//    public boolean isMultipart() {
//        return multipart;
//    }
//
//    public void setMultipart(boolean multipart) {
//        this.multipart = multipart;
//    }
//
//    public boolean isRequestBody() {
//        return requestBody;
//    }
//
//    public void setRequestBody(boolean requestBody) {
//        this.requestBody = requestBody;
//    }
//
//    public ParamType[] getParamTypes() {
//        return paramTypes;
//    }
//
//    public void setParamTypes(ParamType[] paramTypes) {
//        this.paramTypes = paramTypes;
//    }
//
//    public long getTimeout() {
//        return timeout;
//    }
//
//    public void setTimeout(long timeout) {
//        this.timeout = timeout;
//    }
//
//    public boolean isKeepAliveOnTimeout() {
//        return keepAliveOnTimeout;
//    }
//
//    public void setKeepAliveOnTimeout(boolean keepAliveOnTimeout) {
//        this.keepAliveOnTimeout = keepAliveOnTimeout;
//    }
//
//    /***
//     * 参数类型
//     *
//     */
//    public enum ParamType {
//
//        /***
//         * 路径域
//         *
//         */
//        PathField,
//
//        /***
//         * 表单域
//         *
//         */
//        FormField,
//
//        /**
//         * 表单参数
//         *
//         */
//        FormObject,
//
//        /**
//         * 文件域
//         * */
//        FileField,
//
//        /**
//         * 请求体
//         */
//        RequestBody
//    }
//}
