package com.aresstack.huggingface.hub.download;

import com.aresstack.huggingface.hub.HuggingFaceHubClient;
import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.model.HubFile;
import com.aresstack.huggingface.hub.model.ModelDetailsQuery;
import com.aresstack.huggingface.hub.model.ModelReference;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Download every file of a model repository revision into a local directory, optionally filtered by
 * allow/ignore glob patterns. Files are placed under the target directory mirroring their repository
 * paths. Each file is downloaded with the same robustness guarantees as
 * {@link ModelFileDownloadRequest} (temp file, atomic move, overwrite policy, optional resume).
 */
public final class SnapshotDownloadRequest {

    private final HuggingFaceHubClient client;
    private final String id;
    private String revision = "main";
    private Path targetDirectory;
    private final List<GlobPattern> allow = new ArrayList<GlobPattern>();
    private final List<GlobPattern> ignore = new ArrayList<GlobPattern>();
    private OverwritePolicy overwritePolicy = OverwritePolicy.OVERWRITE;
    private boolean resume;
    private SnapshotProgressListener progressListener;

    public SnapshotDownloadRequest(HuggingFaceHubClient client, String id) {
        this.client = client;
        this.id = id;
    }

    public SnapshotDownloadRequest revision(String revision) {
        this.revision = revision;
        return this;
    }

    public SnapshotDownloadRequest downloadTo(Path targetDirectory) {
        this.targetDirectory = targetDirectory;
        return this;
    }

    /** Only download files matching at least one of these glob patterns. */
    public SnapshotDownloadRequest allow(String... patterns) {
        for (String pattern : patterns) {
            if (pattern != null && !pattern.trim().isEmpty()) {
                allow.add(GlobPattern.compile(pattern.trim()));
            }
        }
        return this;
    }

    /** Skip files matching any of these glob patterns. */
    public SnapshotDownloadRequest ignore(String... patterns) {
        for (String pattern : patterns) {
            if (pattern != null && !pattern.trim().isEmpty()) {
                ignore.add(GlobPattern.compile(pattern.trim()));
            }
        }
        return this;
    }

    public SnapshotDownloadRequest overwritePolicy(OverwritePolicy overwritePolicy) {
        this.overwritePolicy = overwritePolicy == null ? OverwritePolicy.OVERWRITE : overwritePolicy;
        return this;
    }

    public SnapshotDownloadRequest resume(boolean resume) {
        this.resume = resume;
        return this;
    }

    public SnapshotDownloadRequest onFile(SnapshotProgressListener progressListener) {
        this.progressListener = progressListener;
        return this;
    }

    public List<DownloadResult> execute() throws HuggingFaceHubException {
        if (targetDirectory == null) {
            throw new IllegalStateException("Target directory must be set before executing snapshot download.");
        }
        ModelDetailsQuery query = new ModelDetailsQuery(ModelReference.model(id));
        query.include("siblings");
        if (revision != null && !revision.trim().isEmpty()) {
            query.revision(revision);
        }
        List<HubFile> files = client.listModelFiles(query);
        List<String> selected = new ArrayList<String>();
        for (HubFile file : files) {
            if (file.getPath() != null && isSelected(file.getPath())) {
                selected.add(file.getPath());
            }
        }

        List<DownloadResult> results = new ArrayList<DownloadResult>(selected.size());
        int index = 0;
        for (String filePath : selected) {
            index++;
            if (progressListener != null) {
                progressListener.onFileStart(filePath, index, selected.size());
            }
            Path target = resolveTarget(filePath);
            DownloadRequest request = new DownloadRequest(ModelReference.model(id), filePath, revision, target,
                    null, overwritePolicy, resume, null, null);
            results.add(client.downloadFile(request));
        }
        return results;
    }

    private boolean isSelected(String path) {
        for (GlobPattern pattern : ignore) {
            if (pattern.matches(path)) {
                return false;
            }
        }
        if (allow.isEmpty()) {
            return true;
        }
        for (GlobPattern pattern : allow) {
            if (pattern.matches(path)) {
                return true;
            }
        }
        return false;
    }

    private Path resolveTarget(String filePath) {
        Path resolved = targetDirectory;
        for (String segment : filePath.split("/")) {
            if (!segment.isEmpty()) {
                resolved = resolved.resolve(segment);
            }
        }
        return resolved;
    }

    /** Notified before each file in a snapshot download begins. */
    public interface SnapshotProgressListener {
        void onFileStart(String filePath, int index, int total);
    }
}
