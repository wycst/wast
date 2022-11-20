package io.github.wycst.wast.common.expression.invoker;

import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.utils.CollectionUtils;
import io.github.wycst.wast.common.utils.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 变量调用值
 *
 * <p> invoke过程：
 * <p> 获取变量上下文：
 * <p> 如果当前为root,则将参数直接作为上下文进行invoke，否则调用parent结果作为上下文；
 * <p> 结果存储到属性value中，一个表达式开始执行到结束每个点只调用一次，表达式执行完成后清除value值；
 *
 * @Author: wangy
 * @Date: 2022/10/28 22:30
 * @Description:
 */
public class VariableInvoker implements Invoker {

    String key;
    ValueInvokeHolder invokeHolder = ValueInvokeHolder.Empty;
    // Context calculated value, each point is calculated only once
    Object value;
    // index pos
    int index;

    // Parent or Up one level
    VariableInvoker parent;
    // is last or not ?
    boolean tail;

    VariableInvoker(String key) {
        this.key = key;
    }

    VariableInvoker(String key, VariableInvoker parent) {
        parent.getClass();
        this.key = key;
        this.parent = parent;
    }

    @Override
    public String toString() {
        return parent.toString() + "." + key;
    }

    public final void reset() {
        this.value = null;
    }

    public final void resetAll() {
        this.reset();
        if (parent != null) {
            parent.resetAll();
        }
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public VariableInvoker getParent() {
        return parent;
    }

    public String getKey() {
        return key;
    }

    /**
     * <p> 获取节点的值,暂时支持pojo对象和map
     * <p>
     * Threads are not safe here
     * Performance and safety are inseparable
     *
     * @param context
     * @return
     */
    public Object invoke(Object context) {
        if (value != null)
            return value;
        Object target = parent.invoke(context);
        return value = invokeValue(target);
    }

    /**
     * <p> 获取节点的值,参数map
     * <p> 线程不安全 Threads are not safe here
     * <p>
     * Performance and safety are inseparable
     *
     * @param context
     * @return
     */
    public Object invoke(Map context) {
        if (value != null)
            return value;
        Object target = parent.invoke(context);
        return value = invokeValue(target);
    }

    @Override
    public Object invokeDirect(Object context) {
        Object target = parent.invokeDirect(context);
        return invokeValue(target);
    }

    @Override
    public Object invokeDirect(Map context) {
        Object target = parent.invokeDirect(context);
        return invokeValue(target);
    }

    @Override
    public Object invoke(Object entityContext, Object[] variableValues) {
        Object value = variableValues[index];
        if (value != null)
            return value;
        Object target = parent.invoke(entityContext, variableValues);
        return variableValues[index] = invokeValue(target);
    }

    @Override
    public Object invoke(Map mapContext, Object[] variableValues) {
        Object value = variableValues[index];
        if (value != null)
            return value;
        Object target = parent.invoke(mapContext, variableValues);
        return variableValues[index] = invokeValue(target);
    }

    // invoke current
    public final Object invokeValue(Object context) {
        Object target;
        try {
            Class<?> contextClass = context.getClass();
            ValueInvokeHolder localHoder = this.invokeHolder;
            if (contextClass == localHoder.targetClass) {
                target = localHoder.valueInvoke.getValue(context);
            } else {
                // create
                ValueInvoke valueInvoke = null;
                if (context instanceof Map) {
                    valueInvoke = new MapValueInvoke(key);
                } else {
                    GetterInfo getterInfo = null;
                    try {
                        getterInfo = ClassStructureWrapper.get(contextClass).getGetterInfo(key);
                        valueInvoke = new ObjectPropertyInvoke(getterInfo);
                    } catch (RuntimeException runtimeException) {
                        if (getterInfo == null) {
                            throw new IllegalArgumentException(String.format("Unresolved field '%s' from %s", key, contextClass.toString()));
                        }
                        throw runtimeException;
                    }
                }
                target = valueInvoke.getValue(context);
                invokeHolder = new ValueInvokeHolder(contextClass, valueInvoke);
            }
            return target;
        } catch (Throwable throwable) {
            if (context == null) {
                throw new IllegalArgumentException(String.format("Unresolved field '%s' for target obj is null or not exist in the context ", key));
            }
            throw new IllegalArgumentException(String.format("Unresolved field '%s' from %s, resion: %s", key, context.getClass(), throwable.getMessage()));
        }
    }

//    /**
//     * <p> 获取上下文的值,暂时支持pojo对象和map
//     * <p> 此方法没有安全问题；
//     *
//     * @param context
//     * @return
//     */
//    final Object invokeValueSafely(Object context) {
//        if (context == null) return null;
//        Object target;
//        if (context instanceof Map) {
//            target = ((Map) context).get(key);
//        } else {
//            Class<?> contextClass = context.getClass();
//            GetterInfo getterInfo = null;
//            try {
//                getterInfo = ClassStructureWrapper.get(contextClass).getGetterInfo(key);
//                target = getterInfo.invoke(context);
//            } catch (RuntimeException throwable) {
//                if (getterInfo == null) {
//                    throw new IllegalArgumentException(String.format("Unresolved field '%s' %s", key, contextClass.toString()));
//                }
//                throw throwable;
//            }
//        }
//        return target;
//    }

    public boolean existKey(Object context) {
        if (context == null) return false;
        Object target = parent == null ? context : parent.invoke(context);
        if (target == null) return false;
        if (context instanceof Map) {
            return ((Map<?, ?>) context).containsKey(key);
        } else {
            Class<?> contextClass = context.getClass();
            try {
                return ClassStructureWrapper.get(contextClass).getGetterInfo(key) != null;
            } catch (RuntimeException throwable) {
                return false;
            }
        }
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }

    public void internKey() {
        this.key = key.intern();
    }

    public boolean isTail() {
        return tail;
    }

    public void setTail(boolean tail) {
        this.tail = tail;
    }

    public boolean isChildEl() {
        return false;
    }

    @Override
    public List<VariableInvoker> tailInvokers() {
        List<VariableInvoker> tailInvokers = new ArrayList<VariableInvoker>();
        tailInvokers.add(this);
        return tailInvokers;
    }

    @Override
    public int size() {
        return 1;
    }

    /**
     * Variable root node
     */
    static class RootVariableInvoke extends VariableInvoker {

        public RootVariableInvoke(String key) {
            super(key);
        }

        @Override
        public Object invoke(Object context) {
            if (value != null)
                return value;
            return value = invokeValue(context);
        }

        @Override
        public Object invoke(Map context) {
            if (value != null)
                return value;
            return value = context.get(key);
        }

        @Override
        public Object invokeDirect(Object context) {
            return invokeValue(context);
        }

        @Override
        public Object invokeDirect(Map context) {
            return context.get(key);
        }

        @Override
        public Object invoke(Object entityContext, Object[] variableValues) {
            Object value = variableValues[index];
            if (value != null)
                return value;
            return variableValues[index] = invokeValue(entityContext);
        }

        @Override
        public Object invoke(Map mapContext, Object[] variableValues) {
            Object value = variableValues[index];
            if (value != null)
                return value;
            return variableValues[index] = mapContext.get(key);
        }

        @Override
        public String toString() {
            return key;
        }
    }

    /**
     * 子表达式变量
     */
    static class ChildElVariableInvoke extends VariableInvoker {

        protected Expression el;

        ChildElVariableInvoke(String key) {
            super(key);
        }

        ChildElVariableInvoke(String key, VariableInvoker parent) {
            super(key, parent);
            // lazy is required
            // this.expression = Expression.parse(key);
        }

        private void parseChildEl() {
            if (el == null) {
                synchronized (this) {
                    if (el == null) {
                        el = Expression.parse(key);
                    }
                }
            }
        }

        @Override
        public Object invoke(Object context) {
            if (value != null)
                return value;
            parseChildEl();
            Object realKey = el.evaluate(context);
            Object target = parent != null ? parent.invoke(context) : context;
            if (realKey instanceof String) {
                return value = ObjectUtils.getAttrValue(target, (String) realKey);
            } else if (realKey instanceof Long) {
                // 数组key
                int indexValue = ((Long) realKey).intValue();
                return value = CollectionUtils.getElement(target, indexValue);
            }
            throw new IllegalArgumentException(String.format("Unresolved child el: '%s', val: %s ", key, String.valueOf(realKey)));
        }

        @Override
        public Object invoke(Map context) {
            if (value != null)
                return value;
            parseChildEl();
            Object realKey = el.evaluate(context);
            Object target = parent != null ? parent.invoke(context) : context;
            if (realKey instanceof String) {
                return value = ObjectUtils.getAttrValue(target, (String) realKey);
            } else if (realKey instanceof Long) {
                // 数组key
                int indexValue = ((Long) realKey).intValue();
                return value = CollectionUtils.getElement(target, indexValue);
            }
            throw new IllegalArgumentException(String.format("Unresolved child el: '%s', val: %s ", key, String.valueOf(realKey)));
        }

        @Override
        public Object invokeDirect(Object context) {
            parseChildEl();
            Object realKey = el.evaluate(context);
            Object target = parent != null ? parent.invokeDirect(context) : context;
            if (realKey instanceof String) {
                return ObjectUtils.getAttrValue(target, (String) realKey);
            } else if (realKey instanceof Long) {
                // 数组key
                int indexValue = ((Long) realKey).intValue();
                return CollectionUtils.getElement(target, indexValue);
            }
            throw new IllegalArgumentException(String.format("Unresolved child el: '%s', val: %s ", key, String.valueOf(realKey)));
        }

        @Override
        public Object invokeDirect(Map context) {
            parseChildEl();
            Object realKey = el.evaluate(context);
            if (parent == null) {
                return context.get(String.valueOf(realKey));
            } else {
                Object target = parent.invokeDirect(context);
                if (realKey instanceof String) {
                    return ObjectUtils.getAttrValue(target, (String) realKey);
                } else if (realKey instanceof Long) {
                    // 数组key
                    int indexValue = ((Long) realKey).intValue();
                    return CollectionUtils.getElement(target, indexValue);
                }
            }
            throw new IllegalArgumentException(String.format("Unresolved child el: '%s', val: %s ", key, String.valueOf(realKey)));
        }

        @Override
        public Object invoke(Object context, Object[] variableValues) {
            Object value = variableValues[index];
            if (value != null)
                return value;
            parseChildEl();
            Object realKey = el.evaluate(context);
            Object target = parent != null ? parent.invoke(context, variableValues) : context;
            if (realKey instanceof String) {
                return variableValues[index] = ObjectUtils.getAttrValue(target, (String) realKey);
            } else if (realKey instanceof Long) {
                // 数组key
                int indexValue = ((Long) realKey).intValue();
                return variableValues[index] = CollectionUtils.getElement(target, indexValue);
            }
            throw new IllegalArgumentException(String.format("Unresolved child el: '%s', val: %s ", key, String.valueOf(realKey)));
        }

        @Override
        public Object invoke(Map context, Object[] variableValues) {
            Object value = variableValues[index];
            if (value != null)
                return value;
            parseChildEl();
            Object realKey = el.evaluate(context);
            Object target = parent != null ? parent.invoke(context, variableValues) : context;
            if (realKey instanceof String) {
                return variableValues[index] = ObjectUtils.getAttrValue(target, (String) realKey);
            } else if (realKey instanceof Long) {
                // 数组key
                int indexValue = ((Long) realKey).intValue();
                return variableValues[index] = CollectionUtils.getElement(target, indexValue);
            }
            throw new IllegalArgumentException(String.format("Unresolved child el: '%s', val: %s ", key, String.valueOf(realKey)));
        }

        public boolean isChildEl() {
            return true;
        }

        @Override
        public String toString() {
            return parent.toString() + "[" + key + "]";
        }
    }

    interface ValueInvoke<T> {
        Object getValue(T context);
    }

    static class MapValueInvoke implements ValueInvoke<Map<String, Object>> {
        private final String key;

        MapValueInvoke(String key) {
            this.key = key;
        }

        @Override
        public Object getValue(Map<String, Object> context) {
            return context.get(key);
        }

        @Override
        public String toString() {
            return "[" + key + "]";
        }
    }

    static class ObjectPropertyInvoke implements ValueInvoke {
        private final GetterInfo getterInfo;

        ObjectPropertyInvoke(GetterInfo getterInfo) {
            getterInfo.getClass();
            this.getterInfo = getterInfo;
        }

        @Override
        public Object getValue(Object context) {
            return getterInfo.invoke(context);
        }
    }

    static class ValueInvokeHolder {

        static final ValueInvokeHolder Empty = new ValueInvokeHolder(null, null);

        final Class targetClass;
        final ValueInvoke valueInvoke;

        public ValueInvokeHolder(Class targetClass, ValueInvoke valueInvoke) {
            this.targetClass = targetClass;
            this.valueInvoke = valueInvoke;
        }
    }

}
