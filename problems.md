# Known problems and limitations

This file tracks only genuine open issues and limitations of the current code. Release mechanics live
in [PUBLISHING.md](PUBLISHING.md); future feature work lives in [ROADMAP.md](ROADMAP.md).

## Git-LFS: basic transfer only
Large/weight files upload via the Git-LFS **basic** single-`PUT` transfer. Not implemented:
- **multipart/chunked** transfer for very large single objects (multi-GB),
- **parallel** LFS uploads of multiple objects,
- **retry/backoff** across repeated `PUT` attempts on a flaky upload.

For the vast majority of model/dataset file sizes the basic transfer is sufficient. Multipart is the
main follow-up (see ROADMAP).

## Write path not exercised against the real Hub in CI
All write/LFS endpoints are covered by tests against a local `HttpServer` (paths, methods, JSON/NDJSON
bodies, headers, error mapping, the LFS batch + streamed upload + verify + `lfsFile` reference, and the
"object already exists" case). A real write against huggingface.co needs a write token and a disposable
repo and is therefore an **optional manual test** (`HuggingFaceHubWriteLifecycleManualTest`, enabled
only when `HF_TOKEN_WRITE` is set), not a mandatory CI test. The exact request/response field names for
`/api/repos/create`, `/api/repos/delete`, `/api/{ns}/{id}/commit/{rev}`, `/api/{ns}/{id}/settings` and
`/{repo}.git/info/lfs/objects/batch` follow the behaviour of `huggingface_hub` and the Git-LFS batch
spec; individual field names could differ server-side and should be confirmed with the manual test
before release.

## Release build (Maven) not run locally
The Maven Central release runs from `pom.xml` (`mvn -B clean deploy`) via the
`central-publishing-maven-plugin`, mirroring `aresstack/win-proxy-java`. Maven is not installed in this
environment, so the `pom.xml` was validated by mirroring the proven sibling project and by building the
same sources with Gradle, not by running `mvn` locally. GPG signing has likewise not been exercised
against a real key here. Run `mvn -B clean verify -Dgpg.skip=true` (and a full `mvn deploy` from CI with
the secrets) to confirm before/at the first release — see PUBLISHING.md.

## Environment-dependent test
`UrlConnectionHubHttpClientTest.stripsAuthorizationOnCrossHostRedirect` needs `localhost` to be
reachable in addition to `127.0.0.1` (two different host strings, to prove the token is not forwarded
across hosts). On machines where `localhost` resolves only to IPv6 `::1` while the test server binds to
`127.0.0.1`, the test is **skipped** (via `Assumptions`) rather than failing.

## Deliberate design decision: checked exceptions
`HuggingFaceHubException` is a checked exception and stays that way for `0.1.x`. Switching to runtime
exceptions would be a larger breaking change that is not warranted for the first release. HTTP failures
are mapped to typed subclasses (`Unauthorized`/`Forbidden`/`NotFound`/`RateLimited` + generic
`Response`) via `HuggingFaceHubException.forStatus(...)`.
