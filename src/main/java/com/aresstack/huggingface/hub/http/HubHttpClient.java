package com.aresstack.huggingface.hub.http;

import java.io.IOException;

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
}
