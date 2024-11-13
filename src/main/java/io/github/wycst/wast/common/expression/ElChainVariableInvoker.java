package io.github.wycst.wast.common.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Building chained calls of multiple Invokes instead of cyclic overhead
 * When the number is limited, the performance is better than that of circular calls;
 *
 * @Author: wangy
 * @Date: 2022/10/29 22:51
 * @Description:
 */
public class ElChainVariableInvoker implements ElInvoker {

    final ElVariableInvoker variableInvoke;
    ElChainVariableInvoker next;

    ElChainVariableInvoker(ElVariableInvoker variableInvoke) {
        this.variableInvoke = variableInvoke;
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

    @Override
    public Object invokeCurrent(Map globalContext, Object parentContext, Object[] variableValues) {
        try {
            variableInvoke.invokeCurrent(globalContext, parentContext, variableValues);
        } catch (RuntimeException runtimeException) {
            throw new IllegalArgumentException(String.format("Unresolved field '%s', reason: %s", variableInvoke.toString(), runtimeException.getMessage()));
        }
        return next.invokeCurrent(globalContext, parentContext, variableValues);
    }

    @Override
    public Object invokeCurrent(Object globalContext, Object parentContext, Object[] variableValues) {
        try {
            variableInvoke.invokeCurrent(globalContext, parentContext, variableValues);
        } catch (RuntimeException runtimeException) {
            throw new IllegalArgumentException(String.format("Unresolved field '%s', reason: %s", variableInvoke.toString(), runtimeException.getMessage()));
        }
        return next.invokeCurrent(globalContext, parentContext, variableValues);
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
    static class TailImpl extends ElChainVariableInvoker {

        TailImpl(ElVariableInvoker variableInvoke) {
            super(variableInvoke);
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
        public Object invokeCurrent(Map globalContext, Object parentContext, Object[] variableValues) {
            try {
                return variableInvoke.invokeCurrent(globalContext, parentContext, variableValues);
            } catch (RuntimeException runtimeException) {
                throw new IllegalArgumentException(String.format("Unresolved field '%s', reason: %s", variableInvoke.toString(), runtimeException.getMessage()));
            }
        }

        @Override
        public Object invokeCurrent(Object globalContext, Object parentContext, Object[] variableValues) {
            try {
                return variableInvoke.invokeCurrent(globalContext, parentContext, variableValues);
            } catch (RuntimeException runtimeException) {
                throw new IllegalArgumentException(String.format("Unresolved field '%s', reason: %s", variableInvoke.toString(), runtimeException.getMessage()));
            }
        }
    }

    public final static ElInvoker build(Map<String, ElVariableInvoker> variableInvokes) {
        return build(variableInvokes, false);
    }

    public final static ElInvoker build(Map<String, ElVariableInvoker> variableInvokes, boolean indexVariable) {
        Collection<ElVariableInvoker> collection = variableInvokes.values();
        int length = collection.size();
        int index = 0;
        boolean onlyOne = length == 1;
        ElChainVariableInvoker head = null, prev = null;
        for (ElVariableInvoker variableInvoke : collection) {
            if (indexVariable) {
                variableInvoke.index(index);
            }
            if (onlyOne) return variableInvoke;
            boolean tail = ++index == length;
            ElChainVariableInvoker node = tail ? new TailImpl(variableInvoke) : new ElChainVariableInvoker(variableInvoke);
            if (head == null) {
                head = node;
            } else {
                prev.next = node;
            }
            prev = node;
        }
        return head;
    }

    public final static ElInvoker buildTailChainInvoker(Map<String, ElVariableInvoker> tailInvokerMap) {
        Collection<ElVariableInvoker> tailInvokerValues = tailInvokerMap.values();
        ArrayList<ElVariableInvoker> leafTailValues = new ArrayList<ElVariableInvoker>();
        for (ElVariableInvoker variableInvoke : tailInvokerValues) {
            if (!variableInvoke.hasChildren) {
                leafTailValues.add(variableInvoke);
            }
        }
        int length = leafTailValues.size();
        if (length == 1) {
            return leafTailValues.get(0);
        }
        int index = 0;
        ElChainVariableInvoker head = null, prev = null;
        for (ElVariableInvoker variableInvoke : leafTailValues) {
            boolean tail = ++index == length;
            ElChainVariableInvoker node = tail ? new TailImpl(variableInvoke) : new ElChainVariableInvoker(variableInvoke);
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
