package io.github.wycst.wast.json;

import java.util.function.Function;
import java.util.function.ToIntFunction;

public class JSONTypeRegistorUtils {

    /**
     * 对象调用toString序列化器
     */
    public final static JSONTypeSerializer TO_STRING = JSONTypeSerializer.TO_STRING;

    /**
     * 构建一个从已经解析完成的字符串转化为指定类型的反序列化器
     */
    public static <E> JSONTypeDeserializer fromString(final Function<String, E> function) {
        return new JSONTypeDeserializer.FromStringImpl() {
            @Override
            public Object of(String value) throws Exception {
                return function.apply(value);
            }
        };
    }

    /**
     * 构建一个从已经解析完成的Integer转化为指定类型的反序列化器
     */
    public static <E> JSONTypeDeserializer fromInteger(final Function<Integer, E> function) {
        return new JSONTypeDeserializer.FromIntegerImpl() {
            @Override
            public Object of(Integer value) throws Exception {
                return function.apply(value);
            }
        };
    }

    /**
     * 构建一个从已经解析完成的Integer转化为指定类型的反序列化器
     */
    public static <E> JSONTypeSerializer toInt(final ToIntFunction<E> function) {
        return new JSONTypeSerializer.ToIntegerImpl<E>() {
            @Override
            public int intValue(E target) throws Exception {
                return function.applyAsInt(target);
            }
        };
    }

}
