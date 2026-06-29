package com.aresstack.huggingface.hub.account;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;

/**
 * Fluent entry point for account operations, returned by {@code HuggingFaceHub.account()}.
 */
public final class AccountOperations {

    private final HuggingFaceHubClient client;

    public AccountOperations(HuggingFaceHubClient client) {
        this.client = client;
    }

    /** Look up the currently authenticated user ({@code GET /api/whoami-v2}). */
    public WhoAmIRequest whoAmI() {
        return new WhoAmIRequest(client);
    }
}
