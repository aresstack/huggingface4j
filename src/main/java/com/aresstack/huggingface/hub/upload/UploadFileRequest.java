package com.aresstack.huggingface.hub.upload;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.repo.RepositoryType;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Convenience fluent request that uploads a single file as one commit, automatically choosing inline
 * (base64) or Git-LFS transfer based on the file size and extension. Use {@link #lfs()} or
 * {@link #smallFileOnly()} to override the decision.
 */
public final class UploadFileRequest {

    private final HuggingFaceHubClient client;
    private final String repoId;
    private final RepositoryType type;
    private final Path localFile;
    private final byte[] content;
    private String repoPath;
    private String revision = "main";
    private String commitMessage;
    private boolean createPullRequest;
    private UploadMode mode = UploadMode.AUTO;
    private long maxInlineBytes = UploadClassifier.DEFAULT_MAX_INLINE_BYTES;
    private UploadProgressListener progressListener;

    public UploadFileRequest(HuggingFaceHubClient client, String repoId, RepositoryType type, Path localFile) {
        this(client, repoId, type, localFile, null);
        if (localFile != null) {
            this.repoPath = localFile.getFileName().toString();
        }
    }

    public UploadFileRequest(HuggingFaceHubClient client, String repoId, RepositoryType type, byte[] content) {
        this(client, repoId, type, null, content);
    }

    private UploadFileRequest(HuggingFaceHubClient client, String repoId, RepositoryType type, Path localFile, byte[] content) {
        this.client = client;
        this.repoId = repoId;
        this.type = type;
        this.localFile = localFile;
        this.content = content;
    }

    /** The destination path inside the repository. Defaults to the local file name. */
    public UploadFileRequest to(String repoPath) {
        this.repoPath = repoPath;
        return this;
    }

    public UploadFileRequest revision(String revision) {
        this.revision = revision;
        return this;
    }

    public UploadFileRequest commitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
        return this;
    }

    public UploadFileRequest createPullRequest(boolean createPullRequest) {
        this.createPullRequest = createPullRequest;
        return this;
    }

    /** Force the Git-LFS transfer path regardless of size. */
    public UploadFileRequest lfs() {
        this.mode = UploadMode.LFS;
        return this;
    }

    /** Force inline (base64) transfer; suitable only for small files. */
    public UploadFileRequest smallFileOnly() {
        this.mode = UploadMode.SMALL;
        return this;
    }

    /** Files at or above this size use Git-LFS in automatic mode. */
    public UploadFileRequest maxInlineBytes(long maxInlineBytes) {
        this.maxInlineBytes = maxInlineBytes;
        return this;
    }

    public UploadFileRequest onProgress(UploadProgressListener progressListener) {
        this.progressListener = progressListener;
        return this;
    }

    public UploadResult execute() throws HuggingFaceHubException {
        if (repoPath == null || repoPath.trim().isEmpty()) {
            throw new IllegalStateException("Destination path must be set with to(...).");
        }
        CommitOperation.AddedFile file = (localFile != null
                ? CommitOperation.addFile(repoPath, localFile, mode)
                : CommitOperation.addFile(repoPath, content, mode))
                .maxInlineBytes(maxInlineBytes);

        long size;
        boolean lfs;
        String sha256;
        try {
            size = file.length();
            lfs = file.isLfs();
            // Pre-compute and cache the digest so the client does not hash the file again.
            sha256 = file.sha256Hex();
        } catch (IOException exception) {
            throw new HuggingFaceHubException("Failed to read '" + repoPath + "' for upload.", exception);
        }

        CommitResult commit = new CommitRequest(client, repoId, type)
                .maxInlineBytes(maxInlineBytes)
                .operation(file)
                .revision(revision)
                .commitMessage(commitMessage == null ? "Upload " + repoPath : commitMessage)
                .createPullRequest(createPullRequest)
                .onProgress(progressListener)
                .execute();

        return new UploadResult(repoPath, size, sha256, lfs, commit);
    }
}
