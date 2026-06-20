package com.aresstack.huggingface.hub.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A single change within a commit: either adding/overwriting a file or deleting one.
 */
public abstract class CommitOperation {

    private final String path;

    private CommitOperation(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository file path must not be blank.");
        }
        this.path = path.trim();
    }

    /** The path of the file inside the repository (forward-slash separated). */
    public String getPath() {
        return path;
    }

    public static AddedFile addFile(String path, byte[] content) {
        return new AddedFile(path, content, null);
    }

    public static AddedFile addFile(String path, Path localFile) {
        return new AddedFile(path, null, localFile);
    }

    public static DeletedFile deleteFile(String path) {
        return new DeletedFile(path);
    }

    /** Add or overwrite a file. Content is provided inline or read from a local file at commit time. */
    public static final class AddedFile extends CommitOperation {
        private final byte[] content;
        private final Path localFile;

        private AddedFile(String path, byte[] content, Path localFile) {
            super(path);
            if (content == null && localFile == null) {
                throw new IllegalArgumentException("Either inline content or a local file is required.");
            }
            this.content = content;
            this.localFile = localFile;
        }

        /** Read the file content, either inline bytes or from the configured local file. */
        public byte[] readContent() throws IOException {
            if (content != null) {
                byte[] copy = new byte[content.length];
                System.arraycopy(content, 0, copy, 0, content.length);
                return copy;
            }
            return Files.readAllBytes(localFile);
        }
    }

    /** Delete a file from the repository. */
    public static final class DeletedFile extends CommitOperation {
        private DeletedFile(String path) {
            super(path);
        }
    }
}
