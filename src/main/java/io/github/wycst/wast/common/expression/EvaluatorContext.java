package io.github.wycst.wast.common.expression;

import io.github.wycst.wast.common.expression.invoker.Invoker;
import io.github.wycst.wast.common.expression.invoker.VariableInvoker;

import java.util.Map;

/**
 * 执行上下文
 *
 * @Author wangyunchao
 * @Date 2022/11/4 16:21
 */
public class EvaluatorContext {

    public final static EvaluatorContext Empty = new EvaluatorContext();
    Object context;

    Object value;

    EvaluatorContext() {
    }

    public Object getContext() {
        return context;
    }

    /**
     * 获取上下文中变量的值
     *
     * @param variableInvoke
     * @return
     */
    public Object getContextValue(VariableInvoker variableInvoke) {
        return variableInvoke.getValue();
    }

    /**
     * 填充表达式中所有位置变量的值
     *
     * @param invoke
     */
    public void setContextVariables(Invoker invoke, Object context) {
        this.context = context;
        if (invoke == null) return;
        invoke.invoke(context);
    }

    /**
     * 填充表达式中所有位置变量的值
     *
     * @param invoke
     */
    public void setContextVariables(Invoker invoke, Map<String, Object> mapContext) {
        this.context = mapContext;
        if (invoke == null) return;
        invoke.invoke(mapContext);
    }

    public void clearContextVariables(Invoker invoke) {
        if (invoke == null) return;
        invoke.reset();
    }

    /**
     * <p> 变量访问安全上下文
     * <p> 有状态，每次执行创建新实例</p>
     * <p> 给执行器提供安全调用；</p>
     */
    static class StatefulEvaluatorContext extends EvaluatorContext {

        Object[] variableValues;

        public StatefulEvaluatorContext() {
        }

        public Object getContextValue(VariableInvoker variableInvoke) {
            return variableValues[variableInvoke.getIndex()];
        }

        public void setContextVariables(Invoker invoke, Object context, int variableCount) {
            this.context = context;
            if (variableCount == 0) return;
            variableValues = initArgumentValues(variableCount); // new Object[variableCount];
            invoke.invoke(context, variableValues);
        }

        public void setContextVariables(Invoker invoke, Map<String, Object> mapContext, int variableCount) {
            this.context = mapContext;
            if (variableCount == 0) return;
            variableValues = initArgumentValues(variableCount);
            invoke.invoke(mapContext, variableValues);
        }

        private Object[] initArgumentValues(int variableCount) {
            return new Object[variableCount];
        }

        public void clearContextVariables(Invoker invoke) {
            // Do nothing in security mode
            this.variableValues = null;
            this.context = null;
        }
    }
}
