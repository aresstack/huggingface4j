package com.aresstack.huggingface.hub.model;

public final class ModelSummary {

    private final String id;

    public ModelSummary(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
