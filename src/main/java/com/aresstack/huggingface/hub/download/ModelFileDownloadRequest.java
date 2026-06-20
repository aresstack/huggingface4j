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
    private OverwritePolicy overwritePolicy = OverwritePolicy.OVERWRITE;
    private boolean resume;
    private String expectedSha256;
    private Long expectedSize;

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

    public ModelFileDownloadRequest overwritePolicy(OverwritePolicy overwritePolicy) {
        this.overwritePolicy = overwritePolicy == null ? OverwritePolicy.OVERWRITE : overwritePolicy;
        return this;
    }

    /** Convenience for {@code overwritePolicy(overwrite ? OVERWRITE : SKIP_IF_EXISTS)}. */
    public ModelFileDownloadRequest overwrite(boolean overwrite) {
        return overwritePolicy(overwrite ? OverwritePolicy.OVERWRITE : OverwritePolicy.SKIP_IF_EXISTS);
    }

    /** Resume a previously interrupted download from its {@code .part} file when present. */
    public ModelFileDownloadRequest resume(boolean resume) {
        this.resume = resume;
        return this;
    }

    /** Verify the downloaded file against the given hex SHA-256 checksum. */
    public ModelFileDownloadRequest verifySha256(String sha256) {
        this.expectedSha256 = sha256;
        return this;
    }

    /** Verify the downloaded file against the given expected size in bytes. */
    public ModelFileDownloadRequest verifySize(long size) {
        this.expectedSize = Long.valueOf(size);
        return this;
    }

    public DownloadResult execute() throws HuggingFaceHubException {
        if (targetFile == null) {
            throw new IllegalStateException("Target file must be set before executing download.");
        }
        DownloadRequest request = new DownloadRequest(ModelReference.model(id), path, revision, targetFile,
                progressListener, overwritePolicy, resume, expectedSha256, expectedSize);
        return client.downloadFile(request);
    }
}
