package io.github.wycst.wast.common.utils;

import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.SetterInfo;

/**
 * @Date 2024/7/30 19:39
 * @Created by wangyc
 */
abstract class InvokeUtils {
    final static UtilsSecureTrustedAccess TRUSTED_ACCESS = new UtilsSecureTrustedAccess();

    final static void invokeSet(SetterInfo setterInfo, Object obj, Object value) {
        TRUSTED_ACCESS.set(setterInfo, obj, value);
    }

    final static Object invokeGet(GetterInfo getterInfo, Object obj) {
        return TRUSTED_ACCESS.get(getterInfo, obj);
    }
}
