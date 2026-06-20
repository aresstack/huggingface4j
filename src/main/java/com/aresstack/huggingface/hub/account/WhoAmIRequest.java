package com.aresstack.huggingface.hub.account;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;

public final class WhoAmIRequest {

    private final HuggingFaceHubClient client;

    WhoAmIRequest(HuggingFaceHubClient client) {
        this.client = client;
    }

    public UserProfile execute() throws HuggingFaceHubException {
        return client.whoAmI();
    }
}
