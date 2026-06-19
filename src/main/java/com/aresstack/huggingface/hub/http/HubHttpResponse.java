package com.aresstack.huggingface.hub.http;

public final class HubHttpResponse {

    private final int statusCode;
    private final byte[] body;
    private final String contentType;

    public HubHttpResponse(int statusCode, byte[] body, String contentType) {
        this.statusCode = statusCode;
        this.body = body == null ? new byte[0] : body;
        this.contentType = contentType;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public byte[] getBody() {
        byte[] copy = new byte[body.length];
        System.arraycopy(body, 0, copy, 0, body.length);
        return copy;
    }

    public String getBodyAsUtf8() {
        try {
            return new String(body, "UTF-8");
        } catch (java.io.UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 must be available.", exception);
        }
    }

    public String getContentType() {
        return contentType;
    }
}
