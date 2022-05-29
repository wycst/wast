package org.framework.light.clients.redis.options;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: wangy
 * @Date: 2020/6/28 18:55
 * @Description:
 */
public class ScoreOptions {

    private boolean limitable;
    private long offset;
    private long count;

    private boolean withscores;

    public boolean isLimitable() {
        return limitable;
    }

    public void setLimitable(boolean limitable) {
        this.limitable = limitable;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public boolean isWithscores() {
        return withscores;
    }

    public void setWithscores(boolean withscores) {
        this.withscores = withscores;
    }

    public List<String> buildCommands() {

        List<String> commands = new ArrayList<String>();

        if(limitable) {
            commands.add("LIMIT");
            commands.add(String.valueOf(offset));
            commands.add(String.valueOf(count));
        }

        if(withscores) {
            commands.add("WITHSCORES");
        }

        return commands;
    }
}
