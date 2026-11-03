# ADR-027: Cloudflare at the Edge; Abuse Protection for Login and Registration

- **Date:** 2026-11-02
- **Status:** Accepted (design; implementation plan in
  [`docs/plans/2026-11-02-cloudflare-edge-protection-plan.md`](../plans/2026-11-02-cloudflare-edge-protection-plan.md))
- **Amends:** [`docs/plans/2026-08-30-accounts-auth-redesign-design.md`](../plans/2026-08-30-accounts-auth-redesign-design.md)
  (see its 2026-11-02 amendment)

## Context

The app is being prepared for public hosting at `https://backyardnaturalist.app`
on the Cloudflare **Free** plan; the hosting platform is undecided. Commit `4aa4532`
(merged in `bf0c34f`) added two in-app rate limiters, `auth.login` (300/min) and
`auth.register` (10/min). The kernel `RateLimiter` is name-scoped, so both are
**global**: one client can spend the whole `auth.register` budget and every honest
sign-up is refused for the window. A mail-bombing defense became a denial-of-service
primitive. The limiters are also the only abuse control there is — there is no bot
gate, no per-client control, and the origin is reachable by anyone who finds its
address.

Abuse protection belongs at three layers, each doing what only it can do:

| Layer | Does | Cannot |
|---|---|---|
| Cloudflare edge | TLS, WAF, per-IP flood valve before the origin sees a byte, hides the origin | Tell a human from a scripted browser (needs Turnstile); on Free, count per minute or tell GET from POST |
| Turnstile | Prove a browser solved a challenge, per form submission, rotating-IP-proof | Stop a flood that never submits the form |
| In-app limiters | Precise per-client windows keyed on the real address; a system-wide CPU and mail ceiling; work with no edge in front | Hide the origin; absorb volumetric traffic |

## Decisions

### D1 — The accounts/auth domain stays as built

Cloudflare is a transport and abuse layer in front of the existing `accounts`
domain, Spring Security chain, registration flow and email verification. Nothing
about identity, credentials, entitlement or the student model moves to the edge.

### D2 — Edge configuration (Free plan)

Verified against Cloudflare's docs on 2026-11-02 (rate-limiting availability table,
WAF availability table, Turnstile plans, origin-protection comparison):

| Free plan gives | Limit |
|---|---|
| Rate limiting rules | **1 rule**, counts by **IP only**, **10 s** period, **10 s** mitigation, expression fields **Path and Verified Bot only** (no method, no URI/query/host/body) |
| Custom rules | 5, all actions except Log, no regex |
| WAF managed rules | Cloudflare Free Managed Ruleset only (no Cloudflare Managed Ruleset, no OWASP) |
| Bot Fight Mode | Free; zone-wide, cannot be scoped by path or bypassed by rules |
| Turnstile | Free: 20 widgets per account, 10 hostnames per widget, unlimited verifications, all widget modes |
| Leaked-credentials detection | One field; not used in this design |

The edge rule set:

| # | Control | Setting |
|---|---|---|
| E1 | Rate limiting rule (the one) | Expression: `(http.request.uri.path in {"/login" "/register" "/reset/request"})`. Characteristic: IP. **More than 10 requests per 10 s → Block for 10 s.** Path-only on Free, so GET page loads count alongside POSTs; an honest login (1 GET + 1 POST, three tries) stays well under. This is a flood valve, not the per-client control — see D5. |
| E2 | Custom rule, Managed Challenge | `(http.request.uri.path in {"/login" "/register"} and not cf.client.bot)` — Cloudflare's own sensitive-forms recipe. Interrupts headless clients before the origin sees them; a browser that passes on the GET carries `cf_clearance` into the POST. Known caveat: clearance is bound to the solving IP, so a client whose address changes mid-form is re-challenged. If the challenge-on-POST path ever loses form data in practice, narrow E2 to the GET pages: a bot still needs the page for its CSRF token. |
| E3 | Bot Fight Mode | **On.** Zone-wide and unscoped, which is acceptable while the only clients are browsers; **turn off** (or move to Super Bot Fight Mode on Pro) when the deferred `/api/**` bearer-token chain gets non-browser clients. |
| E4 | WAF managed rules | Free Managed Ruleset on (default). |
| E5 | TLS | Always Use HTTPS on; HSTS on; minimum TLS 1.2. `.app` is an HSTS-preloaded TLD: browsers refuse plain HTTP to it, so `naturalist.email.public-url` **must** be `https://backyardnaturalist.app`. |
| E6 | Turnstile widget | One widget, **Managed** mode, hostname `backyardnaturalist.app`, placed on `/register` (required) and `/login` (see D4). Dev uses Cloudflare's documented test sitekey, which works on any hostname. |

