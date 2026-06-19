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

    public static final class Builder {
        private HuggingFaceHubClient client;

        public Builder client(HuggingFaceHubClient client) {
            this.client = client;
            return this;
        }

        public Builder anonymous() {
            return this;
        }

        public HuggingFaceHub build() {
            if (client == null) {
                client = new com.aresstack.huggingface.hub.internal.DefaultHuggingFaceHubClient();
            }
            return new HuggingFaceHub(client);
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

        public com.aresstack.huggingface.hub.model.ModelDetails execute() throws HuggingFaceHubException {
            return client.getModelDetails(new com.aresstack.huggingface.hub.model.ModelDetailsQuery(com.aresstack.huggingface.hub.model.ModelReference.model(id)));
        }
    }
}
