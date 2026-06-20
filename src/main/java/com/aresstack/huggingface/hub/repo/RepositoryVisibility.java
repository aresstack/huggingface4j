package com.aresstack.huggingface.hub.repo;

/**
 * Whether a repository is publicly visible or private.
 */
public enum RepositoryVisibility {

    PUBLIC(false),
    PRIVATE(true);

    private final boolean privateRepository;

    RepositoryVisibility(boolean privateRepository) {
        this.privateRepository = privateRepository;
    }

    public boolean isPrivate() {
        return privateRepository;
    }

    public static RepositoryVisibility of(boolean privateRepository) {
        return privateRepository ? PRIVATE : PUBLIC;
    }
}
