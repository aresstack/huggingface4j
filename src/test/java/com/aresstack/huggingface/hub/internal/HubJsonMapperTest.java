package com.aresstack.huggingface.hub.internal;

import com.aresstack.huggingface.hub.model.ModelDetails;
import com.aresstack.huggingface.hub.model.ModelSearchResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HubJsonMapperTest {

    @Test
    void mapsModelSearchResultMetadata() throws Exception {
        String json = "[{\"id\":\"Qwen/Qwen2.5-Coder-0.5B-Instruct\",\"author\":\"Qwen\",\"pipeline_tag\":\"text-generation\",\"library_name\":\"transformers\",\"tags\":[\"safetensors\",\"license:apache-2.0\"],\"downloads\":42,\"likes\":7,\"gated\":false}]";

        ModelSearchResult result = new HubJsonMapper().toModelSearchResult(json);

        assertEquals(1, result.size());
        assertEquals("Qwen/Qwen2.5-Coder-0.5B-Instruct", result.getModels().get(0).getRepoId());
        assertEquals("Qwen", result.getModels().get(0).getAuthor());
        assertEquals("text-generation", result.getModels().get(0).getTask());
        assertEquals("transformers", result.getModels().get(0).getLibrary());
        assertEquals("apache-2.0", result.getModels().get(0).getLicense());
        assertEquals(Boolean.FALSE, result.getModels().get(0).getGated());
        assertEquals(Long.valueOf(42L), result.getModels().get(0).getDownloads());
    }

    @Test
    void mapsModelDetailsFiles() throws Exception {
        String json = "{\"id\":\"org/model\",\"sha\":\"abc\",\"siblings\":[{\"rfilename\":\"config.json\",\"size\":12}]}";

        ModelDetails details = new HubJsonMapper().toModelDetails(json);

        assertEquals("abc", details.getSha());
        assertEquals(1, details.getFiles().size());
        assertEquals("config.json", details.getFiles().get(0).getPath());
        assertEquals(Long.valueOf(12L), details.getFiles().get(0).getSize());
    }
}
