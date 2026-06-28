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
    private long maxInlineBytes = UploadClassifier.DEFAULT_MAX_INLINE_BYTES;
    private UploadProgressListener progressListener;

    public CommitRequest(HuggingFaceHubClient client, String repoId, RepositoryType type) {
        if (repoId == null || repoId.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository id must not be blank.");
        }
        this.client = client;
        this.repoId = repoId.trim();
        this.type = type == null ? RepositoryType.MODEL : type;
    }

    public CommitRequest addFile(String repoPath, byte[] content) {
        return operation(CommitOperation.addFile(repoPath, content));
    }

    public CommitRequest addFile(String repoPath, Path localFile) {
        return operation(CommitOperation.addFile(repoPath, localFile));
    }

    public CommitRequest addFile(String repoPath, byte[] content, UploadMode mode) {
        return operation(CommitOperation.addFile(repoPath, content, mode));
    }

    public CommitRequest addFile(String repoPath, Path localFile, UploadMode mode) {
        return operation(CommitOperation.addFile(repoPath, localFile, mode));
    }

    public CommitRequest deleteFile(String repoPath) {
        return operation(CommitOperation.deleteFile(repoPath));
    }

    public CommitRequest operation(CommitOperation operation) {
        if (operation != null) {
            if (operation instanceof CommitOperation.AddedFile) {
                ((CommitOperation.AddedFile) operation).maxInlineBytes(maxInlineBytes);
            }
            operations.add(operation);
        }
        return this;
    }

    /** Files at or above this size use Git-LFS in {@link UploadMode#AUTO}. */
    public CommitRequest maxInlineBytes(long maxInlineBytes) {
        this.maxInlineBytes = maxInlineBytes;
        for (CommitOperation operation : operations) {
            if (operation instanceof CommitOperation.AddedFile) {
                ((CommitOperation.AddedFile) operation).maxInlineBytes(maxInlineBytes);
            }
        }
        return this;
    }

    public CommitRequest onProgress(UploadProgressListener progressListener) {
        this.progressListener = progressListener;
        return this;
    }

    public UploadProgressListener getProgressListener() {
        return progressListener;
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
