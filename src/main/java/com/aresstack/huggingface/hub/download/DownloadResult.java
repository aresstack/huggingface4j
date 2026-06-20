package com.aresstack.huggingface.hub.download;

import java.nio.file.Path;

public final class DownloadResult {

    private final Path targetFile;
    private final long bytesDownloaded;
    private final long contentLength;
    private final String etag;
    private final String resolvedUrl;
    private final boolean skipped;

    public DownloadResult(Path targetFile, long bytesDownloaded) {
        this(targetFile, bytesDownloaded, bytesDownloaded, null, null, false);
    }

    public DownloadResult(Path targetFile, long bytesDownloaded, long contentLength, String etag, String resolvedUrl, boolean skipped) {
        this.targetFile = targetFile;
        this.bytesDownloaded = bytesDownloaded;
        this.contentLength = contentLength;
        this.etag = etag;
        this.resolvedUrl = resolvedUrl;
        this.skipped = skipped;
    }

    public Path getTargetFile() {
        return targetFile;
    }

    /** The number of bytes actually transferred over the network for this call. */
    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    /** The full size of the downloaded file, or {@code -1} when unknown. */
    public long getContentLength() {
        return contentLength;
    }

    /** The ETag reported by the Hub for the resolved file, or {@code null}. */
    public String getEtag() {
        return etag;
    }

    /** The final URL the file was resolved to after redirects, or {@code null}. */
    public String getResolvedUrl() {
        return resolvedUrl;
    }

    /** Whether an existing file was kept because of the {@link OverwritePolicy}. */
    public boolean isSkipped() {
        return skipped;
    }
}
