package io.github.wycst.wast.json;

import io.github.wycst.wast.common.expression.EvaluateEnvironment;
import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.utils.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 属性级别过滤器(一般针对object类型的节点)
 *
 * @Date 2024/10/10 22:17
 * @Created by wangyc
 */
public abstract class JSONNodePathFilter {

    /**
     * <p> 路径过滤通过的节点可以进行二次过滤（属性级别）；
     * <p> 可以使用node的api或者将node转化为java对象进行下一步过滤操作；
     * <p> 如果要将node转化为java对象可以调用any()
     * <blockquote><pre>
     * node.any()
     * </pre></blockquote>
     *
     * @param node target node
     * @return If passed, return true; otherwise, return false
     */
    public abstract boolean doFilter(JSONNode node);

//    /**
//     * ${key}对应的子节点等于${value}
//     *
//     * @param key
//     * @param value
//     * @return
//     */
//    public final static JSONNodePathFilter eq(String key, Serializable value, boolean notFlag) {
//        return new EQFilterImpl(key, value, notFlag);
//    }
//
//    /**
//     * ${key}对应的子节点为数字节点，且大于${value}
//     *
//     * @param key
//     * @param value
//     * @return
//     */
//    public final static JSONNodePathFilter gt(String key, Number value) {
//        return new GreaterOrLessFilterImpl(key, value, false);
//    }
//
//    /**
//     * ${key}对应的子节点为数字节点，且小于${value}
//     *
//     * @param key
//     * @param value
//     * @return
//     */
//    public final static JSONNodePathFilter lt(String key, Number value) {
//        return new GreaterOrLessFilterImpl(key, value, true);
//    }

    /**
     * <p> 支持表达式过滤，表达式为解析模式（非编译模式）；
     * <p> 表达式执行上下文是当前节点映射的map对象；
     * <p> 表达式结果为false或者0时则视为不通过，其他非零非false均代表通过；
     * <p> 只支持对象节点，数组节点暂时不支持表达式；
     *
     * @param condition
     * @return
     */
    public final static JSONNodePathFilter condition(String condition) {
        return new ExpressionFilterImpl(condition);
    }

    public final static JSONNodePathFilter expression(Expression expression) {
        return new ExpressionFilterImpl(expression);
    }

//    final static class EQFilterImpl extends JSONNodePathFilter {
//        final String key;
//        final Serializable value;
//        final boolean notFlag;
//
//        EQFilterImpl(String key, Serializable value, boolean notFlag) {
//            this.key = key;
//            this.value = value;
//            this.notFlag = notFlag;
//        }
//
//        boolean isEqual(JSONNode node) {
//            if (node.leaf) return false;
//            if (node.isObject()) {
//                JSONNode valueNode = node.getChild(key);
//                if (value == null) {
//                    return valueNode == null || valueNode.isNull();
//                }
//                if (valueNode == null) {
//                    return false;
//                }
//                if (value instanceof Number && valueNode.isNumber()) {
//                    return ((Number) value).doubleValue() == ((Number) valueNode.value).doubleValue();
//                }
//                return value.equals(valueNode.value);
//            }
//            return false;
//        }
//
//        @Override
//        public boolean doFilter(JSONNode node) {
//            boolean result = isEqual(node);
//            return notFlag ^ result;
//        }
//    }
//
//    final static class GreaterOrLessFilterImpl extends JSONNodePathFilter {
//        final String key;
//        final Number value;
//        final boolean less;
//
//        GreaterOrLessFilterImpl(String key, Number value, boolean less) {
//            value.getClass();
//            this.key = key;
//            this.value = value;
//            this.less = less;
//        }
//
//        @Override
//        public boolean doFilter(JSONNode node) {
//            if (node.leaf) return false;
//            JSONNode valueNode = node.getChild(key);
//            if (valueNode == null) {
//                return false;
//            }
//            if (valueNode.isNumber()) {
//                if (less) {
//                    return value.doubleValue() > ((Number) valueNode.value).doubleValue();
//                } else {
//                    return value.doubleValue() < ((Number) valueNode.value).doubleValue();
//                }
//            }
//            return false;
//        }
//    }


    final static EvaluateEnvironment EVALUATE_ENVIRONMENT = EvaluateEnvironment.create();
    final static List<String> CONTEXT_VARS = CollectionUtils.listOf("$", "self", "parent", "this");

    static {
        // 支持空属性
        EVALUATE_ENVIRONMENT.setAllowVariableNull(true);
    }

    /**
     * 表达式实现
     * <p>内置上下文变量: </p>
     * <p>$:      代表根的java对象 </p>
     * <p>self:   代表当前节点的java对象 </p>
     * <p>parent: 代表当前节点的父节点Node模型 </p>
     * <p>this:   代表当前节点的Node模型  </p>
     *
     * @see Expression
     */
    final static class ExpressionFilterImpl extends JSONNodePathFilter {
        final Expression expression;
        final String cacheStr;
        final boolean shouldNewContext;

        ExpressionFilterImpl(String condition) {
            expression = Expression.parse(condition);
            cacheStr = "[" + condition + "]";
            shouldNewContext = checkShouldNewContext();
        }

        public ExpressionFilterImpl(Expression expression) {
            this.expression = expression;
            cacheStr = "[" + expression.getSource() + "]";
            shouldNewContext = checkShouldNewContext();
        }

        private boolean checkShouldNewContext() {
            List<String> vars = expression.getRootVariables();
            for (String var : CONTEXT_VARS) {
                if (vars.contains(var)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean doFilter(JSONNode node) {
            Object any = node.any();
            Object evalResult;
            Map context;
            if (node.isObject()) {
                context = (Map) any;
                if (shouldNewContext) {
                    Map newContext = new HashMap(context);
                    if (!context.containsKey("$")) {
                        newContext.put("$", node.root.any());
                    }
                    if (!context.containsKey("self")) {
                        newContext.put("self", any);
                    }
                    if (!context.containsKey("parent")) {
                        newContext.put("parent", node.parent);
                    }
                    if (!context.containsKey("this")) {
                        newContext.put("this", node);
                    }
                    context = newContext;
                }
            } else {
                context = new HashMap();
                context.put("$", node.root.any());
                context.put("self", any);
                context.put("this", node);
                context.put("parent", node.parent);
            }
            evalResult = expression.evaluate(context, EVALUATE_ENVIRONMENT);
            if (evalResult instanceof Boolean) {
                return (Boolean) evalResult;
            }
            return evalResult != null && !evalResult.equals(0);
        }

        @Override
        public String toString() {
            return cacheStr;
        }
    }
}
