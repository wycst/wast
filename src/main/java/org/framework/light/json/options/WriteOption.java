package org.framework.light.json.options;

/**
 * JSON 序列化配置项
 *
 * @author wangy
 */

public enum WriteOption {

    /**
     * Default: filter empty attributes without formatting and indentation
     */
    Default,

    /**
     * 格式化缩进输出
     * Format indented output
     */
    FormatOut,

    /**
     * 输出全属性
     * Output all attributes
     */
    FullProperty,

    /**
     * 跳过循环引用，防止序列化过程中出现死循环
     * Skip circular references to prevent dead loops during serialization
     */
    SkipCircularReference,

    /**
     * <p> 日期是否格式化
     * 考虑到日期使用的易用性，无论是否开启，没有指定日期格式情况下都将使用'yyyy-MM-dd HH:mm:ss'
     * 如果需要将日期序列为时间戳，请指定WriteDateAsTime
     * <p> Format date
     * In consideration of the ease of use of dates, 'yyyy MM DD HH: mm: Ss' will be used regardless of whether the date format is enabled or not
     * If you need to timestamp a date sequence, specify WriteDateAsTime
     */
    DateFormat,

    /**
     * <p>默认将日期格式化输出，配置此项可以序列化为时间戳
     *
     * <p>By default, the date is formatted for output. If this item is configured, it can be serialized as a timestamp
     */
    WriteDateAsTime,

    /**
     * 默认情况下byte数组会输出为base64字符串，开启配置后将bytes数组输出为16进制字符串
     */
    BytesArrayToHex,

    /**
     * 默认情况下byte数组会输出为base64字符串，开启配置后将bytes数组输出原生字节数组
     */
    BytesArrayToNative,

    /***
     * 是否禁用转义符校验,再确保没有需要转义符的情况下使用，否则序列化结果不会带有\\等标记
     * 换言之如果存在需要转义的信息如"，\n,\r等不要开启此配置
     *
     */
    DisableEscapeValidate,

    /***
     * 是否跳过不存在属性域的getter方法序列化
     */
    SkipGetterOfNoneField,

    /**
     * 序列化后不关闭流，默认自动关闭流，开启后不会调用close
     */
    KeepOpenStream,

    /**
     * 默认情况下map的key统一加双引号输出
     * 开启后将根据实际的key值类型序列化
     */
    AllowUnquotedMapKey,

    /**
     * 使用pojo类的字段进行序列化
     */
    UseFields,

    /**
     * 针对实体bean的驼峰字段名称序列化为下划线模式，eg: userName -> user_name
     */
    CamelCaseToUnderline
}
