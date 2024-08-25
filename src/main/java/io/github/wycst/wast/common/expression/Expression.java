/*
 * Copyright [2020-2024] [wangyunchao]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.github.wycst.wast.common.expression;

import io.github.wycst.wast.common.expression.compile.CompilerEnvironment;
import io.github.wycst.wast.common.expression.compile.CompilerExpression;
import io.github.wycst.wast.common.expression.functions.BuiltInFunction;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 表达式模块
 * <p>
 * 1，常量表达式：
 * Expression.eval("1 + 2");
 * 输出：3
 * <p>
 * Expression.eval("1 + 2 - 3 * 4");
 * 输出： -9
 * <p>
 * Expression.eval("1 + (2 - 3) * 4");
 * 输出： -3
 * <p>
 * 2 变量用法
 * Fact fact = new Fact()；
 * fact.setSize(12);
 * Expression.eval("1 + size * 2", fact);
 * 输出： 25
 * <p>
 * Map map = new HashMap()
 * map.put("a", 1);
 * map.put("b", 2);
 * map.put("c", 3);
 * map.put("msg", "hello");
 * Expression.eval("a + b + c + d", fact);
 * 输出： 6hello
 * <p>
 * 3 函数用法
 * <p>
 * - 通过静态类注册：
 * EvaluateEnvironment evaluateEnvironment = EvaluateEnvironment.create(context);
 * evaluateEnvironment.registerStaticMethods(Math.class, String.class);
 *
 * <p> 调用函数使用@{类名大写}.{方法名称}， 类名大写可以看做为命名空间
 * Expression.eval("@Math.max(1,3)", evaluateEnvironment);
 * 输出：3
 * <p>
 * 也可以global模式注册函数，此时调用需要省略类名，直接@方法名称
 * evaluateEnvironment.registerStaticMethods(true, Math.class, String.class);
 * Expression.eval("@max(1,3)", evaluateEnvironment);
 * 输出：3
 * <p>
 * 注：如果以global模式注册函数，如果有同名函数会被覆盖，即只有一个会生效
 * <p>
 * - 自定义函数名称实现
 * <pre>
 *        evaluateEnvironment.registerFunction("MAX", new ExprFunction<Object, Number>() {
 *            @Override
 *           public Number call(Object... params) {
 *                Arrays.sort(params);
 *                return (Number) params[params.length - 1];
 *            }
 *        });
 *
 *       以上定义了一个名称为'MAX'的函数；
 *       Expression.eval("@MAX(1,3)", evaluateEnvironment);
 *
 *  </pre>
 * 输出：3
 * <p>
 * 创建ExprFunction的子类，然后注册即可，自定义的函数名称统一以global模式生效
 *
 * @Author: wangyunchao
 * @Date: 2021/9/25 22:13
 *
 * <p>常用api:</p>
 * @see Expression#parse(String)
 * @see Expression#eval(String)
 * @see Expression#eval(String, Object)
 * @see Expression#eval(String[], Object)
 * @see Expression#eval(String, EvaluateEnvironment)
 * @see Expression#evalResult(String, Class)
 * @see Expression#evalResult(String, Object, Class)
 * @see Expression#evalResult(String[], Object, Class)
 * @see Expression#evaluate(Object)
 * @see Expression#evaluate(EvaluateEnvironment)
 * @see Expression#evaluateResult(Class)
 * @see Expression#evaluateResult(Object, Class)
 * @see Expression#renderTemplate(String, Object)
 * @see Expression#renderTemplate(String, String, String, Object)
 * <p>
 * 内置函数：
 * @see BuiltInFunction#max(Object...)
 * @see BuiltInFunction#min(Object...)
 * @see BuiltInFunction#avg(Number...)
 * @see BuiltInFunction#sum(Number...)
 * @see BuiltInFunction#abs(Number)
 * @see BuiltInFunction#lower(String)
 * @see BuiltInFunction#upper(String)
 * @see BuiltInFunction#ifNull(Object, Object)
 * @see BuiltInFunction#size(Object)
 */
public abstract class Expression {

    /***
     * 解析表达式 - 字符串解析实现
     *
     * @param expr
     * @return
     */
    public static Expression parse(String expr) {
        return new ExprParser(expr);
    }

    /***
     * 编译表达式
     *
     * @param expr
     * @param environment 编译环境
     * @return
     */
    public static Expression compile(String expr, CompilerEnvironment environment) {
        return CompilerExpression.compile(expr, environment);
    }

    /***
     * 编译表达式
     *
     * @param expr
     * @param environment 编译环境
     * @param coder       Native/Javassist
     * @return
     */
    public static Expression compile(String expr, CompilerEnvironment environment, CompilerExpression.Coder coder) {
        return CompilerExpression.compile(expr, environment, coder);
    }

    /***
     * 执行表达式
     *
     * @param expr
     * @param evaluateEnvironment 执行环境
     * @return
     */
    public static Object eval(String expr, EvaluateEnvironment evaluateEnvironment) {
        return parse(expr).evaluate(evaluateEnvironment);
    }

    /***
     * 执行表达式
     *
     * @param expr
     * @param evaluateEnvironment 执行环境
     * @return
     */
    public static <T> T evalResult(String expr, EvaluateEnvironment evaluateEnvironment, Class<T> targetClass) {
        return toResult(parse(expr).evaluate(evaluateEnvironment), targetClass);
    }

    /**
     * 执行静态表达式
     *
     * @param expr
     * @return
     */
    public static Object eval(String expr) {
        return parse(expr).evaluate();
    }

    /**
     * 执行静态表达式
     *
     * @param expr
     * @param targetClass 返回对象类型
     * @return
     */
    public static <T> T evalResult(String expr, Class<T> targetClass) {
        return parse(expr).evaluateResult(targetClass);
    }

