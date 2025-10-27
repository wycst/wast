package io.github.wycst.wast.clients.http.definition;

public interface ResponseCallback {
    void onDownloadProgress(long downloaded, long total);
}
