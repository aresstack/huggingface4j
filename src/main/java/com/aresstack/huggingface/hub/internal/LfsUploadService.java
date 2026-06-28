package com.aresstack.huggingface.hub.internal;

import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.http.HubHttpClient;
import com.aresstack.huggingface.hub.http.HubHttpRequest;
import com.aresstack.huggingface.hub.http.HubHttpResponse;
import com.aresstack.huggingface.hub.query.HubUrlBuilder;
import com.aresstack.huggingface.hub.repo.RepositoryType;
import com.aresstack.huggingface.hub.upload.CommitOperation;
import com.aresstack.huggingface.hub.upload.UploadProgressListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implement the Git-LFS basic transfer used to upload large files referenced by a commit:
 * a batch negotiation against {@code /{repo}.git/info/lfs/objects/batch}, followed by streaming each
 * object to the storage URL the Hub returns and an optional verify call.
 */
final class LfsUploadService {

    private static final String LFS_CONTENT_TYPE = "application/vnd.git-lfs+json";

    private final HubHttpClient httpClient;

    LfsUploadService(HubHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Ensure every given LFS file exists in the repository's LFS storage, uploading the ones the Hub
     * does not already have.
     */
    void upload(String repoId, RepositoryType type, List<CommitOperation.AddedFile> lfsFiles,
                UploadProgressListener listener) throws HuggingFaceHubException {
        if (lfsFiles.isEmpty()) {
            return;
        }
        try {
            Map<String, CommitOperation.AddedFile> byOid = new LinkedHashMap<String, CommitOperation.AddedFile>();
            JsonArray objects = new JsonArray();
            for (CommitOperation.AddedFile file : lfsFiles) {
                String oid = file.sha256Hex();
                byOid.put(oid, file);
                JsonObject object = new JsonObject();
                object.addProperty("oid", oid);
                object.addProperty("size", file.length());
                objects.add(object);
            }

            HubHttpResponse batchResponse = requestBatch(repoId, type, objects);
            HuggingFaceHubException.Response failure = HuggingFaceHubException.forStatus(
                    batchResponse.getStatusCode(), batchResponse.getBodyAsUtf8());
            if (failure != null) {
                throw failure;
            }

            JsonObject batch = JsonParser.parseString(batchResponse.getBodyAsUtf8()).getAsJsonObject();
            JsonArray responseObjects = batch.has("objects") ? batch.getAsJsonArray("objects") : new JsonArray();
            for (JsonElement element : responseObjects) {
                uploadObject(element.getAsJsonObject(), byOid, listener);
            }
        } catch (IOException exception) {
            throw new HuggingFaceHubException("Failed to upload Git-LFS object.", exception);
        }
    }

    private HubHttpResponse requestBatch(String repoId, RepositoryType type, JsonArray objects) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("operation", "upload");
        JsonArray transfers = new JsonArray();
        transfers.add("basic");
        body.add("transfers", transfers);
        body.addProperty("hash_algo", "sha_256");
        body.add("objects", objects);

        String prefix = type.gitNamespace().isEmpty() ? "" : "/" + type.gitNamespace();
        String path = prefix + HubUrlBuilder.path(repoId) + ".git/info/lfs/objects/batch";
        HubHttpRequest request = HubHttpRequest.withBody("POST", path, body.toString().getBytes("UTF-8"), LFS_CONTENT_TYPE)
                .withHeader("Accept", LFS_CONTENT_TYPE);
        return httpClient.execute(request);
    }

    private void uploadObject(JsonObject object, Map<String, CommitOperation.AddedFile> byOid,
                              UploadProgressListener listener) throws IOException, HuggingFaceHubException {
        String oid = object.has("oid") ? object.get("oid").getAsString() : null;
        CommitOperation.AddedFile file = oid == null ? null : byOid.get(oid);
        if (file == null) {
            return;
        }
        if (object.has("error")) {
            JsonObject error = object.getAsJsonObject("error");
            String message = error.has("message") ? error.get("message").getAsString() : "unknown error";
            throw new HuggingFaceHubException("Git-LFS rejected '" + file.getPath() + "': " + message);
        }
        if (!object.has("actions")) {
            // No actions means the object already exists in LFS storage; nothing to upload.
            return;
        }
        JsonObject actions = object.getAsJsonObject("actions");
        if (!actions.has("upload")) {
            return;
        }
        JsonObject upload = actions.getAsJsonObject("upload");
        String href = upload.get("href").getAsString();
        Map<String, String> headers = headers(upload);

        HubHttpResponse uploadResponse = httpClient.uploadFile(href, file, headers, file.getPath(), listener);
        if (uploadResponse.getStatusCode() < 200 || uploadResponse.getStatusCode() >= 300) {
            throw new HuggingFaceHubException("Git-LFS upload of '" + file.getPath() + "' failed with HTTP "
                    + uploadResponse.getStatusCode() + ": " + uploadResponse.getBodyAsUtf8());
        }

        if (actions.has("verify")) {
            verify(actions.getAsJsonObject("verify"), oid, file.length());
        }
    }

    private void verify(JsonObject verify, String oid, long size) throws IOException, HuggingFaceHubException {
        String href = verify.get("href").getAsString();
        Map<String, String> headers = headers(verify);
        headers.put("Content-Type", LFS_CONTENT_TYPE);
        headers.put("Accept", LFS_CONTENT_TYPE);
        JsonObject body = new JsonObject();
        body.addProperty("oid", oid);
        body.addProperty("size", size);
        HubHttpResponse response = httpClient.executeAbsolute(href, "POST", body.toString().getBytes("UTF-8"), headers);
        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            throw new HuggingFaceHubException("Git-LFS verify of object " + oid + " failed with HTTP "
                    + response.getStatusCode() + ".");
        }
    }

    private static Map<String, String> headers(JsonObject action) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        if (action.has("header")) {
            JsonObject header = action.getAsJsonObject("header");
            for (Map.Entry<String, JsonElement> entry : header.entrySet()) {
                headers.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return headers;
    }
}
