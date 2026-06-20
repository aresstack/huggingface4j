package com.aresstack.huggingface.hub.upload;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.repo.RepositoryType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fluent request that creates a commit with one or more {@link CommitOperation}s
 * (<code>POST /api/{namespace}/{repoId}/commit/{revision}</code>).
 */
public final class CommitRequest {

    private final HuggingFaceHubClient client;
    private final String repoId;
    private final RepositoryType type;
    private final List<CommitOperation> operations = new ArrayList<CommitOperation>();
    private String revision = "main";
    private String summary;
    private String description;
    private boolean createPullRequest;

    public CommitRequest(HuggingFaceHubClient client, String repoId, RepositoryType type) {
        if (repoId == null || repoId.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository id must not be blank.");
        }
        this.client = client;
        this.repoId = repoId.trim();
        this.type = type == null ? RepositoryType.MODEL : type;
    }

    public CommitRequest addFile(String repoPath, byte[] content) {
        operations.add(CommitOperation.addFile(repoPath, content));
        return this;
    }

    public CommitRequest addFile(String repoPath, Path localFile) {
        operations.add(CommitOperation.addFile(repoPath, localFile));
        return this;
    }

    public CommitRequest deleteFile(String repoPath) {
        operations.add(CommitOperation.deleteFile(repoPath));
        return this;
    }

    public CommitRequest operation(CommitOperation operation) {
        if (operation != null) {
            operations.add(operation);
        }
        return this;
    }

    public CommitRequest revision(String revision) {
        this.revision = revision == null || revision.trim().isEmpty() ? "main" : revision.trim();
        return this;
    }

    public CommitRequest commitMessage(String summary) {
        this.summary = summary;
        return this;
    }

    public CommitRequest description(String description) {
        this.description = description;
        return this;
    }

    public CommitRequest createPullRequest(boolean createPullRequest) {
        this.createPullRequest = createPullRequest;
        return this;
    }

    public String getRepoId() {
        return repoId;
    }

    public RepositoryType getType() {
        return type;
    }

    public String getRevision() {
        return revision;
    }

    public String getSummary() {
        return summary == null || summary.trim().isEmpty() ? "Commit via huggingface4j" : summary;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCreatePullRequest() {
        return createPullRequest;
    }

    public List<CommitOperation> getOperations() {
        return Collections.unmodifiableList(operations);
    }

    public CommitResult execute() throws HuggingFaceHubException {
        if (operations.isEmpty()) {
            throw new IllegalStateException("A commit requires at least one operation.");
        }
        return client.commit(this);
    }
}
