package com.aresstack.huggingface.hub.query;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public final class HubUrlBuilder {

    private HubUrlBuilder() {
    }

    public static String path(String rawPath) {
        String[] parts = rawPath.split("/");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].length() > 0) {
                builder.append('/').append(encode(parts[i]));
            }
        }
        return builder.length() == 0 ? "/" : builder.toString();
    }

    public static String appendQuery(String path, HubQueryParameters parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return path;
        }
        return path + "?" + parameters.toQueryString();
    }

    public static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 must be available.", exception);
        }
    }
}
