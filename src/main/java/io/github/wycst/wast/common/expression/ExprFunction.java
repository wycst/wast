package io.github.wycst.wast.common.expression;

/**
 * 提供给使用者注册在表达式中可以通过@调用
 *
 * @Author: wangy
 * @Date: 2021/11/20 14:07
 * @Description:
 */
public interface ExprFunction<I,O> {

    /***
     * 函数接口
     *
     * @param params
     * @return
     */
    public O call(I...params);

}
