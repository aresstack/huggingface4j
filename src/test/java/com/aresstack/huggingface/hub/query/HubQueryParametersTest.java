package com.aresstack.huggingface.hub.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HubQueryParametersTest {

    @Test
    void buildsMinimalSearchQuery() {
        HubQueryParameters parameters = new HubQueryParameters()
                .set("search", "qwen coder")
                .setNumber("limit", Integer.valueOf(20));

        assertEquals("/api/models?search=qwen%20coder&limit=20", HubUrlBuilder.appendQuery("/api/models", parameters));
    }
}
