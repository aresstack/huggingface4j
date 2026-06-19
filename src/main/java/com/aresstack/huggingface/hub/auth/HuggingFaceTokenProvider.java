package com.aresstack.huggingface.hub.auth;

/**
 * Provide the current access token, or null for anonymous requests.
 */
public interface HuggingFaceTokenProvider {

    String getAccessToken();

    final class Anonymous implements HuggingFaceTokenProvider {
        @Override
        public String getAccessToken() {
            return null;
        }
    }

    final class Static implements HuggingFaceTokenProvider {
        private final String accessToken;

        public Static(String accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public String getAccessToken() {
            return accessToken;
        }
    }

    final class Environment implements HuggingFaceTokenProvider {
        private final String environmentVariableName;

        public Environment(String environmentVariableName) {
            this.environmentVariableName = environmentVariableName;
        }

        @Override
        public String getAccessToken() {
            return System.getenv(environmentVariableName);
        }
    }
}
