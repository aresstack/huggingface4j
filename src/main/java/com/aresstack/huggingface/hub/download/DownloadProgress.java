package com.aresstack.huggingface.hub.download;

public final class DownloadProgress {

    private final long bytesDownloaded;
    private final long totalBytes;

    public DownloadProgress(long bytesDownloaded, long totalBytes) {
        this.bytesDownloaded = bytesDownloaded;
        this.totalBytes = totalBytes;
    }

    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getPercent() {
        if (totalBytes <= 0L) {
            return -1;
        }
        return (int) Math.min(100L, Math.max(0L, bytesDownloaded * 100L / totalBytes));
    }
}
