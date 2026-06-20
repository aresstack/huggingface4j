package com.aresstack.huggingface.hub.oauth;

public final class OAuthStart {

    private final String code;
    private final String userCode;
    private final String verificationUri;
    private final String verificationUriComplete;
    private final Integer expiresInSeconds;
    private final Integer intervalSeconds;

    public OAuthStart(String code, String userCode, String verificationUri,
                      String verificationUriComplete, Integer expiresInSeconds, Integer intervalSeconds) {
        this.code = code;
        this.userCode = userCode;
        this.verificationUri = verificationUri;
        this.verificationUriComplete = verificationUriComplete;
        this.expiresInSeconds = expiresInSeconds;
        this.intervalSeconds = intervalSeconds;
    }

    public String getCode() {
        return code;
    }

    public String getUserCode() {
        return userCode;
    }

    public String getVerificationUri() {
        return verificationUri;
    }

    public String getVerificationUriComplete() {
        return verificationUriComplete;
    }

    public Integer getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }
}
