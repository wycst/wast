package io.github.wycst.wast.common.expression.compile;

import io.github.wycst.wast.common.beans.KeyValuePair;
import io.github.wycst.wast.common.compiler.JDKCompiler;
import io.github.wycst.wast.common.compiler.JavaSourceObject;
import io.github.wycst.wast.common.exceptions.ParserException;
import io.github.wycst.wast.common.expression.ExprFunction;
import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.expression.ExpressionException;
import io.github.wycst.wast.common.expression.functions.JavassistExprFunction;
import io.github.wycst.wast.common.expression.invoker.VariableInvoker;
import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author wangyunchao
 * @Date 2022/11/17 15:09
 */
class CompilerCodeUtils {

    private static final AtomicLong ATOMIC_LONG = new AtomicLong(0);
    private static final String PACKAGE_NAME = CompilerExpression.class.getPackage().getName();

    private static final String NativeJavaCodeTemplate =
            "package io.github.wycst.wast.common.expression.compile;\r\n" +
                    "import io.github.wycst.wast.common.expression.ExprFunction;\r\n" +
                    "import io.github.wycst.wast.common.expression.invoker.VariableInvoker;\r\n" +
                    "public class ${className} extends CompilerExpression {\r\n" +
                    "\r\n" +
                    "\tpublic ${className}(CompilerEnvironment environment){\r\n" +
                    "\t\tsuper(environment);\r\n" +
                    "${initInvokes}" +
                    "\t}\r\n" +
                    "\r\n" +
                    "${declareInvokes}" +
                    "${registerFunctions}" +
                    "\r\n" +
                    "\tprotected Object invoke(Object context) {\r\n" +
                    "${assignmentObjectVariables}" +
                    "\t\treturn ${expressionCode};\r\n" +
                    "\t}\r\n" +
                    "\r\n" +
                    "\tprotected Object invoke(java.util.Map context) {\r\n" +
                    "${assignmentMapVariables}" +
                    "\t\treturn ${expressionCode};\r\n" +
                    "\t}\r\n" +
                    "\r\n" +
                    "}";

    private static final Map<Class, KeyValuePair<String, String>> primitiveValueMethods = new ConcurrentHashMap<Class, KeyValuePair<String, String>>();

    static {
        KeyValuePair<String, String> intType = new KeyValuePair<String, String>("int", "intValue");
        KeyValuePair<String, String> longType = new KeyValuePair<String, String>("long", "longValue");
        KeyValuePair<String, String> doubleType = new KeyValuePair<String, String>("double", "doubleValue");
        KeyValuePair<String, String> floatType = new KeyValuePair<String, String>("float", "floatValue");
        KeyValuePair<String, String> shortType = new KeyValuePair<String, String>("short", "shortValue");
        KeyValuePair<String, String> byteType = new KeyValuePair<String, String>("byte", "byteValue");
        KeyValuePair<String, String> booleanType = new KeyValuePair<String, String>("boolean", "booleanValue");
        KeyValuePair<String, String> charType = new KeyValuePair<String, String>("char", "charValue");
        primitiveValueMethods.put(int.class, intType);
        primitiveValueMethods.put(long.class, longType);
        primitiveValueMethods.put(double.class, doubleType);
        primitiveValueMethods.put(float.class, floatType);
        primitiveValueMethods.put(short.class, shortType);
        primitiveValueMethods.put(byte.class, byteType);
        primitiveValueMethods.put(boolean.class, booleanType);
        primitiveValueMethods.put(char.class, charType);
        primitiveValueMethods.put(Integer.class, intType);
        primitiveValueMethods.put(Long.class, longType);
        primitiveValueMethods.put(Double.class, doubleType);
        primitiveValueMethods.put(Float.class, floatType);
        primitiveValueMethods.put(Short.class, shortType);
        primitiveValueMethods.put(Byte.class, byteType);
    }

    static String generateJavaCode(String expr, CompilerEnvironment environment) {
        return generateJavaCode(expr, generateClassName(), environment);
    }

    /**
     * 生成java类代码
     *
     * @param expr
     * @param expressionClassName
     * @param environment
     * @return
     */
    static String generateJavaCode(String expr, String expressionClassName, CompilerEnvironment environment) {
        boolean skipParse = environment.isSkipParse();
        String exprCode;
        String sourceCode;
        if (skipParse) {
            environment.initTypeNameInvokers();
            expr = prepareElOnSkipMode(expr, environment);
            sourceCode = generateJavaSourceCode(expr, expressionClassName, environment);
        } else {
            // prepare parse expr and generate code as new el
            CompilerExprParser exprParser = new CompilerExprParser(expr);
            exprCode = exprParser.code();
            environment.initTypeNameInvokers(exprParser);
            sourceCode = generateJavaSourceCode(exprCode, expressionClassName, environment);
        }
        return sourceCode;
    }

