package io.github.wycst.wast.common.expression;

import io.github.wycst.wast.common.expression.functions.BuiltInFunction;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.BeanUtils;
import io.github.wycst.wast.common.utils.CollectionUtils;
import io.github.wycst.wast.common.utils.ObjectUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.MathContext;
import java.net.URLClassLoader;
import java.util.*;

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
    Object context;
    private Map<String, Object> variables = new HashMap<String, Object>();
    private Map<String, ExprParser> computedEls = new LinkedHashMap<String, ExprParser>();
    boolean computable;
    boolean mapContext;
    boolean allowVariableNull;
    /**
     * 字符串+默认情况下进行拼接，且不支持除+以外其他运算，开启后自动将字符串变量转化为double类型（字符串常量逻辑不变）
     */
    private boolean autoParseStringAsDouble;
    MathContext mathContext = MathContext.DECIMAL128;

    // 静态函数列表（静态类+函数名）
    private Map<String, Method> staticMethods = new HashMap<String, Method>();
    private Set<Class> staticClassSet = new HashSet<Class>();

    private final static Map<String, Method> BUILT_IN_STATIC_METHODS = new HashMap<String, Method>();
    private static final Set<Class> BLACKLIST_LIST = CollectionUtils.setOf(
            System.class,
            Runtime.class,
            URLClassLoader.class
    );

    static {
        registerStaticMethods(true, BuiltInFunction.class, BUILT_IN_STATIC_METHODS);
    }

    static final EvaluateEnvironment DEFAULT = new EvaluateEnvironment();

    public void setAutoParseStringAsDouble(boolean autoParseStringAsDouble) {
        this.autoParseStringAsDouble = autoParseStringAsDouble;
    }

    public final boolean isAutoParseStringAsDouble() {
        return autoParseStringAsDouble;
    }

    public void setAllowVariableNull(boolean allowVariableNull) {
        this.allowVariableNull = allowVariableNull;
    }

    public EvaluateEnvironment allowVariableNull(boolean allowVariableNull) {
        this.allowVariableNull = allowVariableNull;
        return this;
    }

    public final boolean isAllowVariableNull() {
        return allowVariableNull;
    }

    public void setMathContext(MathContext mathContext) {
        mathContext.toString();
        this.mathContext = mathContext;
    }

    public EvaluateEnvironment mathContext(MathContext mathContext) {
        this.mathContext = mathContext;
        return this;
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
                UnsafeHelper.setAccessible(method);
                String methodName = method.getName();
                staticMethods.put(global ? methodName : className + "." + methodName, method);
            }
        }
    }

    protected EvaluateEnvironment() {
        staticMethods.putAll(BUILT_IN_STATIC_METHODS);
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
            if (BLACKLIST_LIST.contains(clazz)) continue;
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
     * @param key   根变量
     * @param value
     * @return
     */
    public EvaluateEnvironment binding(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    public EvaluateEnvironment removeBinding(String key) {
        variables.remove(key);
        computedEls.remove(key);
        computable = !computedEls.isEmpty();
        return this;
    }

    /**
     * 绑定计算变量
     *
     * @param key        根变量
     * @param computedEl
     * @return
     */
    public EvaluateEnvironment bindingComputed(String key, String computedEl) {
        ExprParser exprParser = new ExprParser(computedEl);
        if (exprParser.isConstantExpr()) {
            variables.put(key, exprParser.evaluate());
        } else {
            if (exprParser.getInvokes().containsKey(key)) {
                throw new IllegalArgumentException("There is a variable with the same name key in the expression for '" + key + "'");
            }
            computedEls.put(key, exprParser);
            computable = true;
        }
        return this;
    }

    public EvaluateEnvironment clearFunctions() {
        functionMap.clear();
        staticMethods.clear();
        staticClassSet.clear();
        return this;
    }

    public EvaluateEnvironment clear() {
        clearFunctions();
        variables.clear();
        computedEls.clear();
        computable = false;
        return this;
    }

    final Object computedVariables() {
        return computedVariables(getContext());
    }

    final Object computedVariables(Object context) {
        if (!computable) {
            return context;
        }
        Map<String, Object> newContext = newContext(context);
        // 迭代两次
        Collection<Map.Entry<String, ExprParser>> entrySet = computedEls.entrySet();
        int count = entrySet.size();
        while (count-- > 0) {
            List<Map.Entry<String, ExprParser>> nextEntrys = new ArrayList<Map.Entry<String, ExprParser>>();
            for (Map.Entry<String, ExprParser> entry : entrySet) {
                String key = entry.getKey();
                if (newContext.containsKey(key)) {
                    continue;
                }
                ExprParser exprParser = entry.getValue();
                // 检查变量依赖
                if (checkVariableDependencies(newContext, exprParser.getInvokes())) {
                    // 保存执行结果,注意这里可能出现的死循环
                    Object result = exprParser.evaluateInternal(newContext, this);
                    newContext.put(key, result);
                } else {
                    nextEntrys.add(entry);
                }
            }
            if (nextEntrys.isEmpty()) {
                break;
            }
            entrySet = nextEntrys;
        }

        computable = true;
        return newContext;
    }

    private Map<String, Object> newContext(Object context) {
        Map<String, Object> newContext = new HashMap<String, Object>(variables);
        if (context instanceof Map) {
            newContext.putAll((Map) context);
        } else {
            BeanUtils.copy(context, newContext);
        }
        return newContext;
    }

    private boolean checkVariableDependencies(Map<String, Object> context, Map<String, ElVariableInvoker> invokes) {
        Set<Map.Entry<String, ElVariableInvoker>> entrySet = invokes.entrySet();
        for (Map.Entry<String, ElVariableInvoker> entry : entrySet) {
            ElVariableInvoker invoker = entry.getValue();
            if (invoker.parent == null && !context.containsKey(entry.getKey())) {
                return false;
            }
        }
        return true;
    }

//    /**
//     * 上下文是否为空
//     *
//     * @return
//     */
//    public boolean isEmptyContext() {
//        return useVariables ? variables.isEmpty() : context == null;
//    }

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
                if (parameterLength == 1 && parameterTypes[0].isArray()) {
                    Class<?> componentType = parameterTypes[0].getComponentType();
                    Object arrParam = Array.newInstance(componentType, params.length);
                    int i = 0;
                    for (Object param : params) {
                        Array.set(arrParam, i++, ObjectUtils.toType(param, componentType));
                    }
                    return method.invoke(null, new Object[]{arrParam});
                } else {
                    for (int i = 0; i < parameterTypes.length; ++i) {
                        Object val = params[i];
                        Class<?> parameterType = parameterTypes[i];
                        params[i] = ObjectUtils.toType(val, parameterType);
                    }
                    return method.invoke(null, params);
                }
            } catch (Throwable e) {
                throw new ExpressionException(" method['@" + method + "'] invoke error: " + e.getMessage(), e);
            }
        }
    }

}
