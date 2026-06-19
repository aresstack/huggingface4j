package com.aresstack.huggingface.hub.model;

public final class ModelDetailsQuery {

    private final ModelReference modelReference;

    public ModelDetailsQuery(ModelReference modelReference) {
        this.modelReference = modelReference;
    }

    public ModelReference getModelReference() {
        return modelReference;
    }
}
