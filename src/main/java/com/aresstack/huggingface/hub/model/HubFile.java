package com.aresstack.huggingface.hub.model;

public final class HubFile {

    private final String path;
    private final Long size;

    public HubFile(String path, Long size) {
        this.path = path;
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public Long getSize() {
        return size;
    }
}
