package com.aresstack.huggingface.hub.http;

import com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UrlConnectionHubHttpClient implements HubHttpClient {

    private static final int MAX_REDIRECTS = 10;

    private final String endpoint;
    private final String endpointHost;
    private final HuggingFaceTokenProvider tokenProvider;
    private final Proxy proxy;
    private final ProxySelector proxySelector;

    public UrlConnectionHubHttpClient(String endpoint, HuggingFaceTokenProvider tokenProvider) {
        this(endpoint, tokenProvider, null, null);
    }

    /**
     * @param proxy         a fixed proxy to route all requests through, or {@code null}
     * @param proxySelector a selector consulted per request URL when no fixed {@code proxy} is set,
     *                      or {@code null} to use a direct connection. A fixed {@code proxy} takes
     *                      precedence over the selector.
     */
    public UrlConnectionHubHttpClient(String endpoint, HuggingFaceTokenProvider tokenProvider,
                                      Proxy proxy, ProxySelector proxySelector) {
        this.endpoint = trimTrailingSlash(endpoint);
        this.endpointHost = host(this.endpoint);
        this.tokenProvider = tokenProvider;
        this.proxy = proxy;
        this.proxySelector = proxySelector;
    }

    @Override
    public HubHttpResponse execute(HubHttpRequest request) throws IOException {
        HttpURLConnection connection = openFollowingRedirects(request, "application/json", 0L);
        int statusCode = connection.getResponseCode();
        InputStream inputStream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        return new HubHttpResponse(statusCode, readFully(inputStream), connection.getContentType(), collectHeaders(connection));
    }

    private static Map<String, String> collectHeaders(HttpURLConnection connection) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (key != null && values != null && !values.isEmpty()) {
                headers.put(key, values.get(0));
            }
        }
        return headers;
    }

    @Override
    public HubHttpStream openStream(HubHttpRequest request, long rangeStart) throws IOException {
        HttpURLConnection connection = openFollowingRedirects(request, "application/octet-stream", rangeStart);
        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            byte[] errorBody = readFully(connection.getErrorStream());
            return new HubHttpStream(statusCode, null, errorBody.length, errorBody.length,
                    connection.getURL().toString(), false,
                    new java.io.ByteArrayInputStream(errorBody));
        }
        boolean partial = statusCode == 206;
        long contentLength = connection.getContentLengthLong();
        long totalLength = totalLengthFrom(connection, contentLength, partial);
        String etag = firstHeader(connection, "X-Linked-ETag", "ETag");
        InputStream body = new ConnectionInputStream(connection.getInputStream(), connection);
        return new HubHttpStream(statusCode, etag, contentLength, totalLength,
                connection.getURL().toString(), partial, body);
    }

    @Override
    public HubHttpResponse executeAbsolute(String url, String method, byte[] body, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = open(new URL(url));
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        applyMethod(connection, method);
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }
        if (body != null && body.length > 0) {
            connection.setDoOutput(true);
            OutputStream outputStream = connection.getOutputStream();
            try {
                outputStream.write(body);
            } finally {
                outputStream.close();
            }
        }
        int statusCode = connection.getResponseCode();
        InputStream inputStream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        return new HubHttpResponse(statusCode, readFully(inputStream), connection.getContentType(), collectHeaders(connection));
    }

    @Override
    public HubHttpResponse uploadFile(String url, com.aresstack.huggingface.hub.upload.UploadSource source,
                                      Map<String, String> headers, String repoPath,
                                      com.aresstack.huggingface.hub.upload.UploadProgressListener listener) throws IOException {
        HttpURLConnection connection = open(new URL(url));
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(0);
        connection.setRequestMethod("PUT");
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }
        long length = source.length();
        connection.setFixedLengthStreamingMode(length);
        connection.setDoOutput(true);
        InputStream input = source.openStream();
        try {
            OutputStream output = connection.getOutputStream();
            try {
                byte[] chunk = new byte[1024 * 1024];
                long uploaded = 0L;
                int read;
                while ((read = input.read(chunk)) >= 0) {
                    output.write(chunk, 0, read);
                    uploaded += read;
                    if (listener != null) {
                        listener.onProgress(new com.aresstack.huggingface.hub.upload.UploadProgress(repoPath, uploaded, length));
                    }
                }
            } finally {
                output.close();
            }
        } finally {
            input.close();
        }
        int statusCode = connection.getResponseCode();
        InputStream responseStream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        return new HubHttpResponse(statusCode, readFully(responseStream), connection.getContentType(), collectHeaders(connection));
    }

    private HttpURLConnection openFollowingRedirects(HubHttpRequest request, String accept, long rangeStart) throws IOException {
        URL url = new URL(endpoint + request.getPathAndQuery());
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            HttpURLConnection connection = openUrl(url, request, accept, rangeStart, endpointHost.equalsIgnoreCase(url.getHost()));
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

    private HttpURLConnection openUrl(URL url, HubHttpRequest request, String accept, long rangeStart, boolean sendAuthorization) throws IOException {
        HttpURLConnection connection = open(url);
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        applyMethod(connection, request.getMethod());
        connection.setRequestProperty("Accept", accept);
        if (rangeStart > 0L) {
            connection.setRequestProperty("Range", "bytes=" + rangeStart + "-");
        }
        if (request.getContentType() != null) {
            connection.setRequestProperty("Content-Type", request.getContentType());
        }
        if (sendAuthorization) {
            applyAuthorization(connection);
        }
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        if (request.hasBody()) {
            connection.setDoOutput(true);
            OutputStream outputStream = connection.getOutputStream();
            try {
                outputStream.write(request.getBody());
            } finally {
                outputStream.close();
            }
        }
        return connection;
    }

    private static long totalLengthFrom(HttpURLConnection connection, long contentLength, boolean partial) {
        if (partial) {
            String contentRange = connection.getHeaderField("Content-Range");
            if (contentRange != null) {
                int slash = contentRange.lastIndexOf('/');
                if (slash >= 0 && slash + 1 < contentRange.length()) {
                    try {
                        return Long.parseLong(contentRange.substring(slash + 1).trim());
                    } catch (NumberFormatException ignored) {
                        // fall through
                    }
                }
            }
        }
        return contentLength;
    }

    private static String firstHeader(HttpURLConnection connection, String first, String second) {
        String value = connection.getHeaderField(first);
        return value != null ? value : connection.getHeaderField(second);
    }

    private static void applyMethod(HttpURLConnection connection, String method) throws ProtocolException {
        try {
            connection.setRequestMethod(method);
        } catch (ProtocolException exception) {
            // HttpURLConnection does not allow PATCH directly; tunnel it through POST with an override
            // header, which the Hugging Face Hub (and most servers/proxies) honour.
            if ("PATCH".equals(method)) {
                connection.setRequestMethod("POST");
                connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            } else {
                throw exception;
            }
        }
    }

    /** Open a connection, routing it through the configured proxy (or selector) when present. */
    private HttpURLConnection open(URL url) throws IOException {
        Proxy resolved = resolveProxy(url);
        URLConnection connection = resolved != null ? url.openConnection(resolved) : url.openConnection();
        return (HttpURLConnection) connection;
    }

    private Proxy resolveProxy(URL url) {
        if (proxy != null) {
            return proxy;
        }
        if (proxySelector != null) {
            try {
                List<Proxy> proxies = proxySelector.select(url.toURI());
                if (proxies != null && !proxies.isEmpty()) {
                    // The first entry may be Proxy.NO_PROXY, which means a direct connection.
                    return proxies.get(0);
                }
            } catch (URISyntaxException ignored) {
                // Fall back to a direct connection for a URL that is not a valid URI.
            }
        }
        return null;
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

    /** Closes the underlying {@link HttpURLConnection} when the body stream is closed. */
    private static final class ConnectionInputStream extends FilterInputStream {
        private final HttpURLConnection connection;

        private ConnectionInputStream(InputStream in, HttpURLConnection connection) {
            super(in);
            this.connection = connection;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                connection.disconnect();
            }
        }
    }
}
