package io.github.wycst.wast.json;

public final class JSONTypeRegistor {
    private final JSONStore store;

    JSONTypeRegistor(JSONStore store) {
        this.store = store;
    }

    /**
     * 注册类型映射器
     *
     * @param targetClass 目标类
     * @param typeMapper  类型映射器
     * @param <T>
     */
    public <T> void register(Class<T> targetClass, JSONTypeMapper<T> typeMapper) {
        store.register(targetClass, typeMapper, false);
    }

    /**
     * 注册类型映射器
     *
     * @param targetClass      目标类
     * @param typeMapper       类型映射器
     * @param applyAllSubClass 是否应用所有子类
     * @param <T>
     */
    public <T> void register(Class<T> targetClass, JSONTypeMapper<T> typeMapper, boolean applyAllSubClass) {
        store.register(targetClass, typeMapper, applyAllSubClass);
    }

    /**
     * 注册类型反序列化器
     *
     * @param targetClass  目标类
     * @param deserializer 反序列化器
     */
    public void register(Class<?> targetClass, JSONTypeDeserializer deserializer) {
        store.register(deserializer, targetClass);
    }

    /**
     * 注册类型反序列化器
     *
     * @param targetClass      目标类
     * @param deserializer     反序列化器
     * @param applyAllSubClass 是否应用所有子类
     */
    public void register(Class<?> targetClass, JSONTypeDeserializer deserializer, boolean applyAllSubClass) {
        store.register(deserializer, targetClass, applyAllSubClass);
    }

    /**
     * 注册类型反序列化器(内部注册使用)
     *
     * @param targetClass      目标类
     * @param deserializer     反序列化器
     * @param applyAllSubClass 是否应用所有子类
     */
    void registerInternal(Class<?> targetClass, JSONTypeDeserializer deserializer, boolean applyAllSubClass) {
        store.registerInternal(deserializer, targetClass, applyAllSubClass);
    }

    /**
     * 注册类型序列化器
     *
     * @param targetClass 目标类
     * @param serializer  序列化器
     */
    public void register(Class<?> targetClass, JSONTypeSerializer serializer) {
        store.register(serializer, targetClass);
    }

    /**
     * 注册类型序列化器
     *
     * @param targetClass      目标类
     * @param serializer       序列化器
     * @param applyAllSubClass 是否应用所有子类
     */
    public void register(Class<?> targetClass, JSONTypeSerializer serializer, boolean applyAllSubClass) {
        store.register(serializer, targetClass, applyAllSubClass);
    }
}

