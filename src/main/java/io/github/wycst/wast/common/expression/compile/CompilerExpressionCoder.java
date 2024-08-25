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

    final static boolean ASM_SUPPORTED;
    final static boolean JAVASSIST_SUPPORTED;

    final static CompilerExpressionCoder JAVASSIST_CODER;

    static {
        ASM_SUPPORTED = false;

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

        JAVASSIST_SUPPORTED = javassistSupported;
        JAVASSIST_CODER = javaassitCoder;
    }

    static boolean isJavassistSupported() {
        return JAVASSIST_SUPPORTED;
    }

    /**
     * 动态编译生成Expression引擎类
     *
     * @param vars
     * @return
     */
    protected abstract CompilerExpression compile(Map<String, Object> vars, CompilerEnvironment environment);
}
