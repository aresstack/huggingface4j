package com.aresstack.huggingface.hub.internal;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.http.HubHttpClient;
import com.aresstack.huggingface.hub.http.HubHttpRequest;
import com.aresstack.huggingface.hub.http.HubHttpResponse;
import com.aresstack.huggingface.hub.model.HubFile;
import com.aresstack.huggingface.hub.model.ModelDetails;
import com.aresstack.huggingface.hub.model.ModelDetailsQuery;
import com.aresstack.huggingface.hub.model.ModelSearchResult;
import com.aresstack.huggingface.hub.query.HubQueryParameters;
import com.aresstack.huggingface.hub.query.HubUrlBuilder;
import com.aresstack.huggingface.hub.repo.CreateRepositoryRequest;
import com.aresstack.huggingface.hub.repo.DeleteRepositoryRequest;
import com.aresstack.huggingface.hub.repo.RepositoryInfo;
import com.aresstack.huggingface.hub.repo.UpdateRepositorySettingsRequest;
import com.aresstack.huggingface.hub.upload.CommitOperation;
import com.aresstack.huggingface.hub.upload.CommitRequest;
import com.aresstack.huggingface.hub.upload.CommitResult;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DefaultHuggingFaceHubClient implements HuggingFaceHubClient {

    private final HubHttpClient httpClient;
    private final HubJsonMapper mapper;

    public DefaultHuggingFaceHubClient(HubHttpClient httpClient) {
        this.httpClient = httpClient;
        this.mapper = new HubJsonMapper();
    }

    @Override
    public com.aresstack.huggingface.hub.account.UserProfile whoAmI() throws HuggingFaceHubException {
        HubHttpResponse response = execute(HubHttpRequest.get("/api/whoami-v2"));
        requireSuccess(response);
        return mapper.toUserProfile(response.getBodyAsUtf8());
    }

    @Override
    public ModelSearchResult searchModels(HubQueryParameters parameters) throws HuggingFaceHubException {
        HubHttpResponse response = execute(HubHttpRequest.get(HubUrlBuilder.appendQuery("/api/models", parameters)));
        requireSuccess(response);
        return mapper.toModelSearchResult(response.getBodyAsUtf8());
    }

    @Override
    public ModelDetails getModelDetails(ModelDetailsQuery query) throws HuggingFaceHubException {
        String path = "/api/models" + HubUrlBuilder.path(query.getModelReference().getRepoId());
        path = HubUrlBuilder.appendQuery(path, query.toQueryParameters());
        HubHttpResponse response = execute(HubHttpRequest.get(path));
        requireSuccess(response);
        return mapper.toModelDetails(response.getBodyAsUtf8());
    }

    @Override
    public List<HubFile> listModelFiles(ModelDetailsQuery query) throws HuggingFaceHubException {
        return getModelDetails(query).getFiles();
    }

    @Override
    public com.aresstack.huggingface.hub.download.DownloadResult downloadFile(com.aresstack.huggingface.hub.download.DownloadRequest request) throws HuggingFaceHubException {
        String path = "/" + request.getModelReference().getRepoId() + "/resolve/" + request.getRevision() + "/" + request.getFilePath();
        HubHttpRequest httpRequest = HubHttpRequest.get(HubUrlBuilder.path(path));
        return new com.aresstack.huggingface.hub.download.FileDownloader(httpClient).download(httpRequest, request);
    }

    @Override
    public RepositoryInfo createRepository(CreateRepositoryRequest request) throws HuggingFaceHubException {
        JsonObject body = new JsonObject();
        body.addProperty("type", request.getType().value());
        applyRepoName(body, request.getRepoId());
        body.addProperty("private", request.isPrivateRepository());
        HubHttpResponse response = execute(HubHttpRequest.postJson("/api/repos/create", body.toString()));
        if (request.isExistsOk() && response.getStatusCode() == 409) {
            return new RepositoryInfo(request.getRepoId(), request.getType(), null);
        }
        requireSuccess(response);
        String url = mapper.createdRepositoryUrl(response.getBodyAsUtf8());
        return new RepositoryInfo(request.getRepoId(), request.getType(), url);
    }

    @Override
    public void deleteRepository(DeleteRepositoryRequest request) throws HuggingFaceHubException {
        JsonObject body = new JsonObject();
        body.addProperty("type", request.getType().value());
        applyRepoName(body, request.getRepoId());
        HubHttpResponse response = execute(HubHttpRequest.deleteJson("/api/repos/delete", body.toString()));
        if (request.isMissingOk() && response.getStatusCode() == 404) {
            return;
        }
        requireSuccess(response);
    }

    @Override
    public void updateRepositorySettings(UpdateRepositorySettingsRequest request) throws HuggingFaceHubException {
        JsonObject body = new JsonObject();
        if (request.getPrivateRepository() != null) {
            body.addProperty("private", request.getPrivateRepository());
        }
        if (request.getGated() != null) {
            if ("false".equalsIgnoreCase(request.getGated())) {
                body.addProperty("gated", false);
            } else {
                body.addProperty("gated", request.getGated());
            }
        }
        String path = "/api/" + request.getType().apiNamespace() + HubUrlBuilder.path(request.getRepoId()) + "/settings";
        requireSuccess(execute(HubHttpRequest.putJson(path, body.toString())));
    }

    @Override
    public CommitResult commit(CommitRequest request) throws HuggingFaceHubException {
        Set<CommitOperation.AddedFile> lfsFiles = classifyLfsFiles(request);
        new LfsUploadService(httpClient).upload(request.getRepoId(), request.getType(),
                new ArrayList<CommitOperation.AddedFile>(lfsFiles), request.getProgressListener());

        byte[] body = buildCommitBody(request, lfsFiles);
        String path = "/api/" + request.getType().apiNamespace()
                + HubUrlBuilder.path(request.getRepoId()) + "/commit" + HubUrlBuilder.path(request.getRevision());
        HubQueryParameters params = new HubQueryParameters();
        if (request.isCreatePullRequest()) {
            params.set("create_pr", "1");
        }
        path = HubUrlBuilder.appendQuery(path, params);
        HubHttpResponse response = execute(HubHttpRequest.withBody("POST", path, body, "application/x-ndjson"));
        requireSuccess(response);
        return mapper.toCommitResult(response.getBodyAsUtf8());
    }

    private static Set<CommitOperation.AddedFile> classifyLfsFiles(CommitRequest request) throws HuggingFaceHubException {
        Set<CommitOperation.AddedFile> lfsFiles = new LinkedHashSet<CommitOperation.AddedFile>();
        try {
            for (CommitOperation operation : request.getOperations()) {
                if (operation instanceof CommitOperation.AddedFile && ((CommitOperation.AddedFile) operation).isLfs()) {
                    lfsFiles.add((CommitOperation.AddedFile) operation);
                }
            }
        } catch (IOException exception) {
            throw new HuggingFaceHubException("Failed to inspect file for commit.", exception);
        }
        return lfsFiles;
    }

    private static byte[] buildCommitBody(CommitRequest request, Set<CommitOperation.AddedFile> lfsFiles) throws HuggingFaceHubException {
        StringBuilder ndjson = new StringBuilder();
        JsonObject headerValue = new JsonObject();
        headerValue.addProperty("summary", request.getSummary());
        if (request.getDescription() != null) {
            headerValue.addProperty("description", request.getDescription());
        }
        JsonObject header = new JsonObject();
        header.addProperty("key", "header");
        header.add("value", headerValue);
        ndjson.append(header.toString()).append('\n');
        try {
            for (CommitOperation operation : request.getOperations()) {
                JsonObject value = new JsonObject();
                value.addProperty("path", operation.getPath());
                JsonObject line = new JsonObject();
                if (operation instanceof CommitOperation.AddedFile) {
                    CommitOperation.AddedFile file = (CommitOperation.AddedFile) operation;
                    if (lfsFiles.contains(file)) {
                        value.addProperty("algo", "sha256");
                        value.addProperty("oid", file.sha256Hex());
                        value.addProperty("size", file.length());
                        line.addProperty("key", "lfsFile");
                    } else {
                        value.addProperty("content", Base64.getEncoder().encodeToString(file.readContent()));
                        value.addProperty("encoding", "base64");
                        line.addProperty("key", "file");
                    }
                } else {
                    line.addProperty("key", "deletedFile");
                }
                line.add("value", value);
                ndjson.append(line.toString()).append('\n');
            }
        } catch (IOException exception) {
            throw new HuggingFaceHubException("Failed to read file content for commit.", exception);
        }
        return ndjson.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void applyRepoName(JsonObject body, String repoId) {
        int slash = repoId.indexOf('/');
        if (slash > 0) {
            body.addProperty("organization", repoId.substring(0, slash));
            body.addProperty("name", repoId.substring(slash + 1));
        } else {
            body.addProperty("name", repoId);
        }
    }

    private HubHttpResponse execute(HubHttpRequest request) throws HuggingFaceHubException {
        try {
            return httpClient.execute(request);
        } catch (IOException exception) {
            throw new HuggingFaceHubException("Failed to communicate with the Hugging Face Hub.", exception);
        }
    }

    private static void requireSuccess(HubHttpResponse response) throws HuggingFaceHubException.Response {
        HuggingFaceHubException.Response failure = HuggingFaceHubException.forStatus(response.getStatusCode(), response.getBodyAsUtf8());
        if (failure != null) {
            throw failure;
        }
    }
}
