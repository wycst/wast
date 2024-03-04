package io.github.wycst.wast.jdbc.transform;

import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.jdbc.annotations.Column;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TypeTransformerFactory {

    private static Map<Class<? extends TypeTransformer>, TypeTransformer> typeTransformerMap = new ConcurrentHashMap<Class<? extends TypeTransformer>, TypeTransformer>();

    public static TypeTransformer getTransformer(Column column, SetterInfo setterInfo) {
        Class<? extends TypeTransformer> transformerClass;
        if (column == null || (transformerClass = column.transformer()) == null) return null;
        TypeTransformer typeTransformer = typeTransformerMap.get(transformerClass);
        if (typeTransformer == null) {
            try {
                typeTransformer = transformerClass.newInstance();
                typeTransformer.setParameterizedType(setterInfo.getGenericParameterizedType());
                typeTransformerMap.put(transformerClass, typeTransformer);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return typeTransformer;
    }
}
