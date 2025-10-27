package io.github.wycst.wast.common.idgenerate.providers;

import io.github.wycst.wast.common.idgenerate.entity.IdInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * 雪花算法实现 Snowflake
 *
 * @Author: wangyunchao
 * @Modify by:
 */
class SnowflakeIdGeneratorProvider extends IdGenerator {

    /**
     * 长度类型
     */
    private int bitType;

    /**
     * 生成的最大长度（注意是约定值并不是限定值）
     * bitType如果为1，约定位数是53位，时间位32位（最大），存储的秒偏移，能使用136年左右，超过136年后位数就会超过53位
     * bitType如果为0，约定位数是64位，时间位41位（最大42位，去掉一个符号位），存储的耗秒偏移，能使用69.7年
     */
    private int generateLen;

    /**
     * 开始时间戳,以生效启用后不能更改，暂定2019年10月1日
     * 如果在集群中需要所有点开始时间戳一致
     */
    private final long beginTimeMillis = 1569859200787L;

    /**
     * 开始时间
     */
    private long beginTime;

    /**
     * 实例所占的位数
     */
    private long instanceBitLen;

    /**
     * 支持的最大实例
     */
    private long maxInstance;

    /**
     * 序列在id中占的位数
     */
    private long sequenceBits;

    /**
     * 生成序列的掩码（最大序列值）
     */
    private long sequenceMask;

    /**
     * 配置的实例值
     */
    private final long instance;

    /**
     * 毫秒内序列(0~4095)
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间
     */
    private long lastTime = -1L;

    /**
     * 时间位左移位数
     */
    private long timeLeftShiftBits;

    /**
     * 实例位左移位数
     */
    private long instanceLeftShiftBits;
    private long maxWaitTimeMills = 200l;

    /**
     * 默认构造函数
     * 64bits
     * 实例值0
     */
    public SnowflakeIdGeneratorProvider() {
        this(0, 0);
    }

    /**
     * 指定实例值构造
     *
     * @param bitType  0 - 64位； 1 - 53 位
     * @param instance
     */
    public SnowflakeIdGeneratorProvider(int bitType, int instance) {
        init(bitType);
        if (instance > maxInstance || instance < 0) {
            throw new IllegalArgumentException(String.format("instance value can't be greater than %d or less than 0", instance));
        }
        this.instance = instance;
    }

    /**
     * 自动发现实例值
     *
     * @param bitType
     * @param autoDiscoverInstance
     */
    public SnowflakeIdGeneratorProvider(int bitType, boolean autoDiscoverInstance) {
        init(bitType);
        // 自动发现实例值
        this.instance = handleAutoDiscoverInstance();
    }

