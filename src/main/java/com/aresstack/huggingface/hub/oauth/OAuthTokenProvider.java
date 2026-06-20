package com.aresstack.huggingface.hub.oauth;

import com.aresstack.huggingface.hub.auth.HuggingFaceTokenProvider;

public final class OAuthTokenProvider implements HuggingFaceTokenProvider {

    private final OAuthToken token;

    public OAuthTokenProvider(OAuthToken token) {
        this.token = token;
    }

    @Override
    public String getAccessToken() {
        return token == null ? null : token.getAccessToken();
    }
}
