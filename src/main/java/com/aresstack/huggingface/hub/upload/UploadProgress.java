package com.aresstack.huggingface.hub.upload;

public final class UploadProgress {

    private final String path;
    private final long bytesUploaded;
    private final long totalBytes;

    public UploadProgress(String path, long bytesUploaded, long totalBytes) {
        this.path = path;
        this.bytesUploaded = bytesUploaded;
        this.totalBytes = totalBytes;
    }

    /** The repository path of the file currently being uploaded. */
    public String getPath() {
        return path;
    }

    public long getBytesUploaded() {
        return bytesUploaded;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getPercent() {
        if (totalBytes <= 0L) {
            return -1;
        }
        return (int) Math.min(100L, Math.max(0L, bytesUploaded * 100L / totalBytes));
    }
}