    static String generateClassName() {
        return "CEL$_" + ATOMIC_LONG.getAndIncrement();
    }

    static CompilerExpression compileByNative(String expr, CompilerEnvironment environment) {
        String expressionClassName = generateClassName();
        final String javaCode = generateJavaCode(expr, expressionClassName, environment);
        try {
            Class<?> clazz = JDKCompiler.compileJavaSource(new JavaSourceObject(PACKAGE_NAME, expressionClassName, javaCode));
            Constructor<?> clazzConstructor = clazz.getConstructor(CompilerEnvironment.class);
            clazzConstructor.setAccessible(true);
            return (CompilerExpression) clazzConstructor.newInstance(environment);
        } catch (Throwable e) {
            if (e instanceof ExpressionException) throw (ExpressionException) e;
            throw new ParserException(" parse exception :" + e.getMessage(), e);
        }
    }

    // generate by javassist
    static CompilerExpression compileByJavassist(String expr, CompilerEnvironment environment) {

        boolean skipParse = environment.isSkipParse();
        if (skipParse) {
            environment.initTypeNameInvokers();
            expr = prepareElOnSkipMode(expr, environment).trim();
        } else {
            // prepare parse expr and generate code as new el
            CompilerExprParser exprParser = new CompilerExprParser(expr);
            expr = exprParser.code().trim();
            environment.initTypeNameInvokers(exprParser);
        }

        // build javassist code fragments
        // className, superClass
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("className", generateClassName());
        vars.put("superclass", CompilerExpression.class.getName());

        StringBuilder constructorBodyBuilder = new StringBuilder();
        constructorBodyBuilder.append("{\n");
        constructorBodyBuilder.append("\t\tsuper($1);\n");

        StringBuilder invokeObjMethodBuilder = new StringBuilder();
        StringBuilder invokeMapMethodBuilder = new StringBuilder();
        invokeObjMethodBuilder.append("protected void doInvoke(Object context, ValueHolder valueHolder) {\n");
        invokeMapMethodBuilder.append("protected void doInvoke(java.util.Map context, ValueHolder valueHolder) {\n");

        List<String> functionSources = new ArrayList<String>();

        List<String> fieldSources = new ArrayList<String>();

        List<CompilerEnvironment.TypeNameInvoker> typeNameInvokers = environment.getTypeNameInvokers();
        Set<Integer> defineIndexs = new HashSet<Integer>();

        Map<String, String> defineJavaIdentifiers = new HashMap<String, String>();
        Map<String, Class> definedTypes = new HashMap<String, Class>(environment.getVariableTypes());
        Map<String, Class> deductionTypes = new HashMap<String, Class>();
        Map<Integer, Class> tailTypes = new HashMap<Integer, Class>();

        int ofIndex = 0;
        for (CompilerEnvironment.TypeNameInvoker typeNameInvoker : typeNameInvokers) {
            String defineJavaIdentifier = typeNameInvoker.defineJavaIdentifier;
            VariableInvoker invoker = typeNameInvoker.variableInvoker;
            int index = invoker.getIndex();
            // default type
            tailTypes.put(index, typeNameInvoker.type);

            if (defineIndexs.add(index)) {
                constructorBodyBuilder.append("\t\t$0._invoke_").append(index).append(" = getInvokerAt(").append(ofIndex).append(");\r\n");
                fieldSources.add("final VariableInvoker _invoke_" + index + ";\r\n");
            }

            // define and init invoke fields
            VariableInvoker parent = invoker.getParent();

            VariableInvoker _parentInvoker = parent;
            int currentIndex = index;
            while (_parentInvoker != null) {
                int parentIndex = _parentInvoker.getIndex();
                if (defineIndexs.add(parentIndex)) {
                    constructorBodyBuilder.append("\t\t$0._invoke_").append(parentIndex).append(" = $0._invoke_").append(currentIndex).append(".getParent();\r\n");
                    fieldSources.add("final VariableInvoker _invoke_" + parentIndex + ";\r\n");
                }
                _parentInvoker = _parentInvoker.getParent();
                currentIndex = parentIndex;
            }
            generateInvokerCodeTo(
                    invoker,
                    defineJavaIdentifier,
                    invokeObjMethodBuilder,
                    invokeMapMethodBuilder,
                    defineJavaIdentifiers,
                    definedTypes,
                    deductionTypes,
                    tailTypes);
            ++ofIndex;
        }
        constructorBodyBuilder.append("}");

        if (expr.startsWith("return ")) {
            // delete return && ;
            int len = expr.length();
            while (len > 7 && expr.charAt(len - 1) == ';') {
                --len;
            }
            invokeObjMethodBuilder.append("\t\t$2.setValue(").append(expr, 7, len).append(");\n").append("}");
            invokeMapMethodBuilder.append("\t\t$2.setValue(").append(expr, 7, len).append(");\n").append("}");
        } else {
            // delete ;
            int len = expr.length();
            while (len > 7 && expr.charAt(len - 1) == ';') {
                --len;
            }
            invokeObjMethodBuilder.append("\t\t$2.setValue(").append(expr, 0, len).append(");\n").append("}");
            invokeMapMethodBuilder.append("\t\t$2.setValue(").append(expr, 0, len).append(");\n").append("}");
        }

        vars.put("constructorBody", constructorBodyBuilder.toString());
        vars.put("fieldSources", fieldSources);
        vars.put("invokeObjectSource", invokeObjMethodBuilder.toString());
        vars.put("invokeMapSource", invokeMapMethodBuilder.toString());
        vars.put("functionSources", functionSources);

        // register javassist functions
        // 1、javassist not support variable parameters
        // 2、if use Coder.Javassist mode, Please use an array in the expression, such as new int[] {1, 2, 3} instead of (1,2,3)
        // 3、eg: @min(new int[]{1,2,3,4}) instead of @min(1,2,3)
        Map<String, ExprFunction> functionMap = environment.getFunctionMap();
        Set<Map.Entry<String, ExprFunction>> functionEntrys = functionMap.entrySet();

        if (functionEntrys.size() > 0) {
            for (Map.Entry<String, ExprFunction> functionEntry : functionEntrys) {
                StringBuilder functionBuilder = new StringBuilder();
                String functionName = functionEntry.getKey();
                // exprFunction types
                ExprFunction function = functionEntry.getValue();
                if (function instanceof JavassistExprFunction) continue;
                Type[] types = getFunctionGenericTypes(function.getClass());
                Class<?> inClass = types == null ? Object.class : (Class<?>) types[0];
                Class<?> outClass = types == null ? Object.class : (Class<?>) types[1];
                String inClassName = inClass.getName();
                String outClassName = outClass.getName();
                functionBuilder.append("\tprivate ").append(outClassName).append(" ").append(functionName).append("(").append(inClassName).append("[] params) {\r\n");
                functionBuilder.append("\t\tExprFunction function = getFunction(\"").append(functionName).append("\");\r\n");
                functionBuilder.append("\t\treturn function.call(params);\r\n");
                functionBuilder.append("\t}\r\n");
                functionSources.add(functionBuilder.toString());
            }
        }

        Map<String, CompilerEnvironment.ExprFunctionMeta> javassistFunctionMetaMap = environment.javassistFunctionMetaMap;
        Set<Map.Entry<String, CompilerEnvironment.ExprFunctionMeta>> functionMetaEntrySet = javassistFunctionMetaMap.entrySet();
        if (functionMetaEntrySet.size() > 0) {
            for (Map.Entry<String, CompilerEnvironment.ExprFunctionMeta> functionMetaEntry : functionMetaEntrySet) {
                String functionName = functionMetaEntry.getKey();
                // exprFunction types
                CompilerEnvironment.ExprFunctionMeta functionMeta = functionMetaEntry.getValue();
                Class<?> outClass = functionMeta.returnClass;
                Class<?>[] paramClassList = functionMeta.paramClassList;
                String outClassName = outClass.getName();

                StringBuilder functionBuilder = new StringBuilder();
                functionBuilder.append("\tprivate ").append(outClassName).append(" ").append(functionName).append("(");
                for (int i = 0, len = paramClassList.length; i < len; ++i) {
                    functionBuilder.append(paramClassList[i].getName()).append(" $").append(i + 1);
                    if (i < len - 1) {
                        functionBuilder.append(",");
                    }
                }
                functionBuilder.append(") {\r\n");
                functionBuilder.append("\t\tExprFunction function = getFunction(\"").append(functionName).append("\");\r\n");
                functionBuilder.append("\t\treturn ($r)function.call($args);\r\n");
                functionBuilder.append("\t}\r\n");
                functionSources.add(functionBuilder.toString());

                if (paramClassList.length > 0) {
                    functionBuilder.setLength(0);
                    functionBuilder.append("\tprivate ").append(outClassName).append(" ").append(functionName).append("(");
                    functionBuilder.append(paramClassList[0].getName()).append("[] params");
                    functionBuilder.append(") {\r\n");
                    functionBuilder.append("\t\tJavassistExprFunction function = (JavassistExprFunction) getFunction(\"").append(functionName).append("\");\r\n");
                    functionBuilder.append("\t\treturn ($r)function.call($1);\r\n");
                    functionBuilder.append("\t}\r\n");
                    functionSources.add(functionBuilder.toString());

                    if (paramClassList[0] != Object.class) {
                        functionBuilder.setLength(0);
                        functionBuilder.append("\tprivate ").append(outClassName).append(" ").append(functionName).append("(Object[] params) {\r\n");
                        functionBuilder.append("\t\tJavassistExprFunction function = (JavassistExprFunction) getFunction(\"").append(functionName).append("\");\r\n");
                        functionBuilder.append("\t\treturn ($r)function.call($1);\r\n");
                        functionBuilder.append("\t}\r\n");
                        functionSources.add(functionBuilder.toString());
                    }
                }
            }
        }

        if (!CompilerExpressionCoder.isJavassistSupported()) {
            throw new UnsupportedOperationException("Javassist toolkit dependencies not imported");
        }

        return CompilerExpressionCoder.JavassistCoder.compile(vars, environment);
    }