**What Pro would buy** (recorded so the upgrade question is answerable later): 2
rate-limit rules with 1-minute windows, 1-hour mitigation, URI/host/query fields and
Managed Challenge as the rate-limit action — enough to make the edge the primary
per-IP limiter (login: 20/min per IP → challenge; register: 5/min per IP → block
1 h); plus the Cloudflare Managed and OWASP rulesets and Super Bot Fight Mode.
Method matching starts at Business. None of it changes the in-app design.

### D3 — Origin lockdown: Cloudflare Tunnel

The origin accepts traffic only through a `cloudflared` connector; the host firewall
(or platform ingress) allows **no inbound** HTTP/HTTPS at all. Rationale:

- Works on every candidate host: a VPS, a container platform (sidecar), a home
  server behind NAT. The hosting decision is open and this choice does not wait on it.
- Rated "very secure" by Cloudflare, free, no inbound ports, no IP list to maintain,
  no certificates on the origin. The WAF, rate-limit and challenge rules apply to a
  tunnel's public hostname exactly as to any proxied record, and the connector
  forwards `CF-Connecting-IP` / `X-Forwarded-Proto` to the origin.
- The connector reaches the app from localhost or the pod network, which Tomcat's
  default `internal-proxies` pattern already trusts (D4) — no Cloudflare IP regex in
  the app's configuration.

Fallback only if the chosen platform cannot run a sidecar process: an edge Transform
Rule adds a secret request header and a servlet filter refuses requests without it
("HTTP header validation", which Cloudflare rates moderately secure). Record that
choice in a new ADR if it is ever taken.

### D4 — Client address from `CF-Connecting-IP`, trusted only from the connector

Spring Boot's native strategy, Tomcat's `RemoteIpValve`:

```yaml
server:
  forward-headers-strategy: native
  tomcat:
    remoteip:
      remote-ip-header: CF-Connecting-IP
      protocol-header: X-Forwarded-Proto
      # internal-proxies: Boot's default — loopback, RFC 1918, CGNAT, link-local.
      # The cloudflared connector is the only peer the app ever has, and it is local.
    redirect-context-root: false   # TLS ends at the edge; honour X-Forwarded-Proto before redirecting
```

The test `application.yml` replaces the main one wholesale, so the same `server`
block is duplicated there for the trust-boundary test.

The app reads no forwarded header itself (`ClientKey` is `getRemoteAddr()` and
nothing else). Only a peer matching `internal-proxies` can rewrite the remote
address; a direct connection from anywhere else keeps its own address and its
headers are ignored. With D3 there is no such connection. If the origin is ever
fronted by Cloudflare *without* a tunnel, `internal-proxies` must become the
published Cloudflare ranges — one more reason D3 is the default.

What this changes: `getRemoteAddr()` becomes the visitor's address (so the keyed
limiters of D5 key on the visitor, not on the connector); `isSecure()` / scheme
become `https` (secure session cookie, correct redirects; `public-url` is already
config-driven and unaffected); Tomcat's access log, if enabled, records the visitor.
Observed decisions carry **no** address tag — ADR-017's cardinality rule stands —
and the keyed limiters' rejection log lines carry the address at `WARNING`, which is
PII and sets the log-retention expectation. There is no audit log; who did what
remains "a separate decision" as `AuthenticationEventObserver` already says.

### D5 — In-app limiters: keyed per client *and* global, with the global raised

