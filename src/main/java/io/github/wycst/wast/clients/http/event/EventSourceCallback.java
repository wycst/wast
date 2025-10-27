package io.github.wycst.wast.clients.http.event;

import io.github.wycst.wast.clients.http.definition.HttpClientResponse;
import io.github.wycst.wast.clients.http.definition.ResponseCallback;

public abstract class EventSourceCallback implements ResponseCallback {

    @Override
    public final void onDownloadProgress(long downloaded, long total) {
    }

    public abstract void onmessage(EventSourceMessage message);

    public void onerror(Throwable throwable) {
    }

    public void oncomplete() {
    }

    public void onopen(HttpClientResponse response) {
    }

    public void onclose() {
    }
}