    // 检查安全代码
    // 替换特殊token标记，比如@符号，字符串单引号替换",变量替换
    private static String prepareElOnSkipMode(String expr, CompilerEnvironment environment) {

        StringBuilder builder = new StringBuilder();
        char[] chars = UnsafeHelper.getChars(expr);
        int offset = 0, len = chars.length;
        String fragment = null;
        for (int i = 0; i < len; i++) {
            char ch = chars[i], prev = 0;
            if (ch == '"') {
                fragment = new String(chars, offset, i - offset);
                // 替换变量
                builder.append(checkAndReplaceVariable(fragment, environment));
                // goto next "
                int j = i + 1;
                while (j < len && ((ch = chars[j]) != '"' || prev == '\\')) {
                    prev = ch;
                    ++j;
                }
                if (ch != '"' || prev == '\\') {
                    throw new ExpressionException("Expression syntax error, from pos " + i + ", End token '\"' not found");
                }
                builder.append(new String(chars, i, j - i + 1));
                i = j;
                offset = ++j;
                continue;
            }

            if (ch == '\'') {
                fragment = new String(chars, offset, i - offset);
                // 替换变量
                builder.append(checkAndReplaceVariable(fragment, environment));
                // goto next "
                int j = i + 1;
                while (j < len && ((ch = chars[j]) != '\'' || prev == '\\')) {
                    prev = ch;
                    ++j;
                }
                if (ch != '\'' || prev == '\\') {
                    throw new ExpressionException("Expression syntax error, from pos " + i + ", End token '\'' not found");
                }
                builder.append('"').append(new String(chars, i + 1, j - i - 1)).append('"');
                i = j;
                offset = ++j;
                continue;
            }
            if (ch == '@') {
                fragment = new String(chars, offset, i - offset);
                // 替换变量
                builder.append(checkAndReplaceVariable(fragment, environment));
                offset = i + 1;
                continue;
            }
        }

        fragment = new String(chars, offset, len - offset);
        builder.append(checkAndReplaceVariable(fragment, environment));

        return builder.toString();
    }

