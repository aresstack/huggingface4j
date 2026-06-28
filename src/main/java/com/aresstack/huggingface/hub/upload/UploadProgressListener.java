package com.aresstack.huggingface.hub.upload;

public interface UploadProgressListener {

    void onProgress(UploadProgress progress);
}
