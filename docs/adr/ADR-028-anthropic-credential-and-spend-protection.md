# ADR-028: Anthropic Credential Supply and Spend Protection

> Companion plan with the repo survey, hosting comparison, rotation runbook and
> sequenced steps:
> [`docs/plans/2026-11-02-anthropic-key-and-endpoint-protection-plan.md`](../plans/2026-11-02-anthropic-key-and-endpoint-protection-plan.md)

Status: Proposed (2026-11-02)

Relates to [ADR-027, Cloudflare at the Edge](ADR-027-cloudflare-edge-and-abuse-protection.md),
written the same day. ADR-027 decides the edge, the tunnel-only origin, how
the app learns the client address (D3, D4), the keyed per-client limiters on
login and registration (D5) and Turnstile (D6). This ADR defers to it on all
of those and covers what it does not: the vendor credential, the routes that
spend against it, and the spend ceilings.

The app spends real money against an Anthropic API key on every identification.
Before it is hosted publicly, the credential is supplied through a kernel port
with swappable adapters, every model call stays server-side, and spend is
bounded in layers so that the worst case has a cost ceiling the operator chose.
The primary threat is abuse of the endpoints that spend, not theft of the key.

### Secrets through a kernel port; sources are adapters

- `kernels/framework` gains `com.naturalist.secrets`: the `SecretProvider`
  port, a typed `SecretName`, a `Secret` value whose `toString()` redacts and
  whose `fingerprint()` is the first eight hex digits of its SHA-256, and
  `MissingSecretException`. This mirrors `com.naturalist.resilience` and
  `com.naturalist.notification`: port in the kernel, vendor in an adapter.
- Two zero-dependency sources ship in the same package, like
  `LoggingEmailSender` and `NoOpResilience`: `EnvironmentSecretProvider`
  (`anthropic-api-key` reads `ANTHROPIC_API_KEY`) and `FileSecretProvider`
  (`<dir>/<name>`, the shape of systemd credentials, Docker/Kubernetes secret
  mounts and Cloud Run secret volumes). They are the "environment-variable
  adapter in the public repo" and cover every candidate host, because every
  candidate host can inject a secret as an environment variable or a file.
- A managed-store SDK adapter (`adapters/secrets-<provider>`, depending on
  `framework` plus the vendor SDK with its version in the root pom) is added
  only if the chosen host cannot inject, or if rotation without a restart is
  ever required. None is built up front.
