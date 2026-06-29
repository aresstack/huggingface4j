# Roadmap

huggingface4j covers the Hub's read, auth, download and core write paths. The items below are the
planned direction after `0.1.0`. Versions are indicative, not commitments.

## Released

- **0.1.0** — Auth/OAuth, model search/details/files, streaming + snapshot download, repository
  create/delete/settings, commit API (inline + Git-LFS basic transfer). See
  [RELEASE_NOTES.md](RELEASE_NOTES.md).

## Planned

| Version | Scope |
|---------|-------|
| 0.2.0   | Discussions / Pull Requests read & write (comments, status, merge) |
| 0.3.0   | Collections read & write |
| 0.4.0   | Spaces management (secrets, variables, hardware, runtime) |
| 0.5.0   | Jobs (start, status, cancel, logs) |
| 0.6.0   | Webhooks (create, update, delete, list) |
| 0.7.0   | Inference Endpoints management |
| 0.8.0   | Orgs / Service Accounts / Resource Groups / SCIM |

## Cross-cutting / optional

- **Git-LFS multipart transfer** for very large single objects (multi-GB), plus parallel LFS uploads
  and per-`PUT` retry/backoff. The current `basic` single-`PUT` transfer covers the vast majority of
  model and dataset files.
- **Binary-compatibility gates** (japicmp / revapi) once the public API has stabilised.
- **Preupload-based dedup** (`/preupload`) — currently dedup relies on the LFS batch response, which
  already skips objects the Hub already has.

## API stability

The hand-written public API (`HuggingFaceHub`, the fluent `models()` / `account()` /
`repositories()` builders, the `download`, `upload`, `repo` and `oauth` packages, and
`HuggingFaceHubException`) is intended to stay stable across `0.1.x`. Classes under
`com.aresstack.huggingface.hub.internal` are implementation details and may change without notice.
