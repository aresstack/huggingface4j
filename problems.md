# Offene Punkte / Entscheidungen, die nicht rein aus dem Code lösbar sind

Diese Datei dokumentiert die Stellen, an denen ich auf externe Accounts, Secrets oder bewusste
Design-Entscheidungen gestoßen bin. Der Code ist überall so weit implementiert, dass nur noch die
manuellen/infrastrukturellen Schritte fehlen.

## 1. Maven Central / Central Portal – manuelle, externe Schritte
Folgende Schritte lassen sich grundsätzlich **nicht** aus dem Repository heraus ausführen, weil sie
externe Accounts und Geheimnisse erfordern. Der Build ist aber vollständig darauf vorbereitet:

- **Namespace `io.github.aresstack` im Central Portal verifizieren** – setzt voraus, dass der
  GitHub-User/die Org `aresstack` existiert und im Central Portal als Namespace bestätigt wird.
- **Publishing-Token erzeugen** (Central Portal → User Token) → als Secrets `CENTRAL_USERNAME` /
  `CENTRAL_PASSWORD` hinterlegen.
- **GPG-Key erzeugen** (`gpg --gen-key`, dann ASCII-armored Private Key exportieren) → als Secrets
  `SIGNING_KEY` (armored Key) / `SIGNING_PASSWORD` hinterlegen.
- **GitHub-Secrets setzen**: `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD`.
- **Tatsächliches Release**: Tag `v0.1.0` pushen → `release.yml` lädt die signierten Artefakte in das
  Staging-Repo des Central Portals hoch.

Im Build umgesetzt: `signing`-Plugin mit In-Memory-PGP, bedingtes Signieren (lokaler Build/Tests
laufen ohne Key weiter), Publishing-Repository auf die **OSSRH Staging API** des Central Portals
(`https://ossrh-staging-api.central.sonatype.com/...`), Version `0.1.0`, Sources-/Javadoc-JARs,
`publishToMavenLocal` lokal validiert.

### Bewusste Entscheidung: Publishing-Mechanismus
Ich habe **bewusst** den eingebauten `maven-publish` + `signing`-Weg auf die OSSRH-kompatible
Staging-API gewählt statt eines Drittanbieter-Plugins (z. B. `com.vanniktech.maven.publish` oder das
offizielle `central-publishing`-Plugin). Grund: kein zusätzlicher Plugin-Resolve, hermetischer Build,
keine Überraschungen bei eingeschränkter Netzwerkumgebung. **Einschränkung:** Über die Staging-API
landet das Deployment zunächst in einem *Staging-Repository*; das finale „Publish/Release“ im Central
Portal muss ggf. noch (einmalig in der UI oder per Portal-API) bestätigt bzw. auf Auto-Publish gestellt
werden. Wer ein vollständig automatisiertes Release will, kann später auf das vanniktech-Plugin
(`SonatypeHost.CENTRAL_PORTAL`, `automaticRelease = true`) umstellen.

### Nicht verifizierbar ohne echten Key
Die Signatur-Konfiguration ist verdrahtet, konnte aber lokal **nicht gegen einen echten GPG-Key
getestet** werden (kein Key vorhanden). `publishToMavenLocal` (ohne Signatur) wurde erfolgreich
ausgeführt.

## 2. Exception-Modell: checked beibehalten
Das Arbeitspaket bat um eine Entscheidung „checked vs. runtime“. Ich habe `HuggingFaceHubException`
**bewusst als checked Exception belassen**, weil es die bereits etablierte öffentliche API ist und ein
Wechsel auf Runtime-Exceptions ein größerer Breaking Change wäre, der zu 0.1.0 nicht nötig ist. Die
HTTP-Fehlerabbildung wurde stattdessen vereinheitlicht (`HuggingFaceHubException.forStatus(...)`) und
um typisierte Subklassen (401/403/404/429 + generisch) erweitert.

