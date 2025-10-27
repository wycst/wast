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
    // use for ExprEvaluatorStackSplitImpl & ExprEvaluatorContextValueHolderImpl
    Object value;

    EvaluatorContext() {
    }

    public EvaluatorContext(Object[] variableValues) {
        this.variableValues = variableValues;
    }

    public Object getContextValue(ElVariableInvoker variableInvoker) {
        try {
            return variableValues[variableInvoker.index];
        } catch (Throwable throwable) {
            throw new ExpressionException("unresolved property or variable: '" + variableInvoker + "' from context");
        }
    }

    static class TwinsImpl extends EvaluatorContext {
        final ElVariableInvoker one;
        Object oneValue;
        Object otherValue;

        TwinsImpl(final ElVariableInvoker one, Object oneValue, Object otherValue) {
            this.one = one;
            this.oneValue = oneValue;
            this.otherValue = otherValue;
        }

        @Override
        public final Object getContextValue(ElVariableInvoker variableInvoker) {
            return variableInvoker == one ? oneValue : otherValue;
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

    //    final static class MapImpl extends EvaluatorContext {
//        private final Map context;
//        MapImpl(Map context) {
//            this.context = context;
//        }
//        public Object getContextValue(ElVariableInvoker variableInvoker) {
//            return variableInvoker.invokeDirect(context);
//        }
//    }
//
//
//    final static class ObjectImpl extends EvaluatorContext {
//        private final Object context;
//
//        ObjectImpl(Object context) {
//            this.context = context;
//        }
//
//        public Object getContextValue(ElVariableInvoker variableInvoker) {
//            return variableInvoker.invokeDirect(context);
//        }
//    }
//
    final static class MapRootImpl extends EvaluatorContext {
        private final Map context;

        MapRootImpl(Map context) {
            this.context = context;
        }

        public Object getContextValue(ElVariableInvoker variableInvoker) {
            return context.get(variableInvoker.key);
        }
    }

    final static class ObjectRootImpl extends EvaluatorContext {
        private final Object context;

        ObjectRootImpl(Object context) {
            this.context = context;
        }

        public Object getContextValue(ElVariableInvoker variableInvoker) {
            return variableInvoker.invokeValue(context);
        }
    }

    final static class SingleVariableImpl extends EvaluatorContext {
        SingleVariableImpl(Object variableValue) {
            this.variableValue = variableValue;
        }

        final Object variableValue;

        @Override
        public Object getContextValue(ElVariableInvoker variableInvoker) {
            return variableValue;
        }
    }
}
