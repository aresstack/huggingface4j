package com.aresstack.huggingface.hub.http;

import com.aresstack.huggingface.hub.query.HubQueryParameters;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HubHttpRequest {

    private static final String JSON = "application/json";

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
        byte[] body = utf8(form == null ? "" : form.toQueryString());
        return new HubHttpRequest("POST", pathAndQuery, Collections.<String, String>emptyMap(), body, "application/x-www-form-urlencoded");
    }

    public static HubHttpRequest postJson(String pathAndQuery, String json) {
        return new HubHttpRequest("POST", pathAndQuery, Collections.<String, String>emptyMap(), utf8(json), JSON);
    }

    public static HubHttpRequest putJson(String pathAndQuery, String json) {
        return new HubHttpRequest("PUT", pathAndQuery, Collections.<String, String>emptyMap(), utf8(json), JSON);
    }

    public static HubHttpRequest patchJson(String pathAndQuery, String json) {
        return new HubHttpRequest("PATCH", pathAndQuery, Collections.<String, String>emptyMap(), utf8(json), JSON);
    }

    public static HubHttpRequest delete(String pathAndQuery) {
        return new HubHttpRequest("DELETE", pathAndQuery, Collections.<String, String>emptyMap(), null, null);
    }

    public static HubHttpRequest deleteJson(String pathAndQuery, String json) {
        return new HubHttpRequest("DELETE", pathAndQuery, Collections.<String, String>emptyMap(), utf8(json), JSON);
    }

    /** Generic request with a raw body, used for example for {@code application/x-ndjson} commits. */
    public static HubHttpRequest withBody(String method, String pathAndQuery, byte[] body, String contentType) {
        return new HubHttpRequest(method, pathAndQuery, Collections.<String, String>emptyMap(), body, contentType);
    }

    /** Return a copy of this request with an extra request header set. */
    public HubHttpRequest withHeader(String name, String value) {
        if (name == null || value == null) {
            return this;
        }
        Map<String, String> merged = new LinkedHashMap<String, String>(headers);
        merged.put(name, value);
        return new HubHttpRequest(method, pathAndQuery, merged, body, contentType);
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

    private static byte[] utf8(String value) {
        try {
            return (value == null ? "" : value).getBytes("UTF-8");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 must be available.", exception);
        }
    }
}
