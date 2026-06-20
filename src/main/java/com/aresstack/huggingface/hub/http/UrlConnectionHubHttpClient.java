package com.aresstack.huggingface.hub.http;

import com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public final class UrlConnectionHubHttpClient implements HubHttpClient {

    private static final int MAX_REDIRECTS = 10;

    private final String endpoint;
    private final String endpointHost;
    private final HuggingFaceTokenProvider tokenProvider;

    public UrlConnectionHubHttpClient(String endpoint, HuggingFaceTokenProvider tokenProvider) {
        this.endpoint = trimTrailingSlash(endpoint);
        this.endpointHost = host(this.endpoint);
        this.tokenProvider = tokenProvider;
    }

    @Override
    public HubHttpResponse execute(HubHttpRequest request) throws IOException {
        HttpURLConnection connection = openFollowingRedirects(request, "application/json");
        int statusCode = connection.getResponseCode();
        InputStream inputStream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        return new HubHttpResponse(statusCode, readFully(inputStream), connection.getContentType());
    }

    @Override
    public long download(HubHttpRequest request, Path targetFile, com.aresstack.huggingface.hub.download.ProgressListener progressListener) throws IOException {
        HttpURLConnection connection = openFollowingRedirects(request, "application/octet-stream");
        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            InputStream errorStream = connection.getErrorStream();
            String body = new String(readFully(errorStream), "UTF-8");
            throw new IOException("HTTP " + statusCode + ": " + body);
        }
        Path parent = targetFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        long totalBytes = connection.getContentLengthLong();
        long downloaded = 0L;
        InputStream inputStream = connection.getInputStream();
        try {
            OutputStream outputStream = Files.newOutputStream(targetFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                byte[] chunk = new byte[1024 * 1024];
                int read;
                while ((read = inputStream.read(chunk)) >= 0) {
                    outputStream.write(chunk, 0, read);
                    downloaded += read;
                    if (progressListener != null) {
                        progressListener.onProgress(new com.aresstack.huggingface.hub.download.DownloadProgress(downloaded, totalBytes));
                    }
                }
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
        }
        return downloaded;
    }

    private HttpURLConnection openFollowingRedirects(HubHttpRequest request, String accept) throws IOException {
        URL url = new URL(endpoint + request.getPathAndQuery());
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            HttpURLConnection connection = openUrl(url, request, accept, endpointHost.equalsIgnoreCase(url.getHost()));
            int statusCode = connection.getResponseCode();
            if (!isRedirect(statusCode)) {
                return connection;
            }
            String location = connection.getHeaderField("Location");
            if (location == null || location.trim().isEmpty()) {
                return connection;
            }
            url = new URL(url, location);
        }
        throw new IOException("Too many redirects while contacting Hugging Face Hub.");
    }

    private HttpURLConnection openUrl(URL url, HubHttpRequest request, String accept, boolean sendAuthorization) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestMethod(request.getMethod());
        connection.setRequestProperty("Accept", accept);
        if (sendAuthorization) {
            applyAuthorization(connection);
        }
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        return connection;
    }

    private static boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
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

    private static String host(String endpoint) {
        try {
            return new URL(endpoint).getHost();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid Hugging Face endpoint: " + endpoint, exception);
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
