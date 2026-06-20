package com.aresstack.huggingface.hub.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModelSummary {

    private final String id;
    private final String author;
    private final String task;
    private final String library;
    private final List<String> tags;
    private final String license;
    private final Boolean gated;
    private final Long downloads;
    private final Long likes;

    public ModelSummary(String id) {
        this(id, null, null, null, Collections.<String>emptyList(), null, null, null, null);
    }

    public ModelSummary(String id, String author, String task, String library, List<String> tags,
                        String license, Boolean gated, Long downloads, Long likes) {
        this.id = id;
        this.author = author;
        this.task = task;
        this.library = library;
        this.tags = tags == null ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<String>(tags));
        this.license = license;
        this.gated = gated;
        this.downloads = downloads;
        this.likes = likes;
    }

    public String getId() {
        return id;
    }

    public String getRepoId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getTask() {
        return task;
    }

    public String getLibrary() {
        return library;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getLicense() {
        return license;
    }

    public Boolean getGated() {
        return gated;
    }

    public Long getDownloads() {
        return downloads;
    }

    public Long getLikes() {
        return likes;
    }
}