    private static String checkAndReplaceVariable(String fragment, CompilerEnvironment environment) {

        List<CompilerEnvironment.TypeNameInvoker> typeNameInvokers = environment.getTypeNameInvokers();
        for (CompilerEnvironment.TypeNameInvoker typeNameInvoker : typeNameInvokers) {
            String varName = typeNameInvoker.varName;
            String defineJavaIdentifier = typeNameInvoker.defineJavaIdentifier;
            if (varName != defineJavaIdentifier) {
                fragment = fragment.replace(varName, defineJavaIdentifier);
            }
        }

        if (!environment.isDisableSecurityCheck()) {
            Set<String> disableKeys = environment.getDisableKeys();
            String codeExpr = fragment;
            String[] lines = codeExpr.split(";");
            for (String line : lines) {
                if (!environment.isEnableSystem() && line.indexOf("System") > -1) {
                    throw new SecurityException(String.format(" 编译出现了不支持的安全关键字： 'System', code fragment: '%s'", fragment));
                }
                if (line.indexOf("Runtime") > -1) {
                    throw new SecurityException(String.format(" 编译出现了不支持的安全关键字： 'Runtime', code fragment: '%s'", fragment));
                }
                if (line.indexOf("Class") > -1) {
                    throw new SecurityException(String.format(" 编译出现了不支持的安全关键字： 'Class', code fragment: '%s'", fragment));
                }
                for (String key : disableKeys) {
                    if (line.indexOf(key) > -1) {
                        throw new SecurityException(String.format(" 编译出现了不支持的安全关键字： '%s', code fragment: '%s'", key, fragment));
                    }
                }
            }
        }

        return fragment;
    }

