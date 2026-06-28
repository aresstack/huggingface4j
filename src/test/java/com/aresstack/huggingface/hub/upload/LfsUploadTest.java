package com.aresstack.huggingface.hub.upload;

import com.aresstack.huggingface.hub.HuggingFaceHub;
import com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider;
import com.aresstack.huggingface.hub.http.UrlConnectionHubHttpClient;
import com.aresstack.huggingface.hub.internal.DefaultHuggingFaceHubClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LfsUploadTest {

    private HttpServer server;
    private int port;
    private HuggingFaceHub hub;

    private final AtomicReference<String> commitBody = new AtomicReference<String>();
    private final AtomicReference<byte[]> storedBytes = new AtomicReference<byte[]>();
    private final AtomicReference<String> batchBody = new AtomicReference<String>();
    private final AtomicBoolean verifyCalled = new AtomicBoolean(false);
    private final AtomicInteger uploadCalls = new AtomicInteger(0);

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        hub = HuggingFaceHub.standard()
                .client(new DefaultHuggingFaceHubClient(
                        new UrlConnectionHubHttpClient("http://127.0.0.1:" + port, new HuggingFaceTokenProvider.Static("hf_test"))))
                .build();

        server.createContext("/lfs-storage", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                uploadCalls.incrementAndGet();
                storedBytes.set(readBytes(exchange));
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            }
        });
        server.createContext("/lfs-verify", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                verifyCalled.set(true);
                readBytes(exchange);
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            }
        });
        server.createContext("/api/models/aresstack/big-model/commit/main", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                commitBody.set(new String(readBytes(exchange), StandardCharsets.UTF_8));
                respond(exchange, 200, "{\"commitOid\":\"c0ffee\",\"commitUrl\":\"https://hf.co/c\"}");
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void uploadsLargeFileViaLfsAndReferencesObjectInCommit() throws Exception {
        byte[] payload = "pretend-these-are-model-weights".getBytes(StandardCharsets.UTF_8);
        String oid = sha256Hex(payload);
        installBatch(oid, payload.length, true, true);

        final AtomicInteger lastPercent = new AtomicInteger(-99);
        UploadResult result = hub.repositories()
                .model("aresstack/big-model")
                .uploadContent(payload)
                .to("model.safetensors")          // extension triggers LFS automatically
                .commitMessage("Upload weights")
                .onProgress(progress -> lastPercent.set(progress.getPercent()))
                .execute();

        // The object was negotiated, streamed to storage and verified.
        assertEquals(oid, batchOid(batchBody.get()));
        assertEquals(1, uploadCalls.get());
        assertArrayEquals(payload, storedBytes.get());
        assertTrue(verifyCalled.get());
        assertEquals(100, lastPercent.get());

        // The commit references the LFS object, not inline base64 content.
        assertTrue(commitBody.get().contains("\"key\":\"lfsFile\""), commitBody.get());
        assertTrue(commitBody.get().contains("\"oid\":\"" + oid + "\""), commitBody.get());
        assertTrue(commitBody.get().contains("\"size\":" + payload.length), commitBody.get());
        assertFalse(commitBody.get().contains("\"encoding\":\"base64\""), commitBody.get());

        assertTrue(result.isLfs());
        assertEquals(oid, result.getSha256());
        assertEquals(payload.length, result.getSize());
        assertEquals("c0ffee", result.getCommitOid());
    }

    @Test
    void skipsUploadWhenLfsObjectAlreadyExists() throws Exception {
        byte[] payload = "already-uploaded".getBytes(StandardCharsets.UTF_8);
        String oid = sha256Hex(payload);
        installBatch(oid, payload.length, false, false);   // no actions -> already present

        UploadResult result = hub.repositories()
                .model("aresstack/big-model")
                .uploadContent(payload)
                .to("model.bin")
                .lfs()
                .execute();

        assertEquals(0, uploadCalls.get());
        assertFalse(verifyCalled.get());
        assertTrue(commitBody.get().contains("\"key\":\"lfsFile\""), commitBody.get());
        assertTrue(result.isLfs());
    }

    @Test
    void smallFileStaysInlineWithoutLfsBatch() throws Exception {
        // No batch context installed: if the client tried LFS it would fail to connect.
        UploadResult result = hub.repositories()
                .model("aresstack/big-model")
                .uploadContent("hi".getBytes(StandardCharsets.UTF_8))
                .to("notes.txt")
                .execute();

        assertEquals(0, uploadCalls.get());
        assertFalse(result.isLfs());
        assertTrue(commitBody.get().contains("\"key\":\"file\""), commitBody.get());
        assertTrue(commitBody.get().contains("\"encoding\":\"base64\""), commitBody.get());
    }

    private void installBatch(final String oid, final long size, final boolean withActions, final boolean withVerify) {
        server.createContext("/aresstack/big-model.git/info/lfs/objects/batch", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                batchBody.set(new String(readBytes(exchange), StandardCharsets.UTF_8));
                String base = "http://127.0.0.1:" + port;
                StringBuilder json = new StringBuilder();
                json.append("{\"transfer\":\"basic\",\"objects\":[{\"oid\":\"").append(oid).append("\",\"size\":").append(size);
                if (withActions) {
                    json.append(",\"actions\":{\"upload\":{\"href\":\"").append(base).append("/lfs-storage\"}");
                    if (withVerify) {
                        json.append(",\"verify\":{\"href\":\"").append(base).append("/lfs-verify\"}");
                    }
                    json.append("}");
                }
                json.append("}]}");
                respond(exchange, 200, json.toString());
            }
        });
    }

    private static String batchOid(String body) {
        int idx = body.indexOf("\"oid\":\"");
        if (idx < 0) {
            return null;
        }
        int start = idx + "\"oid\":\"".length();
        return body.substring(start, body.indexOf('"', start));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream out = exchange.getResponseBody();
        out.write(bytes);
        out.close();
    }

    private static byte[] readBytes(HttpExchange exchange) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = exchange.getRequestBody().read(chunk)) >= 0) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder builder = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }
}
