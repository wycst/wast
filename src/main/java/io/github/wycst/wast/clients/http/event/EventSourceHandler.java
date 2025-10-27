package io.github.wycst.wast.clients.http.event;

import io.github.wycst.wast.clients.http.definition.HttpClientResponse;

import java.io.IOException;

public final class EventSourceHandler {
    private boolean success;
    HttpClientResponse response;
    volatile boolean closed = false;

    public EventSourceHandler() {
    }

    /**
     * 客户端关闭SSE（重试循环）
     */
    public void close() {
        try {
            closed = true;
            if (response != null) {
                response.inputStream().close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public HttpClientResponse getResponse() {
        return response;
    }

    public void resultOf(boolean success, HttpClientResponse response) {
        this.success = success;
        this.response = response;
    }

    public boolean isClosed() {
        return closed;
    }
}
