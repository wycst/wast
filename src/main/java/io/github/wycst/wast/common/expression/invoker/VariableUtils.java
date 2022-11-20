package io.github.wycst.wast.common.expression.invoker;

import java.util.*;

/**
 * 变量模型处理工具
 *
 * @Author: wangy
 * @Date: 2022/10/30 0:37
 * @Description:
 */
public class VariableUtils {

    /**
     * 根据链式表达式构建变量执行模型
     *
     * @param elKey
     * @return
     */
    public static VariableInvoker build(String elKey) {
        return build(elKey, new HashMap<String, VariableInvoker>(), new HashMap<String, VariableInvoker>());
    }

    /**
     * 根据链式表达式构建变量执行模型
     *
     * @param elKey
     * @param variableNodeInvokes
     * @param tailNodeInvokes
     * @return
     */
    public static VariableInvoker build(String elKey, Map<String, VariableInvoker> variableNodeInvokes, Map<String, VariableInvoker> tailNodeInvokes) {
        List<String> variableKeys = new ArrayList<String>();
        int beginIndex = 0, splitIndex = elKey.indexOf('.');
        while (splitIndex > -1) {
            variableKeys.add(elKey.substring(beginIndex, splitIndex));
            beginIndex = ++splitIndex;
            splitIndex = elKey.indexOf('.', splitIndex);
        }
        variableKeys.add(elKey.substring(beginIndex));
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
    public static VariableInvoker build(List<String> keys, Map<String, VariableInvoker> variableInvokes, Map<String, VariableInvoker> tailNodeInvokes) {
        VariableInvoker prev = null;
        String path = null;
        StringBuilder stringBuilder = new StringBuilder();
        int index = 0;
        for (String key : keys) {
            boolean isRoot = index++ == 0;
            boolean isChildEL = key.charAt(0) == '(';
            stringBuilder.append(key);
            path = stringBuilder.toString();
            VariableInvoker variableInvoke = variableInvokes.get(path);
            if (variableInvoke == null) {
                if(isRoot) {
                    variableInvoke = isChildEL ? new VariableInvoker.ChildElVariableInvoke(key.substring(1, key.length() - 1)) : new VariableInvoker.RootVariableInvoke(key.intern());
                } else {
                    variableInvoke = isChildEL ? new VariableInvoker.ChildElVariableInvoke(key.substring(1, key.length() - 1), prev) : new VariableInvoker(key, prev);
                }
                variableInvokes.put(path, variableInvoke);
            }
            prev = variableInvoke;
            stringBuilder.append(".");
        }

        VariableInvoker result = prev;

        // prev is the tailInvoke
        Set<String> variableKeys = tailNodeInvokes.keySet();
        boolean ignoreFlag = false;
        Set<String> removeKeys = new HashSet<String>();
        for (String variableKey : variableKeys) {
            if (variableKey.startsWith(path)) {
                ignoreFlag = true;
                break;
            }
            if (path.startsWith(variableKey)) {
                removeKeys.add(variableKey);
            }
        }
        if (!ignoreFlag) {
            tailNodeInvokes.put(path, result);
            for (String removeKey : removeKeys) {
                tailNodeInvokes.remove(removeKey);
            }
        }

        stringBuilder.setLength(0);

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
    public static VariableInvoker buildRoot(String key, Map<String, VariableInvoker> variableInvokes, Map<String, VariableInvoker> tailNodeInvokes) {
        boolean isChildEL = key.charAt(0) == '(';
        VariableInvoker variableInvoke = variableInvokes.get(key);
        if (variableInvoke == null) {
            variableInvoke = isChildEL ? new VariableInvoker.ChildElVariableInvoke(key.substring(1, key.length() - 1)) : new VariableInvoker.RootVariableInvoke(key);
            variableInvokes.put(key, variableInvoke);
        }
        // add or replace
        tailNodeInvokes.put(key, variableInvoke);

        variableInvoke.setTail(true);
        return variableInvoke;
    }

//    public static void main(String[] args) {
//        Map<String, VariableInvoker> variableNodeInvokes = new HashMap<String, VariableInvoker>();
//        Map<String, VariableInvoker> tailVariableInvokes = new HashMap<String, VariableInvoker>();
//
//        build("arg.a.b.c.d.e.f", variableNodeInvokes, tailVariableInvokes);
//        build("arg.a.b.c", variableNodeInvokes, tailVariableInvokes);
//        build("arg.a.b.c.d", variableNodeInvokes, tailVariableInvokes);
//        build("arg.a.b.c.d.e", variableNodeInvokes, tailVariableInvokes);
//        build("arg.a.b", variableNodeInvokes, tailVariableInvokes);
//        build("arg.a.b.c.f.d", variableNodeInvokes, tailVariableInvokes);
//        build("arg.a.b.c.d", variableNodeInvokes, tailVariableInvokes);
//        build("arg.a.b.c.d.e", variableNodeInvokes, tailVariableInvokes);
//
//        System.out.println(variableNodeInvokes);
//        System.out.println(tailVariableInvokes);
//
//    }

}
