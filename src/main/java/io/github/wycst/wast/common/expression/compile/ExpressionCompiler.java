package io.github.wycst.wast.common.expression.compile;

import io.github.wycst.wast.common.compiler.MemoryClassLoader;
import io.github.wycst.wast.common.compiler.MemoryJavaFileManager;
import io.github.wycst.wast.common.compiler.MemoryJavaFileObject;
import io.github.wycst.wast.common.exceptions.ParserException;
import io.github.wycst.wast.common.expression.EvaluateEnvironment;
import io.github.wycst.wast.common.expression.ExprFunction;
import io.github.wycst.wast.common.expression.Expression;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author wangyunchao
 * @Date 2021/11/19 18:39
 */
public abstract class ExpressionCompiler extends Expression {

    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private static final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
    private static final AtomicLong ATOMIC_LONG = new AtomicLong(0);
    private static final String PACKAGE_NAME = ExpressionCompiler.class.getPackage().getName();
    // 编译后的源代码
    private String sourceCode;

    public static CompileEnvironment createEnvironment() {
        return new CompileEnvironment();
    }

    /**
     * 根据解析器转化为代码编译生成字节码然后反射为表达式类
     *
     * @param expr
     * @return
     */
    public static Expression compile(String expr) {
        return compile(expr, new CompileEnvironment());
    }

    /**
     * 编译，跳过解析
     *
     * @param expr
     * @param skipParse
     * @return
     */
    public static Expression compile(String expr, boolean skipParse) {
        CompileEnvironment compileEnvironment = new CompileEnvironment();
        compileEnvironment.setSkipParse(skipParse);
        return compile(expr, compileEnvironment);
    }

    /**
     * 编译表达式 - 字节码实现
     *
     * @param expr
     * @param environment 编译环境
     * @return
     */
    public static Expression compile(String expr, CompileEnvironment environment) {

        boolean skipParse = environment.isSkipParse();
        ExprParserCompiler exprParserCompiler = null;
        String exprCode = null;
        String expressionClassName = String.format("ExpressionCompiler$_%d", ATOMIC_LONG.getAndIncrement());

        String sourceCode;
        if (skipParse) {
            checkSecurityCode(expr, environment);
            sourceCode = generateNativeCode(expr, expressionClassName, environment);
        } else {
            exprParserCompiler = new ExprParserCompiler(expr);
            exprCode = exprParserCompiler.generateCode(environment);
            // 解析模式下不支持；
            sourceCode = generateJavaSourceCode(expressionClassName, null, new String[]{exprCode}, environment);
        }
        final String javaCode = sourceCode;
        try {
            MemoryJavaFileManager javaFileManager = new MemoryJavaFileManager(fileManager);
            JavaFileObject javaFileObject = javaFileManager.createJavaFileObject(expressionClassName + ".java", javaCode);
            JavaCompiler.CompilationTask task = compiler.getTask(null, javaFileManager, null,
                    Arrays.asList(/*"-d", classPath, */"-encoding", "UTF-8"/*, "-XDuseUnsharedTable"*/), null,
                    Arrays.asList(javaFileObject));
            boolean bl = task.call();
            if (bl) {
                MemoryJavaFileObject memoryJavaFileObject = javaFileManager.getMemoryJavaFileObject();
                MemoryClassLoader memoryClassLoader = new MemoryClassLoader(memoryJavaFileObject);
                Class<?> clazz = memoryClassLoader.loadClass(PACKAGE_NAME + "." + expressionClassName);
                Constructor<?> clazzConstructor = clazz.getConstructor(ExprParserCompiler.class, CompileEnvironment.class);
                clazzConstructor.setAccessible(true);

                ExpressionCompiler expressionCompiler = (ExpressionCompiler) clazzConstructor.newInstance(exprParserCompiler, environment);
                expressionCompiler.sourceCode = sourceCode;
                return expressionCompiler;
            }
        } catch (Throwable e) {
            throw new ParserException(" parse exception :" + e.getMessage(), e);
        }
        return null;
    }

    // 检查安全代码
    private static void checkSecurityCode(String expr, CompileEnvironment environment) {
        if (!environment.isDisableSecurityCheck()) {
            Set<String> disableKeys = environment.getDisableKeys();
            String codeExpr = expr.replaceAll("\".*?\"", "");
            String[] lines = codeExpr.split(";");
            for (String line : lines) {
                if (!environment.isEnableSystem() && line.indexOf("System.") > -1) {
                    throw new SecurityException(String.format(" 编译出现了不支持的安全关键字： 'System', code: '%s'", expr));
                }
                if (line.indexOf("Runtime.") > -1) {
                    throw new SecurityException(String.format(" 编译出现了不支持的安全关键字： 'Runtime', code: '%s'", expr));
                }
                for (String key : disableKeys) {
                    if (line.indexOf(key) > -1) {
                        throw new SecurityException(String.format(" 编译出现了不支持的安全关键字： '%s', code: '%s'", key, expr));
                    }
                }
            }
        }
    }

