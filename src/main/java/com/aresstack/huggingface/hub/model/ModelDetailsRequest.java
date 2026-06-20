package com.aresstack.huggingface.hub.model;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;

public final class ModelDetailsRequest {

    private final HuggingFaceHubClient client;
    private final ModelDetailsQuery query;

    ModelDetailsRequest(HuggingFaceHubClient client, String id) {
        this.client = client;
        this.query = new ModelDetailsQuery(ModelReference.model(id));
    }

    public ModelDetailsRequest revision(String revision) {
        query.revision(revision);
        return this;
    }

    public ModelDetailsRequest includeFiles() {
        query.include("siblings");
        return this;
    }

    public ModelDetailsRequest includeConfig() {
        query.include("config");
        return this;
    }

    public ModelDetailsRequest includeSafetensors() {
        query.include("safetensors");
        return this;
    }

    public ModelDetailsRequest includeRawField(String fieldName) {
        query.include(fieldName);
        return this;
    }

    public ModelDetails execute() throws HuggingFaceHubException {
        return client.getModelDetails(query);
    }
}
