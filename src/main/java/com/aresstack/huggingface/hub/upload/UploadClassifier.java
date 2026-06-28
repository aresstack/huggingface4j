package com.aresstack.huggingface.hub.upload;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Decide whether a file should be transferred via Git-LFS or inline (base64 in the commit).
 *
 * <p>The decision is a pure function of the requested {@link UploadMode}, the file size, the file
 * extension and the configured inline-size threshold, so it is deterministic and can be evaluated
 * independently by the fluent request (for reporting) and the client (for transfer).</p>
 */
public final class UploadClassifier {

    /** Files at or above this size are transferred via LFS in {@link UploadMode#AUTO}. */
    public static final long DEFAULT_MAX_INLINE_BYTES = 10L * 1024L * 1024L;

    /** Extensions that the Hub conventionally tracks with Git-LFS regardless of size. */
    private static final Set<String> LFS_EXTENSIONS = new HashSet<String>(Arrays.asList(
            "safetensors", "bin", "gguf", "ggml", "onnx", "pt", "pth", "h5", "msgpack",
            "ckpt", "model", "tflite", "arrow", "pb", "ot", "joblib", "npy", "npz",
            "pickle", "pkl", "tar", "gz", "tgz", "zip", "7z", "bz2", "xz", "zst",
            "parquet", "wasm", "mlmodel"));

    private UploadClassifier() {
    }

    public static boolean isLfs(UploadMode mode, long size, String path, long maxInlineBytes) {
        if (mode == UploadMode.LFS) {
            return true;
        }
        if (mode == UploadMode.SMALL) {
            return false;
        }
        long threshold = maxInlineBytes > 0 ? maxInlineBytes : DEFAULT_MAX_INLINE_BYTES;
        if (size >= threshold) {
            return true;
        }
        return matchesLfsExtension(path);
    }

    public static boolean matchesLfsExtension(String path) {
        if (path == null) {
            return false;
        }
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) {
            return false;
        }
        return LFS_EXTENSIONS.contains(path.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
