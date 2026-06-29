# Release notes

## 0.1.0

First public release. Coordinates: `com.aresstack:huggingface4j:0.1.0` (Java 8, single runtime
dependency: Gson).

### Authentication
- Anonymous, static access token, `HF_TOKEN` environment token and custom token provider.
- OAuth device-code browser login with bounded, cancelable polling and typed error handling
  (`authorization_pending`, `slow_down`, `access_denied`, `expired_token`, `invalid_grant`,
  `invalid_client`).
- `Authorization: Bearer` header restricted to the configured Hub host across redirects.

### Read
- `whoAmI()` account lookup.
- Fluent model search with common filters (task, library, filter, gated, sort, limit) and an open
  query-parameter escape hatch.
- Model details and file listing via Gson mapping.

### Download
- Streaming single-file download to a `.part` temp file with atomic move into place.
- Resume of interrupted downloads, overwrite policy, and optional size / SHA-256 verification.
- `DownloadResult` with bytes, content length, ETag and resolved URL.
- Snapshot/repository download with allow/ignore glob patterns.
- Typed HTTP error mapping (`Unauthorized`, `Forbidden`, `NotFound`, `RateLimited`, generic
  `Response`).

### Write
- Repository management: create, delete and settings (visibility / gating).
- Commit API: upload and delete files, multi-operation commits, optional pull request.
- Confirmation-gated repository deletion (`confirm("org/name")`).
- Automatic small-vs-LFS decision; large/weight files use the streamed Git-LFS **basic** transfer
  with progress reporting; `UploadResult` reports size, SHA-256, LFS flag and commit info.

### Build & tooling
- HTTP layer with JSON `POST`/`PUT`/`PATCH`/`DELETE`, NDJSON bodies, absolute requests, streamed
  uploads and typed response headers.
- GitHub Actions CI (Ubuntu/Windows × JDK 8/17) and a tag-triggered release workflow.
- Maven Central publishing configured (sources + Javadoc, in-memory PGP signing, Central Portal
  staging repository).
- 50 automated tests (offline); optional manual integration tests for real-Hub read and write flows.

### Known limitations
See [ROADMAP.md](ROADMAP.md) and [problems.md](problems.md). Notably: Git-LFS supports the *basic*
single-`PUT` transfer only (no multipart/chunked transfer for multi-GB single files); the write path
is covered by local server tests and an optional manual test, not by mandatory real-Hub CI.
