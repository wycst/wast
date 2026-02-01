package io.github.wycst.wast.json;

/**
 * <p>
 * JSON类型模块化注册（集中注册管理）
 * </p>
 *
 * @Created by wangyc
 */
public interface JSONTypeModule {

    void register(JSONTypeRegistor registor);
}
