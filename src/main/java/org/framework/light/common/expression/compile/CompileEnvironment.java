package org.framework.light.common.expression.compile;

import org.framework.light.common.expression.EvaluateEnvironment;
import org.framework.light.common.expression.ExprFunction;

import java.util.*;

/**
 * @Author: wangy
 * @Date: 2021/11/20 0:00
 * @Description:
 */
public class CompileEnvironment extends EvaluateEnvironment {

    protected CompileEnvironment() {
    }

    // 跳过解析，此模式下注意：
    // 1 直接使用源代码运行，支持所有java代码；
    // 2 如果没有return标记，会在解析所有固化参数后直接return + 表达式
    // 3 变量信息未知，需要通过types, 不支持a.b.c等变量
    private boolean skipParse;
    // 默认禁用system类调用
    private boolean enableSystem;
    // 禁用安全检查
    private boolean disableSecurityCheck;
    // disable keys
    private Set<String> disableKeys = new HashSet<String>();
    // 变量类型映射
    private Map<String, String> typeClassMap = new HashMap<String, String>();

    protected Map<String, String> getTypeClassMap() {
        return typeClassMap;
    }

    public void setVariableType(Class<?> type, String... vars) {
        for (String var : vars) {
            if (type == System.class) continue;
            if (type == Runtime.class) continue;
            typeClassMap.put(var, type.getName());
        }
    }

    public String getVariableType(String type) {
        if (typeClassMap.containsKey(type)) {
            return typeClassMap.get(type) + ".class";
        }
        return null;
    }

    public boolean isSkipParse() {
        return skipParse;
    }

    public void setSkipParse(boolean skipParse) {
        this.skipParse = skipParse;
    }

    public boolean isEnableSystem() {
        return enableSystem;
    }

    public void setEnableSystem(boolean enableSystem) {
        this.enableSystem = enableSystem;
    }

    public boolean isDisableSecurityCheck() {
        return disableSecurityCheck;
    }

    public void setDisableSecurityCheck(boolean disableSecurityCheck) {
        this.disableSecurityCheck = disableSecurityCheck;
    }

    @Override
    protected Map<String, ExprFunction> getFunctionMap() {
        return super.getFunctionMap();
    }

    @Override
    protected ExprFunction getFunction(String functionName) {
        return super.getFunction(functionName);
    }

    /**
     * 设置黑名单词组关键字调用防注入漏洞
     */
    public void setKeyBlacklist(String... keys) {
        for (String key : keys) {
            disableKeys.add(key);
        }
    }

    public void setKeyBlacklist(Collection keys) {
        if(keys != null) {
            disableKeys.addAll(keys);
        }
    }

    public Set<String> getDisableKeys() {
        return disableKeys;
    }
}
