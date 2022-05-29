package org.framework.light.common.idgenerate.providers;

import java.io.InputStream;
import java.util.Properties;

import org.framework.light.common.idgenerate.entity.IdInfo;

/**
 * 分布式全局ID生成器
 */
public abstract class IdGenerator {

    private static String ID_GENERATOR_ALGORITHM;

    public final static String ID_GENERATOR_ALGORITHM_SNOWFLAKE = "snowflake";

    private static int ID_GENERATOR_SNOWFLAKE_BIT_TYPE;

    private static boolean ID_GENERATOR_SNOWFLAKE_INSTANCE_AUTODISCOVER;

    private static int ID_GENERATOR_SNOWFLAKE_INSTANCE;

    private static IdGenerator instance;

    static {
        // 加载classpath下面的 generate.properties
        try {
            Properties props = new Properties();
            InputStream is = IdGenerator.class.getResourceAsStream("/generate.properties");
            if(is != null) {
                props.load(is);
            }
            ID_GENERATOR_ALGORITHM = props.getProperty("id.generate.algorithm");
            if (ID_GENERATOR_ALGORITHM_SNOWFLAKE.equals(ID_GENERATOR_ALGORITHM)) {
                if (props.containsKey("id.generate.snowflake.bit.type")) {
                    ID_GENERATOR_SNOWFLAKE_BIT_TYPE = Integer.parseInt(props.getProperty("id.generate.snowflake.bit.type").trim());
                } else {
                    ID_GENERATOR_SNOWFLAKE_BIT_TYPE = 0;
                }
                if (props.containsKey("id.generate.snowflake.instance.auto-discover")) {
                    ID_GENERATOR_SNOWFLAKE_INSTANCE_AUTODISCOVER = "true".equals(props.getProperty("id.generate.snowflake.instance.auto-discover").trim());
                } else {
                    ID_GENERATOR_SNOWFLAKE_INSTANCE_AUTODISCOVER = false;
                }
                if (props.containsKey("id.generate.snowflake.instance")) {
                    ID_GENERATOR_SNOWFLAKE_INSTANCE = Integer.parseInt(props.getProperty("id.generate.snowflake.instance").trim());
                } else {
                    ID_GENERATOR_SNOWFLAKE_INSTANCE = 0;
                }
                if (ID_GENERATOR_SNOWFLAKE_INSTANCE_AUTODISCOVER) {
                    instance = new SnowflakeIdGeneratorProvider(ID_GENERATOR_SNOWFLAKE_BIT_TYPE, true);
                } else {
                    instance = new SnowflakeIdGeneratorProvider(ID_GENERATOR_SNOWFLAKE_BIT_TYPE, ID_GENERATOR_SNOWFLAKE_INSTANCE);
                }
            } else {
                // 其他算法
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (instance == null) {
            instance = new SnowflakeIdGeneratorProvider();
        }
    }

    /**
     * 获取实例
     *
     * @return
     */
    public static IdGenerator getInstance() {
        return instance;
    }

    /**
     * 获取指定配置实例
     *
     * @return
     */
    public static IdGenerator getInstance(int bitType, int instance) {
        return new SnowflakeIdGeneratorProvider(bitType, instance);
    }

    /**
     * 生成id
     *
     * @return
     */
    public abstract long generateId();

    /**
     * 反解id 时间位+实例+序号
     *
     * @param id
     * @return
     */
    public abstract IdInfo expId(long id);

    /**
     * 反解id 时间位+实例+序号
     *
     * @param hexId
     * @return
     */
    public abstract IdInfo expId(String hexId);

    /**
     * 生成16进制表示的字符串
     *
     * @return
     */
    public String generateHexString() {
        return Long.toHexString(generateId());
    }

    /**
     * 生成16进制表示的字符串
     *
     * @return
     */
    public String generateBinaryString() {
        return Long.toBinaryString(generateId());
    }


}
