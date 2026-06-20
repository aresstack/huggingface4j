package com.aresstack.huggingface.hub.repo;

/**
 * The result of creating (or describing) a repository.
 */
public final class RepositoryInfo {

    private final String repoId;
    private final RepositoryType type;
    private final String url;

    public RepositoryInfo(String repoId, RepositoryType type, String url) {
        this.repoId = repoId;
        this.type = type;
        this.url = url;
    }

    public String getRepoId() {
        return repoId;
    }

    public RepositoryType getType() {
        return type;
    }

    /** The canonical Hub URL of the repository, e.g. {@code https://huggingface.co/org/name}. */
    public String getUrl() {
        return url;
    }
}
