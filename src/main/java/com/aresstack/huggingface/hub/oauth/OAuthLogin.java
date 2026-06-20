package com.aresstack.huggingface.hub.oauth;

import com.aresstack.huggingface.hub.http.HubHttpClient;
import com.aresstack.huggingface.hub.http.HubHttpRequest;
import com.aresstack.huggingface.hub.http.HubHttpResponse;
import com.aresstack.huggingface.hub.query.HubQueryParameters;

import java.io.IOException;

public final class OAuthLogin {

    private static final String DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";

    private final HubHttpClient httpClient;
    private final String clientId;
    private final OAuthStart start;
    private Sleeper sleeper = new ThreadSleeper();

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

    public OAuthToken awaitToken() throws OAuthException {
        int interval = start.getIntervalSeconds() == null ? 5 : Math.max(1, start.getIntervalSeconds().intValue());
        while (true) {
            try {
                return pollToken();
            } catch (OAuthException exception) {
                String error = exception.getError();
                if ("authorization_pending".equals(error)) {
                    sleep(interval);
                    continue;
                }
                if ("slow_down".equals(error)) {
                    interval += 5;
                    sleep(interval);
                    continue;
                }
                throw exception;
            }
        }
    }

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

    private static final class ThreadSleeper implements Sleeper {
        @Override
        public void sleep(long milliseconds) throws InterruptedException {
            Thread.sleep(milliseconds);
        }
    }
}
