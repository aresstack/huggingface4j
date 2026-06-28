package com.aresstack.huggingface.hub.upload;

/**
 * How a file should be transferred when added in a commit.
 */
public enum UploadMode {

    /** Decide automatically based on size and file extension. */
    AUTO,

    /** Force inline (base64 in the commit) transfer, never Git-LFS. */
    SMALL,

    /** Force the Git-LFS transfer path. */
    LFS
}
