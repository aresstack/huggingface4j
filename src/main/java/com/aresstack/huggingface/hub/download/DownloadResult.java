package com.aresstack.huggingface.hub.download;

import java.nio.file.Path;

public final class DownloadResult {

    private final Path targetFile;
    private final long bytesDownloaded;

    public DownloadResult(Path targetFile, long bytesDownloaded) {
        this.targetFile = targetFile;
        this.bytesDownloaded = bytesDownloaded;
    }

    public Path getTargetFile() {
        return targetFile;
    }

    public long getBytesDownloaded() {
        return bytesDownloaded;
    }
}
