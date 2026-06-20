package com.aresstack.huggingface.hub.repo;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.upload.CommitRequest;
import com.aresstack.huggingface.hub.upload.DeleteFileRequest;
import com.aresstack.huggingface.hub.upload.UploadFileRequest;

import java.nio.file.Path;

/**
 * A handle to a specific repository, exposing write operations (upload, delete file, commit,
 * settings, delete repository).
 */
public final class RepositoryResource {

    private final HuggingFaceHubClient client;
    private final String repoId;
    private final RepositoryType type;

    public RepositoryResource(HuggingFaceHubClient client, String repoId, RepositoryType type) {
        this.client = client;
        this.repoId = repoId;
        this.type = type;
    }

    public UploadFileRequest uploadFile(Path localFile) {
        return new UploadFileRequest(client, repoId, type, localFile);
    }

    public UploadFileRequest uploadContent(byte[] content) {
        return new UploadFileRequest(client, repoId, type, content);
    }

    public DeleteFileRequest deleteFile(String repoPath) {
        return new DeleteFileRequest(client, repoId, type, repoPath);
    }

    /** Start a multi-operation commit. */
    public CommitRequest commit() {
        return new CommitRequest(client, repoId, type);
    }

    public UpdateRepositorySettingsRequest settings() {
        return new UpdateRepositorySettingsRequest(client, repoId).type(type);
    }

    public DeleteRepositoryRequest delete() {
        return new DeleteRepositoryRequest(client, repoId).type(type);
    }
}