    /**
     * 生成java本地代码
     *
     * @param source
     * @param expressionClassName
     * @param environment
     * @return
     */
    private static String generateJavaSourceCode(String source, String expressionClassName, CompilerEnvironment environment) {
        StringBuilder initInvokesBuilder = new StringBuilder();
        StringBuilder declareInvokesBuilder = new StringBuilder();
        StringBuilder assignmentObjectVariablesBuilder = new StringBuilder();
        StringBuilder assignmentMapVariablesBuilder = new StringBuilder();
        StringBuilder registerFunctionsBuilder = new StringBuilder();

        Map<String, CharSequence> vars = new HashMap<String, CharSequence>();

        List<CompilerEnvironment.TypeNameInvoker> typeNameInvokers = environment.getTypeNameInvokers();
        Set<Integer> defineIndexs = new HashSet<Integer>();

        Map<String, String> defineJavaIdentifiers = new HashMap<String, String>();
        Map<String, Class> definedTypes = environment.getVariableTypes();
        Map<String, Class> deductionTypes = new HashMap<String, Class>();
        Map<Integer, Class> tailTypes = new HashMap<Integer, Class>();

        int ofIndex = 0;
        for (CompilerEnvironment.TypeNameInvoker typeNameInvoker : typeNameInvokers) {
            // String varName = typeNameInvoker.varName;
            String defineJavaIdentifier = typeNameInvoker.defineJavaIdentifier;
            VariableInvoker invoker = typeNameInvoker.variableInvoker;
            int index = invoker.getIndex();
            // default type
            tailTypes.put(index, typeNameInvoker.type);

            if (defineIndexs.add(index)) {
                initInvokesBuilder.append("\t\tthis._invoke_").append(index).append(" = getInvokerAt(").append(ofIndex).append(");\r\n");
                declareInvokesBuilder.append("\tfinal VariableInvoker _invoke_").append(index).append(";\r\n");
            }

            // define and init invoke fields
            VariableInvoker parent = invoker.getParent();

            VariableInvoker _parentInvoker = parent;
            int currentIndex = index;
            while (_parentInvoker != null) {
                int parentIndex = _parentInvoker.getIndex();
                if (defineIndexs.add(parentIndex)) {
                    initInvokesBuilder.append("\t\tthis._invoke_").append(parentIndex).append(" = this._invoke_").append(currentIndex).append(".getParent();\r\n");
                    declareInvokesBuilder.append("\tfinal VariableInvoker _invoke_").append(parentIndex).append(";\r\n");
                }
                _parentInvoker = _parentInvoker.getParent();
                currentIndex = parentIndex;
            }

            generateInvokerCodeTo(
                    invoker,
                    defineJavaIdentifier,
                    assignmentObjectVariablesBuilder,
                    assignmentMapVariablesBuilder,
                    defineJavaIdentifiers,
                    definedTypes,
                    deductionTypes,
                    tailTypes
            );
            ++ofIndex;
        }

        // registerFunctions
        Map<String, ExprFunction> functionMap = environment.getFunctionMap();
        Set<Map.Entry<String, ExprFunction>> functionEntrys = functionMap.entrySet();

        if (functionEntrys.size() > 0) {
            registerFunctionsBuilder.append("\r\n");
            for (Map.Entry<String, ExprFunction> functionEntry : functionEntrys) {
                String functionName = functionEntry.getKey();
                // exprFunction types
                ExprFunction function = functionEntry.getValue();
                if (function instanceof JavassistExprFunction) continue;
                Type[] types = getFunctionGenericTypes(function.getClass());

                Class<?> inClass = types == null ? Object.class : (Class<?>) types[0];
                Class<?> outClass = types == null ? Object.class : (Class<?>) types[1];
                String inClassName = inClass.getName();
                String outClassName = outClass.getName();

                registerFunctionsBuilder.append("\t@SuppressWarnings(\"unchecked\")\r\n");
                registerFunctionsBuilder.append("\tprivate ").append(outClassName).append(" ").append(functionName).append("(").append(inClassName).append("...params) {\r\n");
                registerFunctionsBuilder.append("\t\tExprFunction<").append(inClassName).append(",").append(outClassName).append("> function = getFunction(\"").append(functionName).append("\");\r\n");
                registerFunctionsBuilder.append("\t\treturn function.call(params);\r\n");
                registerFunctionsBuilder.append("\t}\r\n");
            }
        }

        vars.put("className", expressionClassName);
        vars.put("expressionCode", source);
        vars.put("initInvokes", initInvokesBuilder);
        vars.put("declareInvokes", declareInvokesBuilder);
        vars.put("assignmentObjectVariables", assignmentObjectVariablesBuilder);
        vars.put("assignmentMapVariables", assignmentMapVariablesBuilder);
        vars.put("registerFunctions", registerFunctionsBuilder);

        return Expression.renderTemplate(NativeJavaCodeTemplate, vars);
    }

