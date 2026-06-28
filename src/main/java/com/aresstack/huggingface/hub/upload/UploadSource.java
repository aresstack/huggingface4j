package com.aresstack.huggingface.hub.upload;

import java.io.IOException;
import java.io.InputStream;

/**
 * A re-openable source of bytes to upload, backed by a local file or in-memory content.
 */
public interface UploadSource {

    /** The total number of bytes that {@link #openStream()} will yield. */
    long length() throws IOException;

    /** Open a fresh stream over the content. The caller closes it. */
    InputStream openStream() throws IOException;
}
