package com.aresstack.huggingface.hub.upload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
        return new AddedFile(path, content, null, UploadMode.AUTO);
    }

    public static AddedFile addFile(String path, Path localFile) {
        return new AddedFile(path, null, localFile, UploadMode.AUTO);
    }

    public static AddedFile addFile(String path, byte[] content, UploadMode mode) {
        return new AddedFile(path, content, null, mode);
    }

    public static AddedFile addFile(String path, Path localFile, UploadMode mode) {
        return new AddedFile(path, null, localFile, mode);
    }

    public static DeletedFile deleteFile(String path) {
        return new DeletedFile(path);
    }

    /**
     * Add or overwrite a file. The content is provided inline or read from a local file at commit
     * time. The {@link UploadMode} and inline-size threshold decide whether the file is transferred
     * inline (base64) or via Git-LFS.
     */
    public static final class AddedFile extends CommitOperation implements UploadSource {
        private final byte[] content;
        private final Path localFile;
        private final UploadMode mode;
        private long maxInlineBytes = UploadClassifier.DEFAULT_MAX_INLINE_BYTES;
        private String precomputedSha256;

        private AddedFile(String path, byte[] content, Path localFile, UploadMode mode) {
            super(path);
            if (content == null && localFile == null) {
                throw new IllegalArgumentException("Either inline content or a local file is required.");
            }
            this.content = content;
            this.localFile = localFile;
            this.mode = mode == null ? UploadMode.AUTO : mode;
        }

        public UploadMode getMode() {
            return mode;
        }

        public AddedFile maxInlineBytes(long maxInlineBytes) {
            this.maxInlineBytes = maxInlineBytes;
            return this;
        }

        public long getMaxInlineBytes() {
            return maxInlineBytes;
        }

        /** Provide an already computed hex SHA-256 to avoid hashing the content twice. */
        public AddedFile precomputedSha256(String sha256) {
            this.precomputedSha256 = sha256;
            return this;
        }

        /** Whether this file should be transferred via Git-LFS given its mode, size and extension. */
        public boolean isLfs() throws IOException {
            return UploadClassifier.isLfs(mode, length(), getPath(), maxInlineBytes);
        }

        @Override
        public long length() throws IOException {
            return content != null ? content.length : Files.size(localFile);
        }

        @Override
        public InputStream openStream() throws IOException {
            return content != null ? new ByteArrayInputStream(content) : Files.newInputStream(localFile);
        }

        /** Read the entire content into memory (used for inline base64 transfers). */
        public byte[] readContent() throws IOException {
            if (content != null) {
                byte[] copy = new byte[content.length];
                System.arraycopy(content, 0, copy, 0, content.length);
                return copy;
            }
            return Files.readAllBytes(localFile);
        }

        /** The hex SHA-256 of the content, computed once and cached. */
        public String sha256Hex() throws IOException {
            if (precomputedSha256 != null) {
                return precomputedSha256;
            }
            precomputedSha256 = computeSha256();
            return precomputedSha256;
        }

        private String computeSha256() throws IOException {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                InputStream input = openStream();
                try {
                    byte[] chunk = new byte[1024 * 1024];
                    int read;
                    while ((read = input.read(chunk)) >= 0) {
                        digest.update(chunk, 0, read);
                    }
                } finally {
                    input.close();
                }
                byte[] hash = digest.digest();
                StringBuilder builder = new StringBuilder(hash.length * 2);
                for (byte b : hash) {
                    builder.append(Character.forDigit((b >> 4) & 0xF, 16));
                    builder.append(Character.forDigit(b & 0xF, 16));
                }
                return builder.toString();
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 must be available.", exception);
            }
        }
    }

    /** Delete a file from the repository. */
    public static final class DeletedFile extends CommitOperation {
        private DeletedFile(String path) {
            super(path);
        }
    }
}
