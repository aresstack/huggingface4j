package com.aresstack.huggingface.hub.http;

import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider;
import com.aresstack.huggingface.hub.download.DownloadRequest;
import com.aresstack.huggingface.hub.download.DownloadResult;
import com.aresstack.huggingface.hub.download.FileDownloader;
import com.aresstack.huggingface.hub.download.OverwritePolicy;
import com.aresstack.huggingface.hub.model.ModelReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class UrlConnectionHubHttpClientTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsPostFormBody() throws Exception {
        final AtomicReference<String> method = new AtomicReference<String>();
        final AtomicReference<String> body = new AtomicReference<String>();
        final AtomicReference<String> contentType = new AtomicReference<String>();
        server.createContext("/oauth/device", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                method.set(exchange.getRequestMethod());
                contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
                body.set(readBody(exchange));
                respond(exchange, 200, "{}".getBytes(StandardCharsets.UTF_8));
            }
        });

        HubHttpClient client = new UrlConnectionHubHttpClient(endpoint("127.0.0.1"), new HuggingFaceTokenProvider.Anonymous());
        com.aresstack.huggingface.hub.query.HubQueryParameters form =
                new com.aresstack.huggingface.hub.query.HubQueryParameters().set("client_id", "askai").set("scope", "openid profile");
        client.execute(HubHttpRequest.postForm("/oauth/device", form));

        assertEquals("POST", method.get());
        assertEquals("application/x-www-form-urlencoded", contentType.get());
        assertEquals("client_id=askai&scope=openid%20profile", body.get());
    }

    @Test
    void tunnelsPatchThroughPostWithOverrideHeader() throws Exception {
        final AtomicReference<String> method = new AtomicReference<String>();
        final AtomicReference<String> override = new AtomicReference<String>();
        final AtomicReference<String> body = new AtomicReference<String>();
        server.createContext("/api/models/org/model/settings", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                method.set(exchange.getRequestMethod());
                override.set(exchange.getRequestHeaders().getFirst("X-HTTP-Method-Override"));
                body.set(readBody(exchange));
                respond(exchange, 200, "{}".getBytes(StandardCharsets.UTF_8));
            }
        });

        HubHttpClient client = new UrlConnectionHubHttpClient(endpoint("127.0.0.1"), new HuggingFaceTokenProvider.Anonymous());
        client.execute(HubHttpRequest.patchJson("/api/models/org/model/settings", "{\"private\":true}"));

        assertEquals("POST", method.get());
        assertEquals("PATCH", override.get());
        assertEquals("{\"private\":true}", body.get());
    }

    @Test
    void exposesResponseHeaders() throws Exception {
        server.createContext("/created", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().add("Location", "https://huggingface.co/org/model");
                exchange.getResponseHeaders().add("ETag", "\"v1\"");
                respond(exchange, 201, "{}".getBytes(StandardCharsets.UTF_8));
            }
        });

        HubHttpClient client = new UrlConnectionHubHttpClient(endpoint("127.0.0.1"), new HuggingFaceTokenProvider.Anonymous());
        HubHttpResponse response = client.execute(HubHttpRequest.postJson("/created", "{}"));

        assertEquals(201, response.getStatusCode());
        assertEquals("https://huggingface.co/org/model", response.getLocation());
        assertEquals("\"v1\"", response.getETag());
    }

    @Test
    void keepsAuthorizationOnSameHostRedirect() throws Exception {
        final AtomicReference<String> auth = new AtomicReference<String>();
        server.createContext("/redir", redirectTo("http://127.0.0.1:" + port + "/secured-same"));
        server.createContext("/secured-same", recordAuth(auth));

        HubHttpClient client = new UrlConnectionHubHttpClient(endpoint("127.0.0.1"), new HuggingFaceTokenProvider.Static("hf_secret"));
        client.execute(HubHttpRequest.get("/redir"));

        assertEquals("Bearer hf_secret", auth.get());
    }

    @Test
    void stripsAuthorizationOnCrossHostRedirect() throws Exception {
        assumeTrue(canConnect("localhost", port), "localhost is not reachable on the test machine");
        final AtomicReference<String> auth = new AtomicReference<String>();
        final AtomicReference<Boolean> hit = new AtomicReference<Boolean>(Boolean.FALSE);
        server.createContext("/redir", redirectTo("http://localhost:" + port + "/secured-cross"));
        server.createContext("/secured-cross", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                hit.set(Boolean.TRUE);
                auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
                respond(exchange, 200, "ok".getBytes(StandardCharsets.UTF_8));
            }
        });

        HubHttpClient client = new UrlConnectionHubHttpClient(endpoint("127.0.0.1"), new HuggingFaceTokenProvider.Static("hf_secret"));
        client.execute(HubHttpRequest.get("/redir"));

        assertEquals(Boolean.TRUE, hit.get());
        assertNull(auth.get(), "Authorization header must not leak to a different host");
    }

    @Test
    void downloadsToTempFileThenMovesAtomically(@TempDir Path dir) throws Exception {
        final byte[] payload = "the quick brown fox".getBytes(StandardCharsets.UTF_8);
        server.createContext("/file", serveBytes(payload, true));

        HubHttpClient client = new UrlConnectionHubHttpClient(endpoint("127.0.0.1"), new HuggingFaceTokenProvider.Anonymous());
        Path target = dir.resolve("out.bin");
        DownloadResult result = new FileDownloader(client).download(HubHttpRequest.get("/file"),
                new DownloadRequest(ModelReference.model("org/model"), "out.bin", "main", target, null));

        assertArrayEquals(payload, Files.readAllBytes(target));
        assertEquals(payload.length, result.getBytesDownloaded());
        assertEquals("\"etag-xyz\"", result.getEtag());
        assertNotNull(result.getResolvedUrl());
    }

    @Test
    void resumesPartialDownload(@TempDir Path dir) throws Exception {
        final byte[] payload = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
        server.createContext("/file", serveBytes(payload, true));

        Path target = dir.resolve("out.bin");
        Path part = target.resolveSibling("out.bin.part");
        Files.write(part, "0123456".getBytes(StandardCharsets.UTF_8));

        HubHttpClient client = new UrlConnectionHubHttpClient(endpoint("127.0.0.1"), new HuggingFaceTokenProvider.Anonymous());
        DownloadResult result = new FileDownloader(client).download(HubHttpRequest.get("/file"),
                new DownloadRequest(ModelReference.model("org/model"), "out.bin", "main", target, null,
                        OverwritePolicy.OVERWRITE, true, null, null));

        assertArrayEquals(payload, Files.readAllBytes(target));
        // Only the remaining 9 bytes are transferred when resuming.
        assertEquals(payload.length - 7, result.getBytesDownloaded());
    }

    @Test
    void mapsDownloadErrorStatusToResponse(@TempDir Path dir) {
        server.createContext("/file", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                respond(exchange, 403, "gated repo".getBytes(StandardCharsets.UTF_8));
            }
        });

        HubHttpClient client = new UrlConnectionHubHttpClient(endpoint("127.0.0.1"), new HuggingFaceTokenProvider.Anonymous());
        Path target = dir.resolve("out.bin");
        HuggingFaceHubException.Forbidden failure = assertThrows(HuggingFaceHubException.Forbidden.class,
                () -> new FileDownloader(client).download(HubHttpRequest.get("/file"),
                        new DownloadRequest(ModelReference.model("org/model"), "out.bin", "main", target, null)));
        assertEquals(403, failure.getStatusCode());
    }

    private String endpoint(String host) {
        return "http://" + host + ":" + port;
    }

    private static HttpHandler redirectTo(final String location) {
        return new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().add("Location", location);
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            }
        };
    }

    private static HttpHandler recordAuth(final AtomicReference<String> auth) {
        return new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
                respond(exchange, 200, "ok".getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    private static HttpHandler serveBytes(final byte[] payload, final boolean supportRange) {
        return new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().add("ETag", "\"etag-xyz\"");
                String range = exchange.getRequestHeaders().getFirst("Range");
                if (supportRange && range != null && range.startsWith("bytes=")) {
                    int start = Integer.parseInt(range.substring("bytes=".length()).split("-")[0]);
                    int length = payload.length - start;
                    exchange.getResponseHeaders().add("Content-Range", "bytes " + start + "-" + (payload.length - 1) + "/" + payload.length);
                    exchange.sendResponseHeaders(206, length);
                    OutputStream out = exchange.getResponseBody();
                    out.write(payload, start, length);
                    out.close();
                    return;
                }
                respond(exchange, 200, payload);
            }
        };
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length);
        OutputStream out = exchange.getResponseBody();
        out.write(body);
        out.close();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int read;
        while ((read = exchange.getRequestBody().read(chunk)) >= 0) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static boolean canConnect(String host, int port) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), 500);
            return true;
        } catch (IOException exception) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }
}
