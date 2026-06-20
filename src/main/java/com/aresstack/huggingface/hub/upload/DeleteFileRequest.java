package com.aresstack.huggingface.hub.upload;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.repo.RepositoryType;

/**
 * Convenience fluent request that deletes a single file as one commit.
 */
public final class DeleteFileRequest {

    private final HuggingFaceHubClient client;
    private final String repoId;
    private final RepositoryType type;
    private final String repoPath;
    private String revision = "main";
    private String commitMessage;
    private boolean createPullRequest;

    public DeleteFileRequest(HuggingFaceHubClient client, String repoId, RepositoryType type, String repoPath) {
        if (repoPath == null || repoPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository file path must not be blank.");
        }
        this.client = client;
        this.repoId = repoId;
        this.type = type;
        this.repoPath = repoPath.trim();
    }

    public DeleteFileRequest revision(String revision) {
        this.revision = revision;
        return this;
    }

    public DeleteFileRequest commitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
        return this;
    }

    public DeleteFileRequest createPullRequest(boolean createPullRequest) {
        this.createPullRequest = createPullRequest;
        return this;
    }

    public CommitResult execute() throws HuggingFaceHubException {
        return new CommitRequest(client, repoId, type)
                .deleteFile(repoPath)
                .revision(revision)
                .commitMessage(commitMessage == null ? "Delete " + repoPath : commitMessage)
                .createPullRequest(createPullRequest)
                .execute();
    }
}
