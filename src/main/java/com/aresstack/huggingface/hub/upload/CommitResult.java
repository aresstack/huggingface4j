package com.aresstack.huggingface.hub.upload;

/**
 * The outcome of a commit to a repository.
 */
public final class CommitResult {

    private final String commitOid;
    private final String commitUrl;
    private final String pullRequestUrl;

    public CommitResult(String commitOid, String commitUrl, String pullRequestUrl) {
        this.commitOid = commitOid;
        this.commitUrl = commitUrl;
        this.pullRequestUrl = pullRequestUrl;
    }

    /** The object id (SHA) of the created commit, when reported by the Hub. */
    public String getCommitOid() {
        return commitOid;
    }

    /** The URL of the created commit, when reported by the Hub. */
    public String getCommitUrl() {
        return commitUrl;
    }

    /** The URL of the opened pull request when the commit was created as a PR, otherwise {@code null}. */
    public String getPullRequestUrl() {
        return pullRequestUrl;
    }
}
