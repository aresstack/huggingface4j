package com.aresstack.huggingface.hub.repo;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;

/**
 * Fluent request that deletes a repository (<code>DELETE /api/repos/delete</code>).
 *
 * <p>Deletion is irreversible, so {@link #execute()} refuses to run until the caller has explicitly
 * confirmed the exact repository id via {@link #confirm(String)} (or {@link #confirmDestructiveAction()}).
 * This prevents accidental one-call deletions.</p>
 */
public final class DeleteRepositoryRequest {

    private final HuggingFaceHubClient client;
    private final String repoId;
    private RepositoryType type = RepositoryType.MODEL;
    private boolean missingOk;
    private boolean confirmed;

    public DeleteRepositoryRequest(HuggingFaceHubClient client, String repoId) {
        if (repoId == null || repoId.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository id must not be blank.");
        }
        this.client = client;
        this.repoId = repoId.trim();
    }

    public DeleteRepositoryRequest type(RepositoryType type) {
        this.type = type == null ? RepositoryType.MODEL : type;
        return this;
    }

    /** Do not fail if the repository does not exist. */
    public DeleteRepositoryRequest missingOk(boolean missingOk) {
        this.missingOk = missingOk;
        return this;
    }

    /** Confirm the deletion by repeating the exact repository id. */
    public DeleteRepositoryRequest confirm(String expectedRepoId) {
        this.confirmed = repoId.equals(expectedRepoId == null ? null : expectedRepoId.trim());
        if (!confirmed) {
            throw new IllegalArgumentException("Confirmation '" + expectedRepoId
                    + "' does not match the repository id '" + repoId + "'.");
        }
        return this;
    }

    /** Confirm the deletion without re-typing the id. Prefer {@link #confirm(String)}. */
    public DeleteRepositoryRequest confirmDestructiveAction() {
        this.confirmed = true;
        return this;
    }

    public String getRepoId() {
        return repoId;
    }

    public RepositoryType getType() {
        return type;
    }

    public boolean isMissingOk() {
        return missingOk;
    }

    public void execute() throws HuggingFaceHubException {
        if (!confirmed) {
            throw new IllegalStateException("Refusing to delete '" + repoId
                    + "' without confirmation. Call confirm(\"" + repoId + "\") first.");
        }
        client.deleteRepository(this);
    }
}