- The public build has no private dependency. Which source is live is a
  deployment property, `naturalist.secrets.source` (`environment` by default,
  `file`, or a store adapter's name), selected in a composition-root
  `SecretsConfiguration` the way `EmailConfiguration` selects the mail
  transport.
- The port is the home for the other deployment secrets ADR-027 introduces,
  the Turnstile secret key and the tunnel token, as well as the SMTP and
  datasource passwords the console takes from Spring properties today. ADR-027
  passes the Turnstile secret as `naturalist.challenge.secret-key`; once this
  port exists, `ChallengeConfiguration` resolves `turnstile-secret-key` through
  it instead. That is a one-line reconciliation inside ADR-027's registration
  PR, recorded here so neither effort builds a second mechanism.

### The vendor adapters consume the port and nothing else

- `AnthropicVisionService` and `AnthropicTextGenerationService` take a
  `SecretProvider` in their constructors and resolve `anthropic-api-key` once,
  at construction. Neither touches `System.getenv`, and neither uses the SDK's
  own environment lookup (`fromEnv()`).
- The composition root picks the vision provider by property,
  `naturalist.vision.provider` (`none` or `anthropic`), never by whether an
  environment variable happens to be set. The property has no default: a
  deployment that omits it fails boot naming the property, exactly as
  `AdminProperties` and `EmailConfiguration` fail closed. `anthropic` with no
  resolvable key fails boot naming the property, the secret and the source.
  Tests therefore cannot reach the real API by accident, and a production
  deployment cannot silently run the no-op.
- Build-time gates, two of them because one ArchUnit test on the console
  classpath cannot see `adapters/anthropic-text-generation`:
  `NoSystemGetenv`, an OpenRewrite recipe in `tooling/naturalist-rewrite`
  folded into `EnforceArchitecture`, fails the repo-wide dry run on any
  `System.getenv` outside `com.naturalist.secrets`, with one named exemption:
  `kernels/persistence-test`'s standing-test-database wiring
  (`NATURALIST_TEST_*`), which is a CI contract rather than a deployed secret.
  `SecretsComplianceTest` (ArchUnit, beside `ResilienceComplianceTest`) fails
  the build when any class on the app classpath outside
  `com.naturalist.vision.anthropic` depends on `com.anthropic`.

### All model calls stay server-side

The browser posts a multipart form to `POST /insects/identify` and nothing
else; no JavaScript carries or receives a credential, and the only consumers
of `SecretProvider` are the two adapters, which no template can reach. The
app sends `Content-Security-Policy: connect-src 'self'; form-action 'self';
frame-ancestors 'none'` so a future script cannot call a vendor host from the
page even by mistake. Those three directives are chosen because they do not
touch inline scripts or styles, which the templates use today, nor the
Turnstile script and frame ADR-027 D6 loads from `challenges.cloudflare.com`
(`script-src` and `frame-src` stay unrestricted). A stricter `default-src
'self'` is a separate template effort, not part of this decision.

### Spend is bounded in layers, outermost first

1. A dedicated Anthropic workspace for this deployment, with a monthly spend
   limit set in the Console and a key scoped to that workspace. This is the
   ceiling that holds when every layer below is wrong.
2. The existing durable budget rules in `domains/usage` (global monthly,
   global daily, per-naturalist daily), reserved in
   `InsectIdentificationCommandImpl` before any vision call.
3. The existing global `vision.identification` rate limit, plus a bulkhead of
   two concurrent identifications. The permit is taken in
   `InsectIdentificationCommandImpl` beside the rate gate and before the
   budget reservation, around the whole identification, so a rejected caller
   burns no budget and the count is of identifications, not vision turns. A
   full bulkhead surfaces as the kernel `BulkheadFullException` and the
   `IdentificationRefused.Reason.BUSY` refusal, never a vendor type or a 500.
4. Per-client (remote address) limits. Login and registration are ADR-027 D5
   (`auth.login.client`, `auth.register.client`). This ADR adds
   `vision.identification.client` on the identify routes, keyed through the
   same `RateLimiter.forKey` and `ClientKey`, at two a minute per address,
   below the three-a-minute global so one source cannot take every shared
   slot. Client identity is ADR-027 D4: `getRemoteAddr()` rewritten by
   Tomcat's `RemoteIpValve` from `CF-Connecting-IP`, trusted only from the
   local tunnel connector; nothing in app code reads a forwarded header. For
   the reason D5 gives, the identify limiter lands after that client-address
   change, or every request keys to the connector and two a minute becomes a
   global lockout.
5. Request-size limits: the vendor adapter downscales every image the JDK can
   decode (JPEG, PNG, GIF) to the vendor's effective maximum, longest edge
   1568 px as JPEG, before base64, so upload size never reaches the wire or
   the token count for those formats; WebP, which `ImageIO` cannot decode,
   passes through unchanged and is bounded only by `ImageUpload.MAX_BYTES`.
   No image library is added. EXIF orientation on raw uploads stays the
   parked fork PL-17. The servlet multipart ceiling stays just above
   `ImageUpload.MAX_BYTES`.

Identification stays behind sign-in and the `VISION` authority. Anonymous
identification is not offered.

### Rotation is a rehearsed procedure, supported by code

The adapters log the live key's source and fingerprint at boot, and
`/admin/secrets` shows the same, so an operator can prove which key is live
without ever seeing a value. Rotation is: create the new key in the same
workspace, update the store, restart, confirm the fingerprint, smoke one
identification, deactivate the old key. The runbook is rehearsed once before
the first public deployment and then quarterly. The first real rotation
retires the pre-publication key, because the 2026-09-26 history rewrite cannot
prove what earlier clones hold.

### Rejected alternatives

Recorded so they are not reintroduced.

- **Encrypted environment or startup variable, decrypted by a private module
  excluded from the public repo.** The decryption key must also be present at
  runtime, so the root of trust moves rather than disappears; the scheme
  depends on secrecy of mechanism; and the public build no longer runs for a
  contributor. Rejected.
- **Let the Anthropic SDK read `ANTHROPIC_API_KEY` itself** via
  `AnthropicOkHttpClient.fromEnv()`. Invisible coupling to the process
  environment, no way to swap the source by configuration, no fingerprint to
  verify a rotation, and tests cannot be isolated from a developer's shell.
  Rejected.
- **A `${ANTHROPIC_API_KEY}` placeholder in `application.yml`.** An unresolved
  placeholder binds as the literal string and is non-blank, the trap already
  documented for the admin credentials; and the config file becomes a
  secret-bearing artifact. Rejected.
- **A cloud vendor's Spring Boot secrets starter as the abstraction.** The
  port would then be a vendor type in the composition root; the kernel must
  own the port so sources are swappable and the public build stays free of
  private or vendor dependencies. A starter may still be wrapped behind the
  port as an adapter. Rejected as the port, allowed as an adapter.
- **Short-lived credentials proxied to the browser so it calls the vendor
  directly.** A credential reaches the client, and budget enforcement leaves
  the core use case. Contradicts the server-side rule. Rejected.
- **Fetching the secret from a store on every request.** Adds a network hop
  and a store rate limit to each identification, and a store outage becomes
  an identification outage. Resolve once at construction; add a re-resolve on
  authentication failure if hot rotation is ever wanted. Rejected.
- **Building SDK adapters for every candidate cloud up front.** Every
  candidate host injects secrets as environment variables or files, which the
  two kernel sources already read. Build a store adapter when a host that
  cannot inject is chosen. Rejected for now.
- **Encrypting the key at rest in the app's own database.** The same
  root-of-trust move as the first alternative, with a database write path
  added. Rejected.
- **Parsing `X-Forwarded-For` in app code for per-IP limits.** Any client can
  send the header; it is not an identity. Trust named proxies at the
  deployment layer only. Rejected here and in ADR-027.
- **A second secret mechanism for the Turnstile key and tunnel token.**
  ADR-027 passes the Turnstile secret as a Spring property; keeping that beside
  `SecretProvider` would give the deployment two ways to carry a secret, two
  places to rotate and one that cannot report a fingerprint. Rejected: those
  secrets move onto the port as soon as it exists.
- **Workload Identity Federation now.** It retires the static key entirely and
  is the intended successor on a host that issues OIDC identity tokens, but it
  depends on the host choice, which is open. Designed in as a later adapter
  path (`naturalist.vision.credential=federation`, using the SDK's own
  federation environment), sequenced after hosting is live. Deferred, not
  rejected.
- **Anonymous identification with per-IP budgets.** Decided against for this
  deployment: identification stays behind sign-in and `VISION`. Deferred.

### Review flags

- Any `System.getenv` outside `com.naturalist.secrets`.
- Any `com.anthropic` import outside `adapters/anthropic-*`.
- A secret value in a log line, an exception message, a template model or an
  admin page; only `Secret.fingerprint()` is ever displayed.
- An adapter chosen by environment-variable presence rather than a property.
- A new route that reaches `VisionService` or `TextGenerationService` without
  passing through `InsectIdentificationCommandImpl`'s rate gate, bulkhead and
  budget reservation, or without the `VISION` authority.
- A resilience permit taken after `budget.reserve`, so a rejection burns a
  budget unit.
- A `naturalist.vision.provider` or `naturalist.secrets.source` default added
  to `application.yml`.
- A rotation performed without recording the new fingerprint in the runbook
  log and deactivating the old key.