    /**
     * 根据不同场景生成不同的invoke代码
     *
     * @param variableInvoker       变量表达式模型
     * @param defineJavaIdentifier  定义标识符变量
     * @param objVariablesBuilder   字符串构建器StringBuilder对象
     * @param mapVariablesBuilder   字符串构建器StringBuilder对象
     * @param defineJavaIdentifiers 已经编码的变量集合，防止重复,初始化new
     * @param definedTypes          通过编译环境预制设置的类型映射
     * @param deductionTypes        通过自动推导动态添加的类型映射，初始化new
     * @param tailTypes
     */
    private static void generateInvokerCodeTo(VariableInvoker variableInvoker, String defineJavaIdentifier, StringBuilder objVariablesBuilder, StringBuilder mapVariablesBuilder, Map<String, String> defineJavaIdentifiers, Map<String, Class> definedTypes, Map<String, Class> deductionTypes, Map<Integer, Class> tailTypes) {
        if (variableInvoker == null) return;
        VariableInvoker parent = variableInvoker.getParent();
        // generate parent
        generateInvokerCodeTo(parent, null, objVariablesBuilder, mapVariablesBuilder, defineJavaIdentifiers, definedTypes, deductionTypes, tailTypes);

        int index = variableInvoker.getIndex();
        String key = variableInvoker.getKey();
        boolean childEl = variableInvoker.isChildEl();
        boolean tail = variableInvoker.isTail();

        String invokeUniqueKey = variableInvoker.toString();
        if (defineJavaIdentifier == null) {
            if (definedTypes.containsKey(invokeUniqueKey)) {
                defineJavaIdentifier = invokeUniqueKey.replace('.', '_');
            } else {
                defineJavaIdentifier = "_$" + index;
            }
        }
        if (!defineJavaIdentifiers.containsValue(defineJavaIdentifier)) {
            objVariablesBuilder.append("\t\t");
            mapVariablesBuilder.append("\t\t");
            if (parent == null) {
                // use 'context' as context
                String typeName = null;
                Class<?> defineType = definedTypes.get(invokeUniqueKey);

                if (defineType == null && tail && tailTypes.containsKey(index)) {
                    defineType = tailTypes.get(index);
                }

                if (defineType != null) {
                    typeName = getTypeName(defineType); // defineType.getName();
                    objVariablesBuilder.append(typeName).append(" ").append(defineJavaIdentifier).append(" = ");
                    mapVariablesBuilder.append(typeName).append(" ").append(defineJavaIdentifier).append(" = ");
                    if (defineType.isPrimitive()) {
                        // 基本类型不能强转（T）为Object类型的对象，使用类似intValue/doubleValue过渡
                        String primitiveMethodValue = primitiveValueMethods.get(defineType).getValue();
                        objVariablesBuilder.append(primitiveMethodValue).append("(");
                        mapVariablesBuilder.append(primitiveMethodValue).append("(");
                        if (!childEl) {
                            // T v = getValue(context.get(key),T.class);
                            mapVariablesBuilder.append("context.get(\"").append(key).append("\")");
                        } else {
                            // T v = getValue(_invoke_2.invokeValue(context),T.class);
                            mapVariablesBuilder.append("_invoke_").append(index).append(".invokeValue(context)");
                        }
                        objVariablesBuilder.append("_invoke_").append(index).append(".invokeValue(context)");

                        objVariablesBuilder.append(");\r\n");
                        mapVariablesBuilder.append(");\r\n");
                    } else {
                        // 使用（T）强制转换
                        objVariablesBuilder.append("(").append(typeName).append(") ");
                        mapVariablesBuilder.append("(").append(typeName).append(") ");
                        if (!childEl) {
                            // T v = (T) context.get(key);
                            mapVariablesBuilder.append("context.get(\"").append(key).append("\");\r\n");
                        } else {
                            // T v = (T) _invoke_2.invokeValue(context);
                            mapVariablesBuilder.append("_invoke_").append(index).append(".invokeValue(context);\r\n");
                        }
                        objVariablesBuilder.append("_invoke_").append(index).append(".invokeValue(context);\r\n");
                        // 非基本类型添加到推导映射中
                        deductionTypes.put(invokeUniqueKey, defineType);
                    }
                } else {
                    // use Object
                    objVariablesBuilder.append("Object ").append(defineJavaIdentifier).append(" = ");
                    mapVariablesBuilder.append("Object ").append(defineJavaIdentifier).append(" = ");
                    if (!childEl) {
                        // T v = (T) context.get(key);
                        mapVariablesBuilder.append("context.get(\"").append(key).append("\");\r\n");
                    } else {
                        // T v = (T) _invoke_2.invokeValue(context);
                        mapVariablesBuilder.append("_invoke_").append(index).append(".invokeValue(context);\r\n");
                    }
                    objVariablesBuilder.append("_invoke_").append(index).append(".invokeValue(context);\r\n");
                }
            } else {
                // use value var of parent invoked as context；
                String parentUniqueKey = parent.toString();
                String parentValueVar = defineJavaIdentifiers.get(parentUniqueKey);
                if (parentValueVar == null) {
                    throw new IllegalStateException("Unexpected exception");
                }
                Class<?> parentType = deductionTypes.get(parentUniqueKey);
                // 是否声明了类型,优先在声明的defineTypes中查找是否定义了类型
                Class<?> defineType = definedTypes.get(invokeUniqueKey);

                boolean forceConversionType = defineType != null;
                boolean forcePrimitiveType = forceConversionType ? defineType.isPrimitive() : false;
                GetterInfo getterInfo = null;
                boolean isParentMapType = false;
                if (parentType != null && !parentType.isPrimitive()) {
                    if (Map.class.isAssignableFrom(parentType)) {
                        isParentMapType = true;
                    } else {
                        ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(parentType);
                        if (classCategory == ReflectConsts.ClassCategory.ObjectCategory) {
                            // pojo checks whether there is a get method declared by public, and deduces the return type
                            ClassStructureWrapper parentStructureWrapper = ClassStructureWrapper.get(parentType);
                            getterInfo = getGetterInfo(parentStructureWrapper, key);
                            if (getterInfo != null) {
                                if (getterInfo.getReturnType() == defineType) {
                                    // No forced conversion required
                                    forceConversionType = false;
                                    forcePrimitiveType = false;
                                }
                            }
                        }
                        if (defineType == null) {
                            defineType = getterInfo == null ? null : getterInfo.getReturnType();
                        }
                    }

                    if (defineType == null) {
                        if (tail && tailTypes.containsKey(index)) {
                            defineType = tailTypes.get(index);
                        } else {
                            defineType = Object.class;
                        }
                    }

                    String typeName = getTypeName(defineType);
                    objVariablesBuilder.append(typeName).append(" ").append(defineJavaIdentifier).append(" = ");
                    mapVariablesBuilder.append(typeName).append(" ").append(defineJavaIdentifier).append(" = ");
                    if (forceConversionType) {
                        if (forcePrimitiveType) {
                            // 基本类型不能强转（T）为Object类型的对象，使用类似intValue/doubleValue过渡
                            String primitiveMethodValue = primitiveValueMethods.get(defineType).getValue();
                            objVariablesBuilder.append(primitiveMethodValue).append("(");
                            mapVariablesBuilder.append(primitiveMethodValue).append("(");
                        } else {
                            objVariablesBuilder.append("(").append(typeName).append(") ");
                            mapVariablesBuilder.append("(").append(typeName).append(") ");
                        }
                    }
                    if (getterInfo != null) {
                        objVariablesBuilder.append(parentValueVar).append(".").append(getterInfo.getMethodName()).append("()");
                        mapVariablesBuilder.append(parentValueVar).append(".").append(getterInfo.getMethodName()).append("()");
                    } else if (isParentMapType && !childEl) {
                        objVariablesBuilder.append(parentValueVar).append(".get(\"").append(key).append("\")");
                        mapVariablesBuilder.append(parentValueVar).append(".get(\"").append(key).append("\")");
                    } else {
                        objVariablesBuilder.append("_invoke_").append(index).append(".invokeValue(").append(parentValueVar).append(")");
                        mapVariablesBuilder.append("_invoke_").append(index).append(".invokeValue(").append(parentValueVar).append(")");
                    }
                    if (forcePrimitiveType) {
                        objVariablesBuilder.append(");\r\n");
                        mapVariablesBuilder.append(");\r\n");
                    } else {
                        objVariablesBuilder.append(";\r\n");
                        mapVariablesBuilder.append(";\r\n");
                    }
                } else {
                    // Unknown type uses compatible calling code
                    // example： int a = intValue(_invoke_0.invokeValue(_$2));
                    String typeName = null;

                    if (defineType == null && tail && tailTypes.containsKey(index)) {
                        defineType = tailTypes.get(index);
                    }

                    if (defineType != null) {
                        typeName = getTypeName(defineType); // defineType.getName();
                        objVariablesBuilder.append(typeName).append(" ").append(defineJavaIdentifier).append(" = ");
                        mapVariablesBuilder.append(typeName).append(" ").append(defineJavaIdentifier).append(" = ");
                        if (defineType.isPrimitive()) {
                            // 基本类型不能强转（T）为Object类型的对象，使用intValue/doubleValue过渡
                            String primitiveMethodValue = primitiveValueMethods.get(defineType).getValue();
                            objVariablesBuilder.append(primitiveMethodValue).append("(_invoke_").append(index).append(".invokeValue(").append(parentValueVar).append("));\r\n");
                            mapVariablesBuilder.append(primitiveMethodValue).append("(_invoke_").append(index).append(".invokeValue(").append(parentValueVar).append("));\r\n");
                        } else {
                            // 使用（T）强制转换
                            objVariablesBuilder.append("(").append(typeName).append(") _invoke_").append(index).append(".invokeValue(").append(parentValueVar).append(");\r\n");
                            mapVariablesBuilder.append("(").append(typeName).append(") _invoke_").append(index).append(".invokeValue(").append(parentValueVar).append(");\r\n");
                            // 非基本类型添加到推导映射中
                            deductionTypes.put(invokeUniqueKey, defineType);
                        }
                    } else {
                        // use Object
                        objVariablesBuilder.append("Object ").append(defineJavaIdentifier).append(" = _invoke_").append(index).append(".invokeValue(").append(parentValueVar).append(");\r\n");
                        mapVariablesBuilder.append("Object ").append(defineJavaIdentifier).append(" = _invoke_").append(index).append(".invokeValue(").append(parentValueVar).append(");\r\n");
                    }
                }

                // add to mapping
                if (defineType != null && defineType != Object.class && !defineType.isPrimitive()) {
                    deductionTypes.put(invokeUniqueKey, defineType);
                }
            }
            // 缓存invoke的变量defineJavaIdentifier
            defineJavaIdentifiers.put(invokeUniqueKey, defineJavaIdentifier);
        }
    }

