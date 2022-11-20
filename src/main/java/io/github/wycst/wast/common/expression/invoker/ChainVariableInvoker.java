package io.github.wycst.wast.common.expression.invoker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Building chained calls of multiple Invokes instead of cyclic overhead
 * When the number is limited, the performance is better than that of circular calls;
 *
 * @Author: wangy
 * @Date: 2022/10/29 22:51
 * @Description:
 */
public class ChainVariableInvoker implements Invoker {

    final VariableInvoker variableInvoke;
    ChainVariableInvoker next;

    ChainVariableInvoker(VariableInvoker variableInvoke) {
        this.variableInvoke = variableInvoke;
    }

    public Object invoke(Object context) {
        try {
            variableInvoke.invoke(context);
        } catch (RuntimeException runtimeException) {
            throw new IllegalArgumentException(String.format("Unresolved field '%s', reason: %s", variableInvoke.toString(), runtimeException.getMessage()));
        }
        return next.invoke(context);
    }

    public Object invoke(Map context) {
        try {
            variableInvoke.invoke(context);
        } catch (RuntimeException runtimeException) {
            throw new IllegalArgumentException(String.format("Unresolved field '%s', reason: %s", variableInvoke.toString(), runtimeException.getMessage()));
        }
        return next.invoke(context);
    }

    @Override
    public Object invokeDirect(Object context) {
        throw new UnsupportedOperationException("chain invoker is not supported");
    }

    @Override
    public Object invokeDirect(Map context) {
        throw new UnsupportedOperationException("chain invoker is not supported");
    }

    @Override
    public Object invoke(Object entityContext, Object[] variableValues) {
        try {
            variableInvoke.invoke(entityContext, variableValues);
        } catch (RuntimeException runtimeException) {
            throw new IllegalArgumentException(String.format("Unresolved field '%s', reason: %s", variableInvoke.toString(), runtimeException.getMessage()));
        }
        return next.invoke(entityContext, variableValues);
    }

    @Override
    public Object invoke(Map mapContext, Object[] variableValues) {
        try {
            variableInvoke.invoke(mapContext, variableValues);
        } catch (RuntimeException runtimeException) {
            throw new IllegalArgumentException(String.format("Unresolved field '%s', reason: %s", variableInvoke.toString(), runtimeException.getMessage()));
        }
        return next.invoke(mapContext, variableValues);
    }

    public void reset() {
        variableInvoke.reset();
        next.reset();
    }

    public List<VariableInvoker> tailInvokers() {
        List<VariableInvoker> variableInvokers = new ArrayList<VariableInvoker>();
        if (variableInvoke.isTail()) {
            variableInvokers.add(variableInvoke);
        }
        ChainVariableInvoker next = this.next;
        while (next != null) {
            if (next.variableInvoke.isTail()) {
                variableInvokers.add(next.variableInvoke);
            }
            next = next.next;
        }

        return variableInvokers;
    }

    public void internKey() {
        variableInvoke.internKey();
        if (next != null) {
            next.internKey();
        }
    }

    @Override
    public int size() {
        return next == null ? 1 : 1 + next.size();
    }

    /**
     * tail
     */
    static class TailVariableInvoke extends ChainVariableInvoker {

        TailVariableInvoke(VariableInvoker variableInvoke) {
            super(variableInvoke);
        }

        public Object invoke(Object context) {
            return variableInvoke.invoke(context);
        }

        public Object invoke(Map context) {
            return variableInvoke.invoke(context);
        }

        @Override
        public Object invoke(Object entityContext, Object[] variableValues) {
            try {
                return variableInvoke.invoke(entityContext, variableValues);
            } catch (RuntimeException runtimeException) {
                throw new IllegalArgumentException(String.format("Unresolved field '%s', reason: %s", variableInvoke.toString(), runtimeException.getMessage()));
            }
        }

        @Override
        public Object invoke(Map mapContext, Object[] variableValues) {
            try {
                return variableInvoke.invoke(mapContext, variableValues);
            } catch (RuntimeException runtimeException) {
                throw new IllegalArgumentException(String.format("Unresolved field '%s', reason: %s", variableInvoke.toString(), runtimeException.getMessage()));
            }
        }

        @Override
        public void reset() {
            variableInvoke.reset();
        }
    }

    public static Invoker build(Map<String, VariableInvoker> variableInvokes) {
        return build(variableInvokes, false);
    }

    public static Invoker build(Map<String, VariableInvoker> variableInvokes, boolean indexVariable) {
        Collection<VariableInvoker> collection = variableInvokes.values();
        int length = collection.size();
        int index = 0;
        boolean onlyOne = length == 1;
        ChainVariableInvoker head = null, prev = null;
        for (VariableInvoker variableInvoke : collection) {
            if (indexVariable) {
                variableInvoke.setIndex(index);
            }
            if (onlyOne) return variableInvoke;
            boolean tail = ++index == length;
            ChainVariableInvoker node = tail ? new TailVariableInvoke(variableInvoke) : new ChainVariableInvoker(variableInvoke);
            if (head == null) {
                head = node;
            } else {
                prev.next = node;
            }
            prev = node;
        }
        return head;
    }

}
