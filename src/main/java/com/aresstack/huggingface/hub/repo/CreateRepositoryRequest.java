package com.aresstack.huggingface.hub.repo;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;

/**
 * Fluent request that creates a repository on the Hugging Face Hub
 * (<code>POST /api/repos/create</code>).
 */
public final class CreateRepositoryRequest {

    private final HuggingFaceHubClient client;
    private final String repoId;
    private RepositoryType type = RepositoryType.MODEL;
    private boolean privateRepository;
    private boolean existsOk;

    public CreateRepositoryRequest(HuggingFaceHubClient client, String repoId) {
        if (repoId == null || repoId.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository id must not be blank.");
        }
        this.client = client;
        this.repoId = repoId.trim();
    }

    public CreateRepositoryRequest type(RepositoryType type) {
        this.type = type == null ? RepositoryType.MODEL : type;
        return this;
    }

    public CreateRepositoryRequest privateRepository(boolean privateRepository) {
        this.privateRepository = privateRepository;
        return this;
    }

    public CreateRepositoryRequest visibility(RepositoryVisibility visibility) {
        this.privateRepository = visibility != null && visibility.isPrivate();
        return this;
    }

    /** Do not fail if the repository already exists. */
    public CreateRepositoryRequest existsOk(boolean existsOk) {
        this.existsOk = existsOk;
        return this;
    }

    public String getRepoId() {
        return repoId;
    }

    public RepositoryType getType() {
        return type;
    }

    public boolean isPrivateRepository() {
        return privateRepository;
    }

    public boolean isExistsOk() {
        return existsOk;
    }

    public RepositoryInfo execute() throws HuggingFaceHubException {
        return client.createRepository(this);
    }
}
