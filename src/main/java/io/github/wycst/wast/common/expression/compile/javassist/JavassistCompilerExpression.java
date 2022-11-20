package io.github.wycst.wast.common.expression.compile.javassist;

import io.github.wycst.wast.common.expression.compile.CompilerEnvironment;
import io.github.wycst.wast.common.expression.compile.CompilerExpression;

import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2022/11/13 21:09
 * @Description:
 */
public abstract class JavassistCompilerExpression extends CompilerExpression {

    protected JavassistCompilerExpression(CompilerEnvironment environment) {
        super(environment);
    }

    @Override
    public Object evaluate(Map context) {
        ValueHolder valueHolder = new ValueHolder();
        doInvoke(context, valueHolder);
        return valueHolder.value;
    }

    @Override
    public Object evaluate(Object context) {
        ValueHolder valueHolder = new ValueHolder();
        doInvoke(context, valueHolder);
        return valueHolder.value;
    }

    protected abstract void doInvoke(Map context, ValueHolder valueHolder);

    protected abstract void doInvoke(Object context, ValueHolder valueHolder);
}
