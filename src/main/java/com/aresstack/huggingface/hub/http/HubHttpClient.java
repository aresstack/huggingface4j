package com.aresstack.huggingface.hub.http;

import com.aresstack.huggingface.hub.upload.UploadProgressListener;
import com.aresstack.huggingface.hub.upload.UploadSource;

import java.io.IOException;
import java.util.Map;

public interface HubHttpClient {

    HubHttpResponse execute(HubHttpRequest request) throws IOException;

    /**
     * Open a streaming response for the given request.
     *
     * @param request    the request to send
     * @param rangeStart when greater than zero, request only the bytes starting at this offset
     *                   (an HTTP {@code Range} header), used to resume an interrupted download
     * @return the streaming response; the caller is responsible for closing it
     */
    HubHttpStream openStream(HubHttpRequest request, long rangeStart) throws IOException;

    /**
     * Send a request to an absolute URL (for example a Git-LFS batch action on external storage).
     * No Hub {@code Authorization} header is added; only the supplied headers are sent.
     */
    HubHttpResponse executeAbsolute(String url, String method, byte[] body, Map<String, String> headers) throws IOException;

    /**
     * Stream-upload a source to an absolute URL via HTTP {@code PUT} (the Git-LFS basic transfer).
     * No Hub {@code Authorization} header is added; only the supplied headers are sent.
     */
    HubHttpResponse uploadFile(String url, UploadSource source, Map<String, String> headers,
                               String repoPath, UploadProgressListener listener) throws IOException;
}
