package io.github.wycst.wast.common.expression.compile;

import io.github.wycst.wast.common.expression.EvaluateEnvironment;
import io.github.wycst.wast.common.expression.ExprFunction;
import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.expression.invoker.VariableInvoker;

import java.util.Map;

/**
 * @Author wangyunchao
 * @Date 2021/11/19 18:39
 */
public abstract class CompilerExpression extends Expression {

    private static Coder DefaultCoder;

    static {
        if (CompilerExpressionCoder.isJavassistSupported()) {
            DefaultCoder = Coder.Javassist;
        } else {
            DefaultCoder = Coder.Native;
        }
    }

    public enum Coder {

        /**
         * java内置编译器
         */
        Native,

        /**
         * 表达式解析存在问题
         */
        Javassist,
//        Asm
    }

    public static void setDefaultCoder(Coder defaultCoder) {
        CompilerExpression.DefaultCoder = defaultCoder;
    }

    protected final CompilerEnvironment environment;

    protected CompilerExpression(CompilerEnvironment environment) {
        this.environment = environment;
    }

    public static CompilerEnvironment createEnvironment() {
        return new CompilerEnvironment();
    }

    /**
     * 根据解析器转化为代码编译生成字节码然后反射为表达式类
     *
     * @param expr
     * @return
     */
    public static CompilerExpression compile(String expr) {
        return compile(expr, createEnvironment());
    }

    /**
     * 生成java类代码
     *
     * @param expr
     * @param environment
     * @return
     */
    public static String generateJavaCode(String expr, CompilerEnvironment environment) {
        return CompilerCodeUtils.generateJavaCode(expr, environment);
    }

    /**
     * 编译表达式 - 字节码实现
     *
     * @param expr
     * @param environment env
     * @return
     */
    public static CompilerExpression compile(String expr, CompilerEnvironment environment) {
        return compile(expr, environment, DefaultCoder);
    }

    /**
     * 编译表达式 - 字节码实现
     *
     * @param expr        el
     * @param environment env
     * @param coder       Native/Javassist
     * @return
     */
    public static CompilerExpression compile(String expr, CompilerEnvironment environment, Coder coder) {
        if (coder == null) {
            coder = DefaultCoder == null ? Coder.Native : DefaultCoder;
        }
        switch (coder) {
            case Native:
                return CompilerCodeUtils.compileByNative(expr, environment);
            case Javassist:
                return CompilerCodeUtils.compileByJavassist(expr, environment);
        }
        throw new UnsupportedOperationException("unknown coder " + coder);
    }

    /**
     * Javassist test, which will be removed in the future
     *
     * @param expr
     * @param environment
     * @return
     */
    @Deprecated
    public static CompilerExpression compileJavassist(String expr, CompilerEnvironment environment) {
        return CompilerCodeUtils.compileByJavassist(expr, environment);
    }

    protected final ExprFunction getFunction(String functionName) {
        return environment.getFunction(functionName);
    }

    @Override
    public final Object evaluate() {
        return evaluate((Object) null);
    }

    @Override
    public Object evaluate(Map context) {
        return invoke(context);
    }

    @Override
    public Object evaluate(Object context) {
        return invoke(context);
    }

    @Override
    public final Object evaluate(EvaluateEnvironment evaluateEnvironment) {
        return evaluate(evaluateEnvironment.getContext());
    }

    protected final <T> T getValue(Object value, Class<T> tClass) {
        return (T) value;
    }

    protected VariableInvoker getInvokerAt(int index) {
        return environment.getTypeNameInvokers().get(index).variableInvoker;
    }

    protected Object invoke(Object context) {
        throw new UnsupportedOperationException();
    }

    protected Object invoke(Map context) {
        throw new UnsupportedOperationException();
    }

    protected final int intValue(Object value) {
        Number number = (Number) value;
        return number.intValue();
    }

    protected final byte byteValue(Object value) {
        Number number = (Number) value;
        return number.byteValue();
    }

    protected final double doubleValue(Object value) {
        Number number = (Number) value;
        return number.doubleValue();
    }

    protected final float floatValue(Object value) {
        Number number = (Number) value;
        return number.floatValue();
    }

    protected final long longValue(Object value) {
        Number number = (Number) value;
        return number.longValue();
    }

    protected final char charValue(Object value) {
        return (Character) value;
    }

    protected final short shortValue(Object value) {
        Number number = (Number) value;
        return number.shortValue();
    }

    protected final boolean booleanValue(Object value) {
        return (Boolean) value;
    }

    /**
     * 字符串使用valueOf处理转化
     *
     * @param value
     * @return
     */
    protected final String stringValue(Object value) {
        return String.valueOf(value);
    }

    protected final Object objectValue(Object value) {
        return value;
    }
}
