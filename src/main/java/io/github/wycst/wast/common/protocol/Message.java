package io.github.wycst.wast.common.protocol;

import java.io.Serializable;

/**
 * 字节报文信息
 *
 * @Author: wangy
 * @Date: 2022/9/4 18:56
 * @Description:
 */
public class Message implements Serializable {

    /**
     * 报文长度（4字节）
     */
    private int length;

    /**
     * 内容类型（1字节）
     * <p>
     * <p> 基本类型直接读取长度：
     * <p> 1  byte               1字节；
     * <p> 2  boolean            1字节；
     * <p> 3  char               2字节；
     * <p> 4  short              2字节；
     * <p> 5  int                4字节；
     * <p> 6  float              4字节；
     * <p> 7  long               8字节；
     * <p> 8  double             8字节；
     * <p> 基本类型包装类序列化时多写入一个字节： -1（null）代表直接为null或者（0）代表按基本类型长度读取
     * <p> 9  Byte               1字节；
     * <p> 10 Boolean            1字节；
     * <p> 11 Character          2字节；
     * <p> 12 Short              2字节；
     * <p> 13 Integer            4字节；
     * <p> 14 Float              4字节；
     * <p> 15 Long               8字节；
     * <p> 16 Double             8字节；
     * <p> 17 String取UTF-8编码实际字节数，如果类型是字符串序列化时需要写入长度(占4个字节)，反序列化时先读取长度（4个字节），然后读取内容；如果长度为-1代表为null
     * <p> 18 StringBuffer 和 String类似；
     * <p> 19 StringBuilder和 String类似；
     * <p> 日期类序列化时多写入一个字节： -1（null）代表直接为null或者（0）代表按8字节长度读取
     * <p> 20 Date               8字节(时间戳)；
     * <p> 21 LocalDate          8字节(时间戳)；
     * <p> 21 LocalDateTime      8字节(时间戳)；
     *
     * @see java.io.Serializable
     */
    private byte type;

    /**
     * 报文内容（长度 = ${length} - 5）
     */
    private byte[] data;


}
