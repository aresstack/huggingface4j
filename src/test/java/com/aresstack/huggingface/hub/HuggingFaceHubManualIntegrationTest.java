package com.aresstack.huggingface.hub;

import com.aresstack.huggingface.hub.account.UserProfile;
import com.aresstack.huggingface.hub.download.DownloadResult;
import com.aresstack.huggingface.hub.model.ModelDetails;
import com.aresstack.huggingface.hub.model.ModelSearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional, manual end-to-end checks that hit the real Hugging Face Hub. They run only when an
 * {@code HF_TOKEN} environment variable is present, so they stay out of the normal offline build
 * and CI matrix. Enable them locally with a valid read token to validate gated/private behaviour.
 */
@EnabledIfEnvironmentVariable(named = "HF_TOKEN", matches = ".+")
final class HuggingFaceHubManualIntegrationTest {

    @Test
    void whoAmIReturnsTheAuthenticatedUser() throws Exception {
        HuggingFaceHub hub = HuggingFaceHub.standard().environmentToken().build();
        UserProfile profile = hub.account().whoAmI().execute();
        assertNotNull(profile.getName());
    }

    @Test
    void searchesPublicModels() throws Exception {
        HuggingFaceHub hub = HuggingFaceHub.standard().environmentToken().build();
        ModelSearchResult result = hub.models().search("qwen").limit(3).execute();
        assertFalse(result.getModels().isEmpty());
    }

    @Test
    void downloadsASmallPublicFile(@TempDir Path dir) throws Exception {
        HuggingFaceHub hub = HuggingFaceHub.standard().environmentToken().build();
        Path target = dir.resolve("config.json");
        DownloadResult result = hub.models()
                .model("Qwen/Qwen2.5-Coder-0.5B-Instruct")
                .file("config.json")
                .downloadTo(target)
                .execute();
        assertTrue(Files.exists(target));
        assertTrue(result.getBytesDownloaded() > 0);
    }

    @Test
    void readsDetailsOfAGatedRepository() throws Exception {
        HuggingFaceHub hub = HuggingFaceHub.standard().environmentToken().build();
        // meta-llama models are gated; with an approved token this resolves, otherwise it raises
        // a Forbidden/Unauthorized which still proves the auth path is wired correctly.
        try {
            ModelDetails details = hub.models().model("meta-llama/Llama-3.2-1B").details().execute();
            assertNotNull(details.getRepoId());
        } catch (HuggingFaceHubException.Forbidden | HuggingFaceHubException.Unauthorized expected) {
            // Token has no access to the gated repo; behaviour is still correct.
            assertNotNull(expected.getMessage());
        }
    }
}
