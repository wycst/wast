package io.github.wycst.wast.common.expression;

import io.github.wycst.wast.common.expression.functions.BuiltInFunction;
import io.github.wycst.wast.common.expression.invoker.Invoker;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 执行环境
 *
 * @Author: wangy
 * @Date: 2021/11/29 23:50
 * @Description:
 */
public class EvaluateEnvironment {

    // 注册变量函数
    private Map<String, ExprFunction> functionMap = new HashMap<String, ExprFunction>();
    private Object context;
    private Map<String, Object> variables = new HashMap<String, Object>();
    boolean useVariables;
    private boolean safely;
    private boolean mapContext;

    /**
     * 字符串+默认情况下进行拼接，且不支持除+以外其他运算，开启后自动将字符串变量转化为double类型（字符串常量逻辑不变）
     */
    private boolean autoParseStringAsDouble;

    // 静态函数列表（静态类+函数名）
    private Map<String, Method> staticMethods = new HashMap<String, Method>();
    private Set<Class> staticClassSet = new HashSet<Class>();

    private static Map<String, Method> builtInStaticMethods = new HashMap<String, Method>();

    static {
        registerStaticMethods(true, BuiltInFunction.class, builtInStaticMethods);
    }

    // 通用执行环境
    static final EvaluateEnvironment DefaultEnvironment = new EvaluateEnvironment();

    public void setAutoParseStringAsDouble(boolean autoParseStringAsDouble) {
        this.autoParseStringAsDouble = autoParseStringAsDouble;
    }

    public boolean isAutoParseStringAsDouble() {
        return autoParseStringAsDouble;
    }

    public void setSafely(boolean safely) {
        this.safely = safely;
    }

    public boolean isSafely() {
        return safely;
    }

    // 临时缓存
    private Map<String, ExprFunction> tempFunctionMap = new HashMap<String, ExprFunction>();

