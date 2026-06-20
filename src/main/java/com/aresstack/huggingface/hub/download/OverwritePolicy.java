package com.aresstack.huggingface.hub.download;

/**
 * Decide what happens when the download target file already exists.
 */
public enum OverwritePolicy {

    /** Always download and replace any existing file (default). */
    OVERWRITE,

    /** Keep the existing file and skip the download entirely. */
    SKIP_IF_EXISTS,

    /** Fail with an exception if the target file already exists. */
    FAIL_IF_EXISTS
}
