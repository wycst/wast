package io.github.wycst.wast.common.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 变量模型处理工具
 *
 * @Author: wangy
 * @Date: 2022/10/30 0:37
 * @Description:
 */
public final class ElVariableUtils {

    /**
     * 根据链式表达式构建变量执行模型
     *
     * @param elKey
     * @return
     */
    public static ElVariableInvoker build(String elKey) {
        return build(elKey, new HashMap<String, ElVariableInvoker>(), new HashMap<String, ElVariableInvoker>());
    }

    /**
     * 根据链式表达式构建变量执行模型
     *
     * @param elKey
     * @param variableNodeInvokes
     * @param tailNodeInvokes
     * @return
     */
    public static ElVariableInvoker build(String elKey, Map<String, ElVariableInvoker> variableNodeInvokes, Map<String, ElVariableInvoker> tailNodeInvokes) {
        List<String> variableKeys = new ArrayList<String>();
        int beginIndex = 0, splitIndex = elKey.indexOf('.');
        while (splitIndex > -1) {
            variableKeys.add(new String(elKey.substring(beginIndex, splitIndex)));
            beginIndex = ++splitIndex;
            splitIndex = elKey.indexOf('.', splitIndex);
        }
        variableKeys.add(new String(elKey.substring(beginIndex)));
        return build(variableKeys, variableNodeInvokes, tailNodeInvokes);
    }

    /**
     * 根据集合构建变量执行模型
     *
     * @param keys
     * @param variableInvokes
     * @param tailNodeInvokes
     * @return last value invoke
     */
    public static ElVariableInvoker build(List<String> keys, Map<String, ElVariableInvoker> variableInvokes, Map<String, ElVariableInvoker> tailNodeInvokes) {
        ElVariableInvoker prev = null;
        String path = null;
        int index = 0;
        for (String key : keys) {
            boolean isRoot = index++ == 0;
            path = isRoot ? key : path + '.' + key;
            boolean isChildEL = key.charAt(0) == '(';
            ElVariableInvoker variableInvoke = variableInvokes.get(path);
            if (variableInvoke == null) {
                if (isRoot) {
                    variableInvoke = isChildEL ? new ElVariableInvoker.ChildElImpl(new String(key.substring(1, key.length() - 1))) : new ElVariableInvoker.RootImpl(key.intern());
                } else {
                    variableInvoke = isChildEL ? new ElVariableInvoker.ChildElImpl(new String(key.substring(1, key.length() - 1)), prev) : new ElVariableInvoker(key, prev);
                }
                variableInvokes.put(path, variableInvoke.index(variableInvokes.size()));
            }
            prev = variableInvoke;
        }

        ElVariableInvoker result = prev;
        if (!tailNodeInvokes.containsKey(path)) {
            tailNodeInvokes.put(path, result.tailIndex(tailNodeInvokes.size()));
        }
        result.setTail(true);
        return result;
    }

    /**
     * 构建一级变量执行模型
     *
     * @param key
     * @param variableInvokes
     * @param tailNodeInvokes
     * @return
     */
    public static ElVariableInvoker buildRoot(String key, Map<String, ElVariableInvoker> variableInvokes, Map<String, ElVariableInvoker> tailNodeInvokes) {
        boolean isChildEL = key.charAt(0) == '(';
        ElVariableInvoker variableInvoke = variableInvokes.get(key);
        if (variableInvoke == null) {
            variableInvoke = isChildEL ? new ElVariableInvoker.ChildElImpl(new String(key.substring(1, key.length() - 1))) : new ElVariableInvoker.RootImpl(key);
            variableInvokes.put(key, variableInvoke.index(variableInvokes.size()));
            tailNodeInvokes.put(key, variableInvoke.tailIndex(tailNodeInvokes.size()));
        }
        variableInvoke.setTail(true);
        return variableInvoke;
    }
}
