# Deployment secrets and endpoint protection — plan

**Decision record:** [ADR-028](../adr/ADR-028-secrets-and-model-spend-protection.md)
holds the decisions and the rejected alternatives. This plan holds the survey,
the hosting comparison, the abuse layers, the rotation runbook and the sequenced
work.

**Status (2026-11-03):** approved 2026-11-02. Steps 1 to 6 are implemented and
committed on local main, unpushed (`5f725b0e`, `29766f81`..`20228523`, `bad2a93d`,
`91ec05eb`). Still open, all outside the code: step 0 (vendor workspaces, spend
limits, scoped keys, retiring today's key; the runbook log shows nothing recorded
yet), the rehearsal in step 7, and step 8 (waits on the host decision and the
Cloudflare plan's PR 7). The spike alert on refusals also waits on step 8, because
no metrics backend is wired yet. Sections 1 to 5 are the survey and design as
written on 2026-11-02; where they differ from ADR-028 or the code, those win.
`/admin/secrets` was built and then withdrawn (ADR-028, rejected alternatives).

**Goal:** deploy the management console publicly without the Anthropic API key
reaching a client, a log, a config file or git, and with a cost ceiling the
operator chose for the worst case of endpoint abuse.

**Decisions taken with Pat on 2026-11-02:** hosting platform undecided (this
plan compares options); identification stays behind sign-in plus the `VISION`
authority; the uncommitted `keyed-rate-limits` worktree lands first and this
plan builds on it; the secret port ships now with a static key, Workload
Identity Federation (WIF) is a later adapter.

**Sibling effort, same day:** [ADR-027, Cloudflare at the edge](../adr/ADR-027-cloudflare-edge-and-abuse-protection.md)
and its [plan](2026-11-02-cloudflare-edge-protection-plan.md) decide the edge,
the tunnel-only origin, the client address (`CF-Connecting-IP` through Tomcat's
`RemoteIpValve`), the keyed login/registration limiters (that plan's PRs 1 and
2 are the `keyed-rate-limits` worktree landing) and Turnstile. This plan
defers to them wherever they overlap; §1.6, §4 and §7 say exactly where.

---

## 1. Survey findings (2026-11-02, `main` at `71572997`)

### 1.1 Where the key is read, configured or passed

| Site | What it does | Verdict |
|---|---|---|
| [`AnthropicVisionService.java:68`](../../adapters/anthropic-vision/src/main/java/com/naturalist/vision/anthropic/AnthropicVisionService.java) | `System.getenv("MODEL_CREDENTIAL")` in the constructor; builds `AnthropicOkHttpClient` with the explicit key; throws if absent | Replace with the port |
| [`AnthropicTextGenerationService.java:48`](../../adapters/anthropic-text-generation/src/main/java/com/naturalist/textgeneration/anthropic/AnthropicTextGenerationService.java) | Same pattern | Replace with the port |
| [`VisionConfiguration.java:16`](../../apps/management-console/src/main/java/com/naturalist/console/VisionConfiguration.java) | Reads the env var a second time to choose `AnthropicVisionService` or `NoOpVisionService` | Select by property instead |
| [`IdentificationConfiguration.java`](../../apps/management-console/src/main/java/com/naturalist/console/IdentificationConfiguration.java) | Wires `NoOpTextGenerationService`; the text adapter is not on the app classpath | Leave; note it in the sequence |
| [`NoOpVisionService.java:12`](../../kernels/vision/src/main/java/com/naturalist/vision/NoOpVisionService.java) | Error text names the env var | Reword to name the property |
| [`application.yml`](../../apps/management-console/src/main/resources/application.yml), test `application.yml` | No key, no placeholder. The admin-credential comment already documents the unresolved-placeholder trap | Nothing to remove |
| [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) | Postgres service only; no Anthropic secret, no `secrets.*` reference | Nothing to change; tests must keep running with no key |
| Dockerfile, compose, Procfile, `.env` | None exist | Deployment config is greenfield |
| [`README.md:279`](../../README.md) | Documents `MODEL_CREDENTIAL`; line 30 records the intent to move to WIF once a host is chosen | Update when the property lands |
| `.claude/launch.json` | gitignored; may carry the key for local runs | Keep ignored; add `.env*` to `.gitignore` |
| Adapter tests | `adapters/anthropic-vision` and `adapters/anthropic-text-generation` have no tests at all | The port makes them testable |

Latent hazard found: the real adapter is selected by env-var presence, so a
developer with `MODEL_CREDENTIAL` exported runs every `@SpringBootTest` in
`management-console` against the real API.

### 1.2 Git history

All 904 commits on every ref were scanned for key-shaped strings
(vendor key prefixes), literal `.apiKey("…")` arguments, `MODEL_CREDENTIAL=` assignment
lines, and files ever added with secret-like names (`.env`, `*secret*`,
`*credential*`, keystores). Nothing was found; the only hits were the
`NaturalistCredential` domain classes.

Caveat: history was rewritten with `git filter-repo` on 2026-09-26 (addresses,
GPS, PDFs). This scan covers the surviving history only. Clones or forks taken
before the rewrite are outside its reach, so the first rotation in §5 is
treated as mandatory, not precautionary.

### 1.3 Inbound routes that trigger a model call

Exactly one today:

- `POST /insects/identify` →
  [`InsectsController.identify`](../../domains/insects/insects-console/src/main/java/com/naturalist/insects/InsectsController.java)
  → `InsectCommand.identification().identify(...)` →
  [`InsectIdentificationCommandImpl.identify`](../../domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationCommandImpl.java)
  → `VisionService.identify` (one or two turns) and `TextGenerationService`
  (zero to three enrichment calls, a no-op in the deployed app).

`SecurityConfiguration` also names `/plants/identify`, but no handler exists.
Re-identification was deleted in insects-ports slice 2 and is deferred.

### 1.4 Controls already in place

| Layer | Where | Setting |
|---|---|---|
| Authority | `SecurityConfiguration` | `POST /insects/identify` requires `VISION`; anonymous callers go to login |
| Grant policy | `AccountConfiguration`, `naturalist.access.grant-policy` | `self-serve`: verifying an email grants `VISION`. Anyone with a mailbox can obtain spending rights |
| Rate gate | `InsectIdentificationCommandImpl` step 0a, `vision.identification` | 3 identifications per minute, global, zero wait |
| Budget | `IdentificationBudget` step 0b, seeded `usage-counters.json` | per naturalist 10/day; global 50/day; global 650 since 2026-08-01 |
| Auth limits | `LoginRateLimitFilter`, `RegistrationTransaction` | 300 logins/min, 10 registrations/min, both global |
| Upload bound | `ImageUpload.MAX_BYTES`, multipart config | 20 MB, sniffed by magic bytes; servlet ceiling 21 MB / 22 MB |
| Client resize | `static/js/image-upload.js` | canvas to 1024 px JPEG 0.85, bypassable by a raw POST |
| Timeout | `vision.identification` | `MAX_UPLOAD_TIMEOUT` around `client.messages().create` |

Cost per identification was sized in the 2026-08-25 cost-controls design at
roughly $0.02 to $0.06. With the seeded rules the application-level worst case
is 50 × $0.06 ≈ $3 per day and 650 × $0.06 ≈ $39 for the monthly window.

### 1.5 Client side

No JavaScript calls a vendor host or carries a credential. The identify page
posts a multipart form to the server and includes `image-upload.js` for the
canvas resize. Design direction 2 already holds; §4 adds a header that keeps
it true.

### 1.6 Work in flight that overlaps

The `keyed-rate-limits` worktree (`temp/wt-registration`, uncommitted) adds
`RateLimiter.forKey(String)` to the kernel, a `Resilience4jRateLimiter` that
bounds live keys with LRU eviction, `ClientKey.of(request)` (remote address
only, forwarded headers deliberately not read), and per-client limits on login
and registration. ADR-027 D5 adopts it (with IPv6 keyed on the /64 prefix and
the globals raised to 600 and 60 per minute) as PR 2 of the Cloudflare plan,
after PR 1 gives the app the real client address. Both land before this
plan's step 5; this plan does not re-specify them.

The Cloudflare plan also writes the "client identity behind a proxy" section
of `apps/CLAUDE.md`, the `server.tomcat.remoteip.*` properties and the edge
runbook `docs/runbooks/cloudflare-edge.md`; this plan's step 8 points at them
rather than duplicating them.

---

## 2. Threat model, briefly

Primary: abuse of the endpoints that spend, by verified accounts scripting
`POST /insects/identify`, by sock-puppet accounts minted through self-serve
registration, and by oversized or concurrent uploads that exhaust memory before
any budget is checked. Secondary: disclosure of the key through logs, error
pages, config artifacts, a client bundle, or a pre-rewrite clone.

What bounds the primary threat is §4, outermost layer first. What bounds the
secondary threat is §3 plus §5.

---

## 3. Port and adapter design

### 3.1 Modules and the DAG

```
kernels/framework
  com.naturalist.secrets
    SecretProvider            port:   Secret require(SecretName); Optional<Secret> lookup(SecretName)
    SecretName                typed, kebab-case, e.g. SecretName.of("model-credential")
    Secret                    value; toString() -> "Secret[<fingerprint>]"; fingerprint() = sha256 hex[0..8)
    MissingSecretException    names the SecretName and the source that failed
    EnvironmentSecretProvider name -> UPPER_SNAKE env var ("model-credential" -> MODEL_CREDENTIAL)
    FileSecretProvider        <dir>/<name>, trailing newline trimmed
                              (systemd $CREDENTIALS_DIRECTORY, Docker/Kubernetes/Cloud Run mounts)

adapters/anthropic-vision            -> framework, vision          (unchanged edges)
adapters/anthropic-text-generation   -> framework, vision, text-generation (unchanged edges)
adapters/secrets-<provider>          -> framework + vendor SDK     (only if a host needs it; none now)

apps/management-console
  SecretsConfiguration   naturalist.secrets.source = environment | file[:dir] | <adapter name>
  VisionConfiguration    naturalist.vision.provider = none | anthropic   (fail closed on anthropic + missing key)
```

No new edge enters a domain module. `framework` keeps its allow-list (JDK only
for the new package). The DAG rule "api modules depend only on framework…"
is untouched because no domain sees `SecretProvider`; only adapters and the
composition root do. `kernels/vision` is unchanged.

### 3.2 Why a separate port rather than folding into `VisionService`

`VisionService` is the port the domain calls; the Anthropic adapter is the
consumer of the secret. Folding credentials into `VisionService` would make
the domain aware of authentication, and two adapters share the one key.
A separate port also serves later secrets the console already takes from
Spring properties (SMTP password, datasource password) without touching the
vision seam.

### 3.3 Construction and selection

- `AnthropicVisionService(AnthropicVisionConfig, SecretProvider, Resilience)`
  resolves `secrets.require(CREDENTIAL)` once in the constructor and builds the
  client with `.apiKey(secret.value())`. Same for the text adapter.
- `SecretsConfiguration` builds the one `SecretProvider` bean from
  `naturalist.secrets.source`. Unknown value fails boot.
- `VisionConfiguration` switches on `naturalist.vision.provider`. `none` gives
  `NoOpVisionService`; `anthropic` constructs the adapter, and a
  `MissingSecretException` surfaces as a boot failure whose message names the
  property, the secret name and the source. Observed like `EmailConfiguration`
  so a `none` deployment shows as a standing violation on the dashboard.
- **No default.** Main `application.yml` does not set the property, with the
  same comment the admin credentials carry; a missing value fails boot naming
  it. Test `application.yml` sets `none`; the README run command gains
  `--naturalist.vision.provider=none`, beside the admin flags it already
  passes. This is the `log-only` precedent: a forgotten setting must not
  silently degrade a deployment.
- Later: `naturalist.vision.credential = api-key | federation`. With
  `federation` the adapter builds the client through the SDK's own
  Workload Identity Federation detection (`ANTHROPIC_FEDERATION_RULE_ID`,
  `ANTHROPIC_ORGANIZATION_ID`, `ANTHROPIC_SERVICE_ACCOUNT_ID`,
  `ANTHROPIC_IDENTITY_TOKEN_FILE`), none of which is a secret; the host
  supplies the identity token. The secret port is not involved on that path.

### 3.4 Gates

- `NoSystemGetenv` (OpenRewrite, `tooling/naturalist-rewrite`, in the
  `EnforceArchitecture` composite so `mvn rewrite:dryRun` covers every module,
  including `adapters/anthropic-text-generation`, which is not on the app
  classpath): fails on any `java.lang.System.getenv` invocation outside
  `com.naturalist.secrets`. One exemption, declared in the recipe with its
  reason: `kernels/persistence-test`'s `RdbmsDataSource`, which reads
  `NATURALIST_TEST_JDBC_URL` / `_DB_USER` / `_DB_PASSWORD` to reach the standing
  test database. That is the CI contract in `ci.yml`, not a deployed secret,
  and the URL and user are not secrets at all, so forcing them through the
  port would be a misuse of it. The exemption is scoped to that one class.
- `SecretsComplianceTest` (ArchUnit, `apps/management-console/.../architecture/`):
  no class on the app classpath outside `com.naturalist.vision.anthropic`
  depends on `com.anthropic..`. (The text adapter is not on that classpath;
  the same rule gains its package when it is wired.)
- `Secret` has no `value()` accessor on the kernel's public surface beyond a
  single `String value()` that the adapters call; `equals`/`hashCode` are
  by value, `toString` is the fingerprint form, and there is no Jackson
  serializer (a `Secret` can never be written by the persistence layer).

---

## 4. Abuse protections, outermost first, and where each plugs in

| # | Layer | Mechanism | Where | Status |
|---|---|---|---|---|
| 1 | Vendor ceiling | Dedicated Anthropic workspace for this deployment; monthly spend limit set in the Console; API key scoped to that workspace; workspace rate limits as the Console sets them | Anthropic Console (operational, no code) | New |
| 2 | Durable budgets | `UsageCounter` rules: global monthly, global daily, per-naturalist daily; reserved before any vision call | `domains/usage`, `InsectIdentificationCommandImpl` 0b | Exists |
| 3 | Throughput | Global `vision.identification` rate limit 3/min (exists); add a **bulkhead** of 2 concurrent identifications, zero wait. The permit is taken in the core beside the rate gate (step 0a) and **before** `budget.reserve` (0b), around the whole identification, so a rejection burns no budget and counts identifications, not vision turns. Rejection → kernel `BulkheadFullException` → `IdentificationRefused.Reason.BUSY` → `redirect:/insects/identify?limit=BUSY` | `kernels/framework` (exception), `kernels/identification` (reason), `InsectIdentificationCommandImpl`, `ResilienceConfiguration`, `InsectsController` | New |
| 4 | Per-client | Login and registration: ADR-027 D5 (`auth.login.client` 10/min, `auth.register.client` 3/min, Cloudflare plan PR 2). This plan adds `IdentifyRateLimitFilter` on `POST /*/identify`, strategy `vision.identification.client`, keyed by `ClientKey`, at **2/min per address**, deliberately below the 3/min global so one address cannot hold every shared slot; built like `LoginRateLimitFilter`, lands after Cloudflare PR 1 (before it every request keys to the connector). Once the bulkhead exists the global limit can be raised (6/min is the suggested next value); that is a dial, not part of this plan. Candidate for Pat: add `/insects/identify` to the path set of edge rule E1 (one rule on Free, path-only) so the flood valve covers the spending route too | `apps/management-console/.../auth/`, Cloudflare E1 | Cloudflare plan + new |
| 5 | Request size | Adapter downscales every image the JDK can decode (JPEG, PNG, GIF) to longest edge 1568 px, JPEG, before base64, so upload size never reaches the wire or the token count for those formats. WebP (accepted by `ImageUpload`, undecodable by `ImageIO`) passes through unchanged, bounded only by `ImageUpload.MAX_BYTES`; an oversize WebP is rejected by the vendor and surfaces as a failed identification, one budget unit spent — accepted, because the honest path (`image-upload.js`) always sends JPEG, and the one-line fix if it ever matters is lowering `MAX_BYTES` to 5 MB. No image library is added; EXIF orientation on raw uploads remains parked fork PL-17. Multipart ceiling stays 21/22 MB | `AnthropicVisionService` (vendor limit, so vendor adapter) | New |
| 6 | Authority | `VISION` on identify routes (exists); keep `grant-policy: self-serve` for launch, with `request` as the one-line fallback if sock-puppet accounts appear; layer 2's global ceilings are what bound puppets | `SecurityConfiguration`, `AccountConfiguration` | Exists |
| 7 | Client containment | `Content-Security-Policy: connect-src 'self'; form-action 'self'; frame-ancestors 'none'` so page scripts cannot reach a vendor host and forms cannot post elsewhere. **Not** `default-src 'self'`: the templates use inline `<style>` (login, register, verify), inline `<script>` (description, ancestorIntros, glossary list) and inline handlers (description tabs, guild select), and htmx injects styles; a strict policy is a template effort of its own. The Turnstile widget (ADR-027 D6) loads a script and a frame from `challenges.cloudflare.com`; `script-src` and `frame-src` are unrestricted under this policy, so it is unaffected (if its docs turn out to need `connect-src` as well, add that one host). Plus `X-Content-Type-Options: nosniff` and the existing `Referrer-Policy: no-referrer` | `SecurityConfiguration.headers` | New |
| 8 | Proxy identity | Decided by ADR-027 D3/D4: origin reachable only through a Cloudflare Tunnel connector; `server.forward-headers-strategy: native` with `remote-ip-header: CF-Connecting-IP`, trusted only from Boot's default `internal-proxies` (the local connector). Nothing host-specific is left for this plan | Cloudflare plan PR 1 | Cloudflare plan |

Where the sizing comes from: layer 1 is set to the layer-2 worst case plus
headroom (650 × $0.06 ≈ $39; set $60 and review monthly). Layer 3's bulkhead of
2 is one more than a single honest user needs. Layer 4's 2/min per address is
what one honest user can actually do (an identification takes ten to twenty
seconds) and leaves one global slot a minute for everyone else.

Enforcement stays in the core where the core already has the identity:
budgets and the rate gate key on `NaturalistName` inside the use case
(design principle 1 of the 2026-08-25 cost-controls design). Per-address keys
are a surface concept and live in the console as filters, exactly as
`LoginRateLimitFilter` does.

---

## 5. Secret store options by hosting target

Every candidate can inject a secret as an environment variable or a file, so
the two kernel sources cover all of them. The columns that differ are rotation
mechanics and whether the host issues an OIDC identity token, which is what
makes WIF possible later. Verify each "WIF" cell against the host's current
documentation at setup time.

| Host | Secret injection | Source property | Rotation mechanics | WIF later | Notes |
|---|---|---|---|---|---|
| Fly.io | `fly secrets set NAME=…` → env, machines restart | `environment` | set secret, rolling restart, smoke identification | Likely: Fly Machines mint OIDC tokens (issuer `oidc.fly.io/<org>`) | Cheapest managed path for a Spring Boot jar plus Postgres; forwarded headers from Fly proxy |
| Render | env vars / secret files, env groups; redeploy on change | `environment` or `file` | edit, redeploy | No | Simple; no identity token |
| Railway | variables → env; redeploy | `environment` | edit, redeploy | No | Simple; no identity token |
| AWS ECS Fargate / App Runner | Secrets Manager or SSM injected as env from the task definition | `environment` | update secret version, force new deployment | Not on ECS/App Runner (no OIDC JWT for tasks); yes on EKS via projected service-account tokens | A `secrets-aws` adapter only if hot rotation is wanted |
| GCP Cloud Run | Secret Manager as env or mounted file, versioned | `environment` or `file` | add version, new revision | Yes: metadata server issues identity tokens for the service account | `file` source reads the mounted version |
| Azure Container Apps | Key Vault reference → env | `environment` | new version, revision | Likely: managed identity federated token | |
| Bare VPS (Hetzner etc.) + systemd | `LoadCredentialEncrypted=` → `$CREDENTIALS_DIRECTORY/model-credential` | `file:$CREDENTIALS_DIRECTORY` | re-encrypt, `systemctl restart` | No | Cheapest, most operator burden (TLS, Postgres, backups) |
| Kubernetes (any) | Secret → env or mount | `environment` or `file` | update Secret, rollout | Yes if the cluster's issuer is publicly discoverable | |

ADR-027 D3 changes one thing in this table: with a Cloudflare Tunnel the
origin accepts no inbound traffic at all, so a host needs no public ingress,
and a home server behind NAT becomes a candidate alongside the VPS row. The
tunnel token is a third secret for the chosen host's injection mechanism, and
the `environment` / `file` sources carry it like the others.

**Recommendation (my call, revisable):** Fly.io first, GCP Cloud Run second.
Both keep the `environment`/`file` sources, both have a credible WIF path, and
Fly.io is the smaller operational surface for one Spring Boot jar with a
Postgres next to it. The first four steps of §7 are identical whichever host
is chosen, so the choice does not block them.

**Anthropic-side setup, host-independent:** create workspace
`naturalist-prod` in the Console; set its monthly spend limit (layer 1); create
one API key scoped to it, named by date (`naturalist-prod-2026-11-xx`); keep
the dev key in a separate `naturalist-dev` workspace with its own small limit.
The Admin API (`client.beta.organization.api_keys` list/update, workspace rate
limits) can deactivate a key and read limits; key creation is Console-only.

---

## 6. Key rotation runbook

The live version is [`docs/runbooks/secret-rotation.md`](../runbooks/secret-rotation.md);
follow that one. This is its first draft, corrected where `/admin/secrets` was
withdrawn. Fingerprint = first eight hex digits of SHA-256 of the key:

```bash
printf %s "$KEY" | shasum -a 256 | cut -c1-8
```

**Routine rotation** (quarterly, on operator change, after any rehearsal):

1. Console → workspace `naturalist-prod` → create key `naturalist-prod-YYYY-MM-DD`. Copy it once.
2. Compute the fingerprint locally. Record date, key name and fingerprint in the runbook log. Never record the value.
3. Update the store with the host's command (table in §5). Do not deactivate the old key yet.
4. Restart or redeploy.
5. Smoke: one identification in the deployment with a known photo. It counts against the budget; that is fine.
6. Console (or Admin API `api_keys.update status=inactive`) → deactivate the old key.
7. Smoke again. A pass proves the new key is the live one; a failure means the deployment is still on the old one, so reactivate it and go back to step 3. Delete the old key after seven days.
8. Close the log entry with the old key's deactivation time.

**Emergency rotation** (suspected leak): deactivate the old key first, accept
minutes of identification outage, then run steps 1 to 5 and 8. Afterwards read
the workspace usage in the Console for anomalous spend and the per-naturalist
counters at `/admin/usage` for the source.

**Rehearsal:** run the routine procedure end to end in the pre-public
environment before the first public deployment, timing each step and noting
every observation that did not match the runbook. The first production
rotation then retires the pre-publication key (§1.2).

**Rotation without a restart** is deliberately not built: every host in §5
restarts on a secret change anyway, and a re-resolve on `authentication_error`
is the one-step addition if that ever changes.

---

## 7. Implementation sequence

Each step is one PR-sized slice (ADR-019). Every step ends with the full gate:
`mvn verify` and `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`.
The task-level plan (TDD steps, file lists) is written after this document is
approved.

| Step | Work | Tests that prove it |
|---|---|---|
| 0 | **Vendor ceiling, operational.** Create `naturalist-prod` and `naturalist-dev` workspaces, set spend limits, create scoped keys, record fingerprints. Rotate away from today's key (§1.2). | Manual: one identification in dev with the new dev key; the old key deactivated in the Console; fingerprints logged in the runbook draft. |
| 1 | **Cloudflare plan PRs 1 and 2 land** (not this plan's work): client address behind the connector, then the `keyed-rate-limits` worktree as PR 2 (`RateLimiter.forKey`, `Resilience4jRateLimiter` keyed/LRU, `ClientKey` with IPv6 /64 keying, per-client login and registration limits). Note for whoever lands it: the worktree branches from `bf0c34fe` and `main` is 43 commits ahead, including insects-ports changes to `InsectIdentificationCommandTest` that the worktree also edits, so it is a rebase, not a fast-forward. | That plan's tests. This plan's step 5 depends on `ClientKey` and `forKey` existing and on `getRemoteAddr()` being the visitor. |
| 2 | **Kernel secrets package.** `SecretProvider`, `SecretName`, `Secret`, `MissingSecretException`, `EnvironmentSecretProvider`, `FileSecretProvider` in `kernels/framework`. | `SecretTest`: `toString` is `Secret[<fp>]`, fingerprint is 8 hex and stable, equality by value, blank value refused. `SecretNameTest`: kebab-case only, env mapping `model-credential → MODEL_CREDENTIAL`. `EnvironmentSecretProviderTest`: injected `Map` lookup for present / absent / blank → `MissingSecretException` naming the name and the source. `FileSecretProviderTest`: temp dir, trailing newline trimmed, missing file, unreadable file. |
| 3 | **Adapters consume the port.** Both Anthropic adapters take `SecretProvider`; `System.getenv` removed; `NoOpVisionService` message reworded. | New `AnthropicVisionServiceTest` and `AnthropicTextGenerationServiceTest` (first tests in those modules): construction with a stub provider succeeds without network; missing secret → `MissingSecretException`; `buildTool` schema mapping (already pure) gets a direct test while the module is open. |
| 4 | **Composition root selects by property.** `SecretsConfiguration` (`naturalist.secrets.source`), `VisionConfiguration` on `naturalist.vision.provider` with no default, fail closed, observed; test `application.yml` sets `provider: none`; `NoSystemGetenv` recipe in `EnforceArchitecture` with the one scoped exemption; `SecretsComplianceTest` ArchUnit for `com.anthropic` imports. | `VisionConfigurationTest` (context slices): `none` → `NoOpVisionService`; `anthropic` + stub secret source → `AnthropicVisionService`; `anthropic` with no secret → context fails, message names property, secret and source; property absent → context fails naming the property; unknown `secrets.source` → boot fails. Recipe test in `tooling/naturalist-rewrite` (the existing recipe-test pattern): a `System.getenv` in a scratch class is flagged, one in `com.naturalist.secrets` is not, `RdbmsDataSource` is not; repo-wide `rewrite:dryRun` green. `SecretsComplianceTest` green on the tree. All `@SpringBootTest`s green with `MODEL_CREDENTIAL` exported in the shell. |
| 5 | **Request-size, concurrency and per-client limits.** Kernel `BulkheadFullException` beside `RateLimitExceededException`; `IdentificationRefused.Reason.BUSY`; bulkhead permit (`vision.identification`, 2, zero wait) taken in `InsectIdentificationCommandImpl` around the whole identification, after the rate gate and before `budget.reserve`; controller maps `BUSY` to `?limit=BUSY` and the identify page says so; adapter downscale of decodable images to 1568 px JPEG before base64; `IdentifyRateLimitFilter` (strategy `vision.identification.client`) keyed by `ClientKey` on `POST /*/identify` at 2/min; CSP (`connect-src`, `form-action`, `frame-ancestors`) and `nosniff` headers. | Bulkhead in core: a `Resilience` stub whose bulkhead rejects → `identify` throws `IdentificationRefused(BUSY)`, `budget.reserve` never called, `imageStore.store` never called, vision never called (extends `InsectIdentificationCommandTest`'s existing rate-gate case). Controller: `BUSY` → redirect with `limit=BUSY`. Downscale as a package-private pure function in the adapter: a generated 4000×3000 PNG → longest edge 1568, JPEG; an 800 px JPEG → unchanged bytes; a WebP byte array → returned unchanged. Filter: third POST from one address in a minute → `303` to `/<domain>/identify?limit=RATE` (as built; the upload script follows redirects, so a `429` would only reload the form), a second address unaffected, global limit still enforced. Headers: MockMvc asserts the exact CSP value and `X-Content-Type-Options` on `/insects/identify` and `/`, and a rendered `/login` and `/insects/species` still carry their inline style and script blocks (the policy must not have touched them). |
| 6 | **Rotation support.** `docs/runbooks/secret-rotation.md` with the §6 text and a log table. The `/admin/secrets` view planned here was built and then withdrawn (ADR-028, rejected alternatives): no UI or log shows a secret's source, path or fingerprint. | Behaviour, not a page: the runbook's smoke identification after the old key is deactivated proves which key is live. |
| 7 | **Rehearsal and docs.** Run the runbook once end to end against the pre-public deployment; fix what the rehearsal exposes; README env-var section → properties; `.env*` in `.gitignore`; `work-tracker.md` row (the `apps/CLAUDE.md` proxy section is Cloudflare PR 1's). | The rehearsal log in the runbook with timings and the fingerprints before and after; `mvn verify` green with no key in the environment; CI unchanged and green. |
| 8 | **Deploy configuration** (after the host is chosen). The store command for the chosen host, `naturalist.secrets.source`, `naturalist.vision.provider=anthropic`, and the Turnstile secret and tunnel token through the same source; forwarded-header trust and the edge checks are the Cloudflare runbook's (`docs/runbooks/cloudflare-edge.md`). | A deployed smoke identification; `/admin/resilience` lists `vision.identification` with its bulkhead and `vision.identification.client`; the forged-header check: from one machine, send `POST /login` through the public hostname twenty-five times, one every two seconds (slow enough to stay under Cloudflare rule E1), with a valid session's CSRF token (`CsrfFilter` otherwise answers `403` before the limiter) and a different forged `CF-Connecting-IP` each time. The requests must share one client bucket (ten a minute), so at least one is refused with the app's `429` ("Too many login attempts"), even if a one-minute cycle resets partway through. If the forged header were honoured, each would get its own bucket and none would be refused. Limiters log nothing, so that refusal is the evidence. Also an alert rule on `naturalist.invariant.check` violations (`withinRate`, `notBusy`, `withinBudget`, `withinClientRate`) once the host's metrics backend exists. |
| later | **WIF adapter path** once the host issues identity tokens: `naturalist.vision.credential=federation`, federation issuer and rule created in the Console, the static key deactivated. | Boot with federation env vars and no `model-credential` secret → adapter constructs through the SDK's federation path; a smoke identification; the static key shown inactive in the Console. |

Dependencies: 0 is independent of code. 1 (the Cloudflare plan's PRs 1–2)
before 5. 2 before 3 before 4 before 5, 6. 7 after 6. 8 after the host
decision and after the Cloudflare plan's PR 7. The vendor ceiling (0) should
be in place before any public traffic regardless of where the code is. Steps
2–4 are independent of the Cloudflare chain and can run alongside it.

---

## 8. Calls settled after the builder review (2026-11-02)

A fresh reviewer planned from these documents and came back with six places
where a wrong guess would matter. Each was settled from a precedent already in
the repo; Pat can overturn any of them.

1. **CSP scope.** `connect-src` / `form-action` / `frame-ancestors` only;
   `default-src 'self'` would break the inline styles and scripts the
   templates use today. Stricter CSP is its own template effort.
2. **Bulkhead placement.** In the core beside the rate gate and before the
   budget reservation, around the whole identification, with a kernel
   exception and a `BUSY` refusal. The adapter position would burn a budget
   unit per rejection and count vision turns instead of identifications.
3. **Per-address identify limit.** 2/min, below the 3/min global, so one
   address cannot hold every shared slot. 5/min above the global would never
   have protected anyone.
4. **Undecodable formats.** No image library. JPEG/PNG/GIF are downscaled;
   WebP passes through under `ImageUpload.MAX_BYTES`; EXIF orientation stays
   PL-17.
5. **`System.getenv` gate scope.** Repo-wide via an OpenRewrite recipe in
   `EnforceArchitecture` (one console ArchUnit test cannot see the text
   adapter), with a single scoped exemption for the standing-test-database
   wiring in `kernels/persistence-test`, declared with its reason in the
   recipe. This is designing the gate's scope up front, not weakening it to
   pass.
6. **`naturalist.vision.provider` default.** None; a missing value fails boot.
   Matches `log-only` and the admin credentials. Local runs pass
   `--naturalist.vision.provider=none`.
7. **Overlap with ADR-027 (found after the review).** This plan defers to the
   Cloudflare ADR on client identity, the login/registration limiters and
   Turnstile, and keeps only the identify-route limiter, the bulkhead, the
   image bound, the CSP and everything about the credential. The two secrets
   ADR-027 adds move onto `SecretProvider` once it exists.

## 9. Open questions for Pat

1. Hosting platform. §5 compares; the plan proceeds through step 7 without it.
2. Spend-limit figure for the `naturalist-prod` workspace. The plan proposes $60/month from the §1.4 worst case; raise or lower it in step 0.
3. Whether `naturalist-dev` should share the production workspace's limit or carry its own small one (the plan assumes its own).
4. Whether to add `/insects/identify` to the path set of Cloudflare rule E1 (§4 layer 4). It is a one-line change to an Accepted ADR's edge rule, so it is Pat's to make, not this plan's.
5. Whether the Turnstile secret key should go through `SecretProvider` from the start (an edit to the Cloudflare plan's PR 5) or be moved after both land (this plan's §3.1 note). The plan assumes the latter.
