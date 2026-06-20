package com.aresstack.huggingface.hub.account;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;

public final class AccountOperations {

    private final HuggingFaceHubClient client;

    public AccountOperations(HuggingFaceHubClient client) {
        this.client = client;
    }

    public WhoAmIRequest whoAmI() {
        return new WhoAmIRequest(client);
    }
}
