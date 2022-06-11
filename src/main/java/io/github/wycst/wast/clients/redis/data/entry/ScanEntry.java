package io.github.wycst.wast.clients.redis.data.entry;

import java.util.List;

/**
 * @Author: wangy
 * @Date: 2020/6/2 17:52
 * @Description:
 */
public class ScanEntry {

    private long nextCursor;
    private List result;
    private boolean end;

    public ScanEntry(long nextCursor, List result) {
        this.nextCursor = nextCursor;
        this.result = result;
        this.end = nextCursor == 0;
    }

    public long getNextCursor() {
        return nextCursor;
    }

    public List getResult() {
        return result;
    }

    public boolean isEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "ScanEntry{" +
                "nextCursor=" + nextCursor +
                ", result=" + result +
                ", end=" + end +
                '}';
    }
}
