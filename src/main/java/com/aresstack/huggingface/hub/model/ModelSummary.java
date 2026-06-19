package com.aresstack.huggingface.hub.model;

public final class ModelSummary {

    private final String id;
    private final String task;
    private final String library;
    private final Long downloads;
    private final Long likes;

    public ModelSummary(String id) {
        this(id, null, null, null, null);
    }

    public ModelSummary(String id, String task, String library, Long downloads, Long likes) {
        this.id = id;
        this.task = task;
        this.library = library;
        this.downloads = downloads;
        this.likes = likes;
    }

    public String getId() {
        return id;
    }

    public String getRepoId() {
        return id;
    }

    public String getTask() {
        return task;
    }

    public String getLibrary() {
        return library;
    }

    public Long getDownloads() {
        return downloads;
    }

    public Long getLikes() {
        return likes;
    }
}
