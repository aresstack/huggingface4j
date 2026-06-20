package com.aresstack.huggingface.hub.oauth;

import com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider;
import com.aresstack.huggingface.hub.http.HubHttpClient;
import com.aresstack.huggingface.hub.http.HubHttpRequest;
import com.aresstack.huggingface.hub.http.HubHttpResponse;
import com.aresstack.huggingface.hub.http.UrlConnectionHubHttpClient;
import com.aresstack.huggingface.hub.query.HubQueryParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class OAuthStartRequest {

    private String clientId;
    private String endpoint = "https://huggingface.co";
    private HubHttpClient httpClient;
    private final List<String> scopes = new ArrayList<String>();

    public OAuthStartRequest clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public OAuthStartRequest endpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public OAuthStartRequest httpClient(HubHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public OAuthStartRequest scope(OAuthScope scope) {
        if (scope != null) {
            scopes.add(scope.value());
        }
        return this;
    }

    public OAuthStartRequest scope(String scope) {
        if (scope != null && !scope.trim().isEmpty()) {
            scopes.add(scope.trim());
        }
        return this;
    }

    public OAuthLogin start() throws OAuthException {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalStateException("OAuth clientId must be set.");
        }
        HubHttpClient effectiveClient = httpClient == null
                ? new UrlConnectionHubHttpClient(endpoint, new HuggingFaceTokenProvider.Anonymous())
                : httpClient;
        HubQueryParameters form = new HubQueryParameters()
                .set("client_id", clientId)
                .set("scope", scopeString());
        try {
            HubHttpResponse response = effectiveClient.execute(HubHttpRequest.postForm("/oauth/device", form));
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                throw OAuthJson.parseError(response.getBodyAsUtf8());
            }
            return new OAuthLogin(effectiveClient, clientId, OAuthJson.parseStart(response.getBodyAsUtf8()));
        } catch (IOException exception) {
            throw new OAuthException("Failed to start Hugging Face OAuth login.", exception);
        }
    }

    private String scopeString() {
        StringBuilder builder = new StringBuilder();
        for (String scope : scopes) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(scope);
        }
        return builder.toString();
    }
}
