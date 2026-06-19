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

    private HubHttpResponse execute(HubHttpRequest request) throws HuggingFaceHubException {
        try {
            return httpClient.execute(request);
        } catch (IOException exception) {
            throw new HuggingFaceHubException("Failed to communicate with the Hugging Face Hub.", exception);
        }
    }

    private static void requireSuccess(HubHttpResponse response) throws HuggingFaceHubException.Response {
        int statusCode = response.getStatusCode();
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        String body = response.getBodyAsUtf8();
        if (statusCode == 401) {
            throw new HuggingFaceHubException.Unauthorized(body);
        }
        if (statusCode == 403) {
            throw new HuggingFaceHubException.Forbidden(body);
        }
        if (statusCode == 404) {
            throw new HuggingFaceHubException.NotFound(body);
        }
        if (statusCode == 429) {
            throw new HuggingFaceHubException.RateLimited(body);
        }
        throw new HuggingFaceHubException.Response(statusCode, "Hugging Face Hub returned HTTP " + statusCode + ".", body);
    }
}
