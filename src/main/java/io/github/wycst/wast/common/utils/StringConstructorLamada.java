package io.github.wycst.wast.common.utils;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.BiFunction;

public class StringConstructorLamada extends StringConstructor {


    // JDK8
    static BiFunction<char[], Boolean, String> CHARS_SHARE_BI_FUNCTION;

    // JDK9+
    static BiFunction<byte[], Byte, String> BYTES_CODER_BI_FUNCTION;

    static final byte LATIN1 = 0;
    static final byte UTF16 = 1;

    final static MethodHandles.Lookup LOOKUP;

    static {
        MethodHandles.Lookup lookup = null;
        try {
            Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            UnsafeHelper.setAccessible(field);
            lookup = (MethodHandles.Lookup) field.get(null);
        } catch (Throwable throwable) {
            try {
                Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Class.class, int.class);
                UnsafeHelper.setAccessible(constructor);
                lookup = constructor.newInstance(String.class, null, -1);
            } catch (Exception e) {
            }
        }
        LOOKUP = lookup == null ? MethodHandles.lookup() : lookup;

        MethodHandles.Lookup caller = LOOKUP.in(String.class);
        if (!EnvUtils.JDK_9_ABOVE) {
            try {
                // new String(char[] buf, boolean share)
                MethodHandle handle = caller.unreflectConstructor(String.class.getDeclaredConstructor(char[].class, boolean.class));
                MethodType methodType = handle.type();
                CallSite callSite = LambdaMetafactory.metafactory(
                        caller,
                        "apply",
                        MethodType.methodType(BiFunction.class),
                        methodType.generic(),
                        handle,
                        methodType);
                CHARS_SHARE_BI_FUNCTION = (BiFunction) callSite.getTarget().invokeExact();
                BYTES_CODER_BI_FUNCTION = null;
            } catch (Throwable throwable) {
                throw new UnsupportedOperationException();
            }
        } else {
            throw new UnsupportedOperationException();
            // new String(byte[] buf, byte coder);
//            try {
//                Constructor<String> constructor = String.class.getDeclaredConstructor(byte[].class, byte.class);
//                UnsafeHelper.setAccessible(constructor);
//                MethodHandle handle = caller.unreflectConstructor(constructor);
//                CallSite callSite = LambdaMetafactory.metafactory(
//                        lookup,
//                        "apply",
//                        methodType(BiFunction.class),
//                        MethodType.methodType(Object.class, Object.class, Object.class),
//                        handle,
//                        MethodType.methodType(String.class, byte[].class, Byte.class)
//                );
//                BYTES_CODER_BI_FUNCTION = (BiFunction<byte[], Byte, String>) callSite.getTarget().invoke();
//            } catch (Throwable throwable) {
//                throw new UnsupportedOperationException(throwable);
//            }
        }
    }

    @Override
    public String create(char[] buf, int offset, int len) {
        return CHARS_SHARE_BI_FUNCTION.apply(MemoryCopyUtils.copyOfRange(buf, offset, len), true);
    }

    public String create(char[] buf) {
        return CHARS_SHARE_BI_FUNCTION.apply(buf, true);
    }

//    @Override
//    public String createAscii(byte[] buf, int offset, int len) {
//        return BYTES_CODER_BI_FUNCTION.apply(MemoryCopyUtils.copyOfRange(buf, offset, len), LATIN1);
//    }
//
//    public String createAscii(byte[] bytes) {
//        return BYTES_CODER_BI_FUNCTION.apply(bytes, LATIN1);
//    }
//
//    @Override
//    public String createUTF16(byte[] buf, int offset, int len) {
//        return BYTES_CODER_BI_FUNCTION.apply(MemoryCopyUtils.copyOfRange(buf, offset, len), UTF16);
//    }
//
//    public String createUTF16(byte[] bytes) {
//        return BYTES_CODER_BI_FUNCTION.apply(bytes, UTF16);
//    }
}
