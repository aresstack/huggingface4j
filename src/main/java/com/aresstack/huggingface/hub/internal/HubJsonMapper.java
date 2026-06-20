package com.aresstack.huggingface.hub.internal;

import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.model.HubFile;
import com.aresstack.huggingface.hub.model.ModelDetails;
import com.aresstack.huggingface.hub.model.ModelSearchResult;
import com.aresstack.huggingface.hub.model.ModelSummary;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public final class HubJsonMapper {

    public com.aresstack.huggingface.hub.account.UserProfile toUserProfile(String json) throws HuggingFaceHubException {
        JsonObject root = parse(json).getAsJsonObject();
        return new com.aresstack.huggingface.hub.account.UserProfile(text(root, "name"), text(root, "fullname"), text(root, "type"));
    }

    public ModelSearchResult toModelSearchResult(String json) throws HuggingFaceHubException {
        JsonElement root = parse(json);
        List<ModelSummary> models = new ArrayList<ModelSummary>();
        if (root.isJsonArray()) {
            JsonArray array = root.getAsJsonArray();
            for (int index = 0; index < array.size(); index++) {
                models.add(toModelSummary(array.get(index).getAsJsonObject()));
            }
        }
        return new ModelSearchResult(models);
    }

    public ModelDetails toModelDetails(String json) throws HuggingFaceHubException {
        JsonObject root = parse(json).getAsJsonObject();
        return new ModelDetails(toModelSummary(root), text(root, "sha"), files(root));
    }

    private ModelSummary toModelSummary(JsonObject object) {
        List<String> tags = tags(object);
        return new ModelSummary(
                firstText(object, "id", "modelId"),
                text(object, "author"),
                firstText(object, "pipeline_tag", "pipelineTag"),
                firstText(object, "library_name", "libraryName"),
                tags,
                license(tags, object),
                booleanValue(object, "gated"),
                longValue(object, "downloads"),
                longValue(object, "likes"));
    }

    private List<String> tags(JsonObject object) {
        List<String> tags = new ArrayList<String>();
        JsonArray array = array(object, "tags");
        if (array == null) {
            return tags;
        }
        for (int index = 0; index < array.size(); index++) {
            JsonElement value = array.get(index);
            if (value != null && !value.isJsonNull()) {
                tags.add(value.getAsString());
            }
        }
        return tags;
    }

    private String license(List<String> tags, JsonObject object) {
        String direct = text(object, "license");
        if (direct != null) {
            return direct;
        }
        for (String tag : tags) {
            if (tag != null && tag.startsWith("license:")) {
                return tag.substring("license:".length());
            }
        }
        return null;
    }

    private List<HubFile> files(JsonObject root) {
        List<HubFile> files = new ArrayList<HubFile>();
        JsonArray siblings = array(root, "siblings");
        if (siblings == null) {
            return files;
        }
        for (int index = 0; index < siblings.size(); index++) {
            JsonObject sibling = siblings.get(index).getAsJsonObject();
            files.add(new HubFile(firstText(sibling, "rfilename", "path"), longValue(sibling, "size")));
        }
        return files;
    }

    private JsonElement parse(String json) throws HuggingFaceHubException {
        try {
            return JsonParser.parseString(json == null ? "{}" : json);
        } catch (RuntimeException exception) {
            throw new HuggingFaceHubException("Failed to parse Hugging Face Hub JSON.", exception);
        }
    }

    private static JsonArray array(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || !value.isJsonArray() ? null : value.getAsJsonArray();
    }

    private static String firstText(JsonObject object, String firstName, String secondName) {
        String first = text(object, firstName);
        return first == null ? text(object, secondName) : first;
    }

    private static String text(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static Long longValue(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? null : Long.valueOf(value.getAsLong());
    }

    private static Boolean booleanValue(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
            return Boolean.valueOf(value.getAsBoolean());
        }
        String text = value.getAsString();
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return Boolean.valueOf(text);
        }
        return null;
    }
}
