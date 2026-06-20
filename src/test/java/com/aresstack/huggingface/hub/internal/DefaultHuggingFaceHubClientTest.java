package com.aresstack.huggingface.hub.internal;

import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.download.DownloadRequest;
import com.aresstack.huggingface.hub.download.DownloadResult;
import com.aresstack.huggingface.hub.download.OverwritePolicy;
import com.aresstack.huggingface.hub.http.HubHttpClient;
import com.aresstack.huggingface.hub.http.HubHttpRequest;
import com.aresstack.huggingface.hub.http.HubHttpResponse;
import com.aresstack.huggingface.hub.http.HubHttpStream;
import com.aresstack.huggingface.hub.model.ModelDetails;
import com.aresstack.huggingface.hub.model.ModelDetailsQuery;
import com.aresstack.huggingface.hub.model.ModelReference;
import com.aresstack.huggingface.hub.model.ModelSearchResult;
import com.aresstack.huggingface.hub.query.HubQueryParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultHuggingFaceHubClientTest {

    @Test
    void requestsModelSearchEndpoint() throws Exception {
        FakeHttpClient http = new FakeHttpClient(200, "[{\"id\":\"org/model\"}]");
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(http);

        ModelSearchResult result = client.searchModels(new HubQueryParameters().set("search", "qwen").setNumber("limit", Integer.valueOf(1)));

        assertEquals("/api/models?search=qwen&limit=1", http.lastRequest.getPathAndQuery());
        assertEquals("org/model", result.getModels().get(0).getRepoId());
    }

    @Test
    void requestsModelDetailsEndpointWithExpand() throws Exception {
        FakeHttpClient http = new FakeHttpClient(200, "{\"id\":\"org/model\",\"sha\":\"abc\"}");
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(http);
        ModelDetailsQuery query = new ModelDetailsQuery(ModelReference.model("org/model"));
        query.include("siblings");

        ModelDetails details = client.getModelDetails(query);

        assertEquals("/api/models/org/model?expand=siblings", http.lastRequest.getPathAndQuery());
        assertEquals("abc", details.getSha());
    }

    @Test
    void mapsUnauthorizedResponse() {
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(new FakeHttpClient(401, "nope"));
        assertThrows(HuggingFaceHubException.Unauthorized.class,
                () -> client.searchModels(new HubQueryParameters().set("search", "private")));
    }

    @Test
    void mapsForbiddenResponse() {
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(new FakeHttpClient(403, "gated"));
        assertThrows(HuggingFaceHubException.Forbidden.class,
                () -> client.searchModels(new HubQueryParameters().set("search", "gated")));
    }

    @Test
    void mapsNotFoundResponse() {
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(new FakeHttpClient(404, "missing"));
        assertThrows(HuggingFaceHubException.NotFound.class,
                () -> client.searchModels(new HubQueryParameters().set("search", "missing")));
    }

    @Test
    void mapsRateLimitedResponse() {
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(new FakeHttpClient(429, "slow"));
        assertThrows(HuggingFaceHubException.RateLimited.class,
                () -> client.searchModels(new HubQueryParameters().set("search", "busy")));
    }

    @Test
    void mapsUnexpectedStatusToGenericResponse() {
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(new FakeHttpClient(500, "boom"));
        HuggingFaceHubException.Response failure = assertThrows(HuggingFaceHubException.Response.class,
                () -> client.searchModels(new HubQueryParameters().set("search", "x")));
        assertEquals(500, failure.getStatusCode());
        assertEquals("boom", failure.getResponseBody());
    }

    @Test
    void downloadsFileToTargetWithMetadata(@TempDir Path dir) throws Exception {
        FakeHttpClient http = new FakeHttpClient(200, "[]");
        http.downloadBody = "hello world".getBytes(StandardCharsets.UTF_8);
        http.downloadEtag = "\"etag-1\"";
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(http);
        Path target = dir.resolve("nested").resolve("config.json");

        DownloadResult result = client.downloadFile(new DownloadRequest(
                ModelReference.model("org/model"), "config.json", "abc", target, null));

        assertEquals("/org/model/resolve/abc/config.json", http.lastDownload.getPathAndQuery());
        assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));
        assertEquals(11L, result.getBytesDownloaded());
        assertEquals("\"etag-1\"", result.getEtag());
        assertFalse(result.isSkipped());
        assertFalse(Files.exists(target.resolveSibling("config.json.part")));
    }

    @Test
    void mapsDownloadHttpErrorToResponse(@TempDir Path dir) {
        FakeHttpClient http = new FakeHttpClient(200, "[]");
        http.downloadStatus = 404;
        http.downloadBody = "no such file".getBytes(StandardCharsets.UTF_8);
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(http);

        assertThrows(HuggingFaceHubException.NotFound.class, () -> client.downloadFile(new DownloadRequest(
                ModelReference.model("org/model"), "missing.bin", "main", dir.resolve("missing.bin"), null)));
    }

    @Test
    void skipsDownloadWhenFileExists(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("config.json");
        Files.write(target, "existing".getBytes(StandardCharsets.UTF_8));
        FakeHttpClient http = new FakeHttpClient(200, "[]");
        http.downloadBody = "new".getBytes(StandardCharsets.UTF_8);
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(http);

        DownloadResult result = client.downloadFile(new DownloadRequest(
                ModelReference.model("org/model"), "config.json", "main", target, null,
                OverwritePolicy.SKIP_IF_EXISTS, false, null, null));

        assertTrue(result.isSkipped());
        assertArrayEquals("existing".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));
    }

    @Test
    void failsDownloadWhenFileExistsAndPolicyForbids(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("config.json");
        Files.write(target, "existing".getBytes(StandardCharsets.UTF_8));
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(new FakeHttpClient(200, "[]"));

        assertThrows(HuggingFaceHubException.class, () -> client.downloadFile(new DownloadRequest(
                ModelReference.model("org/model"), "config.json", "main", target, null,
                OverwritePolicy.FAIL_IF_EXISTS, false, null, null)));
    }

    @Test
    void verifiesSizeMismatch(@TempDir Path dir) {
        FakeHttpClient http = new FakeHttpClient(200, "[]");
        http.downloadBody = "1234".getBytes(StandardCharsets.UTF_8);
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(http);
        Path target = dir.resolve("config.json");

        assertThrows(HuggingFaceHubException.class, () -> client.downloadFile(new DownloadRequest(
                ModelReference.model("org/model"), "config.json", "main", target, null,
                OverwritePolicy.OVERWRITE, false, null, Long.valueOf(999L))));
        assertFalse(Files.exists(target));
    }

    private static final class FakeHttpClient implements HubHttpClient {
        private final int status;
        private final String body;
        private HubHttpRequest lastRequest;
        private HubHttpRequest lastDownload;
        private int downloadStatus = 200;
        private byte[] downloadBody = new byte[0];
        private String downloadEtag;

        private FakeHttpClient(int status, String body) {
            this.status = status;
            this.body = body;
        }

        @Override
        public HubHttpResponse execute(HubHttpRequest request) throws IOException {
            this.lastRequest = request;
            return new HubHttpResponse(status, body.getBytes(StandardCharsets.UTF_8), "application/json");
        }

        @Override
        public HubHttpStream openStream(HubHttpRequest request, long rangeStart) {
            this.lastDownload = request;
            return new HubHttpStream(downloadStatus, downloadEtag, downloadBody.length, downloadBody.length,
                    "https://huggingface.co" + request.getPathAndQuery(), false,
                    new ByteArrayInputStream(downloadBody));
        }
    }
}
