package com.aresstack.huggingface.hub.oauth;

public enum OAuthScope {

    OPENID("openid"),
    PROFILE("profile"),
    EMAIL("email"),
    GATED_REPOS("gated-repos"),
    READ_REPOS("read-repos"),
    WRITE_REPOS("write-repos");

    private final String value;

    OAuthScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
