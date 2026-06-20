package com.aresstack.huggingface.hub;

/**
 * Represent failures that occur while talking to the Hugging Face Hub.
 */
public class HuggingFaceHubException extends Exception {

    public HuggingFaceHubException(String message) {
        super(message);
    }

    public HuggingFaceHubException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Map an HTTP status code and body to the most specific {@link Response} subtype.
     * Returns {@code null} for successful (2xx) responses.
     */
    public static Response forStatus(int statusCode, String body) {
        if (statusCode >= 200 && statusCode < 300) {
            return null;
        }
        switch (statusCode) {
            case 401:
                return new Unauthorized(body);
            case 403:
                return new Forbidden(body);
            case 404:
                return new NotFound(body);
            case 429:
                return new RateLimited(body);
            default:
                return new Response(statusCode, "Hugging Face Hub returned HTTP " + statusCode + ".", body);
        }
    }

    public static class Response extends HuggingFaceHubException {

        private final int statusCode;
        private final String responseBody;

        public Response(int statusCode, String message, String responseBody) {
            super(message);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }

    public static final class Unauthorized extends Response {
        public Unauthorized(String responseBody) {
            super(401, "Hugging Face authentication is required or the token is invalid.", responseBody);
        }
    }

    public static final class Forbidden extends Response {
        public Forbidden(String responseBody) {
            super(403, "The authenticated Hugging Face user is not allowed to access this resource.", responseBody);
        }
    }

    public static final class NotFound extends Response {
        public NotFound(String responseBody) {
            super(404, "The requested Hugging Face resource was not found.", responseBody);
        }
    }

    public static final class RateLimited extends Response {
        public RateLimited(String responseBody) {
            super(429, "The Hugging Face Hub rate limit was exceeded.", responseBody);
        }
    }
}
