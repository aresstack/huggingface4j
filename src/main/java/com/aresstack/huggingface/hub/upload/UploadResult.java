package com.aresstack.huggingface.hub.upload;

/**
 * The result of a single-file upload, combining the per-file transfer metadata with the commit it
 * produced.
 */
public final class UploadResult {

    private final String path;
    private final long size;
    private final String sha256;
    private final boolean lfs;
    private final String commitOid;
    private final String commitUrl;
    private final String pullRequestUrl;

    public UploadResult(String path, long size, String sha256, boolean lfs, CommitResult commit) {
        this.path = path;
        this.size = size;
        this.sha256 = sha256;
        this.lfs = lfs;
        this.commitOid = commit == null ? null : commit.getCommitOid();
        this.commitUrl = commit == null ? null : commit.getCommitUrl();
        this.pullRequestUrl = commit == null ? null : commit.getPullRequestUrl();
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    /** The hex SHA-256 of the uploaded content, or {@code null} when not computed. */
    public String getSha256() {
        return sha256;
    }

    /** Whether the file was transferred via Git-LFS rather than inline. */
    public boolean isLfs() {
        return lfs;
    }

    public String getCommitOid() {
        return commitOid;
    }

    public String getCommitUrl() {
        return commitUrl;
    }

    public String getPullRequestUrl() {
        return pullRequestUrl;
    }
}