## 3. OAuth-Integrationstest ist nur „happy path“ automatisierbar
Ein echter End-to-End-OAuth-Login erfordert **menschliche Interaktion** (Browser-Bestätigung) und
lässt sich daher nicht sinnvoll als automatischer Test gegen echtes HF ausführen. Abgedeckt sind:
- Vollständige Unit-Tests für `authorization_pending`, `slow_down`, `access_denied`, `expired_token`,
  Deadline/Timeout und Cancel (`OAuthLoginTest`).
- Der manuelle Integrationstest (`HuggingFaceHubManualIntegrationTest`, nur mit `HF_TOKEN`) deckt
  whoAmI, Suche, Download und gated-Repo-Verhalten ab – nicht den interaktiven OAuth-Flow.

## 4. Cross-Host-Redirect-Test ist umgebungsabhängig
`stripsAuthorizationOnCrossHostRedirect` braucht, dass `localhost` zusätzlich zu `127.0.0.1`
erreichbar ist (unterschiedliche Host-Strings, um das Token-Stripping zu prüfen). Auf Systemen, auf
denen `localhost` ausschließlich auf IPv6 `::1` zeigt und der Test-Server nur an `127.0.0.1` gebunden
ist, wird der Test per `Assumptions.assumeTrue(...)` **übersprungen statt rot**. Lokal lief er grün.

## 6. Write-API (REST-WRITE-COVERAGE-1) – Umfang & Grenzen
Umgesetzt sind **Repository Management** (create/delete/settings) und die **Upload/Commit-API**
(Datei-Upload/-Delete, Multi-Operation-Commit, optional als Pull Request) inkl. der nötigen
HTTP-Primitives (`postJson`/`putJson`/`patchJson`/`deleteJson`/`withBody`/`withHeader`, NDJSON-Body,
typisierte Response-Header) und einer Safety-Gate für destruktive Deletes (`confirm(...)`).

**Bekannte Grenze – Git-LFS:** Die Commit-API sendet Datei-Inhalte **inline als base64**. Das ist
ideal für Text/kleine Binärdateien (Model Cards, Configs, Tokenizer), aber **nicht** für große bzw.
LFS-getrackte Dateien (`*.safetensors`, `*.bin`, große `*.gguf` …). Dafür wäre das mehrstufige
Git-LFS-Pre-Upload-Protokoll nötig (`/preupload`-Batch + S3-Upload + `lfsFile`-Operation). Das ist
bewusst **noch nicht** implementiert; der Hub lehnt zu große Inline-Uploads ab. Folgepaket-Kandidat.

**Nicht getestet gegen echtes HF:** Die Write-Endpunkte sind vollständig gegen einen lokalen
`HttpServer` getestet (Pfade, Methoden, Bodies, Header, Fehler-Mapping, NDJSON-Aufbau), aber ein
echter Schreibvorgang gegen huggingface.co braucht einen Write-Token und ein Wegwerf-Repo und wurde
nicht ausgeführt. Die Annahmen über exakte Request-/Response-Felder (`/api/repos/create`,
`/api/repos/delete`, `/api/{ns}/{id}/commit/{rev}`, `/api/{ns}/{id}/settings`) folgen dem Verhalten
von `huggingface_hub`; einzelne Feldnamen könnten serverseitig abweichen.

## 7. Restliche REST-Bereiche bewusst (noch) nicht abgedeckt
Dieses Paket war auf **Repository Management + Upload/Commit** zugeschnitten. Weiterhin offen (laut
priorisierter Roadmap aus dem Arbeitsauftrag) und damit Kandidaten für Folgepakete:
Discussions/PRs (schreibend), Collections, Spaces-Management, Jobs, Webhooks,
Orgs/Service-Accounts/SCIM, Inference-Endpoint-Management. Der erweiterte HTTP-Layer (JSON-Verben,
Header, Bodies) bildet dafür bereits die Basis.

## 5. Bewusst nicht umgesetzt (als optional markiert)
- **Binary-Compatibility-Gates (japicmp/revapi)**: vom Arbeitspaket als „optional/später“ markiert,
  bewusst nicht hinzugefügt, um den 0.1.0-Scope schlank zu halten.
- **OpenAPI-DTO-Generierung** bleibt wie gehabt ein separater, manueller Gradle-Task und ist nicht
  Teil der öffentlichen API.
