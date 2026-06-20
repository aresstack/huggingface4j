package com.aresstack.huggingface.hub.oauth;

public final class HuggingFaceOAuth {

    private HuggingFaceOAuth() {
    }

    public static OAuthStartRequest deviceCode() {
        return new OAuthStartRequest();
    }
}
