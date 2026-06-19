package com.aresstack.huggingface.hub.http;

import com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public final class UrlConnectionHubHttpClient implements HubHttpClient {

    private final String endpoint;
    private final HuggingFaceTokenProvider tokenProvider;

    public UrlConnectionHubHttpClient(String endpoint, HuggingFaceTokenProvider tokenProvider) {
        this.endpoint = trimTrailingSlash(endpoint);
        this.tokenProvider = tokenProvider;
    }

    @Override
    public HubHttpResponse execute(HubHttpRequest request) throws IOException {
        URL url = new URL(endpoint + request.getPathAndQuery());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestMethod(request.getMethod());
        connection.setRequestProperty("Accept", "application/json");
        applyAuthorization(connection);
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        int statusCode = connection.getResponseCode();
        InputStream inputStream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        return new HubHttpResponse(statusCode, readFully(inputStream), connection.getContentType());
    }

    private void applyAuthorization(HttpURLConnection connection) {
        if (tokenProvider == null) {
            return;
        }
        String accessToken = tokenProvider.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return;
        }
        connection.setRequestProperty("Authorization", "Bearer " + accessToken.trim());
    }

    private static byte[] readFully(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return new byte[0];
        }
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = inputStream.read(chunk)) >= 0) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        } finally {
            inputStream.close();
        }
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "https://huggingface.co";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