    private static GetterInfo getGetterInfo(ClassStructureWrapper parentStructureWrapper, String key) {
        List<GetterInfo> getterInfos = parentStructureWrapper.getGetterInfos();
        for (GetterInfo getterInfo : getterInfos) {
            if (key.equals(getterInfo.getName()) || key.equals(getterInfo.getUnderlineName())) {
                return getterInfo;
            }
        }
        return null;
    }

    /**
     * @param type
     * @return
     * @see Class#getCanonicalName()
     */
    private static String getTypeName(Class<?> type) {
        if (type.isArray()) {
            StringBuilder builder = new StringBuilder();
            Class componentType = type.getComponentType();
            int cnt = 1;
            while (componentType.isArray()) {
                ++cnt;
                componentType = componentType.getComponentType();
            }
            builder.append(componentType.getName());
            for (int i = 0; i < cnt; i++) {
                builder.append("[]");
            }
            return builder.toString();
        } else {
            return type.getName();
        }
    }

    /**
     * 获取function的泛型列表
     *
     * @param targetClass
     * @return
     */
    private static Type[] getFunctionGenericTypes(Class<?> targetClass) {
        Type[] implementTypes = targetClass.getGenericInterfaces();
        if (implementTypes.length == 0) {
            Class<?> parentCls = targetClass.getSuperclass();
            implementTypes = parentCls.getGenericInterfaces();
        }
        for (Type implementType : implementTypes) {
            if (implementType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) implementType;
                Type[] types = parameterizedType.getActualTypeArguments();
                if (types != null && types.length > 0) {
                    return types;
                }
            }
            return null;
        }
        return null;
    }
}
