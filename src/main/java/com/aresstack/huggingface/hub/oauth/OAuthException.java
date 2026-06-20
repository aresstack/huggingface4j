package com.aresstack.huggingface.hub.oauth;

import com.aresstack.huggingface.hub.HuggingFaceHubException;

/**
 * Represent a failure during the Hugging Face OAuth device login flow.
 *
 * <p>When the failure carries a machine readable OAuth {@code error} code it is exposed through
 * {@link #getError()} together with the optional human readable {@link #getErrorDescription()}.
 * The well known device-flow error codes are available as constants and through the boolean helper
 * methods so callers can react without string comparisons.</p>
 */
public class OAuthException extends HuggingFaceHubException {

    /** The user has not yet approved the request; the client should keep polling. */
    public static final String AUTHORIZATION_PENDING = "authorization_pending";
    /** The client is polling too quickly and should increase the interval. */
    public static final String SLOW_DOWN = "slow_down";
    /** The device code expired before the user approved the request. */
    public static final String EXPIRED_TOKEN = "expired_token";
    /** The user (or authorization server) denied the request. */
    public static final String ACCESS_DENIED = "access_denied";
    /** The supplied device code or grant is no longer valid. */
    public static final String INVALID_GRANT = "invalid_grant";
    /** The client id is unknown or otherwise rejected. */
    public static final String INVALID_CLIENT = "invalid_client";

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

    public boolean isAuthorizationPending() {
        return AUTHORIZATION_PENDING.equals(error);
    }

    public boolean isSlowDown() {
        return SLOW_DOWN.equals(error);
    }

    public boolean isExpiredToken() {
        return EXPIRED_TOKEN.equals(error);
    }

    public boolean isAccessDenied() {
        return ACCESS_DENIED.equals(error);
    }

    public boolean isInvalidGrant() {
        return INVALID_GRANT.equals(error);
    }

    public boolean isInvalidClient() {
        return INVALID_CLIENT.equals(error);
    }

    /**
     * Indicate whether the device login was cancelled locally (deadline reached or
     * {@link OAuthLogin#cancel()} called) rather than rejected by the server.
     */
    public boolean isCancelled() {
        return false;
    }

    /** Raised when {@link OAuthLogin#awaitToken()} is stopped before a token is granted. */
    public static final class Cancelled extends OAuthException {
        public Cancelled(String message) {
            super(message, (String) null, null);
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    }
}
