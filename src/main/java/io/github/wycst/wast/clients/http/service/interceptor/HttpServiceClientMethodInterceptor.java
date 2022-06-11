//package io.github.wycst.wast.clients.http.service.interceptor;
//
//import net.sf.cglib.proxy.*;
//import io.github.wycst.wast.core.bean.BeanRegistrationHandler;
//import io.github.wycst.wast.core.config.PropertiesHandler;
//import io.github.wycst.wast.core.factory.BeanFactory;
//
//import java.lang.reflect.Method;
//import java.lang.reflect.Modifier;
//
///**
// * @Author: wangy
// * @Date: 2021/8/14 20:48
// * @Description:
// */
//public abstract class HttpServiceClientMethodInterceptor implements MethodInterceptor, BeanRegistrationHandler {
//
//    private final Enhancer enhancer;
//
//    public HttpServiceClientMethodInterceptor() {
//        enhancer = new Enhancer();
//        enhancer.setCallbacks(new Callback[]{
//                this, NoOp.INSTANCE});
//        enhancer.setCallbackFilter(new CallbackFilter() {
//            public int accept(Method method) {
//                if (!Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers()) || method.getDeclaringClass() == Object.class) {
//                    return 1;
//                }
//                return 0;
//            }
//        });
//    }
//
//    public synchronized Object createHttpServiceClient(Class clazz) {
//        enhancer.setSuperclass(clazz);
//        return enhancer.create();
//    }
//
//    protected abstract Object invoke(Object obj, Method method, Object[] objects) throws Throwable;
//
//    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
//        if (!Modifier.isAbstract(method.getModifiers())) {
//            // if not abstract,call Super
//            return methodProxy.invokeSuper(o, objects);
//        }
//        return invoke(o, method, objects);
//    }
//
//    @Override
//    public void afterResolver(BeanFactory beanFactory, PropertiesHandler propertiesHandler) {
//
//    }
//
//    @Override
//    public void doFinishBeanFactoryInitial() {
//
//    }
//
//    @Override
//    public void destroy() {
//
//    }
//
//}
