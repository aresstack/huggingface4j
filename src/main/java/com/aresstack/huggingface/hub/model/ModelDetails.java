package com.aresstack.huggingface.hub.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModelDetails {

    private final ModelSummary summary;
    private final String sha;
    private final List<HubFile> files;

    public ModelDetails(ModelSummary summary, String sha, List<HubFile> files) {
        this.summary = summary;
        this.sha = sha;
        this.files = files == null ? Collections.<HubFile>emptyList() : Collections.unmodifiableList(new ArrayList<HubFile>(files));
    }

    public ModelSummary getSummary() {
        return summary;
    }

    public String getSha() {
        return sha;
    }

    public List<HubFile> getFiles() {
        return files;
    }
}
