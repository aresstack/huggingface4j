package com.aresstack.huggingface.hub.oauth;

public final class OAuthToken {

    private final String accessToken;
    private final String tokenType;
    private final Integer expiresInSeconds;
    private final String refreshToken;
    private final String scope;

    public OAuthToken(String accessToken, String tokenType, Integer expiresInSeconds, String refreshToken, String scope) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresInSeconds = expiresInSeconds;
        this.refreshToken = refreshToken;
        this.scope = scope;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Integer getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getScope() {
        return scope;
    }
}
