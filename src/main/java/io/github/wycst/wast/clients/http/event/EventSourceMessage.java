package io.github.wycst.wast.clients.http.event;

/**
 * SSE消息对象
 */
public class EventSourceMessage {

    private String id;
    private Object data;
    private String event;
    private long retry;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public long getRetry() {
        return retry;
    }

    public void setRetry(long retry) {
        this.retry = retry;
    }
}