    private static void registerStaticMethods(boolean global, Class<?> functionClass, Map<String, Method> staticMethods) {
        String className = functionClass.getSimpleName();
        // 声明或者继承的public方法
        Method[] methods = functionClass.getMethods();
        // 静态方法如果存在重载情况，将追加后缀
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())) {
                method.setAccessible(true);
                String methodName = method.getName();
                staticMethods.put(global ? methodName : className + "." + methodName, method);
            }
        }
    }

    protected EvaluateEnvironment() {
        staticMethods.putAll(builtInStaticMethods);
    }

    /**
     * 获取执行上下文
     *
     * @return
     */
    public Object getContext() {
        return context;
    }

    /**
     * 上下文对象是否为map
     *
     * @return
     */
    boolean isMapContext() {
        return mapContext;
    }

    /**
     * 使用variables作为参数上下文
     *
     * @return
     */
    public static EvaluateEnvironment create() {
        EvaluateEnvironment environment = new EvaluateEnvironment();
        environment.useVariables = true;
        environment.safely = true;
        environment.context = environment.variables;
        environment.mapContext = true;
        return environment;
    }

    /**
     * 根据上下文context构建执行环境
     *
     * @param context
     * @return
     */
    public static EvaluateEnvironment create(Object context) {
        if (context instanceof EvaluateEnvironment) {
            return (EvaluateEnvironment) context;
        }
        EvaluateEnvironment environment = new EvaluateEnvironment();
        environment.context = context;
        environment.safely = true;
        environment.mapContext = context instanceof Map;
        return environment;
    }

    /**
     * 注册函数
     *
     * @param name     函数名称，表达式中通过@标识符调用
     * @param function 函数对象
     * @return
     */
    public EvaluateEnvironment registerFunction(String name, ExprFunction function) {
        functionMap.put(name.trim(), function);
        return this;
    }

    /**
     * 取消注册函数
     *
     * @param name 函数名称，表达式中通过@标识符调用
     * @return
     */
    public EvaluateEnvironment unregisterFunction(String name) {
        functionMap.remove(name.trim());
        return this;
    }


    /**
     * 注册静态方法(public)
     *
     * @param classList
     * @return
     */
    public EvaluateEnvironment registerStaticMethods(Class<?>... classList) {
        return registerStaticMethods(false, classList);
    }

    /**
     * @param global    是否忽略类名
     * @param classList
     * @return
     */
    public EvaluateEnvironment registerStaticMethods(boolean global, Class<?>... classList) {
        for (Class<?> clazz : classList) {
            if (staticClassSet.add(clazz)) {
                registerStaticMethods(global, clazz, staticMethods);
            }
        }
        return this;
    }


    protected Map<String, ExprFunction> getFunctionMap() {
        return functionMap;
    }

    protected ExprFunction getFunction(String functionName) {
        functionName = functionName.trim();
        if (functionMap.containsKey(functionName)) {
            return functionMap.get(functionName);
        }

        // 临时缓存中查找
        if (tempFunctionMap.containsKey(functionName)) {
            return tempFunctionMap.get(functionName);
        }

        if (staticMethods.containsKey(functionName)) {
            ExprFunction exprFunction = new MethodFunction(functionName, staticMethods.get(functionName));
            tempFunctionMap.put(functionName, exprFunction);
            return exprFunction;
        }
        return null;
    }

    /**
     * 绑定参数上下文
     *
     * @param vars
     * @return
     */
    public EvaluateEnvironment bindings(Map<String, Object> vars) {
        variables.putAll(vars);
        return this;
    }

    /**
     * 绑定变量参数
     *
     * @param key
     * @param value
     * @return
     */
    public EvaluateEnvironment binding(String key, Object value) {
        if (useVariables) {
            variables.put(key, value);
        }
        return this;
    }

    public EvaluateEnvironment clearFunctions() {
        functionMap.clear();
        staticMethods.clear();
        staticClassSet.clear();
        return this;
    }

    public EvaluateEnvironment clearVariables() {
        variables.clear();
        return this;
    }

    /**
     * 上下文是否为空
     *
     * @return
     */
    public boolean isEmptyContext() {
        return useVariables ? variables.isEmpty() : context == null;
    }

    EvaluatorContext createEvaluateContext(Object context, Invoker chainInvoker) {
        EvaluatorContext evaluatorContext = new EvaluatorContext();
        evaluatorContext.setContextVariables(chainInvoker, context);
        return evaluatorContext;
    }

    EvaluatorContext createEvaluateContext(Map context, Invoker chainInvoker) {
        EvaluatorContext evaluatorContext = new EvaluatorContext();
        evaluatorContext.setContextVariables(chainInvoker, context);
        return evaluatorContext;
    }

    EvaluatorContext createEvaluateContext(Invoker chainInvoker, int count) {
        if (safely) {
            EvaluatorContext.StatefulEvaluatorContext evaluatorContext = new EvaluatorContext.StatefulEvaluatorContext();
            if (isMapContext()) {
                evaluatorContext.setContextVariables(chainInvoker, (Map) this.context, count);
            } else {
                evaluatorContext.setContextVariables(chainInvoker, this.context, count);
            }
            return evaluatorContext;
        }
        EvaluatorContext evaluatorContext = new EvaluatorContext();
        if (isMapContext()) {
            evaluatorContext.setContextVariables(chainInvoker, (Map) this.context);
        } else {
            evaluatorContext.setContextVariables(chainInvoker, this.context);
        }
        return evaluatorContext;
    }

    /**
     * 内置运行静态method的函数
     */
    private class MethodFunction implements ExprFunction<Object, Object> {
        private String name;
        private Method method;
        private Class[] parameterTypes;
        private int parameterLength;

        MethodFunction(String name, Method method) {
            this.name = name;
            this.method = method;
            this.parameterTypes = method.getParameterTypes();
            this.parameterLength = parameterTypes.length;
        }

        @Override
        public Object call(Object... params) {
            try {
                Object[] vars;
                // 处理method可变数组问题
                if (parameterLength == 1 && parameterTypes[0].isArray()) {
                    Class<?> componentType = parameterTypes[0].getComponentType();
                    Object arrParam = Array.newInstance(componentType, params.length);
                    int i = 0;
                    for (Object param : params) {
                        Array.set(arrParam, i++, param);
                    }
                    return method.invoke(null, new Object[]{arrParam});
                } else {
                    return method.invoke(null, params);
                }
            } catch (Throwable e) {
                throw new ExpressionException(" method['@" + method + "'] invoke error: " + e.getMessage(), e);
            }
        }
    }

}
