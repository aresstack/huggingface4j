package com.aresstack.huggingface.hub.model;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;

public final class ModelOperations {

    private final HuggingFaceHubClient client;

    public ModelOperations(HuggingFaceHubClient client) {
        this.client = client;
    }

    public ModelSearchRequest search(String text) {
        return new ModelSearchRequest(client, text);
    }

    public ModelSearchRequest search() {
        return new ModelSearchRequest(client, null);
    }

    public ModelResource model(String id) {
        return new ModelResource(client, id);
    }
}
