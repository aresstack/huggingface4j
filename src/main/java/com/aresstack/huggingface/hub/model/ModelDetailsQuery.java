package com.aresstack.huggingface.hub.model;

public final class ModelDetailsQuery {

    private final ModelReference modelReference;
    private final com.aresstack.huggingface.hub.query.HubQueryParameters parameters = new com.aresstack.huggingface.hub.query.HubQueryParameters();

    public ModelDetailsQuery(ModelReference modelReference) {
        this.modelReference = modelReference;
    }

    public ModelReference getModelReference() {
        return modelReference;
    }

    public ModelDetailsQuery revision(String revision) {
        parameters.set("revision", revision);
        return this;
    }

    public ModelDetailsQuery include(String fieldName) {
        parameters.add("expand", fieldName);
        return this;
    }

    public com.aresstack.huggingface.hub.query.HubQueryParameters toQueryParameters() {
        return parameters;
    }
}
