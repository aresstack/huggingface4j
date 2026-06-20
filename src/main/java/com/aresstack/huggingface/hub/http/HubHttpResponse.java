package com.aresstack.huggingface.hub.http;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class HubHttpResponse {

    private final int statusCode;
    private final byte[] body;
    private final String contentType;
    private final Map<String, String> headers;

    public HubHttpResponse(int statusCode, byte[] body, String contentType) {
        this(statusCode, body, contentType, Collections.<String, String>emptyMap());
    }

    public HubHttpResponse(int statusCode, byte[] body, String contentType, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.body = body == null ? new byte[0] : body;
        this.contentType = contentType;
        Map<String, String> lowerCased = new LinkedHashMap<String, String>();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() != null) {
                    lowerCased.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
                }
            }
        }
        this.headers = Collections.unmodifiableMap(lowerCased);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public byte[] getBody() {
        byte[] copy = new byte[body.length];
        System.arraycopy(body, 0, copy, 0, body.length);
        return copy;
    }

    public String getBodyAsUtf8() {
        try {
            return new String(body, "UTF-8");
        } catch (java.io.UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 must be available.", exception);
        }
    }

    public String getContentType() {
        return contentType;
    }

    /** Case-insensitive header lookup; returns {@code null} when absent. */
    public String getHeader(String name) {
        return name == null ? null : headers.get(name.toLowerCase(Locale.ROOT));
    }

    public String getLocation() {
        return getHeader("Location");
    }

    public String getETag() {
        return getHeader("ETag");
    }

    public String getRateLimitRemaining() {
        return getHeader("X-RateLimit-Remaining");
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
