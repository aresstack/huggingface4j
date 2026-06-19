package com.aresstack.huggingface.hub.model;

public final class ModelReference {

    private final String repoId;

    private ModelReference(String repoId) {
        if (repoId == null || repoId.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository id must not be blank.");
        }
        this.repoId = repoId.trim();
    }

    public static ModelReference model(String repoId) {
        return new ModelReference(repoId);
    }

    public String getRepoId() {
        return repoId;
    }
}
