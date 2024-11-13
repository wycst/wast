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
            throw new ExpressionException("unresolved property or variable: '" + variableInvoker + "' from context");
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

    final static class SingleVariableImpl extends EvaluatorContext {
        SingleVariableImpl() {
        }

        Object variableValue;

        @Override
        public Object getContextValue(ElVariableInvoker variableInvoker) {
            return variableValue;
        }

        @Override
        public SingleVariableImpl invokeVariables(ElInvoker invoke, Object context, int variableCount) {
            variableValue = invoke.invokeDirect(context);
            return this;
        }

        @Override
        public SingleVariableImpl invokeVariables(ElInvoker invoke, Map context, int variableCount) {
            variableValue = invoke.invokeDirect(context);
            return this;
        }

        @Override
        protected EvaluatorContext cloneContext() {
            return new SingleVariableImpl();
        }
    }

    final static class SibbingVariablesImpl extends EvaluatorContext {

        final ElVariableInvoker parent;

        SibbingVariablesImpl(ElVariableInvoker parent) {
            this.parent = parent;
        }

        @Override
        public SibbingVariablesImpl invokeVariables(ElInvoker invoke, Object context, int variableCount) {
            Object parentContext = parent.invokeDirect(context);
            variableValues = variableCount < 10 ? new Object[10] : new Object[variableCount];
            invoke.invokeCurrent(context, parentContext, variableValues);
            return this;
        }

        @Override
        public SibbingVariablesImpl invokeVariables(ElInvoker invoke, Map context, int variableCount) {
            Object parentContext = parent.invokeDirect(context);
            variableValues = variableCount < 10 ? new Object[10] : new Object[variableCount];
            invoke.invokeCurrent(context, parentContext, variableValues);
            return this;
        }

        @Override
        protected EvaluatorContext cloneContext() {
            return new SibbingVariablesImpl(parent);
        }
    }

    final static class TwinsImpl extends EvaluatorContext {

        final ElVariableInvoker parent;

        final ElVariableInvoker one;
        final ElVariableInvoker other;
        Object oneValue;
        Object otherValue;

        TwinsImpl(ElVariableInvoker parent, ElVariableInvoker one, ElVariableInvoker other) {
            this.parent = parent;
            this.one = one;
            this.other = other;
        }

        @Override
        public Object getContextValue(ElVariableInvoker variableInvoker) {
            return variableInvoker == one ? oneValue : otherValue;
        }

        @Override
        public TwinsImpl invokeVariables(ElInvoker invoke, Object context, int variableCount) {
            Object parentContext = parent.invokeDirect(context);
            oneValue = one.invokeCurrent(context, parentContext);
            otherValue = other.invokeCurrent(context, parentContext);
            return this;
        }

        @Override
        public TwinsImpl invokeVariables(ElInvoker invoke, Map context, int variableCount) {
            Object parentContext = parent.invokeDirect(context);
            oneValue = one.invokeCurrent(context, parentContext);
            otherValue = other.invokeCurrent(context, parentContext);
            return this;
        }

        @Override
        protected EvaluatorContext cloneContext() {
            return new TwinsImpl(parent, one, other);
        }
    }

    final static class ParametersImpl extends EvaluatorContext {
        public ParametersImpl(Object[] params) {
            this.variableValues = params;
        }

        @Override
        public Object getContextValue(ElVariableInvoker variableInvoker) {
            try {
                return variableValues[variableInvoker.tailIndex];
            } catch (Throwable throwable) {
                throw new ExpressionException("unresolved property or variable: '" + variableInvoker + "' from parameters[" + variableInvoker.tailIndex + "]");
            }
        }
    }
}
