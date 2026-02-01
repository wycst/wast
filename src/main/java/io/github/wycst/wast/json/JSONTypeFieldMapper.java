package io.github.wycst.wast.json;

/**
 * <p> 主要用途于@JsonProperty里面的mapper()</p>
 * <p> 自定义字段类型映射器，继承此抽象类，并实现readOf和writeAs方法，即可完成字段类型映射；
 * <P> 定义在属性上的注解，只对当前注解属性字段生效不会影响全局的类型使用；</p>
 * <p> 也可以用于JSON(或JSONInstance)针对指定类型进行全局注册（singleton()强制true）</p>
 *
 * @Author: wangy
 * @Description:
 * @Date: 2026/1/23 16:05
 **/
public abstract class JSONTypeFieldMapper<E> implements JSONTypeMapper<E> {

    /**
     * 是否序列化
     */
    protected boolean serialize() {
        return true;
    }

    /**
     * 是否反序列化
     */
    protected boolean deserialize() {
        return true;
    }

    /**
     * 是否单例(缓存共享一份)
     */
    protected boolean singleton() {
        return true;
    }
}
