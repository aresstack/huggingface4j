package com.aresstack.huggingface.hub;

import com.aresstack.huggingface.hub.download.DownloadResult;
import com.aresstack.huggingface.hub.model.HubFile;
import com.aresstack.huggingface.hub.repo.RepositoryInfo;
import com.aresstack.huggingface.hub.repo.RepositoryType;
import com.aresstack.huggingface.hub.upload.UploadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional, manual end-to-end smoke test for the write path. It is enabled only when a dedicated
 * {@code HF_TOKEN_WRITE} environment variable with a <strong>write-enabled</strong> token is present,
 * so it never runs in the normal build/CI and a plain read {@code HF_TOKEN} can never trigger
 * repository creation.
 *
 * <p>It creates a uniquely named, private, disposable repository, exercises inline + Git-LFS uploads,
 * listing, download, settings and deletion, and always tries to delete the repository again.</p>
 *
 * <p>Run locally with, for example:</p>
 * <pre>HF_TOKEN_WRITE=hf_xxx ./gradlew test --tests '*WriteLifecycleManualTest'</pre>
 */
@EnabledIfEnvironmentVariable(named = "HF_TOKEN_WRITE", matches = ".+")
final class HuggingFaceHubWriteLifecycleManualTest {

    @Test
    void fullWriteLifecycle(@TempDir Path dir) throws Exception {
        HuggingFaceHub hub = HuggingFaceHub.standard()
                .accessToken(System.getenv("HF_TOKEN_WRITE"))
                .build();

        String user = hub.account().whoAmI().execute().getName();
        assertNotNull(user, "whoAmI must resolve the authenticated user");
        String repoId = user + "/hf4j-smoke-" + System.currentTimeMillis();

        RepositoryInfo created = hub.repositories()
                .create(repoId)
                .type(RepositoryType.MODEL)
                .privateRepository(true)
                .execute();
        assertNotNull(created.getUrl());

        try {
            // Inline uploads (model card + config).
            hub.repositories().model(repoId)
                    .uploadContent("# Smoke test model\n".getBytes(StandardCharsets.UTF_8))
                    .to("README.md").commitMessage("Add model card").execute();

            UploadResult config = hub.repositories().model(repoId)
                    .uploadContent("{\"model_type\":\"test\"}".getBytes(StandardCharsets.UTF_8))
                    .to("config.json").commitMessage("Add config").execute();
            assertTrue(!config.isLfs(), "config.json must be uploaded inline");

            // A tiny file with a weight extension must take the Git-LFS path.
            UploadResult weights = hub.repositories().model(repoId)
                    .uploadContent("not-real-weights".getBytes(StandardCharsets.UTF_8))
                    .to("model.safetensors").commitMessage("Add weights").execute();
            assertTrue(weights.isLfs(), "model.safetensors must be uploaded via LFS");
            assertNotNull(weights.getSha256());

            // Listing should now contain the uploaded files.
            List<HubFile> files = hub.models().model(repoId).files().execute();
            assertTrue(containsPath(files, "config.json"), "config.json should be listed");
            assertTrue(containsPath(files, "model.safetensors"), "model.safetensors should be listed");

            // Download a file back and verify the content.
            Path target = dir.resolve("config.json");
            DownloadResult download = hub.models().model(repoId).file("config.json")
                    .downloadTo(target).execute();
            assertTrue(download.getBytesDownloaded() > 0);
            assertTrue(new String(Files.readAllBytes(target), StandardCharsets.UTF_8).contains("model_type"));

            // Settings change: make it public then private again.
            hub.repositories().model(repoId).settings().privateRepository(false).execute();
            hub.repositories().model(repoId).settings().privateRepository(true).execute();

            // Delete a file via commit.
            hub.repositories().model(repoId)
                    .deleteFile("config.json").commitMessage("Remove config").execute();
        } finally {
            hub.repositories().delete(repoId).type(RepositoryType.MODEL).confirm(repoId).execute();
        }
    }

    private static boolean containsPath(List<HubFile> files, String path) {
        for (HubFile file : files) {
            if (path.equals(file.getPath())) {
                return true;
            }
        }
        return false;
    }
}
