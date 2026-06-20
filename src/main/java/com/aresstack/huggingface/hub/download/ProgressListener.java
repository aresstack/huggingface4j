package com.aresstack.huggingface.hub.download;

public interface ProgressListener {

    void onProgress(DownloadProgress progress);
}
