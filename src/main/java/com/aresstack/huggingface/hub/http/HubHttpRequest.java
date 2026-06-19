package com.aresstack.huggingface.hub.http;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HubHttpRequest {

    private final String method;
    private final String pathAndQuery;
    private final Map<String, String> headers;

    private HubHttpRequest(String method, String pathAndQuery, Map<String, String> headers) {
        this.method = method;
        this.pathAndQuery = pathAndQuery;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<String, String>(headers));
    }

    public static HubHttpRequest get(String pathAndQuery) {
        return new HubHttpRequest("GET", pathAndQuery, Collections.<String, String>emptyMap());
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
}
