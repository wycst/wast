package io.github.wycst.wast.common.expression.invoker;

import java.util.List;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2022/10/30 11:24
 * @Description:
 */
public interface Invoker {

    void reset();

    Object invoke(Object context);

    Object invoke(Map context);

    /**
     * 直接invoke不缓存
     *
     * @param context
     * @return
     */
    Object invokeDirect(Object context);

    /**
     * 直接invoke不缓存
     *
     * @param context
     * @return
     */
    Object invokeDirect(Map context);

    Object invoke(Object entityContext, Object[] variableValues);

    Object invoke(Map mapContext, Object[] variableValues);

    void internKey();

    List<VariableInvoker> tailInvokers();

    int size();
}
