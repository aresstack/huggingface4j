package com.aresstack.huggingface.hub.oauth;

import com.aresstack.huggingface.hub.http.HubHttpClient;
import com.aresstack.huggingface.hub.http.HubHttpRequest;
import com.aresstack.huggingface.hub.http.HubHttpResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OAuthLoginTest {

    @Test
    void startsOAuthLoginWithScopes() throws Exception {
        FakeHttpClient http = new FakeHttpClient();
        http.enqueue(200, "{\"device_code\":\"device-1\",\"user_code\":\"ABCD\",\"verification_uri\":\"https://huggingface.co/activate\",\"expires_in\":600,\"interval\":1}");

        OAuthLogin login = HuggingFaceOAuth.deviceCode()
                .clientId("askai")
                .scope(OAuthScope.OPENID)
                .scope(OAuthScope.PROFILE)
                .scope(OAuthScope.GATED_REPOS)
                .httpClient(http)
                .start();

        assertEquals("POST", http.lastRequest.getMethod());
        assertEquals("/oauth/device", http.lastRequest.getPathAndQuery());
        assertEquals("ABCD", login.getUserCode());
        assertTrue(new String(http.lastRequest.getBody(), "UTF-8").contains("client_id=askai"));
        assertTrue(new String(http.lastRequest.getBody(), "UTF-8").contains("scope=openid%20profile%20gated-repos"));
    }

    @Test
    void pollsOAuthToken() throws Exception {
        FakeHttpClient http = new FakeHttpClient();
        http.enqueue(200, "{\"device_code\":\"device-1\",\"user_code\":\"ABCD\",\"verification_uri\":\"https://huggingface.co/activate\",\"interval\":1}");
        http.enqueue(200, "{\"access_token\":\"hf_token\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"scope\":\"openid profile\"}");

        OAuthLogin login = HuggingFaceOAuth.deviceCode().clientId("askai").httpClient(http).start();
        OAuthToken token = login.pollToken();

        assertEquals("hf_token", token.getAccessToken());
        assertEquals("Bearer", token.getTokenType());
        assertEquals(Integer.valueOf(3600), token.getExpiresInSeconds());
        assertEquals("/oauth/token", http.lastRequest.getPathAndQuery());
        assertTrue(new String(http.lastRequest.getBody(), "UTF-8").contains("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Adevice_code"));
        assertTrue(new String(http.lastRequest.getBody(), "UTF-8").contains("device_code=device-1"));
    }

    @Test
    void waitsWhileAuthorizationIsPending() throws Exception {
        FakeHttpClient http = new FakeHttpClient();
        http.enqueue(200, "{\"device_code\":\"device-1\",\"user_code\":\"ABCD\",\"verification_uri\":\"https://huggingface.co/activate\",\"interval\":1}");
        http.enqueue(400, "{\"error\":\"authorization_pending\"}");
        http.enqueue(200, "{\"access_token\":\"hf_token\"}");

        OAuthLogin login = HuggingFaceOAuth.deviceCode().clientId("askai").httpClient(http).start();
        login.sleeper(new OAuthLogin.Sleeper() {
            @Override
            public void sleep(long milliseconds) {
            }
        });

        assertEquals("hf_token", login.awaitToken().getAccessToken());
    }

    private static final class FakeHttpClient implements HubHttpClient {
        private final Queue<HubHttpResponse> responses = new ArrayDeque<HubHttpResponse>();
        private HubHttpRequest lastRequest;

        void enqueue(int statusCode, String body) throws IOException {
            responses.add(new HubHttpResponse(statusCode, body.getBytes("UTF-8"), "application/json"));
        }

        @Override
        public HubHttpResponse execute(HubHttpRequest request) {
            this.lastRequest = request;
            return responses.remove();
        }

        @Override
        public long download(HubHttpRequest request, Path targetFile, com.aresstack.huggingface.hub.download.ProgressListener progressListener) {
            return 0L;
        }
    }
}
