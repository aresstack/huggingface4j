package com.aresstack.huggingface.hub.http;

import com.aresstack.huggingface.hub.query.HubQueryParameters;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HubHttpRequest {

    private final String method;
    private final String pathAndQuery;
    private final Map<String, String> headers;
    private final byte[] body;
    private final String contentType;

    private HubHttpRequest(String method, String pathAndQuery, Map<String, String> headers, byte[] body, String contentType) {
        this.method = method;
        this.pathAndQuery = pathAndQuery;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<String, String>(headers));
        this.body = body == null ? new byte[0] : body;
        this.contentType = contentType;
    }

    public static HubHttpRequest get(String pathAndQuery) {
        return new HubHttpRequest("GET", pathAndQuery, Collections.<String, String>emptyMap(), null, null);
    }

    public static HubHttpRequest postForm(String pathAndQuery, HubQueryParameters form) {
        try {
            byte[] body = (form == null ? "" : form.toQueryString()).getBytes("UTF-8");
            return new HubHttpRequest("POST", pathAndQuery, Collections.<String, String>emptyMap(), body, "application/x-www-form-urlencoded");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 must be available.", exception);
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPathAndQuery() {
        return pathAndQuery;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        byte[] copy = new byte[body.length];
        System.arraycopy(body, 0, copy, 0, body.length);
        return copy;
    }

    public boolean hasBody() {
        return body.length > 0;
    }

    public String getContentType() {
        return contentType;
    }
}
