package com.aresstack.huggingface.hub.model;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;

import java.util.List;

public final class ModelFilesRequest {

    private final HuggingFaceHubClient client;
    private final ModelDetailsQuery query;

    ModelFilesRequest(HuggingFaceHubClient client, String id) {
        this.client = client;
        this.query = new ModelDetailsQuery(ModelReference.model(id));
        this.query.include("siblings");
    }

    public ModelFilesRequest revision(String revision) {
        query.revision(revision);
        return this;
    }

    public List<HubFile> execute() throws HuggingFaceHubException {
        return client.listModelFiles(query);
    }
}