Build the per-client keyed limiters (the `keyed-rate-limits` WIP: kernel
`RateLimiter.forKey`, adapter LRU of per-key limiters, `ClientKey`, the
`auth.login.client` / `auth.register.client` pair taken **before** the global
permit). Recommendation: **yes, ship it**, because on Free the edge cannot count per
minute, cannot tell GET from POST, and lets a 1 request/s credential-stuffer through
its 10-second window; the keyed limiter is the only per-client, per-minute control.
It also keeps working for any deployment with no edge in front. Two additions to the
WIP: key IPv6 clients on their /64 prefix (one subscriber holds the whole prefix and
can rotate inside it for free), and land it **after** D4 — before D4 every request
keys to the connector's address and the 10/min per-client ceiling becomes a global
lockout.

| Strategy | Was | Now | Why |
|---|---|---|---|
| `auth.login.client` | — | 10 / min per client | Generous for a mistyped password, useless for a dictionary |
| `auth.register.client` | — | 3 / min per client | A household or classroom from one address; more is a script |
| `auth.login` | 300 / min | **600 / min** | Backstop only: bounds BCrypt CPU (≈ 10 hashes/s, under one core) against a key-rotating run that beat the edge and Turnstile |
| `auth.register` | 10 / min | **60 / min** | Backstop only: bounds outbound mail to an unproven address to 60/min; a workshop of 30 signing up at once no longer trips it |

### D6 — Turnstile verification: a kernel port, a vendor adapter, fail **closed**

- **Port** in `kernels/framework`, package `com.naturalist.challenge` (sibling of
  `com.naturalist.notification`, the `EmailSender` precedent): `ChallengeVerifier`
  with `ChallengeVerdict verify(ChallengeSubmission)`. `ChallengeSubmission` is a
  `ValueObject` — nullable token of at most 2048 characters (Turnstile's documented
  maximum), required `action` (`register` / `login`), optional client address.
  `ChallengeVerdict` carries `PASSED | FAILED | UNAVAILABLE` plus the reasons.
  The kernel also ships `LogOnlyChallengeVerifier` (passes everything, logs) exactly
  as it ships `LoggingEmailSender`.
- **Adapter** `adapters/challenge-turnstile`: JDK `java.net.http.HttpClient`
  (already in `ResilienceComplianceTest.HTTP_CLIENT_PREFIXES`) + Jackson, POSTs
  `https://challenges.cloudflare.com/turnstile/v0/siteverify` with `secret`,
  `response`, `remoteip` and a fresh `idempotency_key` per verification (kernel UUID
  generator, never `UUID.randomUUID()`); checks the response's `action` and
  `hostname` against the expected values; `expected-hostname` may be left blank, which
  skips the hostname check and carries a standing `hostnameChecked` violation (the
  manual check against the real endpoint with Cloudflare's test keys runs that way).
  A blank token is `FAILED` without a call.
  Vendor placement despite the JDK-only dependency: the endpoint, field names, error
  codes and test keys are Cloudflare's, and swapping the vendor must be one module.
- **Resilience** (`@Resilient(name = "challenge.verify")`, programmatic facade),
  composed **breaker → timeout → retry → HTTP exchange**, in that nesting:
  `TimeoutConfig` 2 s bounds the *whole* verification including any retry (the
  request-path ceiling; siteverify is an edge POST, typically well under 500 ms);
  inside it, `RetryConfig` 2 attempts, 200 ms backoff, with the JDK client's
  per-request timeout at 900 ms so two attempts fit the ceiling. The vendor retry
  retries every exception and has no predicate, so the adapter expresses the policy
  by **returning** definitive verdicts (`invalid-input-response`,
  `timeout-or-duplicate`, action/hostname mismatch → `FAILED`, never retried) and
  **throwing** only for transport failures, HTTP 5xx and `internal-error`; every
  attempt re-sends the same `idempotency_key`. `CircuitBreakerConfig` outermost:
  50 % failure rate, 1.5 s slow-call threshold (below the timeout, so near-limit
  calls count as slow and a timed-out call counts as a failure), 5 minimum calls,
  30 s open. Timeout or open breaker → `UNAVAILABLE`; worst-case latency added to a
  login or sign-up is 2 s, never more.
- **Observed (ADR-017, `MonitoringMode.ALWAYS`)**: every verification emits
  `challengePassed`, `actionMatches`, `hostnameMatches`, `serviceAvailable`; the
  composition root emits `secretConfigured` and a standing `turnstile` violation in
  log-only mode, as `EmailConfiguration` emits `smtp`.
- **Fail closed.** `UNAVAILABLE` refuses the registration (503, `Retry-After: 30`,
  form re-rendered) and the login (redirect to `/login?unavailable`). Rationale:
  (1) the precedent `e9761bf` — a security control that silently degrades is worse
  than one that refuses loudly; (2) fail-open turns any siteverify degradation,
  including one induced by exhausting the app's outbound HTTP capacity, into a
  silent bypass of the one defense per-IP limiting cannot replace; (3) the blast
  radius is small — existing sessions continue, the catalog is anonymous-readable,
  and the breaker bounds refusals to 30-second windows with a probe; (4) the
  violation rate is the operator's signal. The cost accepted: a Cloudflare
  siteverify outage pauses sign-ups and sign-ins, including the config admin's, for
  its duration. Escape hatch for an incident: `naturalist.challenge.log-only=true`
  and restart, which is loud on the dashboard.
- **Opt-in dev/test mode, like `naturalist.email.log-only`**: with
  `naturalist.challenge.secret-key` (and `site-key`) set, the Turnstile adapter is
  wired; with `naturalist.challenge.log-only=true` and no secret, the kernel
  log-only verifier is wired and the templates render no widget; with neither, the
  app **refuses to boot** naming both remedies. The test `application.yml` sets
  `log-only: true`; the main one sets nothing. For a manual browser check against
  the real endpoint, Cloudflare's documented test keys (sitekey
  `1x00000000000000000000AA` always passes, `2x…AB` always fails,
  `3x…FF` forces the interactive challenge; secret `1x…AA` passes, `2x…AA` fails,
  `3x…AA` reports token-already-spent) work on any hostname, including localhost.

