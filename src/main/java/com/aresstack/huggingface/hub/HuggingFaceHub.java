package com.aresstack.huggingface.hub;

public final class HuggingFaceHub {

    private final HuggingFaceHubClient client;

    private HuggingFaceHub(HuggingFaceHubClient client) {
        this.client = client;
    }

    public static Builder standard() {
        return new Builder();
    }

    public Models models() {
        return new Models(client);
    }

    public Account account() {
        return new Account(client);
    }

    public static final class Builder {
        private HuggingFaceHubClient client;
        private String endpoint = "https://huggingface.co";
        private com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider tokenProvider = new com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider.Anonymous();

        public Builder client(HuggingFaceHubClient client) {
            this.client = client;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder anonymous() {
            this.tokenProvider = new com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider.Anonymous();
            return this;
        }

        public Builder accessToken(String token) {
            this.tokenProvider = new com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider.Static(token);
            return this;
        }

        public Builder environmentToken() {
            this.tokenProvider = new com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider.Environment("HF_TOKEN");
            return this;
        }

        public HuggingFaceHub build() {
            if (client == null) {
                com.aresstack.huggingface.hub.http.HubHttpClient httpClient = new com.aresstack.huggingface.hub.http.UrlConnectionHubHttpClient(endpoint, tokenProvider);
                client = new com.aresstack.huggingface.hub.internal.DefaultHuggingFaceHubClient(httpClient);
            }
            return new HuggingFaceHub(client);
        }
    }

    public static final class Account {
        private final HuggingFaceHubClient client;

        private Account(HuggingFaceHubClient client) {
            this.client = client;
        }

        public WhoAmI whoAmI() {
            return new WhoAmI(client);
        }
    }

    public static final class WhoAmI {
        private final HuggingFaceHubClient client;

        private WhoAmI(HuggingFaceHubClient client) {
            this.client = client;
        }

        public com.aresstack.huggingface.hub.account.UserProfile execute() throws HuggingFaceHubException {
            return client.whoAmI();
        }
    }

    public static final class Models {
        private final HuggingFaceHubClient client;

        private Models(HuggingFaceHubClient client) {
            this.client = client;
        }

        public ModelSearch search(String text) {
            return new ModelSearch(client, text);
        }

        public ModelResource model(String id) {
            return new ModelResource(client, id);
        }
    }

    public static final class ModelSearch {
        private final HuggingFaceHubClient client;
        private final com.aresstack.huggingface.hub.query.HubQueryParameters parameters = new com.aresstack.huggingface.hub.query.HubQueryParameters();

        private ModelSearch(HuggingFaceHubClient client, String text) {
            this.client = client;
            parameters.set("search", text);
            parameters.setNumber("limit", Integer.valueOf(20));
        }

        public ModelSearch task(String task) {
            parameters.set("pipeline_tag", task);
            return this;
        }

        public ModelSearch library(String library) {
            parameters.set("library", library);
            return this;
        }

        public ModelSearch sortByDownloads() {
            parameters.set("sort", "downloads");
            return this;
        }

        public ModelSearch limit(int limit) {
            parameters.setNumber("limit", Integer.valueOf(limit));
            return this;
        }

        public com.aresstack.huggingface.hub.model.ModelSearchResult execute() throws HuggingFaceHubException {
            return client.searchModels(parameters);
        }
    }

    public static final class ModelResource {
        private final HuggingFaceHubClient client;
        private final String id;

        private ModelResource(HuggingFaceHubClient client, String id) {
            this.client = client;
            this.id = id;
        }

        public Details details() {
            return new Details(client, id);
        }

        public com.aresstack.huggingface.hub.model.ModelDetails execute() throws HuggingFaceHubException {
            return details().execute();
        }
    }

    public static final class Details {
        private final HuggingFaceHubClient client;
        private final com.aresstack.huggingface.hub.model.ModelDetailsQuery query;

        private Details(HuggingFaceHubClient client, String id) {
            this.client = client;
            this.query = new com.aresstack.huggingface.hub.model.ModelDetailsQuery(com.aresstack.huggingface.hub.model.ModelReference.model(id));
        }

        public Details revision(String revision) {
            query.revision(revision);
            return this;
        }

        public Details includeFiles() {
            query.include("siblings");
            return this;
        }

        public Details includeConfig() {
            query.include("config");
            return this;
        }

        public com.aresstack.huggingface.hub.model.ModelDetails execute() throws HuggingFaceHubException {
            return client.getModelDetails(query);
        }
    }
}
