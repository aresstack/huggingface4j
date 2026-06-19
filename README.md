# huggingface-hub-java

Java 8 fluent client for the Hugging Face Hub API.

Example:

    HuggingFaceHub hub = HuggingFaceHub.standard()
            .anonymous()
            .build();

    ModelSearchResult result = hub.models()
            .search("qwen coder")
            .task("text-generation")
            .library("transformers")
            .sortByDownloads()
            .limit(20)
            .execute();

The public API is hand-written and stable. OpenAPI generated classes are treated as transport DTOs only.

## First slice

- Anonymous API access
- Static access token support
- HF_TOKEN environment token support
- Fluent model search
- Model details and file listing
- Single file download with progress callback
- whoAmI() account lookup
- Java 8 target

## Generate Hugging Face OpenAPI DTOs

Run:

    ./gradlew generateHuggingFaceOpenApiModels

The generated DTOs are not part of the ergonomic public API.
