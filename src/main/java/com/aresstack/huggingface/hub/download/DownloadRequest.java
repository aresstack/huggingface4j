package com.aresstack.huggingface.hub.download;

import com.aresstack.huggingface.hub.model.ModelReference;

import java.nio.file.Path;

public final class DownloadRequest {

    private final ModelReference modelReference;
    private final String filePath;
    private final String revision;
    private final Path targetFile;
    private final ProgressListener progressListener;
    private final OverwritePolicy overwritePolicy;
    private final boolean resume;
    private final String expectedSha256;
    private final Long expectedSize;

    public DownloadRequest(ModelReference modelReference, String filePath, String revision, Path targetFile, ProgressListener progressListener) {
        this(modelReference, filePath, revision, targetFile, progressListener, OverwritePolicy.OVERWRITE, false, null, null);
    }

    public DownloadRequest(ModelReference modelReference, String filePath, String revision, Path targetFile,
                           ProgressListener progressListener, OverwritePolicy overwritePolicy, boolean resume,
                           String expectedSha256, Long expectedSize) {
        this.modelReference = modelReference;
        this.filePath = filePath;
        this.revision = revision == null || revision.trim().isEmpty() ? "main" : revision.trim();
        this.targetFile = targetFile;
        this.progressListener = progressListener;
        this.overwritePolicy = overwritePolicy == null ? OverwritePolicy.OVERWRITE : overwritePolicy;
        this.resume = resume;
        this.expectedSha256 = expectedSha256;
        this.expectedSize = expectedSize;
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

    public OverwritePolicy getOverwritePolicy() {
        return overwritePolicy;
    }

    public boolean isResume() {
        return resume;
    }

    public String getExpectedSha256() {
        return expectedSha256;
    }

    public Long getExpectedSize() {
        return expectedSize;
    }
}
