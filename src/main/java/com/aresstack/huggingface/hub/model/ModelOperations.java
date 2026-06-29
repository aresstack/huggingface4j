package com.aresstack.huggingface.hub.model;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;

/**
 * Fluent entry point for model search and per-model operations, returned by
 * {@code HuggingFaceHub.models()}.
 */
public final class ModelOperations {

    private final HuggingFaceHubClient client;

    public ModelOperations(HuggingFaceHubClient client) {
        this.client = client;
    }

    /** Start a model search seeded with a free-text query. */
    public ModelSearchRequest search(String text) {
        return new ModelSearchRequest(client, text);
    }

    /** Start a model search without a free-text query (filter-only). */
    public ModelSearchRequest search() {
        return new ModelSearchRequest(client, null);
    }

    /** Operate on a specific model repository (details, files, downloads). */
    public ModelResource model(String id) {
        return new ModelResource(client, id);
    }
}
