package io.github.wycst.wast.common.expression.compile;

import io.github.wycst.wast.common.expression.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author wangyunchao
 * @Date 2021/11/19 18:39
 */
public abstract class CompilerExpression extends Expression {

    private static Coder defaultCoder;

    static {
        defaultCoder = Coder.Native;
    }

    public enum Coder {
        Native,
        Javassist,
    }

    public static void setDefaultCoder(Coder defaultCoder) {
        CompilerExpression.defaultCoder = defaultCoder;
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
        return compile(expr, CompilerEnvironment.COMPILER_DEFAULT);
    }

    /**
     * 生成java类代码
     *
     * @param expr
     * @param environment
     * @return
     */
    public static String generateJavaCode(String expr, CompilerEnvironment environment) {
        return CompilerCodeUtils.generateNativeJavaCode(expr, environment);
    }

    /**
     * 编译表达式 - 字节码实现
     *
     * @param expr
     * @param environment env
     * @return
     */
    public static CompilerExpression compile(String expr, CompilerEnvironment environment) {
        return compile(expr, environment, defaultCoder);
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
            coder = defaultCoder == null ? Coder.Native : defaultCoder;
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
        try {
            return invoke(context);
        } catch (Throwable e) {
            throw new ExpressionException(e.getMessage(), e);
        }
    }

    @Override
    public Object evaluate(Object context) {
        try {
            return invoke(context);
        } catch (Throwable e) {
            throw new ExpressionException(e.getMessage(), e);
        }
    }

    @Override
    public final Object evaluate(final Map context, final long timeout) {
        final ValueHolder valueHolder = new ValueHolder();
        final AtomicBoolean atomicBoolean = new AtomicBoolean();
        return evaluateAsync(valueHolder, atomicBoolean, new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    valueHolder.value = evaluate(context);
                } catch (Throwable throwable) {
                    if (!(throwable instanceof Error)) {
                        throwable.printStackTrace();
                    }
                } finally {
                    atomicBoolean.set(true);
                    synchronized (valueHolder) {
                        valueHolder.notify();
                    }
                }
            }
        }), timeout);
    }

    @Override
    public final Object evaluate(final Object context, final long timeout) {
        final ValueHolder valueHolder = new ValueHolder();
        final AtomicBoolean atomicBoolean = new AtomicBoolean();
        return evaluateAsync(valueHolder, atomicBoolean, new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    valueHolder.value = evaluate(context);
                } catch (Throwable throwable) {
                    if (!(throwable instanceof Error)) {
                        throwable.printStackTrace();
                    }
                } finally {
                    atomicBoolean.set(true);
                    synchronized (valueHolder) {
                        valueHolder.notify();
                    }
                }
            }
        }), timeout);
    }

    final Object evaluateAsync(final ValueHolder valueHolder, final AtomicBoolean atomicBoolean, Thread thread, long timeout) {
        try {
            thread.start();
            synchronized (valueHolder) {
                valueHolder.wait(timeout);
                if (!atomicBoolean.get()) {
                    thread.interrupt();
                }
            }
        } catch (Throwable throwable) {
        }
        return valueHolder.value;
    }

    @Override
    public final Object evaluate(EvaluateEnvironment evaluateEnvironment) {
        return evaluate(evaluateEnvironment.getContext());
    }

    @Override
    public final Object evaluate(Map context, EvaluateEnvironment evaluateEnvironment) {
        return evaluate(context);
    }

    @Override
    public final Object evaluate(Object context, EvaluateEnvironment evaluateEnvironment) {
        return evaluate(context);
    }

    @Override
    public final Object evaluateParameters(Object... params) {
        try {
            return invokeParameters(params);
        } catch (Throwable e) {
            throw new ExpressionException(e.getMessage(), e);
        }
    }

    @Override
    public final Object evaluateParameters(EvaluateEnvironment evaluateEnvironment, Object... params) {
        try {
            return invokeParameters(params);
        } catch (Throwable e) {
            throw new ExpressionException(e.getMessage(), e);
        }
    }

    protected final <T> T getValue(Object value, Class<T> tClass) {
        return (T) value;
    }

    protected ElVariableInvoker getInvokerAt(int index) {
        return environment.getTypeNameInvokers().get(index).variableInvoker;
    }

    protected Object invoke(Object context) throws Throwable {
        throw new UnsupportedOperationException();
    }

    protected Object invoke(Map context) throws Throwable {
        throw new UnsupportedOperationException();
    }

    protected Object invokeParameters(Object[] parameters) throws Throwable {
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
