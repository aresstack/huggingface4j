package com.aresstack.huggingface.hub.internal;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;

public final class DefaultHuggingFaceHubClient implements HuggingFaceHubClient {

    @Override
    public com.aresstack.huggingface.hub.model.ModelSearchResult searchModels(com.aresstack.huggingface.hub.query.HubQueryParameters parameters) throws com.aresstack.huggingface.hub.HuggingFaceHubException {
        return new com.aresstack.huggingface.hub.model.ModelSearchResult(java.util.Collections.<com.aresstack.huggingface.hub.model.ModelSummary>emptyList());
    }

    @Override
    public com.aresstack.huggingface.hub.model.ModelDetails getModelDetails(com.aresstack.huggingface.hub.model.ModelDetailsQuery query) throws com.aresstack.huggingface.hub.HuggingFaceHubException {
        return new com.aresstack.huggingface.hub.model.ModelDetails(new com.aresstack.huggingface.hub.model.ModelSummary(query.getModelReference().getRepoId()), null, java.util.Collections.<com.aresstack.huggingface.hub.model.HubFile>emptyList());
    }
}