### D7 — Where the two checks run

Cheapest check first wherever the code shape allows it.

- **Login**: `LoginRateLimitFilter` (existing) → new `LoginChallengeFilter` →
  `UsernamePasswordAuthenticationFilter`: per-client permit → global permit →
  Turnstile → password check. Failure redirects to `/login?challenge`, the same
  shape as `/login?error`; `/login` is served by `HomeController.login`, which is
  where the site key and the two new messages go.
- **Registration**: `RegistrationController.submit` verifies the Turnstile token
  **before** calling `RegistrationTransaction.register`, which keeps taking the
  per-client and global permits; so the order is Turnstile → per-client permit →
  global permit → field validation → BCrypt → the two-domain write. The inversion
  relative to login is accepted on purpose: a blank or absent token is `FAILED`
  **without** an outbound call, so only token-bearing submissions cost a siteverify
  round trip, and those are bounded by E1 and by the 2 s ceiling above; moving the
  permits out of the transaction would split the one place that composes
  registration. A client refused by a limiter after a successful verification has
  spent a single-use token; the re-rendered form issues a fresh one.

## Rejected alternatives

Recorded so they are not reintroduced.

| Alternative | Why not |
|---|---|
| **Cloudflare Access as the auth system** | A workforce zero-trust gate for known users behind an existing IdP. No public self-registration, no user store, no email verification, no `VISION` entitlement, no suspension, no email-less classroom student accounts. It would sit *in front of* the accounts domain, not replace it, and would wall off the anonymous-browse catalog the design depends on. |
| **A hosted IdP (Auth0, Clerk, Cognito, Supabase)** | Rejected mainly for the student/classroom model: email-less, teacher-sponsored minors with minimized child data do not fit a hosted identity product's user model or data residency, and the entitlement/suspension logic would be split across two systems. The domain already exists and is tested against both adapters. |
| **Global-only in-app limiting as the primary defense** | The defect this ADR exists for: a name-scoped limiter lets one client exhaust the budget for everyone. Kept only as a backstop with raised thresholds (D5). |
| **Edge-only limiting; drop the keyed in-app limiters** | On Free the single rule counts per 10 s, path-only, GET and POST together; it cannot express "10 attempts a minute per client". The keyed limiter is the precision control and keeps working without an edge. |
| **Origin lockdown by allowlisting Cloudflare IP ranges** | Cloudflare rates it moderately secure (IP-spoofable), it needs a firewall the app may not own, a range list to keep current, and the same ranges as a Tomcat `internal-proxies` regex. Impossible on a platform-owned ingress. |
| **Authenticated Origin Pulls (mTLS)** | Very secure, but the origin must terminate TLS and verify Cloudflare's client certificate: either embedded Tomcat owns a certificate and the Cloudflare CA rotation, or a reverse proxy is added just for it. Unavailable where the platform terminates TLS, and explicitly incompatible with Cloudflare Tunnel. |
| **Shared-secret header validation** | Moderately secure (replayable). Kept only as the fallback in D3 for a platform that cannot run `cloudflared`. |
| **`server.forward-headers-strategy=framework` (`ForwardedHeaderFilter`)** | Trusts forwarded headers from any peer; it has no trusted-proxy list. Any client could choose its own rate-limit key. |
| **Reading `CF-Connecting-IP` / `X-Forwarded-For` in application code** | A header any client can send is not an identity. Trust is a property of the peer, which only the container knows (`RemoteIpValve`). `ClientKey` already refuses this. |
| **Turnstile verification inside the `accounts` domain** | `accounts` is pure auth (design D5) and knows nothing of HTTP or forms; the token is a request artifact. Verification is a cross-boundary external call like email and lives at the same level: kernel port, vendor adapter, composition-root wiring. |
| **Turnstile verification as an app-only class with no port** | Breaks ports-and-adapters and the ADR-026 gate's reach; the log-only dev mode needs a second implementation of the same port anyway. |
| **Fail open on `UNAVAILABLE`** | See D6. Converts an outage — or anything that looks like one — into a bypass of the only rotating-IP-proof control, silently. |
| **Fail closed for registration, fail open for login** | Considered for the login-lockout cost. Rejected because the inconsistency is itself a bug surface (two policies to reason about, one of them silent), and the login path is the credential-stuffing target the challenge exists for. |
| **A WAF Managed Challenge rule instead of Turnstile** | Not inline with the form: it cannot bind a solved challenge to one submission, counts nothing per submission, and on Free cannot see the body. Kept as E2, defense in depth, not the primary. |
| **Pro plan as a prerequisite** | Nothing in the design needs it. D2 records what it buys. |

