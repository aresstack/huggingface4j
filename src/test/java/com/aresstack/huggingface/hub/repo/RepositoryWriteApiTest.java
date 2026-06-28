package com.aresstack.huggingface.hub.repo;

import com.aresstack.huggingface.hub.HuggingFaceHub;
import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider;
import com.aresstack.huggingface.hub.http.UrlConnectionHubHttpClient;
import com.aresstack.huggingface.hub.internal.DefaultHuggingFaceHubClient;
import com.aresstack.huggingface.hub.upload.CommitResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RepositoryWriteApiTest {

    private HttpServer server;
    private Recorder recorder;
    private HuggingFaceHub hub;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        recorder = new Recorder();
        int port = server.getAddress().getPort();
        server.start();
        hub = HuggingFaceHub.standard()
                .client(new DefaultHuggingFaceHubClient(
                        new UrlConnectionHubHttpClient("http://127.0.0.1:" + port, new HuggingFaceTokenProvider.Static("hf_test"))))
                .build();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void createsRepository() throws Exception {
        recorder.respond("/api/repos/create", 200, "{\"url\":\"https://huggingface.co/aresstack/my-model\",\"name\":\"aresstack/my-model\"}");

        RepositoryInfo info = hub.repositories()
                .create("aresstack/my-model")
                .type(RepositoryType.MODEL)
                .privateRepository(false)
                .execute();

        assertEquals("POST", recorder.method);
        assertEquals("/api/repos/create", recorder.path);
        assertTrue(recorder.body.contains("\"type\":\"model\""), recorder.body);
        assertTrue(recorder.body.contains("\"name\":\"my-model\""), recorder.body);
        assertTrue(recorder.body.contains("\"organization\":\"aresstack\""), recorder.body);
        assertTrue(recorder.body.contains("\"private\":false"), recorder.body);
        assertEquals("https://huggingface.co/aresstack/my-model", info.getUrl());
        assertEquals("aresstack/my-model", info.getRepoId());
    }

    @Test
    void deleteRepositoryRequiresConfirmation() {
        DeleteRepositoryRequest request = hub.repositories().delete("aresstack/my-model").type(RepositoryType.MODEL);
        assertThrows(IllegalStateException.class, request::execute);
    }

    @Test
    void deleteRepositoryRejectsWrongConfirmation() {
        assertThrows(IllegalArgumentException.class,
                () -> hub.repositories().delete("aresstack/my-model").confirm("aresstack/other"));
    }

    @Test
    void deletesRepositoryWhenConfirmed() throws Exception {
        recorder.respond("/api/repos/delete", 200, "{}");

        hub.repositories()
                .delete("aresstack/my-model")
                .type(RepositoryType.MODEL)
                .confirm("aresstack/my-model")
                .execute();

        assertEquals("DELETE", recorder.method);
        assertEquals("/api/repos/delete", recorder.path);
        assertTrue(recorder.body.contains("\"name\":\"my-model\""), recorder.body);
    }

    @Test
    void updatesRepositorySettings() throws Exception {
        recorder.respond("/api/models/aresstack/my-model/settings", 200, "{}");

        hub.repositories().model("aresstack/my-model").settings().privateRepository(true).execute();

        assertEquals("PUT", recorder.method);
        assertEquals("/api/models/aresstack/my-model/settings", recorder.path);
        assertTrue(recorder.body.contains("\"private\":true"), recorder.body);
    }

    @Test
    void uploadsFileViaCommit() throws Exception {
        recorder.respond("/api/models/aresstack/my-model/commit/main", 200,
                "{\"commitOid\":\"abc123\",\"commitUrl\":\"https://huggingface.co/aresstack/my-model/commit/abc123\"}");

        com.aresstack.huggingface.hub.upload.UploadResult result = hub.repositories()
                .model("aresstack/my-model")
                .uploadContent("hello".getBytes(StandardCharsets.UTF_8))
                .to("config.json")
                .commitMessage("Upload config")
                .execute();

        assertEquals("POST", recorder.method);
        assertEquals("/api/models/aresstack/my-model/commit/main", recorder.path);
        assertEquals("application/x-ndjson", recorder.contentType);

        String[] lines = recorder.body.split("\n");
        assertTrue(lines[0].contains("\"key\":\"header\""), lines[0]);
        assertTrue(lines[0].contains("Upload config"), lines[0]);
        assertTrue(lines[1].contains("\"key\":\"file\""), lines[1]);
        assertTrue(lines[1].contains("\"path\":\"config.json\""), lines[1]);
        assertTrue(lines[1].contains("\"" + Base64.getEncoder().encodeToString("hello".getBytes(StandardCharsets.UTF_8)) + "\""), lines[1]);
        assertEquals("abc123", result.getCommitOid());
        assertEquals(false, result.isLfs());
        assertEquals(5L, result.getSize());
    }

    @Test
    void deletesFileViaCommitWithPullRequest() throws Exception {
        recorder.respond("/api/models/aresstack/my-model/commit/main", 200,
                "{\"commitOid\":\"def456\",\"pullRequestUrl\":\"https://huggingface.co/aresstack/my-model/discussions/1\"}");

        CommitResult result = hub.repositories()
                .model("aresstack/my-model")
                .commit()
                .deleteFile("old.bin")
                .commitMessage("Remove old file")
                .createPullRequest(true)
                .execute();

        assertEquals("POST", recorder.method);
        assertTrue(recorder.rawQuery != null && recorder.rawQuery.contains("create_pr=1"), String.valueOf(recorder.rawQuery));
        assertTrue(recorder.body.contains("\"key\":\"deletedFile\""), recorder.body);
        assertTrue(recorder.body.contains("\"path\":\"old.bin\""), recorder.body);
        assertEquals("https://huggingface.co/aresstack/my-model/discussions/1", result.getPullRequestUrl());
    }

    @Test
    void mapsForbiddenOnWrite() {
        recorder.respond("/api/repos/create", 403, "no write access");
        assertThrows(HuggingFaceHubException.Forbidden.class,
                () -> hub.repositories().create("aresstack/my-model").execute());
    }

    private final class Recorder {
        private volatile String method;
        private volatile String path;
        private volatile String rawQuery;
        private volatile String body;
        private volatile String contentType;

        private void respond(String context, int status, String responseBody) {
            server.createContext(context, new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    method = exchange.getRequestMethod();
                    path = exchange.getRequestURI().getPath();
                    rawQuery = exchange.getRequestURI().getRawQuery();
                    contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                    body = readBody(exchange);
                    byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(status, bytes.length);
                    OutputStream out = exchange.getResponseBody();
                    out.write(bytes);
                    out.close();
                }
            });
        }
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
}
