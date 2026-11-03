# Security review — 2026-11-03

Scope: the uncommitted ADR-028 secrets slice (tasks 1–7) plus the exposed surface of
`apps/management-console`. A first pass listed candidates; each one was then checked by a
separate reviewer, and only findings rated confidence ≥ 8 would count as vulnerabilities.

**Result: no vulnerabilities at or above that bar.** Three candidates were filtered out, but
two of them are worth a decision before public hosting (the action items below).

## Action items (pre-hosting)

- [ ] **S1 — Unverified accounts can squat a victim's email** (reviewer confidence 5, Low).
  `RegistrationController.submit` (`apps/management-console/.../account/RegistrationController.java:90-130`)
  registers any email and auto-logs the registrant in; `POST /verify` (`:155-164`, permitAll)
  redeems the token for whoever submits it, and `AccountCommandImpl.verifyEmail`
  (`domains/accounts/accounts-core/.../AccountCommandImpl.java:115-130`) marks the token's
  account verified with no check that the caller owns it. If an attacker registers
  `victim@example.com`, then a victim who clicks through and presses "Confirm my email" verifies the
  attacker's account, and the victim can no longer register ("already registered"). The attacker
  gains no privilege, because self-serve already grants `VISION` to any registrant. The harm is
  a targeted lock-out with no self-serve remedy: no `/reset` controller exists (the `/reset/**`
  permitAll is dangling) and unverified accounts never expire. Options, cheapest first:
  - add "if you didn't sign up, don't confirm" wording to `verify.jte:75-82`;
  - purge never-verified accounts once the 24h token TTL lapses, which frees the address;
  - ship the reset slice, with its designed expire-all-sessions-on-reset step, so the real
    owner can take the address back.
- [ ] **S2 — Session cookie not `Secure` / HSTS idle behind the tunnel** (reviewer confidence
  2, Informational). Behind the tunnel the origin sees plain HTTP: no `server.*` config, and
  `SecurityConfiguration.java:55-56` sets only Referrer-Policy. Already scheduled as
  [Cloudflare plan](../plans/2026-11-02-cloudflare-edge-protection-plan.md) PR 1 (ADR-027 D4).
  The `.app` TLD is HSTS-preloaded, so there is no plaintext window in practice. Land PR 1
  before the first deploy; `server.servlet.session.cookie.secure: true` is a cheap
  belt-and-braces alongside it.

## Accepted (no action)

- **Email enumeration on `POST /register`** (`RegistrationController.java:101-104`, reviewer
  confidence 2). This is a deliberate trade-off. Anti-enumeration is scoped to password reset
  (`AccountCommandImpl.java:181-182`), and registration probing is *detected* through the
  `register.emailUnregistered` WARN metric. Revisit only if the threat model changes.

## Verified clean — secrets slice

- No path leaks the secret value. `Secret.toString()` prints only an 8-hex fingerprint, and
  `MissingSecretException`, `FileSecretProvider` and `VisionConfiguration` carry the name,
  source and path only. Neither adapter keeps the `Secret` in a field. `RecordingSecretProvider`
  stores fingerprints, not values.
- `SecretName` is restricted to kebab-case (`^[a-z0-9]+(-[a-z0-9]+)*$`), so file-source path
  traversal is impossible.
- The Anthropic base URL is never set by the app; the SDK defaults to `api.anthropic.com` and
  redacts the credential header in its request logging.
- The `VISION` gate does not depend on which `VisionService` is wired.
- The app has no actuator, so `/env` and `/configprops` are not exposed.
- `System.getenv` is called only in `EnvironmentSecretProvider` and the documented
  `RdbmsDataSource` CI exemption.

## Verified clean — console surface

- **Security chain:** CSRF is on, `/admin/**` requires `ROLE_ADMIN`, and form login has no
  open redirect.
- **Identity:** the current naturalist comes from the security context, not from a client
  header.
- **Field notes:** the notes update checks that the viewer owns the observation (no IDOR).
- **Image store:** `FilesystemImageStore` rejects path traversal, names files by UUIDv7, and
  serves the media type sniffed from the bytes.
- **Templates:** every `$unsafe{}` in JTE is pre-escaped.
- **SQL:** no MyBatis `${}` in main source.
- **Accounts:** tokens are 32-byte `SecureRandom` values, stored as SHA-256, single-use with a
  TTL; passwords are bcrypt.
- **Mail:** links are built from `public-url`, never the Host header, and `LoggingEmailSender`
  stays off unless explicitly opted in.
- **Errors:** Boot's defaults keep stack traces out of responses.

## Not covered

- chemistry SVG depiction internals
- garden, soil and library controllers beyond the mapping inventory
- `adapters/spring-runtime` scan wiring
- `kernels/persistence` transaction wiring
- static assets under `/css`, `/js` and `/images`
- JTE script-context escaping at runtime
