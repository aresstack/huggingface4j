package com.aresstack.huggingface.hub.oauth;

import com.aresstack.huggingface.hub.http.HubHttpClient;
import com.aresstack.huggingface.hub.http.HubHttpRequest;
import com.aresstack.huggingface.hub.http.HubHttpResponse;
import com.aresstack.huggingface.hub.query.HubQueryParameters;

import java.io.IOException;

/**
 * Drive a single Hugging Face OAuth device-code login.
 *
 * <p>After the device code has been issued, the user opens {@link #getVerificationUriComplete()}
 * (or {@link #getVerificationUri()} and enters {@link #getUserCode()}). The application then waits
 * for approval with {@link #awaitToken()} or polls manually with {@link #pollToken()}.</p>
 *
 * <p>{@link #awaitToken()} is bounded: it never polls forever. It stops as soon as the device code
 * expires, when an optional caller supplied timeout elapses, or when {@link #cancel()} is invoked
 * from another thread. Terminal server errors such as {@code access_denied} or {@code expired_token}
 * are surfaced as {@link OAuthException} with the corresponding {@link OAuthException#getError()}.</p>
 */
public final class OAuthLogin {

    private static final String DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";
    private static final int DEFAULT_INTERVAL_SECONDS = 5;
    private static final int DEFAULT_EXPIRY_SECONDS = 600;

    private final HubHttpClient httpClient;
    private final String clientId;
    private final OAuthStart start;
    private volatile boolean cancelled;
    private Sleeper sleeper = new ThreadSleeper();
    private Clock clock = new SystemClock();

    OAuthLogin(HubHttpClient httpClient, String clientId, OAuthStart start) {
        this.httpClient = httpClient;
        this.clientId = clientId;
        this.start = start;
    }

    public String getUserCode() {
        return start.getUserCode();
    }

    public String getVerificationUri() {
        return start.getVerificationUri();
    }

    public String getVerificationUriComplete() {
        return start.getVerificationUriComplete();
    }

    public Integer getExpiresInSeconds() {
        return start.getExpiresInSeconds();
    }

    public Integer getIntervalSeconds() {
        return start.getIntervalSeconds();
    }

    /**
     * Wait for the user to approve the login, bounded by the device code expiry.
     */
    public OAuthToken awaitToken() throws OAuthException {
        return awaitToken(0L);
    }

    /**
     * Wait for the user to approve the login.
     *
     * @param timeoutMillis the maximum time to wait, or {@code 0} (or negative) to wait only until
     *                      the device code expires. The effective deadline is always the earlier of
     *                      this timeout and the device code expiry.
     * @throws OAuthException.Cancelled if the deadline is reached or {@link #cancel()} was called
     * @throws OAuthException           if the server rejects the request (for example
     *                                  {@code access_denied}, {@code expired_token},
     *                                  {@code invalid_grant} or {@code invalid_client})
     */
    public OAuthToken awaitToken(long timeoutMillis) throws OAuthException {
        int interval = baseIntervalSeconds();
        long now = clock.nowMillis();
        long deadline = now + expirySeconds() * 1000L;
        if (timeoutMillis > 0L) {
            deadline = Math.min(deadline, now + timeoutMillis);
        }
        while (true) {
            ensureNotCancelled();
            if (clock.nowMillis() >= deadline) {
                throw new OAuthException.Cancelled("Timed out waiting for Hugging Face OAuth authorization.");
            }
            try {
                return pollToken();
            } catch (OAuthException exception) {
                if (exception.isAuthorizationPending()) {
                    sleep(interval);
                    continue;
                }
                if (exception.isSlowDown()) {
                    interval += 5;
                    sleep(interval);
                    continue;
                }
                // expired_token, access_denied, invalid_grant, invalid_client and anything else are terminal.
                throw exception;
            }
        }
    }

    /**
     * Request that an in-progress {@link #awaitToken()} stops at the next opportunity. Safe to call
     * from another thread.
     */
    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Perform a single token poll. Returns the token on success, otherwise throws an
     * {@link OAuthException} carrying the OAuth error code (for example {@code authorization_pending}).
     */
    public OAuthToken pollToken() throws OAuthException {
        HubQueryParameters form = new HubQueryParameters()
                .set("grant_type", DEVICE_GRANT)
                .set("device_code", start.getCode())
                .set("client_id", clientId);
        try {
            HubHttpResponse response = httpClient.execute(HubHttpRequest.postForm("/oauth/token", form));
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                throw OAuthJson.parseError(response.getBodyAsUtf8());
            }
            return OAuthJson.parseToken(response.getBodyAsUtf8());
        } catch (IOException exception) {
            throw new OAuthException("Failed to poll Hugging Face OAuth token.", exception);
        }
    }

    OAuthLogin sleeper(Sleeper sleeper) {
        this.sleeper = sleeper == null ? new ThreadSleeper() : sleeper;
        return this;
    }

    OAuthLogin clock(Clock clock) {
        this.clock = clock == null ? new SystemClock() : clock;
        return this;
    }

    private int baseIntervalSeconds() {
        return start.getIntervalSeconds() == null ? DEFAULT_INTERVAL_SECONDS : Math.max(1, start.getIntervalSeconds().intValue());
    }

    private int expirySeconds() {
        Integer expires = start.getExpiresInSeconds();
        return expires == null || expires.intValue() <= 0 ? DEFAULT_EXPIRY_SECONDS : expires.intValue();
    }

    private void ensureNotCancelled() throws OAuthException {
        if (cancelled) {
            throw new OAuthException.Cancelled("Hugging Face OAuth login was cancelled.");
        }
    }

    private void sleep(int seconds) throws OAuthException {
        try {
            sleeper.sleep(seconds * 1000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OAuthException("Interrupted while waiting for Hugging Face OAuth authorization.", exception);
        }
    }

    interface Sleeper {
        void sleep(long milliseconds) throws InterruptedException;
    }

    interface Clock {
        long nowMillis();
    }

    private static final class ThreadSleeper implements Sleeper {
        @Override
        public void sleep(long milliseconds) throws InterruptedException {
            Thread.sleep(milliseconds);
        }
    }

    private static final class SystemClock implements Clock {
        @Override
        public long nowMillis() {
            return System.currentTimeMillis();
        }
    }
}
