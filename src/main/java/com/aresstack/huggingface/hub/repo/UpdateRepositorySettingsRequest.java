package com.aresstack.huggingface.hub.repo;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;

/**
 * Fluent request that updates repository settings such as visibility or gating
 * (<code>PUT /api/{namespace}/{repoId}/settings</code>). Only the fields that are explicitly set are
 * sent.
 */
public final class UpdateRepositorySettingsRequest {

    private final HuggingFaceHubClient client;
    private final String repoId;
    private RepositoryType type = RepositoryType.MODEL;
    private Boolean privateRepository;
    private String gated;

    public UpdateRepositorySettingsRequest(HuggingFaceHubClient client, String repoId) {
        if (repoId == null || repoId.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository id must not be blank.");
        }
        this.client = client;
        this.repoId = repoId.trim();
    }

    public UpdateRepositorySettingsRequest type(RepositoryType type) {
        this.type = type == null ? RepositoryType.MODEL : type;
        return this;
    }

    public UpdateRepositorySettingsRequest privateRepository(boolean privateRepository) {
        this.privateRepository = Boolean.valueOf(privateRepository);
        return this;
    }

    public UpdateRepositorySettingsRequest visibility(RepositoryVisibility visibility) {
        this.privateRepository = visibility == null ? null : Boolean.valueOf(visibility.isPrivate());
        return this;
    }

    /** Set the gating mode: {@code "auto"}, {@code "manual"} or {@code "false"} to disable. */
    public UpdateRepositorySettingsRequest gated(String gated) {
        this.gated = gated;
        return this;
    }

    public String getRepoId() {
        return repoId;
    }

    public RepositoryType getType() {
        return type;
    }

    public Boolean getPrivateRepository() {
        return privateRepository;
    }

    public String getGated() {
        return gated;
    }

    public void execute() throws HuggingFaceHubException {
        if (privateRepository == null && gated == null) {
            throw new IllegalStateException("No settings to update; set visibility/private or gated first.");
        }
        client.updateRepositorySettings(this);
    }
}
