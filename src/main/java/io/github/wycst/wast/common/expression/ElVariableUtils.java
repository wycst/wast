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
public class ElVariableUtils {

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
        StringBuilder stringBuilder = new StringBuilder();
        int index = 0;
        for (String key : keys) {
            boolean isRoot = index++ == 0;
            boolean isChildEL = key.charAt(0) == '(';
            stringBuilder.append(key);
            path = stringBuilder.toString();
            ElVariableInvoker variableInvoke = variableInvokes.get(path);
            if (variableInvoke == null) {
                if(isRoot) {
                    variableInvoke = isChildEL ? new ElVariableInvoker.ChildElVariableInvoke(new String(key.substring(1, key.length() - 1))) : new ElVariableInvoker.RootVariableInvoke(key.intern());
                } else {
                    variableInvoke = isChildEL ? new ElVariableInvoker.ChildElVariableInvoke(new String(key.substring(1, key.length() - 1)), prev) : new ElVariableInvoker(key, prev);
                }
                variableInvokes.put(path, variableInvoke);
                if(prev != null) {
                    prev.hasChildren = true;
                }
            }
            prev = variableInvoke;
            stringBuilder.append(".");
        }

        ElVariableInvoker result = prev;

        // prev is the tailInvoke
//        Set<String> variableKeys = tailNodeInvokes.keySet();
//        boolean ignoreFlag = false;
//        Set<String> removeKeys = new HashSet<String>();
//        for (String variableKey : variableKeys) {
//            if (variableKey.startsWith(path)) {
//                ignoreFlag = true;
//                break;
//            }
//            if (path.startsWith(variableKey)) {
//                removeKeys.add(variableKey);
//            }
//        }
//        if (!ignoreFlag) {
//            tailNodeInvokes.put(path, result);
//            for (String removeKey : removeKeys) {
//                tailNodeInvokes.remove(removeKey);
//            }
//        }

        tailNodeInvokes.put(path, result);
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
            variableInvoke = isChildEL ? new ElVariableInvoker.ChildElVariableInvoke(new String(key.substring(1, key.length() - 1))) : new ElVariableInvoker.RootVariableInvoke(key);
            variableInvokes.put(key, variableInvoke);
        }
        // add or replace
        tailNodeInvokes.put(key, variableInvoke);

        variableInvoke.setTail(true);
        return variableInvoke;
    }
}
