package com.aresstack.huggingface.hub.repo;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;

/**
 * Entry point for repository management and write operations, returned by
 * {@code HuggingFaceHub.repositories()}.
 */
public final class RepositoryOperations {

    private final HuggingFaceHubClient client;

    public RepositoryOperations(HuggingFaceHubClient client) {
        this.client = client;
    }

    public CreateRepositoryRequest create(String repoId) {
        return new CreateRepositoryRequest(client, repoId);
    }

    public DeleteRepositoryRequest delete(String repoId) {
        return new DeleteRepositoryRequest(client, repoId);
    }

    public UpdateRepositorySettingsRequest settings(String repoId) {
        return new UpdateRepositorySettingsRequest(client, repoId);
    }

    public RepositoryResource model(String repoId) {
        return new RepositoryResource(client, repoId, RepositoryType.MODEL);
    }

    public RepositoryResource dataset(String repoId) {
        return new RepositoryResource(client, repoId, RepositoryType.DATASET);
    }

    public RepositoryResource space(String repoId) {
        return new RepositoryResource(client, repoId, RepositoryType.SPACE);
    }

    public RepositoryResource repository(String repoId, RepositoryType type) {
        return new RepositoryResource(client, repoId, type);
    }
}
