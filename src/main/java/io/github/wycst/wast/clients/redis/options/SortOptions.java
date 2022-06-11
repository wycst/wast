package io.github.wycst.wast.clients.redis.options;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: wangy
 * @Date: 2020/5/28 21:50
 * @Description:
 */
public class SortOptions {

    private boolean limitable;
    private long offset;
    private long count;

    private String order;
    private boolean alpha;
    private String storeKey;
    private String byKeyPattern;
    private List<String> getPatterns = new ArrayList<String>();

    public SortOptions limit(long offset, long count) {
        this.offset = offset;
        this.count = count;
        this.limitable = count > -1;
        return this;
    }

    public SortOptions by(String pattern) {
        this.byKeyPattern = pattern;
        return this;
    }

    public SortOptions unby(String pattern) {
        this.byKeyPattern = null;
        return this;
    }

    public SortOptions get(String pattern) {
        this.getPatterns.add(pattern);
        return this;
    }

    public SortOptions remove(String pattern) {
        this.getPatterns.remove(pattern);
        return this;
    }

    public SortOptions asc() {
        this.order = "ASC";
        return this;
    }

    public SortOptions desc() {
        this.order = "DESC";
        return this;
    }

    public SortOptions alpha(boolean v) {
        this.alpha = v;
        return this;
    }

    public SortOptions store(String key) {
        this.storeKey = storeKey;
        return this;
    }

    public List<String> buildCommands() {

        List<String> commands = new ArrayList<String>();

        if (byKeyPattern != null) {
            commands.add("BY");
            commands.add(byKeyPattern);
        }

        if (limitable) {
            commands.add("LIMIT");
            commands.add(String.valueOf(offset));
            commands.add(String.valueOf(count));
        }

        if (getPatterns.size() > 0) {
            for (String getPattern : getPatterns) {
                commands.add("GET");
                commands.add(getPattern);
            }
        }

        if (order != null) {
            commands.add(order);
        }

        if (alpha) {
            commands.add("ALPHA");
        }

        if (storeKey != null && storeKey.trim().length() > 0) {
            commands.add("STORE");
            commands.add("storeKey");
        }

        return commands;
    }


}
