package io.github.wycst.wast.common.expression.compile;

import java.util.Map;

/**
 * 字节码编译器
 *
 * @Author: wangy
 * @Date: 2022/11/13 10:37
 * @Description:
 */
public abstract class CompilerExpressionCoder {

    final static boolean AsmSupported;
    final static boolean JavassistSupported;

    final static CompilerExpressionCoder JavassistCoder;

    static {
        AsmSupported = false;

        CompilerExpressionCoder javaassitCoder = null;
        boolean javassistSupported = false;
        String javaassitCoderClassName = CompilerExpressionCoder.class.getPackage().getName() + ".javassist.JavassistCompilerExpressionCoder";
        try {
            Class.forName("javassist.ClassPool");
            Class<?> coderImpl = Class.forName(javaassitCoderClassName);
            javaassitCoder = (CompilerExpressionCoder) coderImpl.newInstance();
            javassistSupported = true;
        } catch (Throwable e) {
        }

        JavassistSupported = javassistSupported;
        JavassistCoder = javaassitCoder;
    }

    static boolean isJavassistSupported() {
        return JavassistSupported;
    }

    /**
     * 动态编译生成Expression引擎类
     *
     * @param vars
     * @return
     */
    protected abstract CompilerExpression compile(Map<String, Object> vars, CompilerEnvironment environment);
}
