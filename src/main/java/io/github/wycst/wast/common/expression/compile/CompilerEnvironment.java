package io.github.wycst.wast.common.expression.compile;

import io.github.wycst.wast.common.expression.*;
import io.github.wycst.wast.common.expression.functions.JavassistExprFunction;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * 表达式编译环境
 *
 * @Author: wangy
 * @Date: 2021/11/20 0:00
 * @Description:
 */
public class CompilerEnvironment extends EvaluateEnvironment {

    public CompilerEnvironment() {
    }

    final static CompilerEnvironment COMPILER_DEFAULT = new CompilerEnvironment();

    // 跳过解析直接使用源代码运行，支持所有java代码；
    private boolean skipParse;
    // 默认禁用system类调用
    private boolean enableSystem;
    // 禁用安全检查
    private boolean disableSecurityCheck;
    // disable keys
    private Set<String> disableKeys = new HashSet<String>();
    private boolean supportedContextRoot;
    private Class objectParameterClass;

    Map<String, ExprFunctionMeta> javassistFunctionMetaMap = new LinkedHashMap<String, ExprFunctionMeta>();

    final Set<Class> importSet = new HashSet<Class>();

    // 变量类型映射
    private Map<String, Class> variableTypeMap = new LinkedHashMap<String, Class>();
    // 变量类型映射
    private List<TypeNameInvoker> typeNameInvokers = new ArrayList<TypeNameInvoker>();

    protected List<TypeNameInvoker> getTypeNameInvokers() {
        return typeNameInvokers;
    }

    protected Map<String, Class> getVariableTypes() {
        return variableTypeMap;
    }

    /**
     * 拓展注册函数,仅仅在javassist编译下生效；
     * <p>
     * note: 解决在javassist编译下泛型和基本类型（泛型不支持）引发生成的方法导致表达式调用类型不匹配问题；
     * ps: 如果使用javassist编译带有自定义注册函数的表达式，请使用数组替换可变参数（javassist不支持）；
     *
     * @param name     函数得名称，再表达式种通过@标识符调用
     * @param function 函数对象
     * @return
     */
    public EvaluateEnvironment registerJavassistFunction(String name, JavassistExprFunction function, Class<?> returnClass, Class<?>... paramClassList) {
        name.getClass();
        returnClass.getClass();
        function.getClass();
        registerFunction(name, function);
        ExprFunctionMeta exprFunctionMeta = new ExprFunctionMeta(name, function, returnClass, paramClassList);
        javassistFunctionMetaMap.put(name, exprFunctionMeta);
        return this;
    }

    public final Expression compile(String el) {
        return CompilerExpression.compile(el, this, CompilerExpression.Coder.Native);
    }

    static class ExprFunctionMeta {
        String name;
        JavassistExprFunction function;
        Class<?> returnClass;
        Class<?>[] paramClassList;

        ExprFunctionMeta(String name, JavassistExprFunction function, Class<?> returnClass, Class<?>[] paramClassList) {
            this.name = name;
            this.function = function;
            this.returnClass = returnClass;
            this.paramClassList = paramClassList;
        }
    }

    static class TypeNameInvoker implements Comparable<TypeNameInvoker> {
        final String varName;
        final String defineJavaIdentifier;
        final Class type;
        final ElVariableInvoker variableInvoker;

        public TypeNameInvoker(String varName, String defineJavaIdentifier, Class type, ElVariableInvoker variableInvoker) {
            this.varName = varName;
            this.defineJavaIdentifier = defineJavaIdentifier;
            this.type = type;
            this.variableInvoker = variableInvoker;
        }

        @Override
        public int compareTo(TypeNameInvoker o) {
            return varName.length() > o.varName.length() ? -1 : 1;
        }
    }

    /**
     * 编译模式设置context参数的类型
     *
     * @param objectParameterClass
     */
    public CompilerEnvironment objectParameterClass(Class objectParameterClass) {
        if (!Modifier.isPublic(objectParameterClass.getModifiers()) || objectParameterClass.isInterface()) {
            throw new UnsupportedOperationException("only support public class");
        }
        this.objectParameterClass = objectParameterClass;
        return this;
    }

    public CompilerEnvironment variableTypes(Class<?> type, String... vars) {
        return setVariableType(type, vars);
    }

