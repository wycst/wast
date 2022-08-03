package io.github.wycst.wast.json.options;

/**
 * JSON解析读取配置
 *
 * @Author: wangyunchao
 * @Date: 2021/12/26 0:44
 * @Description:
 */
public enum ReadOption {

    /**
     * 目标类型为byte[]，解析到字符串标记时将按16进制字符串转化为byte[]数组(2个字符转化为一个字节)
     */
    ByteArrayFromHexString,

    /***
     * 是否禁用转义符校验,再确保没有需要转义符的情况下可以使用
     * <p> 不要将此配置项应用到全局配置
     * <p> 小文本下微乎其微，一般场景下可以不用启用， 在大文本字符串解析时可显著提升性能（除非已确保没有需要转义处理的场景）
     * （暂时弃用）
     */
    @Deprecated
    DisableEscapeValidate,

    /***
     * 非标准json特性：允许JSON存在注释，仅仅支持//和/+* *+/，默认关闭注释解析
     */
    AllowComment,

    /**
     * 非标准json特性：允许JSON字段的key没有双引号
     */
    AllowUnquotedFieldNames,

    /**
     * 非标准json特性：允许JSON字段的key使用单引号
     */
    AllowSingleQuotes,

    /**
     * 不存在的枚举类型解析时默认抛出异常，开启后解析为null,
     */
    UnknownEnumAsNull,

    /**
     * <p>
     * 解析实体bean的场景下，如果其属性的类型为普通抽象类或者接口(Map和Collection极其子类接口除外)，如果指定了默认实例将使用默认实例对象
     * 从使用上解决类型映射问题，而不用趟AutoType带来的各种安全漏洞的坑
     */
    UseDefaultFieldInstance,

    /**
     * <p>
     * 当不确定类型时默认根据number的实际类型映射值；
     * 整型数在Integer范围解析为Integer类型，否则解析为Long型；
     * 浮点类型默认解析为Double；
     * 开启后在不确定number类型情况下，统一转化为BigDecimal；
     * <p> BigDecimal在存储结构上比普通number类型多很多字段，即使不调用toString占用内存也高得多，如果对于精度没有要求不建议启用
     */
    UseBigDecimalAsDefaultNumber,

    /**
     * <p>
     * 缺省情况下对于number的解析使用10进制字符累加计算，当double的数字长度超过15位时会存在精度丢失（double）；
     * <p> 当java类型确定为double类型时可以启用此配置，程序将使用jdk内置的Double.parseDouble来解析；
     * <p> 注：即使使用Double.parseDouble也无法保证100%精度完整；
     */
    UseNativeDoubleParser,

    /**
     * <p> 对于map的解析默认缓存了key值
     * <p> 通常都能提升解析性能，不排除发生桶索引（index）大量碰撞的场景，会导致链表过长反而影响性能
     * <p> 设置此项配置可禁用cache key
     *
     * @see io.github.wycst.wast.json.util.FixedNameValueMap
     * @see Options#getCacheKey(char[], int, int, int)
     */
    DisableCacheMapKey
}
