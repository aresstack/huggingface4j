package com.aresstack.huggingface.hub.download;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.model.ModelReference;

import java.nio.file.Path;

public final class ModelFileDownloadRequest {

    private final HuggingFaceHubClient client;
    private final String id;
    private final String path;
    private String revision = "main";
    private Path targetFile;
    private ProgressListener progressListener;

    public ModelFileDownloadRequest(HuggingFaceHubClient client, String id, String path) {
        this.client = client;
        this.id = id;
        this.path = path;
    }

    public ModelFileDownloadRequest revision(String revision) {
        this.revision = revision;
        return this;
    }

    public ModelFileDownloadRequest downloadTo(Path targetFile) {
        this.targetFile = targetFile;
        return this;
    }

    public ModelFileDownloadRequest onProgress(ProgressListener progressListener) {
        this.progressListener = progressListener;
        return this;
    }

    public DownloadResult execute() throws HuggingFaceHubException {
        if (targetFile == null) {
            throw new IllegalStateException("Target file must be set before executing download.");
        }
        DownloadRequest request = new DownloadRequest(ModelReference.model(id), path, revision, targetFile, progressListener);
        return client.downloadFile(request);
    }
}