    /**
     * 执行带上下文表达式
     *
     * @param expr
     * @param context
     * @return
     */
    public static Object eval(String expr, Object context) {
        return parse(expr).evaluate(context);
    }

    /**
     * 执行带上下文表达式
     *
     * @param expr
     * @param context
     * @return
     */
    public static Object eval(String expr, Map context) {
        return parse(expr).evaluate(context);
    }

    /**
     * 批量执行带上下文表达式
     *
     * @param exprs
     * @param context
     * @return
     */
    public static Object[] eval(String[] exprs, Object context) {
        if (exprs == null) {
            return null;
        }
        Object[] objects = new Object[exprs.length];
        int i = 0;
        for (String expr : exprs) {
            objects[i++] = parse(expr).evaluate(context);
        }
        return objects;
    }

    /**
     * 批量执行带上下文表达式
     *
     * @param exprs
     * @param context
     * @param targetClass
     * @return
     */
    public static <T> List<T> evalResult(String[] exprs, Object context, Class<T> targetClass) {
        if (exprs == null) {
            return null;
        }
        List<T> objects = new ArrayList<T>(exprs.length);
        int i = 0;
        for (String expr : exprs) {
            objects.add(parse(expr).evaluateResult(context, targetClass));
        }
        return objects;
    }

    /**
     * 执行带上下文表达式
     *
     * @param expr
     * @param context
     * @param targetClass 返回对象类型
     * @return
     */
    public static <T> T evalResult(String expr, Object context, Class<T> targetClass) {
        return parse(expr).evaluateResult(context, targetClass);
    }

    /**
     * 执行常量运算表达式
     *
     * @return
     */
    public abstract Object evaluate();

    /**
     * 执行变量表达式
     *
     * @param context 显示指定map作为参数上下文
     * @return
     */
    public abstract Object evaluate(Map context);

    /**
     * 执行变量表达式
     *
     * @param context 实体对象或者map
     * @return
     */
    public abstract Object evaluate(Object context);

    /**
     * 执行变量表达式
     *
     * @param evaluateEnvironment 执行环境
     * @return
     */
    public abstract Object evaluate(EvaluateEnvironment evaluateEnvironment);

    /**
     * 执行变量表达式
     *
     * @param context             参数上下文
     * @param evaluateEnvironment 执行环境
     * @return
     */
    public abstract Object evaluate(Map context, EvaluateEnvironment evaluateEnvironment);

    /**
     * 执行变量表达式
     *
     * @param context             参数上下文
     * @param evaluateEnvironment 执行环境
     * @return
     */
    public abstract Object evaluate(Object context, EvaluateEnvironment evaluateEnvironment);

    /**
     * 执行常量运算表达式
     *
     * @param targetClass 目标类型
     * @return
     */
    public final <T> T evaluateResult(Class<T> targetClass) {
        return toResult(evaluate(), targetClass);
    }

    /**
     * 执行常量运算表达式
     *
     * @param context
     * @param targetClass
     * @param <T>
     * @return
     */
    public final <T> T evaluateResult(Object context, Class<T> targetClass) {
        return toResult(evaluate(context), targetClass);
    }

    protected final static <T> T toResult(Object result, Class<T> targetClass) {
        if (result == null) {
            return null;
        }
        // force
        if (targetClass.isInstance(result)) {
            return (T) result;
        }
        return ObjectUtils.toType(result, targetClass);
    }

    /**
     * 提供静态方法直接渲染模板（no编译）
     *
     * @param template
     * @param context
     * @return
     */
    public static String renderTemplate(String template, Object context) {
        return renderTemplate(template, "${", "}", context);
    }

    /**
     * 提供静态方法直接渲染模板（no编译）
     * 不支持嵌套（表达式通过括号可以代替嵌套）
     *
     * @param template
     * @param prefix   模板前缀
     * @param suffix   模板后缀
     * @param context  上下文
     * @return
     */
    public static String renderTemplate(String template, String prefix, String suffix, Object context) {
        if (template == null || prefix == null || suffix == null) return null;

        int prefixLen = prefix.length();
        int suffixLen = suffix.length();
        if (prefixLen == 0 || suffixLen == 0) return template;

        StringBuilder builder = new StringBuilder();
        char[] buffers = getChars(template);
        int length = buffers.length;
        int fromIndex = 0;

        // 先找模板后缀，再往前找模板前缀
        // 后缀位置
        int suffixIndex = template.indexOf(suffix);

        // 前缀
        int prefixIndex = -1;
        while (suffixIndex > 0) {
            prefixIndex = template.lastIndexOf(prefix, suffixIndex - 1);
            if (prefixIndex > fromIndex - 1) {
                builder.append(buffers, fromIndex, prefixIndex - fromIndex);
                ExprParser exprParser = new ExprParser(buffers, prefixIndex + prefixLen, suffixIndex - prefixIndex - prefixLen);
                builder.append(exprParser.evaluate(context));

                // continue find next suffix
                fromIndex = suffixIndex + suffixLen;
                suffixIndex = template.indexOf(suffix, fromIndex);
            } else {
                // keep last fromIndex and update suffixIndex
                suffixIndex = template.indexOf(suffix, suffixIndex + suffixLen);
            }
        }

        if (fromIndex < length) {
            builder.append(buffers, fromIndex, length - fromIndex);
        }

        return builder.toString();
    }

    // get chars
    protected final static char[] getChars(String value) {
        return UnsafeHelper.getChars(value);
    }

    public List<String> getVariables() {
        throw new UnsupportedOperationException();
    }
}