    /**
     * 生成java本地代码
     *
     * @param source
     * @param expressionClassName
     * @param environment
     * @return
     */
    private static String generateNativeCode(String source, String expressionClassName, CompileEnvironment environment) {
        Map<String, String> typeClassMap = environment.getTypeClassMap();
        Set<Map.Entry<String, String>> entrySet = typeClassMap.entrySet();
        String[] defines = new String[typeClassMap.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : entrySet) {
            String defineJavaIdentifier = entry.getKey().replace('.', '_');
            defines[i++] = String.format("%s %s = ObjectUtils.get(context, \"%s\", %s.class)", entry.getValue(), defineJavaIdentifier, entry.getKey(), entry.getValue());
            source = source.replace(entry.getKey(), defineJavaIdentifier);
        }
        String[] codeLines = source.split(";");
        return generateJavaSourceCode(expressionClassName, defines, codeLines, environment);
    }

    private static String generateJavaSourceCode(String className, String[] defines, String[] lines, CompileEnvironment environment) {
        StringBuffer javaCodeBuffer = new StringBuffer();
        javaCodeBuffer.append("package " + PACKAGE_NAME + ";\r\n");
        javaCodeBuffer.append("import io.github.wycst.wast.common.utils.*;\r\n");
        javaCodeBuffer.append("import io.github.wycst.wast.common.expression.ExprFunction;\r\n");
        javaCodeBuffer.append("public class ").append(className).append(" extends ExpressionCompiler {\r\n");
        javaCodeBuffer.append("\r\n");
        javaCodeBuffer.append("\tprivate ExprParserCompiler exprParserCompiler;\r\n");
        javaCodeBuffer.append("\tprivate CompileEnvironment environment;\r\n\r\n");
        javaCodeBuffer.append("\tpublic ").append(className).append("(ExprParserCompiler exprParserCompiler, CompileEnvironment environment){\r\n");
        javaCodeBuffer.append("\t\tthis.exprParserCompiler = exprParserCompiler;\r\n");
        javaCodeBuffer.append("\t\tthis.environment = environment;\r\n");
        javaCodeBuffer.append("\t}\r\n\r\n");
        Map<String, ExprFunction> functionMap = environment.getFunctionMap();
        if (functionMap.size() > 0) {
            for (String functionName : functionMap.keySet()) {
                ExprFunction function = functionMap.get(functionName);
                Class<?> actualReturnCls = getImplementActualType(function.getClass());
                javaCodeBuffer.append("\t@SuppressWarnings(\"unchecked\")\r\n");
                javaCodeBuffer.append("\tpublic  " + actualReturnCls.getName() + " " + functionName + "(Object...params) {\r\n");
                javaCodeBuffer.append("\t\tExprFunction<" + actualReturnCls.getName() + "> function = environment.getFunction(\"" + functionName + "\");\r\n");
                javaCodeBuffer.append("\t\treturn function.call(params);\r\n");
                javaCodeBuffer.append("\t}\r\n");
            }
        }

        javaCodeBuffer.append("\tpublic Object evaluate() {\r\n");
        javaCodeBuffer.append("\t\treturn evaluate(null);\r\n");
        javaCodeBuffer.append("\t}\r\n\r\n");
        javaCodeBuffer.append("\tpublic Object evaluate(Object context) {\r\n");

        if (defines != null) {
            for (String defLine : defines) {
                javaCodeBuffer.append("\t\t").append(defLine).append(";\r\n");
            }
            javaCodeBuffer.append("\r\n");
        }

        int i = 0, length = lines.length;
        for (String line : lines) {
            line = line.trim();
            if (i++ == length - 1 && !line.startsWith("return ")) {
                javaCodeBuffer.append("\t\treturn ").append(line).append(";\r\n");
            } else {
                javaCodeBuffer.append("\t\t").append(line).append(";\r\n");
            }
        }
        javaCodeBuffer.append("\t}\r\n\r\n");
        javaCodeBuffer.append("}");
        return javaCodeBuffer.toString();
    }

    public String getSourceCode() {
        return sourceCode;
    }

    /**
     * 获取实例实现的接口的泛型
     *
     * @param targetClass
     * @return
     */
    private static Class<?> getImplementActualType(Class<?> targetClass) {
        Type[] implementTypes = targetClass.getGenericInterfaces();
        if (implementTypes.length == 0) {
            Class<?> parentCls = (Class) targetClass.getGenericSuperclass();
            implementTypes = parentCls.getGenericInterfaces();
        }
        for (Type implementType : implementTypes) {
            if (implementType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) implementType;
                Type[] types = parameterizedType.getActualTypeArguments();
                if (types != null && types.length > 0) {
                    if (types[0] instanceof Class) {
                        return (Class<?>) types[0];
                    }
                }
            }
            return null;
        }
        return null;
    }

    @Override
    public final Object evaluate(EvaluateEnvironment evaluateEnvironment) {
        if(evaluateEnvironment == null) {
            evaluateEnvironment = EvaluateEnvironment.create();
        }
        return evaluate(evaluateEnvironment.getEvaluateContext());
    }
}
