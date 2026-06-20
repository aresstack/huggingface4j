# huggingface4j

Java 8 fluent client for the Hugging Face Hub API, with an ollama4j-style fluent API.

The public API is hand-written and stable. OpenAPI generated classes are treated as transport DTOs only.

## Usage

Anonymous public model search:

    HuggingFaceHub hub = HuggingFaceHub.standard().anonymous().build();

    ModelSearchResult result = hub.models()
            .search("qwen coder")
            .task("text-generation")
            .library("transformers")
            .filter("safetensors")
            .gated(false)
            .sortByTrendingScore()
            .limit(20)
            .execute();

Authenticated access:

    HuggingFaceHub hub = HuggingFaceHub.standard().accessToken("hf_...").build();

Environment token:

    HuggingFaceHub hub = HuggingFaceHub.standard().environmentToken().build();

Custom token provider:

    HuggingFaceHub hub = HuggingFaceHub.standard()
            .tokenProvider(myTokenProvider)
            .build();

Model details:

    ModelDetails details = hub.models()
            .model("Qwen/Qwen2.5-Coder-0.5B-Instruct")
            .details()
            .includeFiles()
            .includeConfig()
            .execute();

    String repoId = details.getRepoId();

Files only:

    java.util.List<HubFile> files = hub.models()
            .model("Qwen/Qwen2.5-Coder-0.5B-Instruct")
            .files()
            .execute();

Streaming file download:

    DownloadResult download = hub.models()
            .model("Qwen/Qwen2.5-Coder-0.5B-Instruct")
            .file("config.json")
            .revision("main")
            .downloadTo(targetFile)
            .execute();

## Current slice

- Anonymous API access
- Static access token support
- HF_TOKEN environment token support
- Custom token provider support
- OAuth browser login support
- Bearer token HTTP header
- Fluent model search with common filters
- Open query parameter escape hatch
- Model details and file listing
- Gson JSON mapping
- Streaming single-file downloads
- Hub-host-only Authorization across redirects
- whoAmI() account lookup
- Java 8 target
- Maven publishing metadata

## Generate Hugging Face OpenAPI DTOs

Run:

    ./gradlew generateHuggingFaceOpenApiModels

The generated DTOs are not part of the ergonomic public API.
