package com.aresstack.huggingface.hub.repo;

/**
 * The kind of Hugging Face Hub repository an operation targets.
 */
public enum RepositoryType {

    MODEL("model", "models"),
    DATASET("dataset", "datasets"),
    SPACE("space", "spaces");

    private final String value;
    private final String apiNamespace;

    RepositoryType(String value, String apiNamespace) {
        this.value = value;
        this.apiNamespace = apiNamespace;
    }

    /** The value sent in request bodies, e.g. {@code "model"}. */
    public String value() {
        return value;
    }

    /** The path segment used in API URLs, e.g. {@code "models"} in {@code /api/models/{id}}. */
    public String apiNamespace() {
        return apiNamespace;
    }
}
