package com.aresstack.huggingface.hub.download;

import com.aresstack.huggingface.hub.model.ModelReference;

import java.nio.file.Path;

public final class DownloadRequest {

    private final ModelReference modelReference;
    private final String filePath;
    private final String revision;
    private final Path targetFile;
    private final ProgressListener progressListener;

    public DownloadRequest(ModelReference modelReference, String filePath, String revision, Path targetFile, ProgressListener progressListener) {
        this.modelReference = modelReference;
        this.filePath = filePath;
        this.revision = revision == null || revision.trim().isEmpty() ? "main" : revision.trim();
        this.targetFile = targetFile;
        this.progressListener = progressListener;
    }

    public ModelReference getModelReference() {
        return modelReference;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getRevision() {
        return revision;
    }

    public Path getTargetFile() {
        return targetFile;
    }

    public ProgressListener getProgressListener() {
        return progressListener;
    }
}
