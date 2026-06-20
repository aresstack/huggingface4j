package com.aresstack.huggingface.hub.internal;

import com.aresstack.huggingface.hub.model.ModelDetails;
import com.aresstack.huggingface.hub.model.ModelSearchResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void mapsRealisticModelDetailsResponse() throws Exception {
        // Trimmed but realistic shape of GET /api/models/{id}?expand=siblings&expand=config
        String json = "{"
                + "\"_id\":\"66f0a1\",\"id\":\"Qwen/Qwen2.5-Coder-0.5B-Instruct\",\"modelId\":\"Qwen/Qwen2.5-Coder-0.5B-Instruct\","
                + "\"author\":\"Qwen\",\"sha\":\"c4f3a8b\",\"created_at\":\"2024-09-18T09:00:00.000Z\","
                + "\"pipeline_tag\":\"text-generation\",\"library_name\":\"transformers\",\"gated\":\"auto\","
                + "\"downloads\":123456,\"likes\":321,"
                + "\"tags\":[\"transformers\",\"safetensors\",\"qwen2\",\"text-generation\",\"license:apache-2.0\"],"
                + "\"siblings\":["
                + "{\"rfilename\":\".gitattributes\"},"
                + "{\"rfilename\":\"config.json\",\"size\":661},"
                + "{\"rfilename\":\"model.safetensors\",\"size\":988097824},"
                + "{\"rfilename\":\"tokenizer.json\",\"size\":11421896}"
                + "],\"config\":{\"architectures\":[\"Qwen2ForCausalLM\"],\"model_type\":\"qwen2\"}}";

        ModelDetails details = new HubJsonMapper().toModelDetails(json);

        assertEquals("Qwen/Qwen2.5-Coder-0.5B-Instruct", details.getRepoId());
        assertEquals("c4f3a8b", details.getSha());
        assertEquals("Qwen", details.getSummary().getAuthor());
        assertEquals("text-generation", details.getSummary().getTask());
        assertEquals("transformers", details.getSummary().getLibrary());
        assertEquals("apache-2.0", details.getSummary().getLicense());
        assertEquals(Long.valueOf(123456L), details.getSummary().getDownloads());
        // "gated":"auto" is neither true nor false and must not be coerced.
        assertNull(details.getSummary().getGated());
        assertEquals(4, details.getFiles().size());
        assertEquals("config.json", details.getFiles().get(1).getPath());
        assertEquals(Long.valueOf(988097824L), details.getFiles().get(2).getSize());
        // A sibling without a size must still be mapped (null size).
        assertNull(details.getFiles().get(0).getSize());
    }
}