## Consequences

- Deployment gains a hard prerequisite list: a zone on Cloudflare, a tunnel and
  connector, one Turnstile widget, the E1–E5 settings, and three secrets
  (`naturalist.challenge.site-key`, `secret-key`, the tunnel token). The runbook
  in the plan carries them.
- `adapters/` gains `challenge-turnstile`; `kernels/framework` gains
  `com.naturalist.challenge`; the composition root gains `ChallengeConfiguration`
  and `ChallengeProperties` and the `challenge.verify` strategy.
- Two more fail-closed boot conditions join `AdminProperties` and
  `EmailConfiguration`.
- Parking-lot item "per-client keyed limiters" (the `4aa4532` follow-up) is
  decided: build it, after the client-address PR.
- Shares the keyed-limiter worktree, the `ClientKey` deploy-order constraint and a
  future CSP with ADR-028 (deployment secrets and model spend protection); the plan's
  coordination section says who owns what.
- Not covered here and still open before hosting: PL-16 (image reads share the
  resilience executor) and PL-17 (EXIF on raw uploads).

## Open items

Raised during ADR-028's review of this ADR; neither changes the decisions above.

- **Add `POST /insects/identify` (and `/plants/identify`) to rule E1's path set?**
  Recommendation: **no** for E1 — identification is already `VISION`-gated,
  session-authenticated and CSRF-protected, and ADR-028 gives it its own
  per-address limiter and bulkhead; a path-only 10-per-10-s edge rule would also
  count the gallery page loads that share the prefix. Revisit if Security Events
  show volumetric POSTs reaching the origin on those routes.
- **Should `naturalist.challenge.secret-key` resolve through ADR-028's
  `SecretProvider` port?** Recommendation: **yes, once the port exists** — the
  Turnstile secret is exactly the class of credential that port is for, and
  `ChallengeConfiguration` should read it the way the Anthropic key is read.
  Until then it is a plain Spring property supplied at deploy time, never in the
  yml (the `AdminProperties` placeholder trap applies). The fail-closed boot rule
  is unchanged either way.
