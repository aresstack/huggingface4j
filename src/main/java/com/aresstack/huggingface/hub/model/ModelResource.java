package com.aresstack.huggingface.hub.model;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.download.ModelFileDownloadRequest;
import com.aresstack.huggingface.hub.download.SnapshotDownloadRequest;

public final class ModelResource {

    private final HuggingFaceHubClient client;
    private final String id;

    ModelResource(HuggingFaceHubClient client, String id) {
        this.client = client;
        this.id = id;
    }

    public ModelDetailsRequest details() {
        return new ModelDetailsRequest(client, id);
    }

    public ModelFilesRequest files() {
        return new ModelFilesRequest(client, id);
    }

    public ModelFileDownloadRequest file(String path) {
        return new ModelFileDownloadRequest(client, id, path);
    }

    public SnapshotDownloadRequest snapshot() {
        return new SnapshotDownloadRequest(client, id);
    }

    public ModelDetails execute() throws HuggingFaceHubException {
        return details().execute();
    }
}
