package org.framework.light.clients.redis.options;

public class CommandOptions<E> {

    private String code;
    private Class<E> typeCls;
    private String desc;
    private CommandOptions(String code, String desc, Class<E> typeCls) {
        this.code = code;
        this.desc = desc;
        this.typeCls = typeCls;
    }

    public String getCode() {
        return code;
    }

    public Class<E> getTypeCls() {
        return typeCls;
    }

    public static CommandOptions<Long> REFCOUNT = new CommandOptions<Long>("REFCOUNT", "引用次数",Long.class);
    public static CommandOptions<Long> IDLETIME = new CommandOptions<Long>("IDLETIME", "空闲时间",Long.class);
    public static CommandOptions<String> ENCODING = new CommandOptions<String>("ENCODING", "存储类型",String.class);

    public static CommandOptions<String> CHANNELS = new CommandOptions<String>("CHANNELS", "存储类型",String.class);

}
