package org.framework.light.common.expression;

import org.framework.light.common.expression.functions.BuiltInFunction;

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
    private boolean useVariables;

    // 静态函数列表
    private Map<String, Method> staticMethods = new HashMap<String, Method>();
    private Set<Class> staticClassSet = new HashSet<Class>();

    private static Map<String, Method> builtInStaticMethods = new HashMap<String, Method>();
    static {
        registerStaticMethods(true, BuiltInFunction.class, builtInStaticMethods);
    }

    // 临时缓存
    private Map<String, ExprFunction> tempFunctionMap = new HashMap<String, ExprFunction>();

    private static void registerStaticMethods(boolean global, Class<?> functionClass, Map<String, Method> staticMethods) {
        String className = functionClass.getSimpleName();
        // 声明或者继承的public方法
        Method[] methods = functionClass.getMethods();
        // 静态方法如果存在重载情况，将追加后缀
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers())) {
                method.setAccessible(true);
                String methodName = method.getName();
                staticMethods.put(global ? methodName : className + "." + methodName, method);
            }
        }
    }

    protected EvaluateEnvironment() {
//        registerStaticMethods(true, BuiltInFunction.class);
        staticMethods.putAll(builtInStaticMethods);
    }

    /**
     * 获取执行上下文
     *
     * @return
     */
    public Object getEvaluateContext() {
        return useVariables ? variables : context;
    }

    /**
     * 使用variables作为参数上下文
     *
     * @return
     */
    public static EvaluateEnvironment create() {
        EvaluateEnvironment environment = new EvaluateEnvironment();
        environment.useVariables = true;
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
        return environment;
    }

    /**
     * 注册函数
     *
     * @param name     函数得名称，再表达式种通过@标识符调用
     * @param function 函数对象
     * @return
     */
    public EvaluateEnvironment registerFunction(String name, ExprFunction function) {
        functionMap.put(name.trim(), function);
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
        if(tempFunctionMap.containsKey(functionName)) {
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

    /**
     * 内置运行静态method的函数
     */
    private class MethodFunction implements ExprFunction {
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
