package com.aresstack.huggingface.hub.internal;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.http.HubHttpClient;
import com.aresstack.huggingface.hub.http.HubHttpRequest;
import com.aresstack.huggingface.hub.http.HubHttpResponse;
import com.aresstack.huggingface.hub.model.HubFile;
import com.aresstack.huggingface.hub.model.ModelDetails;
import com.aresstack.huggingface.hub.model.ModelDetailsQuery;
import com.aresstack.huggingface.hub.model.ModelSearchResult;
import com.aresstack.huggingface.hub.query.HubQueryParameters;
import com.aresstack.huggingface.hub.query.HubUrlBuilder;

import java.io.IOException;
import java.util.List;

public final class DefaultHuggingFaceHubClient implements HuggingFaceHubClient {

    private final HubHttpClient httpClient;
    private final HubJsonMapper mapper;

    public DefaultHuggingFaceHubClient(HubHttpClient httpClient) {
        this.httpClient = httpClient;
        this.mapper = new HubJsonMapper();
    }

    @Override
    public com.aresstack.huggingface.hub.account.UserProfile whoAmI() throws HuggingFaceHubException {
        HubHttpResponse response = execute(HubHttpRequest.get("/api/whoami-v2"));
        requireSuccess(response);
        return mapper.toUserProfile(response.getBodyAsUtf8());
    }

    @Override
    public ModelSearchResult searchModels(HubQueryParameters parameters) throws HuggingFaceHubException {
        HubHttpResponse response = execute(HubHttpRequest.get(HubUrlBuilder.appendQuery("/api/models", parameters)));
        requireSuccess(response);
        return mapper.toModelSearchResult(response.getBodyAsUtf8());
    }

    @Override
    public ModelDetails getModelDetails(ModelDetailsQuery query) throws HuggingFaceHubException {
        String path = "/api/models" + HubUrlBuilder.path(query.getModelReference().getRepoId());
        path = HubUrlBuilder.appendQuery(path, query.toQueryParameters());
        HubHttpResponse response = execute(HubHttpRequest.get(path));
        requireSuccess(response);
        return mapper.toModelDetails(response.getBodyAsUtf8());
    }

    @Override
    public List<HubFile> listModelFiles(ModelDetailsQuery query) throws HuggingFaceHubException {
        return getModelDetails(query).getFiles();
    }

    @Override
    public com.aresstack.huggingface.hub.download.DownloadResult downloadFile(com.aresstack.huggingface.hub.download.DownloadRequest request) throws HuggingFaceHubException {
        String path = "/" + request.getModelReference().getRepoId() + "/resolve/" + request.getRevision() + "/" + request.getFilePath();
        HubHttpRequest httpRequest = HubHttpRequest.get(HubUrlBuilder.path(path));
        return new com.aresstack.huggingface.hub.download.FileDownloader(httpClient).download(httpRequest, request);
    }

    private HubHttpResponse execute(HubHttpRequest request) throws HuggingFaceHubException {
        try {
            return httpClient.execute(request);
        } catch (IOException exception) {
            throw new HuggingFaceHubException("Failed to communicate with the Hugging Face Hub.", exception);
        }
    }

    private static void requireSuccess(HubHttpResponse response) throws HuggingFaceHubException.Response {
        HuggingFaceHubException.Response failure = HuggingFaceHubException.forStatus(response.getStatusCode(), response.getBodyAsUtf8());
        if (failure != null) {
            throw failure;
        }
    }
}
