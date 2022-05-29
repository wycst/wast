package org.framework.light.common.idgenerate.entity;

import java.util.Date;

/**
 * 反解的id信息
 *
 * @Author: wangyunchao
 * @Modify by:
 */
public class IdInfo {

    /**
     * 时间位偏移量
     */
    private long offsetTime;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 创建时间
     */
    private Date generateTime;

    /**
     * 实例
     */
    private long instance;

    /**
     * 序列值
     */
    private long sequence;

    /**
     * 算法位数
     */
    private int bit;

    public long getOffsetTime() {
        return offsetTime;
    }

    public void setOffsetTime(long offsetTime) {
        this.offsetTime = offsetTime;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Date getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(Date generateTime) {
        this.generateTime = generateTime;
    }

    public long getInstance() {
        return instance;
    }

    public void setInstance(long instance) {
        this.instance = instance;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public int getBit() {
        return bit;
    }

    public void setBit(int bit) {
        this.bit = bit;
    }
}
