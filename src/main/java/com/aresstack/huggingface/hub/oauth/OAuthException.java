package com.aresstack.huggingface.hub.oauth;

import com.aresstack.huggingface.hub.HuggingFaceHubException;

public class OAuthException extends HuggingFaceHubException {

    private final String error;
    private final String errorDescription;

    public OAuthException(String message, String error, String errorDescription) {
        super(message);
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public OAuthException(String message, Throwable cause) {
        super(message, cause);
        this.error = null;
        this.errorDescription = null;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}
