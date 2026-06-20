package com.aresstack.huggingface.hub.upload;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.repo.RepositoryType;

import java.nio.file.Path;

/**
 * Convenience fluent request that uploads a single file as one commit.
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

    public CommitResult execute() throws HuggingFaceHubException {
        if (repoPath == null || repoPath.trim().isEmpty()) {
            throw new IllegalStateException("Destination path must be set with to(...).");
        }
        CommitOperation operation = localFile != null
                ? CommitOperation.addFile(repoPath, localFile)
                : CommitOperation.addFile(repoPath, content);
        return new CommitRequest(client, repoId, type)
                .operation(operation)
                .revision(revision)
                .commitMessage(commitMessage == null ? "Upload " + repoPath : commitMessage)
                .createPullRequest(createPullRequest)
                .execute();
    }
}
