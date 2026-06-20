package com.aresstack.huggingface.hub.oauth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

final class OAuthJson {

    private OAuthJson() {
    }

    static OAuthStart parseStart(String json) throws OAuthException {
        JsonObject root = object(json);
        return new OAuthStart(
                text(root, "device_code"),
                text(root, "user_code"),
                text(root, "verification_uri"),
                firstText(root, "verification_uri_complete", "verification_url"),
                intValue(root, "expires_in"),
                intValue(root, "interval"));
    }

    static OAuthToken parseToken(String json) throws OAuthException {
        JsonObject root = object(json);
        return new OAuthToken(
                text(root, "access_token"),
                text(root, "token_type"),
                intValue(root, "expires_in"),
                text(root, "refresh_token"),
                text(root, "scope"));
    }

    static OAuthException parseError(String json) {
        try {
            JsonObject root = object(json);
            String error = text(root, "error");
            String description = firstText(root, "error_description", "message");
            String message = description == null ? "Hugging Face OAuth error: " + error : description;
            return new OAuthException(message, error, description);
        } catch (OAuthException exception) {
            return new OAuthException("Hugging Face OAuth request failed.", null, json);
        }
    }

    private static JsonObject object(String json) throws OAuthException {
        try {
            return JsonParser.parseString(json == null ? "{}" : json).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new OAuthException("Failed to parse Hugging Face OAuth JSON.", exception);
        }
    }

    private static String firstText(JsonObject object, String firstName, String secondName) {
        String first = text(object, firstName);
        return first == null ? text(object, secondName) : first;
    }

    private static String text(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static Integer intValue(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? null : Integer.valueOf(value.getAsInt());
    }
}
