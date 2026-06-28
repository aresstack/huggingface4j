package com.aresstack.huggingface.hub.repo;

/**
 * The kind of Hugging Face Hub repository an operation targets.
 */
public enum RepositoryType {

    MODEL("model", "models", ""),
    DATASET("dataset", "datasets", "datasets"),
    SPACE("space", "spaces", "spaces");

    private final String value;
    private final String apiNamespace;
    private final String gitNamespace;

    RepositoryType(String value, String apiNamespace, String gitNamespace) {
        this.value = value;
        this.apiNamespace = apiNamespace;
        this.gitNamespace = gitNamespace;
    }

    /** The value sent in request bodies, e.g. {@code "model"}. */
    public String value() {
        return value;
    }

    /** The path segment used in API URLs, e.g. {@code "models"} in {@code /api/models/{id}}. */
    public String apiNamespace() {
        return apiNamespace;
    }

    /**
     * The path prefix used in Git/LFS URLs: empty for models, {@code "datasets"} / {@code "spaces"}
     * for the other types (e.g. {@code huggingface.co/datasets/{id}.git/...}).
     */
    public String gitNamespace() {
        return gitNamespace;
    }
}
