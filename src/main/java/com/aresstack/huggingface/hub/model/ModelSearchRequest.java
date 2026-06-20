package com.aresstack.huggingface.hub.model;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.query.HubQueryParameters;

public final class ModelSearchRequest {

    private final HuggingFaceHubClient client;
    private final HubQueryParameters parameters = new HubQueryParameters();

    ModelSearchRequest(HuggingFaceHubClient client, String text) {
        this.client = client;
        parameters.set("search", text);
        parameters.setNumber("limit", Integer.valueOf(20));
    }

    public ModelSearchRequest author(String author) {
        parameters.set("author", author);
        return this;
    }

    public ModelSearchRequest task(String task) {
        parameters.set("pipeline_tag", task);
        return this;
    }

    public ModelSearchRequest library(String library) {
        parameters.set("library", library);
        return this;
    }

    public ModelSearchRequest tag(String tag) {
        parameters.add("filter", tag);
        return this;
    }

    public ModelSearchRequest sortByDownloads() {
        parameters.set("sort", "downloads");
        return this;
    }

    public ModelSearchRequest sortByLikes() {
        parameters.set("sort", "likes");
        return this;
    }

    public ModelSearchRequest sortByLastModified() {
        parameters.set("sort", "lastModified");
        return this;
    }

    public ModelSearchRequest limit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than zero.");
        }
        parameters.setNumber("limit", Integer.valueOf(limit));
        return this;
    }

    public ModelSearchRequest parameter(String name, String value) {
        parameters.set(name, value);
        return this;
    }

    public ModelSearchRequest addParameter(String name, String value) {
        parameters.add(name, value);
        return this;
    }

    public ModelSearchResult execute() throws HuggingFaceHubException {
        return client.searchModels(parameters);
    }
}