    private int handleAutoDiscoverInstance() {
        // 获取hostname如果以-数字结尾
        int instance = -1;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String hostIP = addr.getHostAddress();
            int nSplitIndex = hostIP.lastIndexOf('.');
            int mSplitIndex = hostIP.lastIndexOf('.', nSplitIndex - 1);
            int n = Integer.parseInt(hostIP.substring(nSplitIndex + 1));
            int m = Integer.parseInt(hostIP.substring(mSplitIndex + 1, nSplitIndex));
            if (m < 5) {
                instance = (m - 1) * (1 << (instanceBitLen - 2)) + n;
            } else {
                String hostName = addr.getHostName().trim();
                if (hostName.matches(".*[-]\\d\\d?")) {
                    int splitIndex = hostName.lastIndexOf('-');
                    int insTmp = Integer.parseInt(hostName.substring(splitIndex + 1));
                    if (instance <= maxInstance) {
                        instance = insTmp;
                    } else {
                        instance = insTmp % (int) (maxInstance + 1);
                    }
                } else {
                    char ch = hostName.charAt(hostName.length() - 1);
                    int insTmp = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789$_".indexOf(ch);
                    if (insTmp > -1) {
                        instance = insTmp;
                    }
                }
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (instance == -1) {
            // 获取IP
            instance = 0;
        }
        return instance;
    }

    private void init(int bitType) {
        this.bitType = bitType;
        if (bitType == 0) {
            this.generateLen = 64;
            // 时间位（41+1符号）+10位实例+12位序列
            this.instanceBitLen = 10L;
            // 每秒ID个数： (1 << 12) * 1000 = 4096000
            this.sequenceBits = 12L;
        } else {
            this.generateLen = 53;
            // 默认 5（机器位） + 16（序列号）
            // 机器实例数调整到10位，支持1024台，
            // 序号位调整到14位，每秒生产1w多id
            // 时间位缩短3位，可用时间 = 136 /8 = 17年
            this.instanceBitLen = 10L;
            this.sequenceBits = 14L;
        }
        this.sequenceMask = -1L ^ (-1L << sequenceBits);
        this.maxInstance = 2 << this.instanceBitLen - 1;
        this.timeLeftShiftBits = this.instanceBitLen + this.sequenceBits;
        this.instanceLeftShiftBits = this.sequenceBits;

        this.beginTime = getTime(beginTimeMillis);
    }

    @Override
    public synchronized long generateId() {

        long currentTime = getTime(System.currentTimeMillis());

        long instance = this.instance;

        // 如果系统时钟回退过
        if (currentTime < lastTime) {
            // 如果使用currentTime = lastTime 相当于消费未来时间的id，如果时间校对后或程序重启可能会出现重复的id
            // 一般程序启动后只要不修改服务器的时间都没有问题
            // 如何解决回拨问题之间的可用性
            long slowTime = lastTime - currentTime;
            long slowTimeMills = this.bitType == 0 ? slowTime : slowTime * 1000;
            if (slowTimeMills < maxWaitTimeMills) {
                // 如果因为自动同步时间导致200毫秒以内阻塞
                try {
                    Thread.sleep(slowTimeMills);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                // 开启备用模式（instance=maxInstance）
                instance = maxInstance;
            }
            currentTime = lastTime;
//            throw new RuntimeException(
//                    String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTime - currentTime));
        }

        //如果是同一时间生成的，则进行(毫)秒内序列
        if (currentTime == lastTime) {
            sequence = (sequence + 1) & sequenceMask;
            //毫秒内序列溢出
            if (sequence == 0) {
                //阻塞到下一个毫秒(秒),获得新的时间
                //如果不阻塞而使用时间+1递归方式会出现大批量申请时间和返回的id携带的时间信息不一致问题
                currentTime = nextUnitTime(lastTime);
            }
        } else {
            //时间改变，序列重置
            sequence = 0L;
        }

        // 上次生成ID的时间
        lastTime = currentTime;

        return ((currentTime - beginTime) << timeLeftShiftBits) //
                | (instance << instanceLeftShiftBits) //
                | sequence;
    }

    private long getTime(long timeMillis) {
        return this.bitType == 0 ? timeMillis : timeMillis / 1000;
    }

    /**
     * 下一个单位时间（毫）秒
     *
     * @param lastTime 上次时间
     * @return 当前时间
     */
    private long nextUnitTime(long lastTime) {
        long time = getTime(System.currentTimeMillis());
        //与机器的时间有关，如果是毫秒不一定会真连续
        //如果使用Thread.sleep方法单位是需要计算差值来确定要阻塞的时间，实际上也拿不到精确的值
        //这里阻塞的目的是为了其他线程在获取ID的时间同步性
        while (time <= lastTime) {
            time = getTime(System.currentTimeMillis());
        }
        return time;
    }

    @Override
    public IdInfo expId(long id) {

        IdInfo idInfo = new IdInfo();
        long time = id >> timeLeftShiftBits;
        long timestamp = time * (bitType == 0 ? 1 : 1000) + beginTimeMillis;
        long instance = (id >> instanceLeftShiftBits) % (1 << instanceBitLen);
        long sequence = id % (1 << sequenceBits);

        idInfo.setOffsetTime(time);
        idInfo.setTimestamp(timestamp);
        idInfo.setGenerateTime(new Date(timestamp));
        idInfo.setInstance(instance);
        idInfo.setSequence(sequence);
        idInfo.setBit(generateLen);

        return idInfo;
    }

    @Override
    public IdInfo expId(String hexId) {
        return expId(Long.parseLong(hexId, 16));
    }

}
