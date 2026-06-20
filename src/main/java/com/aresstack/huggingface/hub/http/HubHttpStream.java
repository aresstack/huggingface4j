package com.aresstack.huggingface.hub.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * A streaming HTTP response used for large downloads.
 *
 * <p>The body is exposed as a raw {@link InputStream} so that callers can stream it directly to disk
 * without buffering it in memory. When {@link #getStatusCode()} indicates an error the stream holds
 * the (typically small) error body instead.</p>
 */
public final class HubHttpStream implements Closeable {

    private final int statusCode;
    private final String etag;
    private final long contentLength;
    private final long totalLength;
    private final String resolvedUrl;
    private final boolean partial;
    private final InputStream body;

    public HubHttpStream(int statusCode, String etag, long contentLength, long totalLength,
                         String resolvedUrl, boolean partial, InputStream body) {
        this.statusCode = statusCode;
        this.etag = etag;
        this.contentLength = contentLength;
        this.totalLength = totalLength;
        this.resolvedUrl = resolvedUrl;
        this.partial = partial;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /** Whether the server answered a range request with {@code 206 Partial Content}. */
    public boolean isPartial() {
        return partial;
    }

    /** The (possibly quoted) ETag of the resolved file, or {@code null} when not provided. */
    public String getEtag() {
        return etag;
    }

    /** The number of bytes in this response body, or {@code -1} when unknown. */
    public long getContentLength() {
        return contentLength;
    }

    /** The total size of the file regardless of any range offset, or {@code -1} when unknown. */
    public long getTotalLength() {
        return totalLength;
    }

    /** The final URL after following redirects, or {@code null} when unavailable. */
    public String getResolvedUrl() {
        return resolvedUrl;
    }

    public InputStream getBody() {
        return body;
    }

    @Override
    public void close() throws IOException {
        if (body != null) {
            body.close();
        }
    }
}
