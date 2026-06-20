package com.aresstack.huggingface.hub.download;

import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.http.HubHttpClient;
import com.aresstack.huggingface.hub.http.HubHttpRequest;
import com.aresstack.huggingface.hub.http.HubHttpStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Orchestrate a robust single-file download: streams to a {@code .part} temp file, optionally
 * resumes an interrupted transfer, verifies size/checksum, and atomically moves the completed file
 * into place. HTTP errors are surfaced as {@link HuggingFaceHubException.Response} subtypes.
 */
public final class FileDownloader {

    private static final int BUFFER_SIZE = 1024 * 1024;

    private final HubHttpClient httpClient;

    public FileDownloader(HubHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public DownloadResult download(HubHttpRequest request, DownloadRequest options) throws HuggingFaceHubException {
        Path target = options.getTargetFile();
        DownloadResult existing = applyOverwritePolicy(options, target);
        if (existing != null) {
            return existing;
        }
        try {
            return doDownload(request, options, target);
        } catch (IOException exception) {
            throw new HuggingFaceHubException("Failed to download Hugging Face file.", exception);
        }
    }

    private DownloadResult applyOverwritePolicy(DownloadRequest options, Path target) throws HuggingFaceHubException {
        if (!Files.exists(target)) {
            return null;
        }
        switch (options.getOverwritePolicy()) {
            case SKIP_IF_EXISTS:
                long size = sizeQuietly(target);
                return new DownloadResult(target, 0L, size, null, null, true);
            case FAIL_IF_EXISTS:
                throw new HuggingFaceHubException("Target file already exists: " + target);
            case OVERWRITE:
            default:
                return null;
        }
    }

    private DownloadResult doDownload(HubHttpRequest request, DownloadRequest options, Path target) throws IOException, HuggingFaceHubException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path partFile = target.resolveSibling(target.getFileName().toString() + ".part");

        long resumeFrom = 0L;
        if (options.isResume() && Files.exists(partFile)) {
            resumeFrom = Files.size(partFile);
        } else {
            Files.deleteIfExists(partFile);
        }

        HubHttpStream stream = httpClient.openStream(request, resumeFrom);
        try {
            if (!stream.isSuccess()) {
                String body = readErrorBody(stream);
                throw HuggingFaceHubException.forStatus(stream.getStatusCode(), body);
            }

            boolean append = stream.isPartial() && resumeFrom > 0L;
            if (!append) {
                // Server ignored the Range header (or no resume): start from scratch.
                resumeFrom = 0L;
                Files.deleteIfExists(partFile);
            }

            long alreadyOnDisk = append ? resumeFrom : 0L;
            long totalLength = stream.getTotalLength();
            long transferred = copy(stream.getBody(), partFile, append, alreadyOnDisk, totalLength, options.getProgressListener());

            move(partFile, target);

            long finalSize = Files.size(target);
            verify(target, finalSize, options);
            return new DownloadResult(target, transferred, finalSize, stream.getEtag(), stream.getResolvedUrl(), false);
        } finally {
            stream.close();
        }
    }

    private long copy(InputStream input, Path partFile, boolean append, long alreadyOnDisk, long totalLength, ProgressListener listener) throws IOException {
        OutputStream output = append
                ? Files.newOutputStream(partFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)
                : Files.newOutputStream(partFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        long transferred = 0L;
        try {
            byte[] chunk = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(chunk)) >= 0) {
                output.write(chunk, 0, read);
                transferred += read;
                if (listener != null) {
                    listener.onProgress(new DownloadProgress(alreadyOnDisk + transferred, totalLength));
                }
            }
        } finally {
            output.close();
        }
        return transferred;
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void verify(Path target, long finalSize, DownloadRequest options) throws IOException, HuggingFaceHubException {
        if (options.getExpectedSize() != null && options.getExpectedSize().longValue() != finalSize) {
            Files.deleteIfExists(target);
            throw new HuggingFaceHubException("Downloaded size " + finalSize
                    + " does not match expected size " + options.getExpectedSize() + ".");
        }
        if (options.getExpectedSha256() != null && !options.getExpectedSha256().trim().isEmpty()) {
            String actual = sha256(target);
            String expected = options.getExpectedSha256().trim().toLowerCase(Locale.ROOT);
            if (!actual.equalsIgnoreCase(expected)) {
                Files.deleteIfExists(target);
                throw new HuggingFaceHubException("Downloaded SHA-256 " + actual
                        + " does not match expected " + expected + ".");
            }
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            InputStream input = Files.newInputStream(file);
            try {
                byte[] chunk = new byte[BUFFER_SIZE];
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

    private static String readErrorBody(HubHttpStream stream) {
        try {
            InputStream input = stream.getBody();
            if (input == null) {
                return "";
            }
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = input.read(chunk)) >= 0) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), "UTF-8");
        } catch (IOException exception) {
            return "";
        }
    }

    private static long sizeQuietly(Path file) {
        try {
            return Files.size(file);
        } catch (IOException exception) {
            return -1L;
        }
    }
}
