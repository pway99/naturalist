# Cloudflare edge + abuse protection — implementation plan

- **Date:** 2026-11-02
- **Decision record:** [ADR-027](../adr/ADR-027-cloudflare-edge-and-abuse-protection.md)
  (read it first; this file only sequences the work and says how each piece is tested)
- **Amends:** [`2026-08-30-accounts-auth-redesign-design.md`](2026-08-30-accounts-auth-redesign-design.md)
- **Status:** PRs 1 and 2 built on branch `keyed-rate-limits` (worktree
  `temp/wt-registration`, on top of `main` at `5f725b0e`), awaiting merge to `main`.
  PRs 3–7 not started.

Fixed inputs: zone `backyardnaturalist.app`, Cloudflare **Free**, hosting undecided
(origin lockdown is a tunnel, which works everywhere — ADR-027 D3).

## PR breakdown (ADR-019: one concern each, dependency order)

| PR | Concern | Depends on | Size |
|---|---|---|---|
| 0 | ADR-027 + this plan + design-doc amendment + index rows | — | docs |
| 1 | **Client address behind the connector** — `server.forward-headers-strategy: native`, `server.tomcat.remoteip.*` (`CF-Connecting-IP`, `X-Forwarded-Proto`), `server.tomcat.redirect-context-root: false` in both the main and the test `application.yml` (the test one replaces the main one); new section "Client identity behind a proxy" in `apps/CLAUDE.md` (the WIP's javadoc already points at it); test below | — | ~150 |
| 2 | **Keyed limiters** — the `keyed-rate-limits` WIP (branched at `bf0c34fe`; rebase onto `main` first): `RateLimiter.forKey` (kernel), `Resilience4jRateLimiter` per-key LRU, `ClientKey`, the `auth.login.client` / `auth.register.client` pair, plus: IPv6 keyed on the /64 prefix; `apps/CLAUDE.md` cross-reference resolved | 1 (deploy order; it compiles alone) | ~300 |
| 3 | **Challenge port** (`kernels/framework`, `com.naturalist.challenge`): `ChallengeVerifier`, `ChallengeSubmission` (VO), `ChallengeVerdict`, `LogOnlyChallengeVerifier`; `kernels/CLAUDE.md` entry | — | ~200 |
| 4 | **Turnstile adapter** `adapters/challenge-turnstile`: `TurnstileChallengeVerifier`, siteverify client on `java.net.http`, response mapping, `@Resilient("challenge.verify")` via the programmatic facade; `adapters/CLAUDE.md` member entry; reactor pom | 3 | ~350 |
| 5 | **Registration wiring**: `ChallengeConfiguration` (fail-closed boot, log-only opt-in, credential via `SecretProvider`, observed), `ChallengeProperties`, the three `challenge.verify` configs in `ResilienceConfiguration`, `RegistrationController.submit` verify-before-register, `register.jte` widget, test yml `log-only: true` | 4; 1 for a meaningful `remoteip` | ~350 |
| 6 | **Login wiring**: `LoginChallengeFilter` after `LoginRateLimitFilter`, `login.jte` widget + `?challenge` / `?unavailable` messages (model attributes from `HomeController.login`) | 5 | ~200 |
| 7 | **Ops runbook + backstop thresholds**: `docs/ops/cloudflare-edge.md` (below), `auth.login` 300 → 600, `auth.register` 10 → 60, work-tracker row closed | 2, 6 (thresholds assume the edge and Turnstile are live) | ~100 |

PRs 1–2 and 3–4 are independent chains and can be worked in parallel; 5 joins
them. Each PR runs `mvn verify` plus the completeness gate
(`mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`).

## Design details the ADR leaves to the builder

- **`ChallengeSubmission`**: `(@Nullable String token, String action, @Nullable String clientAddress)`,
  static `of(...)`; invariants: `action` not blank; `token`, when present, at most
  2048 characters. The controller/filter builds it from the `cf-turnstile-response`
  form field and `request.getRemoteAddr()`.
- **`ChallengeVerdict`**: `(Outcome outcome, List<String> reasons)` with
  `Outcome { PASSED, FAILED, UNAVAILABLE }`; `reasons` are Turnstile's error codes or
  the adapter's own `action-mismatch` / `hostname-mismatch` / `missing-token`.
- **Turnstile adapter**: constructor `(TurnstileConfig config, SecretProvider secrets,
  Resilience resilience)` where `TurnstileConfig(@Nullable expectedHostname, endpoint)`;
  the credential is `secrets.require(SecretName.of("challenge-credential"))`, resolved
  once at construction like the model adapters (ADR-028); the endpoint is a
  constructor input so tests point it at a local stub. Nesting:
  `circuitBreaker.execute(() -> timeout.execute(() -> retry.execute(() -> exchange())))`
  — the 2 s timeout bounds the whole verification; the JDK `HttpClient` request
  timeout is 900 ms per attempt. `exchange()` **returns** a verdict for any 2xx
  body whose `error-codes` are definitive and **throws** for `IOException`, HTTP
  5xx and `internal-error` (that is what the vendor retry sees). Generate the
  `idempotency_key` once per `verify` call with the kernel's UUID generator and
  reuse it across attempts. `remoteip` is the raw `request.getRemoteAddr()`, not
  the /64 `ClientKey`. Observe under `ALWAYS`: `challengePassed`, `actionMatches`,
  `hostnameMatches` (skipped, with a standing `hostnameChecked` violation, when
  `expectedHostname` is blank), `serviceAvailable`.
- **`ChallengeConfiguration`** mirrors `EmailConfiguration`: `challenge-credential`
  resolvable through the `SecretProvider` bean → Turnstile; else `log-only=true` →
  `LogOnlyChallengeVerifier` (standing `turnstile` violation); else
  `IllegalStateException` naming both remedies. `naturalist.challenge.site-key` must
  accompany a resolvable credential (observed `siteKeyConfigured`). Expose the
  site key to templates through a model attribute (`challengeSiteKey`, empty in
  log-only mode → template renders no widget).
- **Templates**: when `challengeSiteKey` is non-empty, render
  `<div class="cf-turnstile" data-sitekey="…" data-action="register">` inside the
  form and `<script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer>`.
  No CSP exists today; if one is added, allow `challenges.cloudflare.com` for
  `script-src` and `frame-src`.
- **`LoginChallengeFilter`**: same construction pattern as `LoginRateLimitFilter`
  (built in `SecurityConfiguration`, not a bean); only `POST /login`; `FAILED` →
  303 to `/login?challenge`; `UNAVAILABLE` → 303 to `/login?unavailable`.
- **Registration refusal**: `FAILED` → re-render with "Please complete the
  verification and try again"; `UNAVAILABLE` → 503, `Retry-After: 30`, re-render
  with "We couldn't confirm your browser just now — please try again in a moment".
- **IPv6 keying** (PR 2): `ClientKey.of` maps an IPv6 literal to its /64 prefix
  (`InetAddress` parse → first 8 bytes → text); IPv4 unchanged; unparseable → the
  literal.

## Coordination with ADR-028 (deployment secrets and model spend protection)

A sibling effort designed the same day
([ADR-028](../adr/ADR-028-secrets-and-model-spend-protection.md),
[plan](2026-11-02-secrets-and-endpoint-protection-plan.md)) defers to this one
on client identity, the login/registration limiters and Turnstile. Agreed split:

- **PRs 1 and 2 here own the `keyed-rate-limits` worktree**; the other plan's step 1
  is "ADR-027 PRs 1 and 2 land". The worktree branched from `bf0c34fe` and `main`
  has moved since, so landing it is a rebase. PR 2 adds IPv6 keyed on the /64
  prefix. **Deploy order: PR 1 before any `ClientKey`-keyed limiter** — before PR 1
  every request keys to the connector's address and a per-client ceiling becomes a
  global lockout. That constraint is recorded in ADR-028 for its own
  `vision.identification.client` limiter (2/min per address).
- **CSP.** ADR-028's policy is `connect-src 'self'; form-action 'self';
  frame-ancestors 'none'` — no `script-src` or `frame-src`, so Turnstile needs no
  allowance under it. If a `script-src` / `frame-src` is ever added, allow
  `https://challenges.cloudflare.com` in both; the widget may also need
  `connect-src https://challenges.cloudflare.com` (verify against Turnstile's CSP
  docs when the policy lands). Whichever PR touches the header second extends the
  same MockMvc header assertion.
- **`/admin/resilience` names.** Both plans register new strategies
  (`challenge.verify` here; the bulkhead and `vision.identification.client` there);
  no collision, but `ResilienceConfigurationTest` is edited by both.
- **Open to Pat (raised by ADR-028, decided here when answered)**: see ADR-027
  "Open items".

## Testing without a Cloudflare account

| Piece | How |
|---|---|
| Keyed limiters (PR 2) | Already in the WIP: `Resilience4jResilienceTest` per-key budgets, independence from the unkeyed budget, blank-key refusal. Add: IPv6 /64 normalization unit test; `RegistrationRateLimitWebMvcTest` sibling that sends two clients and shows the second is untouched by the first's refusal. |
| Client address (PR 1) | `RemoteIpValve` is Tomcat-level, so MockMvc cannot see it. One `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `TestRestTemplate` and a tiny test-only `@RestController` returning `request.getRemoteAddr()` + `isSecure()`: (a) default config, peer is loopback → `CF-Connecting-IP: 203.0.113.9` is honored and `X-Forwarded-Proto: https` makes `isSecure()` true; (b) a second context with `server.tomcat.remoteip.internal-proxies` set to a non-loopback pattern → the same headers are ignored. This proves the trust boundary, not just the rewrite. |
| Challenge port (PR 3) | `ChallengeSubmission` invariant tests (blank action, 2049-char token); `LogOnlyChallengeVerifier` passes a null token and logs. |
| Turnstile adapter (PR 4) | JDK `com.sun.net.httpserver.HttpServer` stub bound to an ephemeral port, endpoint injected: canned `{"success":true,"action":"register","hostname":"backyardnaturalist.app"}` → `PASSED`; `success:false` with each documented error code → `FAILED` with that reason, **no retry** for `invalid-input-response` / `timeout-or-duplicate`; action and hostname mismatch → `FAILED`; HTTP 500 then 200 → `PASSED` on the second attempt with the **same** `idempotency_key` (assert the stub saw it twice); a handler that sleeps 3 s → `UNAVAILABLE` in ≈ 2 s (the outer timeout, not 2 s + retry); five failures → breaker open → `UNAVAILABLE` without a request. Blank token → `FAILED`, zero requests. Metrics asserted on a `SimpleMeterRegistry` as `EmailConfigurationTest` does. |
| Composition (PR 5) | `ChallengeConfigurationTest`, the `EmailConfigurationTest` shape: neither secret nor flag → refuses to boot and both violations counted; flag only → log-only verifier and the standing `turnstile` violation; secret present → Turnstile adapter regardless of the flag. `ResilienceConfigurationTest` gains the three `challenge.verify` names. |
| Registration (PR 5) | `@SpringBootTest` + MockMvc with a `@Primary` stub `ChallengeVerifier` returning a scripted verdict: `PASSED` → redirect `/register?registered`; `FAILED` → 200 re-render with the message, no account created, no email; `UNAVAILABLE` → 503 with `Retry-After`, no account. The existing `RegistrationFlowWebMvcTest` keeps passing on the test yml's `log-only: true`. |
| Login (PR 6) | Same stub: `FAILED` → 303 `/login?challenge` and **no** `AuthenticationSuccessEvent` / `badCredentials` observation (the password was never checked); `UNAVAILABLE` → 303 `/login?unavailable`; `PASSED` → normal login. Order test: a client over `auth.login.client` gets 429 and the stub is never called. |
| Manual browser check (any PR ≥ 5) | Run the console with the Cloudflare test keys (`site-key 1x00000000000000000000AA`, `CHALLENGE_CREDENTIAL=1x0000000000000000000000000000000AA`) against the real siteverify endpoint; then with `2x…` keys to see the refusal; `3x…FF` to see the interactive widget. Needs internet, not an account. |
| Edge rules E1–E5, tunnel | Cannot be tested without the zone. The runbook below carries the post-deploy checks. |

### Owed by PR 5, found while building PRs 3 and 4

- **An overlong token must not become a 500.** The port refuses a token over
  `ChallengeSubmission.MAX_TOKEN_LENGTH` with `InvariantViolationException`, and the
  form field is attacker-controlled. The controller and the filter treat an overlong
  field as a failed challenge before calling the port.
- **Prove the adapter against the real strategy.** An adapter may not depend on another
  adapter, so `TurnstileChallengeVerifierTest` runs on a hand-written `Resilience` double.
  `management-console` has both on its classpath: add a test there that wires the real
  `challenge.verify` strategy and checks the 2 s ceiling and the open breaker through
  Resilience4j, whose timeout and open-state exceptions the double only imitates.
- **The verifier blocks a shared worker pool.** `Resilience4jTimeout` runs every timed
  supplier on one pool sized to the processor count, shared with the other `timeout`
  strategies such as `catalog.fanout` and `image.store`. A slow siteverify can hold a
  worker for up to 2 s per sign-in, and a worker keeps running after its caller has timed
  out, so a login burst could queue those other calls. Size this in PR 5 before wiring:
  either give `challenge.verify` a bulkhead, or give the timeout adapter a dedicated pool.
- **Test keys may not echo the action or hostname.** The manual browser check below has
  not been run, so what the vendor's test keys return is unconfirmed. If they produce
  `action-mismatch`, verify with a real widget; do not relax the check.

## Ops runbook (`docs/ops/cloudflare-edge.md`, written in PR 7)

1. Zone `backyardnaturalist.app` on Cloudflare DNS; SSL/TLS: Always Use HTTPS,
   HSTS, min TLS 1.2 (ADR-027 E5).
2. Tunnel: create in Zero Trust → Networks → Tunnels; install `cloudflared` on the
   origin host (or as a sidecar) with the tunnel token; public hostname
   `backyardnaturalist.app` → `http://localhost:8080`. Firewall / platform ingress:
   no inbound 80/443.
3. Turnstile: one widget, Managed, hostname `backyardnaturalist.app`; set
   `naturalist.challenge.site-key` at deploy and supply `challenge-credential` the way
   `model-credential` is supplied (`docs/runbooks/secret-rotation.md`); never in the
   yml (the `AdminProperties` placeholder trap applies).
4. Security → WAF: rate-limiting rule E1; custom rule E2; Bot Fight Mode on (E3);
   Free Managed Ruleset on (E4).
5. App: `naturalist.email.public-url=https://backyardnaturalist.app`;
   `server.forward-headers-strategy=native` (PR 1 ships it in the main yml).
6. **Checks after deploy**: `curl -sI https://backyardnaturalist.app/login` shows
   `cf-ray`; `curl --resolve backyardnaturalist.app:443:<origin-ip>` and a plain
   `curl http://<origin-ip>:8080/` both fail (lockdown); a `for i in $(seq 12)`
   loop of `curl -o /dev/null -w '%{http_code}\n' https://backyardnaturalist.app/login`
   turns 429 at the edge before the 12th (E1); a scripted `POST /register` without
   `cf-turnstile-response` is refused by the app with the challenge message;
   `/admin/resilience` lists `challenge.verify`, `auth.login.client`,
   `auth.register.client`; the dashboard shows `challengePassed` valid rows (baseline)
   and no standing `turnstile` violation.
7. When the deferred `/api/**` chain gets non-browser clients: turn Bot Fight Mode
   off or scope E2 away from `/api`.
