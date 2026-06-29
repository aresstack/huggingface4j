# huggingface4j

A small, dependency-light **Java 8** client for the [Hugging Face Hub](https://huggingface.co) API,
with an [ollama4j](https://github.com/ollama4j/ollama4j)-style fluent API.

- Model search, model details and file listing
- Robust single-file and snapshot downloads (temp file + atomic move, resume, checksum verification)
- Repository management and uploads: create/delete/settings, plus file upload/delete via the commit
  API, including automatic Git-LFS transfer for large model files
- Token authentication (static, environment, custom provider) and OAuth browser login
- Only one runtime dependency: Gson

The public API is hand-written and intended to be stable. OpenAPI-generated classes, when used, are
treated as transport DTOs only and are not part of the ergonomic public API.

## Installation

Coordinates: `com.aresstack:huggingface4j:0.1.0`

### Maven

```xml
<dependency>
    <groupId>com.aresstack</groupId>
    <artifactId>huggingface4j</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.aresstack:huggingface4j:0.1.0'
```

## Authentication

```java
// Anonymous: public read-only access
HuggingFaceHub hub = HuggingFaceHub.standard().anonymous().build();

// Static access token (hf_...)
HuggingFaceHub hub = HuggingFaceHub.standard().accessToken("hf_xxx").build();

// Read the token from the HF_TOKEN environment variable
HuggingFaceHub hub = HuggingFaceHub.standard().environmentToken().build();

// Custom token provider (e.g. fetched/refreshed elsewhere)
HuggingFaceHub hub = HuggingFaceHub.standard()
        .tokenProvider(() -> myTokenStore.current())
        .build();
```

The `Authorization: Bearer` header is only ever sent to the configured Hub host. If a download is
redirected to a different host (for example a CDN), the token is **not** forwarded.

## Proxy support (since 0.1.1)

All HTTP traffic — API calls, redirects, downloads and LFS uploads — can be routed through a proxy.
Configure either a fixed `Proxy` or a `ProxySelector` (e.g. backed by PAC or Windows proxy settings):

```java
// Fixed proxy
java.net.Proxy proxy = new java.net.Proxy(
        java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress("proxy.corp", 8080));
HuggingFaceHub hub = HuggingFaceHub.standard().accessToken("hf_...").proxy(proxy).build();

// Per-request selection (PAC / system / Windows resolver)
java.net.ProxySelector selector = myWindowsProxyResolver();   // returns Proxy.NO_PROXY for direct
HuggingFaceHub hub = HuggingFaceHub.standard().proxySelector(selector).build();
```

A fixed `proxy(...)` takes precedence over `proxySelector(...)`; with neither set, connections are
direct. The OAuth device flow accepts the same hooks via
`HuggingFaceOAuth.deviceCode().proxy(...)` / `.proxySelector(...)`. Authenticated proxies (requiring
`Proxy-Authorization`) are the caller's responsibility — supply a `ProxySelector` plus a JVM
`Authenticator` as needed.

## Model search

```java
ModelSearchResult result = hub.models()
        .search("qwen coder")
        .task("text-generation")
        .library("transformers")
        .filter("safetensors")
        .gated(false)
        .sortByTrendingScore()
        .limit(20)
        .execute();

for (ModelSummary model : result.getModels()) {
    System.out.println(model.getRepoId() + " — " + model.getDownloads() + " downloads");
}
```

## Model details and files

```java
ModelDetails details = hub.models()
        .model("Qwen/Qwen2.5-Coder-0.5B-Instruct")
        .details()
        .includeFiles()
        .includeConfig()
        .execute();

List<HubFile> files = hub.models()
        .model("Qwen/Qwen2.5-Coder-0.5B-Instruct")
        .files()
        .execute();
```

## Downloading files

Single file with progress reporting:

```java
DownloadResult download = hub.models()
        .model("Qwen/Qwen2.5-Coder-0.5B-Instruct")
        .file("model.safetensors")
        .revision("main")
        .downloadTo(Paths.get("models/model.safetensors"))
        .resume(true)                       // continue an interrupted .part download
        .overwrite(false)                   // keep an already complete file
        .onProgress(p -> System.out.printf("%d%%%n", p.getPercent()))
        .execute();

System.out.println("ETag: " + download.getEtag());
System.out.println("Bytes: " + download.getBytesDownloaded());
```

Downloads stream to a `<name>.part` temp file and are atomically moved into place on success, so an
interrupted run never leaves a half-written target file. Optionally verify the result:

```java
hub.models().model("org/model").file("weights.bin")
        .downloadTo(target)
        .verifySize(988097824L)
        .verifySha256("d2c3...e9")
        .execute();
```

Whole-repository snapshot with allow/ignore glob patterns:

```java
List<DownloadResult> results = hub.models()
        .model("Qwen/Qwen2.5-Coder-0.5B-Instruct")
        .snapshot()
        .downloadTo(Paths.get("models/qwen"))
        .allow("*.json", "*.safetensors")
        .ignore("onnx/**")
        .onFile((path, index, total) -> System.out.printf("[%d/%d] %s%n", index, total, path))
        .execute();
```

## Repository management and uploads (write operations)

All write operations require an authenticated token with write access to the target repository.

Create / delete / settings:

```java
// Create a repository
hub.repositories()
        .create("aresstack/my-model")
        .type(RepositoryType.MODEL)
        .privateRepository(false)
        .execute();

// Change visibility / gating
hub.repositories().model("aresstack/my-model")
        .settings()
        .privateRepository(true)
        .gated("auto")
        .execute();

// Delete a repository — irreversible, so confirmation is mandatory
hub.repositories()
        .delete("aresstack/my-model")
        .type(RepositoryType.MODEL)
        .confirm("aresstack/my-model")   // must repeat the exact id
        .execute();
```

Upload / delete files via the commit API:

```java
// Single file upload
hub.repositories()
        .model("aresstack/my-model")
        .uploadFile(Paths.get("build/config.json"))
        .to("config.json")
        .commitMessage("Upload config")
        .execute();

// Single file delete
hub.repositories()
        .model("aresstack/my-model")
        .deleteFile("old.bin")
        .commitMessage("Remove old file")
        .execute();

// Multiple operations in one commit (optionally as a pull request)
CommitResult commit = hub.repositories()
        .model("aresstack/my-model")
        .commit()
        .addFile("config.json", Paths.get("build/config.json"))
        .addFile("README.md", "# My Model\n".getBytes(StandardCharsets.UTF_8))
        .deleteFile("old.bin")
        .commitMessage("Update model card and config")
        .createPullRequest(true)
        .execute();

System.out.println(commit.getCommitUrl());
```

### Large files and Git-LFS

`uploadFile(...)` automatically decides between an inline (base64) commit and a Git-LFS transfer:
files at or above the inline threshold (10 MB by default) or with a typical weight extension
(`.safetensors`, `.bin`, `.gguf`, `.onnx`, `.pt`, `.pth`, …) go through LFS. Large files are streamed
to LFS storage rather than buffered as base64, with progress reporting.

```java
UploadResult result = hub.repositories()
        .model("aresstack/my-model")
        .uploadFile(Paths.get("model.safetensors"))
        .to("model.safetensors")
        .commitMessage("Upload model weights")
        .onProgress(p -> System.out.println(p.getPercent() + "%"))
        .execute();

System.out.println("LFS: " + result.isLfs() + " sha256=" + result.getSha256());
```

Override the automatic decision when needed:

```java
// Force LFS (e.g. a file the default heuristics would treat as small)
hub.repositories().model("aresstack/my-model")
        .uploadFile(Paths.get("model.gguf")).to("model.gguf").lfs()
        .commitMessage("Upload GGUF model").execute();

// Force inline (fail-fast for things that must stay tiny)
hub.repositories().model("aresstack/my-model")
        .uploadFile(Paths.get("config.json")).to("config.json").smallFileOnly()
        .commitMessage("Upload config").execute();

// Tune the inline threshold
hub.repositories().model("aresstack/my-model")
        .uploadFile(localFile).to("data.parquet").maxInlineBytes(1_000_000L)
        .commitMessage("Upload data").execute();
```

`UploadResult` reports `getSize()`, `getSha256()`, `isLfs()` and the commit fields
(`getCommitOid()`, `getCommitUrl()`, `getPullRequestUrl()`).

**Git-LFS support:**

| Supported | Not supported (yet) |
|-----------|---------------------|
| ✅ basic single-`PUT` transfer | ❌ multipart / chunked multi-GB transfer |
| ✅ SHA-256 oid + size negotiation | ❌ parallel LFS uploads |
| ✅ `verify` action when the Hub returns one | ❌ retry / backoff across `PUT` attempts |
| ✅ dedup (skips objects the Hub already has) | |

This covers the vast majority of model and dataset files. See [ROADMAP.md](ROADMAP.md) for the
multipart follow-up.

Deletion is guarded: `delete(...).execute()` throws unless you confirm with the exact repository id
via `confirm("org/name")` (or `confirmDestructiveAction()`), so a repository can never be removed by
a single accidental call.

## Gated and private models

Some repositories (for example `meta-llama/*`) are **gated**: you must accept their terms on the Hub
and use a token that has been granted access. Provide such a token via `accessToken(...)`,
`environmentToken()` or OAuth with the `gated-repos` scope. Without access you receive a typed
exception:

- `HuggingFaceHubException.Unauthorized` (HTTP 401) — missing or invalid token
- `HuggingFaceHubException.Forbidden` (HTTP 403) — token has no access to this gated/private repo

## OAuth browser login (device flow)

```java
OAuthLogin login = HuggingFaceOAuth.deviceCode()
        .clientId("your-oauth-app-client-id")
        .scope(OAuthScope.OPENID)
        .scope(OAuthScope.PROFILE)
        .scope(OAuthScope.GATED_REPOS)
        .start();

// Show this to the user (or open it in a browser):
System.out.println("Visit " + login.getVerificationUriComplete());
System.out.println("and enter code " + login.getUserCode());

// Wait for approval. Bounded by the device-code expiry; pass a timeout to cap it further.
OAuthToken token = login.awaitToken(Duration.ofMinutes(2).toMillis());

HuggingFaceHub hub = HuggingFaceHub.standard()
        .tokenProvider(new OAuthTokenProvider(token))
        .build();
```

`awaitToken()` never polls forever — it stops when the device code expires, when the optional
timeout elapses, or when `login.cancel()` is called from another thread (raising
`OAuthException.Cancelled`). Terminal server errors are surfaced as `OAuthException` with helpers
such as `isAccessDenied()`, `isExpiredToken()`, `isInvalidGrant()` and `isInvalidClient()`.

## Error handling

All operations throw the checked `HuggingFaceHubException`. HTTP failures are mapped to specific
subtypes that carry the status code and response body:

```java
try {
    hub.models().model("meta-llama/Llama-3.2-1B").details().execute();
} catch (HuggingFaceHubException.Forbidden e) {
    // 403 — request access to the gated repo first
} catch (HuggingFaceHubException.RateLimited e) {
    // 429 — back off and retry
} catch (HuggingFaceHubException.Response e) {
    System.err.println("HTTP " + e.getStatusCode() + ": " + e.getResponseBody());
} catch (HuggingFaceHubException e) {
    // transport/parse failure (see getCause())
}
```

## Comparison with ollama4j / AskAI

huggingface4j deliberately mirrors the fluent, builder-first style of
[ollama4j](https://github.com/ollama4j/ollama4j) so it feels familiar inside projects such as AskAI.
The difference is scope: ollama4j talks to a local Ollama runtime for inference, while huggingface4j
talks to the Hugging Face **Hub** for discovery and model/file retrieval. They compose well — use
huggingface4j to find and download a model, and an inference runtime to run it.

## API status and stability

`0.1.x` is an early release. The hand-written public API surface (`HuggingFaceHub`, the fluent
`models()` / `account()` / `repositories()` builders, and the `download`, `upload`, `repo` and `oauth`
packages, plus `HuggingFaceHubException`) is what we intend to keep stable. Classes under
`com.aresstack.huggingface.hub.internal` are implementation details and may change without notice.
The full plan and known limitations are in [ROADMAP.md](ROADMAP.md) and [problems.md](problems.md).

## Building from source

```bash
./gradlew build            # compile + test (Java 8 bytecode)
./gradlew publishToMavenLocal
```

Optional: regenerate the Hugging Face OpenAPI DTOs (not part of the public API):

```bash
./gradlew generateHuggingFaceOpenApiModels
```

## Documentation map

- [RELEASE_NOTES.md](RELEASE_NOTES.md) — what's in each version.
- [ROADMAP.md](ROADMAP.md) — planned features and cross-cutting work.
- [PUBLISHING.md](PUBLISHING.md) — Maven Central release runbook and required secrets.
- [problems.md](problems.md) — known limitations.

## Running the optional manual integration tests

These hit the real Hub and are skipped unless their token is present:

```bash
# Read-only flows (whoAmI, search, public download, gated read)
HF_TOKEN=hf_read_token ./gradlew test --tests '*ManualIntegrationTest'

# Full write lifecycle on a disposable repo (needs a WRITE token)
HF_TOKEN_WRITE=hf_write_token ./gradlew test --tests '*WriteLifecycleManualTest'
```

## License

Apache License 2.0.
