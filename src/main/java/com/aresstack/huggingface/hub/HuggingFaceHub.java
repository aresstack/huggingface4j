package com.aresstack.huggingface.hub;

import com.aresstack.huggingface.hub.account.AccountOperations;
import com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider;
import com.aresstack.huggingface.hub.http.HubHttpClient;
import com.aresstack.huggingface.hub.http.UrlConnectionHubHttpClient;
import com.aresstack.huggingface.hub.internal.DefaultHuggingFaceHubClient;
import com.aresstack.huggingface.hub.model.ModelOperations;

public final class HuggingFaceHub {

    private final HuggingFaceHubClient client;

    private HuggingFaceHub(HuggingFaceHubClient client) {
        this.client = client;
    }

    public static Builder standard() {
        return new Builder();
    }

    public ModelOperations models() {
        return new ModelOperations(client);
    }

    public AccountOperations account() {
        return new AccountOperations(client);
    }

    public static final class Builder {
        private HuggingFaceHubClient client;
        private String endpoint = "https://huggingface.co";
        private HuggingFaceTokenProvider tokenProvider = new HuggingFaceTokenProvider.Anonymous();

        public Builder client(HuggingFaceHubClient client) {
            this.client = client;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder anonymous() {
            this.tokenProvider = new HuggingFaceTokenProvider.Anonymous();
            return this;
        }

        public Builder accessToken(String token) {
            this.tokenProvider = new HuggingFaceTokenProvider.Static(token);
            return this;
        }

        public Builder environmentToken() {
            this.tokenProvider = new HuggingFaceTokenProvider.Environment("HF_TOKEN");
            return this;
        }

        public Builder tokenProvider(HuggingFaceTokenProvider tokenProvider) {
            this.tokenProvider = tokenProvider == null ? new HuggingFaceTokenProvider.Anonymous() : tokenProvider;
            return this;
        }

        public HuggingFaceHub build() {
            if (client == null) {
                HubHttpClient httpClient = new UrlConnectionHubHttpClient(endpoint, tokenProvider);
                client = new DefaultHuggingFaceHubClient(httpClient);
            }
            return new HuggingFaceHub(client);
        }
    }
}
