package com.aresstack.huggingface.hub;

import com.aresstack.huggingface.hub.account.AccountOperations;
import com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider;
import com.aresstack.huggingface.hub.http.HubHttpClient;
import com.aresstack.huggingface.hub.http.UrlConnectionHubHttpClient;
import com.aresstack.huggingface.hub.internal.DefaultHuggingFaceHubClient;
import com.aresstack.huggingface.hub.model.ModelOperations;
import com.aresstack.huggingface.hub.repo.RepositoryOperations;

/**
 * The main entry point of huggingface4j.
 *
 * <p>Create an instance with {@link #standard()} and one of the authentication methods on the
 * {@link Builder}, then reach the fluent operation groups: {@link #models()} for search, details,
 * file listing and downloads; {@link #account()} for the authenticated user; and
 * {@link #repositories()} for repository management and uploads.</p>
 *
 * <pre>{@code
 * HuggingFaceHub hub = HuggingFaceHub.standard().accessToken("hf_...").build();
 * ModelSearchResult result = hub.models().search("qwen").limit(10).execute();
 * }</pre>
 *
 * <p>Instances are immutable and safe to share between threads. Classes in the
 * {@code com.aresstack.huggingface.hub.internal} package are implementation details and not part of
 * the public API.</p>
 */
public final class HuggingFaceHub {

    private final HuggingFaceHubClient client;

    private HuggingFaceHub(HuggingFaceHubClient client) {
        this.client = client;
    }

    /** Begin configuring a client against the standard Hugging Face Hub endpoint. */
    public static Builder standard() {
        return new Builder();
    }

    /** Model search, details, file listing and downloads. */
    public ModelOperations models() {
        return new ModelOperations(client);
    }

    /** Operations about the authenticated account, such as {@code whoAmI}. */
    public AccountOperations account() {
        return new AccountOperations(client);
    }

    /** Repository management and write operations (create/delete/settings, upload/commit). */
    public RepositoryOperations repositories() {
        return new RepositoryOperations(client);
    }

    /** Configures and builds a {@link HuggingFaceHub}. */
    public static final class Builder {
        private HuggingFaceHubClient client;
        private String endpoint = "https://huggingface.co";
        private HuggingFaceTokenProvider tokenProvider = new HuggingFaceTokenProvider.Anonymous();
        private java.net.Proxy proxy;
        private java.net.ProxySelector proxySelector;

        /** Use a custom client implementation; mainly for testing. Overrides endpoint/token settings. */
        public Builder client(HuggingFaceHubClient client) {
            this.client = client;
            return this;
        }

        /** Override the base endpoint (defaults to {@code https://huggingface.co}). */
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /** Make unauthenticated, public read-only requests. */
        public Builder anonymous() {
            this.tokenProvider = new HuggingFaceTokenProvider.Anonymous();
            return this;
        }

        /** Authenticate with a static {@code hf_...} access token. */
        public Builder accessToken(String token) {
            this.tokenProvider = new HuggingFaceTokenProvider.Static(token);
            return this;
        }

        /** Authenticate with the token from the {@code HF_TOKEN} environment variable. */
        public Builder environmentToken() {
            this.tokenProvider = new HuggingFaceTokenProvider.Environment("HF_TOKEN");
            return this;
        }

        /** Authenticate with a custom token provider (for example an OAuth or refreshing source). */
        public Builder tokenProvider(HuggingFaceTokenProvider tokenProvider) {
            this.tokenProvider = tokenProvider == null ? new HuggingFaceTokenProvider.Anonymous() : tokenProvider;
            return this;
        }

        /**
         * Route all HTTP traffic (API, redirects, downloads and uploads) through a fixed proxy.
         * Takes precedence over {@link #proxySelector(java.net.ProxySelector)}.
         */
        public Builder proxy(java.net.Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        /**
         * Consult a {@link java.net.ProxySelector} per request URL (for example a PAC- or
         * Windows-settings-backed selector). Ignored when a fixed {@link #proxy(java.net.Proxy)} is set.
         */
        public Builder proxySelector(java.net.ProxySelector proxySelector) {
            this.proxySelector = proxySelector;
            return this;
        }

        /** Build the configured {@link HuggingFaceHub}. */
        public HuggingFaceHub build() {
            if (client == null) {
                HubHttpClient httpClient = new UrlConnectionHubHttpClient(endpoint, tokenProvider, proxy, proxySelector);
                client = new DefaultHuggingFaceHubClient(httpClient);
            }
            return new HuggingFaceHub(client);
        }
    }
}