    public CompilerEnvironment setVariableType(Class<?> type, String... vars) {
        for (String var : vars) {
            if (type == System.class) continue;
            if (type == Runtime.class) continue;
            if (!Modifier.isPublic(type.getModifiers())) {
                throw new UnsupportedOperationException(type + " is not public access");
            }
            variableTypeMap.put(var, type);
        }
        return this;
    }

    public CompilerEnvironment importSet(Class<?>... importClasses) {
        for (Class<?> importClass : importClasses) {
            if (importClass.isPrimitive() || importClass.getName().startsWith("java.lang.") || Modifier.isPrivate(importClass.getModifiers())) {
                continue;
            }
            importSet.add(importClass);
        }
        return this;
    }

    public Set<Class> getImportSet() {
        return importSet;
    }

    public CompilerEnvironment clearImports() {
        importSet.clear();
        return this;
    }

    public CompilerEnvironment setSupportedContextRoot(boolean supportedContextRoot) {
        this.supportedContextRoot = supportedContextRoot;
        return this;
    }

    public boolean isSupportedContextRoot() {
        return supportedContextRoot;
    }

    public Class getObjectParameterClass() {
        return objectParameterClass;
    }

    /**
     * 通过解析器初始化invoke
     *
     * @param parser
     */
    void initTypeNameInvokers(CompilerExprParser parser) {
        // 清除设置的变量信息
        typeNameInvokers.clear();
        if (parser.getVariableCount() == 0) return;
        Collection<ElVariableInvoker> tailInvokers = parser.getTailVariableInvokers(); // variableValues.tailInvokers();
        for (ElVariableInvoker tailInvoker : tailInvokers) {
            String varName = "_$" + tailInvoker.getIndex();
            Class type = variableTypeMap.get(tailInvoker.toString());
            if (type == null) {
                type = double.class;
            }
            typeNameInvokers.add(new TypeNameInvoker(varName, varName, type, tailInvoker));
        }
    }

    /**
     * 通过variableTypeMap初始化invoke（SkipParse mode）
     */
    void initTypeNameInvokers() {
        if (variableTypeMap.size() == 0) return;

        typeNameInvokers.clear();
        HashMap<String, ElVariableInvoker> invokes = new HashMap<String, ElVariableInvoker>();
        HashMap<String, ElVariableInvoker> tailInvokes = new HashMap<String, ElVariableInvoker>();

        Set<Map.Entry<String, Class>> entrySet = variableTypeMap.entrySet();
        for (Map.Entry<String, Class> entry : entrySet) {
            String var = entry.getKey();
            Class type = entry.getValue();
            typeNameInvokers.add(new TypeNameInvoker(var, var.replace('.', '_'), type, ElVariableUtils.build(var, invokes, tailInvokes)));
        }
        Collections.sort(typeNameInvokers);
        ElInvoker variableValues = ElChainVariableInvoker.build(invokes, true);
        variableValues.internKey();
    }

    public boolean isSkipParse() {
        return skipParse;
    }

    public void setSkipParse(boolean skipParse) {
        this.skipParse = skipParse;
    }

    public CompilerEnvironment skipParse(boolean skipParse) {
        this.skipParse = skipParse;
        return this;
    }

    public boolean isEnableSystem() {
        return enableSystem;
    }

    public CompilerEnvironment enableSystem(boolean enableSystem) {
        this.enableSystem = enableSystem;
        return this;
    }

    public boolean isDisableSecurityCheck() {
        return disableSecurityCheck;
    }

    public CompilerEnvironment disableSecurityCheck(boolean disableSecurityCheck) {
        this.disableSecurityCheck = disableSecurityCheck;
        return this;
    }

    @Override
    protected Map<String, ExprFunction> getFunctionMap() {
        return super.getFunctionMap();
    }

    @Override
    protected ExprFunction getFunction(String functionName) {
        return super.getFunction(functionName);
    }

    /**
     * 设置黑名单词组关键字调用防注入漏洞
     */
    public CompilerEnvironment setKeyBlacklist(String... keys) {
        for (String key : keys) {
            disableKeys.add(key);
        }
        return this;
    }

    public CompilerEnvironment setKeyBlacklist(Collection keys) {
        if (keys != null) {
            disableKeys.addAll(keys);
        }
        return this;
    }

    public Set<String> getDisableKeys() {
        return disableKeys;
    }
}
