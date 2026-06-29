# Publishing to Maven Central

huggingface4j publishes to Maven Central through the **Central Publisher Portal** using **Maven**
(`pom.xml` + the `central-publishing-maven-plugin`), mirroring the proven setup of the sibling
`aresstack/win-proxy-java` project. Gradle is used for day-to-day development, tests and local installs
(`publishToMavenLocal`); the actual Central release is driven by Maven from `pom.xml`.

The Maven build produces and signs the main, sources and Javadoc jars and deploys them with
`autoPublish=true`, so a successful `mvn deploy` publishes to Central without a manual Portal step.

Coordinates: `com.aresstack:huggingface4j:0.1.0` (the `com.aresstack` namespace is already verified on
the Central Portal).

## Required GitHub Actions secrets

`.github/workflows/release.yml` uses the same secret names as `win-proxy-java`, so existing
organisation-level secrets are reused:

| Secret             | Purpose                                                   |
|--------------------|----------------------------------------------------------|
| `CENTRAL_USERNAME` | Central Portal user-token username                       |
| `CENTRAL_PASSWORD` | Central Portal user-token password                       |
| `GPG_PRIVATE_KEY`  | ASCII-armored PGP **private** key (imported by setup-java)|
| `GPG_PASSPHRASE`   | Passphrase for the PGP key                               |

`setup-java` imports `GPG_PRIVATE_KEY`, writes a Maven `settings.xml` with a `central` server using
`CENTRAL_USERNAME`/`CENTRAL_PASSWORD`, and exposes the passphrase to the `maven-gpg-plugin` as
`MAVEN_GPG_PASSPHRASE` (referenced by `${gpg.passphrase}` in `pom.xml`).

## One-time setup (only if the org secrets are not already present)

1. **Namespace** — `com.aresstack` must be verified in the Central Portal (already done for
   win-proxy-java).
2. **Publishing token** — Portal → *Account* → *Generate User Token* → `CENTRAL_USERNAME` /
   `CENTRAL_PASSWORD`.
3. **GPG key**
   ```bash
   gpg --gen-key
   gpg --armor --export-secret-keys <KEY_ID>            # -> GPG_PRIVATE_KEY
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   ```

## Local validation

Maven (used for the real deploy) — build, sign-less, attach sources + javadoc:

```bash
mvn -B clean verify -Dgpg.skip=true        # full build incl. sources/javadoc jars, no signing
mvn -B clean deploy                         # the real release (needs secrets + GPG key)
```

Gradle (development convenience):

```bash
./gradlew clean build            # compile + all tests, Java 8 bytecode
./gradlew publishToMavenLocal    # install into ~/.m2 for local consumers
```

> Keep `group`/`version` in sync between `pom.xml` and `build.gradle`.

## Cutting a release

1. Ensure `version` is the release version (no `-SNAPSHOT`) in **both** `pom.xml` and `build.gradle`.
2. Run the real write smoke test once (`HF_TOKEN_WRITE=... ./gradlew test --tests '*WriteLifecycleManualTest'`).
3. Commit and push to `master`.
4. Tag and push: `git tag v0.1.0 && git push origin v0.1.0`.
5. `release.yml` runs `mvn -B clean deploy` (sign + publish with `autoPublish=true`) and creates a
   GitHub release. With auto-publish, the artifact appears on Maven Central within ~15–30 minutes; no
   manual Portal confirmation is required.
