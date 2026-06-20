package com.aresstack.huggingface.hub.internal;

import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.download.DownloadRequest;
import com.aresstack.huggingface.hub.http.HubHttpClient;
import com.aresstack.huggingface.hub.http.HubHttpRequest;
import com.aresstack.huggingface.hub.http.HubHttpResponse;
import com.aresstack.huggingface.hub.model.ModelDetails;
import com.aresstack.huggingface.hub.model.ModelDetailsQuery;
import com.aresstack.huggingface.hub.model.ModelReference;
import com.aresstack.huggingface.hub.model.ModelSearchResult;
import com.aresstack.huggingface.hub.query.HubQueryParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        FakeHttpClient http = new FakeHttpClient(401, "nope");
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(http);

        assertThrows(HuggingFaceHubException.Unauthorized.class,
                () -> client.searchModels(new HubQueryParameters().set("search", "private")));
    }

    @Test
    void delegatesDownloadEndpoint() throws Exception {
        FakeHttpClient http = new FakeHttpClient(200, "{}");
        DefaultHuggingFaceHubClient client = new DefaultHuggingFaceHubClient(http);

        client.downloadFile(new DownloadRequest(ModelReference.model("org/model"), "config.json", "abc", Paths.get("target.bin"), null));

        assertEquals("/org/model/resolve/abc/config.json", http.lastDownload.getPathAndQuery());
    }

    private static final class FakeHttpClient implements HubHttpClient {
        private final int status;
        private final String body;
        private HubHttpRequest lastRequest;
        private HubHttpRequest lastDownload;

        private FakeHttpClient(int status, String body) {
            this.status = status;
            this.body = body;
        }

        @Override
        public HubHttpResponse execute(HubHttpRequest request) throws IOException {
            this.lastRequest = request;
            return new HubHttpResponse(status, body.getBytes("UTF-8"), "application/json");
        }

        @Override
        public long download(HubHttpRequest request, java.nio.file.Path targetFile, com.aresstack.huggingface.hub.download.ProgressListener progressListener) {
            this.lastDownload = request;
            return 123L;
        }
    }
}
