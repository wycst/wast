package io.github.wycst.wast.common.expression;

import java.util.Map;

/**
 * eval context
 *
 * @Author wangyunchao
 * @Date 2022/11/4 16:21
 */
public class EvaluatorContext {

    public final static EvaluatorContext EMPTY = new EvaluatorContext();
    Object[] variableValues;
    StringBuilder builder;
    // use for ExprEvaluatorStackSplitImpl & ExprEvaluatorContextValueHolderImpl
    Object value;

    EvaluatorContext() {
    }

    public Object getContextValue(ElVariableInvoker variableInvoker) {
        try {
            return variableValues[variableInvoker.index];
        } catch (Throwable throwable) {
            throw new ExpressionException("Unresolved property or variable '" + variableInvoker + "' by context");
        }
    }

    public EvaluatorContext invokeVariables(ElInvoker invoke, Object context, int variableCount) {
        variableValues = variableCount < 10 ? new Object[10] : new Object[variableCount];
        invoke.invoke(context, variableValues);
        return this;
    }

    public EvaluatorContext invokeVariables(ElInvoker invoke, Map context, int variableCount) {
        variableValues = variableCount < 10 ? new Object[10] : new Object[variableCount];
        invoke.invoke(context, variableValues);
        return this;
    }

    final StringBuilder getStringBuilder() {
        if (builder == null) {
            builder = new StringBuilder();
        } else {
            builder.setLength(0);
        }
        return builder;
    }

    protected EvaluatorContext cloneContext() {
        return new EvaluatorContext();
    }

    final static class EvaluatorContextSingleVariableImpl extends EvaluatorContext {
        EvaluatorContextSingleVariableImpl() {
        }

        Object variableValue;

        @Override
        public Object getContextValue(ElVariableInvoker variableInvoker) {
            return variableValue;
        }

        @Override
        public EvaluatorContextSingleVariableImpl invokeVariables(ElInvoker invoke, Object context, int variableCount) {
            variableValue = invoke.invokeDirect(context);
            return this;
        }

        @Override
        public EvaluatorContextSingleVariableImpl invokeVariables(ElInvoker invoke, Map context, int variableCount) {
            variableValue = invoke.invokeDirect(context);
            return this;
        }

        @Override
        protected EvaluatorContext cloneContext() {
            return new EvaluatorContextSingleVariableImpl();
        }
    }

    final static class EvaluatorContextSibbingVariablesImpl extends EvaluatorContext {

        final ElVariableInvoker parent;

        EvaluatorContextSibbingVariablesImpl(ElVariableInvoker parent) {
            this.parent = parent;
        }

        @Override
        public EvaluatorContextSibbingVariablesImpl invokeVariables(ElInvoker invoke, Object context, int variableCount) {
            Object parentContext = parent.invokeDirect(context);
            variableValues = variableCount < 10 ? new Object[10] : new Object[variableCount];
            invoke.invokeCurrent(context, parentContext, variableValues);
            return this;
        }

        @Override
        public EvaluatorContextSibbingVariablesImpl invokeVariables(ElInvoker invoke, Map context, int variableCount) {
            Object parentContext = parent.invokeDirect(context);
            variableValues = variableCount < 10 ? new Object[10] : new Object[variableCount];
            invoke.invokeCurrent(context, parentContext, variableValues);
            return this;
        }

        @Override
        protected EvaluatorContext cloneContext() {
            return new EvaluatorContextSibbingVariablesImpl(parent);
        }
    }

    final static class EvaluatorContextTwinsImpl extends EvaluatorContext {

        final ElVariableInvoker parent;

        final ElVariableInvoker one;
        final ElVariableInvoker other;
        Object oneValue;
        Object otherValue;

        EvaluatorContextTwinsImpl(ElVariableInvoker parent, ElVariableInvoker one, ElVariableInvoker other) {
            this.parent = parent;
            this.one = one;
            this.other = other;
        }

        @Override
        public Object getContextValue(ElVariableInvoker variableInvoker) {
            return variableInvoker == one ? oneValue : otherValue;
        }

        @Override
        public EvaluatorContextTwinsImpl invokeVariables(ElInvoker invoke, Object context, int variableCount) {
            Object parentContext = parent.invokeDirect(context);
            oneValue = one.invokeCurrent(context, parentContext);
            otherValue = other.invokeCurrent(context, parentContext);
            return this;
        }

        @Override
        public EvaluatorContextTwinsImpl invokeVariables(ElInvoker invoke, Map context, int variableCount) {
            Object parentContext = parent.invokeDirect(context);
            oneValue = one.invokeCurrent(context, parentContext);
            otherValue = other.invokeCurrent(context, parentContext);
            return this;
        }

        @Override
        protected EvaluatorContext cloneContext() {
            return new EvaluatorContextTwinsImpl(parent, one, other);
        }
    }
}
