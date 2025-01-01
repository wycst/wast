package io.github.wycst.wast.common.expression;

import java.util.Map;

/**
 * @Date 2024/12/4 8:36
 * @Created by wangyc
 */
abstract class EvaluatorContextBuilder {

    final ElInvoker invoke;
    final int variableCount;

    EvaluatorContextBuilder() {
        this(null, 0);
    }

    EvaluatorContextBuilder(ElInvoker invoke, int variableCount) {
        this.invoke = invoke;
        this.variableCount = variableCount;
    }

    public abstract EvaluatorContext createEvaluatorContext(Object context);

    public abstract EvaluatorContext createEvaluatorContext(Map context);

    final static EvaluatorContextBuilder EMPTY = new EvaluatorContextBuilder() {
        @Override
        public EvaluatorContext createEvaluatorContext(Object context) {
            return EvaluatorContext.EMPTY;
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Map context) {
            return EvaluatorContext.EMPTY;
        }
    };

    final static class GeneralReusableImpl extends EvaluatorContextBuilder {

        GeneralReusableImpl(ElInvoker invoke, int variableCount) {
            super(invoke, variableCount);
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Object context) {
            Object[] variableValues = variableCount < 5 ? new Object[5] : new Object[variableCount];
            invoke.invoke(context, variableValues);
            return new EvaluatorContext(variableValues);
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Map context) {
            Object[] variableValues = variableCount < 5 ? new Object[5] : new Object[variableCount];
            invoke.invoke(context, variableValues);
            return new EvaluatorContext(variableValues);
        }
    }

    final static class SibbingReusableImpl extends EvaluatorContextBuilder {
        final ElVariableInvoker parent;

        SibbingReusableImpl(ElVariableInvoker parent, ElInvoker invoke, int variableCount) {
            super(invoke, variableCount);
            this.parent = parent;
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Object context) {
            Object parentContext = parent.invokeDirect(context);
            Object[] variableValues = variableCount < 5 ? new Object[5] : new Object[variableCount];
            invoke.invokeCurrent(context, parentContext, variableValues);
            return new EvaluatorContext(variableValues);
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Map context) {
            Object parentContext = parent.invokeDirect(context);
            Object[] variableValues = variableCount < 5 ? new Object[5] : new Object[variableCount];
            invoke.invokeCurrent(context, parentContext, variableValues);
            return new EvaluatorContext(variableValues);
        }
    }

    static class SibbingTwinsReusableImpl extends EvaluatorContextBuilder {

        final ElVariableInvoker parent;
        final ElVariableInvoker one;
        final ElVariableInvoker other;

        SibbingTwinsReusableImpl(ElVariableInvoker parent, ElVariableInvoker one, ElVariableInvoker other) {
            this.parent = parent;
            this.one = one;
            this.other = other;
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Object context) {
            Object parentContext = parent.invokeDirect(context);
            return new EvaluatorContext.TwinsImpl(one, one.invokeCurrent(context, parentContext), other.invokeCurrent(context, parentContext));
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Map context) {
            Object parentContext = parent.invokeDirect(context);
            return new EvaluatorContext.TwinsImpl(one, one.invokeCurrent(context, parentContext), other.invokeCurrent(context, parentContext));
        }
    }

    final static class SibbingTwinsRootReusableImpl extends SibbingTwinsReusableImpl {
        SibbingTwinsRootReusableImpl(ElVariableInvoker one, ElVariableInvoker other) {
            super(null, one, other);
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Object context) {
            return new EvaluatorContext.TwinsImpl(one, one.invokeValue(context), other.invokeValue(context));
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Map context) {
            return new EvaluatorContext.TwinsImpl(one, context.get(one.key), context.get(other.key));
        }
    }

    final static class SibbingRootReusableImpl extends EvaluatorContextBuilder {
        final ElVariableInvoker[] variableInvokers;

        public SibbingRootReusableImpl(ElVariableInvoker[] variableInvokers) {
            this.variableInvokers = variableInvokers;
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Object context) {
            Object[] variableValues = variableCount < 5 ? new Object[5] : new Object[variableCount];
            for (ElVariableInvoker elVariableInvoker : variableInvokers) {
                variableValues[elVariableInvoker.index] = elVariableInvoker.invokeValue(context);
            }
            return new EvaluatorContext(variableValues);
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Map context) {
            Object[] variableValues = variableCount < 5 ? new Object[5] : new Object[variableCount];
            for (ElVariableInvoker elVariableInvoker : variableInvokers) {
                variableValues[elVariableInvoker.index] = context.get(elVariableInvoker.key);
            }
            return new EvaluatorContext(variableValues);
        }
    }

    // a + b + c + ...
    final static class ReusablelessRootImpl extends EvaluatorContextBuilder {
        @Override
        public EvaluatorContext createEvaluatorContext(Object context) {
            return new EvaluatorContext.ObjectRootImpl(context);
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Map context) {
            return new EvaluatorContext.MapRootImpl(context);
        }
    }

    // a + a
    final static class SingleReusableRootImpl extends EvaluatorContextBuilder {
        final ElVariableInvoker invoke;

        public SingleReusableRootImpl(ElVariableInvoker invoke) {
            this.invoke = invoke;
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Object context) {
            return new EvaluatorContext.SingleVariableImpl(invoke.invokeValue(context));
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Map context) {
            return new EvaluatorContext.SingleVariableImpl(context.get(invoke.key));
        }
    }

    // a.b.c + 1 + 2 + 3
    final static class SingleReusableImpl extends EvaluatorContextBuilder {
        public SingleReusableImpl(ElInvoker invoke) {
            super(invoke, 1);
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Object context) {
            return new EvaluatorContext.SingleVariableImpl(invoke.invokeDirect(context));
        }

        @Override
        public EvaluatorContext createEvaluatorContext(Map context) {
            return new EvaluatorContext.SingleVariableImpl(invoke.invokeDirect(context));
        }

//        @Override
//        public EvaluatorContext createEvaluatorContext(Object context) {
//            return new EvaluatorContext.ObjectImpl(context);
//        }
//        @Override
//        public EvaluatorContext createEvaluatorContext(Map context) {
//            return new EvaluatorContext.MapImpl(context);
//        }
    }
}
