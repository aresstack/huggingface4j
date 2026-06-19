package com.aresstack.huggingface.hub.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModelSearchResult {

    private final List<ModelSummary> models;

    public ModelSearchResult(List<ModelSummary> models) {
        this.models = models == null ? Collections.<ModelSummary>emptyList() : Collections.unmodifiableList(new ArrayList<ModelSummary>(models));
    }

    public List<ModelSummary> getModels() {
        return models;
    }

    public int size() {
        return models.size();
    }
}
