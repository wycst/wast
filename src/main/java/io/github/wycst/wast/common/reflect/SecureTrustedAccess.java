package io.github.wycst.wast.common.reflect;

import java.util.Arrays;
import java.util.List;

public abstract class SecureTrustedAccess {

    private final String implTrustedAccessName;
    final static List<String> TRUSTED_ACCESS_NAME_LIST = Arrays.asList(
            "io.github.wycst.wast.common.utils.UtilsSecureTrustedAccess",
            "io.github.wycst.wast.common.expression.ElSecureTrustedAccess",
            "io.github.wycst.wast.json.JSONSecureTrustedAccess"
    );

    protected SecureTrustedAccess() {
        String implTrustedAccessName = this.getClass().getName();
        if (!TRUSTED_ACCESS_NAME_LIST.contains(implTrustedAccessName)) {
            throw new UnsupportedOperationException();
        }
        this.implTrustedAccessName = implTrustedAccessName;
    }

    public final void set(SetterInfo setterInfo, Object target, Object value) {
        setterInfo.invokeInternal(target, value);
    }

    public final Object get(GetterInfo getterInfo, Object target) {
        return getterInfo.invokeInternal(target);
    }

    public final Object getObjectValue(Object target, long fieldOffset) {
        return UnsafeHelper.UNSAFE.getObject(target, fieldOffset);
    }

    public final Object getPrimitiveValue(ReflectConsts.PrimitiveType primitiveType, Object target, long fieldOffset) {
        return primitiveType.getValue(target, fieldOffset);
    }

    public final Object getSetterDefault(SetterInfo setterInfo, Object target) {
        return setterInfo.getDefaultFieldValue(target);
    }


}
