package com.aresstack.huggingface.hub;

import com.aresstack.huggingface.hub.account.UserProfile;
import com.aresstack.huggingface.hub.download.DownloadRequest;
import com.aresstack.huggingface.hub.download.DownloadResult;
import com.aresstack.huggingface.hub.model.ModelDetails;
import com.aresstack.huggingface.hub.model.ModelDetailsQuery;
import com.aresstack.huggingface.hub.model.ModelSearchResult;
import com.aresstack.huggingface.hub.query.HubQueryParameters;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HuggingFaceHubFluentApiTest {

    @Test
    void delegatesSearchParameters() throws Exception {
        RecordingClient client = new RecordingClient();
        HuggingFaceHub hub = HuggingFaceHub.standard().client(client).build();

        hub.models().search("qwen coder").task("text-generation").library("transformers").sortByDownloads().limit(10).execute();

        assertEquals("search=qwen%20coder&pipeline_tag=text-generation&library=transformers&sort=downloads&limit=10", client.parameters.toQueryString());
    }

    @Test
    void delegatesDetailsQuery() throws Exception {
        RecordingClient client = new RecordingClient();
        HuggingFaceHub hub = HuggingFaceHub.standard().client(client).build();

        hub.models().model("Qwen/Qwen2.5-Coder-0.5B-Instruct").details().includeFiles().includeConfig().execute();

        assertEquals("Qwen/Qwen2.5-Coder-0.5B-Instruct", client.detailsQuery.getModelReference().getRepoId());
        assertEquals("expand=siblings&expand=config", client.detailsQuery.toQueryParameters().toQueryString());
    }

    @Test
    void delegatesDownloadRequest() throws Exception {
        RecordingClient client = new RecordingClient();
        HuggingFaceHub hub = HuggingFaceHub.standard().client(client).build();

        hub.models().model("org/model").file("model.safetensors").revision("abc").downloadTo(Paths.get("target.bin")).execute();

        assertEquals("org/model", client.downloadRequest.getModelReference().getRepoId());
        assertEquals("model.safetensors", client.downloadRequest.getFilePath());
        assertEquals("abc", client.downloadRequest.getRevision());
    }

    private static final class RecordingClient implements HuggingFaceHubClient {
        private HubQueryParameters parameters;
        private ModelDetailsQuery detailsQuery;
        private DownloadRequest downloadRequest;

        @Override
        public UserProfile whoAmI() {
            return new UserProfile("tester", null, "user");
        }

        @Override
        public ModelSearchResult searchModels(HubQueryParameters parameters) {
            this.parameters = parameters;
            return new ModelSearchResult(Collections.emptyList());
        }

        @Override
        public ModelDetails getModelDetails(ModelDetailsQuery query) {
            this.detailsQuery = query;
            return new ModelDetails(null, null, Collections.emptyList());
        }

        @Override
        public java.util.List<com.aresstack.huggingface.hub.model.HubFile> listModelFiles(ModelDetailsQuery query) {
            this.detailsQuery = query;
            return Collections.emptyList();
        }

        @Override
        public DownloadResult downloadFile(DownloadRequest request) {
            this.downloadRequest = request;
            return new DownloadResult(request.getTargetFile(), 0L);
        }
    }
}
