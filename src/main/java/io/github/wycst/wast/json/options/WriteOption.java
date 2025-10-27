package io.github.wycst.wast.json.options;

/**
 * JSON 序列化配置项
 *
 * @author wangy
 */

public enum WriteOption {

    /**
     * none options
     */
    DefaultNone,

    /**
     * 格式化缩进输出（默认使用制表符号\t）
     * Format indented output
     */
    FormatOut,

    /**
     * 格式化缩进输出对象属性时是否在冒号后面追加一个空格
     */
    FormatOutColonSpace,

    /**
     * 格式化缩进使用TAB模式(缺省配置)
     */
    FormatIndentUseTab,

    /**
     * 格式化缩进使用空格模式(默认每级4个空格)
     */
    FormatIndentUseSpace,

    /**
     * 格式化缩进每级使用8个空格缩进模式
     */
    FormatIndentUseSpace8,

    /**
     * 输出全属性
     * Output all attributes
     */
    FullProperty,

    /**
     * 过滤null字段(缺省配置)
     */
    IgnoreNullProperty,

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
     * <p> 默认按name序列化枚举类，配置此项将按Ordinal值输出，适合枚举类固定场景使用。
     * <p> 注：枚举类选项发生新增或者删除会影响反序列化结果，谨慎使用。
     */
    WriteEnumAsOrdinal,

    /**
     * <p> 序列号枚举时以枚举的name写入。(缺省配置)
     */
    WriteEnumAsName,

    /**
     * <p> 将number按字符串输出
     */
    WriteNumberAsString,

    /**
     * <p> 后续删除 <p>
     */
    @Deprecated
    WriteDecimalUseToString,

    /**
     * 默认情况下byte数组会输出为base64字符串，开启配置后将bytes数组输出为16进制字符串
     */
    BytesArrayToHex,

    /**
     * 默认情况下byte数组会输出为base64字符串，开启配置后将bytes数组输出原生字节数组
     */
    BytesArrayToNative,

    /***
     *
     * <p> 是否忽略转义符校验,再确保没有需要转义符的情况下使用，可大幅度提高性能
     * <p> 如果存在需要转义的信息如"，\n,\r等不要开启此配置
     *
     */
    IgnoreEscapeCheck,

    /***
     * 是否跳过不存在属性域的getter方法序列化
     */
    SkipGetterOfNoneField,

    /**
     * 序列化后不关闭流，默认自动关闭流，开启后不会调用close
     */
    KeepOpenStream,

    /**
     * <p> 默认情况下map的key统一加双引号输出
     * <p> 开启后将根据实际的key值类型序列化
     */
    AllowUnquotedMapKey,

    /**
     * 使用pojo类的字段进行序列化
     */
    UseFields,

    /**
     * 针对实体bean的驼峰字段名称序列化为下划线模式，eg: userName -> user_name
     */
    CamelCaseToUnderline,

    /**
     * 是否在序列化对象时将实体类名(pojo)写入到json字符串（map和list类型无效）
     */
    WriteClassName,
}
